package com.mundodetronos2.progression;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerProgressData {
    private UUID playerId;
    private int level = 1;
    private int xp = 0;
    private int skillPoints = 0;
    private List<String> unlockedSkills = new ArrayList<>();
    private int backpackTier = 1;
    private int coins = 0;

    public PlayerProgressData() {}

    public int getCoins() {
        return Math.max(0, coins);
    }

    public void setCoins(int coins) {
        this.coins = Math.max(0, coins);
    }

    public int getBackpackTier() {
        return Math.max(1, Math.min(3, backpackTier));
    }

    public void setBackpackTier(int backpackTier) {
        this.backpackTier = Math.max(1, Math.min(3, backpackTier));
    }

    public PlayerProgressData(UUID playerId) {
        this.playerId = playerId;
        this.level = 1;
        this.xp = 0;
        this.skillPoints = 0;
        this.unlockedSkills = new ArrayList<>();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, Math.min(100, level));
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = Math.max(0, xp);
    }

    public int getSkillPoints() {
        return skillPoints;
    }

    public void setSkillPoints(int skillPoints) {
        this.skillPoints = Math.max(0, skillPoints);
    }

    public List<String> getUnlockedSkills() {
        if (unlockedSkills == null) {
            unlockedSkills = new ArrayList<>();
        }
        return unlockedSkills;
    }

    public void setUnlockedSkills(List<String> unlockedSkills) {
        this.unlockedSkills = unlockedSkills;
    }
}
