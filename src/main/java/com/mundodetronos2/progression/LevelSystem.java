package com.mundodetronos2.progression;

public class LevelSystem {

    public static int getXpNeeded(int level) {
        if (level >= 100) return 0;
        if (level <= 10) {
            return level * 150;
        } else {
            return level * level * 50;
        }
    }

    public static boolean addXp(PlayerProgressData data, int amount) {
        if (data.getLevel() >= 100) {
            data.setXp(0);
            return false;
        }

        int currentXp = data.getXp() + amount;
        boolean leveledUp = false;

        while (true) {
            int needed = getXpNeeded(data.getLevel());
            if (needed == 0) {
                data.setXp(0);
                break;
            }

            if (currentXp >= needed) {
                currentXp -= needed;
                data.setLevel(data.getLevel() + 1);
                // Otorgar 1 Skill Point por nivel ganado
                data.setSkillPoints(data.getSkillPoints() + 1);
                leveledUp = true;

                if (data.getLevel() >= 100) {
                    currentXp = 0;
                    break;
                }
            } else {
                data.setXp(currentXp);
                break;
            }
        }

        return leveledUp;
    }
}
