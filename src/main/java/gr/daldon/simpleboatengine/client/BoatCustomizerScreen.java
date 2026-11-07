package gr.daldon.simpleboatengine.client;

import com.mojang.math.Axis;
import gr.daldon.simpleboatengine.common.BoatCustomizerMenu;
import gr.daldon.simpleboatengine.common.BoatPaint;
import gr.daldon.simpleboatengine.common.EngineItems;
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

/** Small, readable workstation with direct drag-to-rotate boat preview. */
public final class BoatCustomizerScreen extends AbstractContainerScreen<BoatCustomizerMenu> {
    private static final String[] PARTS = {
        "Hull", "Floor", "Stripe", "Hatch lid", "Seats", "Antifouling", "Inner walls", "Name"
    };
    private static final int[] MARINE = {0x14585D,0x15345C,0x314238,0x522A32,0x292D35};
    private int selected, marineColor;
    private float angle = 35f, pitch = -53f;
    private EditBox nameInput;
    private String lastName = "";

    public BoatCustomizerScreen(BoatCustomizerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 312;
        imageHeight = 276;
        inventoryLabelX = 75;
        inventoryLabelY = 182;
        titleLabelX = 10;
        titleLabelY = 8;
    }

    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("<"), b ->
            selected = (selected + PARTS.length - 1) % PARTS.length)
            .bounds(leftPos + 12, topPos + 74, 25, 18).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b ->
            selected = (selected + 1) % PARTS.length)
            .bounds(leftPos + 108, topPos + 74, 25, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Apply dye / ink"), b -> {
            if (selected == BoatPaint.CHANNELS) {
                var ink = menu.nameDyeItem();
                send(ink.is(net.minecraft.world.item.Items.GLOW_INK_SAC) ? -3 : -2, -1, "");
            } else {
                send(selected, selected == BoatPaint.ANTIFOUL ? marineColor : -1, "");
            }
        }).bounds(leftPos + 12, topPos + 96, 121, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Apply wood"), b ->
            send(-4, -1, "")).bounds(leftPos + 12, topPos + 117, 121, 18).build());
        nameInput = new EditBox(font, leftPos + 12, topPos + 141,
            136, 17, Component.literal("Boat name"));
        nameInput.setMaxLength(20);
        nameInput.setHint(Component.literal("Boat name"));
        nameInput.setValue(BoatPaint.name(menu.boatItem()));
        lastName = nameInput.getValue();
        addRenderableWidget(nameInput);
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> saveName())
            .bounds(leftPos + 153, topPos + 141, 47, 17).build());

    }

    private void send(int channel, int color, String name) {
        MotorPackets.CHANNEL.sendToServer(new MotorPackets.CustomizeBoat(
            menu.getBlockPos(), channel, color, name));
    }
    private void saveName() {
        if (nameInput != null) send(-1, -1, nameInput.getValue());
    }
    @Override public boolean keyPressed(int code, int scan, int modifiers) {
        if (nameInput != null && nameInput.isFocused() && (code == 257 || code == 335)) {
            saveName();
            nameInput.setFocused(false);
            return true;
        }
        return super.keyPressed(code, scan, modifiers);
    }

    private int currentWoodStyle(SkiffBoatItem boat) {
        int pending = EngineItems.woodStyleFor(menu.woodItem());
        return pending > 0 ? pending : BoatPaint.woodStyle(menu.boatItem(), boat.getHullStyle());
    }
    private int[] displayedPaints() {
        int[] paints = BoatPaint.all(menu.boatItem());
        if (selected >= BoatPaint.CHANNELS) return paints;
        if (menu.dyeItem().getItem() instanceof DyeItem dye) {
            paints[selected] = selected == BoatPaint.ANTIFOUL
                ? marineColor : dye.getDyeColor().getId();
        } else if (selected == BoatPaint.ANTIFOUL) {
            paints[selected] = marineColor;
        }
        return paints;
    }

    private void preview(GuiGraphics g) {
        if (!(menu.boatItem().getItem() instanceof SkiffBoatItem boat)) return;
        // Flush the GUI background before issuing textured 3D triangles.
        g.flush();
        g.enableScissor(leftPos + 147, topPos + 53, leftPos + 303, topPos + 138);
        g.pose().pushPose();
        g.pose().translate(leftPos + 226, topPos + 102, 330);
        g.pose().mulPose(Axis.XP.rotationDegrees(pitch));
        g.pose().mulPose(Axis.YP.rotationDegrees(angle));
        g.pose().scale(22f, -22f, 22f);
        int style = currentWoodStyle(boat);
        BlueBoatMesh.draw(g.pose(), g.bufferSource(), LightTexture.FULL_BRIGHT,
            style, displayedPaints());
        if (selected < BoatPaint.CHANNELS
            && !(menu.dyeItem().getItem() instanceof DyeItem)) {
            SkiffMesh.highlight(g.pose(), g.bufferSource(), LightTexture.FULL_BRIGHT,
                BlueBoatMesh.texture(style), BlueBoatMeshData.PARTS,
                BlueBoatMeshData.CHANNELS, selected + 1);
        }
        g.flush();
        g.pose().popPose();
        g.disableScissor();
    }

    @Override protected void renderBg(GuiGraphics g, float tick, int mx, int my) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF2A2622);
        g.fill(leftPos + 4, topPos + 4, leftPos + 308, topPos + 272, 0xFF9C856C);
        g.fill(leftPos + 8, topPos + 24, leftPos + 304, topPos + 51, 0xFF423A32);
        g.fill(leftPos + 8, topPos + 53, leftPos + 139, topPos + 138, 0xFF51473B);
        g.fill(leftPos + 145, topPos + 53, leftPos + 304, topPos + 138, 0xFF695B4B);
        g.fill(leftPos + 8, topPos + 139, leftPos + 304, topPos + 180, 0xFF51473B);
        g.fill(leftPos + 8, topPos + 181, leftPos + 304, topPos + 272, 0xFF51473B);
        for (int x : new int[]{28, 148, 272}) {
            g.fill(leftPos + x - 2, topPos + 25, leftPos + x + 18,
                topPos + 45, 0xFF231A14);
            g.fill(leftPos + x - 1, topPos + 26, leftPos + x + 17,
                topPos + 44, 0xFFC1AF96);
        }
        // Independent name dye / glow-ink input.
        g.fill(leftPos + 265, topPos + 144, leftPos + 285, topPos + 164, 0xFF231A14);
        g.fill(leftPos + 266, topPos + 145, leftPos + 284, topPos + 163, 0xFFC1AF96);
        for (int i = 0; i < 36; i++) {
            int x = leftPos + 74 + (i % 9) * 18;
            int y = topPos + (i < 27 ? 193 + i / 9 * 18 : 251);
            g.fill(x, y, x + 19, y + 19, 0xFF241C16);
            g.fill(x + 1, y + 1, x + 17, y + 17, 0xFFC4B29A);
        }
        if (selected == BoatPaint.ANTIFOUL)
            for (int i = 0; i < MARINE.length; i++) {
                int x = leftPos + 151 + i * 29, y = topPos + 116;
                g.fill(x - 2, y - 2, x + 23, y + 22,
                    marineColor == i ? 0xFFF5EACF : 0xFF292015);
                g.fill(x, y, x + 21, y + 20, 0xFF000000 | MARINE[i]);
            }
        preview(g);
    }

    @Override protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 10, 8, 0xFFFFFFFF, false);
        g.drawString(font, "Boat", 48, 31, 0xFFFFFFFF, false);
        g.drawString(font, "Dye / Glow", 173, 31, 0xFFFFFFFF, false);
        g.drawString(font, "Wood", 245, 31, 0xFFFFFFFF, false);
        g.drawCenteredString(font, PARTS[selected], 72, 79, 0xFFFFE6AD);
        g.drawString(font, "Drag to rotate", 176, 57, 0xFFF4E3C5, false);
        g.drawString(font, "Name dye", 222, 135, 0xFFFFFFFF, false);
        g.drawString(font, playerInventoryTitle, 75, 182, 0xFFFFFFFF, false);
    }

    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 0 && mx >= leftPos + 145 && mx <= leftPos + 304
            && my >= topPos + 53 && my <= topPos + 139) {
            angle += (float) dx * 1.8f;
            pitch -= (float) dy * 0.8f;
            if (pitch > 180f) pitch -= 360f;
            if (pitch < -180f) pitch += 360f;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }
    @Override public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && selected == BoatPaint.ANTIFOUL) {
            int dx = (int)mx - leftPos - 151, dy = (int)my - topPos - 116;
            if (dx >= 0 && dx < 145 && dy >= 0 && dy < 22 && dx % 29 < 21) {
                marineColor = dx / 29;
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
