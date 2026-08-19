package com.mundodetronos2.throne;

import com.mundodetronos2.config.ConfigManager;
import com.mundodetronos2.data.SaveManager;
import com.mundodetronos2.realm.RealmData;
import com.mundodetronos2.realm.RealmManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ThroneManager {
    private static final Logger LOGGER = LogManager.getLogger();

    // Posición del bloque en el mundo -> Datos del Trono
    private static final Map<BlockPos, ThroneData> thronesByPos = new ConcurrentHashMap<>();
    // ID del trono -> Datos del Trono
    private static final Map<UUID, ThroneData> thronesById = new ConcurrentHashMap<>();

    private static boolean globalEventActive = false;

    // Listas de efectos rotatorios
    private static final List<MobEffect> BUFFS = Arrays.asList(
            MobEffects.REGENERATION,
            MobEffects.DAMAGE_BOOST,
            MobEffects.DAMAGE_RESISTANCE,
            MobEffects.MOVEMENT_SPEED,
            MobEffects.DIG_SPEED
    );

    private static final List<MobEffect> DEBUFFS = Arrays.asList(
            MobEffects.WEAKNESS,
            MobEffects.MOVEMENT_SLOWDOWN,
            MobEffects.DIG_SLOWDOWN,
            MobEffects.BLINDNESS,
            MobEffects.POISON
    );

    public static void init() {
        thronesByPos.clear();
        thronesById.clear();

        Map<UUID, ThroneData> loaded = SaveManager.loadThrones();
        for (ThroneData t : loaded.values()) {
            thronesById.put(t.getId(), t);
            if (t.getPos() != null) {
                thronesByPos.put(t.getPos(), t);
            }
        }

        LOGGER.info("ThroneManager cargado: {} tronos registrados.", thronesById.size());
    }

    public static Map<UUID, ThroneData> getThronesMap() {
        return thronesById;
    }

    public static boolean isGlobalEventActive() {
        return globalEventActive;
    }

    public static void setGlobalEventActive(boolean active) {
        globalEventActive = active;
        for (ThroneData t : thronesById.values()) {
            t.setEventEnabled(active);
            if (active) {
                if (t.getState() == ThroneState.PROTECTED) {
                    t.setState(ThroneState.ACTIVE);
                }
            } else {
                if (t.getState() == ThroneState.ACTIVE) {
                    t.setState(ThroneState.PROTECTED);
                }
            }
        }
        SaveManager.markDirty();
        RealmManager.save(false);
    }

    public static ThroneData registerThrone(UUID realmId, BlockPos pos, String dimension, int maxHealth) {
        RealmData realm = RealmManager.getRealm(realmId);
        if (realm == null) return null;

        // Si ya tiene trono, eliminar el viejo primero
        if (realm.getThroneId() != null) {
            removeThroneByRealm(realmId);
        }

        // Si ya hay un trono en esa posición exacta, no permitir
        if (pos != null && thronesByPos.containsKey(pos)) {
            return null;
        }

        UUID throneId = UUID.randomUUID();
        ThroneData throne = new ThroneData(throneId, realmId, pos, dimension, maxHealth);
        throne.setEventEnabled(globalEventActive);
        throne.setState(globalEventActive ? ThroneState.ACTIVE : ThroneState.PROTECTED);

        thronesById.put(throneId, throne);
        if (pos != null) {
            thronesByPos.put(pos, throne);
        }

        realm.setThroneId(throneId);

        SaveManager.markDirty();
        RealmManager.save(false);

        LOGGER.info("Trono registrado para el reino '{}' en {} [{}]", realm.getName(), pos, dimension);
        return throne;
    }

    public static boolean removeThroneByRealm(UUID realmId) {
        RealmData realm = RealmManager.getRealm(realmId);
        if (realm == null) return false;

        UUID throneId = realm.getThroneId();
        if (throneId == null) return false;

        ThroneData throne = thronesById.get(throneId);
        if (throne != null) {
            if (throne.getPos() != null) {
                thronesByPos.remove(throne.getPos());
            }
            thronesById.remove(throneId);
        }

        realm.setThroneId(null);
        SaveManager.markDirty();
        RealmManager.save(false);
        return true;
    }

    public static ThroneData getThroneAt(BlockPos pos) {
        if (pos == null) return null;
        ThroneData throne = thronesByPos.get(pos);
        if (throne != null) {
            // Verificar cooldown de reparación de forma perezosa (lazy check)
            checkCooldownExpired(throne);
        }
        return throne;
    }

    public static ThroneData getThroneById(UUID id) {
        if (id == null) return null;
        ThroneData throne = thronesById.get(id);
        if (throne != null) {
            checkCooldownExpired(throne);
        }
        return throne;
    }

    public static void checkCooldownExpired(ThroneData throne) {
        if (throne.getState() == ThroneState.REPAIRING) {
            long now = System.currentTimeMillis();
            if (now >= throne.getCooldownEndsAt()) {
                throne.setHealth(throne.getMaxHealth());
                throne.setState(globalEventActive ? ThroneState.ACTIVE : ThroneState.PROTECTED);
                SaveManager.markDirty();
                RealmManager.save(false);
                LOGGER.info("Trono del reino con ID {} finalizó su reparación y vuelve a la normalidad.", throne.getRealmId());
            }
        }
    }

    /**
     * Comprueba periódicamente todos los tronos en cooldown para repararlos si ha pasado el tiempo.
     */
    public static void tick() {
        boolean changed = false;
        long now = System.currentTimeMillis();

        for (ThroneData t : thronesById.values()) {
            // Verificar reparación expirada
            if (t.getState() == ThroneState.REPAIRING && now >= t.getCooldownEndsAt()) {
                t.setHealth(t.getMaxHealth());
                t.setState(globalEventActive ? ThroneState.ACTIVE : ThroneState.PROTECTED);
                changed = true;
                LOGGER.info("Trono del reino con ID {} finalizó su reparación vía tick.", t.getRealmId());
            }
        }
        if (changed) {
            SaveManager.markDirty();
            RealmManager.save(false);
        }
    }
}
