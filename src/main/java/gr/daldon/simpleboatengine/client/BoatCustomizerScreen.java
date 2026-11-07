package gr.daldon.simpleboatengine.client;

import com.mojang.math.Axis;
import gr.daldon.simpleboatengine.common.BoatCustomizerMenu;
import gr.daldon.simpleboatengine.common.BoatPaint;
import gr.daldon.simpleboatengine.common.MotorPackets;
import gr.daldon.simpleboatengine.common.SkiffBoatItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeItem;

/** Compact workbench GUI: the selection controls its 3D preview and dye application. */
public final class BoatCustomizerScreen extends AbstractContainerScreen<BoatCustomizerMenu> {
    private static final String[] PARTS = {
        "Hull", "Floor", "Stripe", "Hatch", "Seats", "Antifouling", "Inner walls"
    };
    private static final int[] MARINE = {0x14585D, 0x15345C, 0x314238, 0x522A32, 0x292D35};
    private int selected, marineColor;
    private EditBox nameInput;
    private String lastName = "";

    public BoatCustomizerScreen(BoatCustomizerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 294;
        imageHeight = 267;
        titleLabelX = 11;
        titleLabelY = 8;
        inventoryLabelX = 65;
        inventoryLabelY = 172;
    }

    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("< Prev"), b ->
            selected = (selected + PARTS.length - 1) % PARTS.length
        ).bounds(leftPos + 12, topPos + 56, 59, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Next >"), b ->
            selected = (selected + 1) % PARTS.length
        ).bounds(leftPos + 76, topPos + 56, 59, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Apply dye"), b ->
            send(selected, selected == BoatPaint.ANTIFOUL ? marineColor : -1, "")
        ).bounds(leftPos + 12, topPos + 108, 123, 19).build());

        nameInput = new EditBox(font, leftPos + 12, topPos + 135,
            123, 17, Component.literal("Boat name"));
        nameInput.setMaxLength(20);
        nameInput.setHint(Component.literal("Name on bow"));
        nameInput.setValue(BoatPaint.name(menu.boatItem()));
        lastName = nameInput.getValue();
        addRenderableWidget(nameInput);
        addRenderableWidget(Button.builder(Component.literal("Save name"), b ->
            saveName()).bounds(leftPos + 141, topPos + 135, 74, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Name dye"), b ->
            send(-2, -1, "")).bounds(leftPos + 12, topPos + 154, 80, 17).build());
        addRenderableWidget(Button.builder(Component.literal("Glow ink"), b ->
            send(-3, -1, "")).bounds(leftPos + 96, topPos + 154, 80, 17).build());
    }

    private void send(int channel, int color, String name) {
        MotorPackets.CHANNEL.sendToServer(new MotorPackets.CustomizeBoat(
            menu.getBlockPos(), channel, color, name));
    }
    private void saveName() {
        if (nameInput != null) send(-1, -1, nameInput.getValue());
    }
    @Override public boolean keyPressed(int code, int scan, int mods) {
        if (nameInput != null && nameInput.isFocused() && (code == 257 || code == 335)) {
            saveName();
            nameInput.setFocused(false);
            return true;
        }
        return super.keyPressed(code, scan, mods);
    }

    private int[] previewPaints() {
        int[] paints = BoatPaint.all(menu.boatItem());
        if (menu.dyeItem().getItem() instanceof DyeItem dye) {
            paints[selected] = selected == BoatPaint.ANTIFOUL
                ? marineColor : dye.getDyeColor().getId();
        } else if (selected == BoatPaint.ANTIFOUL) {
            paints[selected] = marineColor;
        }
        return paints;
    }

    private void drawPreview(GuiGraphics g) {
        if (!(menu.boatItem().getItem() instanceof SkiffBoatItem boat)) return;
        g.flush();
        g.pose().pushPose();
        g.pose().translate(leftPos + 219, topPos + 110, 350);
        // GUI Y grows downward; a negative pitch gives a top-down view rather
        // than the underside-only view seen in v0.3.0.
        g.pose().mulPose(Axis.XP.rotationDegrees(-37f));
        g.pose().mulPose(Axis.YP.rotationDegrees(34f));
        g.pose().scale(29f, -29f, 29f);
        BlueBoatMesh.draw(g.pose(), g.bufferSource(), LightTexture.FULL_BRIGHT,
            boat.getHullStyle(), previewPaints());
        if (!(menu.dyeItem().getItem() instanceof DyeItem)
            && selected != BoatPaint.ANTIFOUL) {
            SkiffMesh.highlight(g.pose(), g.bufferSource(), LightTexture.FULL_BRIGHT,
                BlueBoatMesh.texture(boat.getHullStyle()), BlueBoatMeshData.PARTS,
                BlueBoatMeshData.CHANNELS, selected + 1);
        }
        g.flush();
        g.pose().popPose();
    }

    @Override protected void renderBg(GuiGraphics g, float tick, int mx, int my) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF302820);
        g.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4,
            topPos + imageHeight - 4, 0xFF987A5D);
        g.fill(leftPos + 8, topPos + 23, leftPos + 286, topPos + 52, 0xFF44372F);
        g.fill(leftPos + 8, topPos + 53, leftPos + 140, topPos + 172, 0xFF514337);
        g.fill(leftPos + 141, topPos + 53, leftPos + 286, topPos + 133, 0xFF6C5C4A);
        g.fill(leftPos + 8, topPos + 173, leftPos + 286, topPos + 263, 0xFF514337);
        for (int x : new int[]{30, 256}) {
            g.fill(leftPos + x - 2, topPos + 29,
                leftPos + x + 18, topPos + 49, 0xFF21170F);
            g.fill(leftPos + x - 1, topPos + 30,
                leftPos + x + 17, topPos + 48, 0xFFBAA994);
        }
        // Precisely match the 36 actual inventory slots in BoatCustomizerMenu.
        for (int i = 0; i < 36; i++) {
            int x = leftPos + 64 + i % 9 * 18;
            int y = topPos + (i < 27 ? 183 + i / 9 * 18 : 241);
            g.fill(x, y, x + 19, y + 19, 0xFF221C16);
            g.fill(x + 1, y + 1, x + 17, y + 17, 0xFFC5B5A0);
        }
        if (selected == BoatPaint.ANTIFOUL) for (int i = 0; i < 5; i++) {
            int x = leftPos + 153 + i * 25, y = topPos + 139;
            g.fill(x - 2, y - 2, x + 21, y + 19,
                marineColor == i ? 0xFFFFFFFF : 0xFF271A15);
            g.fill(x, y, x + 19, y + 17, 0xFF000000 | MARINE[i]);
        }
        drawPreview(g);
    }

    @Override protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 11, 8, 0xFFFFFFFF, false);
        g.drawString(font, "BOAT", 51, 34, 0xFFFFFFFF, false);
        g.drawString(font, "DYE / GLOW", 176, 34, 0xFFFFFFFF, false);
        g.drawString(font, "Part: " + PARTS[selected], 12, 83, 0xFFFFE6A4, false);
        g.drawString(font, "Preview before applying", 147, 122, 0xFFEAD8B8, false);
        g.drawString(font, playerInventoryTitle, 65, 172, 0xFFFFFFFF, false);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && selected == BoatPaint.ANTIFOUL) {
            int dx = (int) mx - leftPos - 153;
            int dy = (int) my - topPos - 139;
            if (dx >= 0 && dx < 125 && dy >= 0 && dy < 19 && dx % 25 < 19) {
                marineColor = dx / 25;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        super.render(g, mx, my, partial);
        renderTooltip(g, mx, my);
    }
    @Override protected void containerTick() {
        super.containerTick();
        if (nameInput == null || menu.boatItem().isEmpty()) return;
        String server = BoatPaint.name(menu.boatItem());
        if (!server.equals(lastName)) {
            if (!nameInput.isFocused() || nameInput.getValue().equals(lastName))
                nameInput.setValue(server);
            lastName = server;
        }
    }
}
