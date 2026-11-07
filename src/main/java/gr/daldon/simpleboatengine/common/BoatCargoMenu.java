package gr.daldon.simpleboatengine.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Nine columns, twelve stored rows; six rows shown at once, scroll in one-row steps. */
public final class BoatCargoMenu extends AbstractContainerMenu {
    private final Container cargo;
    private final BlueFishingBoat boat;
    private final int boatId;
    private int startRow;

    public BoatCargoMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, data.readVarInt(), new SimpleContainer(108), null);
    }

    public BoatCargoMenu(int id, Inventory inventory, BlueFishingBoat boat, Container cargo) {
        this(id, inventory, boat.getId(), cargo, boat);
    }

    private BoatCargoMenu(int id, Inventory inventory, int boatId,
                          Container cargo, BlueFishingBoat boat) {
        super(EngineMenus.CARGO.get(), id);
        this.boatId = boatId;
        this.cargo = cargo;
        this.boat = boat;
        for (int row = 0; row < 6; row++)
            for (int col = 0; col < 9; col++) {
                final int relative = row * 9 + col;
                addSlot(new Slot(cargo, relative, 8 + col * 18, 18 + row * 18) {
                    private int actual() { return relative + startRow * 9; }
                    @Override public ItemStack getItem() { return cargo.getItem(actual()); }
                    @Override public boolean hasItem() { return !getItem().isEmpty(); }
                    @Override public void set(ItemStack stack) { cargo.setItem(actual(), stack); setChanged(); }
                    @Override public ItemStack remove(int amount) {
                        return cargo.removeItem(actual(), amount);
                    }
                });
            }
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9,
                    8 + col * 18, 140 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col, 8 + col * 18, 198));
    }

    public int boatId() { return boatId; }
    public int startRow() { return startRow; }
    public void setStartRow(int row) {
        startRow = Math.max(0, Math.min(6, row));
        broadcastChanges();
    }

    @Override public boolean stillValid(Player player) {
        return boat == null || boat.isAlive() && player.distanceToSqr(boat) <= 64.0;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 54) {
            if (!moveItemStackTo(stack, 54, 90, true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 0, 54, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
