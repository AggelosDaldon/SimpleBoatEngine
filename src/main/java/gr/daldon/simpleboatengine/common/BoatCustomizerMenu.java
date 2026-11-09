package gr.daldon.simpleboatengine.common;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
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
    @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
    @Override public boolean stillValid(Player p){
        return table!=null && table.stillValid(p);
    }
}
