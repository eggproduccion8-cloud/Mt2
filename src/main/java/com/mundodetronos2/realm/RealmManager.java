package com.mundodetronos2.realm;

import com.mundodetronos2.config.ConfigManager;
import com.mundodetronos2.data.SaveManager;
import com.mundodetronos2.player.PlayerRealmData;
import com.mundodetronos2.throne.ThroneManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RealmManager {
    private static final Logger LOGGER = LogManager.getLogger();

    // Mapas en memoria cache
    private static final Map<UUID, RealmData> realms = new ConcurrentHashMap<>();
    private static final Map<UUID, PlayerRealmData> playerRealmDataMap = new ConcurrentHashMap<>();
    private static final Map<UUID, InviteData> invitesMap = new ConcurrentHashMap<>();

    // 10 Equipos Predefinidos por Color
    public static final String[] PREDEFINED_COLORS = {
        "rojo", "azul", "verde", "amarillo", "morado", "cian", "naranja", "rosa", "blanco", "negro"
    };

    public static final String[] PREDEFINED_HEX_COLORS = {
        "#FF5555", "#5555FF", "#55FF55", "#FFFF55", "#AA00AA", "#55FFFF", "#FFAA00", "#FF55FF", "#FFFFFF", "#555555"
    };

    public static void init() {
        realms.clear();
        playerRealmDataMap.clear();
        invitesMap.clear();

        realms.putAll(SaveManager.loadRealms());
        playerRealmDataMap.putAll(SaveManager.loadPlayers());
        invitesMap.putAll(SaveManager.loadInvites());

        // Asegurar que existan los 10 equipos predefinidos
        ensurePredefinedTeams();

        LOGGER.info("RealmManager cargado: {} reinos, {} datos de jugadores, {} invitaciones.",
                realms.size(), playerRealmDataMap.size(), invitesMap.size());
    }

    public static void ensurePredefinedTeams() {
        for (int i = 0; i < PREDEFINED_COLORS.length; i++) {
            String colorKey = PREDEFINED_COLORS[i];
            String hexColor = PREDEFINED_HEX_COLORS[i];
            String defaultName = "Equipo " + colorKey.substring(0, 1).toUpperCase() + colorKey.substring(1);

            RealmData existing = getRealmByColorKey(colorKey);
            if (existing == null) {
                UUID teamId = UUID.nameUUIDFromBytes(("team_color_" + colorKey).getBytes());
                RealmData team = new RealmData(teamId, defaultName, null, 6, ConfigManager.get().defaultMaxLives, hexColor);
                realms.put(teamId, team);
            }
        }
        SaveManager.markDirty();
        save(false);
    }

    public static RealmData getRealmByColorKey(String colorKey) {
        if (colorKey == null) return null;
        for (RealmData r : realms.values()) {
            if (r.getColor() != null && r.getColor().equalsIgnoreCase(colorKey)) {
                return r;
            }
        }
        // Fallback por nombre predeterminado
        for (RealmData r : realms.values()) {
            if (r.getName().equalsIgnoreCase("Equipo " + colorKey) || r.getName().equalsIgnoreCase(colorKey)) {
                return r;
            }
        }
        return null;
    }

    public static void save(boolean forceSync) {
        SaveManager.saveAll(realms, ThroneManager.getThronesMap(), playerRealmDataMap, invitesMap, forceSync);
    }

    public static Map<UUID, RealmData> getRealmsMap() {
        return realms;
    }

    public static Map<UUID, PlayerRealmData> getPlayerRealmDataMap() {
        return playerRealmDataMap;
    }

    public static Map<UUID, InviteData> getInvitesMap() {
        return invitesMap;
    }

    public static boolean joinTeam(ServerPlayer player, String colorKey) {
        RealmData team = getRealmByColorKey(colorKey);
        if (team == null) return false;

        UUID playerId = player.getUUID();
        if (playerRealmDataMap.containsKey(playerId)) {
            return false; // Ya pertenece a un equipo
        }

        if (team.getMembers().size() >= team.getMaxPlayers()) {
            return false; // Equipo completo (6/6)
        }

        boolean firstMember = team.getMembers().isEmpty();
        team.getMembers().add(playerId);

        Role role = firstMember ? Role.OWNER : Role.MEMBER;
        if (firstMember) {
            team.setOwnerId(playerId);
            giveLeaderKey(player);
        }

        PlayerRealmData prd = new PlayerRealmData(playerId, team.getId(), role);
        playerRealmDataMap.put(playerId, prd);

        SaveManager.markDirty();
        save(false);
        return true;
    }

    public static void giveLeaderKey(ServerPlayer player) {
        ItemStack keyStack = new ItemStack(Items.TRIPWIRE_HOOK);
        keyStack.getOrCreateTag().putBoolean("mundodetronos2:team_leader_key", true);
        keyStack.setHoverName(Component.literal("§6§lLlave del Líder"));
        player.getInventory().add(keyStack);
    }

    public static boolean leaveRealm(UUID playerId) {
        PlayerRealmData prd = playerRealmDataMap.get(playerId);
        if (prd == null) return false;

        RealmData realm = realms.get(prd.getRealmId());
        if (realm == null) return false;

        realm.getMembers().remove(playerId);
        playerRealmDataMap.remove(playerId);

        // Si era el líder, transferir liderazgo al siguiente miembro disponible
        if (prd.getRole() == Role.OWNER) {
            if (!realm.getMembers().isEmpty()) {
                UUID nextLeader = realm.getMembers().iterator().next();
                realm.setOwnerId(nextLeader);
                PlayerRealmData nextPrd = playerRealmDataMap.get(nextLeader);
                if (nextPrd != null) {
                    nextPrd.setRole(Role.OWNER);
                }
            } else {
                realm.setOwnerId(null); // Sin dueño/líder
            }
        }

        SaveManager.markDirty();
        save(false);
        return true;
    }

    // Acciones Core de Soporte Admin
    public static RealmData createRealm(String name, UUID ownerId, String ownerName) {
        // En la nueva estructura, los equipos son predefinidos.
        return null;
    }

    public static boolean deleteRealm(UUID realmId) {
        return false;
    }

    public static RealmData getRealm(UUID id) {
        return realms.get(id);
    }

    public static RealmData getRealmByName(String name) {
        for (RealmData r : realms.values()) {
            if (r.getName().equalsIgnoreCase(name)) {
                return r;
            }
        }
        return null;
    }

    public static PlayerRealmData getPlayerRealmData(UUID playerId) {
        return playerRealmDataMap.get(playerId);
    }

    public static RealmData getPlayerRealm(UUID playerId) {
        PlayerRealmData prd = getPlayerRealmData(playerId);
        return prd != null ? realms.get(prd.getRealmId()) : null;
    }

    public static boolean sendInvite(UUID realmId, UUID targetId, String targetName, UUID senderId, String senderName) {
        RealmData realm = realms.get(realmId);
        if (realm == null) return false;

        if (realm.getMembers().size() >= realm.getMaxPlayers()) return false;
        if (playerRealmDataMap.containsKey(targetId)) return false;

        for (InviteData invite : invitesMap.values()) {
            if (invite.getRealmId().equals(realmId) && invite.getTargetPlayerId().equals(targetId) && !invite.isExpired()) {
                return false;
            }
        }

        UUID inviteId = UUID.randomUUID();
        long expiresAt = System.currentTimeMillis() + 600000;
        InviteData invite = new InviteData(inviteId, realmId, targetId, senderId, senderName, realm.getName(), expiresAt);
        invitesMap.put(inviteId, invite);
        realm.getInvites().add(inviteId);

        SaveManager.markDirty();
        save(false);
        return true;
    }

    public static boolean acceptInvite(UUID inviteId, UUID playerId) {
        InviteData invite = invitesMap.get(inviteId);
        if (invite == null || invite.isExpired() || !invite.getTargetPlayerId().equals(playerId)) {
            if (invite != null) {
                invitesMap.remove(inviteId);
                SaveManager.markDirty();
            }
            return false;
        }

        RealmData realm = realms.get(invite.getRealmId());
        if (realm == null || realm.getMembers().size() >= realm.getMaxPlayers() || playerRealmDataMap.containsKey(playerId)) {
            invitesMap.remove(inviteId);
            SaveManager.markDirty();
            return false;
        }

        realm.getMembers().add(playerId);
        PlayerRealmData prd = new PlayerRealmData(playerId, realm.getId(), Role.MEMBER);
        playerRealmDataMap.put(playerId, prd);

        invitesMap.remove(inviteId);
        realm.getInvites().remove(inviteId);

        SaveManager.markDirty();
        save(false);
        return true;
    }

    public static boolean rejectInvite(UUID inviteId, UUID playerId) {
        InviteData invite = invitesMap.get(inviteId);
        if (invite == null || !invite.getTargetPlayerId().equals(playerId)) {
            return false;
        }

        RealmData realm = realms.get(invite.getRealmId());
        if (realm != null) {
            realm.getInvites().remove(inviteId);
        }

        invitesMap.remove(inviteId);
        SaveManager.markDirty();
        save(false);
        return true;
    }

    public static boolean kickMember(UUID ownerId, UUID targetId) {
        PlayerRealmData prdOwner = playerRealmDataMap.get(ownerId);
        PlayerRealmData prdTarget = playerRealmDataMap.get(targetId);

        if (prdOwner == null || prdTarget == null) return false;
        if (!prdOwner.getRealmId().equals(prdTarget.getRealmId())) return false;
        if (prdOwner.getRole() != Role.OWNER) return false;
        if (ownerId.equals(targetId)) return false;

        RealmData realm = realms.get(prdOwner.getRealmId());
        if (realm != null) {
            realm.getMembers().remove(targetId);
        }
        playerRealmDataMap.remove(targetId);

        SaveManager.markDirty();
        save(false);
        return true;
    }

    public static List<InviteData> getPlayerInvites(UUID playerId) {
        List<InviteData> playerInvites = new ArrayList<>();
        for (InviteData invite : invitesMap.values()) {
            if (invite.getTargetPlayerId().equals(playerId) && !invite.isExpired()) {
                playerInvites.add(invite);
            }
        }
        return playerInvites;
    }
}
