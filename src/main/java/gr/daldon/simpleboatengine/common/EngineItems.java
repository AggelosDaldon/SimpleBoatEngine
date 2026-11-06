package gr.daldon.simpleboatengine.common;

import gr.daldon.simpleboatengine.SimpleBoatEngine;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class EngineItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, SimpleBoatEngine.ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SimpleBoatEngine.ID);

    public static final RegistryObject<Item> ENGINE = ITEMS.register("engine",
        () -> new EngineItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> FUEL_BUCKET = ITEMS.register("fuel_bucket",
        () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> FUEL_CANISTER = ITEMS.register("fuel_canister",
        () -> new EngineItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> POLE_LIGHT = ITEMS.register("pole_light",
        () -> new EngineItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = TABS.register("main",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.simpleboatengine"))
            .icon(() -> new ItemStack(ENGINE.get()))
            .displayItems((parameters, output) -> {
                output.accept(ENGINE.get());
                output.accept(FUEL_BUCKET.get());
                output.accept(FUEL_CANISTER.get());
                output.accept(POLE_LIGHT.get());
            }).build());

    private EngineItems() {}
}
