package com.mundodetronos2.missions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mundodetronos2.progression.ProgressionManager;
import com.mundodetronos2.realm.RealmData;
import com.mundodetronos2.realm.RealmManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
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

public class MissionManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final File MISSIONS_FILE = new File("world/mundo_de_tronos2/custom_missions.json");
    private static final File PROGRESS_FILE = new File("world/mundo_de_tronos2/missions_progress.json");

    private static final Map<String, Mission> customMissions = new ConcurrentHashMap<>();
    // Mapea ID de grupo (UUID de reino o jugador) a un mapa de sus progresos de misión <ID Misión, Progreso>
    private static final Map<UUID, Map<String, MissionProgress>> progressCache = new ConcurrentHashMap<>();

    private static final ExecutorService SAVE_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "MundoDeTronos-MissionsSaveThread");
        thread.setDaemon(true);
        return thread;
    });

    private static boolean isDirty = false;

    public static void init() {
        customMissions.clear();
        progressCache.clear();
        load();
    }

    public static void load() {
        // Cargar misiones personalizadas creadas por Admin
        if (MISSIONS_FILE.exists()) {
            try (FileReader reader = new FileReader(MISSIONS_FILE)) {
                Type type = new TypeToken<HashMap<String, Mission>>() {}.getType();
                Map<String, Mission> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    customMissions.putAll(loaded);
                    LOGGER.info("Cargadas {} misiones personalizadas.", customMissions.size());
                }
            } catch (Exception e) {
                LOGGER.error("Error al cargar custom_missions.json", e);
            }
        }

        // Cargar progresos de misiones
        if (PROGRESS_FILE.exists()) {
            try (FileReader reader = new FileReader(PROGRESS_FILE)) {
                Type type = new TypeToken<HashMap<UUID, HashMap<String, MissionProgress>>>() {}.getType();
                Map<UUID, Map<String, MissionProgress>> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    progressCache.putAll(loaded);
                    LOGGER.info("Cargados progresos de misiones para {} grupos.", progressCache.size());
                }
            } catch (Exception e) {
                LOGGER.error("Error al cargar missions_progress.json", e);
            }
        }
    }

    public static void save(boolean forceSync) {
        if (!isDirty && !forceSync) return;

        // Clonar datos en el hilo principal
        final String missionsJson;
        final String progressJson;
        synchronized (customMissions) {
            missionsJson = GSON.toJson(customMissions);
        }
        synchronized (progressCache) {
            progressJson = GSON.toJson(progressCache);
        }

        isDirty = false;

        Runnable writeTask = () -> {
            try {
                File dir = new File("world/mundo_de_tronos2");
                if (!dir.exists()) dir.mkdirs();

                try (FileWriter writer = new FileWriter(MISSIONS_FILE)) {
                    writer.write(missionsJson);
                }
                try (FileWriter writer = new FileWriter(PROGRESS_FILE)) {
                    writer.write(progressJson);
                }
            } catch (Exception e) {
                LOGGER.error("Error al guardar misiones de forma asíncrona", e);
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

    public static List<Mission> getAllMissions() {
        List<Mission> list = new ArrayList<>(MissionRegistry.getDefaults().values());
        list.addAll(customMissions.values());
        return list;
    }

    public static Mission getMission(String id) {
        if (id == null) return null;
        Mission m = customMissions.get(id.toLowerCase());
        if (m == null) m = MissionRegistry.getMission(id);
        return m;
    }

    public static void createCustomMission(Mission m) {
        if (m == null) return;
        customMissions.put(m.getId().toLowerCase(), m);
        isDirty = true;
        save(false);
    }

    public static void deleteCustomMission(String id) {
        if (id == null) return;
        customMissions.remove(id.toLowerCase());
        isDirty = true;
        save(false);
    }

    public static UUID getGroupId(ServerPlayer player) {
        if (player == null) return null;
        RealmData realm = RealmManager.getPlayerRealm(player.getUUID());
        return realm != null ? realm.getId() : player.getUUID();
    }

    public static Map<String, MissionProgress> getGroupProgressMap(UUID groupId) {
        return progressCache.computeIfAbsent(groupId, k -> new ConcurrentHashMap<>());
    }

    public static MissionProgress getProgress(UUID groupId, String missionId) {
        Map<String, MissionProgress> map = getGroupProgressMap(groupId);
        return map.computeIfAbsent(missionId, id -> new MissionProgress(id));
    }

    public static boolean acceptMission(ServerPlayer player, String missionId) {
        if (player == null || missionId == null) return false;

        UUID groupId = getGroupId(player);
        Map<String, MissionProgress> progressMap = getGroupProgressMap(groupId);

        // 1. Validar límite de misiones activas (Max 3)
        long activeCount = progressMap.values().stream().filter(p -> p.getStatus().equalsIgnoreCase("ACTIVE")).count();
        if (activeCount >= 3) {
            com.mundodetronos2.network.MessageManager.actionBar(player, "§c⚠ Límite de misiones activas alcanzado (Máx: 3).");
            return false;
        }

        // 2. Activar la misión
        MissionProgress progress = getProgress(groupId, missionId);
        if (!progress.getStatus().equalsIgnoreCase("AVAILABLE")) {
            com.mundodetronos2.network.MessageManager.actionBar(player, "§c⚠ Esta misión ya está activa o completada.");
            return false;
        }

        progress.setStatus("ACTIVE");
        progress.setCurrentCount(0);
        isDirty = true;
        save(false);

        com.mundodetronos2.network.MessageManager.actionBar(player, "§a✓ Misión grupal '" + getMission(missionId).getName() + "' aceptada.");
        return true;
    }

    public static void handleMobKill(ServerPlayer player, EntityType<?> type) {
        if (player == null || type == null) return;

        UUID groupId = getGroupId(player);
        Map<String, MissionProgress> map = getGroupProgressMap(groupId);
        String mobId = EntityType.getKey(type).toString();

        for (MissionProgress progress : map.values()) {
            if (progress.getStatus().equalsIgnoreCase("ACTIVE")) {
                Mission m = getMission(progress.getMissionId());
                if (m != null) {
                    if ((m.getType() == MissionType.KILL_MOBS || m.getType() == MissionType.KILL_SPECIFIC_MOB)
                            && m.getObjective() != null && m.getObjective().getId().equalsIgnoreCase(mobId)) {

                        progress.setCurrentCount(progress.getCurrentCount() + 1);
                        isDirty = true;

                        if (progress.getCurrentCount() >= m.getObjective().getTargetCount()) {
                            completeMissionOnServer(player, progress, m);
                        } else {
                            // Sincronizar progreso a miembros online
                            notifyProgressUpdate(player, m.getName(), progress.getCurrentCount(), m.getObjective().getTargetCount());
                        }
                    }
                }
            }
        }
        save(false);
    }

    public static boolean deliverItems(ServerPlayer player, String missionId) {
        if (player == null || missionId == null) return false;

        UUID groupId = getGroupId(player);
        MissionProgress progress = getProgress(groupId, missionId);
        if (!progress.getStatus().equalsIgnoreCase("ACTIVE")) {
            com.mundodetronos2.network.MessageManager.actionBar(player, "§c⚠ Esta misión no está activa.");
            return false;
        }

        Mission m = getMission(missionId);
        if (m == null || m.getType() != MissionType.COLLECT_ITEMS || m.getObjective() == null) {
            com.mundodetronos2.network.MessageManager.actionBar(player, "§c⚠ Esta misión no requiere entrega de objetos.");
            return false;
        }

        // 1. Escanear inventario del jugador
        int needed = m.getObjective().getTargetCount() - progress.getCurrentCount();
        if (needed <= 0) return false;

        String itemRegistryName = m.getObjective().getId();
        int totalFound = 0;

        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) {
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                if (id.equalsIgnoreCase(itemRegistryName)) {
                    totalFound += stack.getCount();
                }
            }
        }

        if (totalFound == 0) {
            com.mundodetronos2.network.MessageManager.actionBar(player, "§c⚠ No tienes los objetos requeridos en tu inventario.");
            return false;
        }

        // 2. Consumir del inventario físicamente
        int toTake = Math.min(needed, totalFound);
        int remainingToTake = toTake;

        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) {
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                if (id.equalsIgnoreCase(itemRegistryName)) {
                    int count = stack.getCount();
                    if (count <= remainingToTake) {
                        remainingToTake -= count;
                        stack.setCount(0);
                    } else {
                        stack.shrink(remainingToTake);
                        remainingToTake = 0;
                        break;
                    }
                }
            }
        }

        // 3. Sumar al progreso del reino
        progress.setCurrentCount(progress.getCurrentCount() + toTake);
        isDirty = true;

        player.serverLevel().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ITEM_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 1.0F);

        if (progress.getCurrentCount() >= m.getObjective().getTargetCount()) {
            completeMissionOnServer(player, progress, m);
        } else {
            notifyProgressUpdate(player, m.getName(), progress.getCurrentCount(), m.getObjective().getTargetCount());
        }

        save(false);
        return true;
    }

    private static void completeMissionOnServer(ServerPlayer player, MissionProgress progress, Mission m) {
        progress.setStatus("COMPLETED");
        isDirty = true;

        // Otorgar XP a todos los miembros online del reino/grupo
        RealmData realm = RealmManager.getPlayerRealm(player.getUUID());
        if (realm != null) {
            int pointsEarned = Math.max(10, m.getXpReward() / 5);
            realm.setSharedPoints(realm.getSharedPoints() + pointsEarned);
            com.mundodetronos2.realm.RealmManager.save(false);

            for (UUID memberId : realm.getMembers()) {
                ServerPlayer member = player.getServer().getPlayerList().getPlayer(memberId);
                if (member != null) {
                    ProgressionManager.addXp(memberId, m.getXpReward(), member);
                    com.mundodetronos2.network.MessageManager.chat(member, "§6★ ¡Misión grupal '" + m.getName() + "' completada! Todos reciben " + m.getXpReward() + " XP y el reino gana " + pointsEarned + " puntos compartidos. ★");
                }
            }
        } else {
            ProgressionManager.addXp(player.getUUID(), m.getXpReward(), player);
            com.mundodetronos2.network.MessageManager.chat(player, "§6★ ¡Misión '" + m.getName() + "' completada! Recibes " + m.getXpReward() + " XP de progreso. ★");
        }
    }

    private static void notifyProgressUpdate(ServerPlayer triggerPlayer, String missionName, int current, int target) {
        RealmData realm = RealmManager.getPlayerRealm(triggerPlayer.getUUID());
        if (realm != null) {
            for (UUID memberId : realm.getMembers()) {
                ServerPlayer member = triggerPlayer.getServer().getPlayerList().getPlayer(memberId);
                if (member != null) {
                    com.mundodetronos2.network.MessageManager.actionBar(member, "§eProgreso Gremio [" + missionName + "]: " + current + " / " + target);
                }
            }
        } else {
            com.mundodetronos2.network.MessageManager.actionBar(triggerPlayer, "§eProgreso Misión [" + missionName + "]: " + current + " / " + target);
        }
    }

    public static void resetGroupMissions(UUID groupId, ServerPlayer player) {
        progressCache.remove(groupId);
        isDirty = true;
        save(false);

        if (player != null) {
            com.mundodetronos2.network.MessageManager.actionBar(player, "§a✓ Misiones grupales restablecidas.");
        }
    }

    public static void shutdown() {
        save(true);
        SAVE_EXECUTOR.shutdown();
    }
}
