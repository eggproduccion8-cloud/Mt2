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

        // 1. RETRATO DEL JUGADOR
        HudLayoutManager.ComponentConfig headConfig = HudLayoutManager.getConfig(HudLayoutManager.ComponentId.HEAD_AVATAR);
        if (headConfig.visible) {
            int x = HudLayoutManager.getRenderX(headConfig, screenWidth, screenHeight);
            int y = HudLayoutManager.getRenderY(headConfig, screenWidth, screenHeight);
            float scale = headConfig.scale;

            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1.0f);

            ResourceLocation skinTexture = DefaultPlayerSkin.getDefaultSkin(player.getUUID());
            try {
                if (mc.getConnection() != null) {
                    net.minecraft.client.multiplayer.PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
                    if (info != null) skinTexture = info.getSkinLocation();
                }
            } catch (Exception ignored) {}

            PlayerFaceRenderer.draw(graphics, skinTexture, 0, 0, 24);
            graphics.pose().popPose();
        }

        // 2. NOMBRE Y NIVEL
        HudLayoutManager.ComponentConfig nameConfig = HudLayoutManager.getConfig(HudLayoutManager.ComponentId.PLAYER_NAME);
        if (nameConfig.visible) {
            int x = HudLayoutManager.getRenderX(nameConfig, screenWidth, screenHeight);
            int y = HudLayoutManager.getRenderY(nameConfig, screenWidth, screenHeight);
            float scale = nameConfig.scale;

            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1.0f);

            String nameStr = player.getGameProfile().getName();
            int level = ClientPacketHandler.hudPlayerLevel;
            graphics.drawString(font, nameStr + " §7[Lvl " + level + "]", 0, 0, 0xFFFFFFFF, true);
            graphics.pose().popPose();
        }

        // 3. BARRA DE VIDA JUGADOR
        HudLayoutManager.ComponentConfig hpConfig = HudLayoutManager.getConfig(HudLayoutManager.ComponentId.HEALTH_BAR);
        if (hpConfig.visible) {
            int x = HudLayoutManager.getRenderX(hpConfig, screenWidth, screenHeight);
            int y = HudLayoutManager.getRenderY(hpConfig, screenWidth, screenHeight);
            float scale = hpConfig.scale;

            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1.0f);

            int barWidth = 100;
            int barHeight = 6;
            float currentHp = player.getHealth();
            float maxHp = player.getMaxHealth();
            float hpRatio = Math.max(0.0f, Math.min(1.0f, currentHp / Math.max(1.0f, maxHp)));

            graphics.fill(-1, -1, barWidth + 1, barHeight + 1, 0x88000000);
            graphics.fill(0, 0, barWidth, barHeight, 0xAA220808);
            int hpFill = (int) (barWidth * hpRatio);
            if (hpFill > 0) {
                graphics.fill(0, 0, hpFill, barHeight, 0xFFFF3333);
            }
            graphics.drawString(font, (int) currentHp + "/" + (int) maxHp, barWidth + 4, -1, 0xFFFF5555, true);

            graphics.pose().popPose();
        }

        // 4. BARRA DE HAMBRE
        HudLayoutManager.ComponentConfig foodConfig = HudLayoutManager.getConfig(HudLayoutManager.ComponentId.HUNGER_BAR);
        if (foodConfig.visible) {
            int x = HudLayoutManager.getRenderX(foodConfig, screenWidth, screenHeight);
            int y = HudLayoutManager.getRenderY(foodConfig, screenWidth, screenHeight);
            float scale = foodConfig.scale;

            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1.0f);

            int barWidth = 100;
            int barHeight = 5;
            int foodLevel = player.getFoodData().getFoodLevel();
            float foodRatio = Math.max(0.0f, Math.min(1.0f, foodLevel / 20.0f));

            graphics.fill(-1, -1, barWidth + 1, barHeight + 1, 0x88000000);
            graphics.fill(0, 0, barWidth, barHeight, 0xAA221100);
            int foodFill = (int) (barWidth * foodRatio);
            if (foodFill > 0) {
                graphics.fill(0, 0, foodFill, barHeight, 0xFFFF9900);
            }
            graphics.drawString(font, foodLevel + "/20", barWidth + 4, -2, 0xFFFF9900, true);

            graphics.pose().popPose();
        }

        // 5. BARRA DE ABSORCIÓN
        HudLayoutManager.ComponentConfig absConfig = HudLayoutManager.getConfig(HudLayoutManager.ComponentId.ABSORPTION_BAR);
        if (absConfig.visible && player.getAbsorptionAmount() > 0) {
            int x = HudLayoutManager.getRenderX(absConfig, screenWidth, screenHeight);
            int y = HudLayoutManager.getRenderY(absConfig, screenWidth, screenHeight);
            float scale = absConfig.scale;

            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1.0f);

            int barWidth = 100;
            int barHeight = 5;
            float abs = player.getAbsorptionAmount();
            float absRatio = Math.min(1.0f, abs / Math.max(1.0f, player.getMaxHealth()));

            graphics.fill(-1, -1, barWidth + 1, barHeight + 1, 0x88000000);
            graphics.fill(0, 0, barWidth, barHeight, 0xAA222005);
            int absFill = (int) (barWidth * absRatio);
            if (absFill > 0) {
                graphics.fill(0, 0, absFill, barHeight, 0xFFFFD700);
            }
            graphics.drawString(font, "+" + (int) abs, barWidth + 4, -2, 0xFFFFD700, true);

            graphics.pose().popPose();
        }

        // 6. BARRA DE RESPIRACIÓN DE AGUA
        HudLayoutManager.ComponentConfig waterConfig = HudLayoutManager.getConfig(HudLayoutManager.ComponentId.WATER_BREATHING);
        if (waterConfig.visible && player.getAirSupply() < player.getMaxAirSupply()) {
            int x = HudLayoutManager.getRenderX(waterConfig, screenWidth, screenHeight);
            int y = HudLayoutManager.getRenderY(waterConfig, screenWidth, screenHeight);
            float scale = waterConfig.scale;

            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1.0f);

            int barWidth = 100;
            int barHeight = 5;
            int air = player.getAirSupply();
            int maxAir = player.getMaxAirSupply();
            float airRatio = Math.max(0.0f, Math.min(1.0f, (float) air / (float) maxAir));

            graphics.fill(-1, -1, barWidth + 1, barHeight + 1, 0x88000000);
            graphics.fill(0, 0, barWidth, barHeight, 0xAA001122);
            int airFill = (int) (barWidth * airRatio);
            if (airFill > 0) {
                graphics.fill(0, 0, airFill, barHeight, 0xFF55FFFF);
            }
            graphics.drawString(font, "≈ " + (air / 20) + "s", barWidth + 4, -2, 0xFF55FFFF, true);

            graphics.pose().popPose();
        }

        // 7. INDICADORES COMPACTOS: VIDAS EQUIPO, TRONO, TIEMPO, TEAM, ROL
        renderCompactIndicators(graphics, screenWidth, screenHeight, font);

        // 8. PIEZAS DE ARMADURA FLOTANTES INDEPENDIENTES (ARMOR_PANEL)
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

    private static void renderCompactIndicators(GuiGraphics graphics, int screenWidth, int screenHeight, Font font) {
        // ♥ VIDAS EQUIPO
        HudLayoutManager.ComponentConfig teamLivesCfg = HudLayoutManager.getConfig(HudLayoutManager.ComponentId.TEAM_LIVES);
        if (teamLivesCfg.visible) {
            int x = HudLayoutManager.getRenderX(teamLivesCfg, screenWidth, screenHeight);
            int y = HudLayoutManager.getRenderY(teamLivesCfg, screenWidth, screenHeight);
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(teamLivesCfg.scale, teamLivesCfg.scale, 1.0f);
            graphics.drawString(font, "♥ " + ClientPacketHandler.hudSharedPoints, 0, 0, 0xFFFF5555, true);
            graphics.pose().popPose();
        }

        // ♛ VIDAS TRONO
        HudLayoutManager.ComponentConfig throneLivesCfg = HudLayoutManager.getConfig(HudLayoutManager.ComponentId.THRONE_LIVES);
        if (throneLivesCfg.visible) {
            int x = HudLayoutManager.getRenderX(throneLivesCfg, screenWidth, screenHeight);
            int y = HudLayoutManager.getRenderY(throneLivesCfg, screenWidth, screenHeight);
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(throneLivesCfg.scale, throneLivesCfg.scale, 1.0f);
            graphics.drawString(font, "♛ " + ClientPacketHandler.hudThroneLives, 0, 0, 0xFFFFD700, true);
            graphics.pose().popPose();
        }

        // ◷ TIEMPO
        HudLayoutManager.ComponentConfig timeCfg = HudLayoutManager.getConfig(HudLayoutManager.ComponentId.GAME_TIME);
        if (timeCfg.visible) {
            int x = HudLayoutManager.getRenderX(timeCfg, screenWidth, screenHeight);
            int y = HudLayoutManager.getRenderY(timeCfg, screenWidth, screenHeight);
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(timeCfg.scale, timeCfg.scale, 1.0f);

            int totalSecs = ClientPacketHandler.hudRemainingSeconds;
            int hrs = totalSecs / 3600;
            int mins = (totalSecs % 3600) / 60;
            int secs = totalSecs % 60;
            String timeStr = String.format("%d:%02d:%02d", hrs, mins, secs);

            graphics.drawString(font, "◷ " + timeStr, 0, 0, 0xFF55FFFF, true);
            graphics.pose().popPose();
        }

        // ⚔ TEAM
        HudLayoutManager.ComponentConfig teamCfg = HudLayoutManager.getConfig(HudLayoutManager.ComponentId.TEAM_NAME);
        if (teamCfg.visible) {
            int x = HudLayoutManager.getRenderX(teamCfg, screenWidth, screenHeight);
            int y = HudLayoutManager.getRenderY(teamCfg, screenWidth, screenHeight);
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(teamCfg.scale, teamCfg.scale, 1.0f);
            String tName = ClientPacketHandler.targetThroneRealmName != null && !ClientPacketHandler.targetThroneRealmName.isEmpty() ? ClientPacketHandler.targetThroneRealmName : "SIN TEAM";
            graphics.drawString(font, "⚔ " + tName, 0, 0, 0xFFAAAAFF, true);
            graphics.pose().popPose();
        }

        // ◆ ROL DEL JUGADOR
        HudLayoutManager.ComponentConfig roleCfg = HudLayoutManager.getConfig(HudLayoutManager.ComponentId.PLAYER_ROLE);
        if (roleCfg.visible) {
            int x = HudLayoutManager.getRenderX(roleCfg, screenWidth, screenHeight);
            int y = HudLayoutManager.getRenderY(roleCfg, screenWidth, screenHeight);
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(roleCfg.scale, roleCfg.scale, 1.0f);

            String role = ClientEvents.getClientPlayerRole();
            if (role == null || role.isEmpty() || role.equalsIgnoreCase("none")) {
                role = "ASPIRANTE";
            } else {
                role = role.toUpperCase();
            }

            graphics.drawString(font, "◆ " + role, 0, 0, 0xFFFFD700, true);
            graphics.pose().popPose();
        }
    }
}
