package gr.daldon.simpleboatengine.common;

import gr.daldon.simpleboatengine.SimpleBoatEngine;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** First click selects the boat; second click on trailer runs the winch. */
@Mod.EventBusSubscriber(modid=SimpleBoatEngine.ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class BoatWinchEvents {
    private BoatWinchEvents() {}

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
