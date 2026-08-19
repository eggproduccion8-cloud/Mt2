package com.mundodetronos2.init;

import com.mundodetronos2.gui.AdminEquipmentMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, "mundodetronos2");

    public static final RegistryObject<MenuType<AdminEquipmentMenu>> ADMIN_EQUIPMENT_MENU = MENUS.register("admin_equipment_menu",
            () -> IForgeMenuType.create(AdminEquipmentMenu::new));

    public static final RegistryObject<MenuType<com.mundodetronos2.gui.RPGInventoryMenu>> RPG_INVENTORY_MENU = MENUS.register("rpg_inventory_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> new com.mundodetronos2.gui.RPGInventoryMenu(windowId, inv, data != null ? data.readInt() : 1)));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
