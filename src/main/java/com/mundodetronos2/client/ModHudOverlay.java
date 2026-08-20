package com.mundodetronos2.client;

import com.mundodetronos2.config.ConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

@OnlyIn(Dist.CLIENT)
public class ModHudOverlay {

    public static final IGuiOverlay HUD_MMORPG = (gui, graphics, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;

        // 1. Barra de Trono o Muralla en MIRA/Target
        if (mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            net.minecraft.world.phys.BlockHitResult bhr = (net.minecraft.world.phys.BlockHitResult) mc.hitResult;
            BlockPos pos = bhr.getBlockPos();
            if (com.mundodetronos2.throne.ThroneManager.isWallBlock(pos)) {
                drawWallHpBar(graphics, width, height);
            } else {
                long now = System.currentTimeMillis();
                long delta = now - ClientPacketHandler.targetThroneLastHitTime;
                long maxDelta = ConfigManager.get().targetBarSinksSeconds * 1000L;
                if (delta < maxDelta && !ClientPacketHandler.targetThroneRealmName.isEmpty()) {
                    drawThroneHpBar(graphics, width, height);
                }
            }
        }

        // 2. Cinemática de pestañeo de ojos
        if (ClientEvents.eyeTransitionTicks >= 0) {
            drawEyeBlinkOverlay(graphics, width, height);
        }

        // 3. Brújula y Coordenadas
        drawCompassAndCoordinates(graphics, width);

        // 4. Estatus MMORPG, Chat MMORPG y Hotbar MMORPG (Única fuente visual del HUD)
        PlayerStatusHudRenderer.renderPlayerStatus(graphics, width, height);
        drawCleanChatOverlay(graphics);
        if (mc.screen == null) {
            MMORPGHotbarRenderer.renderHotbar(graphics, width, height);
        }

        // 5. Alertas de Muerte y Trono Caído
        if (System.currentTimeMillis() < ClientEvents.deathAlertEndTime && ClientEvents.lastDeadPlayerId != null) {
            drawDeathAlertHud(graphics, height);
        }

        if (System.currentTimeMillis() < ClientEvents.throneAlertEndTime && ClientEvents.throneAlertAttackerId != null) {
            drawThroneAlertHud(graphics, width, height);
        }

        // 6. Notificaciones y Estado de Asedio
        drawNotificationsAndSiege(graphics, width);
    };

    private static void drawLeftMissionHudPanel(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (ClientPacketHandler.hudActiveMissionTitle == null || ClientPacketHandler.hudActiveMissionTitle.isEmpty()) {
            return;
        }

        int x = 12;
        int y = 70; // Left side below player status
        int boxW = 140;
        int boxH = 42;

        // Clean subtle transparent MMORPG overlay without dark brown card fills
        graphics.fill(x - 2, y - 2, x + boxW + 2, y + boxH + 2, 0xAA111111);
        graphics.fill(x, y, x + boxW, y + boxH, 0xCC1A1A1A);

        // Gold Title Accent
        graphics.drawString(mc.font, "📜 MISIÓN ACTIVA", x + 6, y + 4, 0xFFFFD700, false);

        // Mission Name
        String name = ClientPacketHandler.hudActiveMissionTitle;
        if (name.length() > 20) name = name.substring(0, 18) + "...";
        graphics.drawString(mc.font, name, x + 6, y + 16, 0xFFFFFFFF, false);

        // Progress Objective
        if (ClientPacketHandler.hudActiveMissionProgress != null && !ClientPacketHandler.hudActiveMissionProgress.isEmpty()) {
            String prog = "Objetivo: " + ClientPacketHandler.hudActiveMissionProgress;
            if (prog.length() > 20) prog = prog.substring(0, 18) + "...";
            graphics.drawString(mc.font, prog, x + 6, y + 27, 0xFF88FF88, false);
        }
    }

    private static void drawNotificationsAndSiege(GuiGraphics graphics, int screenWidth) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Render Mission HUD Panel on the LEFT side of the screen
        drawLeftMissionHudPanel(graphics);

        int x = screenWidth - 145;
        int y = 10;

