package com.mundodetronos2.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mundodetronos2.player.PlayerRealmData;
import com.mundodetronos2.realm.InviteData;
import com.mundodetronos2.realm.RealmData;
import com.mundodetronos2.throne.ThroneData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SaveManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File BASE_DIR = new File("world/mundo_de_tronos2");

    private static final File REALMS_FILE = new File(BASE_DIR, "realms.json");
    private static final File THRONES_FILE = new File(BASE_DIR, "thrones.json");
    private static final File PLAYERS_FILE = new File(BASE_DIR, "players.json");
    private static final File INVITES_FILE = new File(BASE_DIR, "invites.json");
    private static final File VERSION_FILE = new File(BASE_DIR, "version.json");

    private static final ExecutorService WRITE_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "MundoDeTronos-SaveThread");
        thread.setDaemon(true);
        return thread;
    });

    private static boolean isDirty = false;

    public static void markDirty() {
        isDirty = true;
    }

    public static boolean isDirty() {
        return isDirty;
    }

    public static void saveAll(
            Map<UUID, RealmData> realms,
            Map<UUID, ThroneData> thrones,
            Map<UUID, PlayerRealmData> players,
            Map<UUID, InviteData> invites,
            boolean forceSync
    ) {
        if (!isDirty && !forceSync) {
            return;
        }

        // Serializar a JSON string en el hilo principal para evitar ConcurrentModificationException
        final String realmsJson;
        final String thronesJson;
        final String playersJson;
        final String invitesJson;
        final String versionJson;

        synchronized (SaveManager.class) {
            realmsJson = GSON.toJson(realms);
            thronesJson = GSON.toJson(thrones);
            playersJson = GSON.toJson(players);
            invitesJson = GSON.toJson(invites);

            Map<String, String> ver = new HashMap<>();
            ver.put("version", "1.0.0");
            versionJson = GSON.toJson(ver);
        }

        isDirty = false;

        Runnable writeTask = () -> {
            try {
                if (!BASE_DIR.exists()) {
                    BASE_DIR.mkdirs();
                }
                writeFile(REALMS_FILE, realmsJson);
                writeFile(THRONES_FILE, thronesJson);
                writeFile(PLAYERS_FILE, playersJson);
                writeFile(INVITES_FILE, invitesJson);
                writeFile(VERSION_FILE, versionJson);
            } catch (Exception e) {
                LOGGER.error("Error al escribir datos persistentes en segundo plano", e);
            }
        };

        if (forceSync) {
            writeTask.run();
        } else {
            WRITE_EXECUTOR.submit(writeTask);
        }
    }

    private static void writeFile(File file, String content) throws Exception {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }

    public static Map<UUID, RealmData> loadRealms() {
        if (!REALMS_FILE.exists()) return new HashMap<>();
        try (FileReader reader = new FileReader(REALMS_FILE)) {
            Type type = new TypeToken<HashMap<UUID, RealmData>>() {}.getType();
            Map<UUID, RealmData> data = GSON.fromJson(reader, type);
            return data != null ? data : new HashMap<>();
        } catch (Exception e) {
            LOGGER.error("Error al cargar realms.json, se creará uno nuevo", e);
            return new HashMap<>();
        }
    }

    public static Map<UUID, ThroneData> loadThrones() {
        if (!THRONES_FILE.exists()) return new HashMap<>();
        try (FileReader reader = new FileReader(THRONES_FILE)) {
            Type type = new TypeToken<HashMap<UUID, ThroneData>>() {}.getType();
            Map<UUID, ThroneData> data = GSON.fromJson(reader, type);
            return data != null ? data : new HashMap<>();
        } catch (Exception e) {
            LOGGER.error("Error al cargar thrones.json, se creará uno nuevo", e);
            return new HashMap<>();
        }
    }

    public static Map<UUID, PlayerRealmData> loadPlayers() {
        if (!PLAYERS_FILE.exists()) return new HashMap<>();
        try (FileReader reader = new FileReader(PLAYERS_FILE)) {
            Type type = new TypeToken<HashMap<UUID, PlayerRealmData>>() {}.getType();
            Map<UUID, PlayerRealmData> data = GSON.fromJson(reader, type);
            return data != null ? data : new HashMap<>();
        } catch (Exception e) {
            LOGGER.error("Error al cargar players.json, se creará uno nuevo", e);
            return new HashMap<>();
        }
    }

    public static Map<UUID, InviteData> loadInvites() {
        if (!INVITES_FILE.exists()) return new HashMap<>();
        try (FileReader reader = new FileReader(INVITES_FILE)) {
            Type type = new TypeToken<HashMap<UUID, InviteData>>() {}.getType();
            Map<UUID, InviteData> data = GSON.fromJson(reader, type);
            return data != null ? data : new HashMap<>();
        } catch (Exception e) {
            LOGGER.error("Error al cargar invites.json, se creará uno nuevo", e);
            return new HashMap<>();
        }
    }

    public static void shutdown() {
        WRITE_EXECUTOR.shutdown();
    }
}
