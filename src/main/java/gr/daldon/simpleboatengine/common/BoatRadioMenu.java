package gr.daldon.simpleboatengine.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;

/** Nine-disc radio playlist, stored directly on the boat and in packed boat NBT. */
public final class BoatRadioMenu extends AbstractContainerMenu {
    private final BlueFishingBoat boat;
    private final Container discs;
    private final int boatId;
    private final DataSlot playing=DataSlot.standalone();
    public BoatRadioMenu(int id,Inventory inv,FriendlyByteBuf data){
        this(id,inv,data.readVarInt());
    }
    private BoatRadioMenu(int id,Inventory inv,int boatId){
        this(id,inv,inv.player.level().getEntity(boatId) instanceof BlueFishingBoat boat
            ?boat:null,boatId);
    }
    public BoatRadioMenu(int id,Inventory inv,BlueFishingBoat boat){
        this(id,inv,boat,boat.getId());
    }
    private BoatRadioMenu(int id,Inventory inv,BlueFishingBoat boat,int boatId){
        super(EngineMenus.BOAT_RADIO.get(),id);
        this.boatId=boatId;this.boat=boat;
        this.discs=boat==null?new SimpleContainer(9):boat.radioDiscs();
        addDataSlot(playing);
        for(int i=0;i<9;i++){
            int index=i;
            addSlot(new Slot(discs,i,24+(i%3)*22,26+(i/3)*23){
                @Override public boolean mayPlace(ItemStack item){
                    return item.getItem() instanceof RecordItem ||
                        item.is(net.minecraft.tags.ItemTags.MUSIC_DISCS) ||
                        net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item.getItem())
                            .getNamespace().equals("etched");
                }
                @Override public int getMaxStackSize(){return 1;}
                @Override public void setChanged(){
                    super.setChanged();
                    if(boat!=null && !boat.level().isClientSide && boat.radioIndex()==index)
                        boat.stopRadio();
                }
            });
        }
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)
            addSlot(new Slot(inv,9+row*9+col,68+col*18,167+row*18));
        for(int col=0;col<9;col++)
            addSlot(new Slot(inv,col,68+col*18,225));
    }
    public int boatId(){return boatId;}
    public ItemStack disc(int i){return discs.getItem(i);}
    public int current(){return playing.get();}
    @Override public void broadcastChanges(){
        if(boat!=null)playing.set(boat.radioIndex()+1);
        super.broadcastChanges();
    }
    @Override public boolean stillValid(Player player){
        return boat==null || boat.isAlive() && player.distanceToSqr(boat)<=64.0;
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);
        if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack item=slot.getItem(),original=item.copy();
        if(index<9){
            if(!moveItemStackTo(item,9,45,true))return ItemStack.EMPTY;
        }else{
            if(!(item.getItem() instanceof RecordItem) &&
                !item.is(net.minecraft.tags.ItemTags.MUSIC_DISCS))return ItemStack.EMPTY;
            if(!moveItemStackTo(item,0,9,false))return ItemStack.EMPTY;
        }
        if(item.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();
        return original;
    }
}
