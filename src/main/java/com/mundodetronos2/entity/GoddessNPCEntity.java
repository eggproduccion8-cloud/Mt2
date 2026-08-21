package com.mundodetronos2.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class GoddessNPCEntity extends PathfinderMob {

    private static final EntityDataAccessor<String> SKIN_NAME = SynchedEntityData.defineId(GoddessNPCEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> NPC_TYPE = SynchedEntityData.defineId(GoddessNPCEntity.class, EntityDataSerializers.STRING);

    private final List<String> customDialogues = new ArrayList<>();
    private net.minecraft.core.BlockPos homePos;

    public GoddessNPCEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));
        this.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 0.5D) {
            @Override
            public boolean canUse() {
                if (GoddessNPCEntity.this.homePos != null && GoddessNPCEntity.this.distanceToSqr(GoddessNPCEntity.this.homePos.getX() + 0.5D, GoddessNPCEntity.this.homePos.getY(), GoddessNPCEntity.this.homePos.getZ() + 0.5D) > 16.0D) {
                    return false;
                }
                return super.canUse();
            }
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            // Inicializar homePos con la posición real tras spawn
            if (this.homePos == null || (this.homePos.getX() == 0 && this.homePos.getY() == 0 && this.homePos.getZ() == 0)) {
                if (this.getX() != 0.0D || this.getY() != 0.0D || this.getZ() != 0.0D) {
                    this.homePos = this.blockPosition();
                }
            }

            if (this.homePos != null && this.tickCount % 20 == 0) {
                double distSq = this.distanceToSqr(this.homePos.getX() + 0.5D, this.homePos.getY(), this.homePos.getZ() + 0.5D);
                if (distSq > 64.0D) { // Teletransportar solo si se aleja más de 8 bloques
                    this.teleportTo(this.homePos.getX() + 0.5D, this.homePos.getY(), this.homePos.getZ() + 0.5D);
                    this.setDeltaMovement(0, 0, 0);
                } else if (distSq > 16.0D) { // Retornar caminando hacia homePos
                    this.getNavigation().moveTo(this.homePos.getX() + 0.5D, this.homePos.getY(), this.homePos.getZ() + 0.5D, 0.25D);
                }

                Player nearestPlayer = this.level().getNearestPlayer(this, 6.0D);
                if (nearestPlayer != null) {
                    this.getLookControl().setLookAt(nearestPlayer.getX(), nearestPlayer.getEyeY(), nearestPlayer.getZ(), 30.0F, 30.0F);
                }
            }
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SKIN_NAME, "Wyldune");
        this.entityData.define(NPC_TYPE, "diosa");
    }

    public String getSkinName() {
        return this.entityData.get(SKIN_NAME);
    }

    public void setSkinName(String skinName) {
        this.entityData.set(SKIN_NAME, skinName);
    }

    public String getNpcType() {
        return this.entityData.get(NPC_TYPE);
    }

    public void setNpcType(String type) {
        this.entityData.set(NPC_TYPE, type);
    }

    public List<String> getCustomDialogues() {
        return customDialogues;
    }

    public void clearDialogues() {
        customDialogues.clear();
    }

    public void addDialogue(String line) {
        customDialogues.add(line);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("SkinName", getSkinName());
        tag.putString("NpcType", getNpcType());

        ListTag list = new ListTag();
        for (String line : customDialogues) {
            list.add(StringTag.valueOf(line));
        }
        tag.put("Dialogues", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SkinName")) {
            setSkinName(tag.getString("SkinName"));
        }
        if (tag.contains("NpcType")) {
            setNpcType(tag.getString("NpcType"));
        }

        customDialogues.clear();
        if (tag.contains("Dialogues")) {
            ListTag list = tag.getList("Dialogues", 8);
            for (int i = 0; i < list.size(); i++) {
                customDialogues.add(list.getString(i));
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!player.level().isClientSide && player instanceof ServerPlayer sp) {
            boolean hasRole = com.mundodetronos2.role.RoleManager.getPlayerRoleData(sp.getUUID()).isHasRole();
            com.mundodetronos2.network.NetworkManager.sendToPlayer(new com.mundodetronos2.network.NetworkManager.S2COpenAltarOptionPacket(hasRole), sp);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D);
    }
}
