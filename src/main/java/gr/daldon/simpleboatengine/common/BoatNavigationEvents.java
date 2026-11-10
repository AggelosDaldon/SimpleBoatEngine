package gr.daldon.simpleboatengine.common;

import gr.daldon.simpleboatengine.SimpleBoatEngine;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SimpleBoatEngine.MOD_ID)
public final class BoatNavigationEvents {
    private static final TagKey<Item> RAW_FISH = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "raw_fishes"));
    private static final TagKey<Item> EXTRA_FISH = TagKey.create(Registries.ITEM,
        new ResourceLocation(SimpleBoatEngine.ID, "logbook_fish"));

    private BoatNavigationEvents() {}

    public static boolean isNavigationTool(ItemStack stack) {
        return stack.is(Items.COMPASS) || stack.is(Items.BOOK)
            || stack.is(EngineItems.BOAT_TRACKER.get()) || stack.is(EngineItems.BOAT_LOGBOOK.get());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void useOnBoat(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof BlueFishingBoat boat) || !isNavigationTool(event.getItemStack())) return;
        bindAfloat(event.getEntity(), event.getHand(), boat);
        event.setCancellationResult(InteractionResult.sidedSuccess(boat.level().isClientSide));
        event.setCanceled(true);
    }

    static void bindAfloat(Player player, InteractionHand hand, BlueFishingBoat boat) {
        if (!(boat.level() instanceof ServerLevel level) || !boat.isAlive()
            || !isNavigationTool(player.getItemInHand(hand))) return;
        BoatNavigationData data = BoatNavigationData.get(level);
        UUID id = data.observeBoat(boat, level, 0, true);
        bind(player, hand, id, data);
    }

    static boolean bindParked(Player player, InteractionHand hand, BoatStandBlockEntity stand, ServerLevel level) {
        if (stand.getItem(0).isEmpty() || stand.hasPreview() || !isNavigationTool(player.getItemInHand(hand)))
            return false;
        BoatNavigationData data = BoatNavigationData.get(level);
        ItemStack boat = stand.getItem(0).copy();
        UUID id = data.observePacked(boat, level, stand.getBlockPos(), true);
        if (id == null) return false;
        if (!ItemStack.matches(boat, stand.getItem(0))) stand.setItem(0, boat);
        bind(player, hand, id, data);
        return true;
    }

    private static void bind(Player player, InteractionHand hand, UUID id, BoatNavigationData data) {
        ItemStack held = player.getItemInHand(hand);
        String message;
        if (held.is(Items.COMPASS) || held.is(EngineItems.BOAT_TRACKER.get())) {
            ItemStack tracker = held.is(Items.COMPASS) ? new ItemStack(EngineItems.BOAT_TRACKER.get()) : held;
            BoatTrackerItem.bind(tracker, id, data);
            if (tracker != held) replaceOne(player, hand, held, tracker);
            message = "message.daldonsboatadditions.tracker_linked";
        } else {
            ItemStack book = held.is(Items.BOOK) ? new ItemStack(EngineItems.BOAT_LOGBOOK.get()) : held;
            BoatLogbookItem.Result result = BoatLogbookItem.addBoat(book, id, data);
            if (result == BoatLogbookItem.Result.ADDED && book != held) replaceOne(player, hand, held, book);
            message = switch (result) {
                case ADDED -> "message.daldonsboatadditions.logbook_added";
                case ALREADY_ADDED -> "message.daldonsboatadditions.logbook_duplicate";
                case FULL -> "message.daldonsboatadditions.logbook_full";
            };
        }
        player.displayClientMessage(Component.translatable(message), true);
    }

    private static void replaceOne(Player player, InteractionHand hand, ItemStack held, ItemStack result) {
        if (held.getCount() == 1) player.setItemInHand(hand, result);
        else {
            held.shrink(1);
            if (!player.getInventory().add(result)) player.drop(result, false);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFish(ItemFishedEvent event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) return;
        int count = 0;
        for (ItemStack caught : event.getDrops()) {
            if (caught.is(ItemTags.FISHES) || caught.is(RAW_FISH) || caught.is(EXTRA_FISH))
                count += caught.getCount();
        }
        if (count == 0) return;

        BoatNavigationData data = BoatNavigationData.get(level);
        UUID main = BoatLogbookItem.mainBoat(player);
        double closestDistance = 25.0;
        BlueFishingBoat closest = null;
        BoatStandBlockEntity parked = null;
        for (BlueFishingBoat boat : level.getEntitiesOfClass(BlueFishingBoat.class,
                player.getBoundingBox().inflate(5), BlueFishingBoat::isAlive)) {
            double distance = boat.distanceToSqr(player);
            if (distance <= 25.0 && BoatNavigationData.id(boat).equals(main)) {
                data.addFish(data.observeBoat(boat, level, 0, false), count);
                return;
            }
            if (distance <= closestDistance) {
                closestDistance = distance;
                closest = boat;
            }
        }
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-5, -5, -5), center.offset(5, 5, 5))) {
            double distance = player.distanceToSqr(Vec3.atCenterOf(pos));
            if (distance > 25.0 || !level.hasChunkAt(pos)) continue;
            if (level.getBlockEntity(pos) instanceof BoatStandBlockEntity stand && !stand.getItem(0).isEmpty()) {
                if (main != null && main.equals(BoatNavigationData.packedId(stand.getItem(0)))) {
                    UUID id = data.observePacked(stand.getItem(0), level, stand.getBlockPos(), false);
                    if (id != null) {
                        data.addFish(id, count);
                        return;
                    }
                }
                if (distance < closestDistance || distance == closestDistance && closest == null) {
                    closestDistance = distance;
                    parked = stand;
                }
            }
        }
        UUID id = null;
        if (parked != null) {
            ItemStack boat = parked.getItem(0).copy();
            id = data.observePacked(boat, level, parked.getBlockPos(), true);
            if (id != null && !ItemStack.matches(boat, parked.getItem(0))) parked.setItem(0, boat);
        }
        if (id == null && closest != null) id = data.observeBoat(closest, level, 0, false);
        if (id != null) data.addFish(id, count);
    }

    @SubscribeEvent
    public static void onDrop(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof ItemEntity item
                && BoatNavigationData.packedId(item.getItem()) != null) {
            BoatNavigationData.get(level).observePacked(item.getItem(), level, item.blockPosition(), false);
        }
    }

    @SubscribeEvent
    public static void onBoatUnload(EntityLeaveLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof BlueFishingBoat boat)
            BoatNavigationData.get(level).unload(boat);
    }
}
