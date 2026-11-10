package gr.daldon.simpleboatengine.common;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/** One world-wide record per hull, independent of the entity UUID assigned on launch. */
public final class BoatNavigationData extends SavedData {
    private static final String HULL_ID = "BoatNavigationId";
    private final Map<UUID, Entry> entries = new HashMap<>();
    private final Map<UUID, WeakReference<BlueFishingBoat>> afloat = new HashMap<>();

    static BoatNavigationData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
            BoatNavigationData::load, BoatNavigationData::new, "daldons_boat_navigation");
    }

    private static BoatNavigationData load(CompoundTag root) {
        BoatNavigationData data = new BoatNavigationData();
        for (Tag value : root.getList("Boats", Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag) value;
            ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
            if (!tag.hasUUID("Boat") || dimension == null) continue;
            var location = GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dimension),
                BlockPos.of(tag.getLong("Position")));
            Entry entry = new Entry(location);
            entry.preview = ItemStack.of(tag.getCompound("Preview"));
            entry.distance = Math.max(0, tag.getDouble("Distance"));
            entry.fish = Math.max(0, tag.getLong("Fish"));
            data.entries.put(tag.getUUID("Boat"), entry);
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag root) {
        ListTag boats = new ListTag();
        entries.forEach((id, entry) -> {
            CompoundTag tag = entry.logPage(id);
            tag.putDouble("Distance", entry.distance);
            tag.putString("Dimension", entry.location.dimension().location().toString());
            tag.putLong("Position", entry.location.pos().asLong());
            boats.add(tag);
        });
        root.put("Boats", boats);
        return root;
    }

    private Entry observe(UUID id, ServerLevel level, BlockPos position) {
        GlobalPos location = GlobalPos.of(level.dimension(), position.immutable());
        Entry entry = entries.get(id);
        if (entry == null) {
            entry = new Entry(location);
            entries.put(id, entry);
            setDirty();
        } else if (!entry.location.equals(location)) {
            entry.location = location;
            setDirty();
        }
        return entry;
    }

    private BlueFishingBoat live(UUID id) {
        WeakReference<BlueFishingBoat> ref = afloat.get(id);
        if (ref == null) return null;
        BlueFishingBoat boat = ref.get();
        if (boat != null && boat.isAlive()) return boat;
        afloat.remove(id);
        return null;
    }

    static UUID id(BlueFishingBoat boat) {
        CompoundTag identity = boat.getPersistentData();
        return identity.hasUUID(HULL_ID) ? identity.getUUID(HULL_ID) : boat.getUUID();
    }

    UUID observeBoat(BlueFishingBoat boat, ServerLevel level, double distance, boolean refreshPreview) {
        CompoundTag identity = boat.getPersistentData();
        UUID id = id(boat);
        BlueFishingBoat existing = live(id);
        if (existing != null && existing != boat) id = boat.getUUID();
        if (!identity.hasUUID(HULL_ID) || existing != null && existing != boat) identity.putUUID(HULL_ID, id);
        if (existing != boat) afloat.put(id, new WeakReference<>(boat));
        Entry entry = observe(id, level, boat.blockPosition());
        if (distance > 0) {
            entry.distance += distance;
            setDirty();
        }
        if (refreshPreview || entry.preview.isEmpty()) {
            ItemStack preview = boat.paintedHull();
            copyAttachments(preview, MotorData.read(boat));
            updatePreview(entry, preview);
        }
        return id;
    }

    void unload(BlueFishingBoat boat) {
        CompoundTag identity = boat.getPersistentData();
        if (!identity.hasUUID(HULL_ID)) return;
        UUID id = identity.getUUID(HULL_ID);
        WeakReference<BlueFishingBoat> ref = afloat.get(id);
        if (ref != null && ref.get() == boat) afloat.remove(id);
    }

    static UUID packedId(ItemStack item) {
        if (!item.hasTag()) return null;
        CompoundTag forge = item.getTag().getCompound("PackedFishingBoat").getCompound("ForgeData");
        return forge.hasUUID(HULL_ID) ? forge.getUUID(HULL_ID) : null;
    }

    UUID observePacked(ItemStack boat, ServerLevel level, BlockPos position, boolean assignId) {
        if (!(boat.getItem() instanceof SkiffBoatItem)) return null;
        UUID id = packedId(boat);
        if (id == null) {
            if (!assignId) return null;
            id = UUID.randomUUID();
            BoatPaintSessions.writableMotor(boat);
            boat.getTag().getCompound("PackedFishingBoat").getCompound("ForgeData").putUUID(HULL_ID, id);
        }
        if (live(id) != null) return null;
        Entry entry = observe(id, level, position);
        ItemStack preview = new ItemStack(boat.getItem());
        BoatPaint.setWoodStyle(preview, BoatPaint.woodStyle(boat, ((SkiffBoatItem) boat.getItem()).getHullStyle()));
        for (int channel = 0; channel < BoatPaint.CHANNELS; channel++)
            BoatPaint.set(preview, channel, BoatPaint.get(boat, channel));
        BoatPaint.setName(preview, BoatPaint.name(boat));
        BoatPaint.setNameColor(preview, BoatPaint.nameColor(boat));
        BoatPaint.setNameGlow(preview, BoatPaint.nameGlows(boat));
        copyAttachments(preview, BoatPaintSessions.motor(boat));
        updatePreview(entry, preview);
        return id;
    }

    private static void copyAttachments(ItemStack preview, CompoundTag motor) {
        CompoundTag equipment = BoatPaintSessions.writableMotor(preview);
        for (String key : new String[]{"Installed", "PoleLightInstalled", "CanisterInstalled",
                "IceBreakerInstalled", "RadioInstalled"})
            equipment.putBoolean(key, motor.getBoolean(key));
    }

    private void updatePreview(Entry entry, ItemStack preview) {
        if (ItemStack.matches(entry.preview, preview)) return;
        entry.preview = preview;
        setDirty();
    }

    GlobalPos target(UUID id) {
        Entry entry = entries.get(id);
        return entry == null ? null : entry.location;
    }

    CompoundTag logPage(UUID id) {
        Entry entry = entries.get(id);
        return entry == null ? null : entry.logPage(id);
    }

    void addFish(UUID id, int count) {
        Entry entry = entries.get(id);
        if (entry == null) return;
        entry.fish += count;
        setDirty();
    }

    private static final class Entry {
        private GlobalPos location;
        private ItemStack preview = ItemStack.EMPTY;
        private double distance;
        private long fish;

        Entry(GlobalPos location) {
            this.location = location;
        }

        CompoundTag logPage(UUID id) {
            CompoundTag page = new CompoundTag();
            page.putUUID("Boat", id);
            page.put("Preview", preview.save(new CompoundTag()));
            page.putLong("Distance", (long) distance);
            page.putLong("Fish", fish);
            return page;
        }
    }
}
