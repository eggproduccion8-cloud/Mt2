package com.mundodetronos2.missions;

import java.util.HashMap;
import java.util.Map;

public class MissionRegistry {
    private static final Map<String, Mission> defaults = new HashMap<>();

    static {
        // Misiones de Mobs
        register(new Mission(
                "m_mobs_zombie",
                "Exterminio de Caminantes",
                "Vuestro reino debe unirse para exterminar 50 Zombies y purgar las tierras.",
                MissionType.KILL_MOBS,
                new MissionObjective("minecraft:zombie", 50),
                250
        ));

        register(new Mission(
                "m_mobs_skeleton",
                "Huesos Rotos",
                "Eliminad 40 Esqueletos que acechan en la oscuridad de los bosques.",
                MissionType.KILL_MOBS,
                new MissionObjective("minecraft:skeleton", 40),
                300
        ));

        // Misiones de Items (Recolección)
        register(new Mission(
                "m_items_diamond",
                "Tributo de Diamante",
                "Reunid y entregad 10 Diamantes en el bloque del Gremio para financiar el reino.",
                MissionType.COLLECT_ITEMS,
                new MissionObjective("minecraft:diamond", 10),
                500
        ));

        register(new Mission(
                "m_items_gold",
                "Reservas de Oro de la Diosa",
                "Entregad 32 Lingotes de Oro para consagrar los altares de la Diosa María.",
                MissionType.COLLECT_ITEMS,
                new MissionObjective("minecraft:gold_ingot", 32),
                400
        ));

        // Misión Especial Boss
        register(new Mission(
                "m_boss_dragon",
                "El Fin del Velo",
                "Derrotad al temible Ender Dragon para obtener la gracia eterna de la Diosa.",
                MissionType.KILL_SPECIFIC_MOB,
                new MissionObjective("minecraft:ender_dragon", 1),
                1500
        ));
    }

    public static void register(Mission mission) {
        defaults.put(mission.getId().toLowerCase(), mission);
    }

    public static Mission getMission(String id) {
        if (id == null) return null;
        return defaults.get(id.toLowerCase());
    }

    public static Map<String, Mission> getDefaults() {
        return defaults;
    }
}
