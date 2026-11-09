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
    private final BlockPos standPos;
    private final Container cargo;
    private final DataSlot page=DataSlot.standalone();
    public ParkedBoatCargoMenu(int id,Inventory inv,FriendlyByteBuf packet){
        this(id,inv,packet.readBlockPos());
    }
    private ParkedBoatCargoMenu(int id,Inventory inv,BlockPos pos){
        this(id,inv,pos,inv.player.level().getBlockEntity(pos)
            instanceof BoatStandBlockEntity stand ? new ParkedBoatInventory(stand)
            : new SimpleContainer(135));
    }
    public ParkedBoatCargoMenu(int id,Inventory inv,BoatStandBlockEntity stand){
        this(id,inv,stand.getBlockPos(),new ParkedBoatInventory(stand));
    }
    private ParkedBoatCargoMenu(int id,Inventory inv,BlockPos pos,Container source){
        super(EngineMenus.PARKED_CARGO.get(),id);
        standPos=pos;cargo=source;page.set(0);addDataSlot(page);
        for(int i=0;i<135;i++){
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
        for(int row=0;row<3;row++)
            for(int col=0;col<9;col++)
                addSlot(new Slot(inv,col+row*9+9,8+col*18,140+row*18));
        for(int col=0;col<9;col++)
            addSlot(new Slot(inv,col,8+col*18,198));
    }
    public int page(){return page.get();}
    public void switchPage(int delta){
        page.set(Math.floorMod(page.get()+Integer.signum(delta),3));
        broadcastChanges();
    }
    public BlockPos standPos(){return standPos;}
    @Override public boolean stillValid(Player player){return cargo.stillValid(player);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0 || index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);
        if(!slot.hasItem() || !slot.isActive())return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),copy=stack.copy();
        if(index<135){
            if(!moveItemStackTo(stack,135,171,true))return ItemStack.EMPTY;
        } else {
            int start=page.get()==0?0:27+(page.get()-1)*54;
            int end=page.get()==0?27:start+54;
            if(!moveItemStackTo(stack,start,end,false))return ItemStack.EMPTY;
        }
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
