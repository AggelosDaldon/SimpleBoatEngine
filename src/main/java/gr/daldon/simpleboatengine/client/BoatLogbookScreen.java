package gr.daldon.simpleboatengine.client;

import com.mojang.math.Axis;
import gr.daldon.simpleboatengine.common.BoatLogbookItem;
import gr.daldon.simpleboatengine.common.EngineItems;
import gr.daldon.simpleboatengine.common.MotorPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class BoatLogbookScreen extends Screen {
    private static final int BOOK_WIDTH = 254;
    private static final int BOOK_HEIGHT = 225;
    private static final int INK = 0xFF213447;
    private final InteractionHand hand;
    private int left;
    private int top;
    private int page;
    private Button back;
    private Button next;
    private Button main;

    private BoatLogbookScreen(InteractionHand hand) {
        super(Component.translatable("item.daldonsboatadditions.boat_logbook"));
        this.hand = hand;
    }

    public static void open(InteractionHand hand) {
        Minecraft.getInstance().setScreen(new BoatLogbookScreen(hand));
    }

    @Override protected void init() {
        left = (width - BOOK_WIDTH) / 2;
        top = (height - BOOK_HEIGHT) / 2;
        back = addRenderableWidget(Button.builder(Component.literal("<"), button -> flip(-1))
            .bounds(left + 16, top + 198, 24, 18).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), button -> flip(1))
            .bounds(left + BOOK_WIDTH - 40, top + 198, 24, 18).build());
        main = addRenderableWidget(Button.builder(Component.translatable("screen.daldonsboatadditions.set_main"),
            button -> chooseMain()).bounds(left + (BOOK_WIDTH - 118) / 2, top + 174, 118, 18).build());
    }

    private ItemStack heldBook() {
        if (minecraft == null || minecraft.player == null) return ItemStack.EMPTY;
        ItemStack held = minecraft.player.getItemInHand(hand);
        return held.is(EngineItems.BOAT_LOGBOOK.get()) ? held : ItemStack.EMPTY;
    }

    private ListTag pages() {
        return BoatLogbookItem.pages(heldBook());
    }

    private void chooseMain() {
        var current = pages().getCompound(page);
        if (current.hasUUID("Boat")) MotorPackets.CHANNEL.sendToServer(
            new MotorPackets.SetMainBoat(current.getUUID("Boat"), hand));
    }

    private void flip(int offset) {
        int size = pages().size();
        int changed = Math.max(0, Math.min(size - 1, page + offset));
        if (changed != page && minecraft != null && minecraft.player != null) {
            minecraft.player.playSound(net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1f, 1f);
            page = changed;
        }
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics);
        graphics.fill(left, top, left + BOOK_WIDTH, top + BOOK_HEIGHT, 0xFF172333);
        graphics.fill(left + 3, top + 4, left + BOOK_WIDTH - 3, top + BOOK_HEIGHT - 4, 0xFF42637B);
        graphics.fill(left + 8, top + 7, left + BOOK_WIDTH - 8, top + BOOK_HEIGHT - 7, 0xFFD4BE8D);
        graphics.fillGradient(left + 12, top + 8, left + BOOK_WIDTH - 12, top + BOOK_HEIGHT - 10,
            0xFFF1E2BA, 0xFFE2CCA0);
        int center = left + BOOK_WIDTH / 2;
        graphics.drawString(font, title, center - font.width(title) / 2, top + 15, INK, false);
        graphics.fill(left + 36, top + 30, left + BOOK_WIDTH - 36, top + 31, 0xFF957D52);
        ListTag pages = pages();
        page = Math.min(page, Math.max(0, pages.size() - 1));
        back.active = page > 0;
        next.active = page + 1 < pages.size();
        main.visible = !pages.isEmpty();
        if (pages.isEmpty()) {
            var hint = Component.translatable("screen.daldonsboatadditions.logbook_empty");
            var lines = font.split(hint, BOOK_WIDTH - 55);
            for (int i = 0; i < lines.size(); i++)
                graphics.drawString(font, lines.get(i), left + 26, top + 84 + i * 12, INK, false);
        } else {
            var record = pages.getCompound(page);
            boolean primary = BoatLogbookItem.isMainPage(heldBook(), record);
            main.active = !primary && record.hasUUID("Boat");
            main.setMessage(Component.translatable(primary ? "screen.daldonsboatadditions.main_boat"
                : "screen.daldonsboatadditions.set_main"));
            ItemStack boat = ItemStack.of(record.getCompound("Preview"));
            Component name = boat.isEmpty() ? Component.translatable("screen.daldonsboatadditions.unknown_boat")
                : boat.getHoverName();
            graphics.drawString(font, name, center - font.width(name) / 2, top + 40, INK, false);
            if (!boat.isEmpty()) renderBoat(graphics, boat, center, top + 100);
            graphics.drawString(font, Component.translatable("screen.daldonsboatadditions.distance",
                Long.toString(record.getLong("Distance"))), left + 28, top + 143, INK, false);
            graphics.drawString(font, Component.translatable("screen.daldonsboatadditions.fish",
                Long.toString(record.getLong("Fish"))), left + 28, top + 157, INK, false);
            String pageLabel = (page + 1) + " / " + pages.size();
            graphics.drawString(font, pageLabel, center - font.width(pageLabel) / 2, top + 204, INK, false);
        }
        super.render(graphics, mouseX, mouseY, partial);
    }

    private void renderBoat(GuiGraphics graphics, ItemStack boat, int x, int y) {
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(x, y + 4, 250);
        graphics.pose().mulPose(Axis.XP.rotationDegrees(-38f));
        graphics.pose().mulPose(Axis.YP.rotationDegrees(32f));
        graphics.pose().scale(23f, -23f, 23f);
        BoatItemVisuals.drawBoat(boat, graphics.pose(), graphics.bufferSource(), LightTexture.FULL_BRIGHT);
        graphics.flush();
        graphics.pose().popPose();
    }
}