        // Tarjeta de Asedio Activo
        if (ClientPacketHandler.isAttackActive()) {
            int boxW = 135;
            int boxH = 52;

            graphics.fill(x - 3, y - 3, x + boxW + 3, y + boxH + 3, 0x33362819);
            graphics.fill(x - 1, y - 1, x + boxW + 1, y + boxH + 1, 0x88990000);
            graphics.fill(x, y, x + boxW, y + boxH, 0x44F3E5C8);

            long time = System.currentTimeMillis();
            String prefix = (time / 400) % 2 == 0 ? "§4⚔ " : "§c⚔ ";
            graphics.drawString(mc.font, prefix + "TRONO ATACADO", x + 5, y + 4, 0, false);

            String base = ClientPacketHandler.activeAttackBaseName;
            if (base.length() > 14) base = base.substring(0, 12) + "...";
            graphics.drawString(mc.font, "§0Base: §4" + base, x + 5, y + 14, 0, false);

            graphics.drawString(mc.font, "§0Vida: §d" + ClientPacketHandler.activeAttackThroneHp + "/" + ClientPacketHandler.activeAttackThroneMaxHp, x + 5, y + 24, 0, false);

            String attacker = ClientPacketHandler.activeAttackAttackerTeamName;
            if (attacker.length() > 14) attacker = attacker.substring(0, 12) + "...";
            graphics.drawString(mc.font, "§0Por: §5" + attacker, x + 5, y + 34, 0, false);

            String chargeTimeStr = "00:" + String.format("%02d", ClientPacketHandler.activeAttackSecondsLeft);
            graphics.drawString(mc.font, "§6Detona: " + chargeTimeStr, x + 5, y + 44, 0, false);

            y += boxH + 10;
        }

