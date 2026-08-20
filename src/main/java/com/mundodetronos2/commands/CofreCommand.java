package com.mundodetronos2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mundodetronos2.init.ItemInit;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class CofreCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cofre")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("lvl2")
                        .then(Commands.argument("jugador", EntityArgument.player())
                                .executes(ctx -> giveCrate(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador"), 2)))
                        .executes(ctx -> giveCrateSelf(ctx.getSource(), 2)))
                .then(Commands.literal("lvl3")
                        .then(Commands.argument("jugador", EntityArgument.player())
                                .executes(ctx -> giveCrate(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador"), 3)))
                        .executes(ctx -> giveCrateSelf(ctx.getSource(), 3)))
                .then(Commands.literal("lvl4")
                        .then(Commands.argument("jugador", EntityArgument.player())
                                .executes(ctx -> giveCrate(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador"), 4)))
                        .executes(ctx -> giveCrateSelf(ctx.getSource(), 4)))
        );
    }

    private static int giveCrateSelf(CommandSourceStack src, int level) {
        if (src.getEntity() instanceof ServerPlayer player) {
            return giveCrate(src, player, level);
        } else {
            src.sendFailure(Component.literal("Debes ser un jugador o especificar un objetivo."));
            return 0;
        }
    }

    private static int giveCrate(CommandSourceStack src, ServerPlayer target, int level) {
        ItemStack stack = switch (level) {
            case 2 -> new ItemStack(ItemInit.CRATE_LEVEL2.get());
            case 3 -> new ItemStack(ItemInit.CRATE_LEVEL3.get());
            case 4 -> new ItemStack(ItemInit.CRATE_LEVEL4.get());
            default -> new ItemStack(ItemInit.CRATE_LEVEL1.get());
        };

        target.getInventory().add(stack);
        src.sendSuccess(() -> Component.literal("§a[Mundo de Tronos] Entregado Cofre Nivel " + level + " a " + target.getGameProfile().getName()), true);
        return 1;
    }
}
