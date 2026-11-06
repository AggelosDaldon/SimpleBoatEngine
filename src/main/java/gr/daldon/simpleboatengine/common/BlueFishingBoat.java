package gr.daldon.simpleboatengine.common;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Aggelos' blue fishing boat, with the original rear driver and port-side seat. */
public final class BlueFishingBoat extends SkiffBoat {
    public BlueFishingBoat(EntityType<? extends Boat> type, Level level) {
        super(type, level);
    }

    @Override
    public Item getDropItem() {
        return EngineItems.BLUE_FISHING_BOAT.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(EngineItems.BLUE_FISHING_BOAT.get());
    }

    @Override
    protected Vec3 seatPosition(int seat) {
        // Named Blockbench pieces: drivers_seat (center rear), passenger_seat (port).
        if (seat == 0) return new Vec3(0, 0, 32.10 / 16.0);
        return new Vec3(-9.1 / 16.0, 0, 13.3 / 16.0);
    }
}
