package com.mundodetronos2.entity;

import com.mundodetronos2.npc.BattleManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class SoldierEntity extends CustomNPCEntity {

    private static final EntityDataAccessor<String> TEAM_ID = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.STRING);

    public SoldierEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNpcModel("guard");
        this.setNpcTexture("guardred");
        this.setIdleAnimation("idle");
        this.setWalkAnimation("walk");
        this.setInteractionAnimation("grabsword");
    }

    public static AttributeSupplier.Builder createSoldierAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // Attack Goal: Active only when battle is active
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25D, true) {
            @Override
            public boolean canUse() {
                return BattleManager.isBattleActive() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return BattleManager.isBattleActive() && super.canContinueToUse();
            }
        });

        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8D) {
            @Override
            public boolean canUse() {
                return BattleManager.isBattleActive() && super.canUse();
            }
        });

        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        // Target enemy soldiers from different teams when battle is active
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, SoldierEntity.class, 10, true, false,
                (target) -> {
                    if (!BattleManager.isBattleActive()) return false;
                    if (target instanceof SoldierEntity otherSoldier) {
                        return !otherSoldier.getTeamId().equalsIgnoreCase(this.getTeamId());
                    }
                    return false;
                }));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TEAM_ID, "red");
    }

    public String getTeamId() {
        return this.entityData.get(TEAM_ID);
    }

    public void setTeamId(String teamId) {
        String sanitized = teamId != null ? teamId.toLowerCase() : "red";
        this.entityData.set(TEAM_ID, sanitized);

        // Auto map color texture variant if applicable
        String[] colors = {"cyan", "green", "orange", "pink", "purple", "red", "yellow"};
        boolean isColor = false;
        for (String c : colors) {
            if (c.equals(sanitized)) {
                this.setNpcTexture("guard" + c);
                isColor = true;
                break;
            }
        }
        if (!isColor) {
            this.setNpcTexture("guard");
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return false; // Soldiers can take damage and die
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public String getActualCurrentAnimation() {
        if (this.swingTime > 0) {
            return "attack";
        }
        return super.getActualCurrentAnimation();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("TeamId", getTeamId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("TeamId")) setTeamId(tag.getString("TeamId"));
    }
}
