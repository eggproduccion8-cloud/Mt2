package com.mundodetronos2.init;

import com.mundodetronos2.item.RoleCardItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemInit {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "mundodetronos2");

    public static final RegistryObject<Item> ROLE_CARD = ITEMS.register("role_card",
            () -> new RoleCardItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ROLE_ANVIL = ITEMS.register("role_anvil",
            () -> new net.minecraft.world.item.BlockItem(com.mundodetronos2.init.BlockInit.ROLE_ANVIL.get(), new Item.Properties()));

    public static final RegistryObject<Item> MESA_HERRERO = ITEMS.register("mesa_herrero",
            () -> new net.minecraft.world.item.BlockItem(com.mundodetronos2.init.BlockInit.MESA_HERRERO.get(), new Item.Properties()));

    public static final RegistryObject<Item> CARGA_ASALTO_NIVEL1 = ITEMS.register("carga_asalto_nivel1",
            () -> new com.mundodetronos2.item.CargaAsaltoItem(1, new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> CARGA_ASALTO_NIVEL2 = ITEMS.register("carga_asalto_nivel2",
            () -> new com.mundodetronos2.item.CargaAsaltoItem(2, new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> CARGA_ASALTO = ITEMS.register("carga_asalto",
            () -> new com.mundodetronos2.item.CargaAsaltoItem(1, new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> TNT_BASE = ITEMS.register("tnt_base",
            () -> new Item(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> CARGA_ASALTO_BLOCK = ITEMS.register("carga_asalto_block",
            () -> new net.minecraft.world.item.BlockItem(com.mundodetronos2.init.BlockInit.CARGA_ASALTO_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> MEJORA_MOCHILA = ITEMS.register("mejora_mochila",
            () -> new com.mundodetronos2.item.MejoraMochilaItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> THRONE_ITEM = ITEMS.register("throne_item",
            () -> new net.minecraft.world.item.BlockItem(com.mundodetronos2.init.BlockInit.THRONE_BLOCK.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> CRATE_LEVEL1 = ITEMS.register("crate_level1",
            () -> new com.mundodetronos2.item.CrateBlockItem(com.mundodetronos2.init.BlockInit.CRATE_BLOCK.get(), 1, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> CRATE_LEVEL2 = ITEMS.register("crate_level2",
            () -> new com.mundodetronos2.item.CrateBlockItem(com.mundodetronos2.init.BlockInit.CRATE_BLOCK.get(), 2, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> CRATE_LEVEL3 = ITEMS.register("crate_level3",
            () -> new com.mundodetronos2.item.CrateBlockItem(com.mundodetronos2.init.BlockInit.CRATE_BLOCK.get(), 3, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> CRATE_LEVEL4 = ITEMS.register("crate_level4",
            () -> new com.mundodetronos2.item.CrateBlockItem(com.mundodetronos2.init.BlockInit.CRATE_BLOCK.get(), 4, new Item.Properties().stacksTo(64)));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
