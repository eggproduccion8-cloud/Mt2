package com.mundodetronos2.missions;

public class MissionObjective {
    private String id = ""; // ej: "minecraft:diamond" o "minecraft:zombie"
    private String nbt = ""; // Opcional, para items de mods o especiales
    private int targetCount = 0;

    public MissionObjective() {}

    public MissionObjective(String id, int targetCount) {
        this.id = id;
        this.nbt = "";
        this.targetCount = targetCount;
    }

    public MissionObjective(String id, String nbt, int targetCount) {
        this.id = id;
        this.nbt = nbt;
        this.targetCount = targetCount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNbt() {
        return nbt;
    }

    public void setNbt(String nbt) {
        this.nbt = nbt;
    }

    public int getTargetCount() {
        return targetCount;
    }

    public void setTargetCount(int targetCount) {
        this.targetCount = targetCount;
    }
}
