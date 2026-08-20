package com.mundodetronos2.events;

import com.mundodetronos2.realm.RealmData;
import com.mundodetronos2.realm.RealmManager;
import com.mundodetronos2.throne.ThroneData;
import com.mundodetronos2.throne.ThroneManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DeathSpectatorManager {

    public static class SpectatorSession {
        public final UUID playerId;
        public final String deadPlayerName;
        public final String dimension;
        public final BlockPos deathPos;
        public int remainingSeconds;
        public final boolean isThroneDestruction;

        public SpectatorSession(UUID playerId, String deadPlayerName, String dimension, BlockPos deathPos, int seconds, boolean isThroneDestruction) {
            this.playerId = playerId;
            this.deadPlayerName = deadPlayerName;
            this.dimension = dimension;
            this.deathPos = deathPos;
            this.remainingSeconds = seconds;
            this.isThroneDestruction = isThroneDestruction;
        }
    }

    private static final Map<UUID, SpectatorSession> activeSessions = new ConcurrentHashMap<>();
    private static int tickCounter = 0;

    public static void startPlayerDeathSpectating(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (activeSessions.containsKey(playerId)) return;

        BlockPos pos = player.blockPosition();
        String dim = player.level().dimension().location().toString();
        SpectatorSession session = new SpectatorSession(playerId, player.getScoreboardName(), dim, pos, 10, false);

        player.setGameMode(GameType.SPECTATOR);
        activeSessions.put(playerId, session);

        player.sendSystemMessage(Component.literal("§c[Has muerto] §e" + player.getScoreboardName() + " ha muerto. Reapareciendo en 10 segundos..."));
    }

    public static void triggerThroneDestructionSpectating(net.minecraft.server.MinecraftServer server, RealmData realm) {
        if (server == null || realm == null) return;

        for (UUID memberId : realm.getMembers()) {
            ServerPlayer player = server.getPlayerList().getPlayer(memberId);
            if (player != null) {
                SpectatorSession existing = activeSessions.get(memberId);
                if (existing != null && existing.isThroneDestruction) continue;

                BlockPos pos = player.blockPosition();
                String dim = player.level().dimension().location().toString();
                SpectatorSession session = new SpectatorSession(memberId, player.getScoreboardName(), dim, pos, 25, true);

                player.setGameMode(GameType.SPECTATOR);
                activeSessions.put(memberId, session);

                player.sendSystemMessage(Component.literal("§c[Mundo de Tronos] Tu Trono ha sido destruido. Sobreviviste a la caída del reino. Regresarás a tu Trono en 25 segundos."));
            }
        }
    }

    public static void tick(net.minecraft.server.MinecraftServer server) {
        if (activeSessions.isEmpty() || server == null) return;

        tickCounter++;
        if (tickCounter % 20 != 0) return;

        for (SpectatorSession session : activeSessions.values()) {
            session.remainingSeconds--;

            ServerPlayer player = server.getPlayerList().getPlayer(session.playerId);
            if (player != null) {
                if (session.remainingSeconds > 0) {
                    com.mundodetronos2.network.MessageManager.actionBar(player, "§cRespawn en " + session.remainingSeconds + "s...");
                } else {
                    respawnPlayer(player, session);
                    activeSessions.remove(session.playerId);
                }
            } else if (session.remainingSeconds <= 0) {
                activeSessions.remove(session.playerId);
            }
        }
    }

    private static void respawnPlayer(ServerPlayer player, SpectatorSession session) {
        player.setGameMode(GameType.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.fallDistance = 0.0F;
        player.setDeltaMovement(0, 0, 0);

        BlockPos targetPos = null;
        ServerLevel targetLevel = player.serverLevel();

        RealmData rData = RealmManager.getPlayerRealm(player.getUUID());
        if (rData != null && rData.getThroneId() != null) {
            ThroneData tData = ThroneManager.getThroneById(rData.getThroneId());
            if (tData != null && tData.getPos() != null) {
                targetPos = tData.getPos();
                try {
                    ResourceLocation dimRl = new ResourceLocation(tData.getDimension());
                    ServerLevel level = player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimRl));
                    if (level != null) targetLevel = level;
                } catch (Exception ignored) {}
            }
        }

        if (targetPos != null) {
            player.teleportTo(targetLevel, targetPos.getX() + 0.5D, targetPos.getY() + 2.0D, targetPos.getZ() + 0.5D, player.getYRot(), player.getXRot());
            player.sendSystemMessage(Component.literal("§a[Mundo de Tronos] Reapareciste en el Trono de tu equipo."));
        } else {
            BlockPos spawnPos = targetLevel.getSharedSpawnPos();
            player.teleportTo(targetLevel, spawnPos.getX() + 0.5D, spawnPos.getY() + 1.0D, spawnPos.getZ() + 0.5D, player.getYRot(), player.getXRot());
            player.sendSystemMessage(Component.literal("§a[Mundo de Tronos] Reapareciste en el spawn global."));
        }
    }

    public static boolean isSpectating(UUID uuid) {
        return activeSessions.containsKey(uuid);
    }
}
