package com.mundodetronos2.network;

import com.mundodetronos2.realm.InviteData;
import com.mundodetronos2.realm.RealmData;
import com.mundodetronos2.realm.RealmManager;
import com.mundodetronos2.role.PlayerRole;
import com.mundodetronos2.role.PlayerRoleData;
import com.mundodetronos2.role.RoleManager;
import com.mundodetronos2.throne.ThroneData;
import com.mundodetronos2.throne.ThroneManager;
import com.mundodetronos2.time.TimeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class NetworkManager {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("mundodetronos2", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void registerPackets() {
        // Servidor a Cliente: Mensaje/Feedback
        INSTANCE.registerMessage(packetId++, S2CShowMessagePacket.class,
                S2CShowMessagePacket::encode, S2CShowMessagePacket::decode, S2CShowMessagePacket::handle);

        // Cliente a Servidor: Abrir GUI Principal / Solicitar Sync
        INSTANCE.registerMessage(packetId++, C2SOpenMainGuiPacket.class,
                C2SOpenMainGuiPacket::encode, C2SOpenMainGuiPacket::decode, C2SOpenMainGuiPacket::handle);

        // Servidor a Cliente: Abrir GUI Principal con datos
        INSTANCE.registerMessage(packetId++, S2COpenMainGuiPacket.class,
                S2COpenMainGuiPacket::encode, S2COpenMainGuiPacket::decode, S2COpenMainGuiPacket::handle);

        // Cliente a Servidor: Crear Reino
        INSTANCE.registerMessage(packetId++, C2SCreateRealmPacket.class,
                C2SCreateRealmPacket::encode, C2SCreateRealmPacket::decode, C2SCreateRealmPacket::handle);

        // Cliente a Servidor: Abandonar Reino
        INSTANCE.registerMessage(packetId++, C2SLeaveRealmPacket.class,
                C2SLeaveRealmPacket::encode, C2SLeaveRealmPacket::decode, C2SLeaveRealmPacket::handle);

        // Cliente a Servidor: Solicitar Lista de Reinos (unirse)
        INSTANCE.registerMessage(packetId++, C2SRequestRealmListPacket.class,
                C2SRequestRealmListPacket::encode, C2SRequestRealmListPacket::decode, C2SRequestRealmListPacket::handle);

        // Servidor a Cliente: Enviar Lista de Reinos
        INSTANCE.registerMessage(packetId++, S2CRealmListPacket.class,
                S2CRealmListPacket::encode, S2CRealmListPacket::decode, S2CRealmListPacket::handle);

        // Cliente a Servidor: Solicitar unirse a reino / enviar invitación
        INSTANCE.registerMessage(packetId++, C2SRequestJoinRealmPacket.class,
                C2SRequestJoinRealmPacket::encode, C2SRequestJoinRealmPacket::decode, C2SRequestJoinRealmPacket::handle);

        // Cliente a Servidor: Aceptar invitación
        INSTANCE.registerMessage(packetId++, C2SAcceptInvitePacket.class,
                C2SAcceptInvitePacket::encode, C2SAcceptInvitePacket::decode, C2SAcceptInvitePacket::handle);

        // Cliente a Servidor: Rechazar invitación
        INSTANCE.registerMessage(packetId++, C2SRejectInvitePacket.class,
                C2SRejectInvitePacket::encode, C2SRejectInvitePacket::decode, C2SRejectInvitePacket::handle);

        // Servidor a Cliente: Reproducir Sonido de Golpe en el Trono
        INSTANCE.registerMessage(packetId++, S2CPlayThroneHitSoundPacket.class,
                S2CPlayThroneHitSoundPacket::encode, S2CPlayThroneHitSoundPacket::decode, S2CPlayThroneHitSoundPacket::handle);

        // Servidor a Cliente: Sincronizar datos de un Trono Específico (para barra de vida)
        INSTANCE.registerMessage(packetId++, S2CSyncThroneDataPacket.class,
                S2CSyncThroneDataPacket::encode, S2CSyncThroneDataPacket::decode, S2CSyncThroneDataPacket::handle);

        // --- SISTEMA DE ROLES PACKETS ---

        // Servidor a Cliente: Abrir Selección de Roles
        INSTANCE.registerMessage(packetId++, S2COpenRoleSelectionPacket.class,
                S2COpenRoleSelectionPacket::encode, S2COpenRoleSelectionPacket::decode, S2COpenRoleSelectionPacket::handle);

        // Cliente a Servidor: Seleccionar un Rol
        INSTANCE.registerMessage(packetId++, C2SSelectRolePacket.class,
                C2SSelectRolePacket::encode, C2SSelectRolePacket::decode, C2SSelectRolePacket::handle);

        // Servidor a Cliente: Enviar Datos de Carnet de Rol
        INSTANCE.registerMessage(packetId++, S2CRoleCardDataPacket.class,
                S2CRoleCardDataPacket::encode, S2CRoleCardDataPacket::decode, S2CRoleCardDataPacket::handle);

        // Servidor a Cliente: Iniciar Cinemática de Ojos (Pestañeo)
        INSTANCE.registerMessage(packetId++, S2CStartRoleCinematicPacket.class,
                S2CStartRoleCinematicPacket::encode, S2CStartRoleCinematicPacket::decode, S2CStartRoleCinematicPacket::handle);

        // --- S2C HUD SYNC PACKET ---
        INSTANCE.registerMessage(packetId++, S2CHudSyncPacket.class,
                S2CHudSyncPacket::encode, S2CHudSyncPacket::decode, S2CHudSyncPacket::handle);

        // --- PORTAL DE LA DIOSA PACKETS ---
        INSTANCE.registerMessage(packetId++, S2COpenGoddessPortalPacket.class,
                S2COpenGoddessPortalPacket::encode, S2COpenGoddessPortalPacket::decode, S2COpenGoddessPortalPacket::handle);

        INSTANCE.registerMessage(packetId++, C2SEnterGoddessDimensionPacket.class,
                C2SEnterGoddessDimensionPacket::encode, C2SEnterGoddessDimensionPacket::decode, C2SEnterGoddessDimensionPacket::handle);

        // --- CLIENT TO SERVER: TELEPORT REQUEST (POST-CINEMATIC TYPEWRITER) ---
        INSTANCE.registerMessage(packetId++, C2SSelectRoleTeleportPacket.class,
                C2SSelectRoleTeleportPacket::encode, C2SSelectRoleTeleportPacket::decode, C2SSelectRoleTeleportPacket::handle);

        // --- S2C DEATH REBIRTH CINEMATIC PACKET ---
        INSTANCE.registerMessage(packetId++, S2CStartDeathRebirthCinematicPacket.class,
                S2CStartDeathRebirthCinematicPacket::encode, S2CStartDeathRebirthCinematicPacket::decode, S2CStartDeathRebirthCinematicPacket::handle);

        // --- S2C SHOW DEATH ALERT PACKET ---
        INSTANCE.registerMessage(packetId++, S2CShowDeathAlertPacket.class,
                S2CShowDeathAlertPacket::encode, S2CShowDeathAlertPacket::decode, S2CShowDeathAlertPacket::handle);

        // --- S2C SHOW THRONE LIFE LOSS ALERT PACKET ---
        INSTANCE.registerMessage(packetId++, S2CThroneLifeLossAlertPacket.class,
                S2CThroneLifeLossAlertPacket::encode, S2CThroneLifeLossAlertPacket::decode, S2CThroneLifeLossAlertPacket::handle);

        // --- S2C ROLE ANVIL PACKETS ---
        INSTANCE.registerMessage(packetId++, S2COpenRoleAnvilPacket.class,
                S2COpenRoleAnvilPacket::encode, S2COpenRoleAnvilPacket::decode, S2COpenRoleAnvilPacket::handle);

        INSTANCE.registerMessage(packetId++, C2SForgeEquipmentPacket.class,
                C2SForgeEquipmentPacket::encode, C2SForgeEquipmentPacket::decode, C2SForgeEquipmentPacket::handle);

        // --- C2S ADMIN EQUIPMENT SWITCH PACKET ---
        INSTANCE.registerMessage(packetId++, C2SChangeAdminRoleMenuPacket.class,
                C2SChangeAdminRoleMenuPacket::encode, C2SChangeAdminRoleMenuPacket::decode, C2SChangeAdminRoleMenuPacket::handle);

        // --- SKILL TREE PACKETS ---
        INSTANCE.registerMessage(packetId++, C2SOpenSkillTreePacket.class,
                C2SOpenSkillTreePacket::encode, C2SOpenSkillTreePacket::decode, C2SOpenSkillTreePacket::handle);

        INSTANCE.registerMessage(packetId++, S2COpenSkillTreePacket.class,
                S2COpenSkillTreePacket::encode, S2COpenSkillTreePacket::decode, S2COpenSkillTreePacket::handle);

        INSTANCE.registerMessage(packetId++, C2SCastSkillPacket.class,
                C2SCastSkillPacket::encode, C2SCastSkillPacket::decode, C2SCastSkillPacket::handle);

        INSTANCE.registerMessage(packetId++, C2SUnlockSkillNodePacket.class,
                C2SUnlockSkillNodePacket::encode, C2SUnlockSkillNodePacket::decode, C2SUnlockSkillNodePacket::handle);

        // --- BLACKSMITH TABLE PACKETS ---
        INSTANCE.registerMessage(packetId++, S2COpenBlacksmithTablePacket.class,
                S2COpenBlacksmithTablePacket::encode, S2COpenBlacksmithTablePacket::decode, S2COpenBlacksmithTablePacket::handle);

        INSTANCE.registerMessage(packetId++, C2SClaimBlacksmithKitPacket.class,
                C2SClaimBlacksmithKitPacket::encode, C2SClaimBlacksmithKitPacket::decode, C2SClaimBlacksmithKitPacket::handle);

        // --- C2S COMPLETE ROLE SELECTION PACKET ---
        INSTANCE.registerMessage(packetId++, C2SCompleteRoleSelectionPacket.class,
                C2SCompleteRoleSelectionPacket::encode, C2SCompleteRoleSelectionPacket::decode, C2SCompleteRoleSelectionPacket::handle);

        // --- S2C OPEN ALTAR OPTION PACKET ---
        INSTANCE.registerMessage(packetId++, S2COpenAltarOptionPacket.class,
                S2COpenAltarOptionPacket::encode, S2COpenAltarOptionPacket::decode, S2COpenAltarOptionPacket::handle);

        // --- C2S EXIT ROLE DIMENSION PACKET ---
        INSTANCE.registerMessage(packetId++, C2SExitRoleDimensionPacket.class,
                C2SExitRoleDimensionPacket::encode, C2SExitRoleDimensionPacket::decode, C2SExitRoleDimensionPacket::handle);

        // --- C2S CLAIM GODDESS BOOK PACKET ---
        INSTANCE.registerMessage(packetId++, C2SClaimGoddessBookPacket.class,
                C2SClaimGoddessBookPacket::encode, C2SClaimGoddessBookPacket::decode, C2SClaimGoddessBookPacket::handle);

        // --- C2S DEFUSE SUCCESS PACKET ---
        INSTANCE.registerMessage(packetId++, C2SDefuseSuccessPacket.class,
                C2SDefuseSuccessPacket::encode, C2SDefuseSuccessPacket::decode, C2SDefuseSuccessPacket::handle);

        // --- S2C THRONE ATTACK ALERT PACKET ---
        INSTANCE.registerMessage(packetId++, S2CThroneAttackAlertPacket.class,
                S2CThroneAttackAlertPacket::encode, S2CThroneAttackAlertPacket::decode, S2CThroneAttackAlertPacket::handle);

        // --- C2S CLAIM ROLE ARMOR PACKET ---
        INSTANCE.registerMessage(packetId++, C2SClaimRoleArmorPacket.class,
                C2SClaimRoleArmorPacket::encode, C2SClaimRoleArmorPacket::decode, C2SClaimRoleArmorPacket::handle);

        // --- C2S REPAIR INITIAL ARMOR PACKET ---
        INSTANCE.registerMessage(packetId++, C2SRepairInitialArmorPacket.class,
                C2SRepairInitialArmorPacket::encode, C2SRepairInitialArmorPacket::decode, C2SRepairInitialArmorPacket::handle);

        // --- S2C TOGGLE LIMITES PACKET ---
        INSTANCE.registerMessage(packetId++, S2CToggleLimitesPacket.class,
                S2CToggleLimitesPacket::encode, S2CToggleLimitesPacket::decode, S2CToggleLimitesPacket::handle);

        // --- S2C TOGGLE CHAT PACKET ---
        INSTANCE.registerMessage(packetId++, S2CToggleChatPacket.class,
                S2CToggleChatPacket::encode, S2CToggleChatPacket::decode, S2CToggleChatPacket::handle);

        // --- C2S OPEN RPG INVENTORY PACKET ---
        INSTANCE.registerMessage(packetId++, C2SOpenRPGInventoryPacket.class,
                C2SOpenRPGInventoryPacket::encode, C2SOpenRPGInventoryPacket::decode, C2SOpenRPGInventoryPacket::handle);
    }

    public static void sendToPlayer(Object message, ServerPlayer player) {
        INSTANCE.sendTo(message, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void syncHud(ServerPlayer player) {
        if (player == null) return;

        int lives = 0;
        int points = 0;
        int tx = 0, ty = 0, tz = 0;
        String tDim = "";
        String teamName = "NINGUNO";

        RealmData realm = RealmManager.getPlayerRealm(player.getUUID());
        if (realm != null) {
            lives = realm.getCurrentLives();
            points = realm.getSharedPoints();
            teamName = realm.getName();
            if (realm.getThroneId() != null) {
                ThroneData throne = ThroneManager.getThroneById(realm.getThroneId());
                if (throne != null) {
                    tx = throne.getX();
                    ty = throne.getY();
                    tz = throne.getZ();
                    tDim = throne.getDimension();
                }
            }
        }

        int remaining = TimeManager.getRemainingSeconds(player.getUUID());

        com.mundodetronos2.progression.PlayerProgressData pData = com.mundodetronos2.progression.ProgressionManager.getProgressData(player.getUUID());
        PlayerRoleData rData = RoleManager.getPlayerRoleData(player.getUUID());
        String roleStr = rData.isHasRole() ? rData.getRole().name().toLowerCase() : "none";
        int level = pData.getLevel();
        int currentXp = pData.getXp();
        int neededXp = com.mundodetronos2.progression.LevelSystem.getXpNeeded(level);

        int tutorialLevel = 1;

        S2CHudSyncPacket pkt = new S2CHudSyncPacket(lives, points, remaining, roleStr, level, currentXp, neededXp, tutorialLevel, teamName, tx, ty, tz, tDim);
        sendToPlayer(pkt, player);
    }

    public static net.minecraft.world.item.ItemStack createThroneGuideBook() {
        net.minecraft.world.item.ItemStack book = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITTEN_BOOK);
        net.minecraft.nbt.CompoundTag tag = book.getOrCreateTag();
        tag.putString("title", "§6Guía del Trono");
        tag.putString("author", "El Custodio");

        net.minecraft.nbt.ListTag pages = new net.minecraft.nbt.ListTag();
        pages.add(net.minecraft.nbt.StringTag.valueOf("{\"text\":\"§1§lGuía del Trono§r\\n\\n1. Solo el líder puede colocar el Trono.\\n2. Colócalo donde quieras establecer tu base.\\n3. El Trono será el centro de tu protección.\\n4. La protección será de 150x150 bloques.\"}"));
        pages.add(net.minecraft.nbt.StringTag.valueOf("{\"text\":\"5. Tus miembros podrán construir dentro.\\n6. Durante un asedio la protección se desactiva.\\n7. El Trono tiene vidas.\\n8. Las Cargas de Asalto son necesarias para destruirlo.\"}"));
        pages.add(net.minecraft.nbt.StringTag.valueOf("{\"text\":\"9. Si el Trono llega a 0 HP pierde una vida y entra en reconstrucción.\\n10. El Trono no se debe volver a colocar después de ser dañado.\"}"));
        tag.put("pages", pages);
        return book;
    }

    // ------------------ PACKET DEFINITIONS ------------------

    // S2C Show Message
    public static class S2CShowMessagePacket {
        private final String message;
        private final boolean isError;

        public S2CShowMessagePacket(String message, boolean isError) {
            this.message = message;
            this.isError = isError;
        }

        public static void encode(S2CShowMessagePacket msg, FriendlyByteBuf buf) {
            buf.writeUtf(msg.message);
            buf.writeBoolean(msg.isError);
        }

        public static S2CShowMessagePacket decode(FriendlyByteBuf buf) {
            return new S2CShowMessagePacket(buf.readUtf(), buf.readBoolean());
        }

        public static void handle(S2CShowMessagePacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.mundodetronos2.client.ClientPacketHandler.handleShowMessage(msg.message, msg.isError);
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // --- S2C TOGGLE CHAT PACKET ---
    public static class S2CToggleChatPacket {
        public S2CToggleChatPacket() {}
        public static void encode(S2CToggleChatPacket msg, FriendlyByteBuf buf) {}
        public static S2CToggleChatPacket decode(FriendlyByteBuf buf) { return new S2CToggleChatPacket(); }
        public static void handle(S2CToggleChatPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.mundodetronos2.client.ClientEvents.enableCustomChat = !com.mundodetronos2.client.ClientEvents.enableCustomChat;
                    boolean enabled = com.mundodetronos2.client.ClientEvents.enableCustomChat;
                    com.mundodetronos2.client.ClientPacketHandler.handleShowMessage(enabled ? "Chat MMORPG Activado (Chat Vanilla Oculto)" : "Chat Vanilla Restaurado (Chat MMORPG Oculto)", !enabled);
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // --- C2S UNLOCK SKILL NODE PACKET ---
    public static class C2SUnlockSkillNodePacket {
        private final String roleId;
        private final String skillId;

        public C2SUnlockSkillNodePacket(String roleId, String skillId) {
            this.roleId = roleId;
            this.skillId = skillId;
        }

        public static void encode(C2SUnlockSkillNodePacket msg, FriendlyByteBuf buf) {
            buf.writeUtf(msg.roleId);
            buf.writeUtf(msg.skillId);
        }

        public static C2SUnlockSkillNodePacket decode(FriendlyByteBuf buf) {
            return new C2SUnlockSkillNodePacket(buf.readUtf(), buf.readUtf());
        }

        public static void handle(C2SUnlockSkillNodePacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                com.mundodetronos2.skills.SkillPointManager.tryUnlockSkill(player.getUUID(), msg.roleId, msg.skillId, player);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // C2S Open Main Gui
    public static class C2SOpenMainGuiPacket {
        public C2SOpenMainGuiPacket() {}

        public static void encode(C2SOpenMainGuiPacket msg, FriendlyByteBuf buf) {}
        public static C2SOpenMainGuiPacket decode(FriendlyByteBuf buf) {
            return new C2SOpenMainGuiPacket();
        }

        public static void handle(C2SOpenMainGuiPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                UUID playerId = player.getUUID();
                RealmData playerRealm = RealmManager.getPlayerRealm(playerId);
                ThroneData playerThrone = null;
                if (playerRealm != null && playerRealm.getThroneId() != null) {
                    playerThrone = ThroneManager.getThroneById(playerRealm.getThroneId());
                }

                List<InviteData> playerInvites = RealmManager.getPlayerInvites(playerId);

                S2COpenMainGuiPacket s2c = new S2COpenMainGuiPacket(playerRealm, playerThrone, playerInvites);
                sendToPlayer(s2c, player);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // S2C Open Main Gui
    public static class S2COpenMainGuiPacket {
        private final boolean hasRealm;
        private final String realmName;
        private final String realmColor;
        private final int currentLives;
        private final int maxLives;
        private final int memberCount;
        private final int maxPlayers;
        private final boolean isOwner;
        private final String thronePosStr;
        private final String throneStateStr;
        private final int throneHealth;
        private final int throneMaxHealth;
        private final List<InviteData> invites;

        private final String playerRole;
        private final int playerRoleLevel;

        public S2COpenMainGuiPacket(RealmData realm, ThroneData throne, List<InviteData> invites) {
            this.hasRealm = realm != null;
            if (realm != null) {
                this.realmName = realm.getName();
                this.realmColor = realm.getColor();
                this.currentLives = realm.getCurrentLives();
                this.maxLives = realm.getMaxLives();
                this.memberCount = realm.getMembers().size();
                this.maxPlayers = realm.getMaxPlayers();
                this.isOwner = throne != null && throne.getRealmId().equals(realm.getId());
            } else {
                this.realmName = "";
                this.realmColor = "";
                this.currentLives = 0;
                this.maxLives = 0;
                this.memberCount = 0;
                this.maxPlayers = 0;
                this.isOwner = false;
            }

            if (throne != null) {
                this.thronePosStr = throne.getPos().getX() + ", " + throne.getPos().getY() + ", " + throne.getPos().getZ();
                this.throneStateStr = throne.getState().name();
                this.throneHealth = throne.getHealth();
                this.throneMaxHealth = throne.getMaxHealth();
            } else {
                this.thronePosStr = "No registrado";
                this.throneStateStr = "N/A";
                this.throneHealth = 0;
                this.throneMaxHealth = 0;
            }
            this.invites = invites != null ? invites : new ArrayList<>();
            this.playerRole = "";
            this.playerRoleLevel = 1;
        }

        public S2COpenMainGuiPacket(boolean hasRealm, String realmName, String realmColor, int currentLives, int maxLives,
                                    int memberCount, int maxPlayers, boolean isOwner, String thronePosStr, String throneStateStr,
                                    int throneHealth, int throneMaxHealth, List<InviteData> invites, String playerRole, int playerRoleLevel) {
            this.hasRealm = hasRealm;
            this.realmName = realmName;
            this.realmColor = realmColor;
            this.currentLives = currentLives;
            this.maxLives = maxLives;
            this.memberCount = memberCount;
            this.maxPlayers = maxPlayers;
            this.isOwner = isOwner;
            this.thronePosStr = thronePosStr;
            this.throneStateStr = throneStateStr;
            this.throneHealth = throneHealth;
            this.throneMaxHealth = throneMaxHealth;
            this.invites = invites;
            this.playerRole = playerRole;
            this.playerRoleLevel = playerRoleLevel;
        }

        public static void encode(S2COpenMainGuiPacket msg, FriendlyByteBuf buf) {
            buf.writeBoolean(msg.hasRealm);
            buf.writeUtf(msg.realmName);
            buf.writeUtf(msg.realmColor);
            buf.writeInt(msg.currentLives);
            buf.writeInt(msg.maxLives);
            buf.writeInt(msg.memberCount);
            buf.writeInt(msg.maxPlayers);
            buf.writeBoolean(msg.isOwner);
            buf.writeUtf(msg.thronePosStr);
            buf.writeUtf(msg.throneStateStr);
            buf.writeInt(msg.throneHealth);
            buf.writeInt(msg.throneMaxHealth);
            buf.writeInt(msg.invites.size());
            for (InviteData invite : msg.invites) {
                buf.writeUUID(invite.getInviteId());
                buf.writeUtf(invite.getRealmName());
                buf.writeUtf(invite.getSenderName());
            }
            buf.writeUtf(msg.playerRole);
            buf.writeInt(msg.playerRoleLevel);
        }

        public static S2COpenMainGuiPacket decode(FriendlyByteBuf buf) {
            boolean hasRealm = buf.readBoolean();
            String realmName = buf.readUtf();
            String realmColor = buf.readUtf();
            int currentLives = buf.readInt();
            int maxLives = buf.readInt();
            int memberCount = buf.readInt();
            int maxPlayers = buf.readInt();
            boolean isOwner = buf.readBoolean();
            String thronePosStr = buf.readUtf();
            String throneStateStr = buf.readUtf();
            int throneHealth = buf.readInt();
            int throneMaxHealth = buf.readInt();
            int inviteCount = buf.readInt();
            List<InviteData> invites = new ArrayList<>();
            for (int i = 0; i < inviteCount; i++) {
                UUID inviteId = buf.readUUID();
                String rName = buf.readUtf();
                String sName = buf.readUtf();
                InviteData inv = new InviteData();
                inv.setInviteId(inviteId);
                inv.setRealmName(rName);
                inv.setSenderName(sName);
                invites.add(inv);
            }
            String pRole = buf.readUtf();
            int pLvl = buf.readInt();
            return new S2COpenMainGuiPacket(hasRealm, realmName, realmColor, currentLives, maxLives, memberCount, maxPlayers, isOwner, thronePosStr, throneStateStr, throneHealth, throneMaxHealth, invites, pRole, pLvl);
        }

        public static void handle(S2COpenMainGuiPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                String roleStr = "";
                int roleLvl = 1;
                if (player != null) {
                    PlayerRoleData rData = RoleManager.getPlayerRoleData(player.getUUID());
                    roleStr = rData.isHasRole() ? rData.getRole().name() : "";
                    roleLvl = rData.getLevel();
                }

                final String fRoleStr = roleStr;
                final int fRoleLvl = roleLvl;

                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.mundodetronos2.client.ClientPacketHandler.handleOpenMainGui(msg, fRoleStr, fRoleLvl);
                });
            });
            ctx.get().setPacketHandled(true);
        }

        public boolean hasRealm() { return hasRealm; }
        public String getRealmName() { return realmName; }
        public String getRealmColor() { return realmColor; }
        public int getCurrentLives() { return currentLives; }
        public int getMaxLives() { return maxLives; }
        public int getMemberCount() { return memberCount; }
        public int getMaxPlayers() { return maxPlayers; }
        public boolean isOwner() { return isOwner; }
        public String getThronePosStr() { return thronePosStr; }
        public String getThroneStateStr() { return throneStateStr; }
        public int getThroneHealth() { return throneHealth; }
        public int getThroneMaxHealth() { return throneMaxHealth; }
        public List<InviteData> getInvites() { return invites; }
        public String getPlayerRole() { return playerRole; }
        public int getPlayerRoleLevel() { return playerRoleLevel; }
    }

    // C2S Create Realm
    public static class C2SCreateRealmPacket {
        private final String realmName;

        public C2SCreateRealmPacket(String name) {
            this.realmName = name;
        }

        public static void encode(C2SCreateRealmPacket msg, FriendlyByteBuf buf) {
            buf.writeUtf(msg.realmName);
        }

        public static C2SCreateRealmPacket decode(FriendlyByteBuf buf) {
            return new C2SCreateRealmPacket(buf.readUtf());
        }

        public static void handle(C2SCreateRealmPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                String colorKey = msg.realmName.toLowerCase().trim();
                boolean ok = RealmManager.joinTeam(player, colorKey);
                if (ok) {
                    sendToPlayer(new S2CShowMessagePacket("¡Te has unido al equipo " + colorKey.toUpperCase() + "!", false), player);
                    C2SOpenMainGuiPacket.handle(new C2SOpenMainGuiPacket(), ctx);
                } else {
                    sendToPlayer(new S2CShowMessagePacket("No te pudiste unir al equipo " + colorKey.toUpperCase() + ". (¿Ya perteneces a uno o el equipo está lleno 6/6?)", true), player);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // C2S Leave Realm
    public static class C2SLeaveRealmPacket {
        public C2SLeaveRealmPacket() {}

        public static void encode(C2SLeaveRealmPacket msg, FriendlyByteBuf buf) {}
        public static C2SLeaveRealmPacket decode(FriendlyByteBuf buf) {
            return new C2SLeaveRealmPacket();
        }

        public static void handle(C2SLeaveRealmPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                boolean success = RealmManager.leaveRealm(player.getUUID());
                if (success) {
                    sendToPlayer(new S2CShowMessagePacket("Has salido del reino exitosamente.", false), player);
                    C2SOpenMainGuiPacket.handle(new C2SOpenMainGuiPacket(), ctx);
                } else {
                    sendToPlayer(new S2CShowMessagePacket("No perteneces a ningún reino.", true), player);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // C2S Request Realm List
    public static class C2SRequestRealmListPacket {
        public C2SRequestRealmListPacket() {}

        public static void encode(C2SRequestRealmListPacket msg, FriendlyByteBuf buf) {}
        public static C2SRequestRealmListPacket decode(FriendlyByteBuf buf) {
            return new C2SRequestRealmListPacket();
        }

        public static void handle(C2SRequestRealmListPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                List<RealmData> realms = new ArrayList<>(RealmManager.getRealmsMap().values());
                sendToPlayer(new S2CRealmListPacket(realms), player);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // S2C Realm List Packet
    public static class S2CRealmListPacket {
        private final List<RealmInfo> realms;

        public static class RealmInfo {
            public UUID id;
            public String name;
            public String color;
            public int members;
            public int maxPlayers;

            public RealmInfo(UUID id, String name, String color, int members, int maxPlayers) {
                this.id = id;
                this.name = name;
                this.color = color;
                this.members = members;
                this.maxPlayers = maxPlayers;
            }
        }

        public S2CRealmListPacket(List<RealmData> realmList) {
            this.realms = new ArrayList<>();
            for (RealmData r : realmList) {
                this.realms.add(new RealmInfo(r.getId(), r.getName(), r.getColor(), r.getMembers().size(), r.getMaxPlayers()));
            }
        }

        public S2CRealmListPacket(List<RealmInfo> realms, boolean isDummy) {
            this.realms = realms;
        }

        public static void encode(S2CRealmListPacket msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.realms.size());
            for (RealmInfo info : msg.realms) {
                buf.writeUUID(info.id);
                buf.writeUtf(info.name);
                buf.writeUtf(info.color);
                buf.writeInt(info.members);
                buf.writeInt(info.maxPlayers);
            }
        }

        public static S2CRealmListPacket decode(FriendlyByteBuf buf) {
            int count = buf.readInt();
            List<RealmInfo> list = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                list.add(new RealmInfo(buf.readUUID(), buf.readUtf(), buf.readUtf(), buf.readInt(), buf.readInt()));
            }
            return new S2CRealmListPacket(list, true);
        }

        public static void handle(S2CRealmListPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.mundodetronos2.client.ClientPacketHandler.handleRealmList(msg.realms);
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // C2S Request Join Realm
    public static class C2SRequestJoinRealmPacket {
        private final UUID realmId;

        public C2SRequestJoinRealmPacket(UUID realmId) {
            this.realmId = realmId;
        }

        public static void encode(C2SRequestJoinRealmPacket msg, FriendlyByteBuf buf) {
            buf.writeUUID(msg.realmId);
        }

        public static C2SRequestJoinRealmPacket decode(FriendlyByteBuf buf) {
            return new C2SRequestJoinRealmPacket(buf.readUUID());
        }

        public static void handle(C2SRequestJoinRealmPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                RealmData realm = RealmManager.getRealm(msg.realmId);
                if (realm == null) {
                    sendToPlayer(new S2CShowMessagePacket("¡Reino no encontrado!", true), player);
                    return;
                }

                boolean ok = RealmManager.sendInvite(realm.getId(), player.getUUID(), player.getGameProfile().getName(), realm.getOwnerId(), "Dueño del Reino");
                if (ok) {
                    sendToPlayer(new S2CShowMessagePacket("¡Solicitud enviada al reino '" + realm.getName() + "'!", false), player);
                } else {
                    sendToPlayer(new S2CShowMessagePacket("No se pudo enviar solicitud. ¿Reino lleno o ya tienes invitación activa?", true), player);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // C2S Accept Invite
    public static class C2SAcceptInvitePacket {
        private final UUID inviteId;

        public C2SAcceptInvitePacket(UUID inviteId) {
            this.inviteId = inviteId;
        }

        public static void encode(C2SAcceptInvitePacket msg, FriendlyByteBuf buf) {
            buf.writeUUID(msg.inviteId);
        }

        public static C2SAcceptInvitePacket decode(FriendlyByteBuf buf) {
            return new C2SAcceptInvitePacket(buf.readUUID());
        }

        public static void handle(C2SAcceptInvitePacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                boolean success = RealmManager.acceptInvite(msg.inviteId, player.getUUID());
                if (success) {
                    sendToPlayer(new S2CShowMessagePacket("¡Te has unido al reino exitosamente!", false), player);
                    C2SOpenMainGuiPacket.handle(new C2SOpenMainGuiPacket(), ctx);
                } else {
                    sendToPlayer(new S2CShowMessagePacket("Error al unirte. La invitación puede haber expirado o el reino está lleno.", true), player);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // C2S Reject Invite
    public static class C2SRejectInvitePacket {
        private final UUID inviteId;

        public C2SRejectInvitePacket(UUID inviteId) {
            this.inviteId = inviteId;
        }

        public static void encode(C2SRejectInvitePacket msg, FriendlyByteBuf buf) {
            buf.writeUUID(msg.inviteId);
        }

        public static C2SRejectInvitePacket decode(FriendlyByteBuf buf) {
            return new C2SRejectInvitePacket(buf.readUUID());
        }

        public static void handle(C2SRejectInvitePacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                boolean success = RealmManager.rejectInvite(msg.inviteId, player.getUUID());
                if (success) {
                    sendToPlayer(new S2CShowMessagePacket("Invitación rechazada.", false), player);
                    C2SOpenMainGuiPacket.handle(new C2SOpenMainGuiPacket(), ctx);
                } else {
                    sendToPlayer(new S2CShowMessagePacket("No se pudo rechazar la invitación.", true), player);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // S2C Play Throne Hit Sound
    public static class S2CPlayThroneHitSoundPacket {
        private final double x;
        private final double y;
        private final double z;
        private final String soundType;

        public S2CPlayThroneHitSoundPacket(double x, double y, double z, String soundType) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.soundType = soundType;
        }

        public static void encode(S2CPlayThroneHitSoundPacket msg, FriendlyByteBuf buf) {
            buf.writeDouble(msg.x);
            buf.writeDouble(msg.y);
            buf.writeDouble(msg.z);
            buf.writeUtf(msg.soundType);
        }

        public static S2CPlayThroneHitSoundPacket decode(FriendlyByteBuf buf) {
            return new S2CPlayThroneHitSoundPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readUtf());
        }

        public static void handle(S2CPlayThroneHitSoundPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.mundodetronos2.client.ClientPacketHandler.handlePlaySound(msg.x, msg.y, msg.z, msg.soundType);
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // S2C Sync Throne Data (For Barra de vida)
    public static class S2CSyncThroneDataPacket {
        private final String realmName;
        private final int health;
        private final int maxHealth;
        private final String state;

        public S2CSyncThroneDataPacket(String realmName, int health, int maxHealth, String state) {
            this.realmName = realmName;
            this.health = health;
            this.maxHealth = maxHealth;
            this.state = state;
        }

        public static void encode(S2CSyncThroneDataPacket msg, FriendlyByteBuf buf) {
            buf.writeUtf(msg.realmName);
            buf.writeInt(msg.health);
            buf.writeInt(msg.maxHealth);
            buf.writeUtf(msg.state);
        }

        public static S2CSyncThroneDataPacket decode(FriendlyByteBuf buf) {
            return new S2CSyncThroneDataPacket(buf.readUtf(), buf.readInt(), buf.readInt(), buf.readUtf());
        }

        public static void handle(S2CSyncThroneDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.mundodetronos2.client.ClientPacketHandler.handleSyncThroneData(msg.realmName, msg.health, msg.maxHealth, msg.state);
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // --- PACKETS FOR ROLES SYSTEM ---

    // S2C Open Role Selection
    public static class S2COpenRoleSelectionPacket {
        public S2COpenRoleSelectionPacket() {}
        public static void encode(S2COpenRoleSelectionPacket msg, FriendlyByteBuf buf) {}
        public static S2COpenRoleSelectionPacket decode(FriendlyByteBuf buf) { return new S2COpenRoleSelectionPacket(); }
        public static void handle(S2COpenRoleSelectionPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.mundodetronos2.client.ClientPacketHandler.handleOpenRoleSelection();
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // C2S Select Role
    public static class C2SSelectRolePacket {
        private final String roleId;

        public C2SSelectRolePacket(String roleId) {
            this.roleId = roleId;
        }

        public static void encode(C2SSelectRolePacket msg, FriendlyByteBuf buf) {
            buf.writeUtf(msg.roleId);
        }

        public static C2SSelectRolePacket decode(FriendlyByteBuf buf) {
            return new C2SSelectRolePacket(buf.readUtf());
        }

        public static void handle(C2SSelectRolePacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                PlayerRole role = PlayerRole.fromString(msg.roleId);
                if (role == PlayerRole.NONE) {
                    sendToPlayer(new S2CShowMessagePacket("§cCamino inválido seleccionado.", true), player);
                    return;
                }

                PlayerRoleData data = RoleManager.getPlayerRoleData(player.getUUID());
                if (data.isHasRole()) {
                    sendToPlayer(new S2CShowMessagePacket("§cYa posees un rol asignado permanentemente.", true), player);
                    return;
                }

                sendToPlayer(new S2CStartRoleCinematicPacket(role.name().toLowerCase()), player);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // S2C Role Card Data
    public static class S2CRoleCardDataPacket {
        private final UUID playerId;
        private final String playerName;
        private final String roleName;
        private final int level;
        private final String realmName;
        private final String realmRole;
        private final int currentXp;
        private final int neededXp;

        public S2CRoleCardDataPacket(UUID playerId, String playerName, String roleName, int level, String realmName, String realmRole, int currentXp, int neededXp) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.roleName = roleName;
            this.level = level;
            this.realmName = realmName;
            this.realmRole = realmRole;
            this.currentXp = currentXp;
            this.neededXp = neededXp;
        }

        public static void encode(S2CRoleCardDataPacket msg, FriendlyByteBuf buf) {
            buf.writeUUID(msg.playerId);
            buf.writeUtf(msg.playerName);
            buf.writeUtf(msg.roleName);
            buf.writeInt(msg.level);
            buf.writeUtf(msg.realmName);
            buf.writeUtf(msg.realmRole);
            buf.writeInt(msg.currentXp);
            buf.writeInt(msg.neededXp);
        }

        public static S2CRoleCardDataPacket decode(FriendlyByteBuf buf) {
            return new S2CRoleCardDataPacket(buf.readUUID(), buf.readUtf(), buf.readUtf(), buf.readInt(), buf.readUtf(), buf.readUtf(), buf.readInt(), buf.readInt());
        }

        public static void handle(S2CRoleCardDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.mundodetronos2.client.ClientPacketHandler.handleOpenRoleCard(msg.playerId, msg.playerName, msg.roleName, msg.level, msg.realmName, msg.realmRole, msg.currentXp, msg.neededXp);
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // S2C Start Role Cinematic
    public static class S2CStartRoleCinematicPacket {
        private final String roleId;

        public S2CStartRoleCinematicPacket(String roleId) {
            this.roleId = roleId;
        }

        public static void encode(S2CStartRoleCinematicPacket msg, FriendlyByteBuf buf) {
            buf.writeUtf(msg.roleId);
        }

        public static S2CStartRoleCinematicPacket decode(FriendlyByteBuf buf) {
            return new S2CStartRoleCinematicPacket(buf.readUtf());
        }

        public static void handle(S2CStartRoleCinematicPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.mundodetronos2.client.ClientPacketHandler.handleStartCinematic(msg.roleId);
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // S2C HUD Sync
    public static class S2CHudSyncPacket {
        private final int throneLives;
        private final int sharedPoints;
        private final int remainingSeconds;
        private final String playerRole;
        private final int playerRoleLevel;
        private final int currentXp;
        private final int neededXp;
        private final int tutorialLevel;
        private final String teamName;
        private final int throneX;
        private final int throneY;
        private final int throneZ;
        private final String throneDim;

        public S2CHudSyncPacket(int throneLives, int sharedPoints, int remainingSeconds, String playerRole, int playerRoleLevel, int currentXp, int neededXp, int tutorialLevel, String teamName, int tx, int ty, int tz, String tDim) {
            this.throneLives = throneLives;
            this.sharedPoints = sharedPoints;
            this.remainingSeconds = remainingSeconds;
            this.playerRole = playerRole;
            this.playerRoleLevel = playerRoleLevel;
            this.currentXp = currentXp;
            this.neededXp = neededXp;
            this.tutorialLevel = tutorialLevel;
            this.teamName = teamName != null ? teamName : "NINGUNO";
            this.throneX = tx;
            this.throneY = ty;
            this.throneZ = tz;
            this.throneDim = tDim != null ? tDim : "";
        }

        public static void encode(S2CHudSyncPacket msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.throneLives);
            buf.writeInt(msg.sharedPoints);
            buf.writeInt(msg.remainingSeconds);
            buf.writeUtf(msg.playerRole);
            buf.writeInt(msg.playerRoleLevel);
            buf.writeInt(msg.currentXp);
            buf.writeInt(msg.neededXp);
            buf.writeInt(msg.tutorialLevel);
            buf.writeUtf(msg.teamName);
            buf.writeInt(msg.throneX);
            buf.writeInt(msg.throneY);
            buf.writeInt(msg.throneZ);
            buf.writeUtf(msg.throneDim);
        }

        public static S2CHudSyncPacket decode(FriendlyByteBuf buf) {
            return new S2CHudSyncPacket(
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readUtf(), buf.readInt(),
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readUtf(),
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readUtf()
            );
        }

        public static void handle(S2CHudSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.mundodetronos2.client.ClientPacketHandler.handleHudSync(
                        msg.throneLives, msg.sharedPoints, msg.remainingSeconds, msg.playerRole, msg.playerRoleLevel,
                        msg.currentXp, msg.neededXp, msg.tutorialLevel,
                        msg.teamName, msg.throneX, msg.throneY, msg.throneZ, msg.throneDim
                    );
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // S2C Open Goddess Portal
    public static class S2COpenGoddessPortalPacket {
        public S2COpenGoddessPortalPacket() {}
        public static void encode(S2COpenGoddessPortalPacket msg, FriendlyByteBuf buf) {}
        public static S2COpenGoddessPortalPacket decode(FriendlyByteBuf buf) { return new S2COpenGoddessPortalPacket(); }
        public static void handle(S2COpenGoddessPortalPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.mundodetronos2.client.ClientPacketHandler.handleOpenGoddessPortal();
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // C2S Enter Goddess Dimension
    public static class C2SEnterGoddessDimensionPacket {
        public C2SEnterGoddessDimensionPacket() {}
        public static void encode(C2SEnterGoddessDimensionPacket msg, FriendlyByteBuf buf) {}
        public static C2SEnterGoddessDimensionPacket decode(FriendlyByteBuf buf) { return new C2SEnterGoddessDimensionPacket(buf); }

        public C2SEnterGoddessDimensionPacket(FriendlyByteBuf buf) {}

        public static void handle(C2SEnterGoddessDimensionPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                PlayerRoleData rData = RoleManager.getPlayerRoleData(player.getUUID());
                if (rData.isHasRole() && rData.getRole() != PlayerRole.NONE) {
                    try {
                        ResourceLocation dimRl = new ResourceLocation("mundodetronos2", "role_dimension");
                        ResourceKey<net.minecraft.world.level.Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimRl);
                        ServerLevel level = player.getServer().getLevel(dimKey);
                        if (level != null) {
                            player.teleportTo(level, 0.5D, 64.0D, 0.5D, 0.0F, 0.0F);
                            sendToPlayer(new S2CShowMessagePacket("§a¡Has regresado al templo de la Diosa María!", false), player);
                        } else {
                            sendToPlayer(new S2CShowMessagePacket("§cLa dimensión de roles no se encuentra cargada en el servidor.", true), player);
                        }
                    } catch (Exception e) {
                        sendToPlayer(new S2CShowMessagePacket("§cError al cruzar el portal: " + e.getMessage(), true), player);
                    }
                    return;
                }

                boolean hasOffering = false;
                net.minecraft.world.item.ItemStack offeringStack = null;
                for (net.minecraft.world.item.ItemStack stack : player.getInventory().items) {
                    if (stack.getItem() == net.minecraft.world.item.Items.POPPY && stack.hasTag() && stack.getTag().getBoolean("IsGoddessOffering")) {
                        hasOffering = true;
                        offeringStack = stack;
                        break;
                    }
                }

                if (!hasOffering) {
                    sendToPlayer(new S2CShowMessagePacket("§cNo tienes la Ofrenda de la Diosa María en tu inventario.", true), player);
                    return;
                }

                offeringStack.shrink(1);

                try {
                    ResourceLocation dimRl = new ResourceLocation("mundodetronos2", "role_dimension");
                    ResourceKey<net.minecraft.world.level.Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimRl);
                    ServerLevel level = player.getServer().getLevel(dimKey);
                    if (level != null) {
                        player.teleportTo(level, 0.5D, 64.0D, 0.5D, 0.0F, 0.0F);
                        sendToPlayer(new S2CShowMessagePacket("§a¡Has depositado tu ofrenda y cruzado al templo de la Diosa María!", false), player);
                    } else {
                        sendToPlayer(new S2CShowMessagePacket("§cLa dimensión de roles no se encuentra cargada en el servidor.", true), player);
                    }
                } catch (Exception e) {
                    sendToPlayer(new S2CShowMessagePacket("§cError al cruzar el portal: " + e.getMessage(), true), player);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // --- C2S SELECT ROLE TELEPORT PACKET ---
    public static class C2SSelectRoleTeleportPacket {
        public C2SSelectRoleTeleportPacket() {}
        public static void encode(C2SSelectRoleTeleportPacket msg, FriendlyByteBuf buf) {}
        public static C2SSelectRoleTeleportPacket decode(FriendlyByteBuf buf) { return new C2SSelectRoleTeleportPacket(); }
        public static void handle(C2SSelectRoleTeleportPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                BlockPos targetPos = null;
                ServerLevel targetLevel = player.serverLevel();

                RealmData rData = RealmManager.getPlayerRealm(player.getUUID());
                if (rData != null && rData.getThroneId() != null) {
                    ThroneData tData = ThroneManager.getThroneById(rData.getThroneId());
                    if (tData != null) {
                        targetPos = tData.getPos();
                        try {
                            ResourceLocation dimRl = new ResourceLocation(tData.getDimension());
                            ServerLevel level = player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimRl));
                            if (level != null) {
                                targetLevel = level;
                            }
                        } catch (Exception ignored) {}
                    }
                }

                if (targetPos != null) {
                    player.teleportTo(targetLevel, targetPos.getX() + 0.5D, targetPos.getY() + 1.0D, targetPos.getZ() + 0.5D, player.getYRot(), player.getXRot());
                } else {
                    BlockPos spawnPos = targetLevel.getSharedSpawnPos();
                    player.teleportTo(targetLevel, spawnPos.getX() + 0.5D, spawnPos.getY() + 1.0D, spawnPos.getZ() + 0.5D, player.getYRot(), player.getXRot());
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // --- S2C DEATH REBIRTH CINEMATIC PACKET ---
    public static class S2CStartDeathRebirthCinematicPacket {
        public S2CStartDeathRebirthCinematicPacket() {}
        public static void encode(S2CStartDeathRebirthCinematicPacket msg, FriendlyByteBuf buf) {}
        public static S2CStartDeathRebirthCinematicPacket decode(FriendlyByteBuf buf) { return new S2CStartDeathRebirthCinematicPacket(); }
        public static void handle(S2CStartDeathRebirthCinematicPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.mundodetronos2.client.ClientPacketHandler.handleOpenDeathRebirthCinematic();
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // --- S2C SHOW DEATH ALERT PACKET ---
    public static class S2CShowDeathAlertPacket {
        private final UUID deadPlayerId;
        private final String deadPlayerName;

        public S2CShowDeathAlertPacket(UUID deadPlayerId, String deadPlayerName) {
            this.deadPlayerId = deadPlayerId;
            this.deadPlayerName = deadPlayerName;
        }

        public static void encode(S2CShowDeathAlertPacket msg, FriendlyByteBuf buf) {
            buf.writeUUID(msg.deadPlayerId);
            buf.writeUtf(msg.deadPlayerName);
        }

        public static S2CShowDeathAlertPacket decode(FriendlyByteBuf buf) {
            return new S2CShowDeathAlertPacket(buf.readUUID(), buf.readUtf());
        }

        public static void handle(S2CShowDeathAlertPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.mundodetronos2.client.ClientEvents.triggerDeathAlert(msg.deadPlayerId, msg.deadPlayerName);
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // --- S2C OPEN ROLE ANVIL PACKET ---
    public static class S2COpenRoleAnvilPacket {
        public S2COpenRoleAnvilPacket() {}
        public static void encode(S2COpenRoleAnvilPacket msg, FriendlyByteBuf buf) {}
        public static S2COpenRoleAnvilPacket decode(FriendlyByteBuf buf) { return new S2COpenRoleAnvilPacket(); }
        public static void handle(S2COpenRoleAnvilPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.mundodetronos2.client.ClientPacketHandler.handleOpenRoleAnvil();
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // --- C2S FORGE EQUIPMENT PACKET ---
    public static class C2SForgeEquipmentPacket {
        private final int itemSlot;

        public C2SForgeEquipmentPacket(int itemSlot) {
            this.itemSlot = itemSlot;
        }

        public static void encode(C2SForgeEquipmentPacket msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.itemSlot);
        }

        public static C2SForgeEquipmentPacket decode(FriendlyByteBuf buf) {
            return new C2SForgeEquipmentPacket(buf.readInt());
        }

        public static void handle(C2SForgeEquipmentPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                PlayerRoleData roleData = RoleManager.getPlayerRoleData(player.getUUID());
                if (!roleData.isHasRole() || roleData.getRole() == PlayerRole.NONE) {
                    sendToPlayer(new S2CShowMessagePacket("§cNo posees un rol asignado permanentemente.", true), player);
                    return;
                }

                net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(msg.itemSlot);
                if (stack.isEmpty()) {
                    sendToPlayer(new S2CShowMessagePacket("§cLa ranura seleccionada está vacía.", true), player);
                    return;
                }

                String registryName = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                String roleNameLower = roleData.getRole().name().toLowerCase();

                if (!com.mundodetronos2.role.EquipmentManager.isItemAllowed(roleNameLower, registryName)) {
                    sendToPlayer(new S2CShowMessagePacket("§cEse objeto no está permitido para tu rol.", true), player);
                    return;
                }

                String itemKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
                String uniqueId = roleNameLower + "_" + itemKey + "_" + (100 + player.getRandom().nextInt(900));

                stack.getOrCreateTag().putString("AuthorizedRole", roleNameLower);
                stack.getOrCreateTag().putString("RoleItemID", uniqueId);

                String roleTranslated = "Guerrero";
                if (roleNameLower.equalsIgnoreCase("berserker")) roleTranslated = "Berserker";
                else if (roleNameLower.equalsIgnoreCase("mage")) roleTranslated = "Mago";

                String customName = stack.getHoverName().getString();
                if (!customName.contains("de " + roleTranslated)) {
                    stack.setHoverName(Component.literal("§6" + customName + " de " + roleTranslated));
                }

                player.serverLevel().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ANVIL_USE, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.9F);
                player.serverLevel().sendParticles(
                    net.minecraft.core.particles.ParticleTypes.CRIT,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    35, 0.4D, 0.4D, 0.4D, 0.15D
                );

                sendToPlayer(new S2CShowMessagePacket("§a¡Equipamiento autorizado con éxito! ID: " + uniqueId, false), player);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // --- C2S REPAIR INITIAL ARMOR PACKET ---
    public static class C2SRepairInitialArmorPacket {
        private final int itemSlot;

        public C2SRepairInitialArmorPacket(int itemSlot) {
            this.itemSlot = itemSlot;
        }

        public static void encode(C2SRepairInitialArmorPacket msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.itemSlot);
        }

        public static C2SRepairInitialArmorPacket decode(FriendlyByteBuf buf) {
            return new C2SRepairInitialArmorPacket(buf.readInt());
        }

        public static void handle(C2SRepairInitialArmorPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(msg.itemSlot);
                if (stack.isEmpty()) return;

                boolean isRoleArmor = stack.hasTag() && stack.getTag().getBoolean("mundodetronos2:role_armor");
                boolean isInitialArmor = stack.hasTag() && "any".equalsIgnoreCase(stack.getTag().getString("AuthorizedRole")) && stack.getTag().getString("RoleItemID").startsWith("kit_inicial_");

                if (!isRoleArmor && !isInitialArmor) {
                    MessageManager.actionBar(player, "§c⚠ Solo puedes reparar armaduras de rol o de inicio.");
                    return;
                }

                int ironCount = 0;
                for (net.minecraft.world.item.ItemStack s : player.getInventory().items) {
                    if (!s.isEmpty() && s.getItem() == net.minecraft.world.item.Items.IRON_INGOT) {
                        ironCount += s.getCount();
                    }
                }

                if (ironCount < 3) {
                    MessageManager.actionBar(player, "§c⚠ No tienes suficiente material (Requiere 3 Hierro).");
                    sendToPlayer(new S2CShowMessagePacket("No tienes suficiente material (Requiere 3 Hierro).", true), player);
                    return;
                }

                if (stack.getDamageValue() <= 0) {
                    MessageManager.actionBar(player, "§c⚠ La armadura ya está completamente reparada.");
                    return;
                }

                int remainingToTake = 3;
                for (net.minecraft.world.item.ItemStack s : player.getInventory().items) {
                    if (!s.isEmpty() && s.getItem() == net.minecraft.world.item.Items.IRON_INGOT) {
                        int count = s.getCount();
                        if (count <= remainingToTake) {
                            remainingToTake -= count;
                            s.setCount(0);
                        } else {
                            s.shrink(remainingToTake);
                            remainingToTake = 0;
                            break;
                        }
                    }
                }

                stack.setDamageValue(0);

                player.serverLevel().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ANVIL_USE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.2F);
                MessageManager.actionBar(player, "§a✔ ¡Armadura reparada exitosamente!");
                sendToPlayer(new S2CShowMessagePacket("¡Armadura reparada exitosamente!", false), player);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // --- C2S CHANGE ADMIN ROLE MENU PACKET ---
    public static class C2SChangeAdminRoleMenuPacket {
        private final String role;

        public C2SChangeAdminRoleMenuPacket(String role) {
            this.role = role;
        }

        public static void encode(C2SChangeAdminRoleMenuPacket msg, FriendlyByteBuf buf) {
            buf.writeUtf(msg.role);
        }

        public static C2SChangeAdminRoleMenuPacket decode(FriendlyByteBuf buf) {
            return new C2SChangeAdminRoleMenuPacket(buf.readUtf());
        }

        public static void handle(C2SChangeAdminRoleMenuPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null || !player.hasPermissions(2)) return;

                if (player.containerMenu instanceof com.mundodetronos2.gui.AdminEquipmentMenu menu) {
                    menu.switchRoleOnServer(msg.role);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // --- C2S OPEN SKILL TREE PACKET ---
    public static class C2SOpenSkillTreePacket {
        public C2SOpenSkillTreePacket() {}
        public static void encode(C2SOpenSkillTreePacket msg, FriendlyByteBuf buf) {}
        public static C2SOpenSkillTreePacket decode(FriendlyByteBuf buf) { return new C2SOpenSkillTreePacket(); }
        public static void handle(C2SOpenSkillTreePacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                PlayerRoleData rData = RoleManager.getPlayerRoleData(player.getUUID());
                if (!rData.isHasRole() || rData.getRole() == PlayerRole.NONE) {
                    player.sendSystemMessage(Component.literal("§c¡No tienes ningún rol asignado! Elige tu camino en el Altar de la Diosa María."), true);
                    return;
                }

                com.mundodetronos2.progression.PlayerProgressData pData = com.mundodetronos2.progression.ProgressionManager.getProgressData(player.getUUID());
                sendToPlayer(new S2COpenSkillTreePacket(rData.getRole().name().toLowerCase(), rData.getLevel(), pData.getSkillPoints(), pData.getUnlockedSkills()), player);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // --- S2C OPEN SKILL TREE PACKET ---
    public static class S2COpenSkillTreePacket {
        private final String roleId;
        private final int level;
        private final int skillPoints;
        private final List<String> unlockedSkills;

        public S2COpenSkillTreePacket(String roleId, int level, int skillPoints, List<String> unlockedSkills) {
            this.roleId = roleId;
            this.level = level;
            this.skillPoints = skillPoints;
            this.unlockedSkills = unlockedSkills != null ? unlockedSkills : new java.util.ArrayList<>();
        }

        public static void encode(S2COpenSkillTreePacket msg, FriendlyByteBuf buf) {
            buf.writeUtf(msg.roleId);
            buf.writeInt(msg.level);
            buf.writeInt(msg.skillPoints);
            buf.writeInt(msg.unlockedSkills.size());
            for (String skill : msg.unlockedSkills) {
                buf.writeUtf(skill);
            }
        }

        public static S2COpenSkillTreePacket decode(FriendlyByteBuf buf) {
            String roleId = buf.readUtf();
            int level = buf.readInt();
            int skillPoints = buf.readInt();
            int listSize = buf.readInt();
            List<String> unlocked = new java.util.ArrayList<>();
            for (int i = 0; i < listSize; i++) {
                unlocked.add(buf.readUtf());
            }
            return new S2COpenSkillTreePacket(roleId, level, skillPoints, unlocked);
        }

        public static void handle(S2COpenSkillTreePacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.mundodetronos2.client.ClientPacketHandler.handleOpenSkillTree(msg.roleId, msg.level, msg.skillPoints, msg.unlockedSkills);
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // --- C2S CAST SKILL PACKET ---
    public static class C2SCastSkillPacket {
        public C2SCastSkillPacket() {}
        public static void encode(C2SCastSkillPacket msg, FriendlyByteBuf buf) {}
        public static C2SCastSkillPacket decode(FriendlyByteBuf buf) { return new C2SCastSkillPacket(); }
        public static void handle(C2SCastSkillPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                com.mundodetronos2.role.SkillTreeManager.executeSkill(player);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // --- S2C OPEN BLACKSMITH TABLE PACKET ---
    public static class S2COpenBlacksmithTablePacket {
        public S2COpenBlacksmithTablePacket() {}
        public static void encode(S2COpenBlacksmithTablePacket msg, FriendlyByteBuf buf) {}
        public static S2COpenBlacksmithTablePacket decode(FriendlyByteBuf buf) { return new S2COpenBlacksmithTablePacket(); }
        public static void handle(S2COpenBlacksmithTablePacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.mundodetronos2.client.ClientPacketHandler.handleOpenBlacksmithTable();
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // --- C2S CLAIM BLACKSMITH KIT PACKET ---
    public static class C2SClaimBlacksmithKitPacket {
        public C2SClaimBlacksmithKitPacket() {}
        public static void encode(C2SClaimBlacksmithKitPacket msg, FriendlyByteBuf buf) {}
        public static C2SClaimBlacksmithKitPacket decode(FriendlyByteBuf buf) { return new C2SClaimBlacksmithKitPacket(); }
        public static void handle(C2SClaimBlacksmithKitPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                PlayerRoleData rData = RoleManager.getPlayerRoleData(player.getUUID());
                if (rData.isInitialKitClaimed()) {
                    MessageManager.actionBar(player, "§c⚠ Ya has reclamado tu kit de aventura inicial.");
                    return;
                }

                net.minecraft.world.item.ItemStack helmet = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET);
                helmet.getOrCreateTag().putBoolean("IsGoddessOffering", false);
                helmet.getOrCreateTag().putString("AuthorizedRole", "any");
                helmet.getOrCreateTag().putString("RoleItemID", "kit_inicial_helmet");
                helmet.setHoverName(Component.literal("§6Casco de Cuero de Aventura"));

                net.minecraft.world.item.ItemStack chest = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_CHESTPLATE);
                chest.getOrCreateTag().putString("AuthorizedRole", "any");
                chest.getOrCreateTag().putString("RoleItemID", "kit_inicial_chest");
                chest.setHoverName(Component.literal("§6Peto de Cuero de Aventura"));

                net.minecraft.world.item.ItemStack leggings = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_LEGGINGS);
                leggings.getOrCreateTag().putString("AuthorizedRole", "any");
                leggings.getOrCreateTag().putString("RoleItemID", "kit_inicial_leggings");
                leggings.setHoverName(Component.literal("§6Grebas de Cuero de Aventura"));

                net.minecraft.world.item.ItemStack boots = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_BOOTS);
                boots.getOrCreateTag().putString("AuthorizedRole", "any");
                boots.getOrCreateTag().putString("RoleItemID", "kit_inicial_boots");
                boots.setHoverName(Component.literal("§6Botas de Cuero de Aventura"));

                net.minecraft.world.item.ItemStack sword = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE_SWORD);
                sword.getOrCreateTag().putString("AuthorizedRole", "any");
                sword.getOrCreateTag().putString("RoleItemID", "kit_inicial_sword");
                sword.setHoverName(Component.literal("§eEspada de Piedra de Aventura"));

                net.minecraft.world.item.ItemStack pickaxe = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE_PICKAXE);
                pickaxe.getOrCreateTag().putString("AuthorizedRole", "any");
                pickaxe.getOrCreateTag().putString("RoleItemID", "kit_inicial_pickaxe");
                pickaxe.setHoverName(Component.literal("§ePico de Piedra de Aventura"));

                net.minecraft.world.item.ItemStack food = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COOKED_BEEF, 16);
                food.getOrCreateTag().putString("AuthorizedRole", "any");
                food.getOrCreateTag().putString("RoleItemID", "kit_inicial_food");
                food.setHoverName(Component.literal("§6Ración de Supervivencia"));

                net.minecraft.world.item.ItemStack torches = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TORCH, 16);
                torches.getOrCreateTag().putString("AuthorizedRole", "any");
                torches.getOrCreateTag().putString("RoleItemID", "kit_inicial_torches");
                torches.setHoverName(Component.literal("§eAntorcha de Explorador"));

                player.getInventory().add(helmet);
                player.getInventory().add(chest);
                player.getInventory().add(leggings);
                player.getInventory().add(boots);
                player.getInventory().add(sword);
                player.getInventory().add(pickaxe);
                player.getInventory().add(food);
                player.getInventory().add(torches);

                rData.setInitialKitClaimed(true);
                RoleManager.save(false);

                player.serverLevel().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ITEM_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 1.1F);
                player.serverLevel().sendParticles(
                    net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    25, 0.4D, 0.4D, 0.4D, 0.1D
                );

                MessageManager.actionBar(player, "✦ Kit inicial obtenido");
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // --- C2S COMPLETE ROLE SELECTION PACKET ---
    public static class C2SCompleteRoleSelectionPacket {
        private final String roleId;

        public C2SCompleteRoleSelectionPacket(String roleId) {
            this.roleId = roleId;
        }

        public static void encode(C2SCompleteRoleSelectionPacket msg, FriendlyByteBuf buf) {
            buf.writeUtf(msg.roleId);
        }

        public static C2SCompleteRoleSelectionPacket decode(FriendlyByteBuf buf) {
            return new C2SCompleteRoleSelectionPacket(buf.readUtf());
        }

        public static void handle(C2SCompleteRoleSelectionPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                PlayerRole role = PlayerRole.fromString(msg.roleId);
                if (role == PlayerRole.NONE) return;

                PlayerRoleData data = RoleManager.getPlayerRoleData(player.getUUID());
                if (data.isHasRole()) return;

                RoleManager.setPlayerRole(player.getUUID(), role);
                NetworkManager.syncHud(player);

                player.serverLevel().sendParticles(
                    net.minecraft.core.particles.ParticleTypes.WITCH,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    40, 0.5D, 0.5D, 0.5D, 0.15D
                );
                player.serverLevel().sendParticles(
                    net.minecraft.core.particles.ParticleTypes.INSTANT_EFFECT,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    30, 0.5D, 0.5D, 0.5D, 0.15D
                );
                player.serverLevel().playSound(
                    null, player.blockPosition(),
                    net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    1.4F, 1.2F
                );

                net.minecraft.world.item.ItemStack carnetStack = new net.minecraft.world.item.ItemStack(com.mundodetronos2.init.ItemInit.ROLE_CARD.get());
                CompoundTag tag = carnetStack.getOrCreateTag();
                tag.putString("OwnerUUID", player.getUUID().toString());
                tag.putString("OwnerName", player.getGameProfile().getName());
                tag.putString("RoleID", role.name().toLowerCase());
                tag.putInt("Level", 1);
                player.getInventory().add(carnetStack);

                player.getInventory().add(createGoddessBook());

                BlockPos targetPos = null;
                ServerLevel targetLevel = player.serverLevel();

                RealmData rData = RealmManager.getPlayerRealm(player.getUUID());
                if (rData != null && rData.getThroneId() != null) {
                    ThroneData tData = ThroneManager.getThroneById(rData.getThroneId());
                    if (tData != null) {
                        targetPos = tData.getPos();
                        try {
                            ResourceLocation dimRl = new ResourceLocation(tData.getDimension());
                            ServerLevel level = player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimRl));
                            if (level != null) {
                                targetLevel = level;
                            }
                        } catch (Exception ignored) {}
                    }
                }

                if (targetPos != null) {
                    player.teleportTo(targetLevel, targetPos.getX() + 0.5D, targetPos.getY() + 2.0D, targetPos.getZ() + 0.5D, player.getYRot(), player.getXRot());
                } else {
                    BlockPos spawnPos = targetLevel.getSharedSpawnPos();
                    player.teleportTo(targetLevel, spawnPos.getX() + 0.5D, spawnPos.getY() + 2.0D, spawnPos.getZ() + 0.5D, player.getYRot(), player.getXRot());
                }

                sendToPlayer(new S2CShowMessagePacket("§a¡Has sido elegido e investido por la Diosa María! ¡Regresas a tu trono!", false), player);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // --- S2C SHOW THRONE LIFE LOSS ALERT PACKET ---
    public static class S2CThroneLifeLossAlertPacket {
        private final String realmName;
        private final UUID attackerId;
        private final String attackerName;
        private final int oldLives;
        private final int newLives;

        public S2CThroneLifeLossAlertPacket(String realmName, UUID attackerId, String attackerName, int oldLives, int newLives) {
            this.realmName = realmName;
            this.attackerId = attackerId;
            this.attackerName = attackerName;
            this.oldLives = oldLives;
            this.newLives = newLives;
        }

        public static void encode(S2CThroneLifeLossAlertPacket msg, FriendlyByteBuf buf) {
            buf.writeUtf(msg.realmName);
            buf.writeUUID(msg.attackerId);
            buf.writeUtf(msg.attackerName);
            buf.writeInt(msg.oldLives);
            buf.writeInt(msg.newLives);
        }

        public static S2CThroneLifeLossAlertPacket decode(FriendlyByteBuf buf) {
            return new S2CThroneLifeLossAlertPacket(buf.readUtf(), buf.readUUID(), buf.readUtf(), buf.readInt(), buf.readInt());
        }

        public static void handle(S2CThroneLifeLossAlertPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.mundodetronos2.client.ClientEvents.triggerThroneAlert(msg.realmName, msg.attackerId, msg.attackerName, msg.oldLives, msg.newLives);
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // --- S2C OPEN ALTAR OPTION PACKET ---
    public static class S2COpenAltarOptionPacket {
        private final boolean hasRole;

        public S2COpenAltarOptionPacket(boolean hasRole) {
            this.hasRole = hasRole;
        }

        public static void encode(S2COpenAltarOptionPacket msg, FriendlyByteBuf buf) {
            buf.writeBoolean(msg.hasRole);
        }

        public static S2COpenAltarOptionPacket decode(FriendlyByteBuf buf) {
            return new S2COpenAltarOptionPacket(buf.readBoolean());
        }

        public static void handle(S2COpenAltarOptionPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.mundodetronos2.client.ClientPacketHandler.handleOpenAltarOption(msg.hasRole);
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // --- C2S EXIT ROLE DIMENSION PACKET ---
    public static class C2SExitRoleDimensionPacket {
        public C2SExitRoleDimensionPacket() {}

        public static void encode(C2SExitRoleDimensionPacket msg, FriendlyByteBuf buf) {}
        public static C2SExitRoleDimensionPacket decode(FriendlyByteBuf buf) {
            return new C2SExitRoleDimensionPacket();
        }

        public static void handle(C2SExitRoleDimensionPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                BlockPos targetPos = null;
                ServerLevel targetLevel = player.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);

                RealmData rData = RealmManager.getPlayerRealm(player.getUUID());
                if (rData != null) {
                    if (rData.getThroneId() != null) {
                        ThroneData tData = ThroneManager.getThroneById(rData.getThroneId());
                        if (tData != null) {
                            targetPos = tData.getPos();
                            try {
                                ResourceLocation dimRl = new ResourceLocation(tData.getDimension());
                                ServerLevel level = player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimRl));
                                if (level != null) {
                                    targetLevel = level;
                                }
                            } catch (Exception ignored) {}
                        }
                    }

                    if (targetPos == null) {
                        if (targetLevel != null) {
                            targetPos = targetLevel.getSharedSpawnPos();
                        }
                    }
                }

                if (targetPos == null) {
                    if (targetLevel != null) {
                        targetPos = targetLevel.getSharedSpawnPos();
                    }
                }

                if (targetPos != null && targetLevel != null) {
                    player.teleportTo(targetLevel, targetPos.getX() + 0.5D, targetPos.getY() + 1.0D, targetPos.getZ() + 0.5D, player.getYRot(), player.getXRot());
                    MessageManager.actionBar(player, "§a✓ Has salido de la dimensión de roles.");
                } else {
                    MessageManager.actionBar(player, "§c⚠ No se encontró un destino seguro para salir.");
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // --- C2S CLAIM GODDESS BOOK PACKET ---
    public static class C2SClaimGoddessBookPacket {
        public C2SClaimGoddessBookPacket() {}
        public static void encode(C2SClaimGoddessBookPacket msg, FriendlyByteBuf buf) {}
        public static C2SClaimGoddessBookPacket decode(FriendlyByteBuf buf) { return new C2SClaimGoddessBookPacket(); }
        public static void handle(C2SClaimGoddessBookPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                net.minecraft.world.item.ItemStack book = createGoddessBook();
                player.getInventory().add(book);
                MessageManager.actionBar(player, "§a✓ Has obtenido la Guía de la Diosa María.");
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // --- C2S DEFUSE SUCCESS PACKET ---
    public static class C2SDefuseSuccessPacket {
        private final BlockPos chargePos;

        public C2SDefuseSuccessPacket(BlockPos chargePos) {
            this.chargePos = chargePos;
        }

        public static void encode(C2SDefuseSuccessPacket msg, FriendlyByteBuf buf) {
            buf.writeBlockPos(msg.chargePos);
        }

        public static C2SDefuseSuccessPacket decode(FriendlyByteBuf buf) {
            return new C2SDefuseSuccessPacket(buf.readBlockPos());
        }

        public static void handle(C2SDefuseSuccessPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                String dim = player.level().dimension().location().toString();
                com.mundodetronos2.throne.ThroneAttack attack = com.mundodetronos2.throne.ThroneAttackManager.getAttackByChargePos(msg.chargePos);
                if (attack != null) {
                    com.mundodetronos2.throne.ThroneAttackManager.defuseAttack(attack.getThroneId(), player);
                } else {
                    com.mundodetronos2.throne.ThroneAttackManager.defuseBlockCharge(dim, msg.chargePos, player);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // --- S2C THRONE ATTACK ALERT PACKET ---
    public static class S2CThroneAttackAlertPacket {
        private final boolean active;
        private final String baseName;
        private final int throneHp;
        private final int throneMaxHp;
        private final String attackerTeamName;
        private final int secondsLeft;

        public S2CThroneAttackAlertPacket(boolean active, String baseName, int throneHp, int throneMaxHp, String attackerTeamName, int secondsLeft) {
            this.active = active;
            this.baseName = baseName;
            this.throneHp = throneHp;
            this.throneMaxHp = throneMaxHp;
            this.attackerTeamName = attackerTeamName;
            this.secondsLeft = secondsLeft;
        }

        public static void encode(S2CThroneAttackAlertPacket msg, FriendlyByteBuf buf) {
            buf.writeBoolean(msg.active);
            buf.writeUtf(msg.baseName);
            buf.writeInt(msg.throneHp);
            buf.writeInt(msg.throneMaxHp);
            buf.writeUtf(msg.attackerTeamName);
            buf.writeInt(msg.secondsLeft);
        }

        public static S2CThroneAttackAlertPacket decode(FriendlyByteBuf buf) {
            return new S2CThroneAttackAlertPacket(
                    buf.readBoolean(),
                    buf.readUtf(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readUtf(),
                    buf.readInt()
            );
        }

        public static void handle(S2CThroneAttackAlertPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.mundodetronos2.client.ClientPacketHandler.handleThroneAttackAlert(
                            msg.active,
                            msg.baseName,
                            msg.throneHp,
                            msg.throneMaxHp,
                            msg.attackerTeamName,
                            msg.secondsLeft
                    );
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // --- C2S CLAIM ROLE ARMOR PACKET ---
    public static class C2SClaimRoleArmorPacket {
        private final int entityId;

        public C2SClaimRoleArmorPacket(int entityId) {
            this.entityId = entityId;
        }

        public static void encode(C2SClaimRoleArmorPacket msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.entityId);
        }

        public static C2SClaimRoleArmorPacket decode(FriendlyByteBuf buf) {
            return new C2SClaimRoleArmorPacket(buf.readInt());
        }

        public static void handle(C2SClaimRoleArmorPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                PlayerRoleData rData = RoleManager.getPlayerRoleData(player.getUUID());
                if (!rData.isHasRole() || rData.getRole() == PlayerRole.NONE) {
                    MessageManager.actionBar(player, "§c⚠ No tienes ningún rol asignado.");
                    return;
                }

                com.mundodetronos2.commands.TronosCommand.darArmaduraDirecto(player, rData.getRole().name());
                MessageManager.actionBar(player, "§a✔ ¡Armadura de rol entregada!");
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static net.minecraft.world.item.ItemStack createGoddessBook() {
        net.minecraft.world.item.ItemStack book = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITTEN_BOOK);
        net.minecraft.nbt.CompoundTag tag = book.getOrCreateTag();
        tag.putString("title", "§6Guía de la Diosa María");
        tag.putString("author", "Diosa María");

        net.minecraft.nbt.ListTag pages = new net.minecraft.nbt.ListTag();
        pages.add(net.minecraft.nbt.StringTag.valueOf("{\"text\":\"§1§l¡Bienvenido, Aspirante!§r\\n\\nEn este libro sagrado encontrarás instrucciones sobre cómo obtener tus §2§lSkill Points§r y los detalles de cada rol.\\n\\n§d¡Lee con atención!\"}"));
        pages.add(net.minecraft.nbt.StringTag.valueOf("{\"text\":\"§1§l¿Cómo obtener puntos?§r\\n\\nObtienes §2§lSkill Points§r cada vez que subes de nivel de rol.\\n\\nSubes de nivel acumulando §eXP de progreso§r. ¡Al acumular suficiente XP, subirás de nivel!\"}"));
        pages.add(net.minecraft.nbt.StringTag.valueOf("{\"text\":\"§1§lGuerrero§r\\n\\n§0Especialista en la defensa activa y combate físico. Sus habilidades le permiten bloquear daño y resistir los asaltos más duros.\\n\\n§c⚔ Defensa Activa\\n§0Costo: 10 SP\"}"));
        pages.add(net.minecraft.nbt.StringTag.valueOf("{\"text\":\"§1§lBerserker§r\\n\\n§0Guerrero guiado por la furia. Aumenta drásticamente su velocidad de ataque y puede ignorar la defensa del enemigo.\\n\\n§c🩸 Golpe de Furia\\n§0Costo: 10 SP\"}"));
        pages.add(net.minecraft.nbt.StringTag.valueOf("{\"text\":\"§1§lMago§r\\n\\n§0Manipulador de las artes arcanas. Lanza proyectiles mágicos y desata tormentas elementales devastadoras.\\n\\n§c🧙 Proyectil Mágico\\n§0Costo: 10 SP\"}"));
        pages.add(net.minecraft.nbt.StringTag.valueOf("{\"text\":\"§1§lArquero§r\\n\\n§0Maestro del combate a distancia. Dispara ráfagas de flechas veloces, tiros múltiples y disparos venenosos.\\n\\n§c🏹 Disparo Preciso\\n§0Costo: 10 SP\"}"));
        pages.add(net.minecraft.nbt.StringTag.valueOf("{\"text\":\"§1§lPaladín§r\\n\\n§0Caballero sagrado que protege a sus aliados con auras, cura heridas leves y consagra el suelo.\\n\\n§c🛡 Escudo Protector\\n§0Costo: 10 SP\"}"));
        pages.add(net.minecraft.nbt.StringTag.valueOf("{\"text\":\"§1§lDracónico§r\\n\\n§0Invocador del fuego del dragón. Su piel resiste las llamas y puede desatar alientos térmicos destructivos.\\n\\n§c🐉 Aliento Dracónico\\n§0Costo: 10 SP\"}"));
        pages.add(net.minecraft.nbt.StringTag.valueOf("{\"text\":\"§1§lClérigo§r\\n\\n§0Sanador supremo del reino. Cura en área, otorga escudos de fe y puede obrar milagros de restauración.\\n\\n§c💚 Cantar de Sanación\\n§0Costo: 10 SP\"}"));
        tag.put("pages", pages);
        return book;
    }

    // --- C2S OPEN RPG INVENTORY PACKET ---
    public static class C2SOpenRPGInventoryPacket {
        public C2SOpenRPGInventoryPacket() {}
        public static void encode(C2SOpenRPGInventoryPacket msg, FriendlyByteBuf buf) {}
        public static C2SOpenRPGInventoryPacket decode(FriendlyByteBuf buf) { return new C2SOpenRPGInventoryPacket(); }
        public static void handle(C2SOpenRPGInventoryPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;
                com.mundodetronos2.progression.PlayerProgressData pData = com.mundodetronos2.progression.ProgressionManager.getProgressData(player.getUUID());
                int tier = pData != null ? pData.getBackpackTier() : 1;
                net.minecraftforge.network.NetworkHooks.openScreen(player, new net.minecraft.world.SimpleMenuProvider(
                    (containerId, playerInventory, p) -> new com.mundodetronos2.gui.RPGInventoryMenu(containerId, playerInventory, tier),
                    Component.literal("Inventario RPG")
                ), buf -> buf.writeInt(tier));
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // --- S2C TOGGLE LIMITES PACKET ---
    public static class S2CToggleLimitesPacket {
        public S2CToggleLimitesPacket() {}
        public static void encode(S2CToggleLimitesPacket msg, FriendlyByteBuf buf) {}
        public static S2CToggleLimitesPacket decode(FriendlyByteBuf buf) { return new S2CToggleLimitesPacket(); }
        public static void handle(S2CToggleLimitesPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.mundodetronos2.client.ClientEvents.showBaseLimits = !com.mundodetronos2.client.ClientEvents.showBaseLimits;
                    com.mundodetronos2.client.ClientPacketHandler.handleShowMessage("Visualización de límites: " + (com.mundodetronos2.client.ClientEvents.showBaseLimits ? "§aACTIVADO" : "§cDESACTIVADO"), false);
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
