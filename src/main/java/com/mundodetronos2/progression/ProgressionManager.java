package com.mundodetronos2.progression;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProgressionManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final File PROGRESS_FILE = new File("world/mundo_de_tronos2/progression.json");
    private static final Map<UUID, PlayerProgressData> progressCache = new ConcurrentHashMap<>();

    private static final ExecutorService SAVE_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "MundoDeTronos-ProgressionSaveThread");
        thread.setDaemon(true);
        return thread;
    });

    private static boolean isDirty = false;

    public static class ProgressionContainer {
        public int dataVersion = 1;
        public Map<UUID, PlayerProgressData> progression = new HashMap<>();
    }

    public static void init() {
        progressCache.clear();
        load();
    }

    public static void load() {
        if (!PROGRESS_FILE.exists()) {
            LOGGER.info("No se encontró progression.json. Se creará uno nuevo.");
            return;
        }

        try (FileReader reader = new FileReader(PROGRESS_FILE)) {
            Type type = new TypeToken<ProgressionContainer>() {}.getType();
            ProgressionContainer container = GSON.fromJson(reader, type);
            if (container != null && container.progression != null) {
                progressCache.putAll(container.progression);
                LOGGER.info("ProgressionManager: {} progresos cargados correctamente (versión {}).", progressCache.size(), container.dataVersion);
            }
        } catch (Exception e) {
            LOGGER.error("Error al cargar progression.json", e);
        }
    }

    public static void save(boolean forceSync) {
        if (!isDirty && !forceSync) return;

        // Serializar a JSON string en el hilo principal para evitar ConcurrentModificationException
        ProgressionContainer container = new ProgressionContainer();
        synchronized (progressCache) {
            container.progression.putAll(progressCache);
        }
        final String jsonContent = GSON.toJson(container);
        isDirty = false;

        Runnable writeTask = () -> {
            try {
                if (!PROGRESS_FILE.getParentFile().exists()) {
                    PROGRESS_FILE.getParentFile().mkdirs();
                }
                try (FileWriter writer = new FileWriter(PROGRESS_FILE)) {
                    writer.write(jsonContent);
                }
            } catch (Exception e) {
                LOGGER.error("Error al guardar progression.json de forma asíncrona", e);
            }
        };

        if (forceSync) {
            writeTask.run();
        } else {
            SAVE_EXECUTOR.submit(writeTask);
        }
    }

    public static void markDirty() {
        isDirty = true;
    }

    public static PlayerProgressData getProgressData(UUID playerId) {
        return progressCache.computeIfAbsent(playerId, id -> new PlayerProgressData(id));
    }

    public static void addXp(UUID playerId, int amount, ServerPlayer player) {
        PlayerProgressData progressData = getProgressData(playerId);
        boolean leveledUp = LevelSystem.addXp(progressData, amount);
        isDirty = true;
        save(true);

        // Sincronizar el nivel con PlayerRoleData de RoleManager
        com.mundodetronos2.role.PlayerRoleData roleData = com.mundodetronos2.role.RoleManager.getPlayerRoleData(playerId);
        if (roleData.getLevel() != progressData.getLevel()) {
            roleData.setLevel(progressData.getLevel());
            com.mundodetronos2.role.RoleManager.save(true);
        }

        if (player != null) {
            if (leveledUp) {
                player.serverLevel().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                player.sendSystemMessage(Component.literal("§6★ ¡Has subido al nivel " + progressData.getLevel() + "! Obtienes 1 Skill Point. ★"));
            }
            com.mundodetronos2.network.NetworkManager.syncHud(player);
        }
    }

    public static void setLevelDirectly(UUID playerId, int newLevel, ServerPlayer player) {
        PlayerProgressData progressData = getProgressData(playerId);
        int oldLevel = progressData.getLevel();
        int targetLevel = Math.max(1, Math.min(100, newLevel));

        if (targetLevel > oldLevel) {
            int gained = targetLevel - oldLevel;
            progressData.setSkillPoints(progressData.getSkillPoints() + gained);
        }

        progressData.setLevel(targetLevel);
        progressData.setXp(0);
        isDirty = true;
        save(true);

        com.mundodetronos2.role.PlayerRoleData roleData = com.mundodetronos2.role.RoleManager.getPlayerRoleData(playerId);
        roleData.setLevel(targetLevel);
        com.mundodetronos2.role.RoleManager.save(true);

        if (player != null) {
            com.mundodetronos2.network.NetworkManager.syncHud(player);
            com.mundodetronos2.network.MessageManager.actionBar(player, "§a★ Nivel establecido a " + targetLevel);
        }
    }

    public static void shutdown() {
        save(true);
        SAVE_EXECUTOR.shutdown();
    }
}
