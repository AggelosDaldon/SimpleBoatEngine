package gr.daldon.simpleboatengine.common;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraftforge.network.NetworkHooks;

/** Experimental two-seat fishing skiff using vanilla boat buoyancy and controls. */
public final class SkiffBoat extends Boat {
    public SkiffBoat(EntityType<? extends Boat> type, Level level) {
        super(type, level);
        setVariant(Boat.Type.OAK);
    }

    @Override
    public Item getDropItem() {
        return EngineItems.LARGE_SKIFF.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(EngineItems.LARGE_SKIFF.get());
    }

    @Override
    protected int getMaxPassengers() {
        return 2;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        // Use the vanilla boat's rotation and passenger handling, but move the
        // riders to the exact fore/aft seats of the larger Blockbench hull.
        super.positionRider(passenger, (entity, x, y, z) -> {
            int seat = getPassengers().indexOf(passenger);
            double along = (seat == 0 ? 27.05 : -24.05) / 16.0;
            double yaw = Math.toRadians(180.0 - getYRot());
            // Raise riders with the visible hull, retaining their existing
            // seating height relative to the actual bench tops.
            move.accept(entity, getX() + along * Math.sin(yaw), y + 0.55,
                getZ() + along * Math.cos(yaw));
        });
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
