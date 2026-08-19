package com.mundodetronos2.role;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mundodetronos2.data.SaveManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RoleManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final File ROLES_FILE = new File("world/mundo_de_tronos2/roles.json");
    private static final Map<UUID, PlayerRoleData> rolesCache = new ConcurrentHashMap<>();

    private static final ExecutorService SAVE_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "MundoDeTronos-RoleSaveThread");
        thread.setDaemon(true);
        return thread;
    });

    private static boolean isDirty = false;

    public static boolean isDirty() {
        return isDirty;
    }

    public static class RoleContainer {
        public int dataVersion = 1;
        public Map<UUID, PlayerRoleData> roles = new HashMap<>();
    }

    public static void init() {
        rolesCache.clear();
        load();
    }

    public static void load() {
        if (!ROLES_FILE.exists()) {
            LOGGER.info("No se encontró roles.json. Se creará uno nuevo.");
            return;
        }

        try (FileReader reader = new FileReader(ROLES_FILE)) {
            Type type = new TypeToken<RoleContainer>() {}.getType();
            RoleContainer container = GSON.fromJson(reader, type);
            if (container != null && container.roles != null) {
                rolesCache.putAll(container.roles);
                LOGGER.info("RoleManager: {} roles cargados correctamente (versión {}).", rolesCache.size(), container.dataVersion);
            }
        } catch (Exception e) {
            LOGGER.error("Error al cargar roles.json", e);
        }
    }

    public static void save(boolean forceSync) {
        if (!isDirty && !forceSync) return;

        // Serializar a JSON string en el hilo principal para evitar ConcurrentModificationException
        RoleContainer container = new RoleContainer();
        synchronized (rolesCache) {
            container.roles.putAll(rolesCache);
        }
        final String jsonContent = GSON.toJson(container);
        isDirty = false;

        Runnable writeTask = () -> {
            try {
                if (!ROLES_FILE.getParentFile().exists()) {
                    ROLES_FILE.getParentFile().mkdirs();
                }
                try (FileWriter writer = new FileWriter(ROLES_FILE)) {
                    writer.write(jsonContent);
                }
            } catch (Exception e) {
                LOGGER.error("Error al guardar roles.json de forma asíncrona", e);
            }
        };

        if (forceSync) {
            writeTask.run();
        } else {
            SAVE_EXECUTOR.submit(writeTask);
        }
    }

    public static PlayerRoleData getPlayerRoleData(UUID playerId) {
        return rolesCache.computeIfAbsent(playerId, id -> new PlayerRoleData(id, PlayerRole.NONE));
    }

    public static void setPlayerRole(UUID playerId, PlayerRole role) {
        PlayerRoleData data = getPlayerRoleData(playerId);
        data.setRole(role);
        data.setSelectedAt(System.currentTimeMillis());
        isDirty = true;
        save(false);
    }

    public static void resetPlayerRole(UUID playerId) {
        PlayerRoleData data = getPlayerRoleData(playerId);
        data.setRole(PlayerRole.NONE);
        data.setLevel(1);
        isDirty = true;
        save(false);
    }

    public static Map<UUID, PlayerRoleData> getRolesCache() {
        return rolesCache;
    }

    public static void shutdown() {
        save(true);
        SAVE_EXECUTOR.shutdown();
    }
}
