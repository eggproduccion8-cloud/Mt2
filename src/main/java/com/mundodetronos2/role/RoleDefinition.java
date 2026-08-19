package com.mundodetronos2.role;

import net.minecraft.network.chat.Component;

public class RoleDefinition {
    private final String id;
    private final Component displayName;
    private final Component description;
    private final int color;

    public RoleDefinition(String id, Component displayName, Component description, int color) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public Component getDisplayName() {
        return displayName;
    }

    public Component getDescription() {
        return description;
    }

    public int getColor() {
        return color;
    }
}
