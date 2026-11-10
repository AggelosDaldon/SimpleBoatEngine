package gr.daldon.simpleboatengine.common;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;

/** Refillable steel canister. Works directly in any ordinary player inventory.
 * All fuel is NBT-backed and transferred, never duplicated by right-clicking.
 */
public final class SteelFuelCanisterItem extends Item {
    public static final int CAPACITY=MotorData.TANK_CAPACITY;
    public static final int COAL_UNIT=CAPACITY/60;
    public static final int CATALYZED_UNIT=CAPACITY/20;
    public SteelFuelCanisterItem(Properties properties){super(properties);}
    public static int fuel(ItemStack stack){
        return stack.getOrCreateTag().getInt("SteelFuel");
    }
    public static void fuel(ItemStack stack,int amount){
        stack.getOrCreateTag().putInt("SteelFuel",Math.max(0,Math.min(CAPACITY,amount)));
    }
    public static int catalysts(ItemStack stack){
        return Math.max(0,stack.getOrCreateTag().getInt("RedstoneCatalysts"));
    }
    private static void catalysts(ItemStack stack,int amount){
        stack.getOrCreateTag().putInt("RedstoneCatalysts",Math.max(0,Math.min(20,amount)));
    }
    /** Creative-mode fallback: right-clicking a held canister fills it without
     * requiring ingredient stacks (creative inventory bypasses normal slot hooks).
     * Survival inventory filling remains unchanged.
     */
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack canister=player.getItemInHand(hand);
        if(!player.getAbilities().instabuild)return InteractionResultHolder.pass(canister);
        if(!level.isClientSide){
            fuel(canister,CAPACITY);
            player.displayClientMessage(Component.literal("Steel Fuel Canister: 100%"),true);
        }
        return InteractionResultHolder.sidedSuccess(canister,level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,@Nullable Level world,
                                         List<Component> tooltip,TooltipFlag flag){
        tooltip.add(Component.literal("Fuel: "+fuel(stack)+" / "+CAPACITY+
            " ("+Math.round(100f*fuel(stack)/CAPACITY)+"%)").withStyle(ChatFormatting.GOLD));
        if(catalysts(stack)>0)
            tooltip.add(Component.literal("Redstone catalyst: "+catalysts(stack)+"/20")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Coal / Charcoal: 60 = full").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("20 Redstone + 20 Coal = full").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Lava bucket = full").withStyle(ChatFormatting.GRAY));
    }
    private static boolean isFuel(ItemStack ingredient){
        return ingredient.is(Items.COAL)||ingredient.is(Items.CHARCOAL)
            ||ingredient.is(Items.REDSTONE)||ingredient.is(Items.LAVA_BUCKET);
    }
    /** The actual amount accepted from a source stack; zero means no interaction. */
    private static int transfer(ItemStack canister,ItemStack material,
                                 int maximum,Player player,java.util.function.Consumer<ItemStack> bucketReturn){
        if(!isFuel(material) || maximum<=0)return 0;
        int available=CAPACITY-fuel(canister);
        if(material.is(Items.REDSTONE)){
            int count=Math.min(Math.min(maximum,material.getCount()),20-catalysts(canister));
            if(count<=0 || available<=0)return 0;
            catalysts(canister,catalysts(canister)+count);
            material.shrink(count);
            return count;
        }
        if(available<=0)return 0;
        if(material.is(Items.LAVA_BUCKET)){
            fuel(canister,CAPACITY);
            material.shrink(1);
            bucketReturn.accept(new ItemStack(Items.BUCKET));
            return 1;
        }
        int catalysts=catalysts(canister);
        int spent=0;
        while(spent<maximum && !material.isEmpty() && fuel(canister)<CAPACITY){
            int value=catalysts>0?CATALYZED_UNIT:COAL_UNIT;
            fuel(canister,fuel(canister)+value);
            if(catalysts>0)catalysts--;
            material.shrink(1);
            spent++;
        }
        catalysts(canister,catalysts);
        return spent;
    }
    @Override public boolean overrideOtherStackedOnMe(ItemStack canister,
            ItemStack material,Slot slot,ClickAction action,Player player,SlotAccess carried){
        if(!isFuel(material))return false;
        if(player.level().isClientSide)return true;
        int maximum=action==ClickAction.SECONDARY?1:material.getCount();
        transfer(canister,material,maximum,player,bucket->{
            if(!carried.set(bucket) && !player.getInventory().add(bucket))
                player.drop(bucket,false);
        });
        slot.setChanged();
        return true;
    }
    @Override public boolean overrideStackedOnOther(ItemStack canister,
            Slot slot,ClickAction action,Player player){
        ItemStack material=slot.getItem();
        if(!isFuel(material))return false;
        if(player.level().isClientSide)return true;
        int maximum=action==ClickAction.SECONDARY?1:material.getCount();
        transfer(canister,material,maximum,player,bucket->{
            if(!player.getInventory().add(bucket))player.drop(bucket,false);
        });
        slot.setChanged();
        return true;
    }
}
