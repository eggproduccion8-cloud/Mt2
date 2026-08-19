package com.mundodetronos2.throne;

import com.mundodetronos2.events.GameEventHandler;
import com.mundodetronos2.network.NetworkManager;
import com.mundodetronos2.realm.RealmData;
import com.mundodetronos2.realm.RealmManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ThroneAttackManager {

    public static class ChargeGroup {
        public final String dimension;
        public final BlockPos targetBlockPos;
        public final int requiredCharges;
        public int currentCharges = 0;
        public final List<BlockPos> chargeVisualPositions = new ArrayList<>();
        public UUID attackerId;
        public String attackerTeamName;
        public int remainingSeconds = 10;

        public ChargeGroup(String dimension, BlockPos targetBlockPos, int requiredCharges, UUID attackerId, String attackerTeamName) {
            this.dimension = dimension;
            this.targetBlockPos = targetBlockPos;
            this.requiredCharges = requiredCharges;
            this.attackerId = attackerId;
            this.attackerTeamName = attackerTeamName;
            this.remainingSeconds = 10;
        }
    }

    private static final Map<UUID, ThroneAttack> throneAttacks = new ConcurrentHashMap<>();
    private static final Map<String, ChargeGroup> blockChargeGroups = new ConcurrentHashMap<>();
    private static int tickCounter = 0;

    public static int getActiveAttacksSize() {
        return throneAttacks.size() + blockChargeGroups.size();
    }

    public static int getRequiredCharges(BlockState state) {
        String blockName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().toLowerCase();
        if (blockName.contains("wood") || blockName.contains("log") || blockName.contains("planks") || blockName.contains("fence") || blockName.contains("slab") || blockName.contains("stairs") || state.is(net.minecraft.tags.BlockTags.PLANKS)) {
            return 2; // Wood = 2
        } else if (blockName.contains("stone") || blockName.contains("cobble") || blockName.contains("andesite") || blockName.contains("granite") || blockName.contains("diorite") || blockName.contains("deepslate") || blockName.contains("brick") || blockName.contains("basalt") || blockName.contains("obsidian")) {
            return 3; // Stone / Obsidian = 3
        }
        return 1; // Dirt / Others = 1
    }

    private static String getTargetKey(String dimension, BlockPos pos) {
        return dimension + ":" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    public static void startAttack(UUID throneId, BlockPos chargePos, String dimension, UUID attackerId, String attackerTeamName) {
        ThroneAttack attack = new ThroneAttack(throneId, chargePos, dimension, attackerId, attackerTeamName);
        throneAttacks.put(throneId, attack);
        syncAttackAlert(attack, true);
    }

    public static ChargeGroup registerBlockCharge(String dimension, BlockPos targetBlockPos, BlockPos visualChargePos, BlockState targetState, UUID attackerId, String attackerTeamName) {
        String key = getTargetKey(dimension, targetBlockPos);
        int req = getRequiredCharges(targetState);

        ChargeGroup group = blockChargeGroups.computeIfAbsent(key, k -> new ChargeGroup(dimension, targetBlockPos, req, attackerId, attackerTeamName));
        group.currentCharges++;
        if (!group.chargeVisualPositions.contains(visualChargePos)) {
            group.chargeVisualPositions.add(visualChargePos);
        }
        group.attackerId = attackerId;
        group.attackerTeamName = attackerTeamName;
        return group;
    }

    public static boolean isUnderAttack(UUID throneId) {
        return throneAttacks.containsKey(throneId);
    }

    public static ThroneAttack getAttackByThrone(UUID throneId) {
        return throneAttacks.get(throneId);
    }

    public static ThroneAttack getAttackByChargePos(BlockPos pos) {
        for (ThroneAttack attack : throneAttacks.values()) {
            if (attack.getChargePos().equals(pos)) {
                return attack;
            }
        }
        return null;
    }

    public static ChargeGroup getChargeGroupByVisualPos(String dimension, BlockPos pos) {
        for (ChargeGroup group : blockChargeGroups.values()) {
            if (group.dimension.equalsIgnoreCase(dimension) && group.chargeVisualPositions.contains(pos)) {
                return group;
            }
        }
        return null;
    }

    public static void defuseAttack(UUID throneId, ServerPlayer defuser) {
        ThroneAttack attack = throneAttacks.remove(throneId);
        if (attack != null) {
            syncAttackAlert(attack, false);

            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                try {
                    ResourceLocation dimRl = new ResourceLocation(attack.getDimension());
                    ResourceKey<net.minecraft.world.level.Level> dimKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimRl);
                    ServerLevel level = server.getLevel(dimKey);
                    if (level != null) {
                        level.setBlockAndUpdate(attack.getChargePos(), Blocks.AIR.defaultBlockState());
                        level.playSound(null, attack.getChargePos(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.2F, 1.6F);
                    }
                } catch (Exception ignored) {}
            }

            if (defuser != null) {
                com.mundodetronos2.network.MessageManager.actionBar(defuser, "§a✔ ¡Carga de Trono desactivada con éxito!");
            }
        }
    }

    public static boolean defuseBlockCharge(String dimension, BlockPos visualChargePos, ServerPlayer defuser) {
        ChargeGroup group = getChargeGroupByVisualPos(dimension, visualChargePos);
        if (group == null) return false;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            try {
                ResourceLocation dimRl = new ResourceLocation(group.dimension);
                ResourceKey<net.minecraft.world.level.Level> dimKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimRl);
                ServerLevel level = server.getLevel(dimKey);
                if (level != null) {
                    level.setBlockAndUpdate(visualChargePos, Blocks.AIR.defaultBlockState());
                    level.playSound(null, visualChargePos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.2F, 1.6F);
                }
            } catch (Exception ignored) {}
        }

        group.chargeVisualPositions.remove(visualChargePos);
        group.currentCharges = Math.max(0, group.currentCharges - 1);

        if (group.currentCharges <= 0 || group.chargeVisualPositions.isEmpty()) {
            String key = getTargetKey(group.dimension, group.targetBlockPos);
            blockChargeGroups.remove(key);
        }

        if (defuser != null) {
            com.mundodetronos2.network.MessageManager.actionBar(defuser, "§a✔ ¡Carga desactivada! Cargas restantes en bloque: " + group.currentCharges + " / " + group.requiredCharges);
        }
        return true;
    }

    public static void tick() {
        if (throneAttacks.isEmpty() && blockChargeGroups.isEmpty()) {
            return;
        }

        tickCounter++;
        if (tickCounter % 20 != 0) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        // 1. Tick Throne Attacks
        for (ThroneAttack attack : throneAttacks.values()) {
            attack.tickSecond();

            if (attack.getRemainingSeconds() <= 0) {
                throneAttacks.remove(attack.getThroneId());
                syncAttackAlert(attack, false);

                try {
                    ResourceLocation dimRl = new ResourceLocation(attack.getDimension());
                    ResourceKey<net.minecraft.world.level.Level> dimKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimRl);
                    ServerLevel level = server.getLevel(dimKey);
                    if (level != null) {
                        BlockPos cPos = attack.getChargePos();
                        level.setBlockAndUpdate(cPos, Blocks.AIR.defaultBlockState());

                        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, cPos.getX() + 0.5D, cPos.getY() + 0.5D, cPos.getZ() + 0.5D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                        level.playSound(null, cPos, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 2.0F, 0.8F);

                        ThroneData throne = ThroneManager.getThroneById(attack.getThroneId());
                        if (throne != null) {
                            int currentHp = throne.getHealth();
                            if (currentHp > 0) {
                                currentHp--;
                                throne.setHealth(currentHp);
                                throne.setLastAttacker(attack.getAttackerId());

                                RealmData throneRealm = RealmManager.getRealm(throne.getRealmId());
                                String realmName = throneRealm != null ? throneRealm.getName() : "Desconocido";

                                ServerPlayer attackerPlayer = server.getPlayerList().getPlayer(attack.getAttackerId());
                                if (attackerPlayer != null) {
                                    NetworkManager.sendToPlayer(new NetworkManager.S2CSyncThroneDataPacket(realmName, currentHp, throne.getMaxHealth(), throne.getState().name()), attackerPlayer);
                                }

                                GameEventHandler.sendThroneSound(throne.getPos().getX(), throne.getPos().getY(), throne.getPos().getZ(), "hit", throne.getDimension());

                                if (currentHp <= 0) {
                                    GameEventHandler.reduceThroneLife(server, attack.getAttackerId(), attack.getAttackerTeamName(), throne, throne.getPos());
                                }
                                RealmManager.save(false);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                syncAttackAlert(attack, true);
            }
        }

        // 2. Tick Block Charge Groups (Wall Demolitions)
        Iterator<Map.Entry<String, ChargeGroup>> groupIter = blockChargeGroups.entrySet().iterator();
        while (groupIter.hasNext()) {
            Map.Entry<String, ChargeGroup> entry = groupIter.next();
            ChargeGroup group = entry.getValue();

            group.remainingSeconds--;

            if (group.remainingSeconds <= 0) {
                groupIter.remove();

                try {
                    ResourceLocation dimRl = new ResourceLocation(group.dimension);
                    ResourceKey<net.minecraft.world.level.Level> dimKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimRl);
                    ServerLevel level = server.getLevel(dimKey);
                    if (level != null) {
                        // Clear all visual charge blocks
                        for (BlockPos cPos : group.chargeVisualPositions) {
                            if (level.getBlockState(cPos).is(com.mundodetronos2.init.BlockInit.CARGA_ASALTO_BLOCK.get())) {
                                level.setBlockAndUpdate(cPos, Blocks.AIR.defaultBlockState());
                                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, cPos.getX() + 0.5D, cPos.getY() + 0.5D, cPos.getZ() + 0.5D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                            }
                        }
                        level.playSound(null, group.targetBlockPos, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 2.0F, 0.8F);

                        ServerPlayer player = server.getPlayerList().getPlayer(group.attackerId);

                        if (group.currentCharges >= group.requiredCharges) {
                            // Destroy target block cleanly
                            level.setBlockAndUpdate(group.targetBlockPos, Blocks.AIR.defaultBlockState());
                            level.playSound(null, group.targetBlockPos, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS, 1.5F, 0.8F);
                            if (player != null) {
                                com.mundodetronos2.network.MessageManager.actionBar(player, "§a✔ ¡Bloque destruido! (" + group.currentCharges + " / " + group.requiredCharges + " cargas detonadas)");
                            }
                        } else {
                            int left = group.requiredCharges - group.currentCharges;
                            if (player != null) {
                                com.mundodetronos2.network.MessageManager.actionBar(player, "§e✦ Bloque debilitado (" + group.currentCharges + " / " + group.requiredCharges + " cargas detonadas. Faltan " + left + " cargas).");
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void syncAttackAlert(ThroneAttack attack, boolean active) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        String baseName = "Bloque de Base";
        int hp = 1;
        int maxHp = 1;
        java.util.List<UUID> defendersList = new java.util.ArrayList<>();

        ThroneData throne = ThroneManager.getThroneById(attack.getThroneId());
        if (throne != null) {
            RealmData victimRealm = RealmManager.getRealm(throne.getRealmId());
            if (victimRealm != null) {
                baseName = victimRealm.getName();
                hp = throne.getHealth();
                maxHp = throne.getMaxHealth();
                defendersList = victimRealm.getMembers();
            }
        }

        NetworkManager.S2CThroneAttackAlertPacket pkt = new NetworkManager.S2CThroneAttackAlertPacket(
                active,
                baseName,
                hp,
                maxHp,
                attack.getAttackerTeamName(),
                attack.getRemainingSeconds()
        );

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean isDefender = defendersList.contains(player.getUUID());
            boolean isNear = player.level().dimension().location().toString().equals(attack.getDimension())
                             && player.distanceToSqr(attack.getChargePos().getX() + 0.5D, attack.getChargePos().getY() + 0.5D, attack.getChargePos().getZ() + 0.5D) < 4096.0D;

            if (isDefender || isNear) {
                NetworkManager.sendToPlayer(pkt, player);
            }
        }
    }

    public static void shutdown() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        // Safely remove all visual charge blocks on shutdown so no orphan charge blocks remain
        if (server != null) {
            for (ChargeGroup group : blockChargeGroups.values()) {
                try {
                    ResourceLocation dimRl = new ResourceLocation(group.dimension);
                    ServerLevel level = server.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimRl));
                    if (level != null) {
                        for (BlockPos pos : group.chargeVisualPositions) {
                            if (level.getBlockState(pos).is(com.mundodetronos2.init.BlockInit.CARGA_ASALTO_BLOCK.get())) {
                                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }

            for (ThroneAttack attack : throneAttacks.values()) {
                try {
                    ResourceLocation dimRl = new ResourceLocation(attack.getDimension());
                    ServerLevel level = server.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimRl));
                    if (level != null && level.getBlockState(attack.getChargePos()).is(com.mundodetronos2.init.BlockInit.CARGA_ASALTO_BLOCK.get())) {
                        level.setBlockAndUpdate(attack.getChargePos(), Blocks.AIR.defaultBlockState());
                    }
                } catch (Exception ignored) {}
                syncAttackAlert(attack, false);
            }
        }

        throneAttacks.clear();
        blockChargeGroups.clear();
    }
}
