package gr.daldon.simpleboatengine.common;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Boat cargo wrapper: every change is written back to the original packed item. */
public final class ParkedBoatInventory implements Container {
    private final BoatStandBlockEntity stand;
    private final NonNullList<ItemStack> items=NonNullList.withSize(135,ItemStack.EMPTY);
    public ParkedBoatInventory(BoatStandBlockEntity stand){
        this.stand=stand;
        ItemStack boat=stand.getItem(0);
        if(!boat.hasTag() || !boat.getTag().contains("PackedFishingBoat",10))return;
        CompoundTag saved=boat.getTag().getCompound("PackedFishingBoat");
        for(Tag value:saved.getList("FishingBoatHatchCargo",Tag.TAG_COMPOUND)){
            CompoundTag entry=(CompoundTag)value;
            int i=entry.getByte("Slot") & 255;
            if(i<27)items.set(i,ItemStack.of(entry));
        }
        for(Tag value:saved.getList("FishingBoatCargo",Tag.TAG_COMPOUND)){
            CompoundTag entry=(CompoundTag)value;
            int i=entry.getByte("Slot") & 255;
            if(i<108)items.set(27+i,ItemStack.of(entry));
        }
    }
    @Override public int getContainerSize(){return 135;}
    @Override public boolean isEmpty(){
        for(ItemStack s:items)if(!s.isEmpty())return false;
        return true;
    }
    @Override public ItemStack getItem(int i){return items.get(i);}
    @Override public ItemStack removeItem(int i,int count){
        ItemStack taken=ContainerHelper.removeItem(items,i,count);
        if(!taken.isEmpty())setChanged();
        return taken;
    }
    @Override public ItemStack removeItemNoUpdate(int i){
        return ContainerHelper.takeItem(items,i);
    }
    @Override public void setItem(int i,ItemStack stack){
        items.set(i,stack);
        if(stack.getCount()>getMaxStackSize())stack.setCount(getMaxStackSize());
        setChanged();
    }
    @Override public void setChanged(){
        if(stand==null || stand.getItem(0).isEmpty())return;
        ItemStack boat=stand.getItem(0).copy();
        CompoundTag packed=boat.getOrCreateTag().getCompound("PackedFishingBoat");
        ListTag hatch=new ListTag(),cargo=new ListTag();
        for(int i=0;i<135;i++){
            ItemStack stack=items.get(i);
            if(stack.isEmpty())continue;
            CompoundTag entry=new CompoundTag();
            entry.putByte("Slot",(byte)(i<27?i:i-27));
            stack.save(entry);
            if(i<27)hatch.add(entry);else cargo.add(entry);
        }
        packed.put("FishingBoatHatchCargo",hatch);
        packed.put("FishingBoatCargo",cargo);
        boat.getOrCreateTag().put("PackedFishingBoat",packed);
        stand.setItem(0,boat);
    }
    @Override public boolean stillValid(Player p){
        return stand!=null && stand.getLevel()!=null
            && stand.getLevel().getBlockEntity(stand.getBlockPos())==stand
            && p.distanceToSqr(stand.getBlockPos().getX()+.5,
                stand.getBlockPos().getY()+.5,stand.getBlockPos().getZ()+.5)<=100;
    }
    @Override public void clearContent(){items.clear();setChanged();}
}
