package com.mundodetronos2.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.EnumMap;
import java.util.Map;

public class HudLayoutManager {

    public enum Anchor {
        TOP_LEFT,
        TOP_CENTER,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_CENTER,
        BOTTOM_RIGHT,
        CENTER
    }

    public enum ComponentId {
        // Individual fine-grained HUD components
        HEAD_AVATAR("Retrato del Jugador"),
        PLAYER_NAME("Nombre del Jugador"),
        HEALTH_BAR("Barra de Vida"),
        HUNGER_BAR("Barra de Hambre"),
        ABSORPTION_BAR("Barra de Absorción"),
        WATER_BREATHING("Respiración de Agua"),
        TEAM_LIVES("Vidas del Equipo ♥"),
        THRONE_LIVES("Vidas del Trono ♛"),
        GAME_TIME("Tiempo ◷"),
        TEAM_NAME("Team ⚔"),
        PLAYER_ROLE("Rol del Jugador ◆"),
        COMPASS_COORDS("Brújula y Coordenadas"),
        ACTIVE_MISSION("Misión Activa"),
        HOTBAR("Hotbar MMORPG"),
        OFFHAND_SLOT("Segunda Mano"),
        XP_BAR("Barra de Experiencia"),
        LOGO_EGG("Logo EGPRODUCCION"),

        // Composite / Legacy components
        PLAYER_CARD("Perfil de Jugador (Tarjeta)"),
        KINGDOM_CARD("Estado del Reino (Tarjeta)"),
        CHAT_BOX("Chat MMORPG"),
        ARMOR_PANEL("Panel de Armadura"),
        RPG_INVENTORY("Inventario RPG (Tecla I)");

        private final String displayName;

