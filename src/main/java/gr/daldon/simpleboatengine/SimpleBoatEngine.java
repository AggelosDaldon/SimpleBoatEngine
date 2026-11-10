package gr.daldon.simpleboatengine;

import gr.daldon.simpleboatengine.common.EngineItems;
import gr.daldon.simpleboatengine.common.EngineBlocks;
import gr.daldon.simpleboatengine.common.EngineSounds;
import gr.daldon.simpleboatengine.common.EngineEntities;
import gr.daldon.simpleboatengine.common.MotorPackets;
import gr.daldon.simpleboatengine.common.EngineRecipes;
import gr.daldon.simpleboatengine.common.EngineBlockEntities;
import gr.daldon.simpleboatengine.common.EngineMenus;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod(SimpleBoatEngine.MOD_ID)
public final class SimpleBoatEngine {
    public static final String MOD_ID = "daldonsboatadditions";
    // Registry namespace for every game object. Old entity NBT keys remain readable.
    public static final String ID = MOD_ID;
    public static final String LEGACY_ID = "simpleboatengine";

    public SimpleBoatEngine() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        EngineBlocks.BLOCKS.register(modBus);
        EngineBlockEntities.TYPES.register(modBus);
        EngineMenus.MENUS.register(modBus);
        EngineEntities.ENTITIES.register(modBus);
        EngineItems.ITEMS.register(modBus);
        EngineItems.TABS.register(modBus);
        EngineSounds.SOUNDS.register(modBus);
        EngineRecipes.RECIPES.register(modBus);
        MotorPackets.initialize();
    }
}
