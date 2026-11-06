package gr.daldon.simpleboatengine.client;

import com.mojang.blaze3d.platform.InputConstants;
import gr.daldon.simpleboatengine.SimpleBoatEngine;
import gr.daldon.simpleboatengine.common.EngineItems;
import gr.daldon.simpleboatengine.common.MotorPackets;
import gr.daldon.simpleboatengine.common.MotorTarget;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = SimpleBoatEngine.ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
    value = Dist.CLIENT)
public final class MotorControls {
    private static final KeyMapping UP = new KeyMapping("key.simpleboatengine.up",
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "key.simpleboatengine.category");
    private static final KeyMapping DOWN = new KeyMapping("key.simpleboatengine.down",
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "key.simpleboatengine.category");
    private static boolean useHeld;
    private MotorControls() {}

    @Mod.EventBusSubscriber(modid = SimpleBoatEngine.ID, bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
    public static final class Keys {
        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(UP);
            event.register(DOWN);
        }
    }

    @SubscribeEvent
    public static void rightClickMotor(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        Minecraft game = Minecraft.getInstance();
        if (game.player == null || game.level == null || game.screen != null) return;
        var held = game.player.getMainHandItem();
        if (!held.isEmpty() && !held.is(EngineItems.FUEL_BUCKET.get())) return;

        Vec3 eye = game.player.getEyePosition();
        Vec3 look = game.player.getViewVector(1f);
        for (Entity entity : game.level.entitiesForRendering()) {
            if (!(entity instanceof Boat boat)) continue;
            CompoundTag data = ClientMotorState.get(boat);
            if (data == null || !data.getBoolean("Installed")) continue;
            if (!MotorTarget.aimedAtMotor(boat, eye, look, 5.5)) continue;
            if (event.getHand() == InteractionHand.MAIN_HAND && !useHeld) {
                useHeld = true;
                MotorPackets.CHANNEL.sendToServer(new MotorPackets.UseMotor(boat.getId()));
                event.setSwingHand(true);
            }
            event.setCanceled(true);
            return;
        }
    }

    @SubscribeEvent
    public static void mouseWheel(InputEvent.MouseScrollingEvent event) {
        Minecraft game = Minecraft.getInstance();
        if (game.player == null || game.screen != null ||
            !(game.player.getVehicle() instanceof Boat boat)) return;
        CompoundTag data = ClientMotorState.get(boat);
        if (data == null || !data.getBoolean("Running")) return;
        int direction = (int) Math.signum(event.getScrollDelta());
        if (direction == 0) return;
        change(boat, direction);
        event.setCanceled(true);
    }

    private static void change(Boat boat, int direction) {
        CompoundTag data = ClientMotorState.get(boat);
        if (data == null || !data.getBoolean("Running")) return;
        int old = data.getInt("Throttle");
        int tank = data.getBoolean("CanisterInstalled") ? 6000 : 3000;
        int limit = data.getFloat("Fuel") < tank * 0.2f ? 3 : 10;
        int next = Math.max(0, Math.min(limit, old + Integer.signum(direction)));
        if (next != old) {
            data.putInt("Throttle", next); // Immediate local feedback; server validates.
            MotorPackets.CHANNEL.sendToServer(new MotorPackets.SetThrottle(direction));
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft game = Minecraft.getInstance();
        if (!game.options.keyUse.isDown()) useHeld = false;
        if (game.level == null || game.player == null || game.isPaused()) return;
        if (!(game.player.getVehicle() instanceof Boat boat)) return;
        while (UP.consumeClick()) change(boat, 1);
        while (DOWN.consumeClick()) change(boat, -1);

        CompoundTag data = ClientMotorState.get(boat);
        if (data == null || !data.getBoolean("Running") || data.getFloat("Fuel") <= 0) return;
        int throttle = Math.max(0, Math.min(10, data.getInt("Throttle")));
        if (throttle <= 0 || !boat.isInWaterOrBubble()) return;
        // Throttle is a cruise-control lever: it applies thrust while the driver is
        // aboard, even without W. Vanilla player-controlled boats are client-simulated.
        Vec3 motion = boat.getDeltaMovement();
        double yaw = Math.toRadians(boat.getYRot());
        double acceleration = throttle * 0.006;
        double vx = motion.x - Math.sin(yaw) * acceleration;
        double vz = motion.z + Math.cos(yaw) * acceleration;
        double speed = Math.hypot(vx, vz);
        double maximum = 0.075 * throttle;
        if (speed > maximum) {
            vx *= maximum / speed;
            vz *= maximum / speed;
        }
        boat.setDeltaMovement(vx, motion.y, vz);
    }
}
