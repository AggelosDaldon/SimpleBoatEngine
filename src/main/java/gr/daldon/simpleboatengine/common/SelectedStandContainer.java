package gr.daldon.simpleboatengine.common;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Delegates slots to the painting table's selected nearby boat stand.
 *  If no stand exists, the old station inventory remains accessible for legacy saves.
 */
public final class SelectedStandContainer implements Container {
    private final BoatCustomizerBlockEntity table;
    public SelectedStandContainer(BoatCustomizerBlockEntity table) { this.table=table; }
    private Container target() {
        BoatStandBlockEntity stand=table.linkedStand();
        return stand == null ? table : stand;
    }
    @Override public int getContainerSize(){return 4;}
    @Override public boolean isEmpty(){return target().isEmpty();}
    @Override public ItemStack getItem(int slot){return target().getItem(slot);}
    @Override public ItemStack removeItem(int slot,int amount){return target().removeItem(slot,amount);}
    @Override public ItemStack removeItemNoUpdate(int slot){return target().removeItemNoUpdate(slot);}
    @Override public void setItem(int slot,ItemStack stack){target().setItem(slot,stack);}
    @Override public void setChanged(){target().setChanged();}
    @Override public boolean stillValid(Player player){return table.stillValid(player);}
    @Override public void clearContent(){target().clearContent();}
    @Override public boolean canPlaceItem(int slot,ItemStack item){
        return slot != 0 && target().canPlaceItem(slot,item) || table.linkedStand()==null && target().canPlaceItem(slot,item);
    }
}
