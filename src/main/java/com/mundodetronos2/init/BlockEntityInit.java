package com.mundodetronos2.init;

import com.mundodetronos2.block.ThroneBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockEntityInit {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "mundodetronos2");

    public static final RegistryObject<BlockEntityType<ThroneBlockEntity>> THRONE_BE =
            BLOCK_ENTITIES.register("throne_be",
                    () -> BlockEntityType.Builder.of(ThroneBlockEntity::new, BlockInit.THRONE_BLOCK.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.mundodetronos2.block.CrateBlockEntity>> CRATE_BE =
            BLOCK_ENTITIES.register("crate_be",
                    () -> BlockEntityType.Builder.of(com.mundodetronos2.block.CrateBlockEntity::new, BlockInit.CRATE_BLOCK.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.mundodetronos2.block.CargaAsaltoBlockEntity>> CARGA_ASALTO_BE =
            BLOCK_ENTITIES.register("carga_asalto_be",
                    () -> BlockEntityType.Builder.of(com.mundodetronos2.block.CargaAsaltoBlockEntity::new, BlockInit.CARGA_ASALTO_BLOCK.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
