package com.mundodetronos2.npc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mundodetronos2.entity.GoddessNPCEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NpcRegistryManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File BASE_DIR = new File("world/mundo_de_tronos2");
    private static final File NPCS_FILE = new File(BASE_DIR, "npcs.json");

    // Definition template for the 15 NPCs
    public static class NpcDefinition {
        public final String internalId;
        public final String defaultName;
        public final String defaultSkin;
        public final String npcType;
        public final boolean isTutorial;
        public final int tutorialOrder;
        public final int requiredLevel;
        public final String nextNpcId;
        public final String prevNpcId;
        public final String missionId;

        public NpcDefinition(String internalId, String defaultName, String defaultSkin, String npcType,
                             boolean isTutorial, int tutorialOrder, int requiredLevel,
                             String nextNpcId, String prevNpcId, String missionId) {
            this.internalId = internalId;
            this.defaultName = defaultName;
            this.defaultSkin = defaultSkin;
            this.npcType = npcType;
            this.isTutorial = isTutorial;
            this.tutorialOrder = tutorialOrder;
            this.requiredLevel = requiredLevel;
            this.nextNpcId = nextNpcId;
            this.prevNpcId = prevNpcId;
            this.missionId = missionId;
        }
    }

    // --- ESTRUCTURA MODULAR EXTENDIDA NpcConfig ---
    public static class NpcGeneralConfig {
        public String internalId = "manuel";
        public String name = "Manuel";
        public String skin = "Manuel";
        public String npcType = "manuel";
        public double scale = 1.0;
        public boolean invulnerable = true;
        public boolean hasCollision = true;
        public boolean visible = true;
        public double interactDistance = 4.0;
        public double nameDistance = 16.0;
        public String dimension = "minecraft:overworld";
        public double x;
        public double y;
        public double z;
        public float yRot;
        public float xRot;
    }

    public static class NpcRequirementConfig {
        public int requiredRpgLevel = 1;
        public int requiredCampaignLevel = 1;
        public String requiredMission = "";
        public String requiredNpc = "";
        public String requiredPreviousNpcCompleted = "";
        public String requiredRole = "";
        public String requiredRealm = "";
    }

    public static class NpcTradeData {
        public String id = "trade_1";
        public String input1 = "minecraft:iron_ingot";
        public int count1 = 10;
        public String input2 = "";
        public int count2 = 0;
        public String output = "minecraft:gold_ingot";
        public int outputCount = 1;
        public int maxUses = 100;
        public int currentUses = 0;
        public int pointCost = 0;
        public int minLevel = 1;
        public String requiredRole = "";
        public String requiredMission = "";
        public boolean active = true;
    }

    public static class NpcTradeConfig {
        public List<NpcTradeData> trades = new ArrayList<>();
    }

    public static class NpcSlotData {
        public int slot = 0;
        public String item = "minecraft:air";
        public int count = 1;
        public String action = "NONE";
        public String requirement = "";
        public boolean visible = true;
        public boolean interactive = true;
    }

    public static class NpcSlotConfig {
        public List<NpcSlotData> slots = new ArrayList<>();
    }

    public static class NpcBehaviorConfig {
        public boolean allowMovement = true;
        public double radius = 4.0;
        public double speed = 0.2;
        public boolean lookAtPlayers = true;
        public double lookDistance = 6.0;
        public boolean lookInDialogue = true;
        public boolean returnToHome = true;
        public int pauseTicks = 100;
    }

    public static class NpcConfig {
        public String uuid;
        public NpcGeneralConfig general = new NpcGeneralConfig();
        public NpcRequirementConfig requirements = new NpcRequirementConfig();
        public NpcTradeConfig trades = new NpcTradeConfig();
        public NpcSlotConfig slots = new NpcSlotConfig();
        public NpcBehaviorConfig behavior = new NpcBehaviorConfig();
        public List<String> missionIds = new ArrayList<>();

        public NpcConfig() {}

        public NpcConfig(UUID uuid, NpcDefinition def, String customName, double x, double y, double z, float yRot, float xRot, String dimension) {
            this.uuid = uuid.toString();
            this.general.internalId = def.internalId;
            this.general.name = customName != null ? customName : def.defaultName;
            this.general.skin = def.defaultSkin;
            this.general.npcType = def.npcType;
            this.general.x = x;
            this.general.y = y;
            this.general.z = z;
            this.general.yRot = yRot;
            this.general.xRot = xRot;
            this.general.dimension = dimension;

            this.requirements.requiredCampaignLevel = def.requiredLevel;
            this.requirements.requiredRpgLevel = 1;
            if (def.missionId != null && !def.missionId.isEmpty()) {
                this.missionIds.add(def.missionId);
            }
        }
    }

    private static final Map<String, NpcDefinition> CENTRAL_REGISTRY = new LinkedHashMap<>();
    private static final Map<UUID, NpcConfig> PERSISTENT_NPCS = new ConcurrentHashMap<>();

    static {
        // 9 Tutorial Campaign NPCs in mandatory order (1 -> 9)
        registerDef(new NpcDefinition("manuel", "Manuel", "Manuel", "manuel", true, 1, 1, "laura", null, "tut_manuel"));
        registerDef(new NpcDefinition("laura", "Laura", "Laura", "laura", true, 2, 2, "oscar", "manuel", "tut_laura"));
        registerDef(new NpcDefinition("oscar", "Oscar", "Oscar", "oscar", true, 3, 3, "samuel", "laura", "tut_oscar"));
        registerDef(new NpcDefinition("samuel", "Samuel", "Samuel", "samuel", true, 4, 4, "heraldo", "oscar", "tut_samuel"));
        registerDef(new NpcDefinition("heraldo", "Heraldo", "Heraldo", "heraldo", true, 5, 5, "guardia_rey", "samuel", "tut_heraldo"));
        registerDef(new NpcDefinition("guardia_rey", "Guardia del Rey", "Guardia", "guardia_rey", true, 6, 6, "sacerdote", "heraldo", "tut_guardia_rey"));
        registerDef(new NpcDefinition("sacerdote", "Sacerdote", "Sacerdote", "sacerdote", true, 7, 7, "capitan_arena", "guardia_rey", "tut_sacerdote"));
        registerDef(new NpcDefinition("capitan_arena", "Capitán de Arena", "Capitan", "capitan_arena", true, 8, 8, "maestro_cargas", "sacerdote", "tut_capitan_arena"));
        registerDef(new NpcDefinition("maestro_cargas", "Maestro de Cargas", "Maestro", "maestro_cargas", true, 9, 9, null, "capitan_arena", "tut_maestro_cargas"));

        // 6 Informative Lobby / RPG NPCs (10 -> 15)
        registerDef(new NpcDefinition("monje_destino", "Monje del Destino", "Oby1", "monje_destino", false, 0, 0, null, null, null));
        registerDef(new NpcDefinition("karla", "Karla", "Karla", "karla", false, 0, 0, null, null, null));
        registerDef(new NpcDefinition("diosa_maria", "Diosa María", "Wyldune", "diosa_maria", false, 0, 0, null, null, null));
        registerDef(new NpcDefinition("custodio_trono", "Custodio del Trono", "Grog", "custodio_trono", false, 0, 0, null, null, null));
        registerDef(new NpcDefinition("maestro_roles", "Maestro de Roles", "Alex", "maestro_roles", false, 0, 0, null, null, null));
        registerDef(new NpcDefinition("mercader_gremio", "Mercader del Gremio", "Steve", "mercader_gremio", false, 0, 0, null, null, null));

        // New Character NPCs
        registerDef(new NpcDefinition("adventurer", "Aventurero", "adventurer", "adventurer", false, 0, 0, null, null, null));
        registerDef(new NpcDefinition("king", "Rey", "king", "king", false, 0, 0, null, null, null));
        registerDef(new NpcDefinition("miner", "Minero", "miner", "miner", false, 0, 0, null, null, null));
        registerDef(new NpcDefinition("pirate", "Pirata", "pirate", "pirate", false, 0, 0, null, null, null));
    }

    private static void registerDef(NpcDefinition def) {
        CENTRAL_REGISTRY.put(def.internalId.toLowerCase(), def);
    }

    public static Map<String, NpcDefinition> getCentralRegistry() {
        return Collections.unmodifiableMap(CENTRAL_REGISTRY);
    }

    public static NpcDefinition getDefinition(String internalId) {
        if (internalId == null) return null;
        return CENTRAL_REGISTRY.get(internalId.toLowerCase().trim());
    }

    public static void init() {
        PERSISTENT_NPCS.clear();
        if (!NPCS_FILE.exists()) return;
        try (FileReader reader = new FileReader(NPCS_FILE)) {
            Type type = new TypeToken<HashMap<String, NpcConfig>>() {}.getType();
            Map<String, NpcConfig> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                for (Map.Entry<String, NpcConfig> entry : loaded.entrySet()) {
                    try {
                        UUID uuid = UUID.fromString(entry.getKey());
                        PERSISTENT_NPCS.put(uuid, entry.getValue());
                    } catch (Exception ignored) {}
                }
            }
            LOGGER.info("NpcRegistryManager cargó {} NPCs persistentes de npcs.json.", PERSISTENT_NPCS.size());
        } catch (Exception e) {
            LOGGER.error("Error al cargar npcs.json", e);
        }
    }

    public static void save() {
        try {
            if (!BASE_DIR.exists()) {
                BASE_DIR.mkdirs();
            }
            Map<String, NpcConfig> saveMap = new HashMap<>();
            for (Map.Entry<UUID, NpcConfig> entry : PERSISTENT_NPCS.entrySet()) {
                saveMap.put(entry.getKey().toString(), entry.getValue());
            }
            try (FileWriter writer = new FileWriter(NPCS_FILE)) {
                GSON.toJson(saveMap, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Error al guardar npcs.json", e);
        }
    }

    public static void registerNpcInstance(GoddessNPCEntity entity, NpcDefinition def, String customName) {
        NpcConfig data = new NpcConfig(
                entity.getUUID(),
                def,
                customName,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                entity.getYRot(),
                entity.getXRot(),
                entity.level().dimension().location().toString()
        );
        PERSISTENT_NPCS.put(entity.getUUID(), data);
        save();
    }

    public static void updateNpcConfig(UUID uuid, NpcConfig config) {
        if (uuid != null && config != null) {
            PERSISTENT_NPCS.put(uuid, config);
            save();
        }
    }

    public static NpcConfig getNpcConfig(UUID uuid) {
        return PERSISTENT_NPCS.get(uuid);
    }

    public static void removeNpcInstance(UUID uuid) {
        if (PERSISTENT_NPCS.remove(uuid) != null) {
            save();
        }
    }

    public static int removeAllInDimension(ServerLevel level) {
        String dimStr = level.dimension().location().toString();
        int count = 0;
        List<UUID> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, NpcConfig> entry : PERSISTENT_NPCS.entrySet()) {
            if (entry.getValue().general.dimension.equalsIgnoreCase(dimStr)) {
                toRemove.add(entry.getKey());
            }
        }

        for (UUID uuid : toRemove) {
            PERSISTENT_NPCS.remove(uuid);
            Entity e = level.getEntity(uuid);
            if (e != null) {
                e.discard();
                count++;
            }
        }

        for (Entity e : level.getAllEntities()) {
            if (e instanceof GoddessNPCEntity) {
                e.discard();
                count++;
            }
        }

        save();
        return count;
    }

    public static int resetAll(net.minecraft.server.MinecraftServer server) {
        int count = 0;
        if (server != null) {
            for (ServerLevel level : server.getAllLevels()) {
                for (Entity e : level.getAllEntities()) {
                    if (e instanceof GoddessNPCEntity) {
                        e.discard();
                        count++;
                    }
                }
            }
        }
        PERSISTENT_NPCS.clear();
        save();
        return count;
    }

    public static Map<UUID, NpcConfig> getPersistentNpcs() {
        return Collections.unmodifiableMap(PERSISTENT_NPCS);
    }
}
