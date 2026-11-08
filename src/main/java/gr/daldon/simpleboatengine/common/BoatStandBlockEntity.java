package gr.daldon.simpleboatengine.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Reuses the exact boat-item customization slots, NBT and GUI used by the workshop. */
public final class BoatStandBlockEntity extends BoatCustomizerBlockEntity {
    public BoatStandBlockEntity(BlockPos pos, BlockState state) {
        super(EngineBlockEntities.BOAT_STAND.get(), pos, state);
    }
}
