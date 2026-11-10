package gr.daldon.simpleboatengine.client;

import gr.daldon.simpleboatengine.common.BoatCargoMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** The shared cargo background, grid and scrollbar for live and parked boats. */
abstract class BoatStorageScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private static final int TRACK_X = 175;
    private static final int TRACK_TOP = 42;
    private static final int TRACK_HEIGHT = 53;
    private static final int THUMB_HEIGHT = 11;
    private Button up;
    private Button down;

    BoatStorageScreen(T menu, Inventory inventory, Component title, int width) {
        super(menu, inventory, title);
        imageWidth = width;
        imageHeight = 222;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = 127;
    }

    protected abstract Component storageTitle();
    protected abstract int visibleStorageSlots();
    protected abstract boolean canScrollStorage();
    protected abstract int firstRow();
    protected abstract void requestRow(int row);

    @Override protected void init() {
        super.init();
        up = addRenderableWidget(Button.builder(Component.literal("▲"), button -> scrollTo(firstRow() - 1))
            .bounds(leftPos + 171, topPos + 18, 17, 18).build());
        down = addRenderableWidget(Button.builder(Component.literal("▼"), button -> scrollTo(firstRow() + 1))
            .bounds(leftPos + 171, topPos + 101, 17, 18).build());
    }

    private void scrollTo(int row) {
        int target = Math.max(0, Math.min(BoatCargoMenu.MAX_START_ROW, row));
        if (canScrollStorage() && target != firstRow()) requestRow(target);
    }

    @Override public boolean mouseScrolled(double x, double y, double delta) {
        if (canScrollStorage() && delta != 0 && x >= leftPos && x < leftPos + imageWidth
                && y >= topPos && y < topPos + imageHeight) {
            scrollTo(firstRow() + (delta > 0 ? -1 : 1));
            return true;
        }
        return super.mouseScrolled(x, y, delta);
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        double position = y - topPos - TRACK_TOP;
        if (button == 0 && canScrollStorage() && x >= leftPos + TRACK_X && x < leftPos + TRACK_X + 9
                && position >= 0 && position < TRACK_HEIGHT) {
            scrollTo((int) Math.round((position - THUMB_HEIGHT / 2.0) * BoatCargoMenu.MAX_START_ROW
                / (TRACK_HEIGHT - THUMB_HEIGHT)));
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF24201B);
        graphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + imageHeight - 4, 0xFF9B8268);
        int count = visibleStorageSlots();
        if (count > 0) graphics.fill(leftPos + 5, topPos + 16, leftPos + 170,
            topPos + 17 + (count / 9) * 18, 0xFF5C4A37);
        for (int i = 0; i < count; i++) drawSlot(graphics, i % 9, 17 + (i / 9) * 18);
        for (int i = 0; i < 36; i++) drawSlot(graphics, i % 9, i < 27 ? 139 + (i / 9) * 18 : 197);
        if (canScrollStorage()) {
            int x = leftPos + TRACK_X;
            int y = topPos + TRACK_TOP;
            int thumb = y + firstRow() * (TRACK_HEIGHT - THUMB_HEIGHT) / BoatCargoMenu.MAX_START_ROW;
            graphics.fill(x, y, x + 9, y + TRACK_HEIGHT, 0xFF41362C);
            graphics.fill(x + 1, thumb, x + 8, thumb + THUMB_HEIGHT, 0xFFF1CB7D);
        }
    }

    private void drawSlot(GuiGraphics graphics, int column, int rowY) {
        int x = leftPos + 7 + column * 18;
        int y = topPos + rowY;
        graphics.fill(x, y, x + 18, y + 18, 0xFF241A13);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFFB2A490);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, storageTitle(), titleLabelX, titleLabelY, 0xFFF8EEE0, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFF8EEE0, false);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        up.visible = down.visible = canScrollStorage();
        up.active = firstRow() > 0;
        down.active = firstRow() < BoatCargoMenu.MAX_START_ROW;
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partial);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
