package gr.daldon.simpleboatengine.common;

import gr.daldon.simpleboatengine.SimpleBoatEngine;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Authoritative engine rules; never trusts a client-supplied fuel or throttle value. */
@Mod.EventBusSubscriber(modid = SimpleBoatEngine.ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerMotorEvents {
    private static final Map<Boat, Vec3> PREVIOUS = new WeakHashMap<>();
    private ServerMotorEvents() {}

    @SubscribeEvent
    public static void attach(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Boat boat)
            || event.getHand() != InteractionHand.MAIN_HAND) return;
        ItemStack held = event.getEntity().getMainHandItem();
        if (held.is(EngineItems.POLE_LIGHT.get())) {
            if (!boat.level().isClientSide) {
                CompoundTag data = MotorData.writable(boat);
                if (!MotorData.installed(boat)) {
                    notify(event.getEntity(), "Install a boat engine before fitting the pole light.");
                } else if (data.getBoolean("PoleLightInstalled")) {
                    notify(event.getEntity(), "This boat already has a pole light.");
                } else {
                    data.putBoolean("PoleLightInstalled", true);
                    if (!event.getEntity().getAbilities().instabuild) held.shrink(1);
                    notify(event.getEntity(), "Automatic boat light fitted to the rear left corner.");
                    click(boat);
                }
            }
            event.setCancellationResult(InteractionResult.sidedSuccess(boat.level().isClientSide));
            event.setCanceled(true);
            return;
        }
        if (!held.is(EngineItems.ENGINE.get()) || MotorData.installed(boat)) return;
        if (!boat.level().isClientSide) {
            CompoundTag data = MotorData.writable(boat);
            data.putBoolean("Installed", true);
            data.putBoolean("Running", false);
            data.putInt("Throttle", 0);
            data.putFloat("Fuel", 0f);
            data.putBoolean("PoleLightInstalled", false);
            if (!event.getEntity().getAbilities().instabuild) held.shrink(1);
            notify(event.getEntity(), "Motor installed! Point at the motor to refuel and switch it on.");
            click(boat);
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(boat.level().isClientSide));
        event.setCanceled(true);
    }

    public static void useMotor(ServerPlayer player, Boat boat) {
        if (!MotorData.installed(boat)
            || !MotorTarget.aimedAtMotor(boat, player.getEyePosition(), player.getViewVector(1f), 5.5)) return;
        CompoundTag data = MotorData.writable(boat);
        ItemStack item = player.getMainHandItem();

        if (item.is(EngineItems.FUEL_BUCKET.get())) {
            if (data.getBoolean("Running")) {
                notify(player, "Switch off the motor before refueling.");
                return;
            }
            if (data.getFloat("Fuel") >= MotorData.capacity(data) - 0.01f) {
                notify(player, "The tank is already full.");
                return;
            }
            data.putFloat("Fuel", Math.min(MotorData.capacity(data),
                data.getFloat("Fuel") + MotorData.TANK_CAPACITY));
            if (!player.getAbilities().instabuild) {
                item.shrink(1);
                ItemStack emptyBucket = new ItemStack(Items.BUCKET);
                if (!player.getInventory().add(emptyBucket)) player.drop(emptyBucket, false);
            }
            notify(player, "Fuel: " + (int) data.getFloat("Fuel") + " / " + MotorData.capacity(data));
            click(boat);
            return;
        }

        if (!item.isEmpty()) return;
        boolean start = !data.getBoolean("Running");
        if (start && data.getFloat("Fuel") < 1f) {
            notify(player, "Refuel the motor before starting.");
            return;
        }
        data.putBoolean("Running", start);
        if (!start) data.putInt("Throttle", 0);
        notify(player, start ? "Motor ON - throttle 0" : "Motor OFF");
        click(boat);
    }

    private static void click(Boat boat) {
        boat.level().playSound(null, boat.getX(), boat.getY(), boat.getZ(),
            SoundEvents.LEVER_CLICK, SoundSource.NEUTRAL, 0.55f, 0.85f);
    }

    private static void notify(net.minecraft.world.entity.player.Player player, String message) {
        player.displayClientMessage(Component.literal(message), true);
    }

    /** Dismounting stops the motor immediately, even when cruise throttle was set. */
    @SubscribeEvent
    public static void onDismount(EntityMountEvent event) {
        if (!event.isDismounting()
            || !(event.getEntityBeingMounted() instanceof Boat boat)
            || !(event.getEntityMounting() instanceof ServerPlayer)
            || !(boat.level() instanceof ServerLevel level)
            || !MotorData.installed(boat)) return;

        CompoundTag data = MotorData.writable(boat);
        if (!data.getBoolean("Running")) return;

        data.putBoolean("Running", false);
        data.putInt("Throttle", 0);
        PREVIOUS.remove(boat);
        click(boat);
        // Send the updated state now instead of waiting for the next 5-tick sync.
        for (ServerPlayer nearby : level.players()) {
            if (nearby.distanceToSqr(boat) <= 64.0 * 64.0) {
                MotorPackets.sendStatus(boat, nearby);
            }
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof Boat boat) || !MotorData.installed(boat)) continue;
            CompoundTag data = MotorData.writable(boat);
            Vec3 here = boat.position();

            if (!data.getBoolean("Running")) {
                PREVIOUS.remove(boat);
            } else if (data.getFloat("Fuel") <= 0f) {
                data.putBoolean("Running", false);
                data.putInt("Throttle", 0);
                PREVIOUS.remove(boat);
            } else {
                int throttle = Math.min(MotorData.throttle(data), MotorData.throttleLimit(data));
                data.putInt("Throttle", throttle);

                Vec3 before = PREVIOUS.put(boat, here);
                double distance = before == null ? 0.0
                    : Math.hypot(here.x - before.x, here.z - before.z);
                if (distance <= 8.0 && throttle > 0) {
                    double burned = distance * MotorData.FUEL_PER_BLOCK_AT_FULL_THROTTLE
                        * throttle / 10.0;
                    float remaining = Math.max(0f, data.getFloat("Fuel") - (float) burned);
                    data.putFloat("Fuel", remaining);
                    if (remaining == 0f) {
                        data.putBoolean("Running", false);
                        data.putInt("Throttle", 0);
                        click(boat);
                    }
                }

                // Auto-cruise is applied by the player controlling the boat on the client.
                // This server tick measures the actual distance for fuel consumption.
            }

            if (level.getGameTime() % 5L == 0L) {
                BoatPoleLights.update(level, boat, data);
                for (ServerPlayer player : level.players()) {
                    if (player.distanceToSqr(boat) <= 64.0 * 64.0) {
                        MotorPackets.sendStatus(boat, player);
                    }
                }
            }
        }
    }
}
