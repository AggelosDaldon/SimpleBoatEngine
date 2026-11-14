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

/** Three tabs backed by the parked boat's hatch, full main hold and radio. */
public final class ParkedBoatCargoMenu extends AbstractContainerMenu {
    public static final int HATCH_PAGE = 0;
    public static final int MAIN_PAGE = 1;
    public static final int RADIO_PAGE = 2;
    private static final int HATCH_SLOTS = 27;
    private static final int MAIN_VISIBLE = BoatCargoMenu.COLUMNS * BoatCargoMenu.MAIN_ROWS_VISIBLE;
    private static final int RADIO_FIRST = HATCH_SLOTS + MAIN_VISIBLE;
    private static final int PLAYER_FIRST = RADIO_FIRST + ParkedBoatInventory.RADIO_SLOTS;

    private final BlockPos standPos;
    private final Container cargo;
    private final DataSlot page = DataSlot.standalone();
    private final DataSlot startRow = DataSlot.standalone();
    private final DataSlot installedRadio = DataSlot.standalone();

    public ParkedBoatCargoMenu(int id, Inventory inv, FriendlyByteBuf packet) {
        this(id, inv, packet.readBlockPos(),
            new SimpleContainer(ParkedBoatInventory.CARGO_SLOTS + ParkedBoatInventory.RADIO_SLOTS));
    }

    public ParkedBoatCargoMenu(int id, Inventory inv, BoatStandBlockEntity stand,
                               BoatCustomizerBlockEntity workbench) {
        this(id, inv, stand.getBlockPos(), new ParkedBoatInventory(stand, workbench));
    }

    private ParkedBoatCargoMenu(int id, Inventory inv, BlockPos pos, Container source) {
        super(EngineMenus.PARKED_CARGO.get(), id);
        standPos = pos;
        cargo = source;
        addDataSlot(page);
        addDataSlot(startRow);
        addDataSlot(installedRadio);

        addCargoSlots(HATCH_PAGE, 0, HATCH_SLOTS);
        addCargoSlots(MAIN_PAGE, HATCH_SLOTS, MAIN_VISIBLE);
        for (int i = 0; i < ParkedBoatInventory.RADIO_SLOTS; i++) {
            addSlot(new Slot(source, ParkedBoatInventory.CARGO_SLOTS + i, 8 + i * 18, 18) {
                @Override public boolean isActive() { return page() == RADIO_PAGE && hasRadio(); }
                @Override public boolean mayPlace(ItemStack stack) {
                    return isActive() && EtchedSupport.isPlayable(stack);
                }
                @Override public int getMaxStackSize() { return 1; }
                @Override public boolean mayPickup(Player player) {
                    return isActive() && stillValid(player);
                }
            });
        }
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < BoatCargoMenu.COLUMNS; col++)
                addSlot(new Slot(inv, col + row * BoatCargoMenu.COLUMNS + 9, 8 + col * 18, 140 + row * 18));
        for (int col = 0; col < BoatCargoMenu.COLUMNS; col++)
            addSlot(new Slot(inv, col, 8 + col * 18, 198));
    }

    private void addCargoSlots(int tab, int first, int count) {
        for (int i = 0; i < count; i++) {
            int x = 8 + (i % BoatCargoMenu.COLUMNS) * 18;
            int y = 18 + (i / BoatCargoMenu.COLUMNS) * 18;
            addSlot(new BoatCargoSlot(cargo, first + i, x, y,
                    () -> tab == MAIN_PAGE && cargo instanceof ParkedBoatInventory
                        ? startRow() * BoatCargoMenu.COLUMNS : 0) {
                @Override public boolean isActive() { return page() == tab; }
                @Override public boolean mayPlace(ItemStack stack) { return isActive(); }
                @Override public boolean mayPickup(Player player) { return isActive() && stillValid(player); }
            });
        }
    }

    public int page() { return page.get(); }
    public int startRow() { return startRow.get(); }
    public boolean hasRadio() {
        return cargo instanceof ParkedBoatInventory parked ? parked.hasRadio() : installedRadio.get() != 0;
    }
    @Override public void broadcastChanges() {
        if (cargo instanceof ParkedBoatInventory parked) installedRadio.set(parked.hasRadio() ? 1 : 0);
        super.broadcastChanges();
    }
    public void selectPage(int selected, int row) {
        if (selected < HATCH_PAGE || selected > RADIO_PAGE) return;
        page.set(selected);
        if (selected == MAIN_PAGE) startRow.set(Math.max(0, Math.min(BoatCargoMenu.MAX_START_ROW, row)));
        broadcastChanges();
    }
    public BlockPos standPos() { return standPos; }
    @Override public boolean stillValid(Player player) { return cargo.stillValid(player); }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.isActive()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index < PLAYER_FIRST) {
            if (!moveItemStackTo(stack, PLAYER_FIRST, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (page() == RADIO_PAGE && (!hasRadio() || !EtchedSupport.isPlayable(stack))) return ItemStack.EMPTY;
            int start = switch (page()) {
                case MAIN_PAGE -> HATCH_SLOTS;
                case RADIO_PAGE -> RADIO_FIRST;
                default -> 0;
            };
            int end = switch (page()) {
                case MAIN_PAGE -> RADIO_FIRST;
                case RADIO_PAGE -> PLAYER_FIRST;
                default -> HATCH_SLOTS;
            };
            if (!moveItemStackTo(stack, start, end, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
