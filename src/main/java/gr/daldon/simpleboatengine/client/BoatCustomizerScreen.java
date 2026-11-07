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
import net.minecraft.world.item.Items;

/**
 * Redesigned two-column boat customizer. Left: boat/dye, part selector and
 * apply action. Right: large orbitable 3D preview, boat name and name dye.
 * Vanilla inventory is aligned below; stripped-wood conversion stays available.
 */
public final class BoatCustomizerScreen extends AbstractContainerScreen<BoatCustomizerMenu> {
    private static final String[] PARTS = {
        "Hull", "Floor", "Stripe", "Hatch lid", "Seats", "Antifouling", "Inner walls", "Name"
    };
    private static final int[] MARINE = {
        0x14585D, 0x15345C, 0x314238, 0x522A32, 0x292D35
    };
    private int selected = 0, marineColor = 0;
    private float angle = 35f, pitch = -45f;
    private EditBox nameInput;
    private String lastName = "";
    private Button applyPaint;

    public BoatCustomizerScreen(BoatCustomizerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 340;
        imageHeight = 309;
        titleLabelX = 12;
        titleLabelY = 7;
        inventoryLabelX = 87;
        inventoryLabelY = 213;
    }

    private void updatePart() {
        if (applyPaint != null)
            applyPaint.setMessage(Component.literal(selected == BoatPaint.CHANNELS
                ? "Apply name dye / ink" : "Apply dye"));
    }

    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            selected = (selected + PARTS.length - 1) % PARTS.length;
            updatePart();
        }).bounds(leftPos + 12, topPos + 104, 29, 22).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            selected = (selected + 1) % PARTS.length;
            updatePart();
        }).bounds(leftPos + 124, topPos + 104, 29, 22).build());
        applyPaint = addRenderableWidget(Button.builder(Component.literal("Apply dye"), b -> {
            if (selected == BoatPaint.CHANNELS) {
                send(menu.nameDyeItem().is(Items.GLOW_INK_SAC) ? -3 : -2, -1, "");
            } else {
                send(selected, selected == BoatPaint.ANTIFOUL ? marineColor : -1, "");
            }
        }).bounds(leftPos + 12, topPos + 137, 141, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Apply wood"), b ->
            send(-4, -1, "")).bounds(leftPos + 64, topPos + 178, 89, 20).build());

        nameInput = new EditBox(font, leftPos + 169, topPos + 159,
            122, 19, Component.literal("Boat name"));
        nameInput.setMaxLength(20);
        nameInput.setHint(Component.literal("Boat name"));
        nameInput.setValue(BoatPaint.name(menu.boatItem()));
        lastName = nameInput.getValue();
        addRenderableWidget(nameInput);

        addRenderableWidget(Button.builder(Component.literal("Save name"), b ->
            saveName()).bounds(leftPos + 169, topPos + 181, 75, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Apply name dye"), b ->
            send(menu.nameDyeItem().is(Items.GLOW_INK_SAC) ? -3 : -2, -1, "")
        ).bounds(leftPos + 248, topPos + 181, 81, 18).build());
    }

    private void send(int channel, int color, String name) {
        MotorPackets.CHANNEL.sendToServer(new MotorPackets.CustomizeBoat(
            menu.getBlockPos(), channel, color, name));
    }

    private void saveName() {
        if (nameInput != null) send(-1, -1, nameInput.getValue());
    }

    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (nameInput != null && nameInput.isFocused() && (key == 257 || key == 335)) {
            saveName();
            nameInput.setFocused(false);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
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
        g.flush();
        g.enableScissor(leftPos + 165, topPos + 24, leftPos + 332, topPos + 154);
        g.pose().pushPose();
        g.pose().translate(leftPos + 249, topPos + 91, 340);
        g.pose().mulPose(Axis.XP.rotationDegrees(pitch));
        g.pose().mulPose(Axis.YP.rotationDegrees(angle));
        g.pose().scale(25f, -25f, 25f);
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

    private static void frame(GuiGraphics g, int x0, int y0, int x1, int y1,
                              int outer, int inner) {
        g.fill(x0, y0, x1, y1, outer);
        g.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, inner);
    }

    @Override protected void renderBg(GuiGraphics g, float tick, int mx, int my) {
        frame(g, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight,
            0xFF352B23, 0xFFA38B70);
        frame(g, leftPos + 8, topPos + 22, leftPos + 160, topPos + 95,
            0xFF5C4B3D, 0xFF796451);
        frame(g, leftPos + 8, topPos + 99, leftPos + 160, topPos + 165,
            0xFF5C4B3D, 0xFF796451);
        frame(g, leftPos + 162, topPos + 22, leftPos + 335, topPos + 157,
            0xFF2F251D, 0xFF665A4D);
        frame(g, leftPos + 162, topPos + 156, leftPos + 335, topPos + 204,
            0xFF5C4B3D, 0xFF796451);
        frame(g, leftPos + 8, topPos + 168, leftPos + 160, topPos + 204,
            0xFF5C4B3D, 0xFF796451);
        frame(g, leftPos + 8, topPos + 207, leftPos + 333, topPos + 304,
            0xFF5C4B3D, 0xFF796451);

        // Slots use the exact same coordinate system as BoatCustomizerMenu.
        for (int[] pos : new int[][]{{35, 46}, {124, 46}, {35, 180}, {311, 159}}) {
            int x = leftPos + pos[0] - 1, y = topPos + pos[1] - 1;
            g.fill(x, y, x + 19, y + 19, 0xFF211A14);
            g.fill(x + 1, y + 1, x + 18, y + 18, 0xFFC0AF99);
        }
        for (int i = 0; i < 36; i++) {
            int x = leftPos + 86 + (i % 9) * 18;
            int y = topPos + (i < 27 ? 227 + i / 9 * 18 : 283);
            g.fill(x, y, x + 19, y + 19, 0xFF261D16);
            g.fill(x + 1, y + 1, x + 18, y + 18, 0xFFC2B097);
        }
        if (selected == BoatPaint.ANTIFOUL)
            for (int i = 0; i < MARINE.length; i++) {
                int x = leftPos + 169 + i * 31, y = topPos + 133;
                g.fill(x - 2, y - 2, x + 24, y + 21,
                    marineColor == i ? 0xFFFFFFFF : 0xFF292019);
                g.fill(x, y, x + 22, y + 19, 0xFF000000 | MARINE[i]);
            }
        preview(g);
    }

    @Override protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 12, 7, 0xFFF9EDDA, false);
        g.drawCenteredString(font, "BOAT", 44, 31, 0xFFF8E8CF);
        g.drawCenteredString(font, "DYE", 133, 31, 0xFFF8E8CF);
        g.drawCenteredString(font, PARTS[selected], 82, 111, 0xFFFFE4AC);
        g.drawCenteredString(font, "PREVIEW  -  DRAG TO ROTATE", 249, 29, 0xFFF8E8CF);
        g.drawString(font, "NAME", 170, 145, 0xFFF8E8CF, false);
        g.drawString(font, "DYE", 294, 145, 0xFFF8E8CF, false);
        g.drawString(font, "WOOD", 57, 183, 0xFFF8E8CF, false);
        g.drawString(font, playerInventoryTitle, 87, 213, 0xFFF8E8CF, false);
    }

    @Override public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (btn == 0 && mx >= leftPos + 162 && mx <= leftPos + 335
            && my >= topPos + 22 && my <= topPos + 157) {
            angle += (float)dx * 1.8f;
            pitch -= (float)dy * 0.8f;
            if (pitch > 180f) pitch -= 360f;
            if (pitch < -180f) pitch += 360f;
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && selected == BoatPaint.ANTIFOUL) {
            int dx = (int)mx - leftPos - 169, dy = (int)my - topPos - 133;
            if (dx >= 0 && dx < 155 && dy >= 0 && dy < 21 && dx % 31 < 22) {
                marineColor = dx / 31;
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
