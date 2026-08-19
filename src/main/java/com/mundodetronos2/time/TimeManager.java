package com.mundodetronos2.time;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.level.ServerPlayer;
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

public class TimeManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File TIME_FILE = new File("world/data/mundodetronos2_time.json");

    private static final Map<UUID, Integer> timeCache = new ConcurrentHashMap<>();

    public static int getPlaytimesCount() {
        return timeCache.size();
    }
    private static final ExecutorService SAVE_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MundoDeTronos-TimeSaveThread");
        t.setDaemon(true);
        return t;
    });

    private static boolean isDirty = false;

    public static class TimeContainer {
        public Map<UUID, PlayerTimeData> players = new HashMap<>();
    }

    public static class PlayerTimeData {
        public int remainingSeconds;

        public PlayerTimeData() {}
        public PlayerTimeData(int sec) {
            this.remainingSeconds = sec;
        }
    }

    public static void init() {
        timeCache.clear();
        load();
    }

    public static void load() {
        if (!TIME_FILE.exists()) {
            LOGGER.info("No se encontró mundodetronos2_time.json. Se creará uno nuevo.");
            return;
        }

        try (FileReader reader = new FileReader(TIME_FILE)) {
            Type type = new TypeToken<TimeContainer>() {}.getType();
            TimeContainer container = GSON.fromJson(reader, type);
            if (container != null && container.players != null) {
                for (Map.Entry<UUID, PlayerTimeData> entry : container.players.entrySet()) {
                    timeCache.put(entry.getKey(), entry.getValue().remainingSeconds);
                }
                LOGGER.info("TimeManager: {} registros de tiempo cargados correctamente.", timeCache.size());
            }
        } catch (Exception e) {
            LOGGER.error("Error al cargar mundodetronos2_time.json", e);
        }
    }

    public static void save(boolean forceSync) {
        if (!isDirty && !forceSync) return;

        TimeContainer container = new TimeContainer();
        synchronized (timeCache) {
            for (Map.Entry<UUID, Integer> entry : timeCache.entrySet()) {
                container.players.put(entry.getKey(), new PlayerTimeData(entry.getValue()));
            }
        }

        final String jsonContent = GSON.toJson(container);
        isDirty = false;

        Runnable writeTask = () -> {
            try {
                if (!TIME_FILE.getParentFile().exists()) {
                    TIME_FILE.getParentFile().mkdirs();
                }
                try (FileWriter writer = new FileWriter(TIME_FILE)) {
                    writer.write(jsonContent);
                }
            } catch (Exception e) {
                LOGGER.error("Error al guardar mundodetronos2_time.json de forma asíncrona", e);
            }
        };

        if (forceSync) {
            writeTask.run();
        } else {
            SAVE_EXECUTOR.submit(writeTask);
        }
    }

    public static int getRemainingSeconds(UUID uuid) {
        // Por defecto: 4 horas = 14400 segundos
        return timeCache.computeIfAbsent(uuid, id -> 14400);
    }

    public static void setRemainingSeconds(UUID uuid, int seconds) {
        timeCache.put(uuid, Math.max(0, seconds));
        isDirty = true;
    }

    public static void resetAllPlayers() {
        synchronized (timeCache) {
            for (UUID uuid : timeCache.keySet()) {
                timeCache.put(uuid, 14400);
            }
        }
        isDirty = true;
        save(false);
    }

    /**
     * Decrementa el tiempo de juego de los jugadores conectados.
     * Ejecutado 1 vez por segundo (cada 20 ticks) desde el ServerTickEvent.
     */
    public static void tick(net.minecraft.server.MinecraftServer server) {
        if (server == null) return;

        boolean changed = false;
        List<ServerPlayer> playersToKick = new ArrayList<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean isOp = player.hasPermissions(2);
            UUID uuid = player.getUUID();
            int remaining = getRemainingSeconds(uuid);

            if (remaining > 0) {
                remaining--;
                setRemainingSeconds(uuid, remaining);
                changed = true;

                if (remaining <= 0 && !isOp) {
                    playersToKick.add(player);
                }
            } else {
                if (!isOp) {
                    playersToKick.add(player);
                }
            }
        }

        // Expulsar a los jugadores cuyo tiempo ha expirado (eximiendo OPs)
        for (ServerPlayer player : playersToKick) {
            player.connection.disconnect(net.minecraft.network.chat.Component.literal("Tu tiempo se terminó."));
        }

        if (changed) {
            // Sincronizar periódicamente el HUD
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                com.mundodetronos2.network.NetworkManager.syncHud(player);
            }
        }
    }

    public static void shutdown() {
        save(true);
        SAVE_EXECUTOR.shutdown();
    }
}
