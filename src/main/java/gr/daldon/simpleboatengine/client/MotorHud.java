package gr.daldon.simpleboatengine.client;

import gr.daldon.simpleboatengine.SimpleBoatEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** A compact speed / throttle / fuel indicator next to the hotbar. */
@Mod.EventBusSubscriber(modid = SimpleBoatEngine.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
    value = Dist.CLIENT)
public final class MotorHud {
    private static final int WIDTH = 103;
    private static final int HEIGHT = 42;
    private MotorHud() {}

    @SubscribeEvent
    public static void overlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) return;
        Minecraft game = Minecraft.getInstance();
        if (game.player == null || game.options.hideGui ||
            !(game.player.getVehicle() instanceof Boat boat)) return;
        CompoundTag data = ClientMotorState.get(boat);
        if (data == null || !data.getBoolean("Running")) return;

        int throttle = Math.max(0, Math.min(10, data.getInt("Throttle")));
        int capacity = data.getBoolean("CanisterInstalled") ? 6000 : 3000;
        float fuel = Math.max(0f, Math.min(capacity, data.getFloat("Fuel")));
        float fraction = fuel / capacity;
        double speed = boat.getDeltaMovement().horizontalDistance() * 20.0;
        int color = speed >= 11.0 ? 0xFFFF766B : speed >= 5.0 ? 0xFFFFC56A : 0xFF89D2BF;
        int fuelColor = fraction < 0.2f ? 0xFFFF837B : 0xFF8BD0A3;
        GuiGraphics gui = event.getGuiGraphics();
        int desired = gui.guiWidth() / 2 + 98;
        int left = Math.min(desired, gui.guiWidth() - WIDTH - 3);
        int top = left == desired ? gui.guiHeight() - 45 : gui.guiHeight() - 82;
        gui.fill(left, top, left + WIDTH, top + HEIGHT, 0xC91A252B);
        gui.fill(left, top, left + 2, top + HEIGHT, color);

        gui.drawString(game.font, "SPEED", left + 7, top + 4, 0xFFD8E0DF, false);
        String speedValue = String.format(java.util.Locale.ROOT, "%.1f b/s", speed);
        gui.drawString(game.font, speedValue, left + WIDTH - 5 - game.font.width(speedValue),
            top + 4, color, false);
        gui.drawString(game.font, "THROTTLE", left + 7, top + 15, 0xFFD8E0DF, false);
        String number = Integer.toString(throttle);
        gui.drawString(game.font, number, left + WIDTH - 7 - game.font.width(number),
            top + 15, color, false);
        gui.drawString(game.font, "FUEL", left + 7, top + 26, 0xFFD8E0DF, false);
        String quantity = (int) Math.ceil(fuel) + "/" + capacity;
        gui.drawString(game.font, quantity, left + WIDTH - 5 - game.font.width(quantity),
            top + 26, fuelColor, false);
        gui.fill(left + 5, top + HEIGHT - 3, left + WIDTH - 5, top + HEIGHT - 2, 0xFF465358);
        gui.fill(left + 5, top + HEIGHT - 3,
            left + 5 + Math.round((WIDTH - 10) * fraction), top + HEIGHT - 2, fuelColor);
    }
}
