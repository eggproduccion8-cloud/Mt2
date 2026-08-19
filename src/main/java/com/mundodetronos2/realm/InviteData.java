package com.mundodetronos2.realm;

import java.util.UUID;

public class InviteData {
    private UUID inviteId;
    private UUID realmId;
    private UUID targetPlayerId;
    private UUID senderId;
    private long expiresAt;
    private String senderName;
    private String realmName;

    public InviteData() {}

    public InviteData(UUID inviteId, UUID realmId, UUID targetPlayerId, UUID senderId, String senderName, String realmName, long expiresAt) {
        this.inviteId = inviteId;
        this.realmId = realmId;
        this.targetPlayerId = targetPlayerId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.realmName = realmName;
        this.expiresAt = expiresAt;
    }

    public UUID getInviteId() { return inviteId; }
    public void setInviteId(UUID inviteId) { this.inviteId = inviteId; }

    public UUID getRealmId() { return realmId; }
    public void setRealmId(UUID realmId) { this.realmId = realmId; }

    public UUID getTargetPlayerId() { return targetPlayerId; }
    public void setTargetPlayerId(UUID targetPlayerId) { this.targetPlayerId = targetPlayerId; }

    public UUID getSenderId() { return senderId; }
    public void setSenderId(UUID senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getRealmName() { return realmName; }
    public void setRealmName(String realmName) { this.realmName = realmName; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
