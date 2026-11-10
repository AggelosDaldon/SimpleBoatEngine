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
    static final int CARGO_SLOTS = 135;
    static final int RADIO_SLOTS = 9;
    private final BoatStandBlockEntity stand;
    private ItemStack loadedBoat;
    private final NonNullList<ItemStack> items=NonNullList.withSize(CARGO_SLOTS + RADIO_SLOTS, ItemStack.EMPTY);
    public ParkedBoatInventory(BoatStandBlockEntity stand){
        this.stand=stand;
    }
    private void refresh() {
        ItemStack boat=stand.getItem(0);
        if (boat == loadedBoat) return;
        loadedBoat = boat;
        items.clear();
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
        if ((BoatPaintSessions.attachmentMask(boat) & BoatPaintSessions.RADIO) != 0) {
            for (Tag value : saved.getList(BoatRadioStorage.DISCS, Tag.TAG_COMPOUND)) {
                CompoundTag entry = (CompoundTag) value;
                int i = entry.getByte("Slot") & 255;
                if (i < RADIO_SLOTS) items.set(CARGO_SLOTS + i, ItemStack.of(entry));
            }
        }
    }
    public boolean hasRadio() {
        return (BoatPaintSessions.attachmentMask(stand.getItem(0)) & BoatPaintSessions.RADIO) != 0;
    }
    @Override public int getContainerSize(){return items.size();}
    @Override public boolean isEmpty(){
        refresh();
        for(ItemStack s:items)if(!s.isEmpty())return false;
        return true;
    }
    @Override public ItemStack getItem(int i){refresh();return items.get(i);}
    @Override public ItemStack removeItem(int i,int count){
        refresh();
        ItemStack taken=ContainerHelper.removeItem(items,i,count);
        if(!taken.isEmpty())setChanged();
        return taken;
    }
    @Override public ItemStack removeItemNoUpdate(int i){
        return removeItem(i, Integer.MAX_VALUE);
    }
    @Override public void setItem(int i,ItemStack stack){
        refresh();
        items.set(i,stack);
        if(stack.getCount()>getMaxStackSize())stack.setCount(getMaxStackSize());
        setChanged();
    }
    @Override public void setChanged(){
        if(stand.getItem(0).isEmpty() || stand.getItem(0) != loadedBoat || stand.hasPreview())return;
        ItemStack boat=stand.getItem(0).copy();
        CompoundTag packed=boat.getOrCreateTag().getCompound("PackedFishingBoat");
        ListTag hatch=new ListTag(),cargo=new ListTag(),radio=new ListTag();
        for(int i=0;i<items.size();i++){
            ItemStack stack=items.get(i);
            if(stack.isEmpty())continue;
            CompoundTag entry=new CompoundTag();
            entry.putByte("Slot",(byte)(i<27?i:i<CARGO_SLOTS?i-27:i-CARGO_SLOTS));
            stack.save(entry);
            if(i<27)hatch.add(entry);
            else if(i<CARGO_SLOTS)cargo.add(entry);
            else radio.add(entry);
        }
        packed.put("FishingBoatHatchCargo",hatch);
        packed.put("FishingBoatCargo",cargo);
        if (hasRadio()) packed.put(BoatRadioStorage.DISCS, radio);
        boat.getOrCreateTag().put("PackedFishingBoat",packed);
        stand.setItem(0,boat);
        loadedBoat = stand.getItem(0);
    }
    @Override public boolean stillValid(Player p){
        return !stand.getItem(0).isEmpty() && !stand.hasPreview() && stand.getLevel()!=null
            && stand.getLevel().getBlockEntity(stand.getBlockPos())==stand
            && p.distanceToSqr(stand.getBlockPos().getX()+.5,
                stand.getBlockPos().getY()+.5,stand.getBlockPos().getZ()+.5)<=100;
    }
    @Override public void clearContent(){
        refresh();
        items.clear();
        setChanged();
    }
}
