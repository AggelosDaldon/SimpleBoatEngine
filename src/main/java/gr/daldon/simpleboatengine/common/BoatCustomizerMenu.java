package gr.daldon.simpleboatengine.common;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;

public final class BoatCustomizerMenu extends AbstractContainerMenu {
    private final Container container;
    private final BlockPos blockPos;

    // This constructor runs clientside with the BlockPos sent via openScreen.
    public BoatCustomizerMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, data.readBlockPos());
    }

    private BoatCustomizerMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, inventory.player.level().getBlockEntity(pos) instanceof BoatCustomizerBlockEntity be
            ? be : new SimpleContainer(2), pos);
    }

    public BoatCustomizerMenu(int id, Inventory inventory, BoatCustomizerBlockEntity station) {
        this(id, inventory, station, station.getBlockPos());
    }

    private BoatCustomizerMenu(int id, Inventory inventory, Container data, BlockPos pos) {
        super(EngineMenus.CUSTOMIZER.get(), id);
        container = data;
        blockPos = pos;
        addSlot(new Slot(data, 0, 30, 32) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof SkiffBoatItem;
            }
            @Override public int getMaxStackSize() { return 1; }
        });
        addSlot(new Slot(data, 1, 256, 32) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof DyeItem || stack.is(net.minecraft.world.item.Items.GLOW_INK_SAC);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9,
                    65 + col * 18, 235 + row * 18));
        }
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col, 65 + col * 18, 293));
    }

    public BlockPos getBlockPos() { return blockPos; }
    public ItemStack boatItem() { return container.getItem(0); }
    public ItemStack dyeItem() { return container.getItem(1); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index < 2) {
            if (!moveItemStackTo(original, 2, 38, true)) return ItemStack.EMPTY;
        } else if (original.getItem() instanceof SkiffBoatItem && !slots.get(0).hasItem()) {
            if (!moveItemStackTo(original, 0, 1, false)) return ItemStack.EMPTY;
        } else if (original.getItem() instanceof DyeItem || original.is(net.minecraft.world.item.Items.GLOW_INK_SAC)) {
            if (!moveItemStackTo(original, 1, 2, false)) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;
        if (original.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
}
