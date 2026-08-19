package com.mundodetronos2.init;

import com.mundodetronos2.entity.GoddessNPCEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EntityInit {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "mundodetronos2");

    public static final RegistryObject<EntityType<GoddessNPCEntity>> GODDESS_NPC = ENTITIES.register("goddess_npc",
            () -> EntityType.Builder.of(GoddessNPCEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .build("goddess_npc")
    );

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
