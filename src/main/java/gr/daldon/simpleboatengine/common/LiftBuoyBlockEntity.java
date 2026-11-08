package gr.daldon.simpleboatengine.common;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.registries.ForgeRegistries;

/** Only transfers a single safe block on the same vertical axis. */
public final class LiftBuoyBlockEntity extends BlockEntity {
    private BlockPos linked;
    public LiftBuoyBlockEntity(BlockPos pos, BlockState state) {
        super(EngineBlockEntities.LIFT_BUOY.get(), pos, state);
    }

    private boolean movable(BlockPos source) {
        if (level == null || !level.hasChunkAt(source)) return false;
        BlockState state = level.getBlockState(source);
        return !state.isAir() && !state.is(Blocks.BEDROCK) &&
            state.getDestroySpeed(level, source) >= 0 && !state.hasBlockEntity() &&
            !state.is(Blocks.WATER) && !state.is(Blocks.LAVA) &&
            !state.is(EngineBlocks.LIFT_BUOY.get()) &&
            state.getFluidState().isEmpty();
    }
    public boolean bindClosestUnderneath() {
        if (level == null) return false;
        for (int i = 2; i <= 32; i++) {
            BlockPos found = worldPosition.below(i);
            if (movable(found)) {
                linked = found.immutable();
                setChanged();
                return true;
            }
        }
        return false;
    }
    public boolean raise() {
        if (!(level instanceof ServerLevel server) || linked == null) return false;
        BlockPos dest = worldPosition.below();
        if (linked.getX() != dest.getX() || linked.getZ() != dest.getZ()) return false;
        if (linked.equals(dest)) return true;
        if (!movable(linked) || !server.hasChunkAt(dest)) return false;
        BlockState previous = server.getBlockState(dest);
        if (!previous.canBeReplaced() && !previous.isAir()) return false;
        BlockState moving = server.getBlockState(linked);
        // Reject connected inventories, block entities and multiblock structures.
        // Move only after confirming placement and keep the existing fluid beneath.
        BlockState sourceReplacement = server.getFluidState(linked).is(Fluids.WATER)
            ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
        if (!server.setBlock(dest, moving, 3)) return false;
        if (!server.setBlock(linked, sourceReplacement, 3)) {
            server.setBlock(dest, previous, 3);
            return false;
        }
        linked = dest.immutable();
        setChanged();
        server.playSound(null, worldPosition, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, .7f, 1.3f);
        return true;
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (linked != null) tag.putLong("LinkedBlock", linked.asLong());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        linked = tag.contains("LinkedBlock") ? BlockPos.of(tag.getLong("LinkedBlock")) : null;
    }
}
