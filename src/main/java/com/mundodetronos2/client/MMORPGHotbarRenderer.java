package com.mundodetronos2.client;

import com.mundodetronos2.client.HudLayoutManager.ComponentConfig;
import com.mundodetronos2.client.HudLayoutManager.ComponentId;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MMORPGHotbarRenderer {

    public static void renderHotbar(GuiGraphics graphics, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        Font font = mc.font;

        // 1. HOTBAR RENDERING WITH FULL SCALE SUPPORT
        ComponentConfig hotbarConfig = HudLayoutManager.getConfig(ComponentId.HOTBAR);
        if (hotbarConfig.visible) {
            float scale = hotbarConfig.scale;
            int baseSlotSize = 20;
            int basePadding = 3;

            int totalWidth = (int) (((9 * baseSlotSize) + (8 * basePadding)) * scale);
            int startX = HudLayoutManager.getRenderX(ComponentId.HOTBAR, screenWidth, screenHeight) - totalWidth / 2;
            int startY = HudLayoutManager.getRenderY(ComponentId.HOTBAR, screenWidth, screenHeight);

            int selectedIndex = player.getInventory().selected;

            graphics.pose().pushPose();
            graphics.pose().translate(startX, startY, 0);
            graphics.pose().scale(scale, scale, 1.0f);

            for (int i = 0; i < 9; i++) {
                int slotX = i * (baseSlotSize + basePadding);
                int slotY = 0;
                boolean isSelected = (i == selectedIndex);

                ResourceLocation slotTex = isSelected ?
                    new ResourceLocation("mundodetronos2", "textures/gui/slots_hover.png") :
                    new ResourceLocation("mundodetronos2", "textures/gui/slots.png");

                graphics.blit(slotTex, slotX, slotY, 0, 0, baseSlotSize, baseSlotSize, baseSlotSize, baseSlotSize);

                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty()) {
                    int itemX = slotX + 2;
                    int itemY = slotY + 2;

                    graphics.renderItem(player, stack, itemX, itemY, i);
                    graphics.renderItemDecorations(font, stack, itemX, itemY);
                }
            }
            graphics.pose().popPose();
        }

        // 2. OFFHAND SLOT RENDERING WITH FULL SCALE SUPPORT
        ComponentConfig offhandConfig = HudLayoutManager.getConfig(ComponentId.OFFHAND_SLOT);
        if (offhandConfig.visible) {
            float scale = offhandConfig.scale;
            int baseSlotSize = 20;

            int offX = HudLayoutManager.getRenderX(ComponentId.OFFHAND_SLOT, screenWidth, screenHeight);
            int offY = HudLayoutManager.getRenderY(ComponentId.OFFHAND_SLOT, screenWidth, screenHeight);

            graphics.pose().pushPose();
            graphics.pose().translate(offX, offY, 0);
            graphics.pose().scale(scale, scale, 1.0f);

            ResourceLocation slotTex = new ResourceLocation("mundodetronos2", "textures/gui/slots.png");
            graphics.blit(slotTex, 0, 0, 0, 0, baseSlotSize, baseSlotSize, baseSlotSize, baseSlotSize);

            ItemStack offhandStack = player.getOffhandItem();
            if (!offhandStack.isEmpty()) {
                graphics.renderItem(player, offhandStack, 2, 2, 100);
                graphics.renderItemDecorations(font, offhandStack, 2, 2);
            }
            graphics.pose().popPose();
        }

        // 3. XP BAR RENDERING WITH FULL SCALE SUPPORT
        ComponentConfig xpConfig = HudLayoutManager.getConfig(ComponentId.XP_BAR);
        if (xpConfig.visible) {
            float scale = xpConfig.scale;
            int xpX = HudLayoutManager.getRenderX(ComponentId.XP_BAR, screenWidth, screenHeight);
            int xpY = HudLayoutManager.getRenderY(ComponentId.XP_BAR, screenWidth, screenHeight);

            float xpProgress = player.experienceProgress;
            int xpLevel = player.experienceLevel;

            int badgeW = 32;
            int badgeH = 14;

            graphics.pose().pushPose();
            graphics.pose().translate(xpX - (badgeW * scale) / 2.0f, xpY, 0);
            graphics.pose().scale(scale, scale, 1.0f);

            graphics.fill(-1, -1, badgeW + 1, badgeH + 1, 0x880A0D10);
            graphics.fill(0, 0, badgeW, badgeH, 0xCC0D1810);

            int fillW = (int) (badgeW * xpProgress);
            if (fillW > 0) {
                graphics.fill(0, badgeH - 2, fillW, badgeH, 0xFF55FF55);
            }

            String lvlText = "◈ " + xpLevel;
            int textW = font.width(lvlText);
            graphics.drawString(font, lvlText, (badgeW - textW) / 2, 3, 0xFF55FF55, true);

            graphics.pose().popPose();
        }
    }
}
