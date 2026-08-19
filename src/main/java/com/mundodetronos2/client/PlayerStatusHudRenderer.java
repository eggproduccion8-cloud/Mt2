package com.mundodetronos2.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PlayerStatusHudRenderer {

    public static final ResourceLocation HEART_ICON = new ResourceLocation("mundodetronos2", "textures/hud/heart.png");
    public static final ResourceLocation FOOD_ICON = new ResourceLocation("mundodetronos2", "textures/hud/food.png");

    public static void renderPlayerStatus(GuiGraphics graphics, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        Font font = mc.font;

        // 1. DIBUJAR PERFIL Y BARRAS DEL JUGADOR (PLAYER_CARD)
        HudLayoutManager.ComponentConfig cardConfig = HudLayoutManager.getConfig(HudLayoutManager.ComponentId.PLAYER_CARD);
        if (cardConfig.visible) {
            int startX = HudLayoutManager.getRenderX(HudLayoutManager.ComponentId.PLAYER_CARD, screenWidth, screenHeight);
            int startY = HudLayoutManager.getRenderY(HudLayoutManager.ComponentId.PLAYER_CARD, screenWidth, screenHeight);
            float scale = cardConfig.scale;

            graphics.pose().pushPose();
            graphics.pose().translate(startX, startY, 0);
            graphics.pose().scale(scale, scale, 1.0f);

            int headSize = 22;
            ResourceLocation skinTexture = DefaultPlayerSkin.getDefaultSkin(player.getUUID());
            try {
                if (mc.getConnection() != null) {
                    net.minecraft.client.multiplayer.PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
                    if (info != null) skinTexture = info.getSkinLocation();
                }
            } catch (Exception ignored) {}

            graphics.fill(-2, -2, headSize + 2, headSize + 2, 0xFF4488FF);
            graphics.fill(-1, -1, headSize + 1, headSize + 1, 0xFF0D121D);
            PlayerFaceRenderer.draw(graphics, skinTexture, 0, 0, headSize);

            // NOMBRE Y NIVEL
            int infoX = headSize + 8;
            String nameStr = player.getGameProfile().getName();
            int level = ClientPacketHandler.hudPlayerLevel;
            graphics.drawString(font, nameStr + " §7[Lvl " + level + "]", infoX, -2, 0xFFFFFFFF, true);

            // BARRA DE VIDA
            int barX = infoX;
            int barY = 10;
            int barWidth = 100;
            int barHeight = 6;

            float currentHp = player.getHealth();
            float maxHp = player.getMaxHealth();
            float hpRatio = Math.max(0.0f, Math.min(1.0f, currentHp / Math.max(1.0f, maxHp)));

            graphics.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + barHeight + 1, 0x88000000);
            graphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xAA220808);
            int hpFill = (int) (barWidth * hpRatio);
            if (hpFill > 0) {
                graphics.fill(barX, barY, barX + hpFill, barY + barHeight, 0xFFFF3333);
                graphics.fill(barX, barY, barX + hpFill, barY + 1, 0xFFFF8888);
            }

            graphics.blit(HEART_ICON, barX - 10, barY - 2, 0, 0, 8, 8, 8, 8);
            graphics.drawString(font, (int) currentHp + "/" + (int) maxHp, barX + barWidth + 6, barY - 1, 0xFFFF5555, true);

            // BARRA DE ABSORCIÓN DEDICADA
            float absorption = player.getAbsorptionAmount();
            int lastBarY = barY;
            if (absorption > 0) {
                int absY = barY + 10;
                float absRatio = Math.min(1.0f, absorption / maxHp);
                int absFill = (int) (barWidth * absRatio);

                graphics.fill(barX - 1, absY - 1, barX + barWidth + 1, absY + barHeight + 1, 0x88000000);
                graphics.fill(barX, absY, barX + barWidth, absY + barHeight, 0xAA222005);
                if (absFill > 0) {
                    graphics.fill(barX, absY, barX + absFill, absY + barHeight, 0xFFFFD700);
                    graphics.fill(barX, absY, barX + absFill, absY + 1, 0xFFFFFF88);
                }
                graphics.drawString(font, "+" + (int) absorption + " Abs", barX + barWidth + 6, absY - 1, 0xFFFFD700, true);
                lastBarY = absY;
            }

            // BARRA DE HAMBRE
            int foodY = lastBarY + 10;
            int foodLevel = player.getFoodData().getFoodLevel();
            float foodRatio = Math.max(0.0f, Math.min(1.0f, foodLevel / 20.0f));

            graphics.fill(barX - 1, foodY - 1, barX + barWidth + 1, foodY + barHeight + 1, 0x88000000);
            graphics.fill(barX, foodY, barX + barWidth, foodY + barHeight, 0xAA221405);
            int foodFill = (int) (barWidth * foodRatio);
            if (foodFill > 0) {
                int foodColor = foodLevel > 6 ? 0xFFE08020 : 0xFFCC3311;
                graphics.fill(barX, foodY, barX + foodFill, foodY + barHeight, foodColor);
                graphics.fill(barX, foodY, barX + foodFill, foodY + 1, 0xFFFFAA44);
            }

            graphics.blit(FOOD_ICON, barX - 10, foodY - 2, 0, 0, 8, 8, 8, 8);
            graphics.drawString(font, foodLevel + "/20", barX + barWidth + 6, foodY - 1, 0xFFFFAA00, true);

            // BARRA DE RESPIRACIÓN
            int currentAir = player.getAirSupply();
            int maxAir = player.getMaxAirSupply();
            if (currentAir < maxAir) {
                int airY = foodY + 10;
                float airRatio = Math.max(0.0f, Math.min(1.0f, (float) currentAir / (float) maxAir));
                int airFill = (int) (barWidth * airRatio);

                graphics.fill(barX - 1, airY - 1, barX + barWidth + 1, airY + barHeight + 1, 0x88000000);
                graphics.fill(barX, airY, barX + barWidth, airY + barHeight, 0xAA051522);
                if (airFill > 0) {
                    graphics.fill(barX, airY, barX + airFill, airY + barHeight, 0xFF33CCFF);
                    graphics.fill(barX, airY, barX + airFill, airY + 1, 0xFF88EEFF);
                }
                graphics.drawString(font, "Aire: " + Math.max(0, currentAir) + "/" + maxAir, barX + barWidth + 6, airY - 1, 0xFF55FFFF, true);
            }

            graphics.pose().popPose();
        }

        // 2. PIEZAS DE ARMADURA FLOTANTES INDEPENDIENTES (ARMOR_PANEL)
        HudLayoutManager.ComponentConfig armorConfig = HudLayoutManager.getConfig(HudLayoutManager.ComponentId.ARMOR_PANEL);
        if (armorConfig.visible) {
            EquipmentSlot[] slots = new EquipmentSlot[]{
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
            };

            boolean hasAnyArmor = false;
            for (EquipmentSlot slot : slots) {
                if (!player.getItemBySlot(slot).isEmpty()) {
                    hasAnyArmor = true;
                    break;
                }
            }

            if (hasAnyArmor) {
                int armorX = HudLayoutManager.getRenderX(HudLayoutManager.ComponentId.ARMOR_PANEL, screenWidth, screenHeight);
                int armorY = HudLayoutManager.getRenderY(HudLayoutManager.ComponentId.ARMOR_PANEL, screenWidth, screenHeight);
                float scale = armorConfig.scale;
                int armorBoxSize = 14;

                graphics.pose().pushPose();
                graphics.pose().translate(armorX, armorY, 0);
                graphics.pose().scale(scale, scale, 1.0f);

                int currX = 0;
                for (EquipmentSlot slot : slots) {
                    ItemStack armorStack = player.getItemBySlot(slot);
                    if (!armorStack.isEmpty()) {
                        graphics.fill(currX - 1, -1, currX + armorBoxSize + 1, armorBoxSize + 1, 0x66000000);
                        graphics.pose().pushPose();
                        graphics.pose().translate(currX, 0, 0);
                        graphics.pose().scale(0.8f, 0.8f, 1.0f);
                        graphics.renderItem(armorStack, 0, 0);
                        graphics.pose().popPose();
                        currX += armorBoxSize + 2;
                    }
                }
                graphics.pose().popPose();
            }
        }
    }
}
