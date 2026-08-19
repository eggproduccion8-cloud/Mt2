package com.mundodetronos2.role;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class PortalsManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File PORTALS_FILE = new File("world/mundo_de_tronos2/portals.json");

    private static PortalData registeredPortal = null;

    public static class PortalData {
        public int x;
        public int y;
        public int z;
        public String dimension;
        public String blockType;

        public PortalData() {}

        public PortalData(BlockPos pos, String dimension, String blockType) {
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
        if (!PORTALS_FILE.exists()) {
            LOGGER.info("No se encontró portals.json.");
            return;
        }

        try (FileReader reader = new FileReader(PORTALS_FILE)) {
            registeredPortal = GSON.fromJson(reader, PortalData.class);
            if (registeredPortal != null) {
                LOGGER.info("Portal registrado cargado en {} ({})", registeredPortal.getPos(), registeredPortal.dimension);
            }
        } catch (Exception e) {
            LOGGER.error("Error al cargar portals.json", e);
        }
    }

    public static void save() {
        try {
            if (!PORTALS_FILE.getParentFile().exists()) {
                PORTALS_FILE.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(PORTALS_FILE)) {
                GSON.toJson(registeredPortal, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Error al guardar portals.json", e);
        }
    }

    public static void registerPortal(BlockPos pos, String dimension, String blockType) {
        registeredPortal = new PortalData(pos, dimension, blockType);
        save();
    }

    public static boolean isPortal(BlockPos pos, String dimension) {
        if (registeredPortal == null || pos == null || dimension == null) {
            load();
            if (registeredPortal == null) return false;
        }
        return registeredPortal.x == pos.getX() &&
               registeredPortal.y == pos.getY() &&
               registeredPortal.z == pos.getZ() &&
               registeredPortal.dimension.equalsIgnoreCase(dimension);
    }

    public static PortalData getRegisteredPortal() {
        if (registeredPortal == null) {
            load();
        }
        return registeredPortal;
    }
}
