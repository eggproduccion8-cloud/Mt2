package com.mundodetronos2.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/mundo_de_tronos2.json");

    private static ConfigData instance = new ConfigData();

    public static class ConfigData {
        public int maxPlayersPerRealm = 20;
        public int defaultMaxLives = 5;
        public int throneCooldownMinutes = 5;
        public List<String> allowedThroneBlocks = new ArrayList<>(Arrays.asList(
            "minecraft:anvil",
            "minecraft:chipped_anvil",
            "minecraft:damaged_anvil",
            "minecraft:gold_block",
            "minecraft:quartz_block",
            "minecraft:lodestone",
            "minecraft:netherite_block"
        ));
        public String hitSound = "minecraft:block.anvil.place";
        public String repairSound = "minecraft:block.bell.use";
        public boolean debugMode = false;
        public int targetBarSinksSeconds = 4;

        // Configuración de XP por derrotar Mobs
        public boolean enableMobKillXp = true;
        public int defaultMobKillXp = 15;
        public int bossMobKillXp = 100;
    }

    public static void load() {
        try {
            if (!CONFIG_FILE.getParentFile().exists()) {
                CONFIG_FILE.getParentFile().mkdirs();
            }
            if (CONFIG_FILE.exists()) {
                try (FileReader reader = new FileReader(CONFIG_FILE)) {
                    ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
                    if (loaded != null) {
                        instance = loaded;
                        LOGGER.info("Configuración cargada correctamente.");
                        return;
                    }
                }
            }
            // Si no existe o falla, guardar los valores por defecto
            save();
        } catch (Exception e) {
            LOGGER.error("Error cargando la configuración, usando valores por defecto", e);
        }
    }

    public static void save() {
        try {
            if (!CONFIG_FILE.getParentFile().exists()) {
                CONFIG_FILE.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(instance, writer);
                LOGGER.info("Configuración guardada correctamente.");
            }
        } catch (Exception e) {
            LOGGER.error("Error guardando la configuración", e);
        }
    }

    public static ConfigData get() {
        return instance;
    }

    public static boolean isBlockAllowedForThrone(String blockId) {
        if (blockId == null) return false;
        // Limpiar el ID
        if (!blockId.contains(":")) {
            blockId = "minecraft:" + blockId;
        }
        return instance.allowedThroneBlocks.contains(blockId);
    }
}
