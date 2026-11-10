package gr.daldon.simpleboatengine.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.world.item.ItemStack;

/** Opens the vanilla reader for the mod's own guide item. */
public final class BoatGuideScreen {
    private BoatGuideScreen() {}

    public static void open(ItemStack book) {
        Minecraft.getInstance().setScreen(new BookViewScreen(new BookViewScreen.WrittenBookAccess(book)));
    }
}