        // Notificaciones en cola
        long now = System.currentTimeMillis();
        for (ClientEvents.Notification notif : ClientEvents.activeNotifications) {
            if (now > notif.endTime) {
                ClientEvents.activeNotifications.remove(notif);
                continue;
            }

            int boxW = 135;
            int boxH = 32;
            int borderCol = notif.isDanger ? 0xFF990000 : 0xFFD4AF37;

            graphics.fill(x - 3, y - 3, x + boxW + 3, y + boxH + 3, 0xDD362819);
            graphics.fill(x - 1, y - 1, x + boxW + 1, y + boxH + 1, borderCol);
            graphics.fill(x, y, x + boxW, y + boxH, 0xCCF3E5C8);

            graphics.drawString(mc.font, (notif.isDanger ? "§4" : "§6") + notif.title, x + 5, y + 4, 0, false);

            String l2 = notif.line2;
            if (l2.length() > 22) l2 = l2.substring(0, 20) + "...";
            graphics.drawString(mc.font, "§0" + l2, x + 5, y + 14, 0, false);

            if (notif.line3 != null && !notif.line3.isEmpty()) {
                String l3 = notif.line3;
                if (l3.length() > 22) l3 = l3.substring(0, 20) + "...";
                graphics.drawString(mc.font, "§0" + l3, x + 5, y + 24, 0, false);
            }

            y += boxH + 6;
        }
    }

    private static void drawThroneAlertHud(GuiGraphics graphics, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        int boxWidth = 160;
        int x = screenWidth - boxWidth - 12;
        int y = screenHeight / 2 - 28;

        ResourceLocation skinTexture = DefaultPlayerSkin.getDefaultSkin(ClientEvents.throneAlertAttackerId);
        try {
            if (mc.getConnection() != null && ClientEvents.throneAlertAttackerId != null) {
                net.minecraft.client.multiplayer.PlayerInfo info = mc.getConnection().getPlayerInfo(ClientEvents.throneAlertAttackerId);
                if (info != null) {
                    skinTexture = info.getSkinLocation();
                }
            }
        } catch (Exception ignored) {}

        PlayerFaceRenderer.draw(graphics, skinTexture, x, y, 26);

        graphics.drawString(mc.font, "¡EL TRONO HA CAÍDO!", x + 32, y, 0xFFFF5555, true);
        graphics.drawString(mc.font, "TRONO DE " + ClientEvents.throneAlertRealmName.toUpperCase(), x + 32, y + 10, 0xFFFFAA00, true);
        graphics.drawString(mc.font, "Por: " + ClientEvents.throneAlertAttackerName, x + 32, y + 20, 0xFFE0E0E0, true);
        graphics.drawString(mc.font, "VIDAS: " + ClientEvents.throneAlertOldLives + " → " + ClientEvents.throneAlertNewLives + " (-1)", x + 32, y + 30, 0xFFFF5555, true);
    }

    private static void drawDeathAlertHud(GuiGraphics graphics, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        int x = 12;
        int y = screenHeight / 2 - 25;

        ResourceLocation skinTexture = DefaultPlayerSkin.getDefaultSkin(ClientEvents.lastDeadPlayerId);
        try {
            if (mc.getConnection() != null) {
                net.minecraft.client.multiplayer.PlayerInfo info = mc.getConnection().getPlayerInfo(ClientEvents.lastDeadPlayerId);
                if (info != null) {
                    skinTexture = info.getSkinLocation();
                }
            }
        } catch (Exception ignored) {}

        PlayerFaceRenderer.draw(graphics, skinTexture, x, y, 26);

        graphics.drawString(mc.font, ClientEvents.lastDeadPlayerName.toUpperCase(), x + 32, y, 0xFFFF5555, true);
        graphics.drawString(mc.font, "ha perdido 5 pts", x + 32, y + 10, 0xFFCCCCCC, true);
        graphics.drawString(mc.font, "de su Reino.", x + 32, y + 20, 0xFFCCCCCC, true);
    }

    private static void drawCompassAndCoordinates(GuiGraphics graphics, int screenWidth) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int centerX = screenWidth / 2;
        int y1 = 6;
        int width = 140;

        float yaw = mc.player.getYRot();
        float heading = (yaw % 360 + 360) % 360;

        String[] points = { "S", "SO", "O", "NO", "N", "NE", "E", "SE" };
        int[] degrees = { 0, 45, 90, 135, 180, 225, 270, 315 };

        for (int i = 0; i < points.length; i++) {
            float diff = degrees[i] - heading;
            if (diff < -180) diff += 360;
            if (diff > 180) diff -= 360;

            int xPos = centerX + (int) diff;
            if (xPos >= centerX - width / 2 && xPos <= centerX + width / 2) {
                String label = points[i];
                int color = 0xFFCCCCCC;

                if (label.equals("N") || label.equals("S") || label.equals("E") || label.equals("O")) {
                    color = Math.abs(diff) < 10 ? 0xFFFFD700 : 0xFFD4AF37;
                } else if (Math.abs(diff) < 10) {
                    color = 0xFFFFFFFF;
                }

                int labelW = mc.font.width(label);
                graphics.drawString(mc.font, label, xPos - labelW / 2, y1, color, true);
            }
        }

        graphics.drawString(mc.font, "▲", centerX - mc.font.width("▲") / 2, y1 + 10, 0xFFFFD700, true);

        if (ClientEvents.showHud) {
            String coordsStr = String.format("X: %d   Y: %d   Z: %d", mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ());
            ClientEvents.drawFlatCenteredString(graphics, mc.font, coordsStr, centerX, y1 + 20, 0xFFE0E0E0);
        }
    }

    private static void drawCleanChatOverlay(GuiGraphics graphics) {
        if (!ClientEvents.enableCustomChat) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        HudLayoutManager.ComponentConfig config = HudLayoutManager.getConfig(HudLayoutManager.ComponentId.CHAT_BOX);
        if (!config.visible) return;

        java.util.List<ClientEvents.ChatMessage> messages = ClientEvents.chatMessages;
        if (messages.isEmpty()) return;

        long now = System.currentTimeMillis();
        ClientEvents.ChatMessage latest = messages.get(messages.size() - 1);
        long age = now - latest.timestamp;

        boolean screenOpen = mc.screen != null;
        if (!screenOpen && age > 9000L) return;

        float alpha = 1.0f;
        if (!screenOpen && age > 7000L) {
            alpha = 1.0f - ((float) (age - 7000L) / 2000.0f);
            alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        }

        int bgAlpha = (int) (0x33 * alpha);
        int textAlpha = (int) (255 * alpha);

        int x = HudLayoutManager.getRenderX(HudLayoutManager.ComponentId.CHAT_BOX, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        int y = HudLayoutManager.getRenderY(HudLayoutManager.ComponentId.CHAT_BOX, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        int boxW = config.width;
        int boxH = config.height;

        graphics.fill(x - 2, y - 2, x + boxW + 2, y + boxH + 2, (bgAlpha << 24));
        graphics.fill(x, y, x + boxW, y + boxH, ((bgAlpha / 2) << 24) | 0x101418);

        int maxLines = 5;
        int start = Math.max(0, messages.size() - maxLines);
        int lineY = y + 4;

        int textColor = (textAlpha << 24) | 0xFFFFFF;

        for (int i = start; i < messages.size(); i++) {
            ClientEvents.ChatMessage msg = messages.get(i);
            String formatted = "§e" + msg.sender + ": §f" + msg.text;
            if (formatted.length() > 32) formatted = formatted.substring(0, 30) + "...";
            graphics.drawString(mc.font, formatted, x + 4, lineY, textColor, true);
            lineY += 12;
        }
    }

    private static void drawEyeBlinkOverlay(GuiGraphics graphics, int width, int height) {
        float alpha = 0.0f;
        int half = ClientEvents.maxTransitionTicks / 2;

        if (ClientEvents.eyeTransitionTicks > half) {
            alpha = (float) (ClientEvents.maxTransitionTicks - ClientEvents.eyeTransitionTicks) / (float) half;
        } else {
            alpha = (float) ClientEvents.eyeTransitionTicks / (float) half;
        }

        alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        int colorInt = ((int)(alpha * 255.0f) << 24) | 0x000000;

        graphics.fill(0, 0, width, height, colorInt);

        if (alpha > 0.85f) {
            graphics.drawString(Minecraft.getInstance().font, "La Diosa María te observa...", width / 2 - Minecraft.getInstance().font.width("La Diosa María te observa...") / 2, height / 2, 0xFFFFFFFF, false);
        }
    }

    private static void drawWallHpBar(GuiGraphics graphics, int width, int height) {
        int barWidth = 140;
        int barHeight = 8;
        int x = (width - barWidth) / 2;
        int y = 25;

        int currentHp = 100;
        int maxHp = 100;

        graphics.fill(x - 2, y - 2, x + barWidth + 2, y + barHeight + 2, 0xFF000000);
        graphics.fill(x, y, x + barWidth, y + barHeight, 0xFF444444);

        double fillRatio = (double) currentHp / (double) maxHp;
        int fillWidth = (int) (barWidth * fillRatio);

        if (fillWidth > 0) {
            graphics.fill(x, y, x + fillWidth, y + barHeight, 0xFF55FF55);
        }

        Minecraft mc = Minecraft.getInstance();
        String title = "MURALLA DE PIEDRA OFICIAL";
        String hpText = currentHp + " / " + maxHp + " HP";

        int titleWidth = mc.font.width(title);
        int hpWidth = mc.font.width(hpText);

        graphics.drawString(mc.font, title, (width - titleWidth) / 2, y - 11, 0xFFFFFFFF, false);
        graphics.drawString(mc.font, hpText, (width - hpWidth) / 2, y + 1, 0xFFFFFFFF, false);
    }

    private static void drawThroneHpBar(GuiGraphics graphics, int width, int height) {
        int barWidth = 140;
        int barHeight = 10;
        int x = (width - barWidth) / 2;
        int y = 25;

        if (ClientPacketHandler.targetThroneState.equalsIgnoreCase("REPAIRING")) {
            int cardW = 180;
            int cardH = 68;
            int cx = (width - cardW) / 2;
            int cy = 20;

            graphics.fill(cx - 3, cy - 3, cx + cardW + 3, cy + cardH + 3, 0xFF362819);
            graphics.fill(cx, cy, cx + cardW, cy + cardH, 0xEEF3E5C8);
            graphics.fill(cx + 2, cy + 2, cx + cardW - 2, cy + cardH - 2, 0xEEEBDAB3);

            Minecraft mc = Minecraft.getInstance();
            ClientEvents.drawFlatCenteredString(graphics, mc.font, "§4§l✦ TRONO EN RECONSTRUCCIÓN ✦", cx + cardW / 2, cy + 6, 0);
            ClientEvents.drawFlatCenteredString(graphics, mc.font, "§0VIDA DEL TRONO: §c0 / " + ClientPacketHandler.targetThroneMaxHealth, cx + cardW / 2, cy + 21, 0);
            ClientEvents.drawFlatCenteredString(graphics, mc.font, "§0VIDAS DEL EQUIPO: §1" + ClientPacketHandler.hudThroneLives, cx + cardW / 2, cy + 34, 0);
            ClientEvents.drawFlatCenteredString(graphics, mc.font, "§6§lEN RECONSTRUCCIÓN", cx + cardW / 2, cy + 48, 0);
            return;
        }

        graphics.fill(x - 2, y - 2, x + barWidth + 2, y + barHeight + 2, 0xFF000000);
        graphics.fill(x, y, x + barWidth, y + barHeight, 0xFF444444);

        double ratio = (double) ClientPacketHandler.targetThroneHealth / Math.max(1, ClientPacketHandler.targetThroneMaxHealth);
        int fillWidth = (int) (barWidth * ratio);

        int barColor = 0xFFFF5555;
        if (ClientPacketHandler.targetThroneState.equalsIgnoreCase("PROTECTED")) {
            barColor = 0xFF55FF55;
        }

        if (fillWidth > 0) {
            graphics.fill(x, y, x + fillWidth, y + barHeight, barColor);
        }

        Minecraft mc = Minecraft.getInstance();
        String title = "Trono: " + ClientPacketHandler.targetThroneRealmName;
        String hpText = ClientPacketHandler.targetThroneHealth + " / " + ClientPacketHandler.targetThroneMaxHealth + " HP";

        int titleWidth = mc.font.width(title);
        int hpWidth = mc.font.width(hpText);

        graphics.drawString(mc.font, title, (width - titleWidth) / 2, y - 11, 0xFFFFFFFF, false);
        graphics.drawString(mc.font, hpText, (width - hpWidth) / 2, y + 1, 0xFFFFFFFF, false);
    }
}
