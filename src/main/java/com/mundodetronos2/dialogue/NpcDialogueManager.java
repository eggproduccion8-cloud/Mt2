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
        if (lower.equals("guardian")) return "guardia_rey";
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
        // 1. MANUEL (Tutorial NPC 1)
        Map<String, DialogueNode> manuel = getOrCreateNpcTypeMap("manuel");
        DialogueNode m01 = new DialogueNode("inicio", "Ah... otro viajero.", "npc_manuel_001");
        m01.addOption(new DialogueOption("¿Viajero?", "m_viajero_01", null));
        m01.addOption(new DialogueOption("¿Dónde estoy?", "m_donde_01", null));
        m01.addOption(new DialogueOption("¿Quién eres tú?", "m_quien_01", null));
        manuel.put("inicio", m01);

        DialogueNode mViajero1 = new DialogueNode("m_viajero_01", "Así llamamos a quienes llegan aquí sin reino, sin bandera y sin un nombre que defender.", "npc_manuel_002");
        mViajero1.addOption(new DialogueOption("¿Y todos empiezan así?", "m_viajero_02", null));
        manuel.put("m_viajero_01", mViajero1);

        DialogueNode mViajero2 = new DialogueNode("m_viajero_02", "Todos. Algunos llegan pensando que podrán conquistar el mundo solos.\nNormalmente descubren bastante rápido que estaban equivocados.", "npc_manuel_003");
        mViajero2.addOption(new DialogueOption("¿Entonces necesito un equipo?", "m_viajero_03", null));
        manuel.put("m_viajero_02", mViajero2);

        DialogueNode mViajero3 = new DialogueNode("m_viajero_03", "Necesitas compañeros. Un hombre puede empuñar una espada.\nUn grupo puede construir un reino. Presiona la tecla K para crear tu equipo.", "npc_manuel_001");
        mViajero3.addOption(new DialogueOption("Entendido", "m_equipo_check", null));
        manuel.put("m_viajero_03", mViajero3);

        DialogueNode mDonde1 = new DialogueNode("m_donde_01", "En el corazón de una tierra donde los reinos nacen y desaparecen.\nY tarde o temprano todos terminan buscando lo mismo.", "npc_manuel_002");
        mDonde1.addOption(new DialogueOption("¿El Trono?", "m_donde_02", null));
        manuel.put("m_donde_01", mDonde1);

        DialogueNode mDonde2 = new DialogueNode("m_donde_02", "Exactamente. Pero no confundas el Trono con un simple bloque.\nEl Trono es el corazón de aquello que pretendas construir.", "npc_manuel_003");
        mDonde2.addOption(new DialogueOption("Interesante", "m_equipo_check", null));
        manuel.put("m_donde_02", mDonde2);

        DialogueNode mQuien1 = new DialogueNode("m_quien_01", "Me llaman Manuel. He visto llegar guerreros, aventureros, mercenarios...\ny más de uno que pensaba que era un rey antes siquiera de tener un reino.", "npc_manuel_001");
        mQuien1.addOption(new DialogueOption("¿Y qué sabes de los Tronos?", "m_quien_02", null));
        manuel.put("m_quien_01", mQuien1);

        DialogueNode mQuien2 = new DialogueNode("m_quien_02", "Lo suficiente para decirte que no deberías tocar uno hasta que estés preparado.", "npc_manuel_002");
        mQuien2.addOption(new DialogueOption("Comprendo", "m_equipo_check", null));
        manuel.put("m_quien_02", mQuien2);

        DialogueNode mEquipoCheck = new DialogueNode("m_equipo_check", "Antes de seguir necesito saber algo. Presiona K para crear o unirte a un grupo/equipo. ¿Ya tienes compañeros?", "npc_manuel_003");
        mEquipoCheck.addOption(new DialogueOption("Aún no, presiono K para crear mi grupo.", "m_solo", null));
        mEquipoCheck.addOption(new DialogueOption("¡Sí, ya tengo mi equipo listo!", "m_grupo", null));
        manuel.put("m_equipo_check", mEquipoCheck);

        DialogueNode mSolo = new DialogueNode("m_solo", "Entonces presiona K ahora mismo para crear tu grupo. Invita a las personas en las que confías. Cuando estés listo, habla conmigo de nuevo.", "npc_manuel_001");
        mSolo.addOption(new DialogueOption("Entendido, presiono K", null, "close"));
        manuel.put("m_solo", mSolo);

        DialogueNode mGrupo = new DialogueNode("m_grupo", "Vaya. Así que ya tienes compañeros. Bien. Ahora sí empieza tu viaje.", "npc_manuel_003");
        mGrupo.addOption(new DialogueOption("¿Qué sigue?", "m_sigue", null));
        manuel.put("m_grupo", mGrupo);

        DialogueNode mSigue = new DialogueNode("m_sigue", "Ve con Laura. Ella necesita ayuda con algo mucho más aburrido que conquistar un reino: Trigo.", "npc_manuel_001");
        mSigue.addOption(new DialogueOption("¿Trigo?", "m_trigo", null));
        manuel.put("m_sigue", mSigue);

        DialogueNode mTrigo = new DialogueNode("m_trigo", "Sí. Créeme. Los reinos también tienen hambre.\nCuando termines con Laura, vuelve a caminar hacia adelante.", "npc_manuel_002");
        mTrigo.addOption(new DialogueOption("Comenzar misión con Laura", "inicio", "complete_manuel_tut"));
        manuel.put("m_trigo", mTrigo);

        DialogueNode mPostManuel = new DialogueNode("m_post_completion", "¡Buen día, viajero! Tu equipo ya demostró su valía conmigo. Continúa adelante con Laura.", "npc_manuel_001");
        mPostManuel.addOption(new DialogueOption("Entendido, gracias Manuel.", null, "close"));
        manuel.put("m_post_completion", mPostManuel);

        // 2. LAURA (Tutorial NPC 2)
        Map<String, DialogueNode> laura = getOrCreateNpcTypeMap("laura");
        DialogueNode lLocked = new DialogueNode("l_locked", "Hola, viajero. Primero debes hablar con Manuel y formar tu equipo antes de que podamos trabajar juntos.", "npc_laura_001");
        lLocked.addOption(new DialogueOption("Entendido", null, "close"));
        laura.put("l_locked", lLocked);

        DialogueNode l01 = new DialogueNode("inicio", "¡Ah! Por fin alguien vino a ayudarme.", "npc_laura_001");
        l01.addOption(new DialogueOption("¿Qué necesitas?", "l_necesitas", null));
        l01.addOption(new DialogueOption("Manuel me mandó contigo.", "l_necesitas", null));
        l01.addOption(new DialogueOption("¿Por qué estás tan preocupada?", "l_necesitas", null));
        laura.put("inicio", l01);

        DialogueNode lNecesitas = new DialogueNode("l_necesitas", "El almacén está casi vacío. Y un reino sin comida dura menos que una espada mal cuidada.", "npc_laura_002");
        lNecesitas.addOption(new DialogueOption("¿Cuánto trigo necesitas?", "l_cuanto", null));
        laura.put("l_necesitas", lNecesitas);

        DialogueNode lCuanto = new DialogueNode("l_cuanto", "Muchísimo. Necesito ciento cincuenta de trigo acumulados entre todo tu equipo.", "npc_laura_001");
        lCuanto.addOption(new DialogueOption("¿Cada uno debe traer 150?", "l_cada_uno", null));
        laura.put("l_cuanto", lCuanto);

        DialogueNode lCadaUno = new DialogueNode("l_cada_uno", "¿Cada uno? ¡No! Entre todos. Este es el primer trabajo de tu grupo.", "npc_laura_002");
        lCadaUno.addOption(new DialogueOption("Entregar trigo acumulado", "inicio", "deliver_wheat_laura"));
        lCadaUno.addOption(new DialogueOption("Volveré con el trigo", null, "close"));
        laura.put("l_cada_uno", lCadaUno);

        DialogueNode lInProgress = new DialogueNode("l_in_progress", "Aún faltan recursos de trigo para completar los 150 del reino. Trae el trigo en tu inventario.", "npc_laura_001");
        lInProgress.addOption(new DialogueOption("Entregar trigo acumulado", "inicio", "deliver_wheat_laura"));
        lInProgress.addOption(new DialogueOption("Seguiré buscando trigo", null, "close"));
        laura.put("l_in_progress", lInProgress);

        DialogueNode lReady = new DialogueNode("l_ready", "¡Excelente! Ya tienen los recursos suficientes. Haz clic para entregar el trigo.", "npc_laura_002");
        lReady.addOption(new DialogueOption("Entregar 150 Trigo", "inicio", "deliver_wheat_laura"));
        laura.put("l_ready", lReady);

        DialogueNode lPostLaura = new DialogueNode("l_post_completion", "¡Buen día! Gracias a tu equipo los almacenes están llenos. Oscar los espera más adelante.", "npc_laura_001");
        lPostLaura.addOption(new DialogueOption("Gracias, Laura.", null, "close"));
        laura.put("l_post_completion", lPostLaura);

        // 3. OSCAR (Tutorial NPC 3)
        Map<String, DialogueNode> oscar = getOrCreateNpcTypeMap("oscar");
        DialogueNode oLocked = new DialogueNode("o_locked", "Aún no estás listo. Completa primero la misión del trigo con Laura.", "npc_oscar_001");
        oLocked.addOption(new DialogueOption("Entendido", null, "close"));
        oscar.put("o_locked", oLocked);

        DialogueNode oPostOscar = new DialogueNode("o_post_completion", "¡Que tengan buen día! Ya recibieron su equipamiento. Sigan con Samuel.", "npc_oscar_001");
        oPostOscar.addOption(new DialogueOption("Hasta luego, Oscar.", null, "close"));
        oscar.put("o_post_completion", oPostOscar);

        DialogueNode o01 = new DialogueNode("inicio", "Así que ustedes son los del trigo.", "npc_oscar_001");
        o01.addOption(new DialogueOption("¿Cómo sabes?", "o_sabes", null));
        o01.addOption(new DialogueOption("Laura nos mandó.", "o_sabes", null));
        o01.addOption(new DialogueOption("¿Tienes algo para nosotros?", "o_sabes", null));
        oscar.put("inicio", o01);

        DialogueNode oSabes = new DialogueNode("o_sabes", "En un pueblo pequeño, las noticias viajan más rápido que los caballos.", "npc_oscar_002");
        oSabes.addOption(new DialogueOption("¿Puedes ayudarnos?", "o_ayuda", null));
        oscar.put("o_sabes", oSabes);

        DialogueNode oAyuda = new DialogueNode("o_ayuda", "Claro. Pero no piensen que voy a regalarles una armadura de diamante.", "npc_oscar_001");
        oAyuda.addOption(new DialogueOption("¿Entonces qué tenemos?", "o_armadura", null));
        oscar.put("o_ayuda", oAyuda);

        DialogueNode oArmadura = new DialogueNode("o_armadura", "Algo mejor para empezar: Una oportunidad. Cada uno de ustedes tendrá una armadura. Úsenla, cuídenla y no la vendan por una cerveza.", "npc_oscar_002");
        oArmadura.addOption(new DialogueOption("Reclamar mi armadura inicial", "inicio", "claim_oscar_armor"));
        oArmadura.addOption(new DialogueOption("Volver después", null, "close"));
        oscar.put("o_armadura", oArmadura);

        // 4. SAMUEL (Tutorial NPC 4)
        Map<String, DialogueNode> samuel = getOrCreateNpcTypeMap("samuel");
        samuel.put("s_locked", new DialogueNode("s_locked", "Aún no están listos. Reclamen primero la armadura con Oscar.", "npc_samuel_001"));
        samuel.put("s_post_completion", new DialogueNode("s_post_completion", "¡Buen día, combatientes! Demostraron su fuerza. Busquen a Heraldo.", "npc_samuel_001"));
        DialogueNode s01 = new DialogueNode("inicio", "Antes de que sigan adelante, quiero ver algo. Quiero ver cómo pelean.", "npc_samuel_001");
        s01.addOption(new DialogueOption("¿Contra quién?", "s_contra", null));
        s01.addOption(new DialogueOption("¿Un jugador?", "s_contra", null));
        s01.addOption(new DialogueOption("¿Un monstruo?", "s_contra", null));
        samuel.put("inicio", s01);

        DialogueNode sContra = new DialogueNode("s_contra", "Ninguno. Hoy no quiero verlos morir. Quiero que aprendan.\nHay una criatura de entrenamiento esperando. No los atacará, no puede morir y está aquí para recibir sus golpes.", "npc_samuel_002");
        sContra.addOption(new DialogueOption("¿Tenemos que vencerlo?", "s_vencer", null));
        samuel.put("s_contra", sContra);

        DialogueNode sVencer = new DialogueNode("s_vencer", "No. Tienen que demostrar que están preparados.", "npc_samuel_001");
        sVencer.addOption(new DialogueOption("Iniciar prueba con el Golem de Hierro", "inicio", "start_samuel_golem"));
        sVencer.addOption(new DialogueOption("Aún no", null, "close"));
        samuel.put("s_vencer", sVencer);

        // 5. HERALDO (Tutorial NPC 5)
        Map<String, DialogueNode> heraldo = getOrCreateNpcTypeMap("heraldo");
        heraldo.put("h_locked", new DialogueNode("h_locked", "Aún no están listos. Superen primero la prueba del acero con Samuel.", "npc_heraldo_001"));
        heraldo.put("h_post_completion", new DialogueNode("h_post_completion", "¡Buen día! Ya conocen las Cargas de Asalto. Sigan con el Guardia del Rey.", "npc_heraldo_001"));
        DialogueNode h01 = new DialogueNode("inicio", "Así que Samuel los dejó pasar. Eso significa que al menos saben golpear.", "npc_heraldo_001");
        h01.addOption(new DialogueOption("¿Qué vamos a aprender aquí?", "h_aprender", null));
        h01.addOption(new DialogueOption("¿Qué es una Carga de Asalto?", "h_carga", null));
        h01.addOption(new DialogueOption("¿Sirve para destruir Tronos?", "h_trono", null));
        heraldo.put("inicio", h01);

        DialogueNode hCarga = new DialogueNode("h_carga", "Una Carga de Asalto no es un simple explosivo. Es una herramienta de guerra y sólo funciona cuando las reglas del asalto están activas.", "npc_heraldo_002");
        hCarga.addOption(new DialogueOption("Continuar", "h_prueba", null));
        heraldo.put("h_carga", hCarga);

        DialogueNode hTrono = new DialogueNode("h_trono", "Un muro y un Trono no son lo mismo. Una pared puede caer; un Trono debe ser conquistado.", "npc_heraldo_001");
        hTrono.addOption(new DialogueOption("Continuar", "h_prueba", null));
        heraldo.put("h_trono", hTrono);

        DialogueNode hAprender = new DialogueNode("h_aprender", "Hoy van a aprender las dos cosas. Primero madera, después piedra, después obsidiana... y al final, el Trono.", "npc_heraldo_002");
        hAprender.addOption(new DialogueOption("Iniciar prueba de cargas", "inicio", "start_heraldo_trial"));
        heraldo.put("h_aprender", hAprender);

        DialogueNode hPrueba = new DialogueNode("h_prueba", "Comenzaremos la prueba de demolición.", "npc_heraldo_001");
        hPrueba.addOption(new DialogueOption("Iniciar prueba de cargas", "inicio", "start_heraldo_trial"));
        heraldo.put("h_prueba", hPrueba);

        // 6. GUARDIA DEL REY (Tutorial NPC 6)
        Map<String, DialogueNode> guardia = getOrCreateNpcTypeMap("guardia_rey");
        guardia.put("g_locked", new DialogueNode("g_locked", "Alto. Completen primero la prueba con Heraldo.", "npc_guardia_001"));
        guardia.put("g_post_completion", new DialogueNode("g_post_completion", "Conocen bien el territorio. El Sacerdote los espera.", "npc_guardia_001"));
        DialogueNode g01 = new DialogueNode("inicio", "Alto. ¿Quién les dijo que podían pasar?", "npc_guardia_001");
        g01.addOption(new DialogueOption("Heraldo.", "g_heraldo", null));
        g01.addOption(new DialogueOption("Venimos por una misión.", "g_heraldo", null));
        g01.addOption(new DialogueOption("¿Tenemos que demostrar algo?", "g_heraldo", null));
        guardia.put("inicio", g01);

        DialogueNode gHeraldo = new DialogueNode("g_heraldo", "Exactamente. Un reino no se sostiene únicamente con fuerza. También necesita ojos.", "npc_guardia_001");
        gHeraldo.addOption(new DialogueOption("Iniciar misión de exploración del Lobby", "inicio", "start_guardia_exploration"));
        guardia.put("g_heraldo", gHeraldo);

        // 7. SACERDOTE (Tutorial NPC 7)
        Map<String, DialogueNode> sacerdote = getOrCreateNpcTypeMap("sacerdote");
        sacerdote.put("sac_locked", new DialogueNode("sac_locked", "Aún no es momento. Hablen con el Guardia del Rey.", "npc_sacerdote_001"));
        sacerdote.put("sac_post_completion", new DialogueNode("sac_post_completion", "La leyenda ha sido transmitida. Diríjanse a la Arena.", "npc_sacerdote_001"));
        DialogueNode sac01 = new DialogueNode("inicio", "Han llegado lejos, viajeros. Pero todavía no saben por qué existe el Trono.", "npc_sacerdote_001");
        sac01.addOption(new DialogueOption("¿Entonces por qué existe?", "sac_porque", null));
        sac01.addOption(new DialogueOption("¿Quién creó los Tronos?", "sac_porque", null));
        sac01.addOption(new DialogueOption("¿Quién es María?", "sac_maria", null));
        sacerdote.put("inicio", sac01);

        DialogueNode sacMaria = new DialogueNode("sac_maria", "María. La Diosa que, según las antiguas historias, observó cómo los hombres luchaban por tierras que no sabían proteger.", "npc_sacerdote_002");
        sacMaria.addOption(new DialogueOption("¿Y qué hizo?", "sac_hizo", null));
        sacerdote.put("sac_maria", sacMaria);

        DialogueNode sacHizo = new DialogueNode("sac_hizo", "Creó las pruebas. Los fuertes tendrían la oportunidad de levantar un reino; los débiles tendrían que aprender.", "npc_sacerdote_001");
        sacHizo.addOption(new DialogueOption("¿Y el Trono?", "sac_porque", null));
        sacerdote.put("sac_hizo", sacHizo);

        DialogueNode sacPorque = new DialogueNode("sac_porque", "El Trono representa aquello que quieres proteger. No es riqueza, no es gloria; es responsabilidad.\nPero todavía no están listos. El Capitán de Arena los espera.", "npc_sacerdote_002");
        sacPorque.addOption(new DialogueOption("Avanzar al Capitán de Arena", "inicio", "complete_sacerdote_tut"));
        sacerdote.put("sac_porque", sacPorque);

        // Dialogue Node for Sacerdote Level 10 Throne Claim
        DialogueNode sacThrone10 = new DialogueNode("sacerdote_throne_claim", "Han regresado. Y ahora sí puedo llamarlos algo más que viajeros.\nUn reino debe tener alguien dispuesto a cargar con la responsabilidad.", "npc_sacerdote_001");
        sacThrone10.addOption(new DialogueOption("Reclamar el Trono de nuestro Reino", "sacerdote_confirm_throne", null));
        sacerdote.put("sacerdote_throne_claim", sacThrone10);

        DialogueNode sacConfirm = new DialogueNode("sacerdote_confirm_throne", "¿Estás preparado para aceptar la responsabilidad de tu reino?", "npc_sacerdote_002");
        sacConfirm.addOption(new DialogueOption("Sí, acepto la responsabilidad", "inicio", "claim_leader_throne"));
        sacConfirm.addOption(new DialogueOption("Todavía no", null, "close"));
        sacerdote.put("sacerdote_confirm_throne", sacConfirm);

        // 8. CAPITÁN DE ARENA (Tutorial NPC 8)
        Map<String, DialogueNode> capitan = getOrCreateNpcTypeMap("capitan_arena");
        capitan.put("c_locked", new DialogueNode("c_locked", "Aún no están autorizados. Escuchen primero la leyenda del Sacerdote.", "npc_capitan_001"));
        capitan.put("c_post_completion", new DialogueNode("c_post_completion", "¡Gran combate! El Maestro de Cargas los espera para la prueba final.", "npc_capitan_001"));
        DialogueNode c01 = new DialogueNode("inicio", "¿Así que ustedes son el grupo del que todo el mundo habla?", "npc_capitan_001");
        c01.addOption(new DialogueOption("¿Hablan de nosotros?", "c_hablan", null));
        c01.addOption(new DialogueOption("¿Qué debemos hacer?", "c_hablan", null));
        c01.addOption(new DialogueOption("¿Otra prueba?", "c_hablan", null));
        capitan.put("inicio", c01);

        DialogueNode cHablan = new DialogueNode("c_hablan", "Sí. Pero esta vez no quiero ver quién golpea más fuerte. Quiero ver si saben moverse como uno solo.", "npc_capitan_001");
        cHablan.addOption(new DialogueOption("Entrar a la Prueba de Arena", "inicio", "start_arena_trial"));
        capitan.put("c_hablan", cHablan);

        // 9. MAESTRO DE CARGAS (Tutorial NPC 9)
        Map<String, DialogueNode> maestro = getOrCreateNpcTypeMap("maestro_cargas");
        maestro.put("mst_locked", new DialogueNode("mst_locked", "Aún no están listos para la prueba final. Hablen con el Capitán de Arena.", "npc_maestro_001"));
        maestro.put("mst_post_completion", new DialogueNode("mst_post_completion", "¡Felicitaciones! Su equipo ha completado la campaña. Vayan con el Sacerdote a reclamar su Trono.", "npc_maestro_001"));
        DialogueNode mst01 = new DialogueNode("inicio", "Ya saben trabajar, ya saben pelear, ya saben derribar. Ahora necesito saber si están preparados para tener algo que perder.", "npc_maestro_001");
        mst01.addOption(new DialogueOption("¿Qué tenemos que hacer?", "mst_hacer", null));
        mst01.addOption(new DialogueOption("¿Es la prueba final?", "mst_hacer", null));
        mst01.addOption(new DialogueOption("¿Qué hay detrás de ti?", "mst_hacer", null));
        maestro.put("inicio", mst01);

        DialogueNode mstHacer = new DialogueNode("mst_hacer", "El símbolo de todo lo que vienen aprendiendo: Un Trono.", "npc_maestro_001");
        mstHacer.addOption(new DialogueOption("Iniciar la Prueba Final del Tutorial", "inicio", "start_final_siege_trial"));
        maestro.put("mst_hacer", mstHacer);

        // 10. MONJE DEL DESTINO
        Map<String, DialogueNode> monje = getOrCreateNpcTypeMap("monje_destino");
        DialogueNode mnk01 = new DialogueNode("inicio", "Muchos quieren elegir un camino. Pero pocos entienden lo que significa caminarlo.", "npc_manuel_001");
        mnk01.addOption(new DialogueOption("¿Qué caminos existen?", "mnk_caminos", null));
        mnk01.addOption(new DialogueOption("¿Qué es un rol?", "mnk_rol", null));
        mnk01.addOption(new DialogueOption("¿Puedo cambiar de rol?", "mnk_cambiar", null));
        mnk01.addOption(new DialogueOption("¿Qué es el carnet?", "mnk_carnet", null));
        monje.put("inicio", mnk01);

        DialogueNode mnkCaminos = new DialogueNode("mnk_caminos", "Existen 7 caminos sagrados: Guerrero, Berserker, Mago, Arquero, Paladín, Dracónico y Clérigo.", "npc_manuel_002");
        mnkCaminos.addOption(new DialogueOption("Volver", "inicio", null));
        monje.put("mnk_caminos", mnkCaminos);

        DialogueNode mnkRol = new DialogueNode("mnk_rol", "Un rol te otorga un árbol de habilidades único con hechizos y pasivas poderosas.", "npc_manuel_003");
        mnkRol.addOption(new DialogueOption("Volver", "inicio", null));
        monje.put("mnk_rol", mnkRol);

        DialogueNode mnkCambiar = new DialogueNode("mnk_cambiar", "Una vez elegido ante la Diosa María, tu camino quedará forjado.", "npc_manuel_001");
        mnkCambiar.addOption(new DialogueOption("Volver", "inicio", null));
        monje.put("mnk_cambiar", mnkCambiar);

        DialogueNode mnkCarnet = new DialogueNode("mnk_carnet", "Tu Carnet acredita tu rango e identidad en el reino. Presiona K para verlo.", "npc_manuel_002");
        mnkCarnet.addOption(new DialogueOption("Volver", "inicio", null));
        monje.put("mnk_carnet", mnkCarnet);

        // 11. KARLA — RECEPCIONISTA DEL GREMIO
        Map<String, DialogueNode> karla = getOrCreateNpcTypeMap("karla");
        DialogueNode k01 = new DialogueNode("inicio", "¡Bienvenido al Gremio del Reino! Manuel me avisó de tu llegada. Para aceptar misiones oficiales primero debes pertenecer a un Equipo.", "npc_laura_001");
        k01.addOption(new DialogueOption("¿Qué es un Equipo?", "k_info", null));
        k01.addOption(new DialogueOption("Crear mi propio Equipo", "k_crear_equipo", null));
        k01.addOption(new DialogueOption("Ver misiones disponibles del Gremio", "k_misiones", null));
        karla.put("inicio", k01);

        DialogueNode kInfo = new DialogueNode("k_info", "Un Equipo reúne hasta 6 aventureros bajo una misma bandera. Comparten recursos, territorio, puntos y misiones cooperativas.", "npc_laura_002");
        kInfo.addOption(new DialogueOption("Entendido, quiero crear mi Equipo", "k_crear_equipo", null));
        kInfo.addOption(new DialogueOption("Volver al menú principal", "inicio", null));
        karla.put("k_info", kInfo);

        DialogueNode kCrear = new DialogueNode("k_crear_equipo", "Escribe el nombre para tu nuevo Equipo en la ventana. Te convertirás en su Líder y podrás invitar hasta 5 compañeros más.", "npc_laura_001");
        kCrear.addOption(new DialogueOption("Abrir Registro de Equipo", "inicio", "open_create_team_gui"));
        karla.put("k_crear_equipo", kCrear);

        DialogueNode kMisiones = new DialogueNode("k_misiones", "La primera misión oficial del Gremio es ayudar a Laura en los graneros del Spawn con el suministro de trigo (150 unidades por equipo).", "npc_laura_002");
        kMisiones.addOption(new DialogueOption("Aceptar Misión de Laura (Trigo)", "inicio", "complete_manuel_tut"));
        kMisiones.addOption(new DialogueOption("Volver", "inicio", null));
        karla.put("k_misiones", kMisiones);

        // 12. DIOSA MARÍA
        Map<String, DialogueNode> diosa = getOrCreateNpcTypeMap("diosa_maria");
        DialogueNode d01 = new DialogueNode("inicio", "Todo viajero llega a un punto donde debe decidir qué quiere ser.", "npc_sacerdote_001");
        d01.addOption(new DialogueOption("Quiero conocer los caminos.", "d_caminos", null));
        d01.addOption(new DialogueOption("Quiero elegir mi rol.", "inicio", "close_and_open_role_selection"));
        d01.addOption(new DialogueOption("¿Qué es un rol?", "d_rol", null));
        d01.addOption(new DialogueOption("¿Puedo arrepentirme?", "d_arrepentir", null));
        diosa.put("inicio", d01);

        DialogueNode dCaminos = new DialogueNode("d_caminos", "Guerrero, Berserker, Mago, Arquero, Paladín, Dracónico o Clérigo.", "npc_sacerdote_002");
        dCaminos.addOption(new DialogueOption("Volver", "inicio", null));
        diosa.put("d_caminos", dCaminos);

        DialogueNode dRol = new DialogueNode("d_rol", "Un pacto espiritual que te dará habilidades únicas.", "npc_sacerdote_001");
        dRol.addOption(new DialogueOption("Volver", "inicio", null));
        diosa.put("d_rol", dRol);

        DialogueNode dArrepentir = new DialogueNode("d_arrepentir", "Elige con sabiduría, mortal.", "npc_sacerdote_002");
        dArrepentir.addOption(new DialogueOption("Volver", "inicio", null));
        diosa.put("d_arrepentir", dArrepentir);

        // 13. CUSTODIO DEL TRONO
        Map<String, DialogueNode> custodio = getOrCreateNpcTypeMap("custodio_trono");
        DialogueNode cst01 = new DialogueNode("inicio", "Muchos quieren conquistar un Trono. Los inteligentes primero aprenden a defender el suyo.", "npc_guardia_001");
        cst01.addOption(new DialogueOption("¿Cómo funciona la vida del Trono?", "cst_vidas", null));
        cst01.addOption(new DialogueOption("¿Qué es la protección 150?", "cst_proteccion", null));
        cst01.addOption(new DialogueOption("¿Cómo funcionan las Cargas de Asalto?", "cst_cargas", null));
        custodio.put("inicio", cst01);

        DialogueNode cstVidas = new DialogueNode("cst_vidas", "Cada base destruida en un asalto pierde una vida de trono.", "npc_guardia_001");
        cstVidas.addOption(new DialogueOption("Volver", "inicio", null));
        custodio.put("cst_vidas", cstVidas);

        DialogueNode cstProteccion = new DialogueNode("cst_proteccion", "Un área de 150x150 bloques alrededor de tu trono donde enemigos no pueden romper ni construir mientras el evento esté inactivo.", "npc_guardia_001");
        cstProteccion.addOption(new DialogueOption("Volver", "inicio", null));
        custodio.put("cst_proteccion", cstProteccion);

        DialogueNode cstCargas = new DialogueNode("cst_cargas", "Las cargas de madera, piedra y obsidiana tardan 10 segundos. La carga sobre un Trono tarda 30 segundos.", "npc_guardia_001");
        cstCargas.addOption(new DialogueOption("Volver", "inicio", null));
        custodio.put("cst_cargas", cstCargas);

        // 14. MAESTRO DE ROLES
        Map<String, DialogueNode> mRoles = getOrCreateNpcTypeMap("maestro_roles");
        DialogueNode mr01 = new DialogueNode("inicio", "Te orientaré sobre las armas y armaduras permitidas para tu rol.", "npc_heraldo_001");
        mr01.addOption(new DialogueOption("¿Qué objetos puedo usar?", "mr_equipamiento", null));
        mr01.addOption(new DialogueOption("¿Cómo abro mis habilidades?", "mr_habilidades", null));
        mRoles.put("inicio", mr01);

        DialogueNode mrEquip = new DialogueNode("mr_equipamiento", "Cada rol prohíbe armas y armaduras no autorizadas. Revisa los detalles en tu Carnet (K).", "npc_heraldo_002");
        mrEquip.addOption(new DialogueOption("Volver", "inicio", null));
        mRoles.put("mr_equipamiento", mrEquip);

        DialogueNode mrHab = new DialogueNode("mr_habilidades", "Presiona la tecla M para abrir la pantalla de tu árbol de habilidades.", "npc_heraldo_001");
        mrHab.addOption(new DialogueOption("Volver", "inicio", null));
        mRoles.put("mr_habilidades", mrHab);

        // 15. MERCADER DEL GREMIO
        Map<String, DialogueNode> mercader = getOrCreateNpcTypeMap("mercader_gremio");
        DialogueNode mer01 = new DialogueNode("inicio", "¡Las mejores mercancías del reino las encuentras aquí!", "npc_oscar_001");
        mer01.addOption(new DialogueOption("¿Qué comerciamos?", "mer_comercio", null));
        mercader.put("inicio", mer01);

        DialogueNode merCom = new DialogueNode("mer_comercio", "Canjea puntos de tu reino por recompensas mágicas y botines.", "npc_oscar_002");
        merCom.addOption(new DialogueOption("Volver", "inicio", null));
        mercader.put("mer_comercio", merCom);
    }
}
