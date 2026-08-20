package com.mundodetronos2.throne;

import com.mundodetronos2.entity.SoldierEntity;
import com.mundodetronos2.init.EntityInit;
import com.mundodetronos2.realm.RealmData;
import com.mundodetronos2.realm.RealmManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DefenderManager {

    private static final Map<UUID, Queue<Long>> pendingRespawns = new ConcurrentHashMap<>();

    public static int getMaxDefendersForLevel(int level) {
        return switch (level) {
            case 2 -> 5;
            case 3 -> 7;
            default -> 3;
        };
    }

    public static void recordDefenderDeath(UUID throneId) {
        if (throneId == null) return;
        pendingRespawns.computeIfAbsent(throneId, k -> new LinkedList<>()).add(System.currentTimeMillis() + 10000L);
    }

    public static void spawnInitialDefenders(ServerLevel level, ThroneData throne) {
        if (level == null || throne == null || throne.getPos() == null) return;
        syncDefendersForThrone(level, throne);
    }

    public static void tick(MinecraftServer server) {
        if (server == null) return;

        for (ThroneData throne : ThroneManager.getThronesMap().values()) {
            ResourceLocation dimRl = new ResourceLocation(throne.getDimension());
            ResourceKey<net.minecraft.world.level.Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimRl);
            ServerLevel level = server.getLevel(dimKey);
            if (level != null) {
                syncDefendersForThrone(level, throne);
            }
        }
    }

    public static void syncDefendersForThrone(ServerLevel level, ThroneData throne) {
        if (level == null || throne == null || throne.getPos() == null) return;

        BlockPos pos = throne.getPos();
        String throneIdStr = throne.getId().toString();
        RealmData realm = RealmManager.getRealm(throne.getRealmId());
        String colorKey = realm != null ? realm.getColor() : "rojo";

        // Find existing defenders in 150x150 area
        AABB area = new AABB(pos).inflate(100);
        List<SoldierEntity> existing = level.getEntitiesOfClass(SoldierEntity.class, area,
                s -> throneIdStr.equals(s.getThroneIdStr()));

        // If REPAIRING, remove existing defenders
        if (throne.getState() == ThroneState.REPAIRING) {
            for (SoldierEntity s : existing) {
                s.discard();
            }
            return;
        }

        int maxAllowed = getMaxDefendersForLevel(throne.getThroneLevel());

        // Remove excess defenders if level downgraded
        while (existing.size() > maxAllowed) {
            SoldierEntity excess = existing.remove(existing.size() - 1);
            excess.discard();
        }

        // Handle pending 10-second respawns
        Queue<Long> queue = pendingRespawns.get(throne.getId());
        long now = System.currentTimeMillis();

        if (existing.size() < maxAllowed) {
            boolean shouldSpawn = false;
            if (queue != null && !queue.isEmpty()) {
                if (now >= queue.peek()) {
                    queue.poll(); // Consume expired respawn
                    shouldSpawn = true;
                }
            } else {
                // Initial spawn or missing defenders
                shouldSpawn = true;
            }

            if (shouldSpawn) {
                spawnDefender(level, throne, colorKey);
            }
        }
    }

    private static void spawnDefender(ServerLevel level, ThroneData throne, String colorKey) {
        SoldierEntity soldier = EntityInit.SOLDIER.get().create(level);
        if (soldier == null) return;

        BlockPos center = throne.getPos();
        net.minecraft.util.RandomSource rand = level.getRandom();
        double offsetX = (rand.nextDouble() - 0.5D) * 6.0D;
        double offsetZ = (rand.nextDouble() - 0.5D) * 6.0D;

        BlockPos spawnPos = center.offset((int) offsetX, 1, (int) offsetZ);
        soldier.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, rand.nextFloat() * 360.0F, 0.0F);

        soldier.setTeamId(colorKey);
        soldier.setThroneIdStr(throne.getId().toString());
        soldier.setHomePos(center);

        RealmData realm = RealmManager.getRealm(throne.getRealmId());
        String teamName = realm != null ? realm.getName() : colorKey.toUpperCase();
        soldier.setCustomName(net.minecraft.network.chat.Component.literal("§cDefensor (" + teamName + ")"));
        soldier.setCustomNameVisible(true);

        level.addFreshEntity(soldier);
    }
}