        ComponentId(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static class ComponentConfig {
        public Anchor anchor;
        public int offsetX;
        public int offsetY;
        public float scale = 1.0f;
        public boolean visible = true;
        public int width = 100;
        public int height = 20;

        public ComponentConfig() {}

        public ComponentConfig(Anchor anchor, int offsetX, int offsetY, float scale, boolean visible, int width, int height) {
            this.anchor = anchor;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.scale = scale;
            this.visible = visible;
            this.width = width;
            this.height = height;
        }

        public ComponentConfig copy() {
            return new ComponentConfig(anchor, offsetX, offsetY, scale, visible, width, height);
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<ComponentId, ComponentConfig> COMPONENTS = new EnumMap<>(ComponentId.class);

    static {
        resetToDefaults();
        load();
    }

    public static Map<ComponentId, ComponentConfig> getComponents() {
        return COMPONENTS;
    }

    public static ComponentConfig getConfig(ComponentId id) {
        return COMPONENTS.computeIfAbsent(id, k -> getDefaultConfig(k));
    }

    public static void resetToDefaults() {
        COMPONENTS.clear();
        for (ComponentId id : ComponentId.values()) {
            COMPONENTS.put(id, getDefaultConfig(id));
        }
    }

    private static ComponentConfig getDefaultConfig(ComponentId id) {
        switch (id) {
            case HEAD_AVATAR:
                return new ComponentConfig(Anchor.TOP_LEFT, 10, 10, 1.0f, true, 32, 32);
            case PLAYER_NAME:
                return new ComponentConfig(Anchor.TOP_LEFT, 48, 10, 1.0f, true, 100, 12);
            case HEALTH_BAR:
                return new ComponentConfig(Anchor.TOP_LEFT, 48, 24, 1.0f, true, 120, 10);
            case HUNGER_BAR:
                return new ComponentConfig(Anchor.TOP_LEFT, 48, 36, 1.0f, true, 120, 8);
            case ABSORPTION_BAR:
                return new ComponentConfig(Anchor.TOP_LEFT, 48, 46, 1.0f, true, 120, 8);
            case WATER_BREATHING:
                return new ComponentConfig(Anchor.TOP_LEFT, 48, 56, 1.0f, true, 120, 8);
            case TEAM_LIVES:
                return new ComponentConfig(Anchor.TOP_LEFT, 10, 70, 1.0f, true, 60, 16);
            case THRONE_LIVES:
                return new ComponentConfig(Anchor.TOP_LEFT, 75, 70, 1.0f, true, 50, 16);
            case GAME_TIME:
                return new ComponentConfig(Anchor.TOP_LEFT, 130, 70, 1.0f, true, 70, 16);
            case TEAM_NAME:
                return new ComponentConfig(Anchor.TOP_LEFT, 205, 70, 1.0f, true, 80, 16);
            case PLAYER_ROLE:
                return new ComponentConfig(Anchor.TOP_LEFT, 290, 70, 1.0f, true, 90, 16);
            case COMPASS_COORDS:
                return new ComponentConfig(Anchor.TOP_CENTER, -60, 10, 1.0f, true, 120, 28);
            case ACTIVE_MISSION:
                return new ComponentConfig(Anchor.TOP_RIGHT, -160, 10, 1.0f, true, 150, 40);
            case HOTBAR:
                return new ComponentConfig(Anchor.BOTTOM_CENTER, -111, -28, 1.0f, true, 222, 22);
            case OFFHAND_SLOT:
                return new ComponentConfig(Anchor.BOTTOM_CENTER, -138, -28, 1.0f, true, 22, 22);
            case XP_BAR:
                return new ComponentConfig(Anchor.BOTTOM_CENTER, -111, -38, 1.0f, true, 222, 8);
            case LOGO_EGG:
                return new ComponentConfig(Anchor.BOTTOM_LEFT, 10, -35, 1.0f, true, 110, 28);
            case PLAYER_CARD:
                return new ComponentConfig(Anchor.TOP_LEFT, 10, 10, 1.0f, true, 175, 55);
            case KINGDOM_CARD:
                return new ComponentConfig(Anchor.TOP_RIGHT, -145, 10, 1.0f, true, 135, 48);
            case CHAT_BOX:
                return new ComponentConfig(Anchor.BOTTOM_LEFT, 10, -110, 1.0f, true, 200, 80);
            case ARMOR_PANEL:
                return new ComponentConfig(Anchor.TOP_LEFT, 10, 66, 1.0f, true, 88, 20);
            case RPG_INVENTORY:
                return new ComponentConfig(Anchor.CENTER, -120, -100, 1.0f, true, 240, 200);
            default:
                return new ComponentConfig(Anchor.TOP_LEFT, 0, 0, 1.0f, true, 100, 20);
        }
    }

    public static int getRenderX(ComponentId id, int screenWidth, int screenHeight) {
        return getRenderX(getConfig(id), screenWidth, screenHeight);
    }

    public static int getRenderX(ComponentConfig config, int screenWidth, int screenHeight) {
        int base = 0;
        switch (config.anchor) {
            case TOP_LEFT:
            case BOTTOM_LEFT:
                base = 0;
                break;
            case TOP_CENTER:
            case BOTTOM_CENTER:
            case CENTER:
                base = screenWidth / 2;
                break;
            case TOP_RIGHT:
            case BOTTOM_RIGHT:
                base = screenWidth;
                break;
        }
        return base + config.offsetX;
    }

    public static int getRenderY(ComponentId id, int screenWidth, int screenHeight) {
        return getRenderY(getConfig(id), screenWidth, screenHeight);
    }

    public static int getRenderY(ComponentConfig config, int screenWidth, int screenHeight) {
        int base = 0;
        switch (config.anchor) {
            case TOP_LEFT:
            case TOP_CENTER:
            case TOP_RIGHT:
                base = 0;
                break;
            case CENTER:
                base = screenHeight / 2;
                break;
            case BOTTOM_LEFT:
            case BOTTOM_CENTER:
            case BOTTOM_RIGHT:
                base = screenHeight;
                break;
        }
        return base + config.offsetY;
    }

    private static File getConfigFile() {
        File dir = new File(Minecraft.getInstance().gameDirectory, "config");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "mundodetronos2_hud_layout.json");
    }

    public static void save() {
        File file = getConfigFile();
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(COMPONENTS, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        File file = getConfigFile();
        if (!file.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<EnumMap<ComponentId, ComponentConfig>>() {}.getType();
            Map<ComponentId, ComponentConfig> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                COMPONENTS.putAll(loaded);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
