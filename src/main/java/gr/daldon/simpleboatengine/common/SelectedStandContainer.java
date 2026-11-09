package gr.daldon.simpleboatengine.common;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** The selected boat is per open menu. Painting materials always stay in the
 *  paint table, so switching stands never moves dye or consumes another boat.
 */
public final class SelectedStandContainer implements Container {
    private final BoatCustomizerMenu menu;
    private final BoatCustomizerBlockEntity table;
    public SelectedStandContainer(BoatCustomizerMenu menu, BoatCustomizerBlockEntity table) {
        this.menu=menu; this.table=table;
    }
    private Container target(int slot) {
        if (slot!=0) return table;
        BoatStandBlockEntity stand=menu.selectedStand();
        return stand==null ? table : stand;
    }
    @Override public int getContainerSize(){return 4;}
    @Override public boolean isEmpty(){
        for(int i=0;i<4;i++) if(!getItem(i).isEmpty()) return false;
        return true;
    }
    @Override public ItemStack getItem(int slot){return target(slot).getItem(slot);}
    @Override public ItemStack removeItem(int slot,int count){return target(slot).removeItem(slot,count);}
    @Override public ItemStack removeItemNoUpdate(int slot){return target(slot).removeItemNoUpdate(slot);}
    @Override public void setItem(int slot,ItemStack stack){target(slot).setItem(slot,stack);}
    @Override public void setChanged(){
        table.setChanged();
        BoatStandBlockEntity stand=menu.selectedStand();
        if(stand!=null) stand.setChanged();
    }
    @Override public boolean stillValid(Player player){return table.stillValid(player);}
    @Override public void clearContent(){for(int i=0;i<4;i++)setItem(i,ItemStack.EMPTY);}
    @Override public boolean canPlaceItem(int slot,ItemStack item){
        if(slot==0 && menu.standCount()>0) return false;
        return target(slot).canPlaceItem(slot,item);
    }
}
