package com.mundodetronos2.skills;

import com.mundodetronos2.progression.PlayerProgressData;
import com.mundodetronos2.progression.ProgressionManager;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public class SkillPointManager {

    public static boolean canUnlock(UUID playerId, String roleId, SkillNode node) {
        if (node == null) return false;

        PlayerProgressData data = ProgressionManager.getProgressData(playerId);

        // 1. Comprobar si ya está desbloqueado
        if (data.getUnlockedSkills().contains(node.getId().toLowerCase())) {
            return false;
        }

        // 2. Comprobar nivel requerido
        if (data.getLevel() < node.getRequiredLevel()) {
            return false;
        }

        // 3. Comprobar skill points necesarios
        if (data.getSkillPoints() < node.getCost()) {
            return false;
        }

        // 4. Comprobar requisitos previos
        for (String prereq : node.getPrerequisites()) {
            if (!data.getUnlockedSkills().contains(prereq.toLowerCase())) {
                return false;
            }
        }

        return true;
    }

    public static boolean tryUnlockSkill(UUID playerId, String roleId, String skillId, ServerPlayer player) {
        SkillTree tree = SkillTreeManager.getTree(roleId);
        if (tree == null) return false;

        SkillNode node = tree.getNode(skillId);
        if (node == null) return false;

        if (!canUnlock(playerId, roleId, node)) {
            if (player != null) {
                com.mundodetronos2.network.MessageManager.actionBar(player, "§c⚠ No cumples con los requisitos para desbloquear esta habilidad.");
            }
            return false;
        }

        PlayerProgressData data = ProgressionManager.getProgressData(playerId);

        // Deduzir puntos e insertar habilidad desbloqueada
        data.setSkillPoints(data.getSkillPoints() - node.getCost());
        data.getUnlockedSkills().add(node.getId().toLowerCase());

        ProgressionManager.markDirty();
        ProgressionManager.save(false);

        if (player != null) {
            // Sonido mágico y confirmación Action Bar
            player.serverLevel().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.EVOKER_CAST_SPELL, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 1.2F);
            com.mundodetronos2.network.MessageManager.actionBar(player, "§a✔ ¡Habilidad '" + node.getName() + "' desbloqueada con éxito!");
            com.mundodetronos2.network.NetworkManager.syncHud(player);
        }

        return true;
    }
}
