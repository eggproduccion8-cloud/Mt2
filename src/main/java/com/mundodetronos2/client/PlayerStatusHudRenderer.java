package com.mundodetronos2.client;

import com.mundodetronos2.client.HudLayoutManager.ComponentConfig;
import com.mundodetronos2.client.HudLayoutManager.ComponentId;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PlayerStatusHudRenderer {

    public static void renderPlayerStatus(GuiGraphics graphics, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        Font font = mc.font;

        // 1. BARRA DE VIDA (HP)
        ComponentConfig hpConfig = HudLayoutManager.getConfig(ComponentId.HEALTH_BAR);
        if (hpConfig.visible) {
            int startX = HudLayoutManager.getRenderX(ComponentId.HEALTH_BAR, screenWidth, screenHeight);
            int startY = HudLayoutManager.getRenderY(ComponentId.HEALTH_BAR, screenWidth, screenHeight);
            int barWidth = hpConfig.width;
            int barHeight = hpConfig.height;

            float currentHp = player.getHealth();
            float maxHp = player.getMaxHealth();
            float hpRatio = Math.max(0.0f, Math.min(1.0f, currentHp / Math.max(1.0f, maxHp)));

            // Panel oscuro translúcido minimalista RPG
            graphics.fill(startX - 2, startY - 2, startX + barWidth + 2, startY + barHeight + 2, 0x880A0D10);
            graphics.fill(startX, startY, startX + barWidth, startY + barHeight, 0xAA180808);

            int hpFill = (int) (barWidth * hpRatio);
            if (hpFill > 0) {
                graphics.fill(startX, startY, startX + hpFill, startY + barHeight, 0xFFCC1111);
                graphics.fill(startX, startY, startX + hpFill, startY + 2, 0xFFFF5555);
            }

            // Ícono de corazón + Número actual (Sin textos largos como HP 20 / 20)
            String hpNum = String.valueOf((int) currentHp);
            graphics.drawString(font, "♥ " + hpNum, startX + 4, startY + (barHeight - 8) / 2, 0xFFFFFFFF, true);
        }

        // 2. BARRA DE ABSORCIÓN
        ComponentConfig absConfig = HudLayoutManager.getConfig(ComponentId.ABSORPTION_BAR);
        float absorption = player.getAbsorptionAmount();
        if (absConfig.visible && absorption > 0.0f) {
            int startX = HudLayoutManager.getRenderX(ComponentId.ABSORPTION_BAR, screenWidth, screenHeight);
            int startY = HudLayoutManager.getRenderY(ComponentId.ABSORPTION_BAR, screenWidth, screenHeight);
            int barWidth = absConfig.width;
            int barHeight = absConfig.height;

            float absRatio = Math.min(1.0f, absorption / Math.max(1.0f, player.getMaxHealth()));

            graphics.fill(startX - 2, startY - 2, startX + barWidth + 2, startY + barHeight + 2, 0x880A0D10);
            graphics.fill(startX, startY, startX + barWidth, startY + barHeight, 0xAA221800);

            int absFill = (int) (barWidth * absRatio);
            if (absFill > 0) {
                graphics.fill(startX, startY, startX + absFill, startY + barHeight, 0xFFFFBB00);
                graphics.fill(startX, startY, startX + absFill, startY + 2, 0xFFFFEE66);
            }

            graphics.drawString(font, "✦ +" + (int) absorption, startX + 4, startY + (barHeight - 8) / 2, 0xFFFFFFFF, true);
        }

        // 3. BARRA DE HAMBRE
        ComponentConfig foodConfig = HudLayoutManager.getConfig(ComponentId.HUNGER_BAR);
        if (foodConfig.visible) {
            int startX = HudLayoutManager.getRenderX(ComponentId.HUNGER_BAR, screenWidth, screenHeight);
            int startY = HudLayoutManager.getRenderY(ComponentId.HUNGER_BAR, screenWidth, screenHeight);
            int barWidth = foodConfig.width;
            int barHeight = foodConfig.height;

            int foodLevel = player.getFoodData().getFoodLevel();
            float foodRatio = Math.max(0.0f, Math.min(1.0f, foodLevel / 20.0f));

            graphics.fill(startX - 2, startY - 2, startX + barWidth + 2, startY + barHeight + 2, 0x880A0D10);
            graphics.fill(startX, startY, startX + barWidth, startY + barHeight, 0xAA1C1005);

            int foodFill = (int) (barWidth * foodRatio);
            if (foodFill > 0) {
                int foodColor = foodLevel > 6 ? 0xFFD36E1B : 0xFFBB3311;
                graphics.fill(startX, startY, startX + foodFill, startY + barHeight, foodColor);
                graphics.fill(startX, startY, startX + foodFill, startY + 2, 0xFFFFA044);
            }

            // Ícono de energía/alimento + Número (Sin HAMBRE 20 / 20)
            graphics.drawString(font, "🍗 " + foodLevel, startX + 4, startY + (barHeight - 8) / 2, 0xFFFFFFFF, true);
        }

        // 4. BARRA DE AIRE / NADAR (Solo bajo el agua o cuando disminuye)
        ComponentConfig airConfig = HudLayoutManager.getConfig(ComponentId.AIR_BAR);
        int airSupply = player.getAirSupply();
        int maxAir = player.getMaxAirSupply();
        boolean isUnderWater = player.isEyeInFluid(FluidTags.WATER) || airSupply < maxAir;

        if (airConfig.visible && isUnderWater) {
            int startX = HudLayoutManager.getRenderX(ComponentId.AIR_BAR, screenWidth, screenHeight);
            int startY = HudLayoutManager.getRenderY(ComponentId.AIR_BAR, screenWidth, screenHeight);
            int barWidth = airConfig.width;
            int barHeight = airConfig.height;

            float airRatio = Math.max(0.0f, Math.min(1.0f, (float) airSupply / (float) maxAir));

            graphics.fill(startX - 2, startY - 2, startX + barWidth + 2, startY + barHeight + 2, 0x880A0D10);
            graphics.fill(startX, startY, startX + barWidth, startY + barHeight, 0xAA061C28);

            int airFill = (int) (barWidth * airRatio);
            if (airFill > 0) {
                graphics.fill(startX, startY, startX + airFill, startY + barHeight, 0xFF22AAFF);
                graphics.fill(startX, startY, startX + airFill, startY + 2, 0xFF88DDFF);
            }

            graphics.drawString(font, "🫧 " + (int) (airRatio * 100) + "%", startX + 4, startY + (barHeight - 8) / 2, 0xFFFFFFFF, true);
        }

        // 5. PIEZAS DE ARMADURA EQUIPADAS
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
