package com.mundodetronos2.dialogue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NpcDialogueManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File DIALOGUE_DIR = new File("world/mundo_de_tronos2/npc_dialogues");

    private static final Map<String, Map<String, DialogueNode>> dialoguesByNpcType = new ConcurrentHashMap<>();

    public static void init() {
        dialoguesByNpcType.clear();
        if (!DIALOGUE_DIR.exists()) {
            DIALOGUE_DIR.mkdirs();
        }

        File[] files = DIALOGUE_DIR.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null && files.length > 0) {
            load();
        } else {
            populateDefaults();
            save();
        }
    }

    public static DialogueNode getNode(String npcType, String nodeId) {
        String typeLower = normalizeType(npcType);
        Map<String, DialogueNode> nodes = dialoguesByNpcType.get(typeLower);
        if (nodes != null) {
            return nodes.get(nodeId);
        }
        return null;
    }

    public static Map<String, DialogueNode> getOrCreateNpcTypeMap(String npcType) {
        String typeLower = normalizeType(npcType);
        return dialoguesByNpcType.computeIfAbsent(typeLower, k -> new ConcurrentHashMap<>());
    }

    public static String getSerializedNpcDialogues(String npcType) {
        String typeLower = normalizeType(npcType);
        Map<String, DialogueNode> nodes = dialoguesByNpcType.get(typeLower);
        return GSON.toJson(nodes != null ? nodes : new HashMap<String, DialogueNode>());
    }

    private static String normalizeType(String npcType) {
        if (npcType == null) return "diosa_maria";
        String lower = npcType.toLowerCase().trim();
        if (lower.equals("diosa")) return "diosa_maria";
        if (lower.equals("herrero")) return "samuel";
        if (lower.equals("guardian")) return "guardia_rojo";
        return lower;
    }

    public static void load() {
        File[] files = DIALOGUE_DIR.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File f : files) {
            String npcType = f.getName().substring(0, f.getName().length() - 5).toLowerCase();
            try (FileReader reader = new FileReader(f)) {
                Type type = new TypeToken<HashMap<String, DialogueNode>>() {}.getType();
                Map<String, DialogueNode> nodes = GSON.fromJson(reader, type);
                if (nodes != null) {
                    dialoguesByNpcType.put(npcType, new ConcurrentHashMap<>(nodes));
                }
            } catch (Exception e) {
                LOGGER.error("Error al cargar " + f.getName(), e);
            }
        }
        LOGGER.info("NpcDialogueManager: Cargados diálogos para {} tipos de NPC.", dialoguesByNpcType.size());
    }

    public static void save() {
        try {
            if (!DIALOGUE_DIR.exists()) {
                DIALOGUE_DIR.mkdirs();
            }
            for (Map.Entry<String, Map<String, DialogueNode>> entry : dialoguesByNpcType.entrySet()) {
                File file = new File(DIALOGUE_DIR, entry.getKey() + ".json");
                try (FileWriter writer = new FileWriter(file)) {
                    GSON.toJson(entry.getValue(), writer);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error al guardar npc_dialogues independientes", e);
        }
    }

    private static void populateDefaults() {
        // 1. MANUEL — HERRERO Y TUTORIAL INICIAL
        Map<String, DialogueNode> manuel = getOrCreateNpcTypeMap("manuel");

        DialogueNode m01 = new DialogueNode("inicio",
            "Bienvenido, viajero. Soy Manuel, el herrero del reino.\n\n" +
            "Has puesto los pies en una tierra donde ningún nombre está escrito en piedra y donde cada reino comienza con una sola decisión.\n\n" +
            "Aquí no basta con sobrevivir. Tendrás que construir y elegir tu equipo.",
            "npc_manuel_001");
        m01.addOption(new DialogueOption("¿Qué debo hacer?", "m_que_hacer", null));
        m01.addOption(new DialogueOption("¿Dónde encuentro un equipo?", "m_equipo", null));
        manuel.put("inicio", m01);

        DialogueNode mQueHacer = new DialogueNode("m_que_hacer",
            "Primero dirígete al Gremio a hablar con Karla.\n\n" +
            "Ella se encargará de mostrarte los equipos disponibles para que te unas a uno.",
            "npc_manuel_002");
        mQueHacer.addOption(new DialogueOption("Iré al Gremio con Karla.", "m_final", null));
        manuel.put("m_que_hacer", mQueHacer);

        DialogueNode mEquipo = new DialogueNode("m_equipo",
            "Existen 10 equipos principales representados por su color.\n\n" +
            "Cada equipo puede tener hasta 6 integrantes. Habla con Karla en el Gremio para unirte a uno.",
            "npc_manuel_003");
        mEquipo.addOption(new DialogueOption("Entendido, iré con Karla.", "m_final", null));
        manuel.put("m_equipo", mEquipo);

        DialogueNode mFinal = new DialogueNode("m_final",
            "Ve al Gremio y busca a Karla. Ella te guiará para elegir tu equipo.",
            "npc_manuel_001");
        mFinal.addOption(new DialogueOption("Voy al Gremio", "inicio", "COMPLETE_MANUEL"));
        manuel.put("m_final", mFinal);

        // 2. SAMUEL — PRIMER NPC DEL TUTORIAL
        Map<String, DialogueNode> samuel = getOrCreateNpcTypeMap("samuel");

        DialogueNode s01 = new DialogueNode("inicio",
            "Bienvenido, viajero.\n\n" +
            "Has puesto los pies en una tierra donde ningún nombre está escrito en piedra y donde cada reino comienza con una sola decisión.\n\n" +
            "Éste es Mundo de Tronos.\n\n" +
            "Aquí no basta con sobrevivir. Aquí tendrás que construir. Tendrás que elegir a quién confiar tu espalda. Y algún día tendrás que decidir qué bandera defenderás.",
            "npc_samuel_001");
        s01.addOption(new DialogueOption("¿Qué es Mundo de Tronos?", "s_que_es", null));
        s01.addOption(new DialogueOption("¿Qué debo hacer?", "s_que_hacer", null));
        s01.addOption(new DialogueOption("¿Y tú quién eres?", "s_quien_eres", null));
        samuel.put("inicio", s01);

        DialogueNode sQueEs = new DialogueNode("s_que_es",
            "Esta tierra está dividida entre alianzas, territorios y equipos.\n\n" +
            "Los grandes reinos no nacieron porque un solo guerrero fuera más fuerte que todos.\n\n" +
            "Nacieron porque varias personas decidieron luchar bajo el mismo estandarte.\n\n" +
            "Aquí podrás explorar, comerciar, cumplir misiones, mejorar tus habilidades y construir tu propia historia.\n\n" +
            "Pero antes de pensar en un trono, necesitas aprender a pertenecer a un equipo.",
            "npc_samuel_002");
        sQueEs.addOption(new DialogueOption("¿Entonces necesito un equipo?", "s_necesito_equipo", null));
        samuel.put("s_que_es", sQueEs);

        DialogueNode sNecesitoEquipo = new DialogueNode("s_necesito_equipo",
            "Exactamente.\n\n" +
            "Un guerrero solo puede sobrevivir.\n" +
            "Un equipo puede conquistar.\n\n" +
            "Pero escucha bien esto: no todos los que llevan el mismo color son tus amigos.\n\n" +
            "La confianza se gana con acciones.",
            "npc_samuel_003");
        sNecesitoEquipo.addOption(new DialogueOption("¿Qué debo hacer ahora?", "s_que_hacer", null));
        samuel.put("s_necesito_equipo", sNecesitoEquipo);

        DialogueNode sQueHacer = new DialogueNode("s_que_hacer",
            "Primero conocerás el Gremio.\n\n" +
            "El Gremio es el lugar donde los viajeros comienzan a encontrar su lugar en este mundo.\n\n" +
            "Allí conocerás a Karla.\n\n" +
            "Ella se encargará de explicarte cómo funcionan los equipos, las misiones y las herramientas que tendrás a tu disposición.",
            "npc_samuel_001");
        sQueHacer.addOption(new DialogueOption("¿Y después?", "s_despues", null));
        samuel.put("s_que_hacer", sQueHacer);

        DialogueNode sDespues = new DialogueNode("s_despues",
            "Después comienza realmente tu viaje.\n\n" +
            "No voy a decirte qué camino debes seguir.\n\n" +
            "Eso tendrás que descubrirlo tú.",
            "npc_samuel_002");
        sDespues.addOption(new DialogueOption("Entendido, iré al Gremio.", "s_final", null));
        samuel.put("s_despues", sDespues);

        DialogueNode sQuienEres = new DialogueNode("s_quien_eres",
            "Me llaman Samuel.\n\n" +
            "He trabajado el acero durante muchos años.\n\n" +
            "He visto aventureros llegar creyendo que estaban destinados a gobernar. También he visto campesinos convertirse en líderes.\n\n" +
            "La corona no decide quién es un rey. Las acciones lo hacen.",
            "npc_samuel_001");
        sQuienEres.addOption(new DialogueOption("¿Tú has visto muchos reinos?", "s_visto_reinos", null));
        samuel.put("s_quien_eres", sQuienEres);

        DialogueNode sVistoReinos = new DialogueNode("s_visto_reinos",
            "Demasiados.\n\n" +
            "Algunos nacieron con gloria y desaparecieron en una semana. Otros comenzaron con seis desconocidos que decidieron confiar unos en otros.\n\n" +
            "Por eso mi consejo es sencillo: antes de buscar poder, encuentra personas en las que puedas confiar.",
            "npc_samuel_002");
        sVistoReinos.addOption(new DialogueOption("¿Qué debo hacer ahora?", "s_que_hacer", null));
        samuel.put("s_visto_reinos", sVistoReinos);

        DialogueNode sFinal = new DialogueNode("s_final",
            "Ya sabes suficiente para dar tu primer paso.\n\n" +
            "Ve al Gremio. Busca a Karla. Ella te ayudará a encontrar tu equipo y te explicará cómo funciona este mundo.\n\n" +
            "Cuando estés preparado, vuelve a hablar conmigo. Entonces comenzará tu verdadera historia.",
            "npc_samuel_003");
        sFinal.addOption(new DialogueOption("Voy al Gremio", "inicio", "COMPLETE_SAMUEL_INTRO"));
        samuel.put("s_final", sFinal);

        // 2. KARLA — GREMIO Y EQUIPOS
        Map<String, DialogueNode> karla = getOrCreateNpcTypeMap("karla");

        DialogueNode k01 = new DialogueNode("inicio",
            "¡Bienvenido al Gremio del Reino! Soy Karla.\n\n" +
            "Aquí es donde los viajeros eligen su estandarte entre los 10 equipos oficiales del reino.",
            "npc_karla_001");
        k01.addOption(new DialogueOption("Abrir Panel de Equipos del Gremio", "inicio", "OPEN_GUILD_GUI"));
        k01.addOption(new DialogueOption("¿Cómo funcionan los equipos?", "k_info", null));
        karla.put("inicio", k01);

        DialogueNode kInfo = new DialogueNode("k_info",
            "Existen 10 equipos predefinidos por color. Cada equipo admite hasta 6 integrantes.\n\n" +
            "Puedes unirte desde este panel o con el comando /tronos team unir <color>.",
            "npc_karla_002");
        kInfo.addOption(new DialogueOption("Abrir Panel de Equipos", "inicio", "OPEN_GUILD_GUI"));
        karla.put("k_info", kInfo);

        // 3. DIOSA MARÍA
        Map<String, DialogueNode> diosa = getOrCreateNpcTypeMap("diosa_maria");
        DialogueNode d01 = new DialogueNode("inicio", "Todo viajero llega a un punto donde debe decidir qué quiere ser.", "npc_sacerdote_001");
        d01.addOption(new DialogueOption("Quiero conocer los caminos.", "d_caminos", null));
        d01.addOption(new DialogueOption("Quiero elegir mi rol.", "inicio", "close_and_open_role_selection"));
        diosa.put("inicio", d01);

        DialogueNode dCaminos = new DialogueNode("d_caminos", "Guerrero, Berserker, Mago, Arquero, Paladín, Dracónico o Clérigo.", "npc_sacerdote_002");
        dCaminos.addOption(new DialogueOption("Volver", "inicio", null));
        diosa.put("d_caminos", dCaminos);
    }
}
