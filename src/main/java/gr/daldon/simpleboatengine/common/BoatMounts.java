package gr.daldon.simpleboatengine.common;

import net.minecraft.world.entity.vehicle.Boat;

/** Shared, boat-local attachment points used by rendering, interaction and light logic. */
public final class BoatMounts {
    public record Mount(float x, float y, float z) {}

    // Lower the authored keel into the water, preserving the top of the blue
    // gunwale as a usable surface. The old +0.24 left the antifouling exposed.
    // The source waterline marker rises to 3.65/16 on the underwater hull.
    // Keep the water surface below the 4.95/16 interior floor.
    public static final float HULL_Y = 0.10f;

    private BoatMounts() {}

    public static Mount motor(Boat boat) {
        // Motor front edge intersects the stern by ~0.15 block; propeller
        // spins below the waterline rather than in the air.
        if (boat instanceof BlueFishingBoat) return new Mount(0f, 0.38f, 2.55f);
        if (boat instanceof SkiffBoat) return new Mount(0f, 1.18f, 3.12f);
        if (boat.getVariant() == Boat.Type.BAMBOO)
            return new Mount(0f, boat.getBbHeight() * 0.7f, 1.12f);
        return new Mount(0f, boat.getBbHeight() / 3f, 1.24f);
    }

    public static Mount canister(Boat boat) {
        // Both custom boats carry the auxiliary tank down on the starboard cabin floor,
        // clear of the gunwale and the passenger seats.
        if (boat instanceof BlueFishingBoat) return new Mount(0.59f, HULL_Y + 4.95f / 16f, 0.70f);
        if (boat instanceof SkiffBoat) return new Mount(0.60f, 0.79f, 0.82f);
        return new Mount(0.73f, boat.getBbHeight() * 0.34f, 0.55f);
    }

    public static Mount poleLight(Boat boat) {
        // Pole foot sits on TOP of the outer rail, not down on the inner floor.
        if (boat instanceof BlueFishingBoat) return new Mount(-0.935f, HULL_Y + 14.25f / 16f, 2.09f);
        if (boat instanceof SkiffBoat) return new Mount(-0.85f, 1.38f, 2.08f);
        // Slightly further inward, still on the rear rail of a vanilla boat.
        return new Mount(-0.57f, boat.getBbHeight() * 0.98f, 0.90f);
    }
}
