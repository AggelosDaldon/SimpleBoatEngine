package gr.daldon.simpleboatengine.common;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** A single real inventory slot for the boat being customized. */
public final class BoatCustomizerBlockEntity extends BlockEntity implements Container, MenuProvider {
    private ItemStack boat = ItemStack.EMPTY;

    public BoatCustomizerBlockEntity(BlockPos pos, BlockState state) {
        super(EngineBlockEntities.CUSTOMIZER.get(), pos, state);
    }

    @Override
    public Component getDisplayName() { return Component.translatable("container.simpleboatengine.boat_customizer"); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BoatCustomizerMenu(id, inventory, this);
    }

    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return boat.isEmpty(); }
    @Override public ItemStack getItem(int index) { return index == 0 ? boat : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int index, int count) {
        if (index != 0 || boat.isEmpty()) return ItemStack.EMPTY;
        ItemStack extracted = boat.split(count);
        if (!extracted.isEmpty()) setChanged();
        return extracted;
    }
    @Override public ItemStack removeItemNoUpdate(int index) {
        if (index != 0) return ItemStack.EMPTY;
        ItemStack old = boat;
        boat = ItemStack.EMPTY;
        setChanged();
        return old;
    }
    @Override public void setItem(int index, ItemStack stack) {
        if (index != 0) return;
        boat = stack.copy();
        if (boat.getCount() > 1) boat.setCount(1);
        setChanged();
    }
    @Override public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
            && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) <= 64;
    }
    @Override public void clearContent() { boat = ItemStack.EMPTY; setChanged(); }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!boat.isEmpty()) tag.put("BoatItem", boat.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        boat = tag.contains("BoatItem") ? ItemStack.of(tag.getCompound("BoatItem")) : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
