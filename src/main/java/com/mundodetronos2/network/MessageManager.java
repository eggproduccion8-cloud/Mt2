package com.mundodetronos2.network;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class MessageManager {

    public static void actionBar(ServerPlayer player, String message) {
        if (player == null) return;
        player.displayClientMessage(Component.literal(message), true); // true indicates action bar (overlay)
    }

    public static void chat(ServerPlayer player, String message) {
        if (player == null) return;
        player.displayClientMessage(Component.literal(message), false); // false indicates chat
    }

    public static void title(ServerPlayer player, String title, String subtitle) {
        if (player == null) return;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(10, 70, 20));
        if (title != null) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal(title)));
        }
        if (subtitle != null) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
        }
    }
}
