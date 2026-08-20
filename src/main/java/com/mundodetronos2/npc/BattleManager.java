package com.mundodetronos2.npc;

public class BattleManager {
    private static boolean battleActive = false;

    public static boolean isBattleActive() {
        return battleActive;
    }

    public static void setBattleActive(boolean active) {
        battleActive = active;
    }
}
