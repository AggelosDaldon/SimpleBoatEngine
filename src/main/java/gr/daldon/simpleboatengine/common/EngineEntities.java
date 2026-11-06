package gr.daldon.simpleboatengine.common;

import gr.daldon.simpleboatengine.SimpleBoatEngine;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class EngineEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SimpleBoatEngine.ID);

    public static final RegistryObject<EntityType<SkiffBoat>> LARGE_SKIFF =
        ENTITIES.register("large_fishing_skiff", () ->
            EntityType.Builder.<SkiffBoat>of(SkiffBoat::new, MobCategory.MISC)
                // Match the deck's top surface, rather than the gunwale.
                .sized(2.2f, 0.39f)
                .clientTrackingRange(10)
                .updateInterval(3)
                .build(SimpleBoatEngine.ID + ":large_fishing_skiff"));

    private EngineEntities() {}
}
