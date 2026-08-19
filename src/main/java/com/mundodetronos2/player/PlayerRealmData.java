package com.mundodetronos2.player;

import com.mundodetronos2.realm.Role;

import java.util.UUID;

public class PlayerRealmData {
    private UUID playerId;
    private UUID realmId;
    private Role role;
    private long joinedAt;

    public PlayerRealmData() {}

    public PlayerRealmData(UUID playerId, UUID realmId, Role role) {
        this.playerId = playerId;
        this.realmId = realmId;
        this.role = role;
        this.joinedAt = System.currentTimeMillis();
    }

    public UUID getPlayerId() { return playerId; }
    public void setPlayerId(UUID playerId) { this.playerId = playerId; }

    public UUID getRealmId() { return realmId; }
    public void setRealmId(UUID realmId) { this.realmId = realmId; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public long getJoinedAt() { return joinedAt; }
    public void setJoinedAt(long joinedAt) { this.joinedAt = joinedAt; }
}
