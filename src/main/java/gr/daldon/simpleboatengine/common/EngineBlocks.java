package gr.daldon.simpleboatengine.common;

import gr.daldon.simpleboatengine.SimpleBoatEngine;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Internal, temporary blocks that produce actual vanilla light around a moving boat. */
public final class EngineBlocks {
    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, SimpleBoatEngine.ID);

    public static final RegistryObject<Block> BOAT_LIGHT = BLOCKS.register("boat_light",
        () -> new BoatGlowBlock(BlockBehaviour.Properties.of()
            .noCollission().noOcclusion().noLootTable().replaceable()
            .lightLevel(state -> 14)));

    private EngineBlocks() {}
}
