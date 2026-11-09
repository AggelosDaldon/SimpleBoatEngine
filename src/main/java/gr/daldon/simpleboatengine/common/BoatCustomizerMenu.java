package gr.daldon.simpleboatengine.common;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** No item slots in this GUI. All materials are counted in player inventory,
 * and all painting drafts are validated against this player's selected stand.
 */
public final class BoatCustomizerMenu extends AbstractContainerMenu {
    private final BoatCustomizerBlockEntity table;
    private final BlockPos blockPos;
    private final List<BlockPos> stands;
    private final DataSlot selected=DataSlot.standalone();
    private final SimpleContainer materials=new SimpleContainer(12);
    public static final int MATERIAL_SLOTS=12;

    public BoatCustomizerMenu(int id, Inventory inv, FriendlyByteBuf packet) {
        this(id,inv,packet.readBlockPos());
    }
    private BoatCustomizerMenu(int id,Inventory inv,BlockPos pos) {
        this(id,inv,inv.player.level().getBlockEntity(pos)
            instanceof BoatCustomizerBlockEntity be ? be : null,pos);
    }
    public BoatCustomizerMenu(int id,Inventory inv,BoatCustomizerBlockEntity station) {
        this(id,inv,station,station.getBlockPos());
    }
    private BoatCustomizerMenu(int id,Inventory inv,BoatCustomizerBlockEntity station,BlockPos pos) {
        super(EngineMenus.CUSTOMIZER.get(),id);
        table=station;blockPos=pos;
        stands=station==null?List.of():List.copyOf(station.nearbyStands());
        selected.set(0);
        addDataSlot(selected);
        // Only 12 explicit material slots + the player's own inventory.
        for(int i=0;i<MATERIAL_SLOTS;i++)
            addSlot(new Slot(materials,i,22+(i%6)*25,227+(i/6)*24){
                @Override public int getMaxStackSize(){return 64;}
            });
        for(int row=0;row<3;row++)
            for(int col=0;col<9;col++)
                addSlot(new Slot(inv,col+row*9+9,22+col*18,279+row*18));
        for(int col=0;col<9;col++)
            addSlot(new Slot(inv,col,22+col*18,337));
    }
    public BlockPos getBlockPos(){return blockPos;}
    public int standCount(){return stands.size();}
    public int standIndex(){return stands.isEmpty()?-1:Math.floorMod(selected.get(),stands.size());}
    public BlockPos selectedStandPos(){return stands.isEmpty()?null:stands.get(standIndex());}
    public BoatStandBlockEntity selectedStand(){
        BlockPos target=selectedStandPos();
        return target!=null && table!=null && table.getLevel()!=null
            && table.getLevel().getBlockEntity(target) instanceof BoatStandBlockEntity stand
                ? stand : null;
    }
    public void selectRelative(int delta){
        if(!stands.isEmpty()){
            selected.set(Math.floorMod(standIndex()+Integer.signum(delta),stands.size()));
            broadcastChanges();
        }
    }
    public ItemStack boatItem(){
        BoatStandBlockEntity stand=selectedStand();
        return stand==null?ItemStack.EMPTY:stand.getItem(0);
    }
    public BoatCustomizerBlockEntity table(){return table;}
    // Compatibility with older customization consumers; no visible GUI slots.
    public ItemStack dyeItem(){return ItemStack.EMPTY;}
    public ItemStack woodItem(){return ItemStack.EMPTY;}
    public ItemStack nameDyeItem(){return ItemStack.EMPTY;}
    public int materialSlotCount(){return MATERIAL_SLOTS;}
    public ItemStack materialItem(int i){return materials.getItem(i);}
    public boolean hasMaterials(Map<Item,Integer> cost){
        if(cost.size()>MATERIAL_SLOTS)return false;
        int index=0;
        for(var entry:cost.entrySet()){
            ItemStack stack=materials.getItem(index++);
            if(!stack.is(entry.getKey()) || stack.getCount()<entry.getValue())
                return false;
        }
        return true;
    }
    public void consumeMaterials(Map<Item,Integer> cost){
        int index=0;
        for(var entry:cost.entrySet()) {
            materials.removeItem(index++,entry.getValue());
        }
        broadcastChanges();
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0 || index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);
        if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack original=slot.getItem(),copy=original.copy();
        if(index<MATERIAL_SLOTS){
            if(!moveItemStackTo(original,MATERIAL_SLOTS,slots.size(),true))
                return ItemStack.EMPTY;
        } else {
            if(!moveItemStackTo(original,0,MATERIAL_SLOTS,false))
                return ItemStack.EMPTY;
        }
        if(original.isEmpty())slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
    @Override public void removed(Player player){
        super.removed(player);
        // No materials are stored globally at the painting table.
        for(int i=0;i<MATERIAL_SLOTS;i++) {
            ItemStack stack=materials.removeItemNoUpdate(i);
            if(stack.isEmpty())continue;
            if(!player.getInventory().add(stack))player.drop(stack,false);
        }
    }
    @Override public boolean stillValid(Player p){
        return table!=null && table.stillValid(p);
    }
}
