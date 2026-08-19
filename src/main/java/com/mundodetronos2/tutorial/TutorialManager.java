package com.mundodetronos2.tutorial;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mundodetronos2.network.MessageManager;
import com.mundodetronos2.network.NetworkManager;
import com.mundodetronos2.progression.ProgressionManager;
import com.mundodetronos2.realm.RealmData;
import com.mundodetronos2.realm.RealmManager;
import com.mundodetronos2.role.PlayerRoleData;
import com.mundodetronos2.role.RoleManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.IronGolem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TutorialManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File BASE_DIR = new File("world/mundo_de_tronos2");
    private static final File TUTORIAL_FILE = new File(BASE_DIR, "tutorial_data.json");
    private static final File GUARDIA_CONFIG = new File(BASE_DIR, "guardia_rey.json");

    // Persistent Tutorial Data per Realm
    public static class RealmTutorialData {
        public UUID realmId;
        public int tutorialLevel = 1; // 1 to 10
        public int wheatCount = 0; // NPC 2 (Laura): 150 total
        public Set<UUID> oscarArmorClaimedMembers = new HashSet<>(); // NPC 3 (Oscar)
        public Set<UUID> samuelGolemHitMembers = new HashSet<>(); // NPC 4 (Samuel)
        public int heraldoStage = 0; // NPC 5 (Heraldo): 0=Wood, 1=Stone, 2=Obsidian, 3=Throne, 4=Done
        public Set<String> guardiaCheckpointsVisited = new HashSet<>(); // NPC 6 (Guardia del Rey)
        public Set<UUID> arenaParticipatedMembers = new HashSet<>(); // NPC 8 (Capitan de Arena)
        public boolean maestroTrialCompleted = false; // NPC 9 (Maestro de Cargas)
        public boolean throneClaimed = false; // Sacerdote Lvl 10

        public RealmTutorialData() {}

        public RealmTutorialData(UUID realmId) {
            this.realmId = realmId;
        }
    }

    // Config for Guardia del Rey checkpoints
    public static class CheckpointConfig {
        public String id;
        public String name;
        public String dimension = "minecraft:overworld";
        public double x;
        public double y;
        public double z;
        public double radius = 5.0;

        public CheckpointConfig() {}

        public CheckpointConfig(String id, String name, double x, double y, double z) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static final Map<UUID, RealmTutorialData> REALM_TUTORIAL_MAP = new ConcurrentHashMap<>();
    private static final List<CheckpointConfig> GUARDIA_CHECKPOINTS = new ArrayList<>();

    // Map of active Training Golems (golemEntityId -> realmId)
    private static final Map<Integer, UUID> GOLEM_REALM_MAP = new ConcurrentHashMap<>();

    public static void init() {
        REALM_TUTORIAL_MAP.clear();
        GUARDIA_CHECKPOINTS.clear();

        if (TUTORIAL_FILE.exists()) {
            try (FileReader reader = new FileReader(TUTORIAL_FILE)) {
                Type type = new TypeToken<HashMap<String, RealmTutorialData>>() {}.getType();
                Map<String, RealmTutorialData> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    for (Map.Entry<String, RealmTutorialData> entry : loaded.entrySet()) {
                        REALM_TUTORIAL_MAP.put(UUID.fromString(entry.getKey()), entry.getValue());
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error al cargar tutorial_data.json", e);
            }
        }

        if (GUARDIA_CONFIG.exists()) {
            try (FileReader reader = new FileReader(GUARDIA_CONFIG)) {
                Type type = new TypeToken<ArrayList<CheckpointConfig>>() {}.getType();
                List<CheckpointConfig> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    GUARDIA_CHECKPOINTS.addAll(loaded);
                }
            } catch (Exception e) {
                LOGGER.error("Error al cargar guardia_rey.json", e);
            }
        } else {
            // Default Guardia del Rey checkpoints in lobby
            GUARDIA_CHECKPOINTS.add(new CheckpointConfig("puerta_gremio", "Puerta del Gremio", 0, 64, 20));
            GUARDIA_CHECKPOINTS.add(new CheckpointConfig("fuente", "Fuente Central", 20, 64, 0));
            GUARDIA_CHECKPOINTS.add(new CheckpointConfig("plaza", "Plaza del Reino", -20, 64, 0));
            GUARDIA_CHECKPOINTS.add(new CheckpointConfig("entrada_arena", "Entrada de la Arena", 0, 64, -20));
            saveGuardiaConfig();
        }
    }

    public static void save() {
        try {
            if (!BASE_DIR.exists()) {
                BASE_DIR.mkdirs();
            }
            Map<String, RealmTutorialData> saveMap = new HashMap<>();
            for (Map.Entry<UUID, RealmTutorialData> entry : REALM_TUTORIAL_MAP.entrySet()) {
                saveMap.put(entry.getKey().toString(), entry.getValue());
            }
            try (FileWriter writer = new FileWriter(TUTORIAL_FILE)) {
                GSON.toJson(saveMap, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Error al guardar tutorial_data.json", e);
        }
    }

    private static void saveGuardiaConfig() {
        try {
            if (!BASE_DIR.exists()) {
                BASE_DIR.mkdirs();
            }
            try (FileWriter writer = new FileWriter(GUARDIA_CONFIG)) {
                GSON.toJson(GUARDIA_CHECKPOINTS, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Error al guardar guardia_rey.json", e);
        }
    }

    public static RealmTutorialData getTutorialData(UUID realmId) {
        if (realmId == null) return null;
        return REALM_TUTORIAL_MAP.computeIfAbsent(realmId, k -> new RealmTutorialData(k));
    }

    public static void syncPlayerLevelWithTeam(ServerPlayer player) {
        // Campaña y Nivel RPG están separados. Sólo sincronizamos la interfaz HUD.
        NetworkManager.syncHud(player);
    }

    public static void setTeamTutorialLevel(RealmData realm, int newLevel, net.minecraft.server.MinecraftServer server) {
        RealmTutorialData tut = getTutorialData(realm.getId());
        int oldLevel = tut.tutorialLevel;
        tut.tutorialLevel = Math.max(1, Math.min(10, newLevel));
        save();

        int campaignXpReward = 100; // XP otorgada al avanzar de etapa en la campaña

        for (UUID memberId : realm.getMembers()) {
            ServerPlayer sp = server.getPlayerList().getPlayer(memberId);
            if (sp != null) {
                // Otorgar XP de recompensa sin forzar el nivel RPG
                if (tut.tutorialLevel > oldLevel) {
                    ProgressionManager.addXp(sp.getUUID(), campaignXpReward, sp);
                }
                NetworkManager.syncHud(sp);
                MessageManager.actionBar(sp, "§a✦ ¡Tu equipo ha avanzado a la Etapa " + tut.tutorialLevel + " de la Campaña!");
                sp.serverLevel().playSound(null, sp.blockPosition(), net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.2F);
            } else if (tut.tutorialLevel > oldLevel) {
                ProgressionManager.addXp(memberId, campaignXpReward, null);
            }
        }
    }

    // --- NPC 1: MANUEL ---
    public static boolean completeManuel(ServerPlayer player) {
        RealmData realm = RealmManager.getPlayerRealm(player.getUUID());
        if (realm != null) {
            RealmTutorialData tut = getTutorialData(realm.getId());
            if (tut.tutorialLevel < 2) {
                setTeamTutorialLevel(realm, 2, player.getServer());
            }
        }
        MessageManager.actionBar(player, "✔ Manuel: Instrucción recibida. Ve al Gremio a hablar con Karla para formar tu Equipo.");
        return true;
    }

    // --- NPC 2: LAURA (150 Wheat Shared) ---
    public static boolean deliverWheatLaura(ServerPlayer player) {
        RealmData realm = RealmManager.getPlayerRealm(player.getUUID());
        if (realm == null) {
            MessageManager.actionBar(player, "⚠ Necesitas formar parte de un equipo.");
            return false;
        }

        RealmTutorialData tut = getTutorialData(realm.getId());
        if (tut.tutorialLevel < 2) {
            MessageManager.actionBar(player, "⚠ Tu equipo debe alcanzar el nivel 2.");
            return false;
        }

        if (tut.tutorialLevel >= 3) {
            MessageManager.actionBar(player, "⚠ Ya completaron la misión de Laura.");
            return true;
        }

        net.minecraft.world.item.Item wheatItem = net.minecraft.world.item.Items.WHEAT;
        int countInInv = 0;
        for (net.minecraft.world.item.ItemStack s : player.getInventory().items) {
            if (!s.isEmpty() && s.getItem() == wheatItem) {
                countInInv += s.getCount();
            }
        }

        if (countInInv <= 0) {
            MessageManager.actionBar(player, "⚠ No tienes trigo en tu inventario. Progreso: " + tut.wheatCount + " / 150");
            return false;
        }

        int needed = 150 - tut.wheatCount;
        int toTake = Math.min(needed, countInInv);

        int remainingToTake = toTake;
        for (net.minecraft.world.item.ItemStack s : player.getInventory().items) {
            if (!s.isEmpty() && s.getItem() == wheatItem) {
                int c = s.getCount();
                if (c <= remainingToTake) {
                    remainingToTake -= c;
                    s.setCount(0);
                } else {
                    s.shrink(remainingToTake);
                    remainingToTake = 0;
                    break;
                }
            }
        }

        tut.wheatCount += toTake;
        save();

        if (tut.wheatCount >= 150) {
            setTeamTutorialLevel(realm, 3, player.getServer());
            MessageManager.actionBar(player, "✔ ¡Misión del Trigo completada (150/150)! Nivel 3 alcanzado. Busquen a Oscar.");
            return true;
        } else {
            MessageManager.actionBar(player, "🌾 Trigo entregado. Progreso del equipo: " + tut.wheatCount + " / 150");
            return false;
        }
    }

    // --- NPC 3: OSCAR (Individual Leather Armor Claim) ---
    public static boolean claimOscarArmor(ServerPlayer player) {
        RealmData realm = RealmManager.getPlayerRealm(player.getUUID());
        if (realm == null) {
            MessageManager.actionBar(player, "⚠ Necesitas formar parte de un equipo.");
            return false;
        }

        RealmTutorialData tut = getTutorialData(realm.getId());
        if (tut.tutorialLevel < 3) {
            MessageManager.actionBar(player, "⚠ Tu equipo debe alcanzar el nivel 3.");
            return false;
        }

        if (tut.oscarArmorClaimedMembers.contains(player.getUUID())) {
            MessageManager.actionBar(player, "⚠ Ya recibiste esta recompensa.");
            return false;
        }

        // Deliver plain leather armor set
        net.minecraft.world.item.ItemStack helmet = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET);
        helmet.getOrCreateTag().putString("AuthorizedRole", "any");
        helmet.getOrCreateTag().putString("RoleItemID", "kit_inicial_oscar_helmet");
        helmet.setHoverName(net.minecraft.network.chat.Component.literal("§6Casco de Cuero Inicial"));

        net.minecraft.world.item.ItemStack chest = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_CHESTPLATE);
        chest.getOrCreateTag().putString("AuthorizedRole", "any");
        chest.getOrCreateTag().putString("RoleItemID", "kit_inicial_oscar_chest");
        chest.setHoverName(net.minecraft.network.chat.Component.literal("§6Peto de Cuero Inicial"));

        net.minecraft.world.item.ItemStack leggings = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_LEGGINGS);
        leggings.getOrCreateTag().putString("AuthorizedRole", "any");
        leggings.getOrCreateTag().putString("RoleItemID", "kit_inicial_oscar_leggings");
        leggings.setHoverName(net.minecraft.network.chat.Component.literal("§6Grebas de Cuero Inicial"));

        net.minecraft.world.item.ItemStack boots = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_BOOTS);
        boots.getOrCreateTag().putString("AuthorizedRole", "any");
        boots.getOrCreateTag().putString("RoleItemID", "kit_inicial_oscar_boots");
        boots.setHoverName(net.minecraft.network.chat.Component.literal("§6Botas de Cuero Inicial"));

        player.getInventory().add(helmet);
        player.getInventory().add(chest);
        player.getInventory().add(leggings);
        player.getInventory().add(boots);

        tut.oscarArmorClaimedMembers.add(player.getUUID());
        save();

        MessageManager.actionBar(player, "✔ Recibiste tu armadura inicial de Oscar.");

        // Check if ALL current team members have claimed
        boolean allClaimed = true;
        for (UUID mId : realm.getMembers()) {
            if (!tut.oscarArmorClaimedMembers.contains(mId)) {
                allClaimed = false;
                break;
            }
        }

        if (allClaimed && tut.tutorialLevel == 3) {
            setTeamTutorialLevel(realm, 4, player.getServer());
            MessageManager.actionBar(player, "✔ ¡Todos los miembros reclamaron su armadura! Nivel 4 alcanzado. Busquen a Samuel.");
        }

        return true;
    }

    // --- NPC 4: SAMUEL (Training Golem) ---
    public static boolean startSamuelGolem(ServerPlayer player) {
        RealmData realm = RealmManager.getPlayerRealm(player.getUUID());
        if (realm == null) {
            MessageManager.actionBar(player, "⚠ Necesitas formar parte de un equipo.");
            return false;
        }

        RealmTutorialData tut = getTutorialData(realm.getId());
        if (tut.tutorialLevel < 4) {
            MessageManager.actionBar(player, "⚠ Tu equipo debe alcanzar el nivel 4.");
            return false;
        }

        if (tut.tutorialLevel >= 5) {
            MessageManager.actionBar(player, "⚠ Ya completaron la prueba de Samuel.");
            return true;
        }

        ServerLevel level = player.serverLevel();
        // Check if golem already exists
        for (Map.Entry<Integer, UUID> entry : GOLEM_REALM_MAP.entrySet()) {
            if (entry.getValue().equals(realm.getId())) {
                net.minecraft.world.entity.Entity e = level.getEntity(entry.getKey());
                if (e != null && e.isAlive()) {
                    MessageManager.actionBar(player, "⚔ El Golem de Entrenamiento ya está activo cerca. ¡Golpéalo!");
                    return true;
                }
            }
        }

        // Spawn Training Golem near Samuel / Player
        IronGolem golem = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        if (golem != null) {
            golem.moveTo(player.getX() + 2.0D, player.getY(), player.getZ() + 2.0D, 0.0F, 0.0F);
            golem.setNoAi(true);
            golem.setCustomName(net.minecraft.network.chat.Component.literal("§6§lGolem de Entrenamiento"));
            golem.setCustomNameVisible(true);
            level.addFreshEntity(golem);

            GOLEM_REALM_MAP.put(golem.getId(), realm.getId());
            MessageManager.actionBar(player, "⚔ ¡Golem de Entrenamiento creado! Cada integrante debe golpearlo una vez.");
            return true;
        }
        return false;
    }

    public static void onGolemHit(ServerPlayer player, IronGolem golem) {
        UUID realmId = GOLEM_REALM_MAP.get(golem.getId());
        if (realmId == null) return;

        RealmData realm = RealmManager.getRealm(realmId);
        if (realm == null) return;

        RealmTutorialData tut = getTutorialData(realmId);
        if (tut.samuelGolemHitMembers.contains(player.getUUID())) {
            MessageManager.actionBar(player, "⚠ Ya participaste en la prueba del Golem.");
            return;
        }

        tut.samuelGolemHitMembers.add(player.getUUID());
        save();

        int totalMembers = realm.getMembers().size();
        int currentHits = tut.samuelGolemHitMembers.size();

        MessageManager.actionBar(player, "⚔ Participación registrada. Participantes: " + currentHits + " / " + totalMembers);

        boolean allParticipated = true;
        for (UUID mId : realm.getMembers()) {
            if (!tut.samuelGolemHitMembers.contains(mId)) {
                allParticipated = false;
                break;
            }
        }

        if (allParticipated && tut.tutorialLevel == 4) {
            setTeamTutorialLevel(realm, 5, player.getServer());
            MessageManager.actionBar(player, "✔ ¡Prueba del Acero completada! Nivel 5 alcanzado. Busquen a Heraldo.");
            golem.discard();
            GOLEM_REALM_MAP.remove(golem.getId());
        }
    }

    // --- NPC 5: HERALDO (Assault Charge Trial) ---
    public static boolean startHeraldoTrial(ServerPlayer player) {
        RealmData realm = RealmManager.getPlayerRealm(player.getUUID());
        if (realm == null) {
            MessageManager.actionBar(player, "⚠ Necesitas formar parte de un equipo.");
            return false;
        }

        RealmTutorialData tut = getTutorialData(realm.getId());
        if (tut.tutorialLevel < 5) {
            MessageManager.actionBar(player, "⚠ Tu equipo debe alcanzar el nivel 5.");
            return false;
        }

        if (tut.tutorialLevel >= 6) {
            MessageManager.actionBar(player, "⚠ Ya completaron la prueba de Heraldo.");
            return true;
        }

        setTeamTutorialLevel(realm, 6, player.getServer());
        MessageManager.actionBar(player, "✔ ¡Prueba de Cargas completada! Nivel 6 alcanzado. Busquen al Guardia del Rey.");
        return true;
    }

    // --- NPC 6: GUARDIA DEL REY (Lobby Exploration Checkpoints) ---
    public static void checkGuardiaExploration(ServerPlayer player) {
        RealmData realm = RealmManager.getPlayerRealm(player.getUUID());
        if (realm == null) return;

        RealmTutorialData tut = getTutorialData(realm.getId());
        if (tut.tutorialLevel != 6) return;

        String currentDim = player.level().dimension().location().toString();

        for (CheckpointConfig cp : GUARDIA_CHECKPOINTS) {
            if (!tut.guardiaCheckpointsVisited.contains(cp.id) && cp.dimension.equalsIgnoreCase(currentDim)) {
                double distSq = player.distanceToSqr(cp.x, cp.y, cp.z);
                if (distSq <= cp.radius * cp.radius) {
                    tut.guardiaCheckpointsVisited.add(cp.id);
                    save();

                    int visited = tut.guardiaCheckpointsVisited.size();
                    int total = GUARDIA_CHECKPOINTS.size();

                    MessageManager.actionBar(player, "📍 Punto visitado: " + cp.name + " (" + visited + " / " + total + ")");

                    if (visited >= total) {
                        setTeamTutorialLevel(realm, 7, player.getServer());
                        MessageManager.actionBar(player, "✔ ¡Misión 'Los Ojos del Reino' completada! Nivel 7 alcanzado. Busquen al Sacerdote.");
                    }
                    break;
                }
            }
        }
    }

    // --- NPC 7: SACERDOTE (Story / Legend) ---
    public static boolean completeSacerdoteTutorial(ServerPlayer player) {
        RealmData realm = RealmManager.getPlayerRealm(player.getUUID());
        if (realm == null) {
            MessageManager.actionBar(player, "⚠ Necesitas formar parte de un equipo.");
            return false;
        }

        RealmTutorialData tut = getTutorialData(realm.getId());
        if (tut.tutorialLevel < 7) {
            MessageManager.actionBar(player, "⚠ Tu equipo debe alcanzar el nivel 7.");
            return false;
        }

        if (tut.tutorialLevel >= 8) {
            MessageManager.actionBar(player, "⚠ Ya escucharon la leyenda de María.");
            return true;
        }

        setTeamTutorialLevel(realm, 8, player.getServer());
        MessageManager.actionBar(player, "✔ Sacerdote: El Capitán de Arena los espera. Nivel 8 alcanzado.");
        return true;
    }

    // --- NPC 8: CAPITÁN DE ARENA (Arena Trial) ---
    public static boolean startArenaTrial(ServerPlayer player) {
        RealmData realm = RealmManager.getPlayerRealm(player.getUUID());
        if (realm == null) {
            MessageManager.actionBar(player, "⚠ Necesitas formar parte de un equipo.");
            return false;
        }

        RealmTutorialData tut = getTutorialData(realm.getId());
        if (tut.tutorialLevel < 8) {
            MessageManager.actionBar(player, "⚠ Tu equipo debe alcanzar el nivel 8.");
            return false;
        }

        if (tut.tutorialLevel >= 9) {
            MessageManager.actionBar(player, "⚠ Ya completaron la prueba de la Arena.");
            return true;
        }

        tut.arenaParticipatedMembers.add(player.getUUID());
        save();

        int current = tut.arenaParticipatedMembers.size();
        int total = realm.getMembers().size();

        MessageManager.actionBar(player, "⚔ Participación registrada en Arena: " + current + " / " + total);

        boolean allIn = true;
        for (UUID mId : realm.getMembers()) {
            if (!tut.arenaParticipatedMembers.contains(mId)) {
                allIn = false;
                break;
            }
        }

        if (allIn && tut.tutorialLevel == 8) {
            setTeamTutorialLevel(realm, 9, player.getServer());
            MessageManager.actionBar(player, "✔ ¡Prueba de la Arena completada! Nivel 9 alcanzado. Busquen al Maestro de Cargas.");
        }

        return true;
    }

    // --- NPC 9: MAESTRO DE CARGAS (Final Siege Trial -> Lvl 10) ---
    public static boolean startFinalSiegeTrial(ServerPlayer player) {
        RealmData realm = RealmManager.getPlayerRealm(player.getUUID());
        if (realm == null) {
            MessageManager.actionBar(player, "⚠ Necesitas formar parte de un equipo.");
            return false;
        }

        RealmTutorialData tut = getTutorialData(realm.getId());
        if (tut.tutorialLevel < 9) {
            MessageManager.actionBar(player, "⚠ Tu equipo debe alcanzar el nivel 9.");
            return false;
        }

        if (tut.tutorialLevel >= 10) {
            MessageManager.actionBar(player, "⚠ Ya completaron el Tutorial. ¡Son Nivel 10!");
            return true;
        }

        tut.maestroTrialCompleted = true;
        setTeamTutorialLevel(realm, 10, player.getServer());

        // Broadcast level 10 completion screen dialog
        for (UUID mId : realm.getMembers()) {
            ServerPlayer sp = player.getServer().getPlayerList().getPlayer(mId);
            if (sp != null) {
                NetworkManager.sendToPlayer(new NetworkManager.S2CShowMessagePacket("🏆 NIVEL 10 — TUTORIAL COMPLETADO. Vayan con el Sacerdote a reclamar su Trono.", false), sp);
            }
        }
        return true;
    }

    // --- LEVEL 10 SACERDOTE: CLAIM LEADER THRONE ---
    public static boolean claimLeaderThrone(ServerPlayer player) {
        RealmData realm = RealmManager.getPlayerRealm(player.getUUID());
        if (realm == null) {
            MessageManager.actionBar(player, "⚠ Necesitas formar parte de un equipo.");
            return false;
        }

        RealmTutorialData tut = getTutorialData(realm.getId());
        if (tut.tutorialLevel < 10) {
            MessageManager.actionBar(player, "⚠ Tu equipo todavía no ha alcanzado el nivel 10.");
            return false;
        }

        if (!realm.getOwnerId().equals(player.getUUID())) {
            MessageManager.actionBar(player, "⚠ Debes ser el líder del equipo.");
            return false;
        }

        if (tut.throneClaimed) {
            MessageManager.actionBar(player, "⚠ Tu equipo ya reclamó el Trono.");
            return false;
        }

        if (realm.getThroneId() != null) {
            MessageManager.actionBar(player, "⚠ Tu equipo ya tiene un Trono registrado.");
            return false;
        }

        tut.throneClaimed = true;
        save();

        net.minecraft.world.item.ItemStack throneStack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHERITE_BLOCK);
        throneStack.getOrCreateTag().putBoolean("mundodetronos2:throne_item", true);
        throneStack.getOrCreateTag().putString("mundodetronos2:realm_id", realm.getId().toString());
        throneStack.setHoverName(net.minecraft.network.chat.Component.literal("§6§l♛ Trono de " + realm.getName()));

        net.minecraft.world.item.ItemStack guideBook = NetworkManager.createThroneGuideBook();

        if (!player.getInventory().add(throneStack)) {
            player.drop(throneStack, false);
        }
        if (!player.getInventory().add(guideBook)) {
            player.drop(guideBook, false);
        }

        player.serverLevel().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 1.1F);
        MessageManager.actionBar(player, "✔ ¡Has recibido el Trono y la Guía de tu Reino!");

        // Notify other team members
        for (UUID mId : realm.getMembers()) {
            if (!mId.equals(player.getUUID())) {
                ServerPlayer sp = player.getServer().getPlayerList().getPlayer(mId);
                if (sp != null) {
                    MessageManager.actionBar(sp, "👑 ¡Tu líder " + player.getGameProfile().getName() + " ha reclamado el Trono de tu Reino!");
                }
            }
        }

        return true;
    }
}
