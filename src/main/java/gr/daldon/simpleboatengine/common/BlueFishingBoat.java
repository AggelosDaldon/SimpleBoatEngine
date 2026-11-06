package gr.daldon.simpleboatengine.common;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;

/** Aggelos' blue fishing boat, with the original rear driver and port-side seat. */
public final class BlueFishingBoat extends SkiffBoat {
    private static final EntityDataAccessor<Integer> HULL_STYLE =
        SynchedEntityData.defineId(BlueFishingBoat.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HULL_PAINT =
        SynchedEntityData.defineId(BlueFishingBoat.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FLOOR_PAINT =
        SynchedEntityData.defineId(BlueFishingBoat.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<CompoundTag> CUSTOMIZATION =
        SynchedEntityData.defineId(BlueFishingBoat.class, EntityDataSerializers.COMPOUND_TAG);
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
        return paintedItem();
    }

    private ItemStack paintedItem() {
        ItemStack stack = new ItemStack(getDropItem());
        BoatPaint.setHull(stack, getHullPaint());
        BoatPaint.setFloor(stack, getFloorPaint());
        for (int channel = 2; channel < BoatPaint.CHANNELS; channel++)
            BoatPaint.set(stack, channel, getPaint(channel));
        BoatPaint.setName(stack, getBoatName());
        return stack;
    }

    /** A harvested boat remembers both dyes instead of reverting to the base item. */
    @Override
    protected void destroy(DamageSource source) {
        kill();
        if (level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS))
            spawnAtLocation(paintedItem());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(HULL_STYLE, 0);
        entityData.define(HULL_PAINT, -1);
        entityData.define(FLOOR_PAINT, -1);
        entityData.define(CUSTOMIZATION, new CompoundTag());
    }

    public int getHullStyle() { return entityData.get(HULL_STYLE); }

    public void setHullStyle(int style) {
        entityData.set(HULL_STYLE, Math.max(0, Math.min(9, style)));
    }

    public int getHullPaint() { return entityData.get(HULL_PAINT); }
    public int getFloorPaint() { return entityData.get(FLOOR_PAINT); }
    public void setHullPaint(int id) { entityData.set(HULL_PAINT, id >= 0 && id < 16 ? id : -1); }
    public void setFloorPaint(int id) { entityData.set(FLOOR_PAINT, id >= 0 && id < 16 ? id : -1); }

    public int getPaint(int channel) {
        if (channel == BoatPaint.HULL) return getHullPaint();
        if (channel == BoatPaint.FLOOR) return getFloorPaint();
        return BoatPaint.get(extrasAsItem(), channel);
    }

    private ItemStack extrasAsItem() {
        ItemStack stack = new ItemStack(getDropItem());
        stack.setTag(entityData.get(CUSTOMIZATION).copy());
        return stack;
    }

    public void setPaint(int channel, int id) {
        if (channel == BoatPaint.HULL) { setHullPaint(id); return; }
        if (channel == BoatPaint.FLOOR) { setFloorPaint(id); return; }
        CompoundTag copy = entityData.get(CUSTOMIZATION).copy();
        ItemStack temp = new ItemStack(getDropItem());
        temp.setTag(copy);
        BoatPaint.set(temp, channel, id);
        entityData.set(CUSTOMIZATION, temp.getTag().copy());
    }

    public int[] getPaints() {
        int[] result = new int[BoatPaint.CHANNELS];
        for (int i = 0; i < result.length; i++) result[i] = getPaint(i);
        return result;
    }

    public String getBoatName() { return BoatPaint.name(extrasAsItem()); }

    public void setBoatName(String name) {
        ItemStack temp = extrasAsItem();
        BoatPaint.setName(temp, name);
        entityData.set(CUSTOMIZATION, temp.getTag().copy());
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
        tag.putInt("FishingBoatHullPaint", getHullPaint());
        tag.putInt("FishingBoatFloorPaint", getFloorPaint());
        tag.put("FishingBoatCustomization", entityData.get(CUSTOMIZATION).copy());
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
        setHullPaint(tag.contains("FishingBoatHullPaint") ? tag.getInt("FishingBoatHullPaint") : -1);
        setFloorPaint(tag.contains("FishingBoatFloorPaint") ? tag.getInt("FishingBoatFloorPaint") : -1);
        entityData.set(CUSTOMIZATION, tag.getCompound("FishingBoatCustomization").copy());
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
