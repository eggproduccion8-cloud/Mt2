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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class CustomNPCEntity extends PathfinderMob {

    private static final EntityDataAccessor<String> NPC_MODEL = SynchedEntityData.defineId(CustomNPCEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> NPC_TEXTURE = SynchedEntityData.defineId(CustomNPCEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> IDLE_ANIMATION = SynchedEntityData.defineId(CustomNPCEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> WALK_ANIMATION = SynchedEntityData.defineId(CustomNPCEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> INTERACTION_ANIMATION = SynchedEntityData.defineId(CustomNPCEntity.class, EntityDataSerializers.STRING);

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
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(NPC_MODEL, "guard");
        this.entityData.define(NPC_TEXTURE, "guard");
        this.entityData.define(IDLE_ANIMATION, "idle");
        this.entityData.define(WALK_ANIMATION, "walk");
        this.entityData.define(INTERACTION_ANIMATION, "greet");
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

    public void playAnimation(String animName) {
        if (animName != null && !animName.isEmpty()) {
            this.setIdleAnimation(animName);
        }
    }

    public String getActualCurrentAnimation() {
        return getIdleAnimation();
    }

    @Override
    public net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        if (!player.level().isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            String name = this.getCustomName() != null ? this.getCustomName().getString() : "NPC";
            String type = getNpcModel().toLowerCase().trim();
            if (name.toLowerCase().contains("manuel")) type = "manuel";
            else if (name.toLowerCase().contains("ivan") || name.toLowerCase().contains("karla")) type = "ivan";

            if (sp.isCrouching() && sp.hasPermissions(2)) {
                com.mundodetronos2.network.NetworkManager.S2COpenNpcEditorPacket editorPkt =
                    new com.mundodetronos2.network.NetworkManager.S2COpenNpcEditorPacket(
                        this.getId(),
                        type,
                        name,
                        getNpcTexture(),
                        com.mundodetronos2.dialogue.NpcDialogueManager.getSerializedNpcDialogues(type)
                    );
                com.mundodetronos2.network.NetworkManager.sendToPlayer(editorPkt, sp);
                return net.minecraft.world.InteractionResult.sidedSuccess(player.level().isClientSide());
            }

            // Right click on Ivan opens Guild GUI directly!
            if ("ivan".equalsIgnoreCase(type) || name.toLowerCase().contains("ivan")) {
                com.mundodetronos2.realm.RealmData realm = com.mundodetronos2.realm.RealmManager.getPlayerRealm(sp.getUUID());
                com.mundodetronos2.throne.ThroneData throne = realm != null && realm.getThroneId() != null ? com.mundodetronos2.throne.ThroneManager.getThroneById(realm.getThroneId()) : null;
                java.util.List<com.mundodetronos2.realm.InviteData> invites = com.mundodetronos2.realm.RealmManager.getPlayerInvites(sp.getUUID());
                com.mundodetronos2.network.NetworkManager.sendToPlayer(new com.mundodetronos2.network.NetworkManager.S2COpenMainGuiPacket(realm, throne, invites), sp);
                playAnimation("greet");
                return net.minecraft.world.InteractionResult.sidedSuccess(player.level().isClientSide());
            }

            com.mundodetronos2.dialogue.DialogueNode initialNode = com.mundodetronos2.dialogue.NpcDialogueManager.getNode(type, "inicio");
            if (initialNode != null) {
                playAnimation("greet");
                java.util.List<String> optionTexts = new java.util.ArrayList<>();
                for (com.mundodetronos2.dialogue.DialogueOption opt : initialNode.getOptions()) {
                    optionTexts.add(opt.getText());
                }

                com.mundodetronos2.network.NetworkManager.S2COpenNpcDialoguePacket pkt =
                    new com.mundodetronos2.network.NetworkManager.S2COpenNpcDialoguePacket(
                        this.getId(),
                        type,
                        name,
                        initialNode.getText(),
                        initialNode.getId(),
                        getNpcTexture(),
                        optionTexts
                    );
                com.mundodetronos2.network.NetworkManager.sendToPlayer(pkt, sp);
                return net.minecraft.world.InteractionResult.sidedSuccess(player.level().isClientSide());
            }
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
