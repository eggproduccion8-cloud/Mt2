package com.mundodetronos2.role;

import com.mundodetronos2.network.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SkillTreeManager {

    private static final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    public static class SkillInfo {
        public final String name;
        public final String description;
        public final int cooldownSeconds;
        public final String symbol;

        public SkillInfo(String name, String description, int cooldownSeconds, String symbol) {
            this.name = name;
            this.description = description;
            this.cooldownSeconds = cooldownSeconds;
            this.symbol = symbol;
        }
    }

    private static final Map<PlayerRole, SkillInfo> roleSkills = new HashMap<>();

    static {
        roleSkills.put(PlayerRole.BERSERKER, new SkillInfo("Golpe de Furia", "Aumenta un 35% de fuerza por 8 segundos. ¡Ataque agresivo!", 45, "🩸"));
        roleSkills.put(PlayerRole.GUERRERO, new SkillInfo("Defensa Activa", "Otorga Resistencia III por 5 segundos. ¡Protección estable!", 30, "🛡"));
        roleSkills.put(PlayerRole.MAGO, new SkillInfo("Proyectil Mágico", "Lanza una descarga arcana explosiva a tus enemigos.", 12, "🧙"));
        roleSkills.put(PlayerRole.ARQUERO, new SkillInfo("Disparo Preciso", "Dispara una flecha de energía mágica a alta velocidad.", 15, "🏹"));
        roleSkills.put(PlayerRole.PALADIN, new SkillInfo("Escudo Protector", "Da Resistencia IV por 4s y cura a aliados en 8 bloques.", 45, "✨"));
        roleSkills.put(PlayerRole.DRACONICO, new SkillInfo("Aliento Dracónico", "Quema y lanza por los aires a los rivales frente a ti.", 35, "🔥"));
        roleSkills.put(PlayerRole.CLERIGO, new SkillInfo("Cantar de Sanación", "Regeneración IV grupal y sanación instantánea.", 30, "💚"));
    }

    public static SkillInfo getSkillFor(PlayerRole role) {
        return roleSkills.get(role);
    }

    public static int getRemainingCooldown(UUID playerId, String skillName) {
        Map<String, Long> playerMap = cooldowns.get(playerId);
        if (playerMap == null) return 0;
        Long endsAt = playerMap.get(skillName);
        if (endsAt == null) return 0;
        long remaining = endsAt - System.currentTimeMillis();
        return remaining <= 0 ? 0 : (int) (remaining / 1000L) + 1;
    }

    public static void setCooldown(UUID playerId, String skillName, int seconds) {
        cooldowns.computeIfAbsent(playerId, id -> new ConcurrentHashMap<>())
                .put(skillName, System.currentTimeMillis() + (seconds * 1000L));
    }

    public static boolean executeSkill(ServerPlayer player) {
        PlayerRoleData roleData = RoleManager.getPlayerRoleData(player.getUUID());
        if (!roleData.isHasRole() || roleData.getRole() == PlayerRole.NONE) {
            sendActionBarMessage(player, "§c¡No tienes ningún rol asignado para usar habilidades!");
            return false;
        }

        PlayerRole role = roleData.getRole();
        SkillInfo skill = getSkillFor(role);
        if (skill == null) return false;

        int remainingCd = getRemainingCooldown(player.getUUID(), skill.name);
        if (remainingCd > 0) {
            sendActionBarMessage(player, "§c¡Habilidad en enfriamiento! Faltan " + remainingCd + " segundos.");
            return false;
        }

        // Ejecutar efectos de la habilidad
        boolean success = applySkillEffects(player, role);
        if (success) {
            setCooldown(player.getUUID(), skill.name, skill.cooldownSeconds);
            sendActionBarMessage(player, "§a¡Has usado " + skill.name + " con éxito!");

            // Sincronizar HUD local del cliente
            NetworkManager.syncHud(player);
        }
        return success;
    }

    private static boolean applySkillEffects(ServerPlayer player, PlayerRole role) {
        ServerLevel level = player.serverLevel();
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        switch (role) {
            case BERSERKER:
                // Golpe de Furia: Fuerza II por 8 segundos
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 1));
                level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.2F, 1.2F);
                level.sendParticles(ParticleTypes.LAVA, px, py + 1.0D, pz, 25, 0.4D, 0.4D, 0.4D, 0.1D);
                return true;

            case GUERRERO:
                // Defensa Activa: Resistencia III por 5 segundos
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 2));
                level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 1.0F, 1.5F);
                level.sendParticles(ParticleTypes.CRIT, px, py + 1.0D, pz, 30, 0.4D, 0.4D, 0.4D, 0.1D);
                return true;

            case MAGO:
                // Proyectil Mágico: Lanza ráfaga explosiva sutil (partículas y pequeño empuje)
                level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.2F, 1.2F);
                // Lanzar un destello de partículas en frente del jugador
                double dx = player.getLookAngle().x;
                double dy = player.getLookAngle().y;
                double dz = player.getLookAngle().z;
                for (int i = 1; i <= 6; i++) {
                    level.sendParticles(ParticleTypes.DRAGON_BREATH, px + dx * i, py + 1.0D + dy * i, pz + dz * i, 5, 0.1D, 0.1D, 0.1D, 0.05D);
                }
                // Empujar enemigos cercanos en la dirección del proyectil
                AABB range = new AABB(px - 6, py - 3, pz - 6, px + 6, py + 3, pz + 6);
                for (ServerPlayer target : level.getEntitiesOfClass(ServerPlayer.class, range)) {
                    if (target != player) {
                        target.knockback(0.8F, -dx, -dz);
                        target.hurtMarked = true;
                    }
                }
                return true;

            case ARQUERO:
                // Disparo Preciso: Genera una flecha rápida delante del jugador
                level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.2F, 1.5F);
                Arrow arrow = new Arrow(level, player);
                arrow.shoot(player.getLookAngle().x, player.getLookAngle().y, player.getLookAngle().z, 3.0F, 0.0F);
                level.addFreshEntity(arrow);
                level.sendParticles(ParticleTypes.INSTANT_EFFECT, px, py + 1.0D, pz, 15, 0.3D, 0.3D, 0.3D, 0.1D);
                return true;

            case PALADIN:
                // Escudo Protector: Resistencia IV por 4s y sana aliados en un área de 8 bloques
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 3));
                level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.4F, 1.1F);
                level.sendParticles(ParticleTypes.END_ROD, px, py + 1.0D, pz, 40, 0.6D, 0.6D, 0.6D, 0.1D);

                AABB pRange = new AABB(px - 8, py - 4, pz - 8, px + 8, py + 4, pz + 8);
                for (ServerPlayer ally : level.getEntitiesOfClass(ServerPlayer.class, pRange)) {
                    // Dar absorción de escudo temporal a los aliados cercanos
                    ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, 0));
                    level.sendParticles(ParticleTypes.HEART, ally.getX(), ally.getY() + 1.0D, ally.getZ(), 5, 0.2D, 0.2D, 0.2D, 0.05D);
                }
                return true;

            case DRACONICO:
                // Aliento Dracónico: Lanza llamas e incendia sutilmente enemigos frente a ti
                level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.5F, 0.8F);
                double lookX = player.getLookAngle().x;
                double lookY = player.getLookAngle().y;
                double lookZ = player.getLookAngle().z;

                for (int i = 1; i <= 5; i++) {
                    level.sendParticles(ParticleTypes.FLAME, px + lookX * i, py + 1.0D + lookY * i, pz + lookZ * i, 12, 0.2D, 0.2D, 0.2D, 0.05D);
                }

                AABB dRange = new AABB(px - 5, py - 3, pz - 5, px + 5, py + 3, pz + 5);
                for (ServerPlayer target : level.getEntitiesOfClass(ServerPlayer.class, dRange)) {
                    if (target != player) {
                        target.setSecondsOnFire(4);
                        target.knockback(0.6F, -lookX, -lookZ);
                        target.hurtMarked = true;
                    }
                }
                return true;

            case CLERIGO:
                // Cantar de Sanación: Regeneración IV grupal por 5 segundos e instant health sutil
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 3));
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, 1.0F);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, px, py + 1.0D, pz, 35, 0.5D, 0.5D, 0.5D, 0.1D);

                AABB cRange = new AABB(px - 6, py - 3, pz - 6, px + 6, py + 3, pz + 6);
                for (ServerPlayer ally : level.getEntitiesOfClass(ServerPlayer.class, cRange)) {
                    ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
                    level.sendParticles(ParticleTypes.HEART, ally.getX(), ally.getY() + 1.0D, ally.getZ(), 4, 0.2D, 0.2D, 0.2D, 0.05D);
                }
                return true;
        }

        return false;
    }

    private static void sendActionBarMessage(ServerPlayer player, String message) {
        player.displayClientMessage(Component.literal(message), true);
    }
}
