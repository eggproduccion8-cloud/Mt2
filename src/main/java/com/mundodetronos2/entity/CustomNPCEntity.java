package com.mundodetronos2.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class CustomNPCEntity extends PathfinderMob {

    private static final EntityDataAccessor<String> NPC_MODEL = SynchedEntityData.defineId(CustomNPCEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> NPC_TEXTURE = SynchedEntityData.defineId(CustomNPCEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> IDLE_ANIMATION = SynchedEntityData.defineId(CustomNPCEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> WALK_ANIMATION = SynchedEntityData.defineId(CustomNPCEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> INTERACTION_ANIMATION = SynchedEntityData.defineId(CustomNPCEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> TEMP_ANIMATION = SynchedEntityData.defineId(CustomNPCEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> TEMP_ANIMATION_END_TICK = SynchedEntityData.defineId(CustomNPCEntity.class, EntityDataSerializers.INT);

    public CustomNPCEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(NPC_MODEL, "guard");
        this.entityData.define(NPC_TEXTURE, "guard");
        this.entityData.define(IDLE_ANIMATION, "idle");
        this.entityData.define(WALK_ANIMATION, "walk");
        this.entityData.define(INTERACTION_ANIMATION, "greet");
        this.entityData.define(TEMP_ANIMATION, "");
        this.entityData.define(TEMP_ANIMATION_END_TICK, 0);
    }

    public String getNpcModel() {
        return this.entityData.get(NPC_MODEL);
    }

    public void setNpcModel(String model) {
        this.entityData.set(NPC_MODEL, model != null ? model : "guard");
    }

    public String getNpcTexture() {
        return this.entityData.get(NPC_TEXTURE);
    }

    public void setNpcTexture(String texture) {
        this.entityData.set(NPC_TEXTURE, texture != null ? texture : "guard");
    }

    public String getIdleAnimation() {
        return this.entityData.get(IDLE_ANIMATION);
    }

    public void setIdleAnimation(String anim) {
        this.entityData.set(IDLE_ANIMATION, anim != null ? anim : "idle");
    }

    public String getWalkAnimation() {
        return this.entityData.get(WALK_ANIMATION);
    }

    public void setWalkAnimation(String anim) {
        this.entityData.set(WALK_ANIMATION, anim != null ? anim : "walk");
    }

    public String getInteractionAnimation() {
        return this.entityData.get(INTERACTION_ANIMATION);
    }

    public void setInteractionAnimation(String anim) {
        this.entityData.set(INTERACTION_ANIMATION, anim != null ? anim : "greet");
    }

    public String getTempAnimation() {
        return this.entityData.get(TEMP_ANIMATION);
    }

    public void setTempAnimation(String anim) {
        this.entityData.set(TEMP_ANIMATION, anim != null ? anim : "");
    }

    public int getTempAnimationEndTick() {
        return this.entityData.get(TEMP_ANIMATION_END_TICK);
    }

    public void setTempAnimationEndTick(int tick) {
        this.entityData.set(TEMP_ANIMATION_END_TICK, tick);
    }

    public void triggerTempAnimation(String animName, int durationTicks) {
        if (animName != null && !animName.isEmpty()) {
            setTempAnimation(animName);
            setTempAnimationEndTick(this.tickCount + Math.max(1, durationTicks));
        }
    }

    public void playAnimation(String animName) {
        if (animName != null && !animName.isEmpty()) {
            triggerTempAnimation(animName, 20);
        }
    }

    public void resetTempAnimation() {
        setTempAnimation("");
        setTempAnimationEndTick(0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!getTempAnimation().isEmpty() && this.tickCount >= getTempAnimationEndTick()) {
            resetTempAnimation();
        }
    }

    public String getActualCurrentAnimation() {
        String temp = getTempAnimation();
        if (temp != null && !temp.isEmpty()) {
            return temp;
        }

        double speedSqr = this.getDeltaMovement().x * this.getDeltaMovement().x + this.getDeltaMovement().z * this.getDeltaMovement().z;
        boolean isMoving = speedSqr > 0.0004D || (this.walkAnimation != null && this.walkAnimation.isMoving());
        if (isMoving) {
            return getWalkAnimation();
        }
        return getIdleAnimation();
    }

    @Override
    public net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        if (!player.level().isClientSide()) {
            triggerTempAnimation(getInteractionAnimation(), 40); // 2 segundos (40 ticks)
            return net.minecraft.world.InteractionResult.sidedSuccess(player.level().isClientSide());
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true; // Normal NPCs are invulnerable
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean canCollideWith(net.minecraft.world.entity.Entity entity) {
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("NpcModel", getNpcModel());
        tag.putString("NpcTexture", getNpcTexture());
        tag.putString("IdleAnim", getIdleAnimation());
        tag.putString("WalkAnim", getWalkAnimation());
        tag.putString("InteractionAnim", getInteractionAnimation());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("NpcModel")) setNpcModel(tag.getString("NpcModel"));
        if (tag.contains("NpcTexture")) setNpcTexture(tag.getString("NpcTexture"));
        if (tag.contains("IdleAnim")) setIdleAnimation(tag.getString("IdleAnim"));
        if (tag.contains("WalkAnim")) setWalkAnimation(tag.getString("WalkAnim"));
        if (tag.contains("InteractionAnim")) setInteractionAnimation(tag.getString("InteractionAnim"));
    }
}
