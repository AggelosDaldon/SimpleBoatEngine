package gr.daldon.simpleboatengine.common;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.List;

/**
 * Right-click fishing-boat hull even outside the vanilla 2-block entity hit box.
 * The winch rope never consumes durability and keeps the target UUID on the item.
 */
public final class WinchRopeItem extends Item {
    public WinchRopeItem(Properties properties) { super(properties); }

    @Override public InteractionResultHolder<ItemStack> use(
        Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        List<BlueFishingBoat> boats = level.getEntitiesOfClass(BlueFishingBoat.class,
            player.getBoundingBox().inflate(7.5),
            boat -> boat.isAlive() && player.distanceToSqr(boat) < 64
                && BoatInteractionTarget.aimedAtHull(boat,
                    player.getEyePosition(), player.getViewVector(1f), 6.2));
        if (boats.isEmpty()) return InteractionResultHolder.pass(held);
        BlueFishingBoat target = boats.stream()
            .min(java.util.Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
        if (target == null) return InteractionResultHolder.pass(held);
        if (!level.isClientSide) {
            held.getOrCreateTag().putUUID("BoatWinchTarget", target.getUUID());
            player.displayClientMessage(Component.literal(
                "Boat selected. Right-click the trailer with the winch rope."), true);
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }
}
