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

    public static final RegistryObject<BlockEntityType<LiftBuoyBlockEntity>> LIFT_BUOY =
        TYPES.register("lift_buoy", () -> BlockEntityType.Builder.of(
            LiftBuoyBlockEntity::new, EngineBlocks.LIFT_BUOY.get()).build(null));

    public static final RegistryObject<BlockEntityType<BoatStandBlockEntity>> BOAT_STAND =
        TYPES.register("boat_stand", () -> BlockEntityType.Builder.of(
            BoatStandBlockEntity::new, EngineBlocks.BOAT_STAND.get()).build(null));

    public static final RegistryObject<BlockEntityType<BoatRecoveryBlockEntity>> BOAT_RECOVERY =
        TYPES.register("boat_recovery",() -> BlockEntityType.Builder.of(
            BoatRecoveryBlockEntity::new,EngineBlocks.BOAT_RECOVERY.get()).build(null));

    private EngineBlockEntities() {}
}
