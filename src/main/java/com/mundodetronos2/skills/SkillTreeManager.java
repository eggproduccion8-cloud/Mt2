package com.mundodetronos2.skills;

import java.util.HashMap;
import java.util.Map;

public class SkillTreeManager {
    private static final Map<String, SkillTree> trees = new HashMap<>();

    static {
        // Inicializar los 7 árboles de habilidades
        initGuerrero();
        initBerserker();
        initMago();
        initArquero();
        initPaladin();
        initDraconico();
        initClerigo();
    }

    private static void initGuerrero() {
        SkillTree tree = new SkillTree("guerrero");

        tree.addNode(new SkillNode("g_skill1", "Escudo Rústico", "Estilo de defensa inicial que permite bloquear ataques básicos.", 1, 1, 1, 0));

        tree.addNode(new SkillNode("g_skill2", "Ataque Preciso", "Concentra tu fuerza en un golpe letal contra tus enemigos.", 5, 1, 1, 1)
                .addPrereq("g_skill1"));

        tree.addNode(new SkillNode("g_skill3", "Defensa Activa", "Aumenta la absorción de daño temporalmente cuando estás acorralado.", 10, 1, 0, 2)
                .addPrereq("g_skill2"));

        tree.addNode(new SkillNode("g_skill4", "Fuerza del Reino", "Incentiva el poder y el daño cuerpo a cuerpo de tus aliados.", 15, 2, 2, 2)
                .addPrereq("g_skill2"));

        tree.addNode(new SkillNode("g_skill5", "Coraza Invencible", "Bendición de acero de la Diosa María que te otorga resistencia suprema.", 30, 3, 1, 3)
                .addPrereq("g_skill3")
                .addPrereq("g_skill4"));

        trees.put("guerrero", tree);
    }

    private static void initBerserker() {
        SkillTree tree = new SkillTree("berserker");

        tree.addNode(new SkillNode("b_skill1", "Furia Inicial", "Despierta la ira en tu alma para incrementar la velocidad de ataque.", 1, 1, 1, 0));

        tree.addNode(new SkillNode("b_skill2", "Golpe Devastador", "Un ataque brutal con hacha que ignora parte de la armadura enemiga.", 5, 1, 1, 1)
                .addPrereq("b_skill1"));

        tree.addNode(new SkillNode("b_skill3", "Grito de Guerra", "Intimida a tus oponentes reduciendo su velocidad y fuerza.", 10, 1, 0, 2)
                .addPrereq("b_skill2"));

        tree.addNode(new SkillNode("b_skill4", "Sed de Sangre", "Te cura una pequeña fracción del daño infligido a tus enemigos.", 15, 2, 2, 2)
                .addPrereq("b_skill2"));

        tree.addNode(new SkillNode("b_skill5", "Frenesí Desatado", "Estado de rabia absoluta que duplica tu velocidad por 10 segundos.", 30, 3, 1, 3)
                .addPrereq("b_skill3")
                .addPrereq("b_skill4"));

        trees.put("berserker", tree);
    }

    private static void initMago() {
        SkillTree tree = new SkillTree("mago");

        tree.addNode(new SkillNode("m_skill1", "Centella Arcana", "Lanza una pequeña esfera de energía mágica pura.", 1, 1, 1, 0));

        tree.addNode(new SkillNode("m_skill2", "Proyectil de Fuego", "Invoca una bola de fuego que calcina y debilita a tu objetivo.", 5, 1, 1, 1)
                .addPrereq("m_skill1"));

        tree.addNode(new SkillNode("m_skill3", "Muro de Escarcha", "Crea una barrera helada que congela y ralentiza a los atacantes.", 10, 1, 0, 2)
                .addPrereq("m_skill2"));

        tree.addNode(new SkillNode("m_skill4", "Conocimiento Cósmico", "Reduce los cooldowns de todas tus magias y conjuros.", 15, 2, 2, 2)
                .addPrereq("m_skill2"));

        tree.addNode(new SkillNode("m_skill5", "Tormenta de Rayos", "Desata la ira de las tormentas celestiales sobre un área extensa.", 30, 3, 1, 3)
                .addPrereq("m_skill3")
                .addPrereq("m_skill4"));

        trees.put("mago", tree);
    }

