package com.mundodetronos2;

import com.mundodetronos2.commands.TronosCommand;
import com.mundodetronos2.init.ItemInit;
import com.mundodetronos2.network.NetworkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("mundodetronos2")
public class MundoDeTronos2Mod {
    public static final String MODID = "mundodetronos2";
    private static final Logger LOGGER = LogManager.getLogger();

    public MundoDeTronos2Mod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);

        // Registrar los bloques y items en el bus del mod
        com.mundodetronos2.init.BlockInit.register(modEventBus);
        ItemInit.register(modEventBus);
        com.mundodetronos2.init.ModMenuTypes.register(modEventBus);
        com.mundodetronos2.init.EntityInit.register(modEventBus);
        com.mundodetronos2.init.SoundInit.register(modEventBus);

        // Registrar la clase de comandos y lógica general en la Forge Event Bus
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("Mundo de Tronos 2 Mod inicializado sin registros duplicados.");
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkManager::registerPackets);
        LOGGER.info("Configuraciones comunes y paquetes de red registrados.");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        TronosCommand.register(event.getDispatcher());
        LOGGER.info("Comandos de Mundo de Tronos 2 registrados con éxito.");
    }
}
