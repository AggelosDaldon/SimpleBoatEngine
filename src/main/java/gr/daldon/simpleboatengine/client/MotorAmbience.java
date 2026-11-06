package gr.daldon.simpleboatengine.client;

import gr.daldon.simpleboatengine.SimpleBoatEngine;
import gr.daldon.simpleboatengine.common.EngineSounds;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Original synthesized engine audio and understated exhaust / wake particles. */
@Mod.EventBusSubscriber(modid = SimpleBoatEngine.ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
    value = Dist.CLIENT)
public final class MotorAmbience {
    private static final Map<UUID, RunningSound> PLAYING = new HashMap<>();
    private static ClientLevel lastWorld;
    private MotorAmbience() {}

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft game = Minecraft.getInstance();
        if (game.level != lastWorld) {
            PLAYING.clear();
            lastWorld = game.level;
        }
        if (game.level == null || game.isPaused()) return;
        Iterator<RunningSound> active = PLAYING.values().iterator();
        while (active.hasNext()) if (active.next().finished) active.remove();

        for (Entity entity : game.level.entitiesForRendering()) {
            if (!(entity instanceof Boat boat)) continue;
            CompoundTag data = ClientMotorState.get(boat);
            if (data == null || !data.getBoolean("Installed") || !data.getBoolean("Running")) continue;
            if (!PLAYING.containsKey(boat.getUUID())) {
                RunningSound sound = new RunningSound(boat);
                PLAYING.put(boat.getUUID(), sound);
                game.getSoundManager().play(sound);
            }

            int throttle = Math.max(0, Math.min(10, data.getInt("Throttle")));
            float attachment = boat.getVariant() == Boat.Type.BAMBOO
                ? boat.getBbHeight() * 0.7f : boat.getBbHeight() / 3.0f;
            double stern = boat.getVariant() == Boat.Type.BAMBOO ? 1.12 : 1.24;
            double yaw = Math.toRadians(180.0 - boat.getYRot());

            if (game.level.random.nextInt(4) == 0) {
                // Centre of the Blockbench "smoke particle here" marker.
                Vec3 outlet = position(boat, yaw, (9.22f-8f)/16f*1.5,
                    attachment + 7.94/16.0*1.5, stern + (12.19-8)/16.0*1.5);
                game.level.addParticle(ParticleTypes.SMOKE, outlet.x, outlet.y, outlet.z,
                    0, 0.009 + game.level.random.nextDouble() * 0.014, 0);
            }

            if (throttle > 0 && boat.isInWaterOrBubble()) {
                // The new propeller hub is centered at pixel (8,-4,8.3).
                Vec3 prop = position(boat, yaw, 0,
                    attachment - 4.0/16.0*1.5, stern + (8.3-8)/16.0*1.5);
                if (game.level.random.nextInt(5) != 0) {
                    for (int i = 0; i < 2 + throttle / 2; i++) {
                        double dx = (game.level.random.nextDouble()-0.5)*0.27;
                        double dz = (game.level.random.nextDouble()-0.5)*0.27;
                        game.level.addParticle(ParticleTypes.BUBBLE,
                            prop.x + dx, prop.y, prop.z + dz, dx*0.025, 0.026, dz*0.025);
                    }
                }
                if (game.level.random.nextInt(3) == 0) {
                    game.level.addParticle(ParticleTypes.SPLASH, prop.x, boat.getY()+0.06,
                        prop.z, 0, 0.04 + throttle*0.005, 0);
                }
            }
        }
    }

    private static Vec3 position(Boat boat, double yaw, double x, double height, double z) {
        double c = Math.cos(yaw), s = Math.sin(yaw);
        return new Vec3(boat.getX() + x*c + z*s,
            boat.getY() + height, boat.getZ() - x*s + z*c);
    }

    private static final class RunningSound extends AbstractTickableSoundInstance {
        private final Boat boat;
        private float fade;
        private boolean finished;

        RunningSound(Boat boat) {
            super(EngineSounds.MOTOR_LOOP.get(), SoundSource.NEUTRAL,
                SoundInstance.createUnseededRandom());
            this.boat = boat;
            this.looping = true;
            this.delay = 0;
            this.volume = 0f;
            this.pitch = 0.84f;
            this.x = boat.getX();
            this.y = boat.getY();
            this.z = boat.getZ();
        }

        @Override
        public boolean canStartSilent() { return true; }

        @Override
        public void tick() {
            CompoundTag data = ClientMotorState.get(boat);
            boolean on = !boat.isRemoved() && data != null && data.getBoolean("Running");
            fade = Math.max(0f, Math.min(1f, fade + (on ? 0.025f : -0.12f)));
            if (!on && fade == 0f) {
                finished = true;
                stop();
                return;
            }
            int throttle = data == null ? 0 : Math.max(0, Math.min(10, data.getInt("Throttle")));
            volume = fade * 0.67f;
            pitch = 0.84f + throttle * 0.039f;
            x = boat.getX();
            y = boat.getY();
            z = boat.getZ();
        }
    }
}
