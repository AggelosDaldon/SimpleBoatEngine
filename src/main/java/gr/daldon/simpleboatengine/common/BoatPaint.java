package gr.daldon.simpleboatengine.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Seven independent boat-surface paints and an optional painted bow name. */
public final class BoatPaint {
    public static final int HULL = 0, FLOOR = 1, STRIPE = 2, HATCH = 3,
        SEATS = 4, ANTIFOUL = 5, LINING = 6;
    public static final int CHANNELS = 7;
    private static final String[] KEYS = {
        "BoatPaintHull", "BoatPaintFloor", "BoatPaintStripe", "BoatPaintHatch",
        "BoatPaintSeats", "BoatPaintAntifoul", "BoatPaintLining"
    };
    private static final String NAME = "BoatCustomName";

    private BoatPaint() {}

    public static int hull(ItemStack item) { return get(item, HULL); }
    public static int floor(ItemStack item) { return get(item, FLOOR); }

    public static int get(ItemStack item, int channel) {
        if (channel < 0 || channel >= CHANNELS) return -1;
        return read(item, KEYS[channel], channel == ANTIFOUL ? 5 : 16);
    }

    public static int[] all(ItemStack stack) {
        int[] result = new int[CHANNELS];
        for (int channel = 0; channel < CHANNELS; channel++) result[channel] = get(stack, channel);
        return result;
    }

    private static int read(ItemStack item, String key, int count) {
        CompoundTag tag = item.getTag();
        return tag != null && tag.contains(key, 3) && tag.getInt(key) >= 0
            && tag.getInt(key) < count ? tag.getInt(key) : -1;
    }

    public static void setHull(ItemStack item, int id) {
        set(item, HULL, id);
    }

    public static void setFloor(ItemStack item, int id) {
        set(item, FLOOR, id);
    }

    public static void set(ItemStack item, int channel, int color) {
        if (channel < 0 || channel >= CHANNELS) return;
        int count = channel == ANTIFOUL ? 5 : 16;
        if (color >= 0 && color < count) item.getOrCreateTag().putInt(KEYS[channel], color);
    }

    public static String name(ItemStack item) {
        CompoundTag tag = item.getTag();
        return tag == null ? "" : tag.getString(NAME);
    }

    public static void setName(ItemStack item, String name) {
        String safe = name == null ? "" : name.replace('§', ' ').trim();
        item.getOrCreateTag().putString(NAME, safe.substring(0, Math.min(safe.length(), 20)));
    }
}
