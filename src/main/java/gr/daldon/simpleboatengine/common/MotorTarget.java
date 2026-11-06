package gr.daldon.simpleboatengine.common;

import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Converts a view ray into boat coordinates and tests the fitted outboard's shape. */
public final class MotorTarget {
    private MotorTarget() {}

    public static boolean aimedAtMotor(Boat boat, Vec3 eye, Vec3 direction, double reach) {
        if (direction.lengthSqr() < 0.000001) return false;
        double yaw = Math.toRadians(180.0 - boat.getYRot());
        Vec3 end = eye.add(direction.normalize().scale(reach));
        Vec3 a = local(boat, eye, yaw);
        Vec3 b = local(boat, end, yaw);
        double height = boat instanceof SkiffBoat ? 0.78 : boat.getBbHeight() / 3.0;
        double stern = boat instanceof SkiffBoat ? 2.70 : 1.24;
        if (!(boat instanceof SkiffBoat) && boat.getVariant() == Boat.Type.BAMBOO) {
            height = boat.getBbHeight() * 0.7;
            stern = 1.12;
        }
        // New Blockbench model bounds: x 4.98..11.02, y -8..8, z 3..12.24;
        // the world renderer scales those pixel coordinates by 1.5.
        return new AABB(-0.43, height - 0.83, stern - 0.57,
            0.43, height + 0.83, stern + 0.50).clip(a, b).isPresent();
    }

    private static Vec3 local(Boat boat, Vec3 world, double yaw) {
        double dx = world.x - boat.getX(), dz = world.z - boat.getZ();
        double c = Math.cos(yaw), s = Math.sin(yaw);
        return new Vec3(dx*c - dz*s, world.y - boat.getY(), dx*s + dz*c);
    }
}
