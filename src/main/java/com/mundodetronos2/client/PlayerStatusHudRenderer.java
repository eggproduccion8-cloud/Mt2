package com.mundodetronos2.client;

import com.mundodetronos2.client.HudLayoutManager.ComponentConfig;
import com.mundodetronos2.client.HudLayoutManager.ComponentId;
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

        // 1. UNIFIED PLAYER CARD (Head + Name + HP Bar with heart.png + Hunger Bar with food.png)
        ComponentConfig cardConfig = HudLayoutManager.getConfig(ComponentId.PLAYER_CARD);
        if (cardConfig.visible) {
            int startX = HudLayoutManager.getRenderX(ComponentId.PLAYER_CARD, screenWidth, screenHeight);
            int startY = HudLayoutManager.getRenderY(ComponentId.PLAYER_CARD, screenWidth, screenHeight);

            // Translucent dark panel box
            graphics.fill(startX - 2, startY - 2, startX + 145, startY + 54, 0x880A0D10);
            graphics.fill(startX, startY, startX + 143, startY + 52, 0xAA12151A);

            // Face head
            int headSize = 32;
            ResourceLocation skinTexture = DefaultPlayerSkin.getDefaultSkin(player.getUUID());
            try {
                if (mc.getConnection() != null) {
                    net.minecraft.client.multiplayer.PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
                    if (info != null) skinTexture = info.getSkinLocation();
                }
            } catch (Exception ignored) {}
            PlayerFaceRenderer.draw(graphics, skinTexture, startX + 4, startY + 4, headSize);

            // Name
            String name = player.getGameProfile().getName();
            graphics.drawString(font, name, startX + headSize + 10, startY + 4, 0xFFFFFFFF, true);

            // HP Bar
            int barX = startX + headSize + 10;
            int barY = startY + 16;
            int barWidth = 90;
            int barHeight = 10;

            float currentHp = player.getHealth();
            float maxHp = player.getMaxHealth();
            float hpRatio = Math.max(0.0f, Math.min(1.0f, currentHp / Math.max(1.0f, maxHp)));

            graphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xAA180808);
            int hpFill = (int) (barWidth * hpRatio);
            if (hpFill > 0) {
                graphics.fill(barX, barY, barX + hpFill, barY + barHeight, 0xFFCC1111);
                graphics.fill(barX, barY, barX + hpFill, barY + 2, 0xFFFF5555);
            }
            // Heart Icon PNG
            graphics.blit(HEART_ICON, barX - 12, barY - 1, 0, 0, 10, 10, 10, 10);
            graphics.drawString(font, (int) currentHp + "/" + (int) maxHp, barX + 4, barY + 1, 0xFFFFFFFF, true);

            // Hunger Bar
            int foodY = startY + 30;
            int foodLevel = player.getFoodData().getFoodLevel();
            float foodRatio = Math.max(0.0f, Math.min(1.0f, foodLevel / 20.0f));

            graphics.fill(barX, foodY, barX + barWidth, foodY + barHeight, 0xAA1C1005);
            int foodFill = (int) (barWidth * foodRatio);
            if (foodFill > 0) {
                int foodColor = foodLevel > 6 ? 0xFFD36E1B : 0xFFBB3311;
                graphics.fill(barX, foodY, barX + foodFill, foodY + barHeight, foodColor);
                graphics.fill(barX, foodY, barX + foodFill, foodY + 2, 0xFFFFA044);
            }
            // Food Icon PNG
            graphics.blit(FOOD_ICON, barX - 12, foodY - 1, 0, 0, 10, 10, 10, 10);
            graphics.drawString(font, foodLevel + "/20", barX + 4, foodY + 1, 0xFFFFFFFF, true);
        }

        // 2. PIEZAS DE ARMADURA EQUIPADAS
        ComponentConfig armorConfig = HudLayoutManager.getConfig(ComponentId.ARMOR_PANEL);
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
                int startX = HudLayoutManager.getRenderX(ComponentId.ARMOR_PANEL, screenWidth, screenHeight);
                int startY = HudLayoutManager.getRenderY(ComponentId.ARMOR_PANEL, screenWidth, screenHeight);
                int armorBoxSize = 18;
                int armorX = startX;

                for (EquipmentSlot slot : slots) {
                    ItemStack armorStack = player.getItemBySlot(slot);

                    graphics.fill(armorX - 1, startY - 1, armorX + armorBoxSize + 1, startY + armorBoxSize + 1, 0x880A0D10);
                    graphics.fill(armorX, startY, armorX + armorBoxSize, startY + armorBoxSize, 0xAA12151A);

                    if (!armorStack.isEmpty()) {
                        graphics.renderItem(armorStack, armorX + 1, startY + 1);

                        if (armorStack.isDamageableItem() && armorStack.isDamaged()) {
                            int maxDamage = armorStack.getMaxDamage();
                            int currentDamage = armorStack.getDamageValue();
                            float durRatio = Math.max(0.0f, (float) (maxDamage - currentDamage) / (float) maxDamage);

                            int barLen = (int) (14 * durRatio);
                            int barColor = (int) armorStack.getBarColor();

                            graphics.fill(armorX + 2, startY + armorBoxSize - 3, armorX + 16, startY + armorBoxSize - 1, 0xFF000000);
                            graphics.fill(armorX + 2, startY + armorBoxSize - 3, armorX + 2 + barLen, startY + armorBoxSize - 1, 0xFF000000 | barColor);
                        }
                    }

                    armorX += armorBoxSize + 3;
                }
            }
        }
    }
}
