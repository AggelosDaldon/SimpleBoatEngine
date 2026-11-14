package gr.daldon.simpleboatengine.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Live cargo window for the 27-slot hatch or the scrollable 108-slot main hold. */
public final class BoatCargoMenu extends AbstractContainerMenu {
    public static final int COLUMNS = 9;
    public static final int MAIN_ROWS_VISIBLE = 6;
    public static final int MAX_START_ROW = 6;
    public static final String HATCH_TITLE = "Bow Hatch Storage";
    public static final String MAIN_TITLE = "Main Storage";

    private final BlueFishingBoat boat;
    private final int boatId;
    private final int storageSlots;
    private final boolean hatch;
    private final DataSlot startRow = DataSlot.standalone();

    public BoatCargoMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, data.readVarInt(), data.readBoolean(), null, null);
    }

    public BoatCargoMenu(int id, Inventory inventory, BlueFishingBoat boat, Container cargo, boolean hatch) {
        this(id, inventory, boat.getId(), hatch, cargo, boat);
    }

    private BoatCargoMenu(int id, Inventory inventory, int boatId, boolean hatch,
                          Container cargo, BlueFishingBoat boat) {
        super(EngineMenus.CARGO.get(), id);
        this.boatId = boatId;
        this.boat = boat;
        this.hatch = hatch;
        storageSlots = hatch ? 27 : COLUMNS * MAIN_ROWS_VISIBLE;
        addDataSlot(startRow);
        // Clients hold only the visible window. The server maps it onto the full hold.
        Container window = cargo == null ? new SimpleContainer(storageSlots) : cargo;
        for (int i = 0; i < storageSlots; i++) {
            addSlot(new BoatCargoSlot(window, i, 8 + (i % COLUMNS) * 18, 18 + (i / COLUMNS) * 18,
                () -> boat == null ? 0 : startRow() * COLUMNS));
        }
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < COLUMNS; col++)
                addSlot(new Slot(inventory, col + row * COLUMNS + 9, 8 + col * 18, 140 + row * 18));
        for (int col = 0; col < COLUMNS; col++)
            addSlot(new Slot(inventory, col, 8 + col * 18, 198));
    }

    public int boatId() { return boatId; }
    public int storageSlots() { return storageSlots; }
    public boolean isHatch() { return hatch; }
    public int startRow() { return startRow.get(); }
    public void setStartRow(int row) {
        if (hatch) return;
        startRow.set(Math.max(0, Math.min(MAX_START_ROW, row)));
        broadcastChanges();
    }

    @Override public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && boat != null)
            player.level().playSound(null, boat.blockPosition(),
                net.minecraft.sounds.SoundEvents.BARREL_CLOSE,
                net.minecraft.sounds.SoundSource.BLOCKS, .8f, .95f);
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
        if (index < storageSlots) {
            if (!moveItemStackTo(stack, storageSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 0, storageSlots, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
