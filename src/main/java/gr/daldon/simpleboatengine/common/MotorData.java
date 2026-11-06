package gr.daldon.simpleboatengine.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.vehicle.Boat;

/** This project's own persistent boat state, deliberately isolated from other mods. */
public final class MotorData {
    public static final int TANK_CAPACITY = 3000;
    public static final int EXTRA_CANISTER_CAPACITY = 3000;
    public static final double FUEL_PER_BLOCK_AT_FULL_THROTTLE = TANK_CAPACITY / 5000.0;
    private static final String KEY = "SimpleBoatEngine";

    private MotorData() {}

    public static CompoundTag read(Boat boat) {
        return boat.getPersistentData().getCompound(KEY);
    }

    public static CompoundTag writable(Boat boat) {
        CompoundTag root = boat.getPersistentData();
        if (!root.contains(KEY, 10)) root.put(KEY, new CompoundTag());
        return root.getCompound(KEY);
    }

    public static boolean installed(Boat boat) {
        return read(boat).getBoolean("Installed");
    }

    public static boolean running(Boat boat) {
        return read(boat).getBoolean("Running");
    }

    /** Switches on at the beginning of dusk and off when dawn is brightening. */
    public static boolean isDark(long dayTime) {
        long time = Math.floorMod(dayTime, 24000L);
        return time >= 12000L && time < 23000L;
    }

    public static int capacity(CompoundTag data) {
        return TANK_CAPACITY + (data.getBoolean("CanisterInstalled") ? EXTRA_CANISTER_CAPACITY : 0);
    }

    public static int throttle(CompoundTag data) {
        return Math.max(0, Math.min(10, data.getInt("Throttle")));
    }

    public static int throttleLimit(CompoundTag data) {
        return data.getFloat("Fuel") < capacity(data) * 0.2f ? 3 : 10;
    }

    public static CompoundTag snapshot(Boat boat) {
        CompoundTag stored = read(boat);
        CompoundTag output = new CompoundTag();
        output.putBoolean("Installed", stored.getBoolean("Installed"));
        output.putBoolean("Running", stored.getBoolean("Running"));
        output.putInt("Throttle", throttle(stored));
        output.putFloat("Fuel", stored.getFloat("Fuel"));
        output.putBoolean("CanisterInstalled", stored.getBoolean("CanisterInstalled"));
        output.putBoolean("PoleLightInstalled", stored.getBoolean("PoleLightInstalled"));
        output.putBoolean("RollbarInstalled", stored.getBoolean("RollbarInstalled"));
        return output;
    }
}
