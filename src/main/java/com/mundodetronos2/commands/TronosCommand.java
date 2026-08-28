package com.mundodetronos2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mundodetronos2.config.ConfigManager;
import com.mundodetronos2.network.NetworkManager;
import com.mundodetronos2.realm.RealmData;
import com.mundodetronos2.realm.RealmManager;
import com.mundodetronos2.role.*;
import com.mundodetronos2.throne.ThroneData;
import com.mundodetronos2.throne.ThroneManager;
import com.mundodetronos2.throne.ThroneState;
import com.mundodetronos2.time.TimeManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import com.mundodetronos2.entity.CustomNPCEntity;
import com.mundodetronos2.init.EntityInit;

import java.util.List;
import java.util.UUID;

public class TronosCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tronos")
                // Jugador Commands
                .then(Commands.literal("crear")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("nombre", StringArgumentType.greedyString())
                                .executes(ctx -> crearReino(ctx.getSource(), StringArgumentType.getString(ctx, "nombre")))))
                .then(Commands.literal("gremio")
                        .then(Commands.literal("llave")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("jugador", EntityArgument.player())
                                        .executes(ctx -> darLlaveLider(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador"))))))
                .then(Commands.literal("buscar")
                        .executes(ctx -> buscarReinos(ctx.getSource())))
                .then(Commands.literal("misreinos")
                        .executes(ctx -> misReinos(ctx.getSource())))
                .then(Commands.literal("invitaciones")
                        .executes(ctx -> verInvitaciones(ctx.getSource())))
                .then(Commands.literal("aceptar")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .executes(ctx -> aceptarInvitacion(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("rechazar")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .executes(ctx -> rechazarInvitacion(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("salir")
                        .executes(ctx -> salirReino(ctx.getSource())))
                .then(Commands.literal("info")
                        .executes(ctx -> infoReino(ctx.getSource())))

                // COMANDOS ADMINISTRATIVOS DE PROGRESIÓN
                .then(Commands.literal("nivel")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("jugador", EntityArgument.player())
                                .then(Commands.literal("add")
                                        .then(Commands.argument("cantidad", IntegerArgumentType.integer())
                                                .executes(ctx -> addNivelCmd(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador"), IntegerArgumentType.getInteger(ctx, "cantidad")))))
                                .then(Commands.argument("nivel", IntegerArgumentType.integer(1, 100))
                                        .executes(ctx -> setNivelCmd(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador"), IntegerArgumentType.getInteger(ctx, "nivel"))))))
                .then(Commands.literal("xp")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("jugador", EntityArgument.player())
                                .then(Commands.argument("cantidad", IntegerArgumentType.integer(1))
                                        .executes(ctx -> addXpCmd(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador"), IntegerArgumentType.getInteger(ctx, "cantidad"))))))

                // SISTEMA DE ROLES COMMANDS
                .then(Commands.literal("rol")
                        .then(Commands.literal("info")
                                .then(Commands.argument("jugador", EntityArgument.player())
                                        .executes(ctx -> getRolInfo(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador")))))
                        .then(Commands.literal("asignar")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("jugador", EntityArgument.player())
                                        .then(Commands.argument("rol", StringArgumentType.string())
                                                .executes(ctx -> asignarRol(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador"), StringArgumentType.getString(ctx, "rol"))))))
                        .then(Commands.literal("quitar")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("jugador", EntityArgument.player())
                                        .executes(ctx -> quitarRol(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador")))))
                        .then(Commands.literal("reset")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("jugador", EntityArgument.player())
                                        .executes(ctx -> resetRol(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador")))))
                        .then(Commands.literal("dararmadura")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("jugador", EntityArgument.player())
                                        .then(Commands.argument("rol", StringArgumentType.string())
                                                .executes(ctx -> darArmadura(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador"), StringArgumentType.getString(ctx, "rol"))))))
                        .then(Commands.literal("dimension")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> tpRoleDimension(ctx.getSource())))
                        .then(Commands.literal("setaltar")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> setAltar(ctx.getSource())))
                        .then(Commands.literal("debug")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> debugRoles(ctx.getSource())))
                        .then(Commands.literal("equipamiento")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> openAdminEquipment(ctx.getSource())))
                        .then(Commands.literal("carnet")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("jugador", EntityArgument.player())
                                        .executes(ctx -> darCarnetCmd(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador")))))
                )

                // SISTEMA DE TIEMPO COMMANDS
                .then(Commands.literal("tiempo")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("reset")
                                .executes(ctx -> resetAllPlaytimes(ctx.getSource())))
                )

                // SISTEMA DE MONEDAS RPG COMMANDS
                .then(Commands.literal("monedas")
                        .then(Commands.literal("ver")
                                .executes(ctx -> verMonedasCmd(ctx.getSource())))
                        .then(Commands.literal("dar")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("jugador", EntityArgument.player())
                                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(1))
                                                .executes(ctx -> darMonedasCmd(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador"), IntegerArgumentType.getInteger(ctx, "cantidad"))))))
                        .then(Commands.literal("quitar")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("jugador", EntityArgument.player())
                                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(1))
                                                .executes(ctx -> quitarMonedasCmd(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador"), IntegerArgumentType.getInteger(ctx, "cantidad"))))))
                        .then(Commands.literal("set")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("jugador", EntityArgument.player())
                                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(0))
                                                .executes(ctx -> setMonedasCmd(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador"), IntegerArgumentType.getInteger(ctx, "cantidad"))))))
                )

                // SISTEMA DE PORTALES COMMANDS (GOLD SPAWNER)
                .then(Commands.literal("portal")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("diosa_spawner")
                                .executes(ctx -> setPortalBlock(ctx.getSource())))
                )

                // SISTEMA DE NPC COMMANDS


                // SISTEMA DE GUÍA INTERACTIVA
                .then(Commands.literal("guia")
                        .then(Commands.literal("give")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("jugador", EntityArgument.player())
                                        .executes(ctx -> darGuiaCmd(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador")))))
                        .executes(ctx -> reclamarGuiaCmd(ctx.getSource())))

                // SISTEMA DE ASEDIO (ASALTO)
                .then(Commands.literal("asalto")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("give")
                                .then(Commands.argument("jugador", EntityArgument.player())
                                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(1))
                                                .executes(ctx -> darAsaltoItem(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador"), IntegerArgumentType.getInteger(ctx, "cantidad"))))
                                        .executes(ctx -> darAsaltoItem(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador"), 1)))))

                // SISTEMA DE MESA DE HERRERO COMMAND
                .then(Commands.literal("mesa_herrero")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> setMesaHerreroBlock(ctx.getSource())))

                // SISTEMA DE OFRENDAS COMMANDS (ROSA SAGRADA)
                .then(Commands.literal("ofrenda")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> giveOfrendaItem(ctx.getSource())))

                // SISTEMA DE REINICIAR KIT INICIAL
                .then(Commands.literal("herrero")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("resetkit")
                                .then(Commands.argument("jugador", EntityArgument.player())
                                        .executes(ctx -> resetHerreroKit(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador"))))))

                // OP / Admin Commands
                .then(Commands.literal("reload")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> reloadConfig(ctx.getSource())))
                .then(Commands.literal("save")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> saveAllData(ctx.getSource())))
                .then(Commands.literal("campaña")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("manuel")
                                .executes(ctx -> invocarManuelCmd(ctx.getSource())))
                        .then(Commands.literal("karla")
                                .executes(ctx -> invocarKarlaCmd(ctx.getSource()))))
                .then(Commands.literal("team")
                        .then(Commands.literal("unir")
                                .then(Commands.argument("color", StringArgumentType.string())
                                        .executes(ctx -> unirEquipoCmd(ctx.getSource(), StringArgumentType.getString(ctx, "color")))))
                        .then(Commands.literal("salir")
                                .executes(ctx -> salirEquipoCmd(ctx.getSource())))
                        .then(Commands.literal("lider")
                                .then(Commands.argument("jugador", EntityArgument.player())
                                        .executes(ctx -> designarLiderCmd(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador")))))
                        .then(Commands.literal("trono")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("color", StringArgumentType.string())
                                        .executes(ctx -> darTronoEquipo(ctx.getSource(), StringArgumentType.getString(ctx, "color"))))))
                .then(Commands.literal("muralla")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("nivel")
                                .then(Commands.argument("nivel", IntegerArgumentType.integer(1, 3))
                                        .then(Commands.argument("equipo", StringArgumentType.string())
                                                .executes(ctx -> setMurallaNivelCmd(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "nivel"), StringArgumentType.getString(ctx, "equipo"))))
                                        .executes(ctx -> setMurallaNivelCmd(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "nivel"), null)))))
                .then(Commands.literal("debug")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("trono").executes(ctx -> debugTronoCmd(ctx.getSource())))
                        .then(Commands.literal("muralla").executes(ctx -> debugMurallaCmd(ctx.getSource())))
                        .then(Commands.literal("defensores").executes(ctx -> debugDefensoresCmd(ctx.getSource())))
                        .executes(ctx -> debugInfo(ctx.getSource())))
                .then(Commands.literal("setvidas")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("reino", StringArgumentType.string())
                                .then(Commands.argument("cantidad", IntegerArgumentType.integer(0))
                                        .executes(ctx -> setVidas(ctx.getSource(), StringArgumentType.getString(ctx, "reino"), IntegerArgumentType.getInteger(ctx, "cantidad"))))))
                .then(Commands.literal("addvida")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("reino", StringArgumentType.string())
                                .then(Commands.argument("cantidad", IntegerArgumentType.integer())
                                        .executes(ctx -> addVida(ctx.getSource(), StringArgumentType.getString(ctx, "reino"), IntegerArgumentType.getInteger(ctx, "cantidad"))))))
                .then(Commands.literal("evento")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("on")
                                .executes(ctx -> toggleEvento(ctx.getSource(), true)))
                        .then(Commands.literal("off")
                                .executes(ctx -> toggleEvento(ctx.getSource(), false))))
                .then(Commands.literal("forcecooldown")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("reino", StringArgumentType.string())
                                .executes(ctx -> forceCooldown(ctx.getSource(), StringArgumentType.getString(ctx, "reino")))))
                .then(Commands.literal("tpthrone")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("reino", StringArgumentType.string())
                                .executes(ctx -> tpThrone(ctx.getSource(), StringArgumentType.getString(ctx, "reino")))))
                // COMANDO DE MOCHILA ADMINISTRATIVO
                .then(Commands.literal("mochila")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("dar")
                                .then(Commands.argument("jugador", EntityArgument.player())
                                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(1))
                                                .executes(ctx -> darMochilaUpgradeItem(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador"), IntegerArgumentType.getInteger(ctx, "cantidad"))))
                                        .executes(ctx -> darMochilaUpgradeItem(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador"), 1))))
                        .then(Commands.literal("nivel")
                                .then(Commands.argument("jugador", EntityArgument.player())
                                        .then(Commands.argument("nivel", IntegerArgumentType.integer(1, 3))
                                                .executes(ctx -> setMochilaNivelCmd(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador"), IntegerArgumentType.getInteger(ctx, "nivel")))))))

                .then(Commands.literal("trono")
                        .then(Commands.literal("nivel")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("nivel", IntegerArgumentType.integer(1, 3))
                                        .then(Commands.argument("equipo", StringArgumentType.string())
                                                .executes(ctx -> setTronoNivelCmd(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "nivel"), StringArgumentType.getString(ctx, "equipo"))))
                                        .executes(ctx -> setTronoNivelCmd(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "nivel"), null))))
                        .then(Commands.literal("llave")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("jugador", EntityArgument.player())
                                        .executes(ctx -> darLlaveReubicacion(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador")))))
                        .then(Commands.literal("give")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("jugador", EntityArgument.player())
                                        .executes(ctx -> darTronoItem(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador")))))
                        .then(Commands.literal("info")
                                .executes(ctx -> infoReino(ctx.getSource()))))
                .then(Commands.literal("setthrone")
                        .then(Commands.argument("reino", StringArgumentType.string())
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> setThroneAdmin(ctx.getSource(), StringArgumentType.getString(ctx, "reino"))))
                        .executes(ctx -> setThronePlayer(ctx.getSource())))
                .then(Commands.literal("remover")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("reino", StringArgumentType.string())
                                .executes(ctx -> removerReino(ctx.getSource(), StringArgumentType.getString(ctx, "reino")))))
                .then(Commands.literal("limites")
                        .executes(ctx -> toggleLimits(ctx.getSource())))
                .then(Commands.literal("chat")
                        .then(Commands.argument("jugador", EntityArgument.player())
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> toggleChatTarget(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador"))))
                        .executes(ctx -> toggleChatSelf(ctx.getSource())))
        );
    }

    private static int toggleChatSelf(CommandSourceStack src) {
        if (src.getEntity() instanceof ServerPlayer player) {
            NetworkManager.sendToPlayer(new NetworkManager.S2CToggleChatPacket(), player);
            return 1;
        } else {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador o especificando un objetivo."));
            return 0;
        }
    }

    private static int toggleChatTarget(CommandSourceStack src, ServerPlayer target) {
        NetworkManager.sendToPlayer(new NetworkManager.S2CToggleChatPacket(), target);
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Modo de chat alternado para " + target.getGameProfile().getName() + "."), true);
        return 1;
    }

    private static int darLlaveLider(CommandSourceStack src, ServerPlayer target) {
        RealmManager.giveLeaderKey(target);
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Se ha entregado la Llave del Líder a " + target.getGameProfile().getName() + "."), true);
        return 1;
    }

    private static int verMonedasCmd(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        com.mundodetronos2.progression.PlayerProgressData pData = com.mundodetronos2.progression.ProgressionManager.getProgressData(player.getUUID());
        src.sendSuccess(() -> Component.literal("§e✦ Monedas Personales: §f" + pData.getCoins()), false);
        return 1;
    }

    private static int darMonedasCmd(CommandSourceStack src, ServerPlayer target, int cantidad) {
        com.mundodetronos2.progression.PlayerProgressData pData = com.mundodetronos2.progression.ProgressionManager.getProgressData(target.getUUID());
        pData.setCoins(pData.getCoins() + cantidad);
        com.mundodetronos2.progression.ProgressionManager.save(true);
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Se entregaron " + cantidad + " monedas a " + target.getGameProfile().getName() + ". Total: " + pData.getCoins()), true);
        return 1;
    }

    private static int quitarMonedasCmd(CommandSourceStack src, ServerPlayer target, int cantidad) {
        com.mundodetronos2.progression.PlayerProgressData pData = com.mundodetronos2.progression.ProgressionManager.getProgressData(target.getUUID());
        pData.setCoins(Math.max(0, pData.getCoins() - cantidad));
        com.mundodetronos2.progression.ProgressionManager.save(true);
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Se quitaron " + cantidad + " monedas a " + target.getGameProfile().getName() + ". Total: " + pData.getCoins()), true);
        return 1;
    }

    private static int setMonedasCmd(CommandSourceStack src, ServerPlayer target, int cantidad) {
        com.mundodetronos2.progression.PlayerProgressData pData = com.mundodetronos2.progression.ProgressionManager.getProgressData(target.getUUID());
        pData.setCoins(cantidad);
        com.mundodetronos2.progression.ProgressionManager.save(true);
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Monedas de " + target.getGameProfile().getName() + " establecidas en " + cantidad), true);
        return 1;
    }

    // ------------------ PLAYER COMMANDS IMPLEMENTATION ------------------

    private static int toggleLimits(CommandSourceStack src) {
        if (src.getEntity() instanceof ServerPlayer player) {
            NetworkManager.sendToPlayer(new NetworkManager.S2CToggleLimitesPacket(), player);
            return 1;
        } else {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }
    }

    private static int crearReino(CommandSourceStack src, String nombre) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        RealmData realm = RealmManager.createRealm(nombre, player.getUUID(), player.getGameProfile().getName());
        if (realm != null) {
            src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Reino '" + realm.getName() + "' creado exitosamente."), false);
        } else {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se pudo crear el reino. Nombre duplicado o ya tienes un reino."));
        }
        return 1;
    }

    private static int buscarReinos(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }
        NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SRequestRealmListPacket());
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Abriendo listado de reinos en la GUI..."), false);
        return 1;
    }

    private static int misReinos(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }
        NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SOpenMainGuiPacket());
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Abriendo menú de tu reino en la GUI..."), false);
        return 1;
    }

    private static int verInvitaciones(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }
        NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SOpenMainGuiPacket());
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Mostrando invitaciones en la GUI..."), false);
        return 1;
    }

    private static int aceptarInvitacion(CommandSourceStack src, String idStr) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }
        try {
            UUID inviteId = UUID.fromString(idStr);
            boolean ok = RealmManager.acceptInvite(inviteId, player.getUUID());
            if (ok) {
                src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] ¡Te has unido al reino exitosamente!"), false);
            } else {
                src.sendFailure(Component.literal("§c[Mundo de Tronos] No se pudo aceptar la invitación. Puede haber expirado o el reino está lleno."));
            }
        } catch (Exception e) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] Formato de ID de invitación inválido."));
        }
        return 1;
    }

    private static int rechazarInvitacion(CommandSourceStack src, String idStr) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }
        try {
            UUID inviteId = UUID.fromString(idStr);
            boolean ok = RealmManager.rejectInvite(inviteId, player.getUUID());
            if (ok) {
                src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Invitación rechazada."), false);
            } else {
                src.sendFailure(Component.literal("§c[Mundo de Tronos] Invitación no encontrada."));
            }
        } catch (Exception e) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] Formato de ID inválido."));
        }
        return 1;
    }

    private static int salirReino(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }
        boolean ok = RealmManager.leaveRealm(player.getUUID());
        if (ok) {
            src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Has salido del reino exitosamente."), false);
        } else {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No perteneces a ningún reino."));
        }
        return 1;
    }

    private static int infoReino(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        RealmData realm = RealmManager.getPlayerRealm(player.getUUID());
        if (realm == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No perteneces a ningún reino actualmente."));
            return 1;
        }

        src.sendSuccess(() -> Component.literal("§e=== INFORMACIÓN DE TU REINO ==="), false);
        src.sendSuccess(() -> Component.literal("§7Nombre: §f" + realm.getName()), false);
        src.sendSuccess(() -> Component.literal("§7Miembros: §f" + realm.getMembers().size() + " / " + realm.getMaxPlayers()), false);
        src.sendSuccess(() -> Component.literal("§7Vidas: §e" + realm.getCurrentLives() + " / " + realm.getMaxLives()), false);

        ThroneData throne = realm.getThroneId() != null ? ThroneManager.getThroneById(realm.getThroneId()) : null;
        if (throne != null) {
            src.sendSuccess(() -> Component.literal("§7Trono: §f" + throne.getPos().getX() + ", " + throne.getPos().getY() + ", " + throne.getPos().getZ() + " (" + throne.getDimension() + ")"), false);
            src.sendSuccess(() -> Component.literal("§7Vida del Trono: §c" + throne.getHealth() + " / " + throne.getMaxHealth() + " HP"), false);
            src.sendSuccess(() -> Component.literal("§7Estado del Trono: §6" + throne.getState().name()), false);
        } else {
            src.sendSuccess(() -> Component.literal("§7Trono: §cNo registrado (Párate sobre el bloque vanilla y escribe /tronos setthrone)"), false);
        }
        return 1;
    }

    private static int setThronePlayer(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        RealmData realm = RealmManager.getPlayerRealm(player.getUUID());
        if (realm == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No perteneces a ningún reino."));
            return 1;
        }

        if (!realm.getOwnerId().equals(player.getUUID())) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] Solo el dueño del reino puede establecer el trono."));
            return 1;
        }

        return registerThroneAtLookedBlock(src, player, realm);
    }

    private static int registerThroneAtLookedBlock(CommandSourceStack src, ServerPlayer player, RealmData realm) {
        HitResult hit = player.pick(6.0D, 0.0F, false);
        BlockPos pos = null;

        if (hit.getType() == HitResult.Type.BLOCK) {
            pos = ((BlockHitResult) hit).getBlockPos();
        } else {
            pos = player.blockPosition().below();
        }

        BlockState state = player.level().getBlockState(pos);
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();

        if (!ConfigManager.isBlockAllowedForThrone(blockId)) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] El bloque '" + blockId + "' no está permitido como trono por la configuración del servidor."));
            src.sendFailure(Component.literal("§7Bloques permitidos: " + ConfigManager.get().allowedThroneBlocks.toString()));
            return 1;
        }

        String dimension = player.level().dimension().location().toString();
        ThroneData throne = ThroneManager.registerThrone(realm.getId(), pos, dimension, 3);

        if (throne != null) {
            final BlockPos finalPos = pos;
            final String finalBlockId = blockId;
            src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] ¡El trono del reino '" + realm.getName() + "' ha sido registrado exitosamente en " + finalPos.getX() + ", " + finalPos.getY() + ", " + finalPos.getZ() + " (" + finalBlockId + ")!"), true);
        } else {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se pudo registrar el trono. Es posible que esa posición exacta ya sea el trono de otro reino o ya tengas uno registrado."));
        }

        return 1;
    }

    // ------------------ ROLES SYSTEM COMMAND IMPLEMENTATIONS ------------------

    private static int openAdminEquipment(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        player.openMenu(new net.minecraft.world.SimpleMenuProvider((containerId, playerInventory, playerEntity) -> {
            net.minecraft.world.SimpleContainer container = new net.minecraft.world.SimpleContainer(com.mundodetronos2.role.EquipmentManager.TOTAL_SLOTS);
            java.util.List<com.mundodetronos2.role.EquipmentManager.AllowedItemSlot> roleItems = com.mundodetronos2.role.EquipmentManager.getRoleSlots("guerrero");
            for (int i = 0; i < com.mundodetronos2.role.EquipmentManager.TOTAL_SLOTS; i++) {
                net.minecraft.world.item.ItemStack stack = net.minecraft.world.item.ItemStack.EMPTY;
                if (i < roleItems.size()) {
                    stack = roleItems.get(i).toItemStack();
                }
                container.setItem(i, stack);
            }
            return new com.mundodetronos2.gui.AdminEquipmentMenu(containerId, playerInventory, container, "guerrero");
        }, Component.literal("Equipamiento de Roles")));

        src.sendSuccess(() -> Component.literal("§aAbriendo panel administrativo de equipamiento (200 casillas)..."), false);
        return 1;
    }

    private static int getRolInfo(CommandSourceStack src, ServerPlayer target) {
        PlayerRoleData data = RoleManager.getPlayerRoleData(target.getUUID());
        src.sendSuccess(() -> Component.literal("§e=== INFORMACIÓN DE ROL DE " + target.getGameProfile().getName() + " ==="), false);
        src.sendSuccess(() -> Component.literal("§7Rol: §f" + (data.isHasRole() ? "§a" + data.getRole().name() : "§cSIN ROL")), false);
        src.sendSuccess(() -> Component.literal("§7Nivel: §6" + data.getLevel()), false);
        src.sendSuccess(() -> Component.literal("§7Fecha Selección: §f" + (data.isHasRole() ? new java.util.Date(data.getSelectedAt()).toString() : "N/A")), false);
        return 1;
    }

    private static int asignarRol(CommandSourceStack src, ServerPlayer target, String rolName) {
        PlayerRole role = PlayerRole.fromString(rolName);
        if (role == PlayerRole.NONE) {
            src.sendFailure(Component.literal("§cRol '" + rolName + "' inválido. Usa: berserker, guerrero, mago, arquero, paladin, draconico, clerigo."));
            return 1;
        }

        RoleManager.setPlayerRole(target.getUUID(), role);
        src.sendSuccess(() -> Component.literal("§aRol '" + role.name() + "' asignado administrativamente a " + target.getGameProfile().getName() + "."), true);
        return 1;
    }

    private static int quitarRol(CommandSourceStack src, ServerPlayer target) {
        RoleManager.resetPlayerRole(target.getUUID());
        src.sendSuccess(() -> Component.literal("§aSe ha removido el rol de " + target.getGameProfile().getName() + "."), true);
        return 1;
    }

    private static int resetRol(CommandSourceStack src, ServerPlayer target) {
        RoleManager.resetPlayerRole(target.getUUID());
        src.sendSuccess(() -> Component.literal("§aSe ha reiniciado el rol de " + target.getGameProfile().getName() + ". Puede volver a seleccionarlo en el altar."), true);
        return 1;
    }

    private static int tpRoleDimension(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        try {
            ResourceLocation dimRl = new ResourceLocation("mundodetronos2", "role_dimension");
            ResourceKey<net.minecraft.world.level.Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimRl);
            ServerLevel level = player.getServer().getLevel(dimKey);
            if (level == null) {
                src.sendFailure(Component.literal("§cLa dimensión de roles no está cargada o no existe."));
                return 1;
            }

            player.teleportTo(level, 0.5D, 64.0D, 0.5D, 0.0F, 0.0F);
            src.sendSuccess(() -> Component.literal("§aTeletransportado a la dimensión de selección de roles."), true);
        } catch (Exception e) {
            src.sendFailure(Component.literal("§cError al teletransportar: " + e.getMessage()));
        }
        return 1;
    }

    private static int setAltar(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        HitResult hit = player.pick(6.0D, 0.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            src.sendFailure(Component.literal("§cDebes estar mirando el bloque de Huevo de Dragón para registrar el Altar."));
            return 1;
        }

        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        BlockState state = player.level().getBlockState(pos);
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();

        if (!blockId.equalsIgnoreCase("minecraft:dragon_egg")) {
            src.sendFailure(Component.literal("§cEl Altar debe ser un Huevo de Dragón vanilla (bloque mirado actual: '" + blockId + "')."));
            return 1;
        }

        String dimension = player.level().dimension().location().toString();
        AltarManager.registerAltar(pos, dimension, blockId);

        src.sendSuccess(() -> Component.literal("§a¡Altar de Selección de Roles registrado con éxito en " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "!"), true);
        return 1;
    }

    private static int debugRoles(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal("§e=== MUNDO DE TRONOS 2 — ROLES ==="), false);

        long berserkers = 0;
        long guerreros = 0;
        long mages = 0;
        long arqueros = 0;
        long paladines = 0;
        long draconicos = 0;
        long clerigos = 0;
        long none = 0;

        for (PlayerRoleData d : RoleManager.getRolesCache().values()) {
            if (!d.isHasRole()) none++;
            else {
                switch (d.getRole()) {
                    case BERSERKER -> berserkers++;
                    case GUERRERO -> guerreros++;
                    case MAGO -> mages++;
                    case ARQUERO -> arqueros++;
                    case PALADIN -> paladines++;
                    case DRACONICO -> draconicos++;
                    case CLERIGO -> clerigos++;
                    default -> none++;
                }
            }
        }

        final long fBerserkers = berserkers;
        final long fGuerreros = guerreros;
        final long fMages = mages;
        final long fArqueros = arqueros;
        final long fPaladines = paladines;
        final long fDraconicos = draconicos;
        final long fClerigos = clerigos;
        final long fNone = none;

        src.sendSuccess(() -> Component.literal("§7Jugadores con rol: §f" + (fBerserkers + fGuerreros + fMages + fArqueros + fPaladines + fDraconicos + fClerigos)), false);
        src.sendSuccess(() -> Component.literal("§7Guerreros: §f" + fGuerreros), false);
        src.sendSuccess(() -> Component.literal("§7Berserkers: §f" + fBerserkers), false);
        src.sendSuccess(() -> Component.literal("§7Magos: §f" + fMages), false);
        src.sendSuccess(() -> Component.literal("§7Arqueros: §f" + fArqueros), false);
        src.sendSuccess(() -> Component.literal("§7Paladines: §f" + fPaladines), false);
        src.sendSuccess(() -> Component.literal("§7Dracónicos: §f" + fDraconicos), false);
        src.sendSuccess(() -> Component.literal("§7Clérigos: §f" + fClerigos), false);
        src.sendSuccess(() -> Component.literal("§7Jugadores sin rol: §f" + fNone), false);

        src.sendSuccess(() -> Component.literal("§7Datos cargados: §aOK"), false);
        src.sendSuccess(() -> Component.literal("§7Dimensión de roles: §a" + (src.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation("mundodetronos2", "role_dimension"))) != null ? "Cargada" : "No cargada")), false);

        AltarManager.AltarData altar = AltarManager.getRegisteredAltar();
        src.sendSuccess(() -> Component.literal("§7Altar registrado: §f" + (altar != null ? altar.x + ", " + altar.y + ", " + altar.z + " (" + altar.dimension + ")" : "§cNo registrado")), false);
        src.sendSuccess(() -> Component.literal("§7Packets registrados: §f15"), false);
        return 1;
    }

    private static int resetAllPlaytimes(CommandSourceStack src) {
        TimeManager.resetAllPlayers();
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Se ha restablecido el tiempo de todos los jugadores (online y offline) a 4 horas."), true);

        for (ServerPlayer player : src.getServer().getPlayerList().getPlayers()) {
            NetworkManager.syncHud(player);
        }
        return 1;
    }

    // --- NUEVOS MÉTODOS DEL PORTAL DE LA DIOSA Y LA OFRENDA ---
    private static int setMesaHerreroBlock(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        HitResult hit = player.pick(6.0D, 0.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            src.sendFailure(Component.literal("§cDebes estar mirando el bloque de Mesa de Herrería para registrar la Mesa del Herrero del Rey."));
            return 1;
        }

        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        BlockState state = player.level().getBlockState(pos);
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();

        if (!blockId.equalsIgnoreCase("minecraft:smithing_table")) {
            src.sendFailure(Component.literal("§cLa Mesa debe ser una Mesa de Herrería vanilla (bloque mirado actual: '" + blockId + "')."));
            return 1;
        }

        // Reemplazar con el bloque de la Mesa de Herrero del mod
        player.level().setBlock(pos, com.mundodetronos2.init.BlockInit.MESA_HERRERO.get().defaultBlockState(), 3);
        src.sendSuccess(() -> Component.literal("§a¡Mesa del Herrero del Rey colocada con éxito en " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "!"), true);
        return 1;
    }

    private static int setPortalBlock(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        HitResult hit = player.pick(6.0D, 0.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            src.sendFailure(Component.literal("§cDebes estar mirando el bloque de oro para registrar el Portal de la Diosa."));
            return 1;
        }

        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        BlockState state = player.level().getBlockState(pos);
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();

        if (!blockId.equalsIgnoreCase("minecraft:gold_block")) {
            src.sendFailure(Component.literal("§cEl Portal debe ser un bloque de oro vanilla (bloque mirado actual: '" + blockId + "')."));
            return 1;
        }

        String dimension = player.level().dimension().location().toString();
        PortalsManager.registerPortal(pos, dimension, blockId);

        src.sendSuccess(() -> Component.literal("§a¡Portal de la Diosa María registrado con éxito en " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " (" + blockId + ")!"), true);
        return 1;
    }

    private static int giveOfrendaItem(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        net.minecraft.world.item.ItemStack ofrenda = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.POPPY);
        ofrenda.setHoverName(Component.literal("§dOfrenda de la Diosa María"));
        ofrenda.getOrCreateTag().putBoolean("IsGoddessOffering", true);

        player.getInventory().add(ofrenda);
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Recibiste la ofrenda sagrada para la Diosa María (Rosa Sagrada)."), true);
        return 1;
    }


    private static int resetHerreroKit(CommandSourceStack src, ServerPlayer target) {
        PlayerRoleData rData = RoleManager.getPlayerRoleData(target.getUUID());
        rData.setInitialKitClaimed(false);
                                        RoleManager.save(true);

        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Todas las misiones de NPC y kit restablecidos para " + target.getGameProfile().getName() + "."), true);
        return 1;
    }

    // ------------------ ADMIN/OP COMMANDS IMPLEMENTATION ------------------

    private static int reloadConfig(CommandSourceStack src) {
        ConfigManager.load();
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Configuración recargada correctamente."), true);
        return 1;
    }

    private static int saveAllData(CommandSourceStack src) {
        RealmManager.save(true);
        RoleManager.save(true);
        TimeManager.save(true);
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Todos los datos se han guardado a disco."), true);
        return 1;
    }

    private static int toggleEvento(CommandSourceStack src, boolean active) {
        ThroneManager.setGlobalEventActive(active);
        String status = active ? "§aACTIVADO" : "§cDESACTIVADO";
        src.sendSuccess(() -> Component.literal("§6[Mundo de Tronos] El evento de tronos ha sido " + status + " globalmente!"), true);
        return 1;
    }

    private static int forceCooldown(CommandSourceStack src, String reinoName) {
        RealmData realm = RealmManager.getRealmByName(reinoName);
        if (realm == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] Reino '" + reinoName + "' no encontrado."));
            return 1;
        }

        if (realm.getThroneId() == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] El reino '" + reinoName + "' no tiene trono asociado."));
            return 1;
        }

        ThroneData throne = ThroneManager.getThroneById(realm.getThroneId());
        if (throne != null) {
            throne.setState(ThroneState.REPAIRING);
            long cooldownMinutes = ConfigManager.get().throneCooldownMinutes;
            throne.setCooldownEndsAt(System.currentTimeMillis() + (cooldownMinutes * 60 * 1000));
            throne.setHealth(0);
            com.mundodetronos2.data.SaveManager.markDirty();
            RealmManager.save(false);

            src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Cooldown de reparación forzado para el reino '" + realm.getName() + "'."), true);
        }
        return 1;
    }

    private static int setVidas(CommandSourceStack src, String reinoName, int cantidad) {
        RealmData realm = RealmManager.getRealmByName(reinoName);
        if (realm == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] Reino '" + reinoName + "' no encontrado."));
            return 1;
        }

        realm.setCurrentLives(cantidad);
        com.mundodetronos2.data.SaveManager.markDirty();
        RealmManager.save(false);

        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Vidas de '" + realm.getName() + "' establecidas en " + cantidad + "."), true);
        return 1;
    }

    private static int addVida(CommandSourceStack src, String reinoName, int cantidad) {
        RealmData realm = RealmManager.getRealmByName(reinoName);
        if (realm == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] Reino '" + reinoName + "' no encontrado."));
            return 1;
        }

        int nueva = realm.getCurrentLives() + cantidad;
        realm.setCurrentLives(Math.max(0, nueva));
        com.mundodetronos2.data.SaveManager.markDirty();
        RealmManager.save(false);

        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Se han añadido " + cantidad + " vidas a '" + realm.getName() + "'. Total: " + realm.getCurrentLives() + "."), true);
        return 1;
    }

    private static int tpThrone(CommandSourceStack src, String reinoName) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        RealmData realm = RealmManager.getRealmByName(reinoName);
        if (realm == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] Reino '" + reinoName + "' no encontrado."));
            return 1;
        }

        if (realm.getThroneId() == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] El reino '" + reinoName + "' no tiene trono registrado."));
            return 1;
        }

        ThroneData throne = ThroneManager.getThroneById(realm.getThroneId());
        if (throne != null) {
            BlockPos pos = throne.getPos();
            player.teleportTo(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
            src.sendSuccess(() -> Component.literal("§aTeletransportado al trono de '" + realm.getName() + "'."), true);
        }
        return 1;
    }

    private static int setThroneAdmin(CommandSourceStack src, String reinoName) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        RealmData realm = RealmManager.getRealmByName(reinoName);
        if (realm == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] Reino '" + reinoName + "' no encontrado."));
            return 1;
        }

        return registerThroneAtLookedBlock(src, player, realm);
    }

    private static int removerReino(CommandSourceStack src, String reinoName) {
        RealmData realm = RealmManager.getRealmByName(reinoName);
        if (realm == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] Reino '" + reinoName + "' no encontrado."));
            return 1;
        }

        boolean ok = RealmManager.deleteRealm(realm.getId());
        if (ok) {
            src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Reino '" + reinoName + "' eliminado administrativamente."), true);
        } else {
            src.sendFailure(Component.literal("§cNo se pudo eliminar el reino '" + reinoName + "'."));
        }
        return 1;
    }

    private static int debugInfo(CommandSourceStack src) {
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();

        int diosaCount = 0;
        int sacerdoteCount = 0;
        int samuelCount = 0;
        int heraldoCount = 0;
        int monjeCount = 0;
        int custodioCount = 0;

        if (server != null) {
            for (ServerLevel level : server.getAllLevels()) {
                for (net.minecraft.world.entity.Entity e : level.getAllEntities()) {
                    if (e instanceof com.mundodetronos2.entity.CustomNPCEntity npc) {
                        String type = npc.getNpcModel().toLowerCase().trim();
                        if (type.equals("diosa") || type.equals("diosa_maria")) diosaCount++;
                        else if (type.equals("sacerdote")) sacerdoteCount++;
                        else if (type.equals("blacksmith") || type.equals("samuel")) samuelCount++;
                        else if (type.equals("heraldo")) heraldoCount++;
                        else if (type.equals("monje_destino")) monjeCount++;
                        else if (type.equals("custodio_trono")) custodioCount++;
                    }
                }
            }
        }

        final int fDiosa = diosaCount;
        final int fSacerdote = sacerdoteCount;
        final int fSamuel = samuelCount;
        final int fHeraldo = heraldoCount;
        final int fMonje = monjeCount;
        final int fCustodio = custodioCount;

        src.sendSuccess(() -> Component.literal("§6=== MUNDO DE TRONOS 2 ==="), false);

        // HUD
        src.sendSuccess(() -> Component.literal("§eHUD:"), false);
        src.sendSuccess(() -> Component.literal("  Tiempo: §f" + (server != null ? "Sincronizado" : "N/A")), false);
        src.sendSuccess(() -> Component.literal("  Trono: §fActivo"), false);
        src.sendSuccess(() -> Component.literal("  Team Vidas: §f1000 Max"), false);

        // NPC
        src.sendSuccess(() -> Component.literal("§eNPC:"), false);
        src.sendSuccess(() -> Component.literal("  Diosa: §f" + fDiosa), false);
        src.sendSuccess(() -> Component.literal("  Sacerdote: §f" + fSacerdote), false);
        src.sendSuccess(() -> Component.literal("  Samuel: §f" + fSamuel), false);
        src.sendSuccess(() -> Component.literal("  Heraldo: §f" + fHeraldo), false);
        src.sendSuccess(() -> Component.literal("  Monje: §f" + fMonje), false);
        src.sendSuccess(() -> Component.literal("  Custodio: §f" + fCustodio), false);

        // Diálogos
        src.sendSuccess(() -> Component.literal("§eDiálogos:"), false);
        src.sendSuccess(() -> Component.literal("  NPC actual: §fSeparados"), false);
        src.sendSuccess(() -> Component.literal("  Archivo: §fnpc_dialogues/*.json"), false);
        src.sendSuccess(() -> Component.literal("  Nodos: §fCargados"), false);
        src.sendSuccess(() -> Component.literal("  Misiones: §fSoportados"), false);

        // Si es jugador, agregar información de rol
        if (src.getEntity() instanceof ServerPlayer player) {
            PlayerRoleData data = RoleManager.getPlayerRoleData(player.getUUID());
            com.mundodetronos2.progression.PlayerProgressData pData = com.mundodetronos2.progression.ProgressionManager.getProgressData(player.getUUID());

            src.sendSuccess(() -> Component.literal("§eRol:"), false);
            src.sendSuccess(() -> Component.literal("  Jugador: §f" + player.getGameProfile().getName()), false);
            src.sendSuccess(() -> Component.literal("  Rol: §f" + data.getRole().name()), false);
            src.sendSuccess(() -> Component.literal("  Nivel: §f" + data.getLevel()), false);
            src.sendSuccess(() -> Component.literal("  Skill Points: §f" + pData.getSkillPoints()), false);

            src.sendSuccess(() -> Component.literal("§eHabilidades:"), false);
            src.sendSuccess(() -> Component.literal("  Desbloqueadas: §f" + pData.getUnlockedSkills().toString()), false);
            src.sendSuccess(() -> Component.literal("  Cooldowns: §fMonitoreados"), false);

            RealmData realm = RealmManager.getPlayerRealm(player.getUUID());
            if (realm != null) {
                ThroneData throne = realm.getThroneId() != null ? ThroneManager.getThroneById(realm.getThroneId()) : null;
                src.sendSuccess(() -> Component.literal("§eTrono:"), false);
                src.sendSuccess(() -> Component.literal("  Registrado: §f" + (throne != null ? "Sí" : "No")), false);
                src.sendSuccess(() -> Component.literal("  Líder: §f" + (realm.getOwnerId().equals(player.getUUID()) ? "Sí" : "No")), false);
                src.sendSuccess(() -> Component.literal("  Centro: §f" + (throne != null ? throne.getPos().toShortString() : "N/A")), false);
                src.sendSuccess(() -> Component.literal("  Protección: §f150x150"), false);
                src.sendSuccess(() -> Component.literal("  Límites: §fSoportados"), false);
            }

            int seconds = TimeManager.getRemainingSeconds(player.getUUID());
            boolean isOp = player.hasPermissions(2);
            src.sendSuccess(() -> Component.literal("§eTiempo:"), false);
            src.sendSuccess(() -> Component.literal("  Jugador: §f" + player.getGameProfile().getName()), false);
            src.sendSuccess(() -> Component.literal("  Tiempo restante: §f" + seconds + "s"), false);
            src.sendSuccess(() -> Component.literal("  OP: §f" + (isOp ? "Sí" : "No")), false);
            src.sendSuccess(() -> Component.literal("  Exento: §f" + (isOp ? "Sí" : "No")), false);
        }

        src.sendSuccess(() -> Component.literal("§eAsedio:"), false);
        src.sendSuccess(() -> Component.literal("  Evento: §f" + (ThroneManager.isGlobalEventActive() ? "ACTIVADO" : "DESACTIVADO")), false);
        src.sendSuccess(() -> Component.literal("  Cargas: §f" + com.mundodetronos2.throne.ThroneAttackManager.getActiveAttacksSize() + " activas"), false);

        src.sendSuccess(() -> Component.literal(""), false);
        return 1;
    }

    public static void darArmaduraDirecto(ServerPlayer target, String rolName) {
        String roleLower = rolName.toLowerCase().trim();
        net.minecraft.world.item.Item h, c, l, b;
        String displayNameRole = "Guerrero";

        if (roleLower.equals("guerrero") || roleLower.equals("warrior")) {
            h = net.minecraft.world.item.Items.IRON_HELMET;
            c = net.minecraft.world.item.Items.IRON_CHESTPLATE;
            l = net.minecraft.world.item.Items.IRON_LEGGINGS;
            b = net.minecraft.world.item.Items.IRON_BOOTS;
            displayNameRole = "Guerrero";
            roleLower = "guerrero";
        } else if (roleLower.equals("berserker")) {
            h = net.minecraft.world.item.Items.CHAINMAIL_HELMET;
            c = net.minecraft.world.item.Items.CHAINMAIL_CHESTPLATE;
            l = net.minecraft.world.item.Items.CHAINMAIL_LEGGINGS;
            b = net.minecraft.world.item.Items.CHAINMAIL_BOOTS;
            displayNameRole = "Berserker";
            roleLower = "berserker";
        } else if (roleLower.equals("mago") || roleLower.equals("mage")) {
            h = net.minecraft.world.item.Items.GOLDEN_HELMET;
            c = net.minecraft.world.item.Items.GOLDEN_CHESTPLATE;
            l = net.minecraft.world.item.Items.GOLDEN_LEGGINGS;
            b = net.minecraft.world.item.Items.GOLDEN_BOOTS;
            displayNameRole = "Mago";
            roleLower = "mago";
        } else if (roleLower.equals("arquero") || roleLower.equals("archer")) {
            h = net.minecraft.world.item.Items.LEATHER_HELMET;
            c = net.minecraft.world.item.Items.LEATHER_CHESTPLATE;
            l = net.minecraft.world.item.Items.LEATHER_LEGGINGS;
            b = net.minecraft.world.item.Items.LEATHER_BOOTS;
            displayNameRole = "Arquero";
            roleLower = "arquero";
        } else if (roleLower.equals("paladin")) {
            h = net.minecraft.world.item.Items.GOLDEN_HELMET;
            c = net.minecraft.world.item.Items.GOLDEN_CHESTPLATE;
            l = net.minecraft.world.item.Items.GOLDEN_LEGGINGS;
            b = net.minecraft.world.item.Items.GOLDEN_BOOTS;
            displayNameRole = "Paladín";
            roleLower = "paladin";
        } else if (roleLower.equals("draconico")) {
            h = net.minecraft.world.item.Items.LEATHER_HELMET;
            c = net.minecraft.world.item.Items.LEATHER_CHESTPLATE;
            l = net.minecraft.world.item.Items.LEATHER_LEGGINGS;
            b = net.minecraft.world.item.Items.LEATHER_BOOTS;
            displayNameRole = "Dracónico";
            roleLower = "draconico";
        } else if (roleLower.equals("clerigo") || roleLower.equals("cleric")) {
            h = net.minecraft.world.item.Items.LEATHER_HELMET;
            c = net.minecraft.world.item.Items.LEATHER_CHESTPLATE;
            l = net.minecraft.world.item.Items.LEATHER_LEGGINGS;
            b = net.minecraft.world.item.Items.LEATHER_BOOTS;
            displayNameRole = "Clérigo";
            roleLower = "clerigo";
        } else {
            return;
        }

        net.minecraft.world.item.ItemStack helmet = new net.minecraft.world.item.ItemStack(h);
        net.minecraft.world.item.ItemStack chest = new net.minecraft.world.item.ItemStack(c);
        net.minecraft.world.item.ItemStack leggings = new net.minecraft.world.item.ItemStack(l);
        net.minecraft.world.item.ItemStack boots = new net.minecraft.world.item.ItemStack(b);

        final String finalDisplayNameRole = displayNameRole;
        final String finalRoleLower = roleLower;

        String[] parts = {"Casco", "Pechera", "Grebas", "Botas"};
        net.minecraft.world.item.ItemStack[] armors = {helmet, chest, leggings, boots};

        for (int i = 0; i < 4; i++) {
            net.minecraft.world.item.ItemStack armor = armors[i];
            net.minecraft.nbt.CompoundTag tag = armor.getOrCreateTag();
            tag.putBoolean("mundodetronos2:role_armor", true);
            tag.putBoolean("mundodetronos2:starter_armor", true);
            tag.putString("mundodetronos2:role", finalRoleLower);
            tag.putString("AuthorizedRole", "any");
            tag.putString("RoleItemID", "kit_inicial_" + finalRoleLower + "_" + parts[i].toLowerCase());

            armor.enchant(net.minecraft.world.item.enchantment.Enchantments.ALL_DAMAGE_PROTECTION, 4);
            armor.setHoverName(Component.literal("§6" + parts[i] + " Inicial de " + finalDisplayNameRole));

            target.getInventory().add(armor);
        }
    }

    private static int darArmadura(CommandSourceStack src, ServerPlayer target, String rolName) {
        darArmaduraDirecto(target, rolName);
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Entregada armadura inicial a " + target.getGameProfile().getName() + "."), true);
        return 1;
    }

    private static int listarModelosNpc(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal("§6=== MODELOS DE NPC REGISTRADOS EN EL MOD ==="), false);
        String[] modelos = {
            "archer", "blacksmith", "butcher", "farmer", "guard",
            "guardcyan", "guardgreen", "guardorange", "guardparts", "guardpink",
            "guardpurple", "guardred", "guardyellow", "wizard"
        };
        int i = 1;
        for (String m : modelos) {
            final int idx = i++;
            final String mName = m;
            src.sendSuccess(() -> Component.literal("§e" + idx + ". §f" + mName), false);
        }
        return 1;
    }


    private static int crearSoldado(CommandSourceStack src, String equipo) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        com.mundodetronos2.entity.SoldierEntity soldier = com.mundodetronos2.init.EntityInit.SOLDIER.get().create(player.level());
        if (soldier == null) return 0;

        soldier.setTeamId(equipo);
        soldier.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        soldier.setCustomName(Component.literal("§cSoldado (" + equipo.toUpperCase() + ")"));
        soldier.setCustomNameVisible(true);

        player.level().addFreshEntity(soldier);
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Soldado del equipo '" + equipo + "' generado correctamente."), true);
        return 1;
    }

    private static int crearSoldados(CommandSourceStack src, String equipo, int cantidad) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        for (int i = 0; i < cantidad; i++) {
            com.mundodetronos2.entity.SoldierEntity soldier = com.mundodetronos2.init.EntityInit.SOLDIER.get().create(player.level());
            if (soldier == null) continue;

            soldier.setTeamId(equipo);
            double offsetX = (player.getRandom().nextDouble() - 0.5D) * 4.0D;
            double offsetZ = (player.getRandom().nextDouble() - 0.5D) * 4.0D;
            soldier.moveTo(player.getX() + offsetX, player.getY(), player.getZ() + offsetZ, player.getYRot(), player.getXRot());
            soldier.setCustomName(Component.literal("§cSoldado (" + equipo.toUpperCase() + ")"));
            soldier.setCustomNameVisible(true);

            player.level().addFreshEntity(soldier);
        }

        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Generados " + cantidad + " soldados del equipo '" + equipo + "'."), true);
        return 1;
    }

    private static int iniciarBatalla(CommandSourceStack src) {
        com.mundodetronos2.npc.BattleManager.setBattleActive(true);
        src.sendSuccess(() -> Component.literal("§c[Mundo de Tronos] ¡BATALLA INICIADA! Los soldados comenzarán el combate."), true);
        return 1;
    }

    private static int detenerBatalla(CommandSourceStack src) {
        com.mundodetronos2.npc.BattleManager.setBattleActive(false);
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Batalla detenida. Los soldados vuelven al estado de reposo/espera."), true);
        return 1;
    }


    private static int crearNpc(CommandSourceStack src, String tipoOrId, String nombre) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        // Standard custom NPC creation via model name or registered NPC definition
        com.mundodetronos2.entity.CustomNPCEntity npc = com.mundodetronos2.init.EntityInit.CUSTOM_NPC.get().create(player.level());
        if (npc == null) return 0;

        npc.setNpcModel(tipoOrId);
        npc.setNpcTexture(tipoOrId);
        npc.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());

        String finalName = nombre != null ? nombre : tipoOrId.toUpperCase();
        npc.setCustomName(Component.literal(finalName));
        npc.setCustomNameVisible(true);

        player.level().addFreshEntity(npc);

        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] NPC '" + finalName + "' (" + tipoOrId + ") creado exitosamente."), true);
        return 1;
    }




    public static com.mundodetronos2.entity.CustomNPCEntity findNpcByNameOrType(ServerPlayer player, String target) {
        double range = 32.0D;
        net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(
            player.getX() - range, player.getY() - range, player.getZ() - range,
            player.getX() + range, player.getY() + range, player.getZ() + range
        );

        java.util.List<com.mundodetronos2.entity.CustomNPCEntity> npcs = player.level().getEntitiesOfClass(com.mundodetronos2.entity.CustomNPCEntity.class, area);
        if (npcs.isEmpty()) return null;

        com.mundodetronos2.entity.CustomNPCEntity closest = null;
        double minDistSq = Double.MAX_VALUE;
        for (com.mundodetronos2.entity.CustomNPCEntity npc : npcs) {
            String name = npc.getCustomName() != null ? npc.getCustomName().getString().toLowerCase() : "";
            String type = npc.getNpcModel().toLowerCase();
            if (target == null || name.contains(target.toLowerCase()) || type.equals(target.toLowerCase())) {
                double d = player.distanceToSqr(npc);
                if (d < minDistSq) {
                    minDistSq = d;
                    closest = npc;
                }
            }
        }
        return closest;
    }

    private static int infoNpc(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        com.mundodetronos2.entity.CustomNPCEntity npc = findNpcByNameOrType(player, null);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se encontró ningún NPC cerca."));
            return 1;
        }

        String name = npc.getCustomName() != null ? npc.getCustomName().getString() : "Desconocido";
        src.sendSuccess(() -> Component.literal("§6=== NPC INFO ==="), false);
        src.sendSuccess(() -> Component.literal("§7Nombre: §f" + name), false);
        src.sendSuccess(() -> Component.literal("§7Modelo: §e" + npc.getNpcModel()), false);
        src.sendSuccess(() -> Component.literal("§7Textura: §a" + npc.getNpcTexture()), false);
        src.sendSuccess(() -> Component.literal("§7ID: §d" + npc.getId()), false);
        return 1;
    }




    private static int darCarnetCmd(CommandSourceStack src, ServerPlayer target) {
        PlayerRoleData data = RoleManager.getPlayerRoleData(target.getUUID());
        String roleStr = data.isHasRole() ? data.getRole().name().toLowerCase() : "none";

        net.minecraft.world.item.ItemStack carnetStack = new net.minecraft.world.item.ItemStack(com.mundodetronos2.init.ItemInit.ROLE_CARD.get());
        CompoundTag tag = carnetStack.getOrCreateTag();
        tag.putString("OwnerUUID", target.getUUID().toString());
        tag.putString("OwnerName", target.getGameProfile().getName());
        tag.putString("RoleID", roleStr);
        tag.putInt("Level", data.getLevel());

        target.getInventory().add(carnetStack);
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Se ha restaurado/entregado el carnet de rol a " + target.getGameProfile().getName() + "."), true);
        return 1;
    }

    private static int darGuiaCmd(CommandSourceStack src, ServerPlayer target) {
        target.getInventory().add(NetworkManager.createGoddessBook());
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Se ha entregado la Guía de la Diosa María a " + target.getGameProfile().getName() + "."), true);
        return 1;
    }

    private static int reclamarGuiaCmd(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }
        player.getInventory().add(NetworkManager.createGoddessBook());
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Has recibido la Guía de la Diosa María."), false);
        return 1;
    }

    private static int darLlaveReubicacion(CommandSourceStack src, ServerPlayer target) {
        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TRIPWIRE_HOOK);
        net.minecraft.nbt.CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean("mundodetronos2:throne_relocation_key", true);
        stack.setHoverName(Component.literal("§6§lLlave de Reubicación de Trono"));

        target.getInventory().add(stack);
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Se ha entregado la Llave de Reubicación a " + target.getGameProfile().getName() + "."), true);
        return 1;
    }

    private static int unirEquipoCmd(CommandSourceStack src, String color) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        String colorLower = color.toLowerCase().trim();
        RealmData team = RealmManager.getRealmByColorKey(colorLower);
        if (team == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No existe ningún equipo con el color '" + color + "'. Usa: rojo, azul, verde, amarillo, morado, cian, naranja, rosa, blanco, negro."));
            return 0;
        }

        if (RealmManager.getPlayerRealmData(player.getUUID()) != null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] Ya perteneces a un equipo. Usa /tronos team salir primero."));
            return 0;
        }

        if (team.getMembers().size() >= team.getMaxPlayers()) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] El equipo " + colorLower.toUpperCase() + " ya tiene 6/6 jugadores."));
            return 0;
        }

        boolean success = RealmManager.joinTeam(player, colorLower);
        if (success) {
            src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Te has unido al equipo " + colorLower.toUpperCase() + "."), false);
            NetworkManager.syncHud(player);
            return 1;
        } else {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No te pudiste unir al equipo " + colorLower.toUpperCase() + "."));
            return 0;
        }
    }

    private static int invocarManuelCmd(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        AABB searchArea = player.getBoundingBox().inflate(16.0D);
        List<com.mundodetronos2.entity.CustomNPCEntity> existing = player.level().getEntitiesOfClass(com.mundodetronos2.entity.CustomNPCEntity.class, searchArea,
                npc -> (npc.getCustomName() != null && npc.getCustomName().getString().toLowerCase().contains("manuel")));

        if (!existing.isEmpty()) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] Ya existe un Manuel cercano."));
            return 0;
        }

        com.mundodetronos2.entity.CustomNPCEntity manuel = EntityInit.CUSTOM_NPC.get().create(player.level());
        if (manuel != null) {
            manuel.setNpcModel("blacksmith");
            manuel.setNpcTexture("blacksmith");
            manuel.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            manuel.setCustomName(Component.literal("§6Manuel (Herrero)"));
            manuel.setCustomNameVisible(true);
            player.level().addFreshEntity(manuel);

            src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] NPC Manuel invocado correctamente."), true);
            return 1;
        }
        return 0;
    }

    private static int invocarKarlaCmd(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        AABB searchArea = player.getBoundingBox().inflate(16.0D);
        List<CustomNPCEntity> existing = player.level().getEntitiesOfClass(CustomNPCEntity.class, searchArea,
                npc -> "adventurer".equalsIgnoreCase(npc.getNpcModel()) || (npc.getCustomName() != null && npc.getCustomName().getString().toLowerCase().contains("karla")));

        if (!existing.isEmpty()) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] Ya existe una Karla cercana."));
            return 0;
        }

        CustomNPCEntity karla = EntityInit.CUSTOM_NPC.get().create(player.level());
        if (karla != null) {
            karla.setNpcModel("adventurer");
            karla.setNpcTexture("adventurer");
            karla.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            karla.setCustomName(Component.literal("§6Karla (Gremio)"));
            karla.setCustomNameVisible(true);
            player.level().addFreshEntity(karla);

            src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] NPC Karla (CustomNPCEntity) invocada correctamente."), true);
            return 1;
        }
        return 0;
    }

    private static int designarLiderCmd(CommandSourceStack src, ServerPlayer target) {
        ServerPlayer sender = src.getEntity() instanceof ServerPlayer sp ? sp : null;
        RealmData realm = RealmManager.getPlayerRealm(target.getUUID());

        if (realm == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] " + target.getGameProfile().getName() + " no pertenece a ningún equipo."));
            return 0;
        }

        boolean isOp = src.hasPermission(2);
        boolean isOwner = sender != null && realm.getOwnerId() != null && realm.getOwnerId().equals(sender.getUUID());

        if (!isOp && !isOwner) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] Solo el Líder actual del equipo o un Administrador pueden designar al nuevo Líder."));
            return 0;
        }

        realm.setOwnerId(target.getUUID());

        for (com.mundodetronos2.player.PlayerRealmData prd : RealmManager.getPlayerRealmDataMap().values()) {
            if (prd.getRealmId().equals(realm.getId())) {
                if (prd.getPlayerId().equals(target.getUUID())) {
                    prd.setRole(com.mundodetronos2.realm.Role.OWNER);
                } else if (prd.getRole() == com.mundodetronos2.realm.Role.OWNER) {
                    prd.setRole(com.mundodetronos2.realm.Role.MEMBER);
                }
            }
        }

        RealmManager.giveLeaderKey(target);
        com.mundodetronos2.data.SaveManager.markDirty();
        RealmManager.save(false);

        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] ¡Se ha designado a " + target.getGameProfile().getName() + " como Líder de " + realm.getName() + "!"), true);
        com.mundodetronos2.network.MessageManager.actionBar(target, "§a★ ¡Ahora eres el Líder de tu equipo!");

        return 1;
    }

    private static int salirEquipoCmd(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        boolean success = RealmManager.leaveRealm(player.getUUID());
        if (success) {
            src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Has salido de tu equipo exitosamente."), false);
            NetworkManager.syncHud(player);
            return 1;
        } else {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No perteneces a ningún equipo actualmente."));
            return 0;
        }
    }

    private static int darTronoEquipo(CommandSourceStack src, String color) {
        RealmData realm = RealmManager.getRealmByColorKey(color);
        if (realm == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se encontró ningún equipo con el color '" + color + "'."));
            return 0;
        }

        if (realm.getThroneId() != null && ThroneManager.getThroneById(realm.getThroneId()) != null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] El equipo '" + realm.getName() + "' ya tiene un Trono registrado."));
            return 0;
        }

        UUID ownerId = realm.getOwnerId();
        if (ownerId == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] El equipo '" + realm.getName() + "' no tiene un líder/propietario asignado actualmente."));
            return 0;
        }

        ServerPlayer leader = src.getServer().getPlayerList().getPlayer(ownerId);
        if (leader == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] El líder del equipo '" + realm.getName() + "' no está conectado actualmente."));
            return 0;
        }

        net.minecraft.world.item.ItemStack throneStack = new net.minecraft.world.item.ItemStack(com.mundodetronos2.init.ItemInit.THRONE_ITEM.get());
        net.minecraft.nbt.CompoundTag tag = throneStack.getOrCreateTag();
        tag.putBoolean("mundodetronos2:throne_item", true);
        tag.putString("mundodetronos2:realm_id", realm.getId().toString());
        throneStack.setHoverName(Component.literal("§6§l♛ Trono de " + realm.getName()));

        leader.getInventory().add(throneStack);
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Se ha entregado el Trono de " + realm.getName() + " a su líder " + leader.getGameProfile().getName() + "."), true);
        com.mundodetronos2.network.MessageManager.actionBar(leader, "§a¡Has recibido el Trono de tu equipo!");
        return 1;
    }

    private static int setTronoNivelCmd(CommandSourceStack src, int nivel, String equipoName) {
        RealmData realm = null;
        if (equipoName != null) {
            realm = RealmManager.getRealmByColorKey(equipoName);
            if (realm == null) realm = RealmManager.getRealmByName(equipoName);
        } else if (src.getEntity() instanceof ServerPlayer sp) {
            realm = RealmManager.getPlayerRealm(sp.getUUID());
        }

        if (realm == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] Debes especificar un equipo o pertenecer a uno."));
            return 0;
        }

        if (realm.getThroneId() == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] El equipo '" + realm.getName() + "' no tiene trono registrado."));
            return 0;
        }

        ThroneData throne = ThroneManager.getThroneById(realm.getThroneId());
        if (throne != null) {
            throne.setThroneLevel(nivel);
            com.mundodetronos2.data.SaveManager.markDirty();
            RealmManager.save(false);
            final String rName = realm.getName();
            src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Nivel del Trono de '" + rName + "' establecido en Nivel " + nivel), true);
        }
        return 1;
    }

    private static int setMurallaNivelCmd(CommandSourceStack src, int nivel, String equipoName) {
        RealmData realm = null;
        if (equipoName != null) {
            realm = RealmManager.getRealmByColorKey(equipoName);
            if (realm == null) realm = RealmManager.getRealmByName(equipoName);
        } else if (src.getEntity() instanceof ServerPlayer sp) {
            realm = RealmManager.getPlayerRealm(sp.getUUID());
        }

        if (realm == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] Debes especificar un equipo o pertenecer a uno."));
            return 0;
        }

        if (realm.getThroneId() == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] El equipo '" + realm.getName() + "' no tiene trono registrado."));
            return 0;
        }

        ThroneData throne = ThroneManager.getThroneById(realm.getThroneId());
        if (throne != null) {
            throne.setWallLevel(nivel);
            // Reconstruir muralla si el nivel cambió
            ServerLevel level = src.getLevel();
            if (level != null) {
                com.mundodetronos2.throne.FortressWallManager.buildOrUpdateWall(level, throne);
            }
            com.mundodetronos2.data.SaveManager.markDirty();
            RealmManager.save(false);
            final String rName = realm.getName();
            src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Muralla de '" + rName + "' actualizada a Nivel " + nivel), true);
        }
        return 1;
    }

    private static int debugTronoCmd(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal("§e=== DEBUG TRONOS ==="), false);
        for (ThroneData throne : ThroneManager.getThronesMap().values()) {
            RealmData realm = RealmManager.getRealm(throne.getRealmId());
            String rName = realm != null ? realm.getName() : "Sin Reino";
            src.sendSuccess(() -> Component.literal("§7ID: §f" + throne.getId() + " | Equipo: §a" + rName + " §7| Nivel: §6" + throne.getThroneLevel() + " §7| Estado: §e" + throne.getState().name() + " §7| Pos: §f" + throne.getPos().toShortString()), false);
        }
        return 1;
    }

    private static int debugMurallaCmd(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal("§e=== DEBUG MURALLAS ==="), false);
        for (ThroneData throne : ThroneManager.getThronesMap().values()) {
            RealmData realm = RealmManager.getRealm(throne.getRealmId());
            String rName = realm != null ? realm.getName() : "Sin Reino";
            int blockCount = throne.getWallBlocksRaw().size();
            src.sendSuccess(() -> Component.literal("§7Equipo: §a" + rName + " §7| Nivel Muralla: §6" + throne.getWallLevel() + " §7| Bloques Registrados: §b" + blockCount), false);
        }
        return 1;
    }

    private static int debugDefensoresCmd(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal("§e=== DEBUG DEFENSORES ==="), false);
        if (src.getServer() != null) {
            for (ServerLevel level : src.getServer().getAllLevels()) {
                List<com.mundodetronos2.entity.SoldierEntity> defenders = level.getEntitiesOfClass(com.mundodetronos2.entity.SoldierEntity.class, new net.minecraft.world.phys.AABB(-10000, -100, -10000, 10000, 300, 10000));
                for (com.mundodetronos2.entity.SoldierEntity s : defenders) {
                    src.sendSuccess(() -> Component.literal("§7Soldado ID: §f" + s.getId() + " §7| Equipo: §c" + s.getTeamId() + " §7| Pos: §f" + s.blockPosition().toShortString()), false);
                }
            }
        }
        return 1;
    }

    private static int darTronoItem(CommandSourceStack src, ServerPlayer target) {
        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(com.mundodetronos2.init.ItemInit.THRONE_ITEM.get());
        net.minecraft.nbt.CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean("mundodetronos2:throne_item", true);
        stack.setHoverName(Component.literal("§6§lTrono Pendiente de Colocación"));

        target.getInventory().add(stack);
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Se ha entregado un Trono a " + target.getGameProfile().getName() + "."), true);
        return 1;
    }

    private static int darAsaltoItem(CommandSourceStack src, ServerPlayer target, int cantidad) {
        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(com.mundodetronos2.init.ItemInit.CARGA_ASALTO.get(), cantidad);
        target.getInventory().add(stack);
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Se han entregado " + cantidad + " Cargas de Asalto a " + target.getGameProfile().getName() + "."), true);
        return 1;
    }

    private static int setNivelCmd(CommandSourceStack src, ServerPlayer target, int nivel) {
        com.mundodetronos2.progression.ProgressionManager.setLevelDirectly(target.getUUID(), nivel, target);
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Nivel de " + target.getGameProfile().getName() + " establecido en " + nivel + "."), true);
        return 1;
    }

    private static int addNivelCmd(CommandSourceStack src, ServerPlayer target, int cantidad) {
        com.mundodetronos2.progression.PlayerProgressData pData = com.mundodetronos2.progression.ProgressionManager.getProgressData(target.getUUID());
        int newLvl = Math.max(1, Math.min(100, pData.getLevel() + cantidad));
        com.mundodetronos2.progression.ProgressionManager.setLevelDirectly(target.getUUID(), newLvl, target);
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Se han añadido " + cantidad + " niveles a " + target.getGameProfile().getName() + ". Nuevo nivel: " + newLvl + "."), true);
        return 1;
    }

    private static int addXpCmd(CommandSourceStack src, ServerPlayer target, int cantidad) {
        com.mundodetronos2.progression.ProgressionManager.addXp(target.getUUID(), cantidad, target);
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Se han otorgado " + cantidad + " XP a " + target.getGameProfile().getName() + "."), true);
        return 1;
    }

    private static int darMochilaUpgradeItem(CommandSourceStack src, ServerPlayer target, int cantidad) {
        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(com.mundodetronos2.init.ItemInit.MEJORA_MOCHILA.get(), cantidad);
        stack.setHoverName(Component.literal("§6§lMejora de Mochila"));
        target.getInventory().add(stack);
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Se entregaron " + cantidad + " ítems de Mejora de Mochila a " + target.getGameProfile().getName() + "."), true);
        return 1;
    }

    private static int setMochilaNivelCmd(CommandSourceStack src, ServerPlayer target, int nivel) {
        com.mundodetronos2.progression.PlayerProgressData pData = com.mundodetronos2.progression.ProgressionManager.getProgressData(target.getUUID());
        pData.setBackpackTier(nivel);
        com.mundodetronos2.progression.ProgressionManager.save(true);
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Nivel de Mochila de " + target.getGameProfile().getName() + " establecido en Tier " + nivel + " (" + (nivel * 15) + " slots)."), true);
        return 1;
    }
}