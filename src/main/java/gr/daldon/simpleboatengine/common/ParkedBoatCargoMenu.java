package gr.daldon.simpleboatengine.common;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Hatch (27), front main cargo (54), back main cargo (54). */
public final class ParkedBoatCargoMenu extends AbstractContainerMenu {
    public static final int RADIO_PAGE = 3;
    private static final int CARGO_SLOTS = ParkedBoatInventory.CARGO_SLOTS;
    private static final int BOAT_SLOTS = CARGO_SLOTS + ParkedBoatInventory.RADIO_SLOTS;
    private final BlockPos standPos;
    private final Container cargo;
    private final DataSlot page=DataSlot.standalone();
    private final DataSlot installedRadio = DataSlot.standalone();
    public ParkedBoatCargoMenu(int id,Inventory inv,FriendlyByteBuf packet){
        this(id,inv,packet.readBlockPos());
    }
    private ParkedBoatCargoMenu(int id,Inventory inv,BlockPos pos){
        this(id,inv,pos,new SimpleContainer(BOAT_SLOTS));
    }
    public ParkedBoatCargoMenu(int id,Inventory inv,BoatStandBlockEntity stand){
        this(id,inv,stand.getBlockPos(),new ParkedBoatInventory(stand));
    }
    private ParkedBoatCargoMenu(int id,Inventory inv,BlockPos pos,Container source){
        super(EngineMenus.PARKED_CARGO.get(),id);
        standPos=pos;cargo=source;addDataSlot(page);addDataSlot(installedRadio);
        for(int i=0;i<CARGO_SLOTS;i++){
            final int slot=i;
            int relative=i<27?i:i-27;
            int x=8+(relative%9)*18;
            int y=19+((relative/9)%6)*18;
            addSlot(new Slot(source,slot,x,y){
                @Override public boolean isActive(){
                    return slot<27 ? page.get()==0 : page.get()==1+(slot-27)/54;
                }
            });
        }
        for (int i=0;i<ParkedBoatInventory.RADIO_SLOTS;i++) {
            addSlot(new Slot(source,CARGO_SLOTS+i,8+i*18,19) {
                @Override public boolean isActive() { return page()==RADIO_PAGE && hasRadio(); }
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public boolean mayPickup(Player player) {
                    return isActive() && stillValid(player);
                }
            });
        }
        for(int row=0;row<3;row++)
            for(int col=0;col<9;col++)
                addSlot(new Slot(inv,col+row*9+9,8+col*18,140+row*18));
        for(int col=0;col<9;col++)
            addSlot(new Slot(inv,col,8+col*18,198));
    }
    public int page(){return page.get();}
    public boolean hasRadio(){
        return cargo instanceof ParkedBoatInventory parked ? parked.hasRadio() : installedRadio.get() != 0;
    }
    @Override public void broadcastChanges() {
        if (cargo instanceof ParkedBoatInventory parked) installedRadio.set(parked.hasRadio() ? 1 : 0);
        super.broadcastChanges();
    }
    public void selectPage(int selected){
        if (selected < 0 || selected > RADIO_PAGE) return;
        page.set(selected);
        broadcastChanges();
    }
    public BlockPos standPos(){return standPos;}
    @Override public boolean stillValid(Player player){return cargo.stillValid(player);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!stillValid(player) || index<0 || index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);
        if(!slot.hasItem() || !slot.isActive())return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),copy=stack.copy();
        if(index<BOAT_SLOTS){
            if(!moveItemStackTo(stack,BOAT_SLOTS,slots.size(),true))return ItemStack.EMPTY;
        } else {
            if (page()==RADIO_PAGE) return ItemStack.EMPTY;
            int start=page.get()==0?0:27+(page.get()-1)*54;
            int end=page.get()==0?27:start+54;
            if(!moveItemStackTo(stack,start,end,false))return ItemStack.EMPTY;
        }
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
