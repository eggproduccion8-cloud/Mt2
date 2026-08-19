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
        PLAYER_CARD("Perfil de Jugador"),
        KINGDOM_CARD("Estado del Reino"),
        CHAT_BOX("Chat MMORPG"),
        ARMOR_PANEL("Panel de Armadura"),
        HOTBAR("Hotbar MMORPG"),
        OFFHAND_SLOT("Segunda Mano"),
        XP_BAR("Barra de Experiencia");

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
        public int width = 120;
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
            case PLAYER_CARD:
                return new ComponentConfig(Anchor.TOP_LEFT, 10, 10, 1.0f, true, 140, 52);
            case KINGDOM_CARD:
                return new ComponentConfig(Anchor.TOP_RIGHT, -145, 10, 1.0f, true, 135, 48);
            case CHAT_BOX:
                return new ComponentConfig(Anchor.BOTTOM_LEFT, 10, -110, 1.0f, true, 200, 80);
            case ARMOR_PANEL:
                return new ComponentConfig(Anchor.TOP_LEFT, 10, 66, 1.0f, true, 88, 20);
            case HOTBAR:
                return new ComponentConfig(Anchor.BOTTOM_CENTER, 0, -28, 1.0f, true, 222, 22);
            case OFFHAND_SLOT:
                return new ComponentConfig(Anchor.BOTTOM_CENTER, -125, -28, 1.0f, true, 22, 22);
            case XP_BAR:
                return new ComponentConfig(Anchor.BOTTOM_CENTER, 0, -54, 1.0f, true, 222, 10);
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
