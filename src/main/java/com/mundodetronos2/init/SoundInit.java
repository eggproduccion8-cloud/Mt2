package com.mundodetronos2.init;

import com.mundodetronos2.MundoDeTronos2Mod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SoundInit {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MundoDeTronos2Mod.MODID);

    // Register placeholder sound events for NPC dialogues
    public static final RegistryObject<SoundEvent> NPC_MANUEL_001 = registerSound("npc_manuel_001");
    public static final RegistryObject<SoundEvent> NPC_MANUEL_002 = registerSound("npc_manuel_002");
    public static final RegistryObject<SoundEvent> NPC_MANUEL_003 = registerSound("npc_manuel_003");

    public static final RegistryObject<SoundEvent> NPC_LAURA_001 = registerSound("npc_laura_001");
    public static final RegistryObject<SoundEvent> NPC_LAURA_002 = registerSound("npc_laura_002");

    public static final RegistryObject<SoundEvent> NPC_OSCAR_001 = registerSound("npc_oscar_001");
    public static final RegistryObject<SoundEvent> NPC_OSCAR_002 = registerSound("npc_oscar_002");

    public static final RegistryObject<SoundEvent> NPC_SAMUEL_001 = registerSound("npc_samuel_001");
    public static final RegistryObject<SoundEvent> NPC_SAMUEL_002 = registerSound("npc_samuel_002");

    public static final RegistryObject<SoundEvent> NPC_HERALDO_001 = registerSound("npc_heraldo_001");
    public static final RegistryObject<SoundEvent> NPC_HERALDO_002 = registerSound("npc_heraldo_002");

    public static final RegistryObject<SoundEvent> NPC_GUARDIA_001 = registerSound("npc_guardia_001");

    public static final RegistryObject<SoundEvent> NPC_SACERDOTE_001 = registerSound("npc_sacerdote_001");
    public static final RegistryObject<SoundEvent> NPC_SACERDOTE_002 = registerSound("npc_sacerdote_002");

    public static final RegistryObject<SoundEvent> NPC_CAPITAN_001 = registerSound("npc_capitan_001");

    public static final RegistryObject<SoundEvent> NPC_MAESTRO_001 = registerSound("npc_maestro_001");

    private static RegistryObject<SoundEvent> registerSound(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MundoDeTronos2Mod.MODID, name)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
