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
    private int protectionRadius = 75; // 75 blocks radius (150x150 defense zone)
    private int throneLevel = 1;
    private int wallLevel = 1;
    private java.util.List<int[]> wallBlocks = new java.util.ArrayList<>();

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
        this.protectionRadius = 75;
    }

    public int getThroneLevel() { return throneLevel; }
    public void setThroneLevel(int throneLevel) { this.throneLevel = Math.max(1, Math.min(3, throneLevel)); }

    public int getWallLevel() { return wallLevel; }
    public void setWallLevel(int wallLevel) { this.wallLevel = Math.max(1, Math.min(3, wallLevel)); }

    public java.util.List<int[]> getWallBlocksRaw() {
        if (wallBlocks == null) wallBlocks = new java.util.ArrayList<>();
        return wallBlocks;
    }

    public void setWallBlocksRaw(java.util.List<int[]> wallBlocks) {
        this.wallBlocks = wallBlocks;
    }

    public boolean isWallBlock(BlockPos pos) {
        if (pos == null || wallBlocks == null) return false;
        int px = pos.getX(), py = pos.getY(), pz = pos.getZ();
        for (int[] arr : wallBlocks) {
            if (arr != null && arr.length >= 3 && arr[0] == px && arr[1] == py && arr[2] == pz) {
                return true;
            }
        }
        return false;
    }

    public void addWallBlock(BlockPos pos) {
        if (pos != null) {
            if (wallBlocks == null) wallBlocks = new java.util.ArrayList<>();
            if (!isWallBlock(pos)) {
                wallBlocks.add(new int[]{pos.getX(), pos.getY(), pos.getZ()});
            }
        }
    }

    public void removeWallBlock(BlockPos pos) {
        if (pos != null && wallBlocks != null) {
            int px = pos.getX(), py = pos.getY(), pz = pos.getZ();
            wallBlocks.removeIf(arr -> arr != null && arr.length >= 3 && arr[0] == px && arr[1] == py && arr[2] == pz);
        }
    }

    public void clearWallBlocks() {
        if (wallBlocks != null) {
            wallBlocks.clear();
        }
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
