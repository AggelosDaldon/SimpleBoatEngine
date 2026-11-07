package gr.daldon.simpleboatengine.client;

import gr.daldon.simpleboatengine.common.BoatCargoMenu;
import gr.daldon.simpleboatengine.common.MotorPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Scrollable 108-slot boat cargo, with 54 slots visible at once. */
public final class BoatCargoScreen extends AbstractContainerScreen<BoatCargoMenu> {
    public BoatCargoScreen(BoatCargoMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 192;
        imageHeight = 222;
        titleLabelX = 8; titleLabelY = 6;
        inventoryLabelX = 8; inventoryLabelY = 127;
    }
    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("▲"), b ->
            scrollTo(menu.startRow() - 1)).bounds(leftPos + 171, topPos + 18, 17, 18).build());
        addRenderableWidget(Button.builder(Component.literal("▼"), b ->
            scrollTo(menu.startRow() + 1)).bounds(leftPos + 171, topPos + 101, 17, 18).build());
    }
    private void scrollTo(int row) {
        row = Math.max(0, Math.min(6, row));
        if (row == menu.startRow()) return;
        menu.setStartRow(row);
        MotorPackets.CHANNEL.sendToServer(new MotorPackets.ScrollCargo(menu.boatId(), row));
    }
    @Override public boolean mouseScrolled(double mx, double my, double delta) {
        if (delta != 0) {
            scrollTo(menu.startRow() + (delta > 0 ? -1 : 1));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }
    @Override protected void renderBg(GuiGraphics g, float tick, int mx, int my) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF24201B);
        g.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4,
            topPos + imageHeight - 4, 0xFF9B8268);
        g.fill(leftPos + 5, topPos + 16, leftPos + 168, topPos + 124, 0xFF5C4A37);
        for (int i = 0; i < 90; i++) {
            int col = i < 54 ? i % 9 : (i - 54) % 9;
            int row = i < 54 ? i / 9 : (i < 81 ? (i - 54) / 9 : 3);
            int x = leftPos + 7 + col * 18;
            int y = topPos + (i < 54 ? 17 + row * 18 : i < 81 ? 139 + row * 18 : 197);
            g.fill(x, y, x + 18, y + 18, 0xFF241A13);
            g.fill(x + 1, y + 1, x + 17, y + 17, 0xFFB2A490);
        }
        g.fill(leftPos + 175, topPos + 42, leftPos + 184, topPos + 95, 0xFF41362C);
        int thumb = topPos + 42 + menu.startRow() * 7;
        g.fill(leftPos + 176, thumb, leftPos + 183, thumb + 11, 0xFFF1CB7D);
    }
    @Override protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0xFFF8EEE0, false);
        g.drawString(font, "Rows " + (menu.startRow() + 1) + "-"+(menu.startRow()+6)+"/12",
            98, 6, 0xFFF8EEE0, false);
        g.drawString(font, playerInventoryTitle, 8, 127, 0xFFF8EEE0, false);
    }
    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        renderBackground(g);
        super.render(g, mx, my, delta);
        renderTooltip(g, mx, my);
    }
}
