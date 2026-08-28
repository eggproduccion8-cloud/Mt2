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
                        .executes(ctx -> removeClosestNpc(ctx.getSource())))
                .then(Commands.literal("remove_all")
                        .executes(ctx -> removeAllNpcs(ctx.getSource())))
                .then(Commands.literal("info")
                        .executes(ctx -> infoClosestNpc(ctx.getSource())))
                .then(Commands.literal("animation")
                        .then(Commands.argument("animacion", StringArgumentType.string())
                                .executes(ctx -> testAnimationClosest(ctx.getSource(), StringArgumentType.getString(ctx, "animacion"))))
                        .then(Commands.argument("npc", StringArgumentType.string())
                                .then(Commands.argument("animacion", StringArgumentType.string())
                                        .executes(ctx -> testAnimationTarget(ctx.getSource(), StringArgumentType.getString(ctx, "npc"), StringArgumentType.getString(ctx, "animacion"))))))
                .then(Commands.literal("setanimation")
                        .then(Commands.argument("animacion", StringArgumentType.string())
                                .executes(ctx -> setIdleAnimationClosest(ctx.getSource(), StringArgumentType.getString(ctx, "animacion")))))
                .then(Commands.literal("setmodel")
                        .then(Commands.argument("modelo", StringArgumentType.string())
                                .executes(ctx -> setModelClosest(ctx.getSource(), StringArgumentType.getString(ctx, "modelo")))))
                .then(Commands.literal("settexture")
                        .then(Commands.argument("textura", StringArgumentType.string())
                                .executes(ctx -> setTextureClosest(ctx.getSource(), StringArgumentType.getString(ctx, "textura")))))
                .then(Commands.literal("setname")
                        .then(Commands.argument("nombre", StringArgumentType.greedyString())
                                .executes(ctx -> setNameClosest(ctx.getSource(), StringArgumentType.getString(ctx, "nombre")))))
                .then(Commands.literal("resetanimation")
                        .executes(ctx -> resetAnimationClosest(ctx.getSource())))
        );
    }

    private static CustomNPCEntity findClosestNpc(ServerPlayer player, String nameOrTypeFilter) {
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
            String model = npc.getNpcModel().toLowerCase();
            if (nameOrTypeFilter == null || name.contains(nameOrTypeFilter.toLowerCase()) || model.equalsIgnoreCase(nameOrTypeFilter.toLowerCase())) {
                double dist = player.distanceToSqr(npc);
                if (dist < minDistSq) {
                    minDistSq = dist;
                    closest = npc;
                }
            }
        }
        return closest;
    }

    private static int listModels(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal("§6=== MODELOS DE NPC REGISTRADOS EN EL MOD ==="), false);
        int index = 1;
        for (String m : NPCModelRegistry.getRegisteredModelIds()) {
            final int idx = index++;
            final String modelId = m;
            src.sendSuccess(() -> Component.literal("§e" + idx + ". §f" + modelId), false);
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

        src.sendSuccess(() -> Component.literal("§a[NPC] Invocado NPC '" + finalName + "' con modelo '" + modelId + "'."), true);
        return 1;
    }

    private static int removeClosestNpc(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findClosestNpc(player, null);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[NPC] No se encontró ningún NPC cercano para eliminar."));
            return 0;
        }

        String name = npc.getCustomName() != null ? npc.getCustomName().getString() : "NPC";
        npc.discard();
        src.sendSuccess(() -> Component.literal("§a[NPC] Eliminado NPC cercano '" + name + "'."), true);
        return 1;
    }

    private static int removeAllNpcs(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        int count = 0;
        for (net.minecraft.world.entity.Entity e : level.getAllEntities()) {
            if (e instanceof CustomNPCEntity npc) {
                npc.discard();
                count++;
            }
        }

        final int removedCount = count;
        src.sendSuccess(() -> Component.literal("§a[NPC] Eliminados " + removedCount + " NPCs de la dimensión actual."), true);
        return 1;
    }

    private static int infoClosestNpc(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findClosestNpc(player, null);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[NPC] No se encontró ningún NPC cercano."));
            return 0;
        }

        String name = npc.getCustomName() != null ? npc.getCustomName().getString() : "Desconocido";
        src.sendSuccess(() -> Component.literal("§6=== INFORMACIÓN DEL NPC MÁS CERCANO ==="), false);
        src.sendSuccess(() -> Component.literal("§7Nombre: §f" + name), false);
        src.sendSuccess(() -> Component.literal("§7Modelo: §e" + npc.getNpcModel()), false);
        src.sendSuccess(() -> Component.literal("§7Textura: §a" + npc.getNpcTexture()), false);
        src.sendSuccess(() -> Component.literal("§7Animación Idle: §b" + npc.getIdleAnimation()), false);
        src.sendSuccess(() -> Component.literal("§7Animación Walk: §b" + npc.getWalkAnimation()), false);
        src.sendSuccess(() -> Component.literal("§7Animación Interact: §b" + npc.getInteractionAnimation()), false);
        src.sendSuccess(() -> Component.literal("§7UUID: §d" + npc.getUUID()), false);
        return 1;
    }

    private static int testAnimationClosest(CommandSourceStack src, String animName) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findClosestNpc(player, null);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[NPC] No se encontró ningún NPC cercano."));
            return 0;
        }

        npc.playAnimation(animName);
        src.sendSuccess(() -> Component.literal("§a[NPC] Reproduciendo animación '" + animName + "' en " + npc.getCustomName().getString()), true);
        return 1;
    }

    private static int testAnimationTarget(CommandSourceStack src, String targetFilter, String animName) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findClosestNpc(player, targetFilter);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[NPC] No se encontró ningún NPC con el nombre/modelo '" + targetFilter + "'."));
            return 0;
        }

        npc.playAnimation(animName);
        src.sendSuccess(() -> Component.literal("§a[NPC] Reproduciendo animación '" + animName + "' en " + npc.getCustomName().getString()), true);
        return 1;
    }

    private static int setIdleAnimationClosest(CommandSourceStack src, String animName) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findClosestNpc(player, null);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[NPC] No se encontró ningún NPC cercano."));
            return 0;
        }

        npc.setIdleAnimation(animName);
        src.sendSuccess(() -> Component.literal("§a[NPC] Animación idle de '" + npc.getCustomName().getString() + "' establecida en '" + animName + "'."), true);
        return 1;
    }

    private static int setModelClosest(CommandSourceStack src, String model) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findClosestNpc(player, null);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[NPC] No se encontró ningún NPC cercano."));
            return 0;
        }

        npc.setNpcModel(model);
        src.sendSuccess(() -> Component.literal("§a[NPC] Modelo de '" + npc.getCustomName().getString() + "' cambiado a '" + model + "'."), true);
        return 1;
    }

    private static int setTextureClosest(CommandSourceStack src, String texture) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findClosestNpc(player, null);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[NPC] No se encontró ningún NPC cercano."));
            return 0;
        }

        npc.setNpcTexture(texture);
        src.sendSuccess(() -> Component.literal("§a[NPC] Textura de '" + npc.getCustomName().getString() + "' cambiada a '" + texture + "'."), true);
        return 1;
    }

    private static int setNameClosest(CommandSourceStack src, String name) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findClosestNpc(player, null);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[NPC] No se encontró ningún NPC cercano."));
            return 0;
        }

        npc.setCustomName(Component.literal(name));
        npc.setCustomNameVisible(true);
        src.sendSuccess(() -> Component.literal("§a[NPC] Nombre del NPC cambiado a '" + name + "'."), true);
        return 1;
    }

    private static int resetAnimationClosest(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findClosestNpc(player, null);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[NPC] No se encontró ningún NPC cercano."));
            return 0;
        }

        npc.setIdleAnimation("idle");
        npc.setWalkAnimation("walk");
        npc.setInteractionAnimation("greet");
        src.sendSuccess(() -> Component.literal("§a[NPC] Animaciones de '" + npc.getCustomName().getString() + "' restablecidas al comportamiento automático (idle/walk)."), true);
        return 1;
    }
}
