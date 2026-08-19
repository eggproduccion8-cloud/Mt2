package com.mundodetronos2.role;

public enum PlayerRole {
    NONE("none"),
    BERSERKER("berserker"),
    GUERRERO("guerrero"),
    MAGO("mago"),
    ARQUERO("arquero"),
    PALADIN("paladin"),
    DRACONICO("draconico"),
    CLERIGO("clerigo");

    private final String id;

    PlayerRole(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static PlayerRole fromString(String str) {
        if (str == null) return NONE;
        String clean = str.trim().toUpperCase();
        if (clean.equals("WARRIOR") || clean.equals("GUERRERO")) return GUERRERO;
        if (clean.equals("BERSERKER")) return BERSERKER;
        if (clean.equals("MAGE") || clean.equals("MAGO")) return MAGO;
        if (clean.equals("ARCHER") || clean.equals("ARQUERO")) return ARQUERO;
        if (clean.equals("PALADIN") || clean.equals("PALADÍN")) return PALADIN;
        if (clean.equals("DRACONICO") || clean.equals("DRACÓNICO")) return DRACONICO;
        if (clean.equals("CLERIGO") || clean.equals("CLÉRIGO")) return CLERIGO;
        try {
            return PlayerRole.valueOf(clean);
        } catch (Exception e) {
            return NONE;
        }
    }
}
