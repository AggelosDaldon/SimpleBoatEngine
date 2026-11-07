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

    /** Exact five-part white hatch at the bow, not the surrounding blue deck. */
    public static boolean aimedAtHatch(BlueFishingBoat boat, Vec3 eye, Vec3 view, double reach) {
        return target(boat, eye, view, reach,
            new AABB(-0.38, BoatMounts.HULL_Y + 13.2 / 16.0,
                -1.98, 0.38, BoatMounts.HULL_Y + 15.0 / 16.0, -1.34));
    }

    /** The custom 'storage 2' access area immediately behind the raised bow deck. */
    public static boolean aimedAtStorage2(BlueFishingBoat boat, Vec3 eye, Vec3 view, double reach) {
        return target(boat, eye, view, reach,
            new AABB(-0.48, BoatMounts.HULL_Y + 6.2 / 16.0,
                -1.32, 0.48, BoatMounts.HULL_Y + 14.0 / 16.0, -0.44));
    }

    /** Extended clickable shape; vanilla boat pick box stops well before the bow. */
    public static boolean aimedAtHull(BlueFishingBoat boat, Vec3 eye, Vec3 view, double reach) {
        return target(boat, eye, view, reach,
            new AABB(-1.10, -0.20, -2.58, 1.10, 1.50, 2.46));
    }
}
