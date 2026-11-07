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

/** Expanded boat paint workbench with 3D preview and selection highlighting. */
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
        imageHeight = 327;
        titleLabelX = 12;
        titleLabelY = 9;
        inventoryLabelX = 65;
        inventoryLabelY = 222;
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < PARTS.length; i++) {
            final int part = i;
            addRenderableWidget(Button.builder(Component.literal(PARTS[i]),
                button -> selected = part).bounds(leftPos + 13,
                    topPos + 55 + i * 18, 110, 17).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Apply dye"), button ->
            send(selected, selected == BoatPaint.ANTIFOUL ? marineColor : -1, "")
        ).bounds(leftPos + 151, topPos + 180, 124, 18).build());
        nameInput = new EditBox(font, leftPos + 13, topPos + 181,
            128, 17, Component.literal("Boat name"));
        nameInput.setMaxLength(20);
        nameInput.setHint(Component.literal("Boat name"));
        nameInput.setValue(BoatPaint.name(menu.boatItem()));
        lastName = nameInput.getValue();
        addRenderableWidget(nameInput);
        addRenderableWidget(Button.builder(Component.literal("Save name"), b ->
            saveName()).bounds(leftPos + 13, topPos + 201, 82, 17).build());
        addRenderableWidget(Button.builder(Component.literal("Name dye"), b ->
            send(-2, -1, "")).bounds(leftPos + 101, topPos + 201, 85, 17).build());
        addRenderableWidget(Button.builder(Component.literal("Glow ink"), b ->
            send(-3, -1, "")).bounds(leftPos + 192, topPos + 201, 83, 17).build());
    }

    private void send(int channel, int color, String name) {
        MotorPackets.CHANNEL.sendToServer(new MotorPackets.CustomizeBoat(
            menu.getBlockPos(), channel, color, name));
    }
    private void saveName() {
        if (nameInput != null) send(-1, -1, nameInput.getValue());
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameInput != null && nameInput.isFocused() &&
            (keyCode == 257 || keyCode == 335)) {
            saveName();
            nameInput.setFocused(false);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void preview(GuiGraphics g) {
        if (!(menu.boatItem().getItem() instanceof SkiffBoatItem boat)) return;
        g.pose().pushPose();
        g.pose().translate(leftPos + 206, topPos + 121, 300);
        g.pose().mulPose(Axis.XP.rotationDegrees(27f));
        g.pose().mulPose(Axis.YP.rotationDegrees(38f));
        g.pose().scale(17f, -17f, 17f);
        BlueBoatMesh.draw(g.pose(), g.bufferSource(), LightTexture.FULL_BRIGHT,
            boat.getHullStyle(), BoatPaint.all(menu.boatItem()));
        SkiffMesh.highlight(g.pose(), g.bufferSource(), LightTexture.FULL_BRIGHT,
            BlueBoatMesh.texture(boat.getHullStyle()), BlueBoatMeshData.PARTS,
            BlueBoatMeshData.CHANNELS, selected + 1);
        g.flush();
        g.pose().popPose();
    }

    @Override protected void renderBg(GuiGraphics g, float delta, int mx, int my) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF29231E);
        g.fill(leftPos + 5, topPos + 5, leftPos + imageWidth - 5,
            topPos + imageHeight - 6, 0xFF94775C);
        g.fill(leftPos + 9, topPos + 23, leftPos + imageWidth - 9,
            topPos + 51, 0xFF40342E);
        g.fill(leftPos + 9, topPos + 53, leftPos + 127, topPos + 179, 0xFF544336);
        g.fill(leftPos + 132, topPos + 53, leftPos + 284, topPos + 177, 0xFF4B4038);
        g.fill(leftPos + 9, topPos + 180, leftPos + 284, topPos + 220, 0xFF544336);
        g.fill(leftPos + 9, topPos + 221, leftPos + 284, topPos + 319, 0xFF544336);
        for (int x : new int[]{30, 256}) {
            g.fill(leftPos + x - 2, topPos + 30,
                leftPos + x + 18, topPos + 50, 0xFF191614);
            g.fill(leftPos + x - 1, topPos + 31,
                leftPos + x + 17, topPos + 49, 0xFFB8AA96);
        }
        for (int i = 0; i < 7; i++)
            if (i == selected) g.fill(leftPos + 10,
                topPos + 54 + i * 18, leftPos + 126,
                topPos + 72 + i * 18, 0xFFCEA64E);
        for (int i = 0; i < 36; i++) {
            int x = leftPos + 64 + (i % 9) * 18;
            int y = topPos + (i < 27 ? 234 + (i / 9) * 18 : 292);
            g.fill(x, y, x + 19, y + 19, 0xFF231B16);
            g.fill(x + 1, y + 1, x + 17, y + 17, 0xFFB8AA96);
        }
        if (selected == BoatPaint.ANTIFOUL) {
            for (int i = 0; i < 5; i++) {
                int x = leftPos + 145 + (i % 5) * 25;
                int y = topPos + 154;
                g.fill(x - 2, y - 2, x + 21, y + 19,
                    marineColor == i ? 0xFFFFFFFF : 0xFF231A15);
                g.fill(x, y, x + 19, y + 17, 0xFF000000 | MARINE[i]);
            }
        }
        preview(g);
    }

    @Override protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0xFFFFFFFF, false);
        g.drawString(font, "Boat", 53, 36, 0xFFFFFFFF, false);
        g.drawString(font, "Dye / Glow Ink", 167, 36, 0xFFFFFFFF, false);
        g.drawString(font, "Selected: " + PARTS[selected], 140, 58, 0xFFFFE39B, false);
        g.drawString(font, playerInventoryTitle, 65, 222, 0xFFFFFFFF, false);
    }

    @Override public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && selected == BoatPaint.ANTIFOUL) {
            int dx = (int) mx - leftPos - 145;
            int dy = (int) my - topPos - 154;
            if (dx >= 0 && dx < 125 && dy >= 0 && dy < 19 && dx % 25 < 19) {
                marineColor = dx / 25;
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override public void render(GuiGraphics g, int mx, int my, float tick) {
        renderBackground(g);
        super.render(g, mx, my, tick);
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
