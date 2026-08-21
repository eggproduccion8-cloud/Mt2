package com.mundodetronos2.role;

import java.util.UUID;

public class PlayerRoleData {
    private UUID playerId;
    private PlayerRole role = PlayerRole.NONE;
    private boolean hasRole = false;
    private long selectedAt;
    private int level = 1;
    private boolean initialKitClaimed = false;
    private String tutorialState = "VISITAR_SAMUEL";

    public PlayerRoleData() {}

    public PlayerRoleData(UUID playerId, PlayerRole role) {
        this.playerId = playerId;
        this.role = role;
        this.hasRole = role != PlayerRole.NONE;
        this.selectedAt = System.currentTimeMillis();
        this.level = 1;
        this.initialKitClaimed = false;
    }

    public UUID getPlayerId() { return playerId; }
    public void setPlayerId(UUID playerId) { this.playerId = playerId; }

    public PlayerRole getRole() { return role; }
    public void setRole(PlayerRole role) {
        this.role = role;
        this.hasRole = role != PlayerRole.NONE;
    }

    public boolean isHasRole() { return hasRole; }
    public void setHasRole(boolean hasRole) { this.hasRole = hasRole; }

    public long getSelectedAt() { return selectedAt; }
    public void setSelectedAt(long selectedAt) { this.selectedAt = selectedAt; }

    public int getLevel() {
        if (playerId != null) {
            return com.mundodetronos2.progression.ProgressionManager.getProgressData(playerId).getLevel();
        }
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
        if (playerId != null) {
            com.mundodetronos2.progression.PlayerProgressData pData = com.mundodetronos2.progression.ProgressionManager.getProgressData(playerId);
            if (pData.getLevel() != level) {
                pData.setLevel(level);
                com.mundodetronos2.progression.ProgressionManager.markDirty();
            }
        }
    }

    public boolean isInitialKitClaimed() { return initialKitClaimed; }
    public void setInitialKitClaimed(boolean initialKitClaimed) { this.initialKitClaimed = initialKitClaimed; }


    public String getTutorialState() { return tutorialState; }
    public void setTutorialState(String state) { this.tutorialState = state != null ? state : "VISITAR_SAMUEL"; }
}
