package gr.daldon.simpleboatengine.common;

import gr.daldon.simpleboatengine.SimpleBoatEngine;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Walkable, two-level deck for a 4.7-block-long boat. Vanilla Boat collision is
 * only a small central AABB, so it cannot act as the long bow/stern floor.
 * Supports players on client and server without altering water buoyancy bounds.
 */
@Mod.EventBusSubscriber(modid = SimpleBoatEngine.ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BoatDeckSupport {
    // Client and integrated server tick on different threads in singleplayer.
    private static final Map<Player, Track> TRACKED = Collections.synchronizedMap(new WeakHashMap<>());
    private static final double FLOOR = BoatMounts.HULL_Y + 4.95 / 16.0;
    private static final double BOW_DECK = BoatMounts.HULL_Y + 13.27 / 16.0;

    private static final class Track {
        final BlueFishingBoat boat;
        Vec3 last;

        Track(BlueFishingBoat boat) {
            this.boat = boat;
            this.last = boat.position();
        }
    }

    private BoatDeckSupport() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.isPassenger() || player.isSpectator() || player.isCreative() && player.getAbilities().flying) {
            TRACKED.remove(player);
            return;
        }

        Track track = TRACKED.get(player);
        if (track != null && (!track.boat.isAlive() || track.boat.level() != player.level())) {
            TRACKED.remove(player);
            track = null;
        }

        // Move with the moving hull as a real deck would. Compare against the
        // previous tracked position, not xOld, to avoid double carry on ticks.
        if (track != null) {
            Vec3 delta = track.boat.position().subtract(track.last);
            if (delta.lengthSqr() < 1.0 && delta.lengthSqr() > 0.0000001)
                player.setPos(player.getX() + delta.x, player.getY() + delta.y,
                    player.getZ() + delta.z);
            track.last = track.boat.position();
        }

        BlueFishingBoat support = null;
        double supportY = 0;
        for (BlueFishingBoat boat : player.level().getEntitiesOfClass(BlueFishingBoat.class,
                player.getBoundingBox().inflate(3.1, 1.6, 3.1))) {
            double dx = player.getX() - boat.getX();
            double dz = player.getZ() - boat.getZ();
            double yaw = Math.toRadians(180.0 - boat.getYRot());
            double c = Math.cos(yaw), s = Math.sin(yaw);
            double x = dx * c - dz * s;
            double z = dx * s + dz * c;

            // Bow deck reaches the blue nose. White inner floor reaches stern.
            boolean bow = z >= -2.40 && z < -0.49 && Math.abs(x) <=
                (z < -1.85 ? 0.42 : z < -1.35 ? 0.60 : 0.79);
            boolean floor = z >= -0.51 && z <= 2.26 && Math.abs(x) <= 0.78;
            if (!bow && !floor) continue;

            double desiredY = boat.getY() + (bow ? BOW_DECK : FLOOR);
            double feet = player.getY();
            boolean alreadyOnBoat = track != null && track.boat == boat;
            // No automatic climb out of water. A first-time landing must be
            // from above; existing walkers can step over the 0.52-block ledge.
            if (player.isInWater() && (!alreadyOnBoat || feet < desiredY - 0.10)) continue;
            double minFeet = alreadyOnBoat ? desiredY - 0.68 : desiredY - 0.075;
            if (feet < minFeet || feet > desiredY + 0.64) continue;
            if (player.getDeltaMovement().y > 0.13) continue; // jumping

            support = boat;
            supportY = desiredY;
            break;
        }

        if (support == null) {
            TRACKED.remove(player);
            return;
        }
        if (track == null || track.boat != support) TRACKED.put(player, new Track(support));
        // Keep the player's feet at the exact textured floor/deck top.
        player.setPos(player.getX(), supportY, player.getZ());
        Vec3 velocity = player.getDeltaMovement();
        if (velocity.y < 0) player.setDeltaMovement(velocity.x, 0, velocity.z);
        player.setOnGround(true);
        player.fallDistance = 0;
    }
}
