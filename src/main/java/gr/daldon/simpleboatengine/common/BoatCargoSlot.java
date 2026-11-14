package gr.daldon.simpleboatengine.common;

import java.util.function.IntSupplier;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** A visible cargo cell mapped to a physical slot by the server's current scroll row. */
class BoatCargoSlot extends Slot {
    private final IntSupplier offset;

    BoatCargoSlot(Container cargo, int index, int x, int y, IntSupplier offset) {
        super(cargo, index, x, y);
        this.offset = offset;
    }

    private int physicalSlot() {
        return getContainerSlot() + offset.getAsInt();
    }

    @Override public ItemStack getItem() {
        return container.getItem(physicalSlot());
    }

    @Override public void set(ItemStack stack) {
        container.setItem(physicalSlot(), stack);
        setChanged();
    }

    @Override public ItemStack remove(int count) {
        return container.removeItem(physicalSlot(), count);
    }
}
