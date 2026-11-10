package gr.daldon.simpleboatengine.common;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** A portable crate which captures a boat, then docks it in an empty stand.
 *  The entire packed boat NBT is carried by the recovery tool.
 */
public final class BoatRecoveryItem extends Item {
    private static final String STORED="RecoveredBoat";
    public BoatRecoveryItem(Properties properties) {super(properties);}
    public static boolean loaded(ItemStack stack){
        return stack.hasTag() && stack.getTag().contains(STORED,10);
    }
    public static ItemStack boat(ItemStack stack) {
        return loaded(stack)?ItemStack.of(stack.getTag().getCompound(STORED)):ItemStack.EMPTY;
    }
    public static void put(ItemStack kit, ItemStack boat) {
        if(boat.isEmpty())kit.getOrCreateTag().remove(STORED);
        else kit.getOrCreateTag().put(STORED,boat.save(new CompoundTag()));
    }
    @Override public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level level,
                                       net.minecraft.world.entity.Entity carrier, int slot, boolean selected) {
        if (level instanceof net.minecraft.server.level.ServerLevel server && loaded(stack) && level.getGameTime() % 10 == 0)
            BoatNavigationData.get(server).observePacked(boat(stack), server, carrier.blockPosition(), false);
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        var level=context.getLevel();
        var pos=context.getClickedPos();
        if(level.getBlockEntity(pos) instanceof BoatStandBlockEntity stand) {
            ItemStack kit=context.getItemInHand();
            if(level.isClientSide)return InteractionResult.SUCCESS;
            if(loaded(kit) && stand.getItem(0).isEmpty()) {
                stand.setItem(0,boat(kit));
                put(kit,ItemStack.EMPTY);
                return InteractionResult.CONSUME;
            }
            if(!loaded(kit) && !stand.getItem(0).isEmpty() && !stand.hasPreview()) {
                put(kit,stand.removeItemNoUpdate(0));
                return InteractionResult.CONSUME;
            }
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }
}
