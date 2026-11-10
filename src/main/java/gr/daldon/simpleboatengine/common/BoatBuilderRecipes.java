package gr.daldon.simpleboatengine.common;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** All boats and every equippable marine attachment available from one dock. */
public final class BoatBuilderRecipes {
    public record Blueprint(ItemStack result,LinkedHashMap<Item,Integer> cost) {}
    private BoatBuilderRecipes(){}
    private static Blueprint b(ItemStack result,Object... ingredients){
        LinkedHashMap<Item,Integer> cost=new LinkedHashMap<>();
        for(int i=0;i<ingredients.length;i+=2) cost.put((Item)ingredients[i],(int)ingredients[i+1]);
        return new Blueprint(result,cost);
    }
    public static List<Blueprint> all(){
        List<Blueprint> out=new ArrayList<>();
        out.add(b(new ItemStack(EngineItems.BLUE_FISHING_BOAT.get()),
            Items.OAK_PLANKS,24,Items.IRON_INGOT,6,Items.CHEST,2));
        Item[] woods={Items.OAK_PLANKS,Items.SPRUCE_PLANKS,Items.BIRCH_PLANKS,
            Items.JUNGLE_PLANKS,Items.ACACIA_PLANKS,Items.DARK_OAK_PLANKS,
            Items.MANGROVE_PLANKS,Items.CHERRY_PLANKS,Items.BAMBOO_PLANKS};
        for(int i=0;i<EngineItems.WOOD_BOATS.size();i++){
            out.add(b(new ItemStack(EngineItems.WOOD_BOATS.get(i).get()),
                woods[i],24,Items.IRON_INGOT,6,Items.CHEST,2));
        }
        out.add(b(new ItemStack(EngineItems.ENGINE.get()),
            Items.IRON_INGOT,12,Items.REDSTONE,4,Items.PISTON,2));
        out.add(b(new ItemStack(EngineItems.POLE_LIGHT.get()),
            Items.IRON_INGOT,4,Items.GLASS_PANE,2,Items.GLOWSTONE_DUST,3));
        out.add(b(new ItemStack(EngineItems.FUEL_CANISTER.get()),
            Items.IRON_INGOT,6,Items.RED_DYE,2,Items.BUCKET,1));
        out.add(b(new ItemStack(EngineItems.ICE_BREAKER.get()),
            Items.IRON_INGOT,10,Items.IRON_PICKAXE,1));
        out.add(b(new ItemStack(EngineItems.BOAT_RECOVERY_KIT.get()),
            Items.IRON_INGOT,4,Items.CHAIN,2));
        out.add(b(new ItemStack(EngineItems.FUEL_BUCKET.get()),
            Items.BUCKET,1,Items.IRON_INGOT,4));
        return List.copyOf(out);
    }
}
