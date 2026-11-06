package gr.daldon.simpleboatengine.common;

import net.minecraft.world.entity.vehicle.Boat;

/** Shared, boat-local attachment points used by rendering, interaction and light logic. */
public final class BoatMounts {
    public record Mount(float x, float y, float z) {}

    // Waterline-calibrated hull origin. The previous 0.48 offset caused the
    // keel to float visibly above the water surface.
    public static final float HULL_Y = 0.24f;

    private BoatMounts() {}

    public static Mount motor(Boat boat) {
        if (boat instanceof BlueFishingBoat) return new Mount(0f, 0.96f, 2.88f);
        if (boat instanceof SkiffBoat) return new Mount(0f, 1.18f, 3.12f);
        if (boat.getVariant() == Boat.Type.BAMBOO)
            return new Mount(0f, boat.getBbHeight() * 0.7f, 1.12f);
        return new Mount(0f, boat.getBbHeight() / 3f, 1.24f);
    }

    public static Mount canister(Boat boat) {
        // Both custom boats carry the auxiliary tank down on the starboard cabin floor,
        // clear of the gunwale and the passenger seats.
        if (boat instanceof BlueFishingBoat) return new Mount(0.59f, 0.55f, 0.70f);
        if (boat instanceof SkiffBoat) return new Mount(0.60f, 0.79f, 0.82f);
        return new Mount(0.73f, boat.getBbHeight() * 0.34f, 0.55f);
    }

    public static Mount poleLight(Boat boat) {
        if (boat instanceof BlueFishingBoat) return new Mount(-0.84f, 1.13f, 1.62f);
        if (boat instanceof SkiffBoat) return new Mount(-0.85f, 1.38f, 2.08f);
        // Slightly further inward, still on the rear rail of a vanilla boat.
        return new Mount(-0.57f, boat.getBbHeight() * 0.98f, 0.90f);
    }
}
