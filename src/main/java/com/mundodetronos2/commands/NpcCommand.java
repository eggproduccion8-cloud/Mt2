package com.mundodetronos2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mundodetronos2.entity.CustomNPCEntity;
import com.mundodetronos2.init.EntityInit;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class NpcCommand {

    private static final String[] KNOWN_MODELS = {
        "archer", "blacksmith", "butcher", "farmer", "guard",
        "guardcyan", "guardgreen", "guardorange", "guardparts", "guardpink",
        "guardpurple", "guardred", "guardyellow", "wizard", "adventurer", "king", "miner", "pirate"
    };

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
                        .executes(ctx -> removeClosest(ctx.getSource())))
                .then(Commands.literal("remove_all")
                        .executes(ctx -> removeAll(ctx.getSource())))
                .then(Commands.literal("info")
                        .executes(ctx -> infoClosest(ctx.getSource())))
                .then(Commands.literal("animation")
                        .then(Commands.argument("animacion", StringArgumentType.string())
                                .executes(ctx -> playAnimOnClosest(ctx.getSource(), StringArgumentType.getString(ctx, "animacion")))))
                .then(Commands.literal("setanimation")
                        .then(Commands.argument("animacion", StringArgumentType.string())
                                .executes(ctx -> setAnimOnClosest(ctx.getSource(), StringArgumentType.getString(ctx, "animacion")))))
                .then(Commands.literal("setmodel")
                        .then(Commands.argument("modelo", StringArgumentType.string())
                                .executes(ctx -> setModelOnClosest(ctx.getSource(), StringArgumentType.getString(ctx, "modelo")))))
                .then(Commands.literal("settexture")
                        .then(Commands.argument("textura", StringArgumentType.string())
                                .executes(ctx -> setTextureOnClosest(ctx.getSource(), StringArgumentType.getString(ctx, "textura")))))
                .then(Commands.literal("setname")
                        .then(Commands.argument("nombre", StringArgumentType.greedyString())
                                .executes(ctx -> setNameOnClosest(ctx.getSource(), StringArgumentType.getString(ctx, "nombre")))))
                .then(Commands.literal("resetanimation")
                        .executes(ctx -> resetAnimOnClosest(ctx.getSource())))
        );
    }

    private static CustomNPCEntity findClosestNpc(ServerPlayer player) {
        double range = 16.0D;
        AABB area = new AABB(
            player.getX() - range, player.getY() - range, player.getZ() - range,
            player.getX() + range, player.getY() + range, player.getZ() + range
        );
        List<CustomNPCEntity> npcs = player.level().getEntitiesOfClass(CustomNPCEntity.class, area);
        if (npcs.isEmpty()) return null;

        CustomNPCEntity closest = null;
        double minDistSq = Double.MAX_VALUE;
        for (CustomNPCEntity npc : npcs) {
            double d = player.distanceToSqr(npc);
            if (d < minDistSq) {
                minDistSq = d;
                closest = npc;
            }
        }
        return closest;
    }

    private static int listModels(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal("§6=== MODELOS DE NPC DISPONIBLES ==="), false);
        int i = 1;
        for (String m : KNOWN_MODELS) {
            final int idx = i++;
            final String name = m;
            src.sendSuccess(() -> Component.literal("§e" + idx + ". §f" + name), false);
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

        String finalName = name != null ? name : modelId.toUpperCase();
        npc.setCustomName(Component.literal(finalName));
        npc.setCustomNameVisible(true);

        player.level().addFreshEntity(npc);

        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] NPC '" + finalName + "' (" + modelId + ") invocado exitosamente."), true);
        return 1;
    }

    private static int removeClosest(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findClosestNpc(player);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se encontró ningún NPC cercano."));
            return 0;
        }

        String name = npc.getCustomName() != null ? npc.getCustomName().getString() : "NPC";
        npc.discard();
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] NPC '" + name + "' eliminado."), true);
        return 1;
    }

    private static int removeAll(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        int count = 0;
        for (net.minecraft.world.entity.Entity e : player.serverLevel().getAllEntities()) {
            if (e instanceof CustomNPCEntity) {
                e.discard();
                count++;
            }
        }

        final int removedCount = count;
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Eliminados " + removedCount + " NPCs de la dimensión."), true);
        return 1;
    }

    private static int infoClosest(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findClosestNpc(player);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se encontró ningún NPC cercano."));
            return 0;
        }

        String name = npc.getCustomName() != null ? npc.getCustomName().getString() : "Desconocido";
        src.sendSuccess(() -> Component.literal("§6=== NPC INFO ==="), false);
        src.sendSuccess(() -> Component.literal("§7Nombre: §f" + name), false);
        src.sendSuccess(() -> Component.literal("§7Modelo: §e" + npc.getNpcModel()), false);
        src.sendSuccess(() -> Component.literal("§7Textura: §a" + npc.getNpcTexture()), false);
        src.sendSuccess(() -> Component.literal("§7Animación actual: §d" + npc.getActualCurrentAnimation()), false);
        src.sendSuccess(() -> Component.literal("§7ID: §b" + npc.getId()), false);
        return 1;
    }

    private static int playAnimOnClosest(CommandSourceStack src, String animName) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findClosestNpc(player);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se encontró ningún NPC cercano."));
            return 0;
        }

        npc.playAnimation(animName);
        src.sendSuccess(() -> Component.literal("§aReproduciendo animación '" + animName + "' en el NPC cercano."), true);
        return 1;
    }

    private static int setAnimOnClosest(CommandSourceStack src, String animName) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findClosestNpc(player);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se encontró ningún NPC cercano."));
            return 0;
        }

        npc.setIdleAnimation(animName);
        src.sendSuccess(() -> Component.literal("§aAnimación base establecida a '" + animName + "'."), true);
        return 1;
    }

    private static int setModelOnClosest(CommandSourceStack src, String modelName) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findClosestNpc(player);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se encontró ningún NPC cercano."));
            return 0;
        }

        npc.setNpcModel(modelName);
        src.sendSuccess(() -> Component.literal("§aModelo de NPC cambiado a '" + modelName + "'."), true);
        return 1;
    }

    private static int setTextureOnClosest(CommandSourceStack src, String texName) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findClosestNpc(player);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se encontró ningún NPC cercano."));
            return 0;
        }

        npc.setNpcTexture(texName);
        src.sendSuccess(() -> Component.literal("§aTextura de NPC cambiada a '" + texName + "'."), true);
        return 1;
    }

    private static int setNameOnClosest(CommandSourceStack src, String name) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findClosestNpc(player);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se encontró ningún NPC cercano."));
            return 0;
        }

        npc.setCustomName(Component.literal(name));
        npc.setCustomNameVisible(true);
        src.sendSuccess(() -> Component.literal("§aNombre de NPC cambiado a '" + name + "'."), true);
        return 1;
    }

    private static int resetAnimOnClosest(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findClosestNpc(player);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se encontró ningún NPC cercano."));
            return 0;
        }

        npc.resetAnimation();
        src.sendSuccess(() -> Component.literal("§aAnimación de NPC reiniciada a automática (idle/walk)."), true);
        return 1;
    }
}
