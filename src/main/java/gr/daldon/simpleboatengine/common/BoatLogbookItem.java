package gr.daldon.simpleboatengine.common;

import gr.daldon.simpleboatengine.client.BoatLogbookScreen;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public final class BoatLogbookItem extends Item {
    private static final String PAGES = "Boats";
    private static final String MAIN_BOAT = "DaldonsMainFishingBoat";
    static final int MAX_BOATS = 32;
    enum Result { ADDED, ALREADY_ADDED, FULL }

    BoatLogbookItem(Properties properties) {
        super(properties);
    }

    public static ListTag pages(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getList(PAGES, Tag.TAG_COMPOUND) : new ListTag();
    }

    static Result addBoat(ItemStack book, UUID id, BoatNavigationData data) {
        ListTag pages = pages(book);
        for (Tag tag : pages) {
            CompoundTag page = (CompoundTag) tag;
            if (page.hasUUID("Boat") && id.equals(page.getUUID("Boat"))) return Result.ALREADY_ADDED;
        }
        if (pages.size() >= MAX_BOATS) return Result.FULL;
        pages.add(data.logPage(id));
        book.getOrCreateTag().put(PAGES, pages);
        return Result.ADDED;
    }

    static UUID mainBoat(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return persisted.hasUUID(MAIN_BOAT) ? persisted.getUUID(MAIN_BOAT) : null;
    }

    public static boolean isMainPage(ItemStack book, CompoundTag page) {
        CompoundTag tag = book.getTag();
        return tag != null && tag.hasUUID(MAIN_BOAT) && page.hasUUID("Boat")
            && tag.getUUID(MAIN_BOAT).equals(page.getUUID("Boat"));
    }

    static void selectMain(Player player, InteractionHand hand, UUID id) {
        ItemStack book = player.getItemInHand(hand);
        if (!book.is(EngineItems.BOAT_LOGBOOK.get())) return;
        ListTag pages = pages(book);
        for (int i = 0; i < Math.min(MAX_BOATS, pages.size()); i++) {
            CompoundTag page = pages.getCompound(i);
            if (!page.hasUUID("Boat") || !id.equals(page.getUUID("Boat"))) continue;
            CompoundTag root = player.getPersistentData();
            CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
            persisted.putUUID(MAIN_BOAT, id);
            root.put(Player.PERSISTED_NBT_TAG, persisted);
            syncMain(book, id);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "message.daldonsboatadditions.main_boat"), true);
            return;
        }
    }

    private static void syncMain(ItemStack book, UUID id) {
        CompoundTag tag = book.getTag();
        if (id == null) {
            if (tag != null) tag.remove(MAIN_BOAT);
        } else if (tag == null || !tag.hasUUID(MAIN_BOAT) || !id.equals(tag.getUUID(MAIN_BOAT))) {
            book.getOrCreateTag().putUUID(MAIN_BOAT, id);
        }
    }

    @Override public void inventoryTick(ItemStack stack, Level level, Entity carrier, int slot, boolean selected) {
        if (!(level instanceof ServerLevel server) || level.getGameTime() % 20 != 0) return;
        if (carrier instanceof Player player) syncMain(stack, mainBoat(player));
        ListTag old = pages(stack);
        if (old.isEmpty()) return;
        BoatNavigationData data = BoatNavigationData.get(server);
        ListTag updated = new ListTag();
        for (int i = 0; i < Math.min(MAX_BOATS, old.size()); i++) {
            CompoundTag page = old.getCompound(i);
            if (!page.hasUUID("Boat")) continue;
            CompoundTag fresh = data.logPage(page.getUUID("Boat"));
            updated.add(fresh == null ? page.copy() : fresh);
        }
        if (!old.equals(updated)) stack.getOrCreateTag().put(PAGES, updated);
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide)
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BoatLogbookScreen.open(hand));
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
