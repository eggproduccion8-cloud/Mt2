package com.mundodetronos2.role;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class AltarManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File ALTAR_FILE = new File("world/mundo_de_tronos2/altar.json");

    private static AltarData registeredAltar = null;

    public static class AltarData {
        public int x;
        public int y;
        public int z;
        public String dimension;
        public String blockType;

        public AltarData() {}

        public AltarData(BlockPos pos, String dimension, String blockType) {
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
            this.dimension = dimension;
            this.blockType = blockType;
        }

        public BlockPos getPos() {
            return new BlockPos(x, y, z);
        }
    }

    public static void load() {
        if (!ALTAR_FILE.exists()) {
            LOGGER.info("No se encontró altar.json.");
            return;
        }

        try (FileReader reader = new FileReader(ALTAR_FILE)) {
            registeredAltar = GSON.fromJson(reader, AltarData.class);
            if (registeredAltar != null) {
                LOGGER.info("Altar registrado cargado en {} ({})", registeredAltar.getPos(), registeredAltar.dimension);
            }
        } catch (Exception e) {
            LOGGER.error("Error al cargar altar.json", e);
        }
    }

    public static void save() {
        try {
            if (!ALTAR_FILE.getParentFile().exists()) {
                ALTAR_FILE.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(ALTAR_FILE)) {
                GSON.toJson(registeredAltar, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Error al guardar altar.json", e);
        }
    }

    public static void registerAltar(BlockPos pos, String dimension, String blockType) {
        registeredAltar = new AltarData(pos, dimension, blockType);
        save();
    }

    public static boolean isAltar(BlockPos pos, String dimension) {
        if (registeredAltar == null || pos == null || dimension == null) {
            // Cargar de forma perezosa por si acaso
            load();
            if (registeredAltar == null) return false;
        }
        return registeredAltar.x == pos.getX() &&
               registeredAltar.y == pos.getY() &&
               registeredAltar.z == pos.getZ() &&
               registeredAltar.dimension.equalsIgnoreCase(dimension);
    }

    public static AltarData getRegisteredAltar() {
        if (registeredAltar == null) {
            load();
        }
        return registeredAltar;
    }
}
