package com.mundodetronos2.init;

import com.mundodetronos2.block.RoleAnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockInit {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "mundodetronos2");

    public static final RegistryObject<Block> ROLE_ANVIL = BLOCKS.register("role_anvil",
            () -> new RoleAnvilBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F, 1200.0F)
                    .sound(SoundType.ANVIL)
            ));

    public static final RegistryObject<Block> MESA_HERRERO = BLOCKS.register("mesa_herrero",
            () -> new com.mundodetronos2.block.BlacksmithTableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F)
                    .sound(SoundType.WOOD)
            ));

    public static final RegistryObject<Block> CARGA_ASALTO_BLOCK = BLOCKS.register("carga_asalto_block",
            () -> new com.mundodetronos2.block.CargaAsaltoBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.CLAY)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
            ));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
