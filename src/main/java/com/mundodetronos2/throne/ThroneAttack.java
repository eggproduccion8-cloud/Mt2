package com.mundodetronos2.throne;

import net.minecraft.core.BlockPos;
import java.util.UUID;

public class ThroneAttack {
    private final UUID throneId;
    private final BlockPos chargePos;
    private final String dimension;
    private final UUID attackerId;
    private final String attackerTeamName;
    private int remainingSeconds = 30;

    // Para ataques a bloques de construcción
    private final boolean isBlockAttack;
    private final BlockPos baseBlockPos;

    public ThroneAttack(UUID throneId, BlockPos chargePos, String dimension, UUID attackerId, String attackerTeamName) {
        this.throneId = throneId;
        this.chargePos = chargePos;
        this.dimension = dimension;
        this.attackerId = attackerId;
        this.attackerTeamName = attackerTeamName;
        this.remainingSeconds = 30; // 30 seconds for Throne
        this.isBlockAttack = false;
        this.baseBlockPos = null;
    }

    public ThroneAttack(UUID blockAttackId, BlockPos baseBlockPos, BlockPos chargePos, String dimension, UUID attackerId, String attackerTeamName, boolean isBlockAttack, int durationSeconds) {
        this.throneId = blockAttackId;
        this.chargePos = chargePos;
        this.dimension = dimension;
        this.attackerId = attackerId;
        this.attackerTeamName = attackerTeamName;
        this.remainingSeconds = durationSeconds; // 10 seconds for Wall
        this.isBlockAttack = isBlockAttack;
        this.baseBlockPos = baseBlockPos;
    }

    public UUID getThroneId() {
        return throneId;
    }

    public BlockPos getChargePos() {
        return chargePos;
    }

    public String getDimension() {
        return dimension;
    }

    public UUID getAttackerId() {
        return attackerId;
    }

    public String getAttackerTeamName() {
        return attackerTeamName;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(int remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public boolean isBlockAttack() {
        return isBlockAttack;
    }

    public BlockPos getBaseBlockPos() {
        return baseBlockPos;
    }

    public void tickSecond() {
        if (remainingSeconds > 0) {
            remainingSeconds--;
        }
    }
}
