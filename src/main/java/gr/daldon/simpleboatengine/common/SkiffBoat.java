package gr.daldon.simpleboatengine.common;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** Experimental two-seat fishing skiff using vanilla boat buoyancy and controls. */
public class SkiffBoat extends Boat {
    public SkiffBoat(EntityType<? extends Boat> type, Level level) {
        super(type, level);
        setVariant(Boat.Type.OAK);
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
    protected int getMaxPassengers() {
        return 2;
    }

    protected Vec3 seatPosition(int seat) {
        return new Vec3(0, 0, (seat == 0 ? 27.05 : -24.05) / 16.0);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        // Use the vanilla boat's rotation and passenger handling, but move the
        // riders to the exact fore/aft seats of the larger Blockbench hull.
        super.positionRider(passenger, (entity, x, y, z) -> {
            int seat = getPassengers().indexOf(passenger);
            Vec3 local = seatPosition(seat);
            double yaw = Math.toRadians(180.0 - getYRot());
            // Keep both passengers aligned with the lower floating hull.
            // BlueFishingBoat overrides the two local positions for its side seat.
            move.accept(entity,
                getX() + local.x * Math.cos(yaw) + local.z * Math.sin(yaw),
                // Both named Blockbench seats top out at 9.43/16 model units.
                // Renderer translates the hull up by BoatMounts.HULL_Y. Vanilla
                // passenger placement already subtracts roughly 0.10 blocks.
                // Compensate that offset so rider feet rest ON the seat surface.
                y + (this instanceof BlueFishingBoat
                    ? BoatMounts.HULL_Y + 9.43 / 16.0 - 0.21 : 0.71),
                getZ() - local.x * Math.sin(yaw) + local.z * Math.cos(yaw));
        });
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
