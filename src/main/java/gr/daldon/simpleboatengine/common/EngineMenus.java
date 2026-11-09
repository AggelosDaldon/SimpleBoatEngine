package gr.daldon.simpleboatengine.common;

import gr.daldon.simpleboatengine.SimpleBoatEngine;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class EngineMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, SimpleBoatEngine.ID);
    public static final RegistryObject<MenuType<BoatCustomizerMenu>> CUSTOMIZER =
        MENUS.register("boat_customizer", () -> IForgeMenuType.create(BoatCustomizerMenu::new));
    public static final RegistryObject<MenuType<BoatCargoMenu>> CARGO =
        MENUS.register("fishing_boat_cargo", () -> IForgeMenuType.create(BoatCargoMenu::new));
    public static final RegistryObject<MenuType<ParkedBoatCargoMenu>> PARKED_CARGO =
        MENUS.register("parked_boat_cargo",() -> IForgeMenuType.create(ParkedBoatCargoMenu::new));
    public static final RegistryObject<MenuType<BoatBuilderMenu>> BOAT_BUILDER =
        MENUS.register("boat_builder",()->IForgeMenuType.create(BoatBuilderMenu::new));

    private EngineMenus() {}
}
