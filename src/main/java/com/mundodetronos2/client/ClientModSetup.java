package com.mundodetronos2.client;

import com.mundodetronos2.client.model.NPCModelRegistry;
import com.mundodetronos2.client.renderer.CustomNPCRenderer;
import com.mundodetronos2.init.EntityInit;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = "mundodetronos2", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(NPCModelRegistry::initClient);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityInit.GODDESS_NPC.get(), GoddessNPCRenderer::new);
        event.registerEntityRenderer(EntityInit.CUSTOM_NPC.get(), CustomNPCRenderer::new);
        event.registerEntityRenderer(EntityInit.SOLDIER.get(), CustomNPCRenderer::new);
    }
}
