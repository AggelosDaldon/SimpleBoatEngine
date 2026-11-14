package gr.daldon.simpleboatengine.client;

import gr.daldon.simpleboatengine.common.BoatRadioMenu;
import gr.daldon.simpleboatengine.common.BoatRadioTrackNames;
import gr.daldon.simpleboatengine.common.MotorPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class BoatRadioScreen extends AbstractContainerScreen<BoatRadioMenu> {
    private static final int PLAYLIST_X = 113;
    private static final int TRACK_Y = 35;
    private static final int ROW_HEIGHT = 11;
    private static final int BUTTON_Y = 161;
    private static final int BUTTON_WIDTH = 88;
    private static final int BUTTON_GAP = 12;
    private static final String[] BUTTONS = {"PLAY", "STOP", "NEXT"};
    private static final int[] BUTTON_COLORS = {0xFF376B52, 0xFF85424A, 0xFF315F88};
    private int selected;
    private float scale;
    private float shiftX;
    private float shiftY;

    public BoatRadioScreen(BoatRadioMenu menu, Inventory player, Component title) {
        super(menu, player, title);
        imageWidth = 304;
        imageHeight = 283;
    }

    @Override protected void init() {
        super.init();
        scale = Math.min(1f, Math.min((width - 8f) / imageWidth, (height - 8f) / imageHeight));
        shiftX = (width - imageWidth * scale) / 2f - leftPos * scale;
        shiftY = (height - imageHeight * scale) / 2f - topPos * scale;
    }

    private static void panel(GuiGraphics g, int x, int y, int width, int height, int color) {
        g.fill(x, y, x + width, y + height, 0xFF111A27);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, color);
    }

    @Override protected void renderBg(GuiGraphics g, float tick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        g.fillGradient(x, y, x + imageWidth, y + imageHeight, 0xFF34485B, 0xFF1B2634);
        panel(g, x + 8, y + 19, 96, 118, 0xFF514339);
        panel(g, x + PLAYLIST_X, y + 19, 183, 118, 0xFF223344);
        for (int i = 0; i < 9; i++) {
            int sx = x + BoatRadioMenu.recordX(i);
            int sy = y + BoatRadioMenu.recordY(i);
            panel(g, sx - 1, sy - 1, 18, 18, 0xFFB19669);
            if (i == selected)
                g.fill(x + PLAYLIST_X + 3, y + TRACK_Y - 1 + i * ROW_HEIGHT,
                    x + 293, y + TRACK_Y + 9 + i * ROW_HEIGHT, 0xFF365570);
        }
        panel(g, x + 8, y + 141, 288, 15, 0xFF202F3E);
        for (int i = 0; i < BUTTONS.length; i++) {
            int bx = x + buttonX(i);
            panel(g, bx, y + BUTTON_Y, BUTTON_WIDTH, 19, BUTTON_COLORS[i]);
            g.drawCenteredString(font, BUTTONS[i], bx + BUTTON_WIDTH / 2, y + BUTTON_Y + 6, 0xFFF9F6EC);
        }
        panel(g, x + 63, y + 185, 178, 92, 0xFF304256);
        for (int i = 0; i < 36; i++) {
            int sx = x + BoatRadioMenu.inventoryX(i % 9);
            int sy = y + BoatRadioMenu.inventoryY(i / 9);
            panel(g, sx - 1, sy - 1, 18, 18, 0xFF748394);
        }
    }

    private static int buttonX(int index) {
        return 8 + index * (BUTTON_WIDTH + BUTTON_GAP);
    }

    private String track(int index) {
        return menu.disc(index).isEmpty() ? "— Empty —" : BoatRadioTrackNames.track(menu.disc(index));
    }

    private void fittedText(GuiGraphics g, String text, int x, int y, int maxWidth, int color) {
        float fit = Math.min(1f, maxWidth / (float) Math.max(1, font.width(text)));
        g.pose().pushPose();
        g.pose().translate(x, y + (1f - fit) * 4f, 0f);
        g.pose().scale(fit, fit, 1f);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, "BOAT RADIO", 9, 6, 0xFFFFDE9E, false);
        g.drawString(font, "RECORDS", 17, 23, 0xFFFFDE9E, false);
        g.drawString(font, "PLAYLIST", 121, 23, 0xFFCFEAFF, false);
        for (int i = 0; i < 9; i++) {
            int color = i + 1 == menu.current() ? 0xFF9FE8AE : 0xFFEDF4F9;
            fittedText(g, (i + 1) + ". " + track(i), 120, TRACK_Y + i * ROW_HEIGHT, 170, color);
        }
        if (menu.current() > 0) {
            fittedText(g, "Now playing  " + track(menu.current() - 1), 14, 145, 275, 0xFF82E8D3);
        }
        g.drawString(font, "PLAYER INVENTORY", 71, 189, 0xFFE8F1F8, false);
    }

    private double unscaleX(double x) { return (x - shiftX) / scale; }
    private double unscaleY(double y) { return (y - shiftY) / scale; }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        double unscaledX = unscaleX(mx);
        double unscaledY = unscaleY(my);
        double x = unscaledX - leftPos;
        double y = unscaledY - topPos;
        if (button == 0) {
            if (x >= PLAYLIST_X && x < 296 && y >= TRACK_Y && y < TRACK_Y + 9 * ROW_HEIGHT) {
                selected = (int) (y - TRACK_Y) / ROW_HEIGHT;
                return true;
            }
            for (int i = 0; i < BUTTONS.length; i++) {
                if (y >= BUTTON_Y && y < BUTTON_Y + 19 && x >= buttonX(i) && x < buttonX(i) + BUTTON_WIDTH) {
                    MotorPackets.CHANNEL.sendToServer(new MotorPackets.RadioControl(menu.boatId(), i, selected));
                    return true;
                }
            }
        }
        return super.mouseClicked(unscaledX, unscaledY, button);
    }

    @Override public boolean mouseReleased(double x, double y, int button) {
        return super.mouseReleased(unscaleX(x), unscaleY(y), button);
    }

    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return super.mouseDragged(unscaleX(x), unscaleY(y), button, dx / scale, dy / scale);
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        g.pose().pushPose();
        g.pose().translate(shiftX, shiftY, 0f);
        g.pose().scale(scale, scale, 1f);
        int x = (int) unscaleX(mouseX);
        int y = (int) unscaleY(mouseY);
        super.render(g, x, y, partial);
        renderTooltip(g, x, y);
        g.pose().popPose();
    }
}
