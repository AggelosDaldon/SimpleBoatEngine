package gr.daldon.simpleboatengine.client;

import gr.daldon.simpleboatengine.common.BoatCargoMenu;
import gr.daldon.simpleboatengine.common.MotorPackets;
import gr.daldon.simpleboatengine.common.ParkedBoatCargoMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ParkedBoatCargoScreen extends BoatStorageScreen<ParkedBoatCargoMenu> {
    private static final String[] TABS = {BoatCargoMenu.HATCH_TITLE, BoatCargoMenu.MAIN_TITLE, "Radio Inventory"};

    public ParkedBoatCargoScreen(ParkedBoatCargoMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 306);
    }

    @Override protected void init() {
        super.init();
        for (int i = 0; i < TABS.length; i++) {
            int tab = i;
            addRenderableWidget(Button.builder(Component.literal(TABS[i]), button -> select(tab, firstRow()))
                .bounds(leftPos + 194, topPos + 18 + i * 23, 104, 20).build());
        }
    }

    private void select(int page, int row) {
        MotorPackets.CHANNEL.sendToServer(new MotorPackets.SelectParkedCargo(menu.standPos(), page, row));
    }

    @Override protected Component storageTitle() { return Component.literal(TABS[menu.page()]); }
    @Override protected int visibleStorageSlots() {
        return switch (menu.page()) {
            case ParkedBoatCargoMenu.MAIN_PAGE -> 54;
            case ParkedBoatCargoMenu.RADIO_PAGE -> menu.hasRadio() ? 9 : 0;
            default -> 27;
        };
    }
    @Override protected boolean canScrollStorage() { return menu.page() == ParkedBoatCargoMenu.MAIN_PAGE; }
    @Override protected int firstRow() { return menu.startRow(); }
    @Override protected void requestRow(int row) { select(ParkedBoatCargoMenu.MAIN_PAGE, row); }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        if (menu.page() == ParkedBoatCargoMenu.RADIO_PAGE && !menu.hasRadio())
            graphics.drawString(font, "No radio installed", 8, 22, 0xFFF8EEE0, false);
    }
}
