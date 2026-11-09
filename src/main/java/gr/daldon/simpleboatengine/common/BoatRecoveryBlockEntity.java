package gr.daldon.simpleboatengine.common;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Physical one-boat storage cradle. It never destroys cargo or attachments. */
public final class BoatRecoveryBlockEntity extends BlockEntity {
    private ItemStack boat=ItemStack.EMPTY;
    public BoatRecoveryBlockEntity(BlockPos pos,BlockState state){
        super(EngineBlockEntities.BOAT_RECOVERY.get(),pos,state);
    }
    public ItemStack boat(){return boat;}
    public boolean empty(){return boat.isEmpty();}
    public void store(ItemStack value){boat=value.copy();setChanged();sync();}
    public ItemStack take(){
        ItemStack result=boat;
        boat=ItemStack.EMPTY;setChanged();sync();
        return result;
    }
    private void sync(){if(level!=null && !level.isClientSide)
        level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag tag){
        super.saveAdditional(tag);
        if(!boat.isEmpty())tag.put("Boat",boat.save(new CompoundTag()));
    }
    @Override public void load(CompoundTag tag){
        super.load(tag);
        boat=tag.contains("Boat",10)?ItemStack.of(tag.getCompound("Boat")):ItemStack.EMPTY;
    }
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
