package gr.daldon.simpleboatengine.common;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
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

    private boolean movable(BlockPos pos) {
        if (level == null || !level.hasChunkAt(pos)) return false;
        BlockState block = level.getBlockState(pos);
        return !block.isAir() && !block.is(Blocks.BEDROCK) &&
            block.getDestroySpeed(level,pos) >= 0f && !block.hasBlockEntity() &&
            block.getFluidState().isEmpty() &&
            !block.is(EngineBlocks.LIFT_BUOY.get()) &&
            block.isCollisionShapeFullBlock(level, pos);
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
            BlockState state=server.getBlockState(found);
            boolean wet=server.getFluidState(upper).is(Fluids.WATER);
            if (!server.setBlock(upper,state,3)) return false;
            if (!server.setBlock(found,Blocks.AIR.defaultBlockState(),3)) {
                server.setBlock(upper,wet ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(),3);
                return false;
            }
            origin=found;
            upperWasWater=wet;
            raised=true;
            synchronize();
            return true;
        }
        if (origin == null || !movable(upper) || !replaceable(origin)) return false;
        BlockState state=server.getBlockState(upper);
        boolean originWet=server.getFluidState(origin).is(Fluids.WATER);
        if (!server.setBlock(origin,state,3)) return false;
        if (!server.setBlock(upper,upperWasWater ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(),3)) {
            server.setBlock(origin,originWet ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(),3);
            return false;
        }
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
