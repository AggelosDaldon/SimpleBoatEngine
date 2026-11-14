package gr.daldon.simpleboatengine.common;

import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Interaction shapes covering the full fishing hull without inflating buoyancy physics. */
public final class BoatInteractionTarget {
    private BoatInteractionTarget() {}

    private static Vec3 local(Boat boat, Vec3 world) {
        double yaw = Math.toRadians(180.0 - boat.getYRot());
        double c = Math.cos(yaw), s = Math.sin(yaw);
        double dx = world.x - boat.getX(), dz = world.z - boat.getZ();
        return new Vec3(dx * c - dz * s, world.y - boat.getY(), dx * s + dz * c);
    }

    private static boolean target(Boat boat, Vec3 eye, Vec3 view, double reach, AABB shape) {
        if (view.lengthSqr() < 0.000001) return false;
        Vec3 start = local(boat, eye);
        Vec3 end = local(boat, eye.add(view.normalize().scale(reach)));
        return shape.contains(start) || shape.clip(start, end).isPresent();
    }

    /** Bow hatch, including the lip and handle, with a small margin around the model. */
    public static boolean aimedAtHatch(BlueFishingBoat boat, Vec3 eye, Vec3 view, double reach) {
        return target(boat, eye, view, reach,
            new AABB(-.39, BoatMounts.HULL_Y + .79, -2.01,
                .39, BoatMounts.HULL_Y + .95, -1.32));
    }

    /** The custom 'storage 2' access area immediately behind the raised bow deck. */
    public static boolean aimedAtStorage2(BlueFishingBoat boat, Vec3 eye, Vec3 view, double reach) {
        return target(boat, eye, view, reach,
            new AABB(-.46, BoatMounts.HULL_Y + .36, -1.31,
                .45, BoatMounts.HULL_Y + .76, -.40));
    }

    public static boolean aimedAtRadio(BlueFishingBoat boat,Vec3 eye,Vec3 view,double reach){
        return target(boat,eye,view,reach,
            new AABB(-.65, BoatMounts.HULL_Y + .79, -1.14,
                     -.23, BoatMounts.HULL_Y + 1.24, -.74));
    }
    public static boolean aimedAtNavigationHull(BlueFishingBoat boat, net.minecraft.world.entity.player.Player player) {
        if (!boat.isAlive() || player.distanceToSqr(boat) > 56.25) return false;
        Vec3 eye = player.getEyePosition();
        double reach = Math.min(5, eye.distanceTo(player.pick(5, 1f, false).getLocation()));
        return aimedAtHull(boat, eye, player.getViewVector(1f), reach);
    }

    /** Extended clickable shape; vanilla boat pick box stops well before the bow. */
    public static boolean aimedAtHull(BlueFishingBoat boat, Vec3 eye, Vec3 view, double reach) {
        return target(boat, eye, view, reach,
            new AABB(-1.10, -0.20, -2.58, 1.10, 1.50, 2.46));
    }
}
