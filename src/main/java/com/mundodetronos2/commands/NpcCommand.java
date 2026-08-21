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

import java.util.*;

public class NpcCommand {

    private static final List<String> DEFAULT_MODELS = List.of(
            "adventurer", "archer", "blacksmith", "butcher", "farmer", "guard",
            "guardcyan", "guardgreen", "guardorange", "guardparts", "guardpink",
            "guardpurple", "guardred", "guardyellow", "king", "lootbag",
            "minecart", "miner", "npcgreeting", "pirate", "tap", "tavern", "wizard"
    );

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
                        .executes(ctx -> showNpcInfo(ctx.getSource())))
                .then(Commands.literal("animation")
                        .then(Commands.argument("targetOrAnim", StringArgumentType.string())
                                .then(Commands.argument("animacion", StringArgumentType.string())
                                        .executes(ctx -> triggerAnimationOnTarget(ctx.getSource(), StringArgumentType.getString(ctx, "targetOrAnim"), StringArgumentType.getString(ctx, "animacion"))))
                                .executes(ctx -> triggerAnimationOnNearest(ctx.getSource(), StringArgumentType.getString(ctx, "targetOrAnim")))))
                .then(Commands.literal("setanimation")
                        .then(Commands.argument("animacion", StringArgumentType.string())
                                .executes(ctx -> setBaseAnimation(ctx.getSource(), StringArgumentType.getString(ctx, "animacion")))))
                .then(Commands.literal("setmodel")
                        .then(Commands.argument("modelo", StringArgumentType.string())
                                .executes(ctx -> setNpcModel(ctx.getSource(), StringArgumentType.getString(ctx, "modelo")))))
                .then(Commands.literal("settexture")
                        .then(Commands.argument("textura", StringArgumentType.string())
                                .executes(ctx -> setNpcTexture(ctx.getSource(), StringArgumentType.getString(ctx, "textura")))))
                .then(Commands.literal("setname")
                        .then(Commands.argument("nombre", StringArgumentType.greedyString())
                                .executes(ctx -> setNpcName(ctx.getSource(), StringArgumentType.getString(ctx, "nombre")))))
                .then(Commands.literal("resetanimation")
                        .executes(ctx -> resetAnimation(ctx.getSource())))
        );
    }

    private static int listModels(CommandSourceStack src) {
        Set<String> modelSet = new TreeSet<>(DEFAULT_MODELS);
        Collection<String> registered = NPCModelRegistry.getRegisteredModelIds();
        if (registered != null && !registered.isEmpty()) {
            modelSet.addAll(registered);
        }

        src.sendSuccess(() -> Component.literal("§6=== MODELOS DE NPC REGISTRADOS (" + modelSet.size() + ") ==="), false);
        int idx = 1;
        for (String m : modelSet) {
            final int number = idx++;
            final String name = m;
            src.sendSuccess(() -> Component.literal("§e" + number + ". §f" + name), false);
        }
        return 1;
    }

    private static int spawnNpc(CommandSourceStack src, String id, String customName) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = EntityInit.CUSTOM_NPC.get().create(player.level());
        if (npc == null) {
            src.sendFailure(Component.literal("§cError al instanciar la entidad CustomNPCEntity."));
            return 0;
        }

        String modelId = id.toLowerCase().trim();
        npc.setNpcModel(modelId);
        npc.setNpcTexture(modelId);
        npc.setIdleAnimation("idle");
        npc.setWalkAnimation("walk");
        npc.setInteractionAnimation("greet");

        String displayName = customName != null ? customName : (modelId.substring(0, 1).toUpperCase() + modelId.substring(1));
        npc.setCustomName(Component.literal(displayName));
        npc.setCustomNameVisible(true);

        npc.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        player.level().addFreshEntity(npc);

        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] NPC '" + displayName + "' (Modelo: " + modelId + ") invocado exitosamente."), true);
        return 1;
    }

    private static int removeNearestNpc(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findNearestNpc(player, 16.0D);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se encontró ningún NPC cercano para eliminar."));
            return 0;
        }

        String name = npc.getCustomName() != null ? npc.getCustomName().getString() : npc.getNpcModel();
        npc.discard();
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] NPC '" + name + "' eliminado exitosamente."), true);
        return 1;
    }

    private static int removeAllNpcs(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        List<CustomNPCEntity> list = level.getEntitiesOfClass(CustomNPCEntity.class, new AABB(-30000, -128, -30000, 30000, 320, 30000));
        int count = list.size();
        for (CustomNPCEntity npc : list) {
            npc.discard();
        }

        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Eliminados " + count + " NPCs de la dimensión actual."), true);
        return 1;
    }

    private static int showNpcInfo(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findNearestNpc(player, 32.0D);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se encontró ningún NPC cercano."));
            return 0;
        }

        String name = npc.getCustomName() != null ? npc.getCustomName().getString() : "Sin Nombre";
        src.sendSuccess(() -> Component.literal("§6=== NPC INFORMACIÓN ==="), false);
        src.sendSuccess(() -> Component.literal("§7Nombre: §f" + name), false);
        src.sendSuccess(() -> Component.literal("§7Modelo: §e" + npc.getNpcModel()), false);
        src.sendSuccess(() -> Component.literal("§7Textura: §a" + npc.getNpcTexture()), false);
        src.sendSuccess(() -> Component.literal("§7Anim Base (Idle): §b" + npc.getIdleAnimation()), false);
        src.sendSuccess(() -> Component.literal("§7Anim Walk: §b" + npc.getWalkAnimation()), false);
        src.sendSuccess(() -> Component.literal("§7Anim Interact: §b" + npc.getInteractionAnimation()), false);
        src.sendSuccess(() -> Component.literal("§7Anim Actual: §d" + npc.getActualCurrentAnimation()), false);
        src.sendSuccess(() -> Component.literal("§7Posición: §f" + npc.blockPosition().toShortString()), false);
        src.sendSuccess(() -> Component.literal("§7UUID: §8" + npc.getUUID()), false);

        return 1;
    }

    private static int triggerAnimationOnNearest(CommandSourceStack src, String anim) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findNearestNpc(player, 32.0D);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se encontró ningún NPC cercano."));
            return 0;
        }

        npc.triggerTempAnimation(anim, 60); // 3 segundos de animación temporal
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Reproduciendo animación temporal '" + anim + "' en NPC '" + getEntityName(npc) + "'."), true);
        return 1;
    }

    private static int triggerAnimationOnTarget(CommandSourceStack src, String target, String anim) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findMatchingNpc(player, target, 32.0D);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se encontró ningún NPC que coincida con '" + target + "'."));
            return 0;
        }

        npc.triggerTempAnimation(anim, 60);
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Reproduciendo animación '" + anim + "' en NPC '" + getEntityName(npc) + "'."), true);
        return 1;
    }

    private static int setBaseAnimation(CommandSourceStack src, String anim) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findNearestNpc(player, 32.0D);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se encontró ningún NPC cercano."));
            return 0;
        }

        npc.setIdleAnimation(anim);
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Animación base (Idle) de '" + getEntityName(npc) + "' establecida en '" + anim + "'."), true);
        return 1;
    }

    private static int setNpcModel(CommandSourceStack src, String modelo) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findNearestNpc(player, 32.0D);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se encontró ningún NPC cercano."));
            return 0;
        }

        String modelLower = modelo.toLowerCase().trim();
        npc.setNpcModel(modelLower);
        npc.setNpcTexture(modelLower);
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Modelo de '" + getEntityName(npc) + "' cambiado a '" + modelLower + "'."), true);
        return 1;
    }

    private static int setNpcTexture(CommandSourceStack src, String textura) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findNearestNpc(player, 32.0D);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se encontró ningún NPC cercano."));
            return 0;
        }

        String texLower = textura.toLowerCase().trim();
        npc.setNpcTexture(texLower);
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Textura de '" + getEntityName(npc) + "' cambiada a '" + texLower + "'."), true);
        return 1;
    }

    private static int setNpcName(CommandSourceStack src, String nombre) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findNearestNpc(player, 32.0D);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se encontró ningún NPC cercano."));
            return 0;
        }

        npc.setCustomName(Component.literal(nombre));
        npc.setCustomNameVisible(true);
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Nombre de NPC cambiado a '" + nombre + "'."), true);
        return 1;
    }

    private static int resetAnimation(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Este comando solo puede ser ejecutado por un jugador."));
            return 0;
        }

        CustomNPCEntity npc = findNearestNpc(player, 32.0D);
        if (npc == null) {
            src.sendFailure(Component.literal("§c[Mundo de Tronos] No se encontró ningún NPC cercano."));
            return 0;
        }

        npc.setIdleAnimation("idle");
        npc.setWalkAnimation("walk");
        npc.setInteractionAnimation("greet");
        npc.resetTempAnimation();

        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Animaciones de '" + getEntityName(npc) + "' restablecidas al comportamiento automático (idle/walk/greet)."), true);
        return 1;
    }

    private static CustomNPCEntity findNearestNpc(ServerPlayer player, double maxDist) {
        AABB area = player.getBoundingBox().inflate(maxDist);
        List<CustomNPCEntity> npcs = player.level().getEntitiesOfClass(CustomNPCEntity.class, area);
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

    private static CustomNPCEntity findMatchingNpc(ServerPlayer player, String search, double maxDist) {
        AABB area = player.getBoundingBox().inflate(maxDist);
        List<CustomNPCEntity> npcs = player.level().getEntitiesOfClass(CustomNPCEntity.class, area);
        String term = search.toLowerCase().trim();

        for (CustomNPCEntity npc : npcs) {
            String name = npc.getCustomName() != null ? npc.getCustomName().getString().toLowerCase() : "";
            String model = npc.getNpcModel().toLowerCase();
            if (name.contains(term) || model.contains(term)) {
                return npc;
            }
        }
        return findNearestNpc(player, maxDist);
    }

    private static String getEntityName(CustomNPCEntity npc) {
        return npc.getCustomName() != null ? npc.getCustomName().getString() : npc.getNpcModel();
    }
}
