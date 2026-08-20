package com.mundodetronos2.entity;

import com.mundodetronos2.player.PlayerRealmData;
import com.mundodetronos2.realm.RealmData;
import com.mundodetronos2.realm.RealmManager;
import com.mundodetronos2.throne.DefenderManager;
import com.mundodetronos2.throne.ThroneData;
import com.mundodetronos2.throne.ThroneManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class SoldierEntity extends CustomNPCEntity {

    private static final EntityDataAccessor<String> TEAM_ID = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> THRONE_ID = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<BlockPos> HOME_POS = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Integer> ATTACK_TICKS = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.INT);

    public SoldierEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNpcModel("guard");
        this.setNpcTexture("guardred");
        this.setIdleAnimation("idle");
        this.setWalkAnimation("walk");
        this.setInteractionAnimation("attack");
    }

    public static AttributeSupplier.Builder createSoldierAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // 1. Melee Attack Goal
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25D, true));

        // 2. Follow Team Leader Goal (Before Throne is placed)
        this.goalSelector.addGoal(2, new Goal() {
            private Player leaderPlayer;

            @Override
            public boolean canUse() {
                if (getHomePos() != null && !getHomePos().equals(BlockPos.ZERO)) {
                    return false; // Throne already placed -> Defend Throne
                }
                RealmData myRealm = RealmManager.getRealmByColorKey(getTeamId());
                if (myRealm != null && myRealm.getOwnerId() != null) {
                    if (SoldierEntity.this.level() instanceof net.minecraft.server.level.ServerLevel sLevel) {
                        Player p = sLevel.getServer().getPlayerList().getPlayer(myRealm.getOwnerId());
                        if (p != null && p.level() == SoldierEntity.this.level()) {
                            this.leaderPlayer = p;
                            return SoldierEntity.this.distanceToSqr(p) > 16.0D; // Follow if > 4 blocks away
                        }
                    }
                }
                return false;
            }

            @Override
            public boolean canContinueToUse() {
                return leaderPlayer != null && leaderPlayer.isAlive() && SoldierEntity.this.distanceToSqr(leaderPlayer) > 9.0D;
            }

            @Override
            public void start() {
                if (leaderPlayer != null) {
                    SoldierEntity.this.getNavigation().moveTo(leaderPlayer, 1.15D);
                }
            }

            @Override
            public void tick() {
                if (leaderPlayer != null && SoldierEntity.this.tickCount % 10 == 0) {
                    SoldierEntity.this.getNavigation().moveTo(leaderPlayer, 1.15D);
                }
            }

            @Override
            public void stop() {
                this.leaderPlayer = null;
                SoldierEntity.this.getNavigation().stop();
            }
        });

        // 3. Return to Throne Defense zone (> 75 blocks)
        this.goalSelector.addGoal(3, new Goal() {
            @Override
            public boolean canUse() {
                BlockPos home = getHomePos();
                if (home == null || home.equals(BlockPos.ZERO)) return false;
                return SoldierEntity.this.distanceToSqr(home.getX() + 0.5D, home.getY() + 1.0D, home.getZ() + 0.5D) > 5625.0D; // 75^2
            }

            @Override
            public void start() {
                BlockPos home = getHomePos();
                if (home != null && !home.equals(BlockPos.ZERO)) {
                    SoldierEntity.this.setTarget(null);
                    SoldierEntity.this.getNavigation().moveTo(home.getX() + 0.5D, home.getY() + 1.0D, home.getZ() + 0.5D, 1.2D);
                }
            }
        });

        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        // Target enemy players
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                (target) -> {
                    if (!(target instanceof Player player)) return false;
                    if (player.isCreative() || player.isSpectator()) return false;

                    // Check team affiliation
                    PlayerRealmData prd = RealmManager.getPlayerRealmData(player.getUUID());
                    if (prd != null) {
                        RealmData myRealm = RealmManager.getRealmByColorKey(getTeamId());
                        if (myRealm != null && prd.getRealmId().equals(myRealm.getId())) {
                            return false; // Same team member or leader!
                        }
                    }
                    return true; // Enemy player
                }));

        // Target hostile mobs
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                (target) -> true));

        // Target enemy soldiers
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, SoldierEntity.class, 10, true, false,
                (target) -> {
                    if (target instanceof SoldierEntity other) {
                        return !other.getTeamId().equalsIgnoreCase(this.getTeamId());
                    }
                    return false;
                }));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TEAM_ID, "rojo");
        this.entityData.define(THRONE_ID, "");
        this.entityData.define(HOME_POS, BlockPos.ZERO);
        this.entityData.define(ATTACK_TICKS, 0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        int ticks = this.entityData.get(ATTACK_TICKS);
        if (ticks > 0) {
            this.entityData.set(ATTACK_TICKS, ticks - 1);
        }
    }

    public String getTeamId() {
        return this.entityData.get(TEAM_ID);
    }

    public void setTeamId(String teamId) {
        String sanitized = teamId != null ? teamId.toLowerCase().trim() : "rojo";
        this.entityData.set(TEAM_ID, sanitized);

        // Map team colors to guard texture names
        String texName = switch (sanitized) {
            case "rojo", "red" -> "guardred";
            case "azul", "blue", "cian", "cyan" -> "guardcyan";
            case "verde", "green" -> "guardgreen";
            case "amarillo", "yellow" -> "guardyellow";
            case "morado", "purple" -> "guardpurple";
            case "naranja", "orange" -> "guardorange";
            case "rosa", "pink" -> "guardpink";
            default -> "guard";
        };

        this.setNpcTexture(texName);
    }

    public String getThroneIdStr() {
        return this.entityData.get(THRONE_ID);
    }

    public void setThroneIdStr(String idStr) {
        this.entityData.set(THRONE_ID, idStr != null ? idStr : "");
    }

    public BlockPos getHomePos() {
        return this.entityData.get(HOME_POS);
    }

    public void setHomePos(BlockPos pos) {
        this.entityData.set(HOME_POS, pos != null ? pos : BlockPos.ZERO);
    }

    @Override
    public void setTarget(@org.jetbrains.annotations.Nullable LivingEntity target) {
        if (target instanceof Player player) {
            PlayerRealmData prd = RealmManager.getPlayerRealmData(player.getUUID());
            if (prd != null) {
                RealmData myRealm = RealmManager.getRealmByColorKey(getTeamId());
                if (myRealm != null && prd.getRealmId().equals(myRealm.getId())) {
                    return;
                }
            }
        } else if (target instanceof SoldierEntity other) {
            if (other.getTeamId().equalsIgnoreCase(this.getTeamId())) {
                return;
            }
        }
        super.setTarget(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && source.getEntity() instanceof Player player) {
            PlayerRealmData prd = RealmManager.getPlayerRealmData(player.getUUID());
            if (prd != null) {
                RealmData myRealm = RealmManager.getRealmByColorKey(getTeamId());
                if (myRealm != null && prd.getRealmId().equals(myRealm.getId())) {
                    if (this.getTarget() == player) {
                        this.setTarget(null);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt) {
            this.entityData.set(ATTACK_TICKS, 20);
            this.swing(InteractionHand.MAIN_HAND, true);
        }
        return hurt;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return false; // Defenders can be damaged and killed
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public String getActualCurrentAnimation() {
        if (this.entityData.get(ATTACK_TICKS) > 0 || this.swingTime > 0) {
            return "attack";
        }
        if (this.getDeltaMovement().horizontalDistanceSqr() > 0.001) {
            return "walk";
        }
        return "idle";
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.level().isClientSide() && !getThroneIdStr().isEmpty()) {
            try {
                UUID tId = UUID.fromString(getThroneIdStr());
                DefenderManager.recordDefenderDeath(tId);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("TeamId", getTeamId());
        tag.putString("ThroneId", getThroneIdStr());
        BlockPos home = getHomePos();
        if (home != null) {
            tag.putInt("HomeX", home.getX());
            tag.putInt("HomeY", home.getY());
            tag.putInt("HomeZ", home.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("TeamId")) setTeamId(tag.getString("TeamId"));
        if (tag.contains("ThroneId")) setThroneIdStr(tag.getString("ThroneId"));
        if (tag.contains("HomeX")) {
            setHomePos(new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ")));
        }
    }
}
