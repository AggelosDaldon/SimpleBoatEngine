package gr.daldon.simpleboatengine.client;

import gr.daldon.simpleboatengine.SimpleBoatEngine;
import gr.daldon.simpleboatengine.common.EngineBlockEntities;
import gr.daldon.simpleboatengine.common.BoatAttachmentTooltip;
import gr.daldon.simpleboatengine.common.EngineMenus;
import gr.daldon.simpleboatengine.common.EngineItems;
import gr.daldon.simpleboatengine.common.BoatTrackerItem;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SimpleBoatEngine.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT)
public final class BoatCustomizerRegistration {
    private BoatCustomizerRegistration() {}

    @SubscribeEvent
    public static void onClient(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(EngineItems.BOAT_TRACKER.get(), new ResourceLocation("angle"),
            new CompassItemPropertyFunction((level, stack, entity) -> BoatTrackerItem.target(stack))));
        event.enqueueWork(() -> MenuScreens.register(EngineMenus.CUSTOMIZER.get(),
            BoatCustomizerScreen::new));
        event.enqueueWork(() -> MenuScreens.register(EngineMenus.CARGO.get(), BoatCargoScreen::new));
        event.enqueueWork(() -> MenuScreens.register(EngineMenus.PARKED_CARGO.get(), ParkedBoatCargoScreen::new));
        event.enqueueWork(() -> MenuScreens.register(EngineMenus.BOAT_BUILDER.get(), BoatBuilderScreen::new));
        event.enqueueWork(() -> MenuScreens.register(EngineMenus.BOAT_RADIO.get(), BoatRadioScreen::new));

    }

    @SubscribeEvent
    public static void registerTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(BoatAttachmentTooltip.class, BoatAttachmentTooltipRenderer::new);
    }

    @SubscribeEvent
    public static void onRegister(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(EngineBlockEntities.CUSTOMIZER.get(),
            BoatCustomizerRenderer::new);
        event.registerBlockEntityRenderer(EngineBlockEntities.BOAT_STAND.get(),
            BoatStandRenderer::new);
    }
}
