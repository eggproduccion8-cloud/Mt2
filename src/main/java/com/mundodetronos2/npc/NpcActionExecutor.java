package com.mundodetronos2.npc;

import com.mundodetronos2.network.MessageManager;
import com.mundodetronos2.network.NetworkManager;
import com.mundodetronos2.progression.ProgressionManager;
import com.mundodetronos2.tutorial.TutorialManager;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class NpcActionExecutor {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, BiConsumer<ServerPlayer, String>> ACTION_REGISTRY = new HashMap<>();

    static {
        registerAction("CLOSE", (player, arg) -> {});
        registerAction("GIVE_XP", (player, arg) -> {
            try {
                int xp = Integer.parseInt(arg.trim());
                ProgressionManager.addXp(player.getUUID(), xp, player);
            } catch (Exception e) {
                ProgressionManager.addXp(player.getUUID(), 100, player);
            }
        });
        registerAction("ADD_CAMPAIGN_LEVEL", (player, arg) -> {
            com.mundodetronos2.realm.RealmData realm = com.mundodetronos2.realm.RealmManager.getPlayerRealm(player.getUUID());
            if (realm != null) {
                TutorialManager.RealmTutorialData tut = TutorialManager.getTutorialData(realm.getId());
                if (tut != null) {
                    TutorialManager.setTeamTutorialLevel(realm, tut.tutorialLevel + 1, player.getServer());
                }
            }
        });
        registerAction("COMPLETE_SAMUEL_INTRO", (player, arg) -> {
            MessageManager.actionBar(player, "§a✔ Hablaste con Samuel. Ve con Karla al Gremio.");
        });
        registerAction("OPEN_GUILD_GUI", (player, arg) -> {
            com.mundodetronos2.realm.RealmData realm = com.mundodetronos2.realm.RealmManager.getPlayerRealm(player.getUUID());
            com.mundodetronos2.throne.ThroneData throne = realm != null && realm.getThroneId() != null ? com.mundodetronos2.throne.ThroneManager.getThroneById(realm.getThroneId()) : null;
            java.util.List<com.mundodetronos2.realm.InviteData> invites = com.mundodetronos2.realm.RealmManager.getPlayerInvites(player.getUUID());
            NetworkManager.sendToPlayer(new NetworkManager.S2COpenMainGuiPacket(realm, throne, invites), player);
        });
        registerAction("COMPLETE_MANUEL", (player, arg) -> TutorialManager.completeManuel(player));
        registerAction("COMPLETE_MANUEL_TUT", (player, arg) -> TutorialManager.completeManuel(player));
        registerAction("DELIVER_WHEAT_LAURA", (player, arg) -> TutorialManager.deliverWheatLaura(player));
        registerAction("CLAIM_OSCAR_ARMOR", (player, arg) -> TutorialManager.claimOscarArmor(player));
        registerAction("START_SAMUEL_GOLEM", (player, arg) -> TutorialManager.startSamuelGolem(player));
        registerAction("START_HERALDO_TRIAL", (player, arg) -> TutorialManager.startHeraldoTrial(player));
        registerAction("START_GUARDIA_EXPLORATION", (player, arg) -> TutorialManager.checkGuardiaExploration(player));
        registerAction("COMPLETE_SACERDOTE_TUT", (player, arg) -> TutorialManager.completeSacerdoteTutorial(player));
        registerAction("START_ARENA_TRIAL", (player, arg) -> TutorialManager.startArenaTrial(player));
        registerAction("START_FINAL_SIEGE_TRIAL", (player, arg) -> TutorialManager.startFinalSiegeTrial(player));
        registerAction("CLAIM_LEADER_THRONE", (player, arg) -> TutorialManager.claimLeaderThrone(player));
        registerAction("OPEN_ROLE_SELECTION", (player, arg) -> NetworkManager.sendToPlayer(new NetworkManager.S2COpenRoleSelectionPacket(), player));
        registerAction("OPEN_CREATE_TEAM_GUI", (player, arg) -> {
            com.mundodetronos2.realm.RealmData realm = com.mundodetronos2.realm.RealmManager.getPlayerRealm(player.getUUID());
            com.mundodetronos2.throne.ThroneData throne = realm != null && realm.getThroneId() != null ? com.mundodetronos2.throne.ThroneManager.getThroneById(realm.getThroneId()) : null;
            java.util.List<com.mundodetronos2.realm.InviteData> invites = com.mundodetronos2.realm.RealmManager.getPlayerInvites(player.getUUID());
            com.mundodetronos2.role.PlayerRoleData rData = com.mundodetronos2.role.RoleManager.getPlayerRoleData(player.getUUID());
            NetworkManager.sendToPlayer(new NetworkManager.S2COpenMainGuiPacket(realm, throne, invites), player);
        });
        registerAction("OPEN_GUIDE", (player, arg) -> {
            player.getInventory().add(NetworkManager.createGoddessBook());
            MessageManager.actionBar(player, "§a✔ Has obtenido la Guía.");
        });
    }

    public static void registerAction(String actionName, BiConsumer<ServerPlayer, String> executor) {
        ACTION_REGISTRY.put(actionName.toUpperCase(), executor);
    }

    public static boolean execute(ServerPlayer player, String actionStr) {
        if (actionStr == null || actionStr.trim().isEmpty()) return false;
        String[] parts = actionStr.split(":", 2);
        String name = parts[0].trim().toUpperCase();
        String arg = parts.length > 1 ? parts[1].trim() : "";

        BiConsumer<ServerPlayer, String> consumer = ACTION_REGISTRY.get(name);
        if (consumer != null) {
            consumer.accept(player, arg);
            return true;
        } else {
            LOGGER.warn("Acción de NPC no registrada: {}", actionStr);
            return false;
        }
    }
}
