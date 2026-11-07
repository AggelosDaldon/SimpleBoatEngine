package gr.daldon.simpleboatengine.common;

import gr.daldon.simpleboatengine.SimpleBoatEngine;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = SimpleBoatEngine.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class EngineItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, SimpleBoatEngine.ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SimpleBoatEngine.ID);

    public static final RegistryObject<Item> ENGINE = ITEMS.register("engine",
        () -> new EngineItem(new Item.Properties().stacksTo(1)));
    // Historical registry id retained so existing worlds and inventories still load.
    public static final RegistryObject<Item> FUEL_BUCKET = ITEMS.register("fuel_bucket",
        () -> new EngineItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> EMPTY_FUEL_CANISTER = ITEMS.register("empty_fuel_canister",
        () -> new EngineItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> FUEL_CANISTER = ITEMS.register("fuel_canister",
        () -> new EngineItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> POLE_LIGHT = ITEMS.register("pole_light",
        () -> new EngineItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CUSTOMIZER = ITEMS.register("boat_customizer",
        () -> new BlockItem(EngineBlocks.CUSTOMIZER.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLUE_FISHING_BOAT = ITEMS.register("aggelos_blue_fishing_boat",
        () -> new SkiffBoatItem(new Item.Properties().stacksTo(1), EngineEntities.BLUE_FISHING_BOAT));
    public static final RegistryObject<Item> BOAT_TRAILER = ITEMS.register("boat_trailer",
        () -> new BoatTrailerItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> WINCH_ROPE = ITEMS.register("winch_rope",
        () -> new WinchRopeItem(new Item.Properties().stacksTo(1)));

    public static final String[] WOODS = {
        "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo"
    };
    public static final List<RegistryObject<Item>> WOOD_BOATS = registerWoodBoats();

    private static List<RegistryObject<Item>> registerWoodBoats() {
        List<RegistryObject<Item>> boats = new ArrayList<>();
        for (int index = 0; index < WOODS.length; index++) {
            final int style = index + 1;
            boats.add(ITEMS.register(WOODS[index] + "_fishing_boat",
                () -> new SkiffBoatItem(new Item.Properties().stacksTo(1),
                    EngineEntities.BLUE_FISHING_BOAT, style)));
        }
        return List.copyOf(boats);
    }

    /** Match actual vanilla stripped wood to each boat material. */
    public static int woodStyleFor(ItemStack stack) {
        Item item = stack.getItem();
        if (item == net.minecraft.world.item.Items.STRIPPED_OAK_LOG) return 1;
        if (item == net.minecraft.world.item.Items.STRIPPED_SPRUCE_LOG) return 2;
        if (item == net.minecraft.world.item.Items.STRIPPED_BIRCH_LOG) return 3;
        if (item == net.minecraft.world.item.Items.STRIPPED_JUNGLE_LOG) return 4;
        if (item == net.minecraft.world.item.Items.STRIPPED_ACACIA_LOG) return 5;
        if (item == net.minecraft.world.item.Items.STRIPPED_DARK_OAK_LOG) return 6;
        if (item == net.minecraft.world.item.Items.STRIPPED_MANGROVE_LOG) return 7;
        if (item == net.minecraft.world.item.Items.STRIPPED_CHERRY_LOG) return 8;
        if (item == net.minecraft.world.item.Items.STRIPPED_BAMBOO_BLOCK) return 9;
        return -1;
    }

    public static Item boatForStyle(int style) {
        return style > 0 && style <= WOOD_BOATS.size()
            ? WOOD_BOATS.get(style - 1).get() : BLUE_FISHING_BOAT.get();
    }

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = TABS.register("main",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.simpleboatengine"))
            .icon(() -> new ItemStack(ENGINE.get()))
            .displayItems((parameters, output) -> {
                output.accept(ENGINE.get());
                output.accept(FUEL_BUCKET.get());
                output.accept(EMPTY_FUEL_CANISTER.get());
                output.accept(FUEL_CANISTER.get());
                output.accept(POLE_LIGHT.get());
                output.accept(CUSTOMIZER.get());
                output.accept(BOAT_TRAILER.get());
                output.accept(WINCH_ROPE.get());
                output.accept(BLUE_FISHING_BOAT.get());
                for (RegistryObject<Item> boat : WOOD_BOATS) output.accept(boat.get());
            }).build());

    /** Show the skiff in a vanilla tab as well as in Simple Boat Engine's tab. */
    @SubscribeEvent
    public static void populateVanillaTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(BLUE_FISHING_BOAT.get());
            event.accept(BOAT_TRAILER.get());
            for (RegistryObject<Item> boat : WOOD_BOATS) event.accept(boat.get());
        }
    }

    private EngineItems() {}
}
