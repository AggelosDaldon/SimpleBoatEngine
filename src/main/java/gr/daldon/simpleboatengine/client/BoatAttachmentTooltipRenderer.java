package gr.daldon.simpleboatengine.client;

import gr.daldon.simpleboatengine.common.BoatAttachmentTooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;

final class BoatAttachmentTooltipRenderer implements ClientTooltipComponent {
    private static final int CELL = 18;
    private static final Component LABEL = Component.translatable("tooltip.daldonsboatadditions.attachments");
    private final BoatAttachmentTooltip tooltip;

    BoatAttachmentTooltipRenderer(BoatAttachmentTooltip tooltip) {
        this.tooltip = tooltip;
    }

    @Override public int getHeight() {
        return 12 + CELL + 2;
    }

    @Override public int getWidth(Font font) {
        return Math.max(font.width(LABEL), CELL * tooltip.attachments().size());
    }

    @Override public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        graphics.drawString(font, LABEL, x, y, 0xFFD8D8D8, false);
        int top = y + 12;
        for (var stack : tooltip.attachments()) {
            graphics.fill(x, top, x + CELL, top + CELL, 0xFF745D89);
            graphics.fill(x + 1, top + 1, x + CELL - 1, top + CELL - 1, 0xFF392B48);
            graphics.renderFakeItem(stack, x + 1, top + 1);
            x += CELL;
        }
    }
}
