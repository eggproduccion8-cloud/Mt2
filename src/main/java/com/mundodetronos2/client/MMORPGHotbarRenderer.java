package com.mundodetronos2.client;

import com.mundodetronos2.client.HudLayoutManager.ComponentConfig;
import com.mundodetronos2.client.HudLayoutManager.ComponentId;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MMORPGHotbarRenderer {

    public static void renderHotbar(GuiGraphics graphics, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        Font font = mc.font;

        // 1. DIBUJAR HOTBAR TRANSPARENTE CON SLOTS REDONDEADOS/SEPARADOS (SIN VANILLA BOX)
        ComponentConfig hotbarConfig = HudLayoutManager.getConfig(ComponentId.HOTBAR);
        if (hotbarConfig.visible) {
            int slotSize = 20;
            int padding = 3;
            int totalWidth = (9 * slotSize) + (8 * padding);

            int startX = HudLayoutManager.getRenderX(ComponentId.HOTBAR, screenWidth, screenHeight) - totalWidth / 2;
            int startY = HudLayoutManager.getRenderY(ComponentId.HOTBAR, screenWidth, screenHeight);

            int selectedIndex = player.getInventory().selected;

            for (int i = 0; i < 9; i++) {
                int slotX = startX + i * (slotSize + padding);
                int slotY = startY;
                boolean isSelected = (i == selectedIndex);

                if (isSelected) {
                    // Resplandor elegante alrededor del slot seleccionado
                    graphics.fill(slotX - 2, slotY - 2, slotX + slotSize + 2, slotY + slotSize + 2, 0x884488FF);
                    graphics.fill(slotX - 1, slotY - 1, slotX + slotSize + 1, slotY + slotSize + 1, 0xFF88CCFF);
                    graphics.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, 0xCC18202A);
                } else {
                    // Slot oscuro translúcido con borde discreto
                    graphics.fill(slotX - 1, slotY - 1, slotX + slotSize + 1, slotY + slotSize + 1, 0x44FFFFFF);
                    graphics.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, 0xAA0D1014);
                }

                // Renderizado del ItemStack
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty()) {
                    int itemX = slotX + 2;
                    int itemY = slotY + 2;

                    graphics.renderItem(player, stack, itemX, itemY, i);
                    graphics.renderItemDecorations(font, stack, itemX, itemY);
                }
            }
        }

        // 2. DIBUJAR SLOT DE SEGUNDA MANO (OFFHAND) INTEGRADO DISCRETAMENTE
        ComponentConfig offhandConfig = HudLayoutManager.getConfig(ComponentId.OFFHAND_SLOT);
        if (offhandConfig.visible) {
            int slotSize = 20;
            int offX = HudLayoutManager.getRenderX(ComponentId.OFFHAND_SLOT, screenWidth, screenHeight);
            int offY = HudLayoutManager.getRenderY(ComponentId.OFFHAND_SLOT, screenWidth, screenHeight);

            graphics.fill(offX - 1, offY - 1, offX + slotSize + 1, offY + slotSize + 1, 0x44FFFFFF);
            graphics.fill(offX, offY, offX + slotSize, offY + slotSize, 0xAA0D1014);

            ItemStack offhandStack = player.getOffhandItem();
            if (!offhandStack.isEmpty()) {
                int itemX = offX + 2;
                int itemY = offY + 2;

                graphics.renderItem(player, offhandStack, itemX, itemY, 100);
                graphics.renderItemDecorations(font, offhandStack, itemX, itemY);
            }
        }

        // 3. INSIGNIA / ELEMENTO VISUAL DE NIVEL Y PROGRESIÓN XP (SIN BARRA VANILLA GIGANTE)
        ComponentConfig xpConfig = HudLayoutManager.getConfig(ComponentId.XP_BAR);
        if (xpConfig.visible) {
            int xpX = HudLayoutManager.getRenderX(ComponentId.XP_BAR, screenWidth, screenHeight);
            int xpY = HudLayoutManager.getRenderY(ComponentId.XP_BAR, screenWidth, screenHeight);

            float xpProgress = player.experienceProgress;
            int xpLevel = player.experienceLevel;

            // Insignia mística redonda/cuadrada (Ej: ◈ 10)
            int badgeW = 32;
            int badgeH = 14;
            int bX = xpX - badgeW / 2;

            graphics.fill(bX - 1, xpY - 1, bX + badgeW + 1, xpY + badgeH + 1, 0x880A0D10);
            graphics.fill(bX, xpY, bX + badgeW, xpY + badgeH, 0xCC0D1810);

            // Anillo/indicador de progreso alrededor
            int fillW = (int) (badgeW * xpProgress);
            if (fillW > 0) {
                graphics.fill(bX, xpY + badgeH - 2, bX + fillW, xpY + badgeH, 0xFF55FF55);
            }

            String lvlText = "◈ " + xpLevel;
            int textW = font.width(lvlText);
            graphics.drawString(font, lvlText, bX + (badgeW - textW) / 2, xpY + 3, 0xFF55FF55, true);
        }
    }
}
