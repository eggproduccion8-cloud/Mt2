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
            String name = this.getCustomName() != null ? this.getCustomName().getString() : "NPC";
            String type = getNpcType().toLowerCase().trim();

            // SHIFT + CLICK DERECHO (Solo para OP) -> Abre el editor profesional del NPC
            if (sp.isCrouching() && sp.hasPermissions(2)) {
                com.mundodetronos2.npc.NpcRegistryManager.NpcConfig cfg = com.mundodetronos2.npc.NpcRegistryManager.getNpcConfig(this.getUUID());
                String cfgJson = cfg != null ? new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(cfg) : com.mundodetronos2.dialogue.NpcDialogueManager.getSerializedNpcDialogues(type);
                com.mundodetronos2.network.NetworkManager.S2COpenNpcEditorPacket editorPkt =
                    new com.mundodetronos2.network.NetworkManager.S2COpenNpcEditorPacket(
                        this.getId(),
                        type,
                        name,
                        this.getSkinName(),
                        cfgJson
                    );
                com.mundodetronos2.network.NetworkManager.sendToPlayer(editorPkt, sp);
                return InteractionResult.sidedSuccess(player.level().isClientSide);
            }

            String initialNodeId = "inicio";
            com.mundodetronos2.realm.RealmData realm = com.mundodetronos2.realm.RealmManager.getPlayerRealm(sp.getUUID());
            com.mundodetronos2.tutorial.TutorialManager.RealmTutorialData tut = realm != null ? com.mundodetronos2.tutorial.TutorialManager.getTutorialData(realm.getId()) : null;

            // Determinar nodo contextual según el estado del jugador/equipo frente al NPC
            if (type.equalsIgnoreCase("manuel")) {
                if (tut != null && tut.tutorialLevel >= 2) {
                    initialNodeId = "m_post_completion";
                } else if (realm != null) {
                    initialNodeId = "m_grupo";
                } else {
                    initialNodeId = "inicio";
                }
            } else if (type.equalsIgnoreCase("laura")) {
                if (tut == null || tut.tutorialLevel < 2) {
                    initialNodeId = "l_locked";
                } else if (tut.tutorialLevel > 2) {
                    initialNodeId = "l_post_completion";
                } else if (tut.wheatCount >= 150) {
                    initialNodeId = "l_ready";
                } else if (tut.wheatCount > 0) {
                    initialNodeId = "l_in_progress";
                }
            } else if (type.equalsIgnoreCase("oscar")) {
                if (tut == null || tut.tutorialLevel < 3) {
                    initialNodeId = "o_locked";
                } else if (tut.tutorialLevel > 3 || (tut.oscarArmorClaimedMembers != null && tut.oscarArmorClaimedMembers.contains(sp.getUUID()))) {
                    initialNodeId = "o_post_completion";
                }
            } else if (type.equalsIgnoreCase("samuel")) {
                if (tut == null || tut.tutorialLevel < 4) {
                    initialNodeId = "s_locked";
                } else if (tut.tutorialLevel > 4) {
                    initialNodeId = "s_post_completion";
                }
            } else if (type.equalsIgnoreCase("heraldo")) {
                if (tut == null || tut.tutorialLevel < 5) {
                    initialNodeId = "h_locked";
                } else if (tut.tutorialLevel > 5) {
                    initialNodeId = "h_post_completion";
                }
            } else if (type.equalsIgnoreCase("guardia_rey")) {
                if (tut == null || tut.tutorialLevel < 6) {
                    initialNodeId = "g_locked";
                } else if (tut.tutorialLevel > 6) {
                    initialNodeId = "g_post_completion";
                }
            } else if (type.equalsIgnoreCase("sacerdote")) {
                if (tut == null || tut.tutorialLevel < 7) {
                    initialNodeId = "sac_locked";
                } else if (tut.tutorialLevel >= 10 && !tut.throneClaimed) {
                    initialNodeId = "sacerdote_throne_claim";
                } else if (tut.tutorialLevel > 7) {
                    initialNodeId = "sac_post_completion";
                }
            } else if (type.equalsIgnoreCase("capitan_arena")) {
                if (tut == null || tut.tutorialLevel < 8) {
                    initialNodeId = "c_locked";
                } else if (tut.tutorialLevel > 8) {
                    initialNodeId = "c_post_completion";
                }
            } else if (type.equalsIgnoreCase("maestro_cargas")) {
                if (tut == null || tut.tutorialLevel < 9) {
                    initialNodeId = "mst_locked";
                } else if (tut.tutorialLevel >= 10) {
                    initialNodeId = "mst_post_completion";
                }
            }

            com.mundodetronos2.dialogue.DialogueNode initialNode = com.mundodetronos2.dialogue.NpcDialogueManager.getNode(type, initialNodeId);
            if (initialNode == null) {
                initialNode = com.mundodetronos2.dialogue.NpcDialogueManager.getNode(type, "inicio");
            }
            if (initialNode != null) {
                List<String> optionTexts = new ArrayList<>();
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
                        this.getSkinName(),
                        optionTexts
                    );
                com.mundodetronos2.network.NetworkManager.sendToPlayer(pkt, sp);
            } else {
                List<String> dList = new ArrayList<>();
                if (type.equals("guardian")) {
                    dList.add("Hola. Soy el Guardián de la base. Puedo entregarte tu armadura inicial de rol.");
                } else if (type.equals("sacerdote")) {
                    dList.add("Sólo puedo entregarte la ofrenda si me das algo a cambio.");
                } else if (type.equals("heraldo")) {
                    dList.add("¡Saludos! Te enseñaré lo que debes hacer en Mundo de Tronos 2. ¿Deseas ver la guía interactiva?");
                } else {
                    dList.add("Hola, caminante de tronos. Elige tu rol en el Altar y sigue la senda de tu equipo.");
                }

                List<String> optionTexts = new ArrayList<>();
                optionTexts.add("Cerrar");
                com.mundodetronos2.network.NetworkManager.S2COpenNpcDialoguePacket pkt =
                    new com.mundodetronos2.network.NetworkManager.S2COpenNpcDialoguePacket(
                        this.getId(),
                        type,
                        name,
                        dList.get(0),
                        "inicio",
                        this.getSkinName(),
                        optionTexts
                    );
                com.mundodetronos2.network.NetworkManager.sendToPlayer(pkt, sp);
            }
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
