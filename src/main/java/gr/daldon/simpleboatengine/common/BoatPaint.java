package gr.daldon.simpleboatengine.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Two independent optional Minecraft-dye indices, persisted on each boat item. */
public final class BoatPaint {
    private static final String HULL = "BoatPaintHull";
    private static final String FLOOR = "BoatPaintFloor";

    private BoatPaint() {}

    public static int hull(ItemStack item) { return read(item, HULL); }
    public static int floor(ItemStack item) { return read(item, FLOOR); }

    private static int read(ItemStack item, String key) {
        CompoundTag tag = item.getTag();
        return tag != null && tag.contains(key, 3) ? valid(tag.getInt(key)) : -1;
    }

    private static int valid(int id) { return id >= 0 && id < 16 ? id : -1; }

    public static void setHull(ItemStack item, int id) {
        if (valid(id) >= 0) item.getOrCreateTag().putInt(HULL, id);
    }

    public static void setFloor(ItemStack item, int id) {
        if (valid(id) >= 0) item.getOrCreateTag().putInt(FLOOR, id);
    }
}
