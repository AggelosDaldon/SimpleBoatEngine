package gr.daldon.simpleboatengine.common;

import gr.daldon.simpleboatengine.SimpleBoatEngine;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class EngineBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SimpleBoatEngine.ID);

    public static final RegistryObject<BlockEntityType<BoatCustomizerBlockEntity>> CUSTOMIZER =
        TYPES.register("boat_customizer", () -> BlockEntityType.Builder.of(
            BoatCustomizerBlockEntity::new, EngineBlocks.CUSTOMIZER.get()).build(null));

    private EngineBlockEntities() {}
}
