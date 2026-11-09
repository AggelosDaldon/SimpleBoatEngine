package gr.daldon.simpleboatengine.common;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.List;
import java.util.Map;

public final class BoatBuilderMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final SimpleContainer ingredients=new SimpleContainer(12);
    private final List<BoatBuilderRecipes.Blueprint> blueprints=BoatBuilderRecipes.all();
    private final DataSlot selected=DataSlot.standalone();
    public BoatBuilderMenu(int id,Inventory inv,FriendlyByteBuf buf){
        this(id,inv,buf.readBlockPos());
    }
    public BoatBuilderMenu(int id,Inventory inv,BlockPos pos){
        super(EngineMenus.BOAT_BUILDER.get(),id);
        this.pos=pos;selected.set(0);addDataSlot(selected);
        for(int i=0;i<12;i++)addSlot(new Slot(ingredients,i,27+(i%6)*23,82+(i/6)*25));
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)
            addSlot(new Slot(inv,9+col+row*9,17+col*18,153+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,17+col*18,211));
    }
    public int selection(){return selected.get();}
    public BlockPos position(){return pos;}
    public BoatBuilderRecipes.Blueprint recipe(){return blueprints.get(Math.floorMod(selected.get(),blueprints.size()));}
    public void change(int direction){
        selected.set(Math.floorMod(selected.get()+Integer.signum(direction),blueprints.size()));
        broadcastChanges();
    }
    public ItemStack input(int i){return ingredients.getItem(i);}
    public boolean canCraft(){
        int slot=0;
        for(var entry:recipe().cost().entrySet()){
            ItemStack stack=input(slot++);
            if(!stack.is(entry.getKey()) || stack.getCount()<entry.getValue())return false;
        }
        return true;
    }
    public void craft(Player player){
        if(!canCraft())return;
        int slot=0;
        for(var entry:recipe().cost().entrySet())
            ingredients.removeItem(slot++,entry.getValue());
        ItemStack result=recipe().result().copy();
        if(!player.getInventory().add(result))player.drop(result,false);
        broadcastChanges();
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0 || index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);
        if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),original=stack.copy();
        if(index<12){
            if(!moveItemStackTo(stack,12,slots.size(),true))return ItemStack.EMPTY;
        }else if(!moveItemStackTo(stack,0,12,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();
        return original;
    }
    @Override public void removed(Player player){
        super.removed(player);
        for(int i=0;i<12;i++){
            ItemStack stack=ingredients.removeItemNoUpdate(i);
            if(!stack.isEmpty() && !player.getInventory().add(stack))player.drop(stack,false);
        }
    }
    @Override public boolean stillValid(Player player){
        return player.level().getBlockState(pos).is(EngineBlocks.BOAT_BUILDER.get())
            && player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64;
    }
}
