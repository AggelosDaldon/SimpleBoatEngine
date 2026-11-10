package gr.daldon.simpleboatengine.common;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Standard one-chest GUI with the opening/closing sound of a barrel. */
public final class BarrelHatchMenu extends ChestMenu {
    private final BlueFishingBoat owner;
    public BarrelHatchMenu(int id,Inventory inventory,Container storage,BlueFishingBoat owner){
        super(MenuType.GENERIC_9x3,id,inventory,storage,3);
        this.owner=owner;
    }
    @Override public void removed(Player player){
        super.removed(player);
        if(!player.level().isClientSide)
            player.level().playSound(null,owner.blockPosition(),
                SoundEvents.BARREL_CLOSE,SoundSource.BLOCKS,.8f,.95f);
    }
}
