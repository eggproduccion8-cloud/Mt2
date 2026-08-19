package com.mundodetronos2.realm;

import com.mundodetronos2.config.ConfigManager;
import com.mundodetronos2.data.SaveManager;
import com.mundodetronos2.player.PlayerRealmData;
import com.mundodetronos2.throne.ThroneManager;
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

    public static void init() {
        realms.clear();
        playerRealmDataMap.clear();
        invitesMap.clear();

        realms.putAll(SaveManager.loadRealms());
        playerRealmDataMap.putAll(SaveManager.loadPlayers());
        invitesMap.putAll(SaveManager.loadInvites());

        LOGGER.info("RealmManager cargado: {} reinos, {} datos de jugadores, {} invitaciones.",
                realms.size(), playerRealmDataMap.size(), invitesMap.size());
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

    // Acciones Core
    public static RealmData createRealm(String name, UUID ownerId, String ownerName) {
        // Verificar si el jugador ya pertenece a un reino
        if (playerRealmDataMap.containsKey(ownerId)) {
            return null;
        }

        // Verificar si el nombre del reino ya existe
        for (RealmData r : realms.values()) {
            if (r.getName().equalsIgnoreCase(name)) {
                return null;
            }
        }

        UUID realmId = UUID.randomUUID();
        int maxPlayers = 6; // Límite exacto exigido: Máximo 6 integrantes por Equipo
        int maxLives = ConfigManager.get().defaultMaxLives;

        // Colores por defecto aleatorios o simple hex
        String[] colors = {"#FF5555", "#55FF55", "#5555FF", "#FFFF55", "#FF55FF", "#55FFFF", "#FFAA00"};
        String color = colors[Math.abs(name.hashCode()) % colors.length];

        RealmData realm = new RealmData(realmId, name, ownerId, maxPlayers, maxLives, color);
        realms.put(realmId, realm);

        PlayerRealmData prd = new PlayerRealmData(ownerId, realmId, Role.OWNER);
        playerRealmDataMap.put(ownerId, prd);

        SaveManager.markDirty();
        save(false);

        return realm;
    }

    public static boolean deleteRealm(UUID realmId) {
        RealmData realm = realms.get(realmId);
        if (realm == null) return false;

        // Quitar trono asociado si existe
        if (realm.getThroneId() != null) {
            ThroneManager.removeThroneByRealm(realmId);
        }

        // Remover miembros
        for (UUID memberId : realm.getMembers()) {
            playerRealmDataMap.remove(memberId);
        }

        // Quitar invitaciones del reino
        invitesMap.entrySet().removeIf(entry -> entry.getValue().getRealmId().equals(realmId));

        realms.remove(realmId);
        SaveManager.markDirty();
        save(false);
        return true;
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

        // Comprobar límite de jugadores
        if (realm.getMembers().size() >= realm.getMaxPlayers()) {
            return false;
        }

        // Comprobar si ya está en el reino
        if (playerRealmDataMap.containsKey(targetId)) {
            return false;
        }

        // Comprobar si ya está invitado
        for (InviteData invite : invitesMap.values()) {
            if (invite.getRealmId().equals(realmId) && invite.getTargetPlayerId().equals(targetId) && !invite.isExpired()) {
                return false;
            }
        }

        UUID inviteId = UUID.randomUUID();
        // Expira en 10 minutos
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
        if (realm == null) {
            invitesMap.remove(inviteId);
            SaveManager.markDirty();
            return false;
        }

        // Comprobar límites
        if (realm.getMembers().size() >= realm.getMaxPlayers()) {
            invitesMap.remove(inviteId);
            SaveManager.markDirty();
            return false;
        }

        // Si ya está en un reino, no puede unirse
        if (playerRealmDataMap.containsKey(playerId)) {
            invitesMap.remove(inviteId);
            SaveManager.markDirty();
            return false;
        }

        // Unir al reino
        realm.getMembers().add(playerId);
        PlayerRealmData prd = new PlayerRealmData(playerId, realm.getId(), Role.MEMBER);
        playerRealmDataMap.put(playerId, prd);

        // Sincronizar automáticamente el nivel de tutorial del nuevo integrante con su equipo
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            net.minecraft.server.level.ServerPlayer sp = server.getPlayerList().getPlayer(playerId);
            if (sp != null) {
                com.mundodetronos2.tutorial.TutorialManager.syncPlayerLevelWithTeam(sp);
            }
        }

        // Limpiar todas sus otras invitaciones para este reino o en general
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

    public static boolean leaveRealm(UUID playerId) {
        PlayerRealmData prd = playerRealmDataMap.get(playerId);
        if (prd == null) return false;

        RealmData realm = realms.get(prd.getRealmId());
        if (realm == null) return false;

        if (prd.getRole() == Role.OWNER) {
            // El dueño no puede salirse sin más, debe borrar el reino o transferirlo.
            // En este prototipo, salirse disuelve el reino por completo.
            return deleteRealm(realm.getId());
        } else {
            realm.getMembers().remove(playerId);
            playerRealmDataMap.remove(playerId);
            SaveManager.markDirty();
            save(false);
            return true;
        }
    }

    public static boolean kickMember(UUID ownerId, UUID targetId) {
        PlayerRealmData prdOwner = playerRealmDataMap.get(ownerId);
        PlayerRealmData prdTarget = playerRealmDataMap.get(targetId);

        if (prdOwner == null || prdTarget == null) return false;
        if (!prdOwner.getRealmId().equals(prdTarget.getRealmId())) return false;
        if (prdOwner.getRole() != Role.OWNER) return false;
        if (ownerId.equals(targetId)) return false; // No se puede auto-expulsar

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
