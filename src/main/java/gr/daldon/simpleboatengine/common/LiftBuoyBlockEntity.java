package gr.daldon.simpleboatengine.common;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.FluidState;

/** One-block vertical lift, synchronized to clients for the visible rope. */
public final class LiftBuoyBlockEntity extends BlockEntity {
    private BlockPos origin;
    private boolean raised;
    private boolean upperWasWater;

    public LiftBuoyBlockEntity(BlockPos pos, BlockState state) {
        super(EngineBlockEntities.LIFT_BUOY.get(), pos, state);
    }
    public boolean isRaised() { return raised; }
    public BlockPos ropeTarget() { return origin; }

    /** The only movable tile entity is Fishermen's Trap, whose saved
     * handler and tickCounter must travel together with the block. */
    private boolean isFishTrap(BlockState state) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id != null && id.getNamespace().equals("fishermens_trap")
            && id.getPath().equals("fishtrap");
    }

    private boolean movable(BlockPos pos) {
        if (level == null || !level.hasChunkAt(pos)) return false;
        BlockState block = level.getBlockState(pos);
        if (isFishTrap(block)) return level.getBlockEntity(pos) != null;
        return !block.isAir() && !block.is(Blocks.BEDROCK) &&
            block.getDestroySpeed(level,pos) >= 0f && !block.hasBlockEntity() &&
            block.getFluidState().isEmpty() &&
            !block.is(EngineBlocks.LIFT_BUOY.get()) &&
            block.isCollisionShapeFullBlock(level, pos);
    }

    /** Transfers a fish trap without triggering its normal inventory drop.
     * Fishermen's Trap calls Containers.dropContents in onRemove, even when
     * blocks are programmatically relocated. Empty the old entity first,
     * then restore the full original NBT at the new location.
     */
    private boolean transfer(BlockPos from, BlockPos to) {
        if (!(level instanceof ServerLevel server) || !server.hasChunkAt(from)
                || !server.hasChunkAt(to) || !replaceable(to) || !movable(from)) return false;
        BlockState original = server.getBlockState(from);
        boolean destinationWet = server.getFluidState(to).is(Fluids.WATER);
        boolean originalWet = original.getFluidState().is(Fluids.WATER);
        boolean trap = isFishTrap(original);
        CompoundTag saved = null;
        BlockEntity source = server.getBlockEntity(from);
        if (trap) {
            if (source == null) return false;
            saved = source.saveWithFullMetadata();
            // This handler has ten slots. Clearing before onRemove prevents
            // its custom onRemove from dropping duplicated bait and fish.
            CompoundTag emptied = saved.copy();
            CompoundTag emptyHandler = new CompoundTag();
            emptyHandler.putInt("Size", 10);
            emptyHandler.put("Items", new ListTag());
            emptied.put("handler", emptyHandler);
            source.load(emptied);
        }
        BlockState fromReplacement = originalWet
            ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
        if (!server.setBlock(from, fromReplacement, 3)) {
            if (trap && source != null) source.load(saved);
            return false;
        }
        BlockState destination = original;
        if (trap && destination.hasProperty(BlockStateProperties.WATERLOGGED)) {
            destination = destination.setValue(BlockStateProperties.WATERLOGGED, destinationWet);
        }
        if (!server.setBlock(to, destination, 3)) {
            // Restore original state and inventory if the destination refuses.
            server.setBlock(from, original, 3);
            if (trap && saved != null && server.getBlockEntity(from) != null)
                server.getBlockEntity(from).load(saved);
            return false;
        }
        if (trap) {
            BlockEntity target = server.getBlockEntity(to);
            if (target == null) {
                server.setBlock(to, destinationWet ? Blocks.WATER.defaultBlockState()
                    : Blocks.AIR.defaultBlockState(), 3);
                server.setBlock(from, original, 3);
                if (server.getBlockEntity(from) != null) server.getBlockEntity(from).load(saved);
                return false;
            }
            saved.putInt("x",to.getX());
            saved.putInt("y",to.getY());
            saved.putInt("z",to.getZ());
            target.load(saved);
            target.setChanged();
            server.sendBlockUpdated(to,destination,destination,3);
        }
        return true;
    }
    private boolean replaceable(BlockPos pos) {
        if (level == null || !level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(Blocks.WATER);
    }
    private void synchronize() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    public boolean toggle() {
        if (!(level instanceof ServerLevel server)) return false;
        BlockPos upper = worldPosition.below();
        if (!raised) {
            if (!replaceable(upper)) return false;
            BlockPos found = origin;
            if (found == null || !movable(found)) {
                found = null;
                for (int depth=2; depth<=32; depth++) {
                    BlockPos candidate=worldPosition.below(depth);
                    if (movable(candidate)) { found=candidate.immutable(); break; }
                }
            }
            if (found == null) return false;
            boolean wet=server.getFluidState(upper).is(Fluids.WATER);
            if (!transfer(found, upper)) return false;
            origin=found;
            upperWasWater=wet;
            raised=true;
            synchronize();
            return true;
        }
        if (origin == null || !movable(upper) || !replaceable(origin)) return false;
        if (!transfer(upper, origin)) return false;
        raised=false;
        synchronize();
        return true;
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (origin!=null) tag.putLong("Origin",origin.asLong());
        tag.putBoolean("Raised",raised);
        tag.putBoolean("UpperWater",upperWasWater);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        origin=tag.contains("Origin") ? BlockPos.of(tag.getLong("Origin")) : null;
        raised=tag.getBoolean("Raised");
        upperWasWater=tag.getBoolean("UpperWater");
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
