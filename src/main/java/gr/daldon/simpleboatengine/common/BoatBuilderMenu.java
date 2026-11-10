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
    private final SimpleContainer output=new SimpleContainer(1);
    private final List<BoatBuilderRecipes.Blueprint> blueprints=BoatBuilderRecipes.all();
    private final DataSlot selected=DataSlot.standalone();
    public BoatBuilderMenu(int id,Inventory inv,FriendlyByteBuf buf){
        this(id,inv,buf.readBlockPos());
    }
    public BoatBuilderMenu(int id,Inventory inv,BlockPos pos){
        super(EngineMenus.BOAT_BUILDER.get(),id);
        this.pos=pos;selected.set(0);addDataSlot(selected);
        ingredients.addListener(this::slotsChanged);
        for(int i=0;i<12;i++)addSlot(new Slot(ingredients,i,193+(i%6)*29,198+(i/6)*27));
        addSlot(new Slot(output,0,409,213){
            @Override public boolean mayPlace(ItemStack stack){return false;}
            @Override public boolean mayPickup(Player player){return canCraft();}
            @Override public void onTake(Player player,ItemStack taken){
                if(!canCraft())return;
                consumeRecipe();
                updateOutput();
                super.onTake(player,taken);
            }
        });
        updateOutput();
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)
            addSlot(new Slot(inv,9+col+row*9,142+col*18,276+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,142+col*18,334));
    }
    public int selection(){return selected.get();}
    public BlockPos position(){return pos;}
    public BoatBuilderRecipes.Blueprint recipe(){return blueprints.get(Math.floorMod(selected.get(),blueprints.size()));}
    public int category(){
        return selection()<10?0:selection()<blueprints.size()-3?1:2;
    }
    public int categoryStart(int category){
        return switch(category){case 0->0;case 1->10;default->blueprints.size()-3;};
    }
    public int categoryEnd(int category){
        return switch(category){case 0->10;case 1->blueprints.size()-3;default->blueprints.size();};
    }
    public int categorySize(){return categoryEnd(category())-categoryStart(category());}
    public ItemStack entry(int offset){
        int id=categoryStart(category())+offset;
        return offset>=0 && id<categoryEnd(category())
            ? blueprints.get(id).result():ItemStack.EMPTY;
    }
    public void changeCategory(int direction){
        int next=Math.floorMod(category()+Integer.signum(direction),3);
        selected.set(categoryStart(next));updateOutput();broadcastChanges();
    }
    public void selectVisible(int offset){
        int id=categoryStart(category())+offset;
        if(offset<0||id>=categoryEnd(category()))return;
        selected.set(id);updateOutput();broadcastChanges();
    }
    public void change(int direction){changeCategory(direction);}
    public ItemStack input(int i){return ingredients.getItem(i);}
    private void updateOutput(){
        ItemStack result=canCraft()?recipe().result().copy():ItemStack.EMPTY;
        output.setItem(0,result);
    }
    @Override public void slotsChanged(net.minecraft.world.Container inventory){
        if(output!=null && ingredients!=null)updateOutput();
        super.slotsChanged(inventory);
    }
    public boolean canCraft(){
        // Any arrangement is valid. A stack may contain more than the recipe needs.
        for(var entry:recipe().cost().entrySet()){
            int count=0;
            for(int i=0;i<12;i++)
                if(input(i).is(entry.getKey()))count+=input(i).getCount();
            if(count<entry.getValue())return false;
        }
        return true;
    }
    private void consumeRecipe(){
        for(var entry:recipe().cost().entrySet()){
            int remaining=entry.getValue();
            for(int i=0;i<12 && remaining>0;i++){
                ItemStack stack=input(i);
                if(!stack.is(entry.getKey()))continue;
                int used=Math.min(remaining,stack.getCount());
                ingredients.removeItem(i,used);
                remaining-=used;
            }
        }
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0 || index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);
        if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),original=stack.copy();
        if(index==12){
            if(!canCraft())return ItemStack.EMPTY;
            if(!moveItemStackTo(stack,13,slots.size(),true))return ItemStack.EMPTY;
            slot.onTake(player,original);
            return original;
        }
        if(index<12){
            if(!moveItemStackTo(stack,13,slots.size(),true))return ItemStack.EMPTY;
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
