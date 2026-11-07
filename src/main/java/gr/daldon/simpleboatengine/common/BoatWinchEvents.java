package gr.daldon.simpleboatengine.common;

import gr.daldon.simpleboatengine.SimpleBoatEngine;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** First click selects the boat; second click on trailer runs the winch. */
@Mod.EventBusSubscriber(modid=SimpleBoatEngine.ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class BoatWinchEvents {
    private BoatWinchEvents() {}

    /**
     * Direct Automobility coupling: hold the unplaced trailer item and
     * right-click an automobile to spawn and hitch it immediately.
     */
    @SubscribeEvent
    public static void attachTrailerToCar(PlayerInteractEvent.EntityInteract event) {
        if (!BoatTrailer.automobile(event.getTarget())) return;
        var player = event.getEntity();
        var stack = player.getItemInHand(event.getHand());
        if (!stack.is(EngineItems.BOAT_TRAILER.get())) return;
        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            BoatTrailer trailer = EngineEntities.BOAT_TRAILER.get().create(player.level());
            if (trailer != null) {
                Vec3 target = BoatTrailer.couplingPosition(event.getTarget());
                trailer.setPos(target.x, target.y, target.z);
                trailer.setYRot(event.getTarget().getYRot());
                trailer.coupleTo(event.getTarget());
                trailer.setMaxUpStep(1.20f);
                if (player.level().addFreshEntity(trailer)) {
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    player.displayClientMessage(Component.literal(
                        "Boat trailer attached to Automobility car."), true);
                }
            }
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void selectBoat(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof BlueFishingBoat boat)) return;
        var player = event.getEntity();
        var rope = player.getItemInHand(event.getHand());
        if (!rope.is(EngineItems.WINCH_ROPE.get())) return;
        if (!player.level().isClientSide) {
            rope.getOrCreateTag().putUUID("BoatWinchTarget", boat.getUUID());
            player.displayClientMessage(Component.literal(
                "Boat selected. Right-click the trailer with this black rope to winch it in."), true);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
