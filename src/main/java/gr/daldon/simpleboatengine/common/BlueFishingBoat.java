package gr.daldon.simpleboatengine.common;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Containers;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Aggelos' blue fishing boat, with the original rear driver and port-side seat. */
public final class BlueFishingBoat extends SkiffBoat {
    private static final EntityDataAccessor<Integer> HULL_STYLE =
        SynchedEntityData.defineId(BlueFishingBoat.class, EntityDataSerializers.INT);
    private final SimpleContainer cargo = new SimpleContainer(54);
    private boolean cargoDropped;

    public BlueFishingBoat(EntityType<? extends Boat> type, Level level) {
        super(type, level);
    }

    @Override
    public Item getDropItem() {
        return EngineItems.boatForStyle(getHullStyle());
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(getDropItem());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(HULL_STYLE, 0);
    }

    public int getHullStyle() { return entityData.get(HULL_STYLE); }

    public void setHullStyle(int style) {
        entityData.set(HULL_STYLE, Math.max(0, Math.min(9, style)));
    }

    public void openCargo(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
            (id, inventory, owner) -> ChestMenu.sixRows(id, inventory, cargo),
            Component.translatable("container.simpleboatengine.fishing_boat")));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FishingBoatHullStyle", getHullStyle());
        ListTag list = new ListTag();
        for (int index = 0; index < cargo.getContainerSize(); index++) {
            var stack = cargo.getItem(index);
            if (stack.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putByte("Slot", (byte) index);
            stack.save(entry);
            list.add(entry);
        }
        tag.put("FishingBoatCargo", list);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setHullStyle(tag.getInt("FishingBoatHullStyle"));
        cargo.clearContent();
        for (Tag value : tag.getList("FishingBoatCargo", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            int slot = entry.getByte("Slot") & 255;
            if (slot < cargo.getContainerSize())
                cargo.setItem(slot, ItemStack.of(entry));
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide && reason.shouldDestroy() && !cargoDropped) {
            cargoDropped = true;
            Containers.dropContents(level(), blockPosition(), cargo);
        }
        super.remove(reason);
    }

    @Override
    protected Vec3 seatPosition(int seat) {
        // Named Blockbench pieces: drivers_seat (center rear), passenger_seat (port).
        if (seat == 0) return new Vec3(0, 0, 32.10 / 16.0);
        return new Vec3(-9.1 / 16.0, 0, 13.3 / 16.0);
    }
}