    private static void initArquero() {
        SkillTree tree = new SkillTree("arquero");

        tree.addNode(new SkillNode("a_skill1", "Flecha Veloz", "Dispara proyectiles con mayor velocidad inicial.", 1, 1, 1, 0));

        tree.addNode(new SkillNode("a_skill2", "Tiro Triple", "Dispara tres flechas en abanico consumiendo una sola munición.", 5, 1, 1, 1)
                .addPrereq("a_skill1"));

        tree.addNode(new SkillNode("a_skill3", "Disparo Venenoso", "Impregna tus proyectiles con toxinas que dañan con el tiempo.", 10, 1, 0, 2)
                .addPrereq("a_skill2"));

        tree.addNode(new SkillNode("a_skill4", "Pies Ligeros", "Incrementa tu velocidad de movimiento al apuntar con arco.", 15, 2, 2, 2)
                .addPrereq("a_skill2"));

        tree.addNode(new SkillNode("a_skill5", "Lluvia de Flechas", "Invoca una lluvia incesante de proyectiles sobre tus enemigos.", 30, 3, 1, 3)
                .addPrereq("a_skill3")
                .addPrereq("a_skill4"));

        trees.put("arquero", tree);
    }

    private static void initPaladin() {
        SkillTree tree = new SkillTree("paladin");

        tree.addNode(new SkillNode("p_skill1", "Aura de Rectitud", "Otorga una sutil resistencia al daño a los aliados cercanos.", 1, 1, 1, 0));

        tree.addNode(new SkillNode("p_skill2", "Golpe Sagrado", "Un impacto imbuido en luz divina que inflige daño adicional a monstruos.", 5, 1, 1, 1)
                .addPrereq("p_skill1"));

        tree.addNode(new SkillNode("p_skill3", "Escudo Divino", "Un escudo místico que te hace inmune a efectos de estado negativos.", 10, 1, 0, 2)
                .addPrereq("p_skill2"));

        tree.addNode(new SkillNode("p_skill4", "Plegaria de Alivio", "Cura de inmediato una pequeña porción de salud a un aliado herido.", 15, 2, 2, 2)
                .addPrereq("p_skill2"));

        tree.addNode(new SkillNode("p_skill5", "Consagración", "Purifica el suelo bajo tus pies, curando aliados y dañando enemigos.", 30, 3, 1, 3)
                .addPrereq("p_skill3")
                .addPrereq("p_skill4"));

        trees.put("paladin", tree);
    }

    private static void initDraconico() {
        SkillTree tree = new SkillTree("draconico");

        tree.addNode(new SkillNode("d_skill1", "Escamas de Dragón", "Fortalece tu piel otorgando inmunidad sutil al fuego.", 1, 1, 1, 0));

        tree.addNode(new SkillNode("d_skill2", "Aliento Ígneo", "Lanza una llamarada frontal que incinera el suelo y oponentes.", 5, 1, 1, 1)
                .addPrereq("d_skill1"));

        tree.addNode(new SkillNode("d_skill3", "Garra Ancestral", "Ataque veloz de corto alcance que inflige quemaduras profundas.", 10, 1, 0, 2)
                .addPrereq("d_skill2"));

        tree.addNode(new SkillNode("d_skill4", "Vuelo Dracónico", "Permite dar saltos dobles muy elevados impulsados por alas etéreas.", 15, 2, 2, 2)
                .addPrereq("d_skill2"));

        tree.addNode(new SkillNode("d_skill5", "Furia de Tiamat", "Llama el poder ancestral de los dragones para provocar explosiones térmicas.", 30, 3, 1, 3)
                .addPrereq("d_skill3")
                .addPrereq("d_skill4"));

        trees.put("draconico", tree);
    }

    private static void initClerigo() {
        SkillTree tree = new SkillTree("clerigo");

        tree.addNode(new SkillNode("c_skill1", "Bendición Menor", "Otorga una regeneración de vida muy leve a un aliado seleccionado.", 1, 1, 1, 0));

        tree.addNode(new SkillNode("c_skill2", "Cantar de Sanación", "Un himno sagrado que cura a todos los aliados en un radio mediano.", 5, 1, 1, 1)
                .addPrereq("c_skill1"));

        tree.addNode(new SkillNode("c_skill3", "Escudo de Fe", "Protege a un aliado reduciendo a la mitad el siguiente daño recibido.", 10, 1, 0, 2)
                .addPrereq("c_skill2"));

        tree.addNode(new SkillNode("c_skill4", "Resurrección Astral", "Permite que un aliado caído recupere sus vidas de trono más rápido.", 15, 2, 2, 2)
                .addPrereq("c_skill2"));

        tree.addNode(new SkillNode("c_skill5", "Milagro de la Diosa", "Una plegaria divina de la Diosa María que restaura salud completa al grupo.", 30, 3, 1, 3)
                .addPrereq("c_skill3")
                .addPrereq("c_skill4"));

        trees.put("clerigo", tree);
    }

    public static SkillTree getTree(String roleId) {
        if (roleId == null) return null;
        String key = roleId.toLowerCase().trim();
        // Normalizar ID de Guerrero/Mago/Arquero
        if (key.equals("warrior")) key = "guerrero";
        if (key.equals("mage")) key = "mago";
        if (key.equals("archer")) key = "arquero";
        return trees.get(key);
    }
}
