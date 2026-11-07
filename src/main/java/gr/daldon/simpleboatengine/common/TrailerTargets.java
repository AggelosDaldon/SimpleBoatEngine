package gr.daldon.simpleboatengine.common;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Extended picking volume; visual trailer is much longer than vanilla entity BB. */
public final class TrailerTargets {
    private TrailerTargets() {}

    public static boolean aimedAtTrailer(BoatTrailer trailer, Vec3 eye, Vec3 direction,
                                         double reach) {
        AABB bounds = trailer.getBoundingBox().inflate(0.5, 0.65, 1.8);
        return intersects(bounds, eye, direction, reach);
    }
    public static boolean aimedAtCar(Entity car, Vec3 eye, Vec3 direction,
                                     double reach) {
        return intersects(car.getBoundingBox().inflate(0.55), eye, direction, reach);
    }
    private static boolean intersects(AABB box, Vec3 eye, Vec3 direction, double reach) {
        if (direction.lengthSqr() < 1.0e-8) return false;
        return box.contains(eye) || box.clip(eye, eye.add(direction.normalize().scale(reach))).isPresent();
    }
}
