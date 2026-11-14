package gr.daldon.simpleboatengine.common;

import gr.daldon.simpleboatengine.SimpleBoatEngine;
import gr.daldon.simpleboatengine.client.BoatGuideScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SimpleBoatEngine.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BoatGuideItem extends WrittenBookItem {
    private static final int GUIDE_REVISION = 3;
    private static final String AUTHOR = "AggelosDaldon";
    private static final String GIVEN_KEY = "DaldonsBoatGuideGiven";
    private static final String[][] CHAPTERS = {
        {"Getting started", "Craft a Boat Builder and a Marine Paint Workshop at a crafting table. Use the Builder to make boats, gear, buoys and a Boat Parking Stand."},
        {"Boat Builder", "Select a blueprint, then add the materials shown. Stacks can go in any material slot. Take your crafted item from Output."},
        {"Fishing boats", "The Blue Fishing Boat and the nine wood variants share the same equipment. Right-click water to launch. Use the bow hatch and the inner cabinet to access separate storage."},
        {"Parking Stand", "Right-click an empty stand with a fishing boat to park it. Put the stand within 10 blocks of the Marine Paint Workshop."},
        {"Marine Workshop", "Select a parked boat to paint its parts, change its name or fit attachments. Supply the requested materials and use Apply to save paint and name changes."},
        {"Fitting attachments", "Put an attachment in any free equipment slot. The item type determines its place on the hull. Remove it from the slot to take it off. Fitting gear works without an engine."},
        {"Boat Engine", "Fit it at the workshop or right-click the hull with the engine. Aim at the motor with an empty hand and right-click to switch it on or off."},
        {"Driving", "Use the mouse wheel while driving to change throttle from 0 to 10. Steer as usual. The motor stops when you dismount."},
        {"Steel Fuel Canister", "Right-click the canister in your inventory while holding fuel on the cursor. A lava bucket or 60 coal or charcoal fills it. Alternatively use 20 coal and 20 redstone."},
        {"Refueling", "Switch the motor off and right-click it with the Steel Fuel Canister. Fit the red Auxiliary Fuel Canister to increase the boat's capacity from 3,000 to 6,000."},
        {"Pole Light", "Fit this light to illuminate the boat's surroundings. It turns on automatically at dusk and off at dawn. The light works even when the engine is removed."},
        {"Ice Breaker", "Fit it to a fishing boat to clear ice ahead of the bow. A Silk Touch Ice Breaker collects the ice into the boat's storage."},
        {"Boat Radio", "Right-click the fitted radio to open it. Insert up to nine discs, select a track and press Play. Use Stop to end playback or Next to advance."},
        {"Radio inventory", "Use Boat Inventory > Radio Inventory at the Marine Workshop to add or remove discs on a parked boat. If you remove the radio itself, its discs stay inside the item."},
        {"Recovery Hook", "Use the hook on an unoccupied fishing boat to recover it with its cargo and gear. The hook can also move a parked boat between parking stands."},
        {"Boat Tracker", "Right-click a fishing boat with a Compass to turn it into a Boat Tracker. The needle points to the boat's last known position in your dimension."},
        {"Boat Logbook", "Right-click a fishing boat with a Book to create a logbook. Use the logbook on more boats to add pages. Open it in the air to see previews and stats."},
        {"Fish caught", "The logbook counts fish reeled in within five blocks of the nearest fishing boat, even from shore. Set a Main Boat on its page to prioritize it within range."},
        {"Modded fishing", "Vanilla fish and tagged fish from compatible fishing mods, including Aquaculture 2, count when caught. Junk and treasure do not count."},
        {"Lift Buoys", "Orange, yellow and white buoys work the same way. Place one on a water source above a compatible fishing trap. Right-click the buoy to raise or lower the trap."},
    };

    public BoatGuideItem(Properties properties) {
        super(properties);
    }

    static ItemStack create() {
        ItemStack stack = new ItemStack(EngineItems.BOAT_GUIDE.get());
        writePages(stack);
        return stack;
    }

    private static void writePages(ItemStack stack) {
        ListTag pages = new ListTag();
        for (String[] chapter : CHAPTERS) {
            Component page = Component.literal(chapter[0]).withStyle(ChatFormatting.DARK_BLUE, ChatFormatting.BOLD)
                .append(Component.literal("\n\n" + chapter[1]).withStyle(style ->
                    style.withColor(ChatFormatting.BLACK).withBold(false)));
            pages.add(StringTag.valueOf(Component.Serializer.toJson(page)));
        }
        var tag = stack.getOrCreateTag();
        tag.putString("title", "Daldon's Boat Guide");
        tag.putString("author", AUTHOR);
        tag.put("pages", pages);
        tag.putBoolean("resolved", true);
        tag.putInt("GuideRevision", GUIDE_REVISION);
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack book = player.getItemInHand(hand);
        if (!book.hasTag() || book.getTag().getInt("GuideRevision") < GUIDE_REVISION) writePages(book);
        book.getOrCreateTag().putString("author", AUTHOR);
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BoatGuideScreen.open(book));
        }
        return InteractionResultHolder.sidedSuccess(book, level.isClientSide);
    }

    @Override public void appendHoverText(ItemStack stack, Level level, java.util.List<Component> lines,
                                          net.minecraft.world.item.TooltipFlag flags) {
        // Also display the updated credit on guides issued by older versions.
        ItemStack credited = stack.copy();
        credited.getOrCreateTag().putString("author", AUTHOR);
        super.appendHoverText(credited, level, lines, flags);
    }

    @SubscribeEvent
    public static void giveFirstGuide(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var root = player.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) {
            root.put(Player.PERSISTED_NBT_TAG, new net.minecraft.nbt.CompoundTag());
        }
        var persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(GIVEN_KEY)) return;
        ItemStack guide = create();
        if (!player.getInventory().add(guide)) player.drop(guide, false);
        persisted.putBoolean(GIVEN_KEY, true);
    }
}
