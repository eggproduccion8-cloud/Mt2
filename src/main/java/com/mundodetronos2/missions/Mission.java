package com.mundodetronos2.missions;

public class Mission {
    private String id;
    private String name;
    private String description;
    private MissionType type;
    private MissionObjective objective;
    private int xpReward;
    private boolean repeatable = false;
    private boolean groupShared = true;

    public Mission() {}

    public Mission(String id, String name, String description, MissionType type, MissionObjective objective, int xpReward) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.objective = objective;
        this.xpReward = xpReward;
        this.repeatable = false;
        this.groupShared = true;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MissionType getType() {
        return type;
    }

    public void setType(MissionType type) {
        this.type = type;
    }

    public MissionObjective getObjective() {
        return objective;
    }

    public void setObjective(MissionObjective objective) {
        this.objective = objective;
    }

    public int getXpReward() {
        return xpReward;
    }

    public void setXpReward(int xpReward) {
        this.xpReward = xpReward;
    }

    public boolean isRepeatable() {
        return repeatable;
    }

    public void setRepeatable(boolean repeatable) {
        this.repeatable = repeatable;
    }

    public boolean isGroupShared() {
        return groupShared;
    }

    public void setGroupShared(boolean groupShared) {
        this.groupShared = groupShared;
    }
}
