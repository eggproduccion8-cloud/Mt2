package com.mundodetronos2.realm;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RealmData {
    private UUID id;
    private String name;
    private UUID ownerId;
    private List<UUID> members = new ArrayList<>();
    private List<UUID> invites = new ArrayList<>();
    private int maxPlayers;
    private int maxLives;
    private int currentLives;
    private String color;
    private long createdAt;
    private boolean active = true;
    private UUID throneId; // ID del trono asociado (puede ser null inicialmente)

    // Puntos compartidos del reino (Default 1000)
    private int sharedPoints = 1000;

    public RealmData() {}

    public RealmData(UUID id, String name, UUID ownerId, int maxPlayers, int maxLives, String color) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.maxPlayers = maxPlayers;
        this.maxLives = maxLives;
        this.currentLives = maxLives;
        this.color = color;
        this.createdAt = System.currentTimeMillis();
        this.members.add(ownerId);
        this.sharedPoints = 1000;
    }

    // Getters y Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }

    public List<UUID> getMembers() { return members; }
    public void setMembers(List<UUID> members) { this.members = members; }

    public List<UUID> getInvites() { return invites; }
    public void setInvites(List<UUID> invites) { this.invites = invites; }

    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }

    public int getMaxLives() { return maxLives; }
    public void setMaxLives(int maxLives) { this.maxLives = maxLives; }

    public int getCurrentLives() { return currentLives; }
    public void setCurrentLives(int currentLives) { this.currentLives = currentLives; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public UUID getThroneId() { return throneId; }
    public void setThroneId(UUID throneId) { this.throneId = throneId; }

    public int getSharedPoints() { return sharedPoints; }
    public void setSharedPoints(int sharedPoints) {
        this.sharedPoints = Math.max(0, sharedPoints); // No puede bajar de 0
    }
}
