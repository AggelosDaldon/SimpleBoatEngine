package gr.daldon.simpleboatengine;

import gr.daldon.simpleboatengine.common.EngineItems;
import gr.daldon.simpleboatengine.common.EngineBlocks;
import gr.daldon.simpleboatengine.common.EngineSounds;
import gr.daldon.simpleboatengine.common.MotorPackets;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod(SimpleBoatEngine.ID)
public final class SimpleBoatEngine {
    public static final String ID = "simpleboatengine";

    public SimpleBoatEngine() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        EngineBlocks.BLOCKS.register(modBus);
        EngineItems.ITEMS.register(modBus);
        EngineItems.TABS.register(modBus);
        EngineSounds.SOUNDS.register(modBus);
        MotorPackets.initialize();
    }
}
