package com.mundodetronos2.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "mundodetronos2", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class KeyInit {
    public static final String CATEGORY = "key.categories.mundodetronos2";

    public static final KeyMapping OPEN_GUI_KEY = new KeyMapping(
            "key.mundodetronos2.open_gui",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K, // Tecla K por defecto para el menú principal
            CATEGORY
    );

    public static final KeyMapping OPEN_SKILLS_KEY = new KeyMapping(
            "key.mundodetronos2.open_skills",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M, // Tecla M por defecto para el árbol de habilidades
            CATEGORY
    );

    public static final KeyMapping TOGGLE_HUD_KEY = new KeyMapping(
            "key.mundodetronos2.toggle_hud",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R, // Tecla R por defecto para ocultar/mostrar HUD
            CATEGORY
    );

    public static final KeyMapping EDIT_HUD_KEY = new KeyMapping(
            "key.mundodetronos2.edit_hud",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H, // Tecla H por defecto para abrir el editor de HUD
            CATEGORY
    );

    public static final KeyMapping OPEN_RPG_INVENTORY_KEY = new KeyMapping(
            "key.mundodetronos2.open_rpg_inventory",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_I, // Tecla I por defecto para abrir el Inventario MMORPG
            CATEGORY
    );

    public static final KeyMapping EDIT_INVENTORY_KEY = new KeyMapping(
            "key.mundodetronos2.edit_inventory",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Y, // Tecla Y por defecto para abrir el Editor de Inventario RPG
            CATEGORY
    );

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_GUI_KEY);
        event.register(OPEN_SKILLS_KEY);
        event.register(TOGGLE_HUD_KEY);
        event.register(EDIT_HUD_KEY);
        event.register(OPEN_RPG_INVENTORY_KEY);
        event.register(EDIT_INVENTORY_KEY);
    }

    @SubscribeEvent
    public static void onClientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraft.client.gui.screens.MenuScreens.register(
                com.mundodetronos2.init.ModMenuTypes.ADMIN_EQUIPMENT_MENU.get(),
                com.mundodetronos2.gui.AdminEquipmentScreen::new
            );
            net.minecraft.client.gui.screens.MenuScreens.register(
                com.mundodetronos2.init.ModMenuTypes.RPG_INVENTORY_MENU.get(),
                com.mundodetronos2.gui.RPGInventoryScreen::new
            );
        });
    }
}
