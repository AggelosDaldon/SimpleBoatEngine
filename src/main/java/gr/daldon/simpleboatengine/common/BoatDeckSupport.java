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
    private static final double RAIL = BoatMounts.HULL_Y + 14.3 / 16.0;

    private static final class Track {
        final BlueFishingBoat boat;
        Vec3 last;
        int unsupportedTicks;

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

            // Distinct support heights: bow, inner floor, both side rails and
            // full stern transom. A wide flat AABB could not support these.
            boolean bow = z >= -2.42 && z < -0.50 && Math.abs(x) <=
                (z < -1.85 ? 0.43 : z < -1.35 ? 0.66 : 0.86);
            boolean sternRail = z >= 2.12 && z <= 2.34 && Math.abs(x) <= 1.03;
            boolean sideRail = z >= -1.35 && z <= 2.30
                && Math.abs(x) > 0.73 && Math.abs(x) <= 1.08;
            boolean floor = z >= -1.25 && z < 2.14 && Math.abs(x) <= 0.82;
            if (!bow && !floor && !sideRail && !sternRail) continue;

            double desiredY = boat.getY() + (bow ? BOW_DECK :
                sternRail || sideRail ? RAIL : FLOOR);
            double feet = player.getY();
            boolean alreadyOnBoat = track != null && track.boat == boat;
            // No automatic climb out of water. A first-time landing must be
            // from above; existing walkers can step over the 0.52-block ledge.
            if (player.isInWater() && (!alreadyOnBoat || feet < desiredY - 0.12)) continue;
            double minFeet = alreadyOnBoat ? desiredY - 0.85 : desiredY - 0.09;
            if (feet < minFeet || feet > desiredY + 0.70) continue;
            // Jumping must not forget the boat underneath: previously clearing
            // TRACKED here caused clipping when walking onto the raised bow.
            if (player.getDeltaMovement().y > 0.13) continue;

            support = boat;
            supportY = desiredY;
            break;
        }

        if (support == null) {
            if (track == null || ++track.unsupportedTicks > 16
                || player.distanceToSqr(track.boat) > 25.0) TRACKED.remove(player);
            return;
        }
        if (track == null || track.boat != support) TRACKED.put(player, new Track(support));
        else track.unsupportedTicks = 0;
        // Keep the player's feet at the exact textured floor/deck top.
        player.setPos(player.getX(), supportY, player.getZ());
        Vec3 velocity = player.getDeltaMovement();
        if (velocity.y < 0) player.setDeltaMovement(velocity.x, 0, velocity.z);
        player.setOnGround(true);
        player.fallDistance = 0;
    }
}
