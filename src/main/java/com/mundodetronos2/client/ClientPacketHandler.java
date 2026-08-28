package com.mundodetronos2.client;

import com.mundodetronos2.config.ConfigManager;
import com.mundodetronos2.gui.GoddessCinematicScreen;
import com.mundodetronos2.gui.GoddessPortalScreen;
import com.mundodetronos2.gui.MainGuiScreen;
import com.mundodetronos2.gui.RoleCardScreen;
import com.mundodetronos2.gui.RoleSelectionScreen;
import com.mundodetronos2.network.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.UUID;

public class ClientPacketHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    // Variables locales para renderizar la barra de vida del trono
    public static String targetThroneRealmName = "";
    public static int targetThroneHealth = 0;
    public static int targetThroneMaxHealth = 0;
    public static String targetThroneState = "";
    public static long targetThroneLastHitTime = 0;

    // --- NUEVOS DATOS DE ALERTA DE ATAQUE AL TRONO ---
    public static boolean activeAttackActive = false;
    public static String activeAttackBaseName = "";
    public static int activeAttackThroneHp = 0;
    public static int activeAttackThroneMaxHp = 0;
    public static String activeAttackAttackerTeamName = "";
    public static int activeAttackSecondsLeft = 0;

    public static boolean isAttackActive() {
        return activeAttackActive;
    }

    public static int getAttackSecondsLeft() {
        return activeAttackSecondsLeft;
    }

    // --- DATOS DEL HUD LOCAL CLIENT-SIDE ---
    public static int hudThroneLives = 5;
    public static int hudSharedPoints = 1000;
    public static int hudRemainingSeconds = 14400; // 4 horas
    public static long hudLastSyncTime = 0;
    public static String hudPlayerRole = "none";
    public static int hudPlayerLevel = 1;
    public static int hudCurrentXp = 0;
    public static int hudNeededXp = 150;
    public static int hudTutorialLevel = 1;
    public static String hudActiveMissionTitle = "";
    public static String hudActiveMissionProgress = "";
    public static String hudTeamName = "NINGUNO";
    public static int clientThroneX = 0;
    public static int clientThroneY = 0;
    public static int clientThroneZ = 0;
    public static String clientThroneDim = "";

    public static void handleShowMessage(String message, boolean isError) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            // Mostrar en Action Bar encima de los ítems de la barra para evitar saturar notificaciones laterales
            mc.player.displayClientMessage(Component.literal(message), true);
        }
    }

    public static void handleOpenMainGui(NetworkManager.S2COpenMainGuiPacket data, String roleName, int roleLevel) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new MainGuiScreen(data, roleName, roleLevel));
    }

    public static void handleRealmList(List<NetworkManager.S2CRealmListPacket.RealmInfo> list) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof MainGuiScreen) {
            ((MainGuiScreen) mc.screen).updateRealmList(list);
        }
    }

    public static void handleSyncThroneData(String realmName, int health, int maxHealth, String state) {
        targetThroneRealmName = realmName;
        targetThroneHealth = health;
        targetThroneMaxHealth = maxHealth;
        targetThroneState = state;
        targetThroneLastHitTime = System.currentTimeMillis();
    }

    public static void handlePlaySound(double x, double y, double z, String soundType) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        String soundIdStr;
        float volume = 1.0f;
        float pitch = 1.0f;

        switch (soundType) {
            case "hit":
                soundIdStr = ConfigManager.get().hitSound;
                pitch = 1.1f + mc.level.random.nextFloat() * 0.2f;
                break;
            case "repair_start":
                soundIdStr = ConfigManager.get().repairSound;
                pitch = 0.8f;
                break;
            case "repair_tick":
                soundIdStr = "minecraft:block.iron_trapdoor.close";
                pitch = 1.5f;
                volume = 0.4f;
                break;
            case "repair_done":
                soundIdStr = "minecraft:entity.player.levelup";
                pitch = 1.0f;
                break;
            default:
                soundIdStr = "minecraft:block.anvil.hit";
                break;
        }

        try {
            ResourceLocation soundRl = new ResourceLocation(soundIdStr);
            SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(soundRl);
            if (sound != null) {
                mc.level.playLocalSound(x, y, z, sound, SoundSource.BLOCKS, volume, pitch, false);
            }
        } catch (Exception e) {
            LOGGER.error("Error al reproducir sonido del trono: " + soundIdStr, e);
        }
    }

    // --- MANEJO DE SELECCIÓN Y CARNETS DE ROL CLIENT-SIDE ---

    public static void handleOpenRoleSelection() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new RoleSelectionScreen());
    }

    public static void handleOpenRoleCard(UUID playerId, String playerName, String roleName, int level, String realmName, String realmRole, int currentXp, int neededXp) {
        hudCurrentXp = currentXp;
        hudNeededXp = neededXp;
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new RoleCardScreen(playerId, playerName, roleName, level, realmName, realmRole, currentXp, neededXp));
        if (mc.player != null && mc.level != null) {
            mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, 1.0F, false);
        }
    }

    public static void handleStartCinematic(String roleId) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new GoddessCinematicScreen(roleId));
    }

    // --- MANEJO DE SINCRONIZACIÓN DE HUD ---
    public static void handleHudSync(int lives, int points, int seconds, String role, int level, int currentXp, int neededXp, int tutorialLevel, String missionTitle, String missionProgress, String teamName, int tx, int ty, int tz, String tDim) {
        hudThroneLives = lives;
        hudSharedPoints = points;
        hudRemainingSeconds = seconds;
        hudPlayerRole = role != null ? role.toLowerCase() : "none";
        hudPlayerLevel = level;
        hudCurrentXp = currentXp;
        hudNeededXp = neededXp > 0 ? neededXp : 150;
        hudTutorialLevel = Math.max(1, tutorialLevel);
        hudActiveMissionTitle = missionTitle != null ? missionTitle : "";
        hudActiveMissionProgress = missionProgress != null ? missionProgress : "";
        hudTeamName = teamName != null && !teamName.isEmpty() ? teamName : "NINGUNO";
        clientThroneX = tx;
        clientThroneY = ty;
        clientThroneZ = tz;
        clientThroneDim = tDim != null ? tDim : "";
        hudLastSyncTime = System.currentTimeMillis();
    }

    // --- MANEJO DE PORTAL DE OFRENDA ---
    public static void handleOpenGoddessPortal() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new GoddessPortalScreen());
    }

    // --- MANEJO DE CINEMÁTICA DE RESURRECCIÓN DE LA DIOSA ---
    public static void handleOpenDeathRebirthCinematic() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new com.mundodetronos2.gui.GoddessDeathRebirthScreen());
    }

    // --- MANEJO DE APERTURA DEL YUNQUE DE ROLES ---
    public static void handleOpenRoleAnvil() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new com.mundodetronos2.gui.RoleAnvilScreen());
    }

    // --- MANEJO DE APERTURA DEL MENÚ DE INTERACCIÓN DEL ALTAR ---
    public static void handleOpenAltarOption(boolean hasRole) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new com.mundodetronos2.gui.AltarInteractionScreen(hasRole));
    }

    // --- MANEJO DE APERTURA DE DIÁLOGO DE LA DIOSA ---

    // --- MANEJO DE APERTURA DE DIÁLOGO GENERAL DE NPC ---

    // --- MANEJO DE ALERTA DE ATAQUE AL TRONO ---
    public static void handleThroneAttackAlert(boolean active, String baseName, int hp, int maxHp, String attacker, int seconds) {
        activeAttackActive = active;
        activeAttackBaseName = baseName;
        activeAttackThroneHp = hp;
        activeAttackThroneMaxHp = maxHp;
        activeAttackAttackerTeamName = attacker;
        activeAttackSecondsLeft = seconds;
    }

    // --- MANEJO DE APERTURA DEL MINIJUEGO DE DESACTIVACIÓN ---
    public static void handleOpenDefuseMinigame(net.minecraft.core.BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new com.mundodetronos2.gui.DefuseMinigameScreen(pos));
    }

    // --- MANEJO DE APERTURA DEL EDITOR ADMINISTRATIVO DE NPC ---


    // --- MANEJO DE APERTURA DEL ÁRBOL DE HABILIDADES ---
    public static void handleOpenSkillTree(String roleId, int level, int skillPoints, java.util.List<String> unlockedSkills) {
        Minecraft mc = Minecraft.getInstance();
        com.mundodetronos2.gui.RoleSkillTreeScreen screen = new com.mundodetronos2.gui.RoleSkillTreeScreen(roleId, level);
        screen.updateData(skillPoints, unlockedSkills);
        mc.setScreen(screen);
    }

    // --- MANEJO DE APERTURA DE LA MESA DEL HERRERO ---
    public static void handleOpenBlacksmithTable() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new com.mundodetronos2.gui.BlacksmithTableScreen());
    }
}
