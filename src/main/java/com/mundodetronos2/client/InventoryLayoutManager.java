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

public class InventoryLayoutManager {

    public enum Anchor {
        TOP_LEFT,
        TOP_CENTER,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_CENTER,
        BOTTOM_RIGHT,
        CENTER
    }

    public enum InventoryComponentId {
        MODEL_3D("Personaje 3D"),
        EQUIPMENT_SLOTS("Slots de Armadura y Offhand"),
        PLAYER_STATS("Atributos del Jugador"),
        PLAYER_INVENTORY_GRID("Inventario Real del Jugador"),
        CRAFTING_STATION_3X3("Mesa de Fabricación 3x3"),
        CRAFTING_CATALOG("Catálogo de Recetas"),
        MOCHILA_CONTAINER("Mochila de Aventurero"),
        LOGO_T2("Logo EGPRODUCCION / T2");

        private final String displayName;

        InventoryComponentId(String displayName) {
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
    private static final Map<InventoryComponentId, ComponentConfig> COMPONENTS = new EnumMap<>(InventoryComponentId.class);

    static {
        resetToDefaults();
        load();
    }

    public static Map<InventoryComponentId, ComponentConfig> getComponents() {
        return COMPONENTS;
    }

    public static ComponentConfig getConfig(InventoryComponentId id) {
        return COMPONENTS.computeIfAbsent(id, k -> getDefaultConfig(k));
    }

    public static void resetToDefaults() {
        COMPONENTS.clear();
        for (InventoryComponentId id : InventoryComponentId.values()) {
            COMPONENTS.put(id, getDefaultConfig(id));
        }
    }

    private static ComponentConfig getDefaultConfig(InventoryComponentId id) {
        switch (id) {
            case MODEL_3D:
                return new ComponentConfig(Anchor.CENTER, -120, 20, 2.0f, true, 80, 140);
            case EQUIPMENT_SLOTS:
                return new ComponentConfig(Anchor.CENTER, -250, -80, 1.0f, true, 110, 180);
            case PLAYER_STATS:
                return new ComponentConfig(Anchor.CENTER, 50, -110, 1.0f, true, 200, 70);
            case PLAYER_INVENTORY_GRID:
                return new ComponentConfig(Anchor.CENTER, 50, -20, 1.0f, true, 234, 110);
            case CRAFTING_STATION_3X3:
                return new ComponentConfig(Anchor.CENTER, -240, -80, 1.0f, true, 200, 120);
            case CRAFTING_CATALOG:
                return new ComponentConfig(Anchor.CENTER, 10, -110, 1.0f, true, 270, 220);
            case MOCHILA_CONTAINER:
                return new ComponentConfig(Anchor.CENTER, -117, -90, 1.0f, true, 234, 180);
            case LOGO_T2:
                return new ComponentConfig(Anchor.BOTTOM_LEFT, 15, -45, 3.0f, true, 120, 35);
            default:
                return new ComponentConfig(Anchor.CENTER, 0, 0, 1.0f, true, 100, 20);
        }
    }

    public static int getRenderX(InventoryComponentId id, int screenWidth, int screenHeight) {
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

    public static int getRenderY(InventoryComponentId id, int screenWidth, int screenHeight) {
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
        return new File(dir, "mundodetronos2_inventory_layout.json");
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
            Type type = new TypeToken<EnumMap<InventoryComponentId, ComponentConfig>>() {}.getType();
            Map<InventoryComponentId, ComponentConfig> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                COMPONENTS.putAll(loaded);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
