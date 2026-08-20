package com.mundodetronos2.events;

import com.mundodetronos2.entity.CustomNPCEntity;
import com.mundodetronos2.entity.GoddessNPCEntity;
import com.mundodetronos2.entity.SoldierEntity;
import com.mundodetronos2.init.EntityInit;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "mundodetronos2", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntityEvent {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(EntityInit.GODDESS_NPC.get(), GoddessNPCEntity.createAttributes().build());
        event.put(EntityInit.CUSTOM_NPC.get(), CustomNPCEntity.createAttributes().build());
        event.put(EntityInit.SOLDIER.get(), SoldierEntity.createSoldierAttributes().build());
    }
}
