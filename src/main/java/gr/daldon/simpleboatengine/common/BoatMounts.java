package gr.daldon.simpleboatengine.common;

import net.minecraft.world.entity.vehicle.Boat;

/** Shared, boat-local attachment points used by rendering, interaction and light logic. */
public final class BoatMounts {
    public record Mount(float x, float y, float z) {}

    // The entire Blockbench hull is translated up by this much in both custom renderers.
    // The 4.95px floor planks consequently render at Y = 0.789 blocks.
    public static final float HULL_Y = 0.48f;

    private BoatMounts() {}

    public static Mount motor(Boat boat) {
        if (boat instanceof BlueFishingBoat) return new Mount(0f, 1.18f, 3.05f);
        if (boat instanceof SkiffBoat) return new Mount(0f, 1.18f, 3.12f);
        if (boat.getVariant() == Boat.Type.BAMBOO)
            return new Mount(0f, boat.getBbHeight() * 0.7f, 1.12f);
        return new Mount(0f, boat.getBbHeight() / 3f, 1.24f);
    }

    public static Mount canister(Boat boat) {
        // Both custom boats carry the auxiliary tank down on the starboard cabin floor,
        // clear of the gunwale and the passenger seats.
        if (boat instanceof BlueFishingBoat) return new Mount(0.59f, 0.79f, 0.70f);
        if (boat instanceof SkiffBoat) return new Mount(0.60f, 0.79f, 0.82f);
        return new Mount(0.73f, boat.getBbHeight() * 0.34f, 0.55f);
    }

    public static Mount poleLight(Boat boat) {
        if (boat instanceof BlueFishingBoat) return new Mount(-0.87f, 1.38f, 2.02f);
        if (boat instanceof SkiffBoat) return new Mount(-0.85f, 1.38f, 2.08f);
        // Slightly further inward, still on the rear rail of a vanilla boat.
        return new Mount(-0.19f, boat.getBbHeight() * 0.98f, 0.83f);
    }
}
