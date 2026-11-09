package gr.daldon.simpleboatengine.common;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;

/** A portable crate which captures a boat, then docks it in an empty stand.
 *  The entire packed boat NBT is carried by the recovery tool.
 */
public final class BoatRecoveryItem extends BlockItem {
    private static final String STORED="RecoveredBoat";
    public BoatRecoveryItem(Properties properties) {super(EngineBlocks.BOAT_RECOVERY.get(),properties);}
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
        // A loaded kit must not turn into an empty placed block and lose its boat.
        return loaded(context.getItemInHand())?InteractionResult.FAIL:super.useOn(context);
    }
}
