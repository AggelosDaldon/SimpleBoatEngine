package gr.daldon.simpleboatengine.client;

import gr.daldon.simpleboatengine.common.BoatCargoMenu;
import gr.daldon.simpleboatengine.common.MotorPackets;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class BoatCargoScreen extends BoatStorageScreen<BoatCargoMenu> {
    public BoatCargoScreen(BoatCargoMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 192);
    }

    @Override protected Component storageTitle() { return title; }
    @Override protected int visibleStorageSlots() { return menu.storageSlots(); }
    @Override protected boolean canScrollStorage() { return !menu.isHatch(); }
    @Override protected int firstRow() { return menu.startRow(); }
    @Override protected void requestRow(int row) {
        requestView(row, () ->
            MotorPackets.CHANNEL.sendToServer(new MotorPackets.ScrollCargo(menu.boatId(), row)));
    }
}
