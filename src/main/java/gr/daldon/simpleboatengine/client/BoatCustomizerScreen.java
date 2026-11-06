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

/** A real boat slot and dye slot: each paint operation consumes the placed dye. */
public final class BoatCustomizerScreen extends AbstractContainerScreen<BoatCustomizerMenu> {
    private static final String[] PARTS = {
        "Hull", "Floor", "Stripe", "Hatch", "Seats", "Antifouling", "Inner walls"
    };
    private static final int[] ANTIFOUL_COLORS = {0x14585D, 0x15345C, 0x314238, 0x522A32, 0x292D35};
    private int selected;
    private int marineColor;
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
                selected = channel).bounds(leftPos + 12, topPos + 54 + part * 17,
                    111, 15).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Apply dye"), button -> {
            MotorPackets.CHANNEL.sendToServer(new MotorPackets.CustomizeBoat(
                menu.getBlockPos(), selected,
                selected == BoatPaint.ANTIFOUL ? marineColor : -1, ""));
        }).bounds(leftPos + 139, topPos + 67, 87, 19).build());
        nameInput = new EditBox(font, leftPos + 14, topPos + 183,
            145, 17, Component.literal("Boat name"));
        nameInput.setMaxLength(20);
        nameInput.setValue(BoatPaint.name(menu.boatItem()));
        nameInput.setHint(Component.literal("Type your boat name"));
        lastName = nameInput.getValue();
        addRenderableWidget(nameInput);
        addRenderableWidget(Button.builder(Component.literal("Save name"), button ->
            saveBoatName()).bounds(leftPos + 165, topPos + 182, 63, 18).build());
    }

    private void saveBoatName() {
        if (nameInput != null) MotorPackets.CHANNEL.sendToServer(new MotorPackets.CustomizeBoat(
            menu.getBlockPos(), -1, -1, nameInput.getValue()));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameInput != null && nameInput.isFocused() && (keyCode == 257 || keyCode == 335)) {
            saveBoatName();
            nameInput.setFocused(false);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics g, float delta, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF2B2320);
        g.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4,
            topPos + imageHeight - 4, 0xFF99785C);
        g.fill(leftPos + 8, topPos + 23, leftPos + imageWidth - 8,
            topPos + 51, 0xFF4C3E35);
        g.fill(leftPos + 8, topPos + 52, leftPos + imageWidth - 8,
            topPos + 173, 0xFF5D4B3F);
        g.fill(leftPos + 8, topPos + 174, leftPos + imageWidth - 8,
            topPos + 203, 0xFF4C3E35);
        g.fill(leftPos + 8, topPos + 205, leftPos + imageWidth - 8,
            topPos + imageHeight - 6, 0xFF5D4B3F);
        // Actual container slots (rather than decorative fake inventory cells).
        for (int x : new int[]{30, 166}) {
            g.fill(leftPos + x - 2, topPos + 28, leftPos + x + 18,
                topPos + 48, 0xFF1D1716);
            g.fill(leftPos + x - 1, topPos + 29, leftPos + x + 17,
                topPos + 47, 0xFFB8AA96);
        }
        for (int part = 0; part < PARTS.length; part++) {
            if (part == selected) g.fill(leftPos + 9, topPos + 53 + part * 17,
                leftPos + 126, topPos + 69 + part * 17, 0xFFB18D4F);
        }
        if (selected == BoatPaint.ANTIFOUL) {
            for (int dye = 0; dye < 5; dye++) {
                int x = leftPos + 139 + dye % 4 * 22;
                int y = topPos + 107 + dye / 4 * 24;
                g.fill(x - 2, y - 2, x + 19, y + 19,
                    marineColor == dye ? 0xFFFFFFFF : 0xFF211A15);
                g.fill(x, y, x + 17, y + 17, 0xFF000000 | ANTIFOUL_COLORS[dye]);
            }
        } else if (!menu.dyeItem().isEmpty()
                   && menu.dyeItem().getItem() instanceof net.minecraft.world.item.DyeItem dyeItem) {
            float[] rgb = dyeItem.getDyeColor().getTextureDiffuseColors();
            int color = (Math.round(rgb[0] * 255) << 16)
                | (Math.round(rgb[1] * 255) << 8) | Math.round(rgb[2] * 255);
            g.fill(leftPos + 154, topPos + 108,
                leftPos + 209, topPos + 130, 0xFF1D1716);
            g.fill(leftPos + 157, topPos + 111,
                leftPos + 206, topPos + 127, 0xFF000000 | color);
        }
        for (int slot = 0; slot < 36; slot++) {
            int x = leftPos + 13 + (slot % 9) * 18;
            int y = topPos + (slot < 27 ? 222 + (slot / 9) * 18 : 280);
            g.fill(x, y, x + 19, y + 19, 0xFF271F1B);
            g.fill(x + 1, y + 1, x + 17, y + 17, 0xFFB8AA96);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0xFFF9E5CA, false);
        g.drawString(font, "Boat", 53, 32, 0xFFF5EDE1, false);
        g.drawString(font, "Dye", 188, 32, 0xFFF5EDE1, false);
        g.drawString(font, selected == BoatPaint.ANTIFOUL ? "Marine paints" : "Use dye in slot",
            138, 95, 0xFFF5EDE1, false);
        g.drawString(font, "Name on bow", 14, 172, 0xFFF5EDE1, false);
        g.drawString(font, playerInventoryTitle, 14, 209, 0xFFF5EDE1, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && selected == BoatPaint.ANTIFOUL) {
            int dx = (int)mouseX - leftPos - 139;
            int dy = (int)mouseY - topPos - 107;
            if (dx >= 0 && dx < 88 && dy >= 0 && dy < 48) {
                int color = (dy / 24) * 4 + dx / 22;
                if (dx % 22 < 17 && dy % 24 < 17 && color < 5) {
                    marineColor = color;
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

    @Override
    protected void containerTick() {
        super.containerTick();
        if (nameInput == null || menu.boatItem().isEmpty()) return;
        String serverName = BoatPaint.name(menu.boatItem());
        if (!serverName.equals(lastName)) {
            // A name arriving from the server must not clobber active typing.
            if (!nameInput.isFocused() || nameInput.getValue().equals(lastName))
                nameInput.setValue(serverName);
            lastName = serverName;
        }
    }
}
