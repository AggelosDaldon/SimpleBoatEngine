package gr.daldon.simpleboatengine.common;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Persistent boat and dye slots for the customization workbench. */
public class BoatCustomizerBlockEntity extends BlockEntity implements Container, MenuProvider {
    private ItemStack boat = ItemStack.EMPTY;
    private ItemStack dye = ItemStack.EMPTY;
    private ItemStack wood = ItemStack.EMPTY;
    private ItemStack nameDye = ItemStack.EMPTY;
    private BlockPos selectedStand;


    public BoatCustomizerBlockEntity(BlockPos pos, BlockState state) {
        this(EngineBlockEntities.CUSTOMIZER.get(), pos, state);
    }
    protected BoatCustomizerBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,
                                        BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public java.util.List<BlockPos> nearbyStands() {
        if (level == null) return java.util.List.of();
        java.util.List<BlockPos> found = new java.util.ArrayList<>();
        BlockPos.betweenClosedStream(worldPosition.offset(-10,-10,-10), worldPosition.offset(10,10,10))
            .filter(p -> p.distSqr(worldPosition) <= 100 && level.getBlockEntity(p) instanceof BoatStandBlockEntity)
            .forEach(p -> found.add(p.immutable()));
        found.sort(java.util.Comparator.comparingDouble((BlockPos p) -> p.distSqr(worldPosition))
            .thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getY).thenComparingInt(BlockPos::getZ));
        return found;
    }

    public BlockPos getSelectedStand() {
        var stands = nearbyStands();
        if (stands.isEmpty()) return null;
        return selectedStand != null && stands.contains(selectedStand) ? selectedStand : stands.get(0);
    }

    public int selectionIndex() {
        return nearbyStands().indexOf(getSelectedStand());
    }

    public int selectionCount() { return nearbyStands().size(); }

    public void selectRelative(int delta) {
        var stands = nearbyStands();
        if (stands.isEmpty()) { selectedStand=null; setChanged(); return; }
        int index=stands.indexOf(getSelectedStand());
        selectedStand=stands.get(Math.floorMod(index + Integer.signum(delta), stands.size()));
        setChanged();
    }

    public BoatStandBlockEntity linkedStand() {
        BlockPos pos = getSelectedStand();
        return pos != null && level.getBlockEntity(pos) instanceof BoatStandBlockEntity stand ? stand : null;
    }

    @Override
    public Component getDisplayName() { return Component.translatable("container.simpleboatengine.boat_customizer"); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BoatCustomizerMenu(id, inventory, this);
    }

    @Override public int getContainerSize() { return 4; }
    @Override public boolean isEmpty() { return boat.isEmpty() && dye.isEmpty() && wood.isEmpty() && nameDye.isEmpty(); }
    @Override public ItemStack getItem(int index) {
        return index == 0 ? boat : index == 1 ? dye : index == 2 ? wood : index == 3 ? nameDye : ItemStack.EMPTY;
    }
    @Override public ItemStack removeItem(int index, int count) {
        if (index < 0 || index > 3) return ItemStack.EMPTY;
        ItemStack extracted = (index == 0 ? boat : index == 1 ? dye : index == 2 ? wood : nameDye).split(count);
        if (!extracted.isEmpty()) setChanged();
        return extracted;
    }
    @Override public ItemStack removeItemNoUpdate(int index) {
        if (index < 0 || index > 3) return ItemStack.EMPTY;
        ItemStack old = index == 0 ? boat : index == 1 ? dye : index == 2 ? wood : nameDye;
        if (index == 0) boat = ItemStack.EMPTY;
        else if (index == 1) dye = ItemStack.EMPTY;
        else if (index == 2) wood = ItemStack.EMPTY;
        else nameDye = ItemStack.EMPTY;
        setChanged();
        return old;
    }
    @Override public void setItem(int index, ItemStack stack) {
        if (index == 0) {
            boat = stack.copy();
            if (boat.getCount() > 1) boat.setCount(1);
        } else if (index == 1) {
            dye = stack.copy();
        } else if (index == 2) {
            wood = stack.copy();
        } else if (index == 3) {
            nameDye = stack.copy();
        } else return;
        setChanged();
    }
    @Override public boolean canPlaceItem(int index, ItemStack stack) {
        return index == 0 && stack.getItem() instanceof SkiffBoatItem
            || index == 1 && (stack.getItem() instanceof net.minecraft.world.item.DyeItem
                || stack.is(net.minecraft.world.item.Items.GLOW_INK_SAC))
            || index == 2 && EngineItems.woodStyleFor(stack) > 0
            || index == 3 && (stack.getItem() instanceof net.minecraft.world.item.DyeItem
                || stack.is(net.minecraft.world.item.Items.GLOW_INK_SAC));
    }
    @Override public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
            && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) <= 64;
    }
    @Override public void clearContent() {
        boat = ItemStack.EMPTY;
        dye = ItemStack.EMPTY;
        wood = ItemStack.EMPTY;
        nameDye = ItemStack.EMPTY;
        setChanged();
    }

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
        if (!dye.isEmpty()) tag.put("DyeItem", dye.save(new CompoundTag()));
        if (!wood.isEmpty()) tag.put("WoodItem", wood.save(new CompoundTag()));
        if (!nameDye.isEmpty()) tag.put("NameDyeItem", nameDye.save(new CompoundTag()));
        if (selectedStand != null) tag.putLong("SelectedBoatStand",selectedStand.asLong());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        boat = tag.contains("BoatItem") ? ItemStack.of(tag.getCompound("BoatItem")) : ItemStack.EMPTY;
        dye = tag.contains("DyeItem") ? ItemStack.of(tag.getCompound("DyeItem")) : ItemStack.EMPTY;
        wood = tag.contains("WoodItem") ? ItemStack.of(tag.getCompound("WoodItem")) : ItemStack.EMPTY;
        nameDye = tag.contains("NameDyeItem") ? ItemStack.of(tag.getCompound("NameDyeItem")) : ItemStack.EMPTY;
        selectedStand = tag.contains("SelectedBoatStand") ? BlockPos.of(tag.getLong("SelectedBoatStand")) : null;
    }

    @Override
    public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
