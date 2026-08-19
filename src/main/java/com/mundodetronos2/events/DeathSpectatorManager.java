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

import java.util.UUID;

public class DeathSpectatorManager {

    public static void startSpectating(ServerPlayer player) {
        // En lugar de usar Modo Espectador que causa bugs de movimiento,
        // restablecemos la vida al máximo y teletransportamos al jugador AL INSTANTE
        // sobre su trono en modo SURVIVAL.
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
            if (tData != null) {
                targetPos = tData.getPos();
                try {
                    ResourceLocation dimRl = new ResourceLocation(tData.getDimension());
                    ServerLevel level = player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimRl));
                    if (level != null) {
                        targetLevel = level;
                    }
                } catch (Exception ignored) {}
            }
        }

        if (targetPos != null) {
            // Teletransportar 2 bloques arriba para aparecer encima del bloque de trono
            player.teleportTo(targetLevel, targetPos.getX() + 0.5D, targetPos.getY() + 2.0D, targetPos.getZ() + 0.5D, player.getYRot(), player.getXRot());
        } else {
            BlockPos spawnPos = targetLevel.getSharedSpawnPos();
            player.teleportTo(targetLevel, spawnPos.getX() + 0.5D, spawnPos.getY() + 2.0D, spawnPos.getZ() + 0.5D, player.getYRot(), player.getXRot());
        }

        player.sendSystemMessage(Component.literal("§a[Mundo de Tronos] ¡Has regresado a salvo a tu trono!"));
    }

    public static void tick(net.minecraft.server.MinecraftServer server) {
        // Método shim vacío ya que el cambio a survival e instant-tp se realiza de inmediato al morir.
    }

    public static boolean isSpectating(UUID uuid) {
        return false;
    }
}
