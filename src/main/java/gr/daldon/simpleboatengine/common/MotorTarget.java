package gr.daldon.simpleboatengine.common;

import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Converts a view ray into boat coordinates and tests our original motor's shape. */
public final class MotorTarget {
    private MotorTarget() {}

    public static boolean aimedAtMotor(Boat boat, Vec3 eye, Vec3 direction, double reach) {
        if (direction.lengthSqr() < 0.000001) return false;
        double yaw = Math.toRadians(180.0 - boat.getYRot());
        Vec3 end = eye.add(direction.normalize().scale(reach));
        Vec3 a = local(boat, eye, yaw);
        Vec3 b = local(boat, end, yaw);
        double height = boat.getBbHeight() / 3.0;
        double stern = 1.24;
        if (boat.getVariant() == Boat.Type.BAMBOO) {
            height = boat.getBbHeight() * 0.7;
            stern = 1.12;
        }
        return new AABB(-0.90, height - 0.83, stern - 0.66,
            0.52, height + 1.48, stern + 0.74).clip(a, b).isPresent();
    }

    private static Vec3 local(Boat boat, Vec3 world, double yaw) {
        double dx = world.x - boat.getX(), dz = world.z - boat.getZ();
        double c = Math.cos(yaw), s = Math.sin(yaw);
        return new Vec3(dx*c - dz*s, world.y - boat.getY(), dx*s + dz*c);
    }
}
