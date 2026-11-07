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

    public static final RegistryObject<EntityType<BlueFishingBoat>> BLUE_FISHING_BOAT =
        ENTITIES.register("aggelos_blue_fishing_boat", () ->
            EntityType.Builder.<BlueFishingBoat>of(BlueFishingBoat::new, MobCategory.MISC)
                .sized(2.2f, 0.39f)
                .clientTrackingRange(10)
                .updateInterval(3)
                .build(SimpleBoatEngine.ID + ":aggelos_blue_fishing_boat"));

    public static final RegistryObject<EntityType<BoatTrailer>> BOAT_TRAILER =
        ENTITIES.register("boat_trailer", () ->
            EntityType.Builder.<BoatTrailer>of(BoatTrailer::new, MobCategory.MISC)
                .sized(2.75f, 1.05f)
                .clientTrackingRange(12).updateInterval(2)
                .build(SimpleBoatEngine.ID + ":boat_trailer"));

    private EngineEntities() {}
}
