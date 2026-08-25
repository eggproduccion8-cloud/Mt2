package com.mundodetronos2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mundodetronos2.client.model.NPCModelRegistry;
import com.mundodetronos2.entity.CustomNPCEntity;
import com.mundodetronos2.init.EntityInit;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class NpcCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("npc")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("list")
                        .executes(ctx -> listModels(ctx.getSource())))
                .then(Commands.literal("spawn")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .then(Commands.argument("nombre", StringArgumentType.greedyString())
                                        .executes(ctx -> spawnNpc(ctx.getSource(), StringArgumentType.getString(ctx, "id"), StringArgumentType.getString(ctx, "nombre"))))
                                .executes(ctx -> spawnNpc(ctx.getSource(), StringArgumentType.getString(ctx, "id"), null))))
                .then(Commands.literal("remove")
                        .executes(ctx -> removeNearestNpc(ctx.getSource())))
                .then(Commands.literal("remove_all")
                        .executes(ctx -> removeAllNpcs(ctx.getSource())))
                .then(Commands.literal("info")
                        .executes(ctx -> infoNpc(ctx.getSource())))
                .then(Commands.literal("animation")
                        .then(Commands.argument("animacion", StringArgumentType.string())
                                .executes(ctx -> playAnimNearest(ctx.getSource(), StringArgumentType.getString(ctx, "animacion"))))
                        .then(Commands.argument("npc", StringArgumentType.string())
                                .then(Commands.argument("animacion", StringArgumentType.string())
                                        .executes(ctx -> playAnimTarget(ctx.getSource(), StringArgumentType.getString(ctx, "npc"), StringArgumentType.getString(ctx, "animacion"))))))
                .then(Commands.literal("setanimation")
                        .then(Commands.argument("animacion", StringArgumentType.string())
                                .executes(ctx -> setAnimNearest(ctx.getSource(), StringArgumentType.getString(ctx, "animacion")))))
                .then(Commands.literal("setmodel")
                        .then(Commands.argument("modelo", StringArgumentType.string())
                                .executes(ctx -> setModelNearest(ctx.getSource(), StringArgumentType.getString(ctx, "modelo")))))
                .then(Commands.literal("settexture")
                        .then(Commands.argument("textura", StringArgumentType.string())
                                .executes(ctx -> setTextureNearest(ctx.getSource(), StringArgumentType.getString(ctx, "textura")))))
                .then(Commands.literal("setname")
                        .then(Commands.argument("nombre", StringArgumentType.greedyString())
                                .executes(ctx -> setNameNearest(ctx.getSource(), StringArgumentType.getString(ctx, "nombre")))))
                .then(Commands.literal("resetanimation")
                        .executes(ctx -> resetAnimNearest(ctx.getSource())))
        );
    }

    private static int listModels(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal("§6=== MODELOS DE NPC REGISTRADOS ==="), false);
        String[] defaultModels = {
            "adventurer", "archer", "bard", "blacksmith", "butcher", "farmer", "guard",
            "guardcyan", "guardgreen", "guardorange", "guardparts", "guardpink",
            "guardpurple", "guardred", "guardyellow", "king", "miner", "pirate", "wizard"
        };
        int idx = 1;
        for (String m : defaultModels) {
            final int i = idx++;
            final String name = m;
            src.sendSuccess(() -> Component.literal("§e" + i + ". §f" + name), false);
        }
        return 1;
    }

    private static int spawnNpc(CommandSourceStack src, String modelId, String name) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = EntityInit.CUSTOM_NPC.get().create(player.level());
        if (npc == null) return 0;

        npc.setNpcModel(modelId);
        npc.setNpcTexture(modelId);
        npc.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());

        String finalName = name != null && !name.trim().isEmpty() ? name : modelId.toUpperCase();
        npc.setCustomName(Component.literal(finalName));
        npc.setCustomNameVisible(true);

        player.level().addFreshEntity(npc);

        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] NPC '" + finalName + "' (" + modelId + ") invocado correctamente."), true);
        return 1;
    }

    private static CustomNPCEntity findNearestNpc(ServerPlayer player, String target) {
        double range = 32.0D;
        AABB area = new AABB(
            player.getX() - range, player.getY() - range, player.getZ() - range,
            player.getX() + range, player.getY() + range, player.getZ() + range
        );

        List<CustomNPCEntity> npcs = player.level().getEntitiesOfClass(CustomNPCEntity.class, area);
        if (npcs.isEmpty()) return null;

        CustomNPCEntity closest = null;
        double minDistSq = Double.MAX_VALUE;
        for (CustomNPCEntity npc : npcs) {
            String name = npc.getCustomName() != null ? npc.getCustomName().getString().toLowerCase() : "";
            String type = npc.getNpcModel().toLowerCase();
            if (target == null || name.contains(target.toLowerCase()) || type.equalsIgnoreCase(target)) {
                double d = player.distanceToSqr(npc);
                if (d < minDistSq) {
                    minDistSq = d;
                    closest = npc;
                }
            }
        }
        return closest;
    }

    private static int removeNearestNpc(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findNearestNpc(player, null);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se encontró ningún NPC cercano."));
            return 1;
        }

        String name = npc.getCustomName() != null ? npc.getCustomName().getString() : "NPC";
        npc.discard();
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] NPC '" + name + "' eliminado."), true);
        return 1;
    }

    private static int removeAllNpcs(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        List<CustomNPCEntity> npcs = level.getEntitiesOfClass(CustomNPCEntity.class, new AABB(-30000, -100, -30000, 30000, 300, 30000));
        int count = npcs.size();
        for (CustomNPCEntity npc : npcs) {
            npc.discard();
        }

        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Eliminados " + count + " NPCs de la dimensión actual."), true);
        return 1;
    }

    private static int infoNpc(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findNearestNpc(player, null);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se encontró ningún NPC cercano."));
            return 1;
        }

        String name = npc.getCustomName() != null ? npc.getCustomName().getString() : "Sin nombre";
        src.sendSuccess(() -> Component.literal("§6=== NPC INFO ==="), false);
        src.sendSuccess(() -> Component.literal("§7Nombre: §f" + name), false);
        src.sendSuccess(() -> Component.literal("§7Modelo: §e" + npc.getNpcModel()), false);
        src.sendSuccess(() -> Component.literal("§7Textura: §a" + npc.getNpcTexture()), false);
        src.sendSuccess(() -> Component.literal("§7Animación actual: §b" + npc.getActualCurrentAnimation()), false);
        src.sendSuccess(() -> Component.literal("§7ID Entidad: §d" + npc.getId()), false);
        return 1;
    }

    private static int playAnimNearest(CommandSourceStack src, String animName) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findNearestNpc(player, null);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se encontró ningún NPC cercano."));
            return 1;
        }

        npc.playAnimation(animName, 60);
        src.sendSuccess(() -> Component.literal("§aReproduciendo animación '" + animName + "' en NPC cercano."), true);
        return 1;
    }

    private static int playAnimTarget(CommandSourceStack src, String targetNpc, String animName) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findNearestNpc(player, targetNpc);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se encontró ningún NPC con el nombre/tipo '" + targetNpc + "'."));
            return 1;
        }

        npc.playAnimation(animName, 60);
        src.sendSuccess(() -> Component.literal("§aReproduciendo animación '" + animName + "' en " + (npc.getCustomName() != null ? npc.getCustomName().getString() : targetNpc) + "."), true);
        return 1;
    }

    private static int setAnimNearest(CommandSourceStack src, String animName) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findNearestNpc(player, null);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se encontró ningún NPC cercano."));
            return 1;
        }

        npc.setInteractionAnimation(animName);
        src.sendSuccess(() -> Component.literal("§aAnimación de interacción establecida a '" + animName + "'."), true);
        return 1;
    }

    private static int setModelNearest(CommandSourceStack src, String model) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findNearestNpc(player, null);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se encontró ningún NPC cercano."));
            return 1;
        }

        npc.setNpcModel(model);
        src.sendSuccess(() -> Component.literal("§aModelo cambiado a '" + model + "'."), true);
        return 1;
    }

    private static int setTextureNearest(CommandSourceStack src, String texture) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findNearestNpc(player, null);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se encontró ningún NPC cercano."));
            return 1;
        }

        npc.setNpcTexture(texture);
        src.sendSuccess(() -> Component.literal("§aTextura cambiada a '" + texture + "'."), true);
        return 1;
    }

    private static int setNameNearest(CommandSourceStack src, String nombre) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findNearestNpc(player, null);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se encontró ningún NPC cercano."));
            return 1;
        }

        npc.setCustomName(Component.literal(nombre));
        npc.setCustomNameVisible(true);
        src.sendSuccess(() -> Component.literal("§aNombre cambiado a '" + nombre + "'."), true);
        return 1;
    }

    private static int resetAnimNearest(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findNearestNpc(player, null);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se encontró ningún NPC cercano."));
            return 1;
        }

        npc.resetAnimation();
        src.sendSuccess(() -> Component.literal("§aAnimación de NPC reiniciada al comportamiento automático (idle/walk)."), true);
        return 1;
    }
}
