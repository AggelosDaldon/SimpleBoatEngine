package gr.daldon.simpleboatengine.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Reuses the exact boat-item customization slots, NBT and GUI used by the workshop. */
public final class BoatStandBlockEntity extends BoatCustomizerBlockEntity {
    private net.minecraft.world.item.ItemStack previewBackup=net.minecraft.world.item.ItemStack.EMPTY;
    private java.util.UUID previewOwner;
    public boolean previewLockedFor(java.util.UUID id) {
        return previewOwner != null && !previewOwner.equals(id);
    }
    public net.minecraft.world.item.ItemStack previewOriginal() {
        return previewBackup.isEmpty() ? getItem(0).copy() : previewBackup.copy();
    }
    public void beginPreview(java.util.UUID owner) {
        if(previewOwner==null) {
            previewOwner=owner;
            previewBackup=getItem(0).copy();
            setChanged();
        }
    }
    public boolean hasPreview() {return previewOwner!=null;}
    public void cancelPreview(java.util.UUID owner) {
        if(previewOwner!=null && previewOwner.equals(owner)) {
            setItem(0,previewBackup.copy());
            finishPreview();
        }
    }
    public void finishPreview() {
        previewBackup=net.minecraft.world.item.ItemStack.EMPTY;
        previewOwner=null;
        setChanged();
    }
    @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag tag) {
        super.saveAdditional(tag);
        if(previewOwner!=null) {
            tag.putUUID("PreviewOwner",previewOwner);
            tag.put("PreviewOriginalBoat",previewBackup.save(new net.minecraft.nbt.CompoundTag()));
        }
    }
    @Override public void load(net.minecraft.nbt.CompoundTag tag) {
        super.load(tag);
        previewOwner=tag.hasUUID("PreviewOwner")?tag.getUUID("PreviewOwner"):null;
        previewBackup=tag.contains("PreviewOriginalBoat",10)
            ? net.minecraft.world.item.ItemStack.of(tag.getCompound("PreviewOriginalBoat"))
            : net.minecraft.world.item.ItemStack.EMPTY;
    }

    public BoatStandBlockEntity(BlockPos pos, BlockState state) {
        super(EngineBlockEntities.BOAT_STAND.get(), pos, state);
    }
}
