package com.mundodetronos2.throne;

import net.minecraft.core.BlockPos;

import java.util.UUID;

public class ThroneData {
    private UUID id;
    private UUID realmId;

    // Almacenamiento plano para perfecta persistencia GSON sin adaptadores complejos
    private int x;
    private int y;
    private int z;

    private String dimension; // Representado como string de dimensión (e.g. "minecraft:overworld")
    private int maxHealth;
    private int health;
    private ThroneState state = ThroneState.PROTECTED;
    private long cooldownEndsAt;
    private boolean eventEnabled = false;
    private UUID lastAttacker;
    private int protectionRadius = 15; // default 15 blocks radius (30x30 protection zone)

    public ThroneData() {}

    public ThroneData(UUID id, UUID realmId, BlockPos pos, String dimension, int maxHealth) {
        this.id = id;
        this.realmId = realmId;
        if (pos != null) {
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
        }
        this.dimension = dimension;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.state = ThroneState.PROTECTED;
    }

    // Getters y Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getRealmId() { return realmId; }
    public void setRealmId(UUID realmId) { this.realmId = realmId; }

    public BlockPos getPos() {
        return new BlockPos(x, y, z);
    }
    public void setPos(BlockPos pos) {
        if (pos != null) {
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
        }
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }

    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }

    public int getMaxHealth() { return maxHealth; }
    public void setMaxHealth(int maxHealth) { this.maxHealth = maxHealth; }

    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }

    public ThroneState getState() { return state; }
    public void setState(ThroneState state) { this.state = state; }

    public long getCooldownEndsAt() { return cooldownEndsAt; }
    public void setCooldownEndsAt(long cooldownEndsAt) { this.cooldownEndsAt = cooldownEndsAt; }

    public boolean isEventEnabled() { return eventEnabled; }
    public void setEventEnabled(boolean eventEnabled) { this.eventEnabled = eventEnabled; }

    public UUID getLastAttacker() { return lastAttacker; }
    public void setLastAttacker(UUID lastAttacker) { this.lastAttacker = lastAttacker; }

    public int getProtectionRadius() { return protectionRadius; }
    public void setProtectionRadius(int protectionRadius) { this.protectionRadius = Math.max(1, protectionRadius); }
}
