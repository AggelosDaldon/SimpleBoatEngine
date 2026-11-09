package gr.daldon.simpleboatengine.common;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.DataSlot;
import java.util.List;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;

public final class BoatCustomizerMenu extends AbstractContainerMenu {
    private final Container container;
    private final BlockPos blockPos;
    private final BoatCustomizerBlockEntity table;
    private final DataSlot selected = DataSlot.standalone();

    // This constructor runs clientside with the BlockPos sent via openScreen.
    public BoatCustomizerMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, data.readBlockPos());
    }

    private BoatCustomizerMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, inventory.player.level().getBlockEntity(pos) instanceof BoatCustomizerBlockEntity be
            ? be : null, pos);
    }

    public BoatCustomizerMenu(int id, Inventory inventory, BoatCustomizerBlockEntity station) {
        this(id, inventory, station, station.getBlockPos());
    }

    private BoatCustomizerMenu(int id, Inventory inventory, BoatCustomizerBlockEntity station, BlockPos pos) {
        super(EngineMenus.CUSTOMIZER.get(), id);
        table = station;
        Container data = station == null ? new SimpleContainer(4) : new SelectedStandContainer(this, station);
        container = data;
        blockPos = pos;
        selected.set(0);
        addDataSlot(selected);
        addSlot(new Slot(data, 0, 35, 46) {
            @Override public boolean mayPlace(ItemStack stack) {
                return standCount()==0 && stack.getItem() instanceof SkiffBoatItem;
            }
            @Override public boolean mayPickup(Player player) {
                return standCount()==0;
            }
            @Override public int getMaxStackSize() { return 1; }
        });
        addSlot(new Slot(data, 1, 124, 46) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof DyeItem || stack.is(net.minecraft.world.item.Items.GLOW_INK_SAC);
            }
        });
        addSlot(new Slot(data, 2, 35, 180) {
            @Override public boolean mayPlace(ItemStack stack) {
                return EngineItems.woodStyleFor(stack) > 0;
            }
        });
        addSlot(new Slot(data, 3, 311, 159) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof DyeItem
                    || stack.is(net.minecraft.world.item.Items.GLOW_INK_SAC);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9,
                    87 + col * 18, 228 + row * 18));
        }
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col, 87 + col * 18, 284));
    }

    public BlockPos getBlockPos() { return blockPos; }
    public List<BlockPos> nearbyStands() { return table==null ? List.of() : table.nearbyStands(); }
    public int standCount() { return nearbyStands().size(); }
    public int standIndex() { return standCount()==0 ? -1 : Math.floorMod(selected.get(),standCount()); }
    public BlockPos selectedStandPos() {
        List<BlockPos> stands=nearbyStands();
        return stands.isEmpty() ? null : stands.get(standIndex());
    }
    public BoatStandBlockEntity selectedStand() {
        BlockPos pos=selectedStandPos();
        return pos!=null && table.getLevel()!=null
            && table.getLevel().getBlockEntity(pos) instanceof BoatStandBlockEntity stand ? stand : null;
    }
    public void selectRelative(int direction) {
        if(standCount()==0) return;
        selected.set(Math.floorMod(standIndex()+Integer.signum(direction),standCount()));
        broadcastChanges();
    }
    public BoatCustomizerBlockEntity table(){return table;}
    public ItemStack boatItem() { return container.getItem(0); }
    public ItemStack dyeItem() { return container.getItem(1); }
    public ItemStack woodItem() { return container.getItem(2); }
    public ItemStack nameDyeItem() { return container.getItem(3); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index == 0 && standCount()>0) return ItemStack.EMPTY;
        if (index < 4) {
            if (!moveItemStackTo(original, 4, 40, true)) return ItemStack.EMPTY;
        } else if (standCount()==0 && original.getItem() instanceof SkiffBoatItem && !slots.get(0).hasItem()) {
            if (!moveItemStackTo(original, 0, 1, false)) return ItemStack.EMPTY;
        } else if (original.getItem() instanceof DyeItem || original.is(net.minecraft.world.item.Items.GLOW_INK_SAC)) {
            if (!moveItemStackTo(original, 1, 2, false)) return ItemStack.EMPTY;
        } else if (EngineItems.woodStyleFor(original) > 0) {
            if (!moveItemStackTo(original, 2, 3, false)) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;
        if (original.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
}
