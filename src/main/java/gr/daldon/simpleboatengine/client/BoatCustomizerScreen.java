package gr.daldon.simpleboatengine.client;

import gr.daldon.simpleboatengine.common.BoatCustomizerMenu;
import gr.daldon.simpleboatengine.common.BoatPaint;
import gr.daldon.simpleboatengine.common.MotorPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;

/** Compact dye-part picker with a real boat slot and persistent custom name. */
public final class BoatCustomizerScreen extends AbstractContainerScreen<BoatCustomizerMenu> {
    private static final String[] PARTS = {
        "Hull", "Floor", "Stripe", "Hatch", "Seats", "Antifouling", "Inner walls"
    };
    private static final String[] ANTIFOULING = {"Teal", "Marine blue", "Green", "Maroon", "Graphite"};
    private static final int[] ANTIFOUL_COLORS = {0x14585D, 0x15345C, 0x314238, 0x522A32, 0x292D35};
    private int selected;
    private EditBox nameInput;
    private String lastName = "";

    public BoatCustomizerScreen(BoatCustomizerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 238;
        imageHeight = 313;
        inventoryLabelY = 210;
        inventoryLabelX = 14;
        titleLabelX = 12;
        titleLabelY = 9;
    }

    @Override
    protected void init() {
        super.init();
        for (int part = 0; part < PARTS.length; part++) {
            final int channel = part;
            addRenderableWidget(Button.builder(Component.literal(PARTS[part]), button ->
                selected = channel).bounds(leftPos + 12, topPos + 53 + part * 17,
                    111, 15).build());
        }
        nameInput = new EditBox(font, leftPos + 14, topPos + 183,
            145, 17, Component.literal("Boat name"));
        nameInput.setMaxLength(20);
        nameInput.setValue(BoatPaint.name(menu.boatItem()));
        lastName = nameInput.getValue();
        addRenderableWidget(nameInput);
        addRenderableWidget(Button.builder(Component.literal("Save name"), button -> {
            if (!menu.boatItem().isEmpty())
                MotorPackets.CHANNEL.sendToServer(new MotorPackets.CustomizeBoat(
                    menu.getBlockPos(), -1, -1, nameInput.getValue()));
        }).bounds(leftPos + 165, topPos + 182, 63, 18).build());
    }

    @Override
    protected void renderBg(GuiGraphics g, float delta, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF17212C);
        g.fill(leftPos + 5, topPos + 5, leftPos + imageWidth - 5,
            topPos + imageHeight - 5, 0xFF36475A);
        g.fill(leftPos + 9, topPos + 26, leftPos + 52, topPos + 48, 0xFF17212C);
        g.fill(leftPos + 27, topPos + 27, leftPos + 51, topPos + 51, 0xFF91A4B7);
        for (int part = 0; part < PARTS.length; part++) {
            if (part == selected) g.fill(leftPos + 9, topPos + 51 + part * 17,
                leftPos + 126, topPos + 69 + part * 17, 0xFF4EA7CF);
        }
        int count = selected == BoatPaint.ANTIFOUL ? 5 : 16;
        for (int dye = 0; dye < count; dye++) {
            int x = leftPos + 140 + dye % 4 * 22;
            int y = topPos + 57 + dye / 4 * 23;
            int color = swatch(dye);
            g.fill(x - 2, y - 2, x + 19, y + 19, 0xFF15202A);
            g.fill(x, y, x + 17, y + 17, 0xFF000000 | color);
            if (BoatPaint.get(menu.boatItem(), selected) == dye) {
                g.fill(x, y - 2, x + 17, y, 0xFFFFFFFF);
                g.fill(x, y + 17, x + 17, y + 19, 0xFFFFFFFF);
            }
        }
        for (int slot = 0; slot < 36; slot++) {
            int x = leftPos + 12 + (slot % 9) * 18;
            int y = topPos + (slot < 27 ? 221 + (slot / 9) * 18 : 279);
            g.fill(x, y, x + 20, y + 20, 0xFF17212C);
            g.fill(x + 1, y + 1, x + 18, y + 18, 0xFF8392A3);
        }
    }

    private int swatch(int index) {
        if (selected == BoatPaint.ANTIFOUL) return ANTIFOUL_COLORS[index];
        float[] rgb = DyeColor.byId(index).getTextureDiffuseColors();
        return (Math.round(rgb[0] * 255) << 16)
            | (Math.round(rgb[1] * 255) << 8) | Math.round(rgb[2] * 255);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0xFFF2F4F8, false);
        g.drawString(font, "Boat", 59, 31, 0xFFE7EEF6, false);
        g.drawString(font, selected == BoatPaint.ANTIFOUL ? "Marine coating (5)" : "Requires dye",
            136, 40, 0xFFF0F5FB, false);
        g.drawString(font, "Name on bow", 14, 171, 0xFFF0F5FB, false);
        g.drawString(font, playerInventoryTitle, 14, 209, 0xFFF0F5FB, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !menu.boatItem().isEmpty()) {
            int dx = (int)mouseX - leftPos - 140;
            int dy = (int)mouseY - topPos - 57;
            if (dx >= 0 && dx < 88 && dy >= 0 && dy < 92) {
                int col = dx / 22, row = dy / 23;
                int color = row * 4 + col;
                if (dx % 22 < 17 && dy % 23 < 17
                    && color < (selected == BoatPaint.ANTIFOUL ? 5 : 16)) {
                    MotorPackets.CHANNEL.sendToServer(new MotorPackets.CustomizeBoat(
                        menu.getBlockPos(), selected, color, ""));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
