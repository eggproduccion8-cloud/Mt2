package com.mundodetronos2.skills;

import java.util.ArrayList;
import java.util.List;

public class SkillNode {
    private final String id;
    private final String name;
    private final String description;
    private final int requiredLevel;
    private final int cost;
    private final List<String> prerequisites;
    private final int x; // Coordenada X de la cuadrícula visual
    private final int y; // Coordenada Y de la cuadrícula visual

    public SkillNode(String id, String name, String description, int requiredLevel, int cost, int x, int y) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.requiredLevel = requiredLevel;
        this.cost = cost;
        this.prerequisites = new ArrayList<>();
        this.x = x;
        this.y = y;
    }

    public SkillNode addPrereq(String parentId) {
        this.prerequisites.add(parentId);
        return this;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public int getCost() {
        return cost;
    }

    public List<String> getPrerequisites() {
        return prerequisites;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
