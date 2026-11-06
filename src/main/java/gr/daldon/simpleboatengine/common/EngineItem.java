package gr.daldon.simpleboatengine.common;

import gr.daldon.simpleboatengine.client.MotorItemRenderer;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** Renders the same detailed outboard in the inventory and in the world. */
public class EngineItem extends Item {
    public EngineItem(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft game = Minecraft.getInstance();
                    renderer = new MotorItemRenderer(game.getBlockEntityRenderDispatcher(),
                        game.getEntityModels());
                }
                return renderer;
            }
        });
    }
}
