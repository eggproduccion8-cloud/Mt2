package com.mundodetronos2.gui;

import com.mundodetronos2.network.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class RoleAnvilScreen extends Screen {

    private int selectedInventorySlot = -1;
    private final List<Integer> repairableArmorSlots = new ArrayList<>();
    private int scrollOffset = 0;

    private int playerIronCount = 0;

    public RoleAnvilScreen() {
        super(Component.literal("Mesa de Reparaciones"));
        scanInventoryForRepairableArmors();
    }

    private void scanInventoryForRepairableArmors() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        repairableArmorSlots.clear();
        playerIronCount = 0;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() == Items.IRON_INGOT) {
                playerIronCount += stack.getCount();
            }

            // Detectar si es una armadura de inicio o de rol
            if (stack.getItem() instanceof ArmorItem && stack.hasTag()) {
                boolean isRoleArmor = stack.getTag().getBoolean("mundodetronos2:role_armor");
                boolean isInitialArmor = "any".equalsIgnoreCase(stack.getTag().getString("AuthorizedRole")) && stack.getTag().getString("RoleItemID").startsWith("kit_inicial_");

                if (isRoleArmor || isInitialArmor) {
                    repairableArmorSlots.add(i);
                }
            }
        }
    }

    @Override
    protected void init() {
        this.clearWidgets();
        scanInventoryForRepairableArmors();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int buttonWidth = 180;
        int buttonHeight = 22;

        ItemStack selectedStack = selectedInventorySlot != -1 ? Minecraft.getInstance().player.getInventory().getItem(selectedInventorySlot) : ItemStack.EMPTY;
        boolean canRepair = !selectedStack.isEmpty() && selectedStack.getDamageValue() > 0 && playerIronCount >= 3;

        // Botón Reparar
        GoddessIntroDialogueScreen.TransparentButton repairBtn = new GoddessIntroDialogueScreen.TransparentButton(
            centerX - 100, centerY + 65, 100, buttonHeight,
            Component.literal("REPARAR"), btn -> {
                if (canRepair) {
                    NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SRepairInitialArmorPacket(selectedInventorySlot));
                    this.onClose();
                }
            }
        );
        repairBtn.active = canRepair;
        this.addRenderableWidget(repairBtn);

        // Botón Cerrar
        this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(
            centerX + 10, centerY + 65, 90, buttonHeight,
            Component.literal("Cerrar"), btn -> this.onClose()
        ));

        // Botones de la Lista del Inventario (Panel Izquierdo)
        int listX = centerX - 140;
        int listY = centerY - 35;

        for (int i = 0; i < 4; i++) {
            int index = i + scrollOffset;
            if (index < repairableArmorSlots.size()) {
                final int slotIndex = repairableArmorSlots.get(index);
                ItemStack stack = Minecraft.getInstance().player.getInventory().getItem(slotIndex);

                String displayName = stack.getHoverName().getString();
                if (displayName.length() > 16) displayName = displayName.substring(0, 14) + "...";

                this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(
                    listX, listY + i * 21, 95, 18, Component.literal(displayName), btn -> {
                        selectedInventorySlot = slotIndex;
                        this.init(); // Redibujar con el estado de selección actualizado
                    }
                ));
            }
        }

        // Scroll de la lista de items
        if (repairableArmorSlots.size() > 4) {
            this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(
                centerX - 40, listY, 15, 18, Component.literal("▲"), btn -> {
                    if (scrollOffset > 0) {
                        scrollOffset--;
                        this.init();
                    }
                }
            ));

            this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(
                centerX - 40, listY + 63, 15, 18, Component.literal("▼"), btn -> {
                    if (scrollOffset < repairableArmorSlots.size() - 4) {
                        scrollOffset++;
                        this.init();
                    }
                }
            ));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int cardWidth = 300;
        int cardHeight = 180;
        int x = centerX - cardWidth / 2;
        int y = centerY - cardHeight / 2 - 10;

        // --- DISEÑO DE PERGAMINO MEDIEVAL ---
        graphics.fill(x - 4, y - 4, x + cardWidth + 4, y + cardHeight + 4, 0xFF362819);
        graphics.fill(x - 2, y - 2, x + cardWidth + 2, y + cardHeight + 2, 0xFF4A3B2C);
        graphics.fill(x, y, x + cardWidth, y + cardHeight, 0xFFF3E5C8);
        graphics.fill(x + 3, y + 3, x + cardWidth - 3, y + cardHeight - 3, 0xFFEBDAB3);

        int innerBorderColor = 0xFF8F7051;
        graphics.fill(x + 5, y + 5, x + cardWidth - 5, y + 6, innerBorderColor);
        graphics.fill(x + 5, y + cardHeight - 6, x + cardWidth - 5, y + cardHeight - 5, innerBorderColor);
        graphics.fill(x + 5, y + 5, x + 6, y + cardHeight - 5, innerBorderColor);
        graphics.fill(x + cardWidth - 6, y + 5, x + cardWidth - 5, y + cardHeight - 5, innerBorderColor);

        // Títulos de la interfaz
        graphics.drawString(this.font, "§4REPARAR ARMADURA", centerX - this.font.width("§4REPARAR ARMADURA") / 2, y + 10, 0, false);
        graphics.drawString(this.font, "§8Restaura tus armaduras de inicio o de clase", centerX - this.font.width("§8Restaura tus armaduras de clase") / 2, y + 21, 0, false);

        // Línea divisoria de tinta
        graphics.fill(x + 12, y + 31, x + cardWidth - 12, y + 32, 0x884A3B2C);

        int panelY = y + 38;
        int panelWidth = 110;
        int panelHeight = 82;

        // Panel Izquierdo: Lista de Armaduras
        int p1X = x + 12;
        graphics.fill(p1X, panelY, p1X + panelWidth, panelY + panelHeight, 0xFFE5D5B0);
        graphics.fill(p1X + 1, panelY + 1, p1X + panelWidth - 1, panelY + 12, 0xFF8F7051);
        graphics.drawString(this.font, "§fARMADURAS DAÑADAS", p1X + 4, panelY + 3, 0xFFFFFFFF, false);

        // Panel Derecho: Detalles de Reparación
        int p2X = x + 178;
        graphics.fill(p2X, panelY, p2X + panelWidth, panelY + panelHeight, 0xFFE5D5B0);
        graphics.fill(p2X + 1, panelY + 1, p2X + panelWidth - 1, panelY + 12, 0xFF8F7051);
        graphics.drawString(this.font, "§fDETALLES", p2X + 4, panelY + 3, 0xFFFFFFFF, false);

        if (selectedInventorySlot != -1) {
            ItemStack selectedStack = Minecraft.getInstance().player.getInventory().getItem(selectedInventorySlot);
            String rawName = selectedStack.getHoverName().getString();
            if (rawName.length() > 16) rawName = rawName.substring(0, 14) + "...";

            int maxDamage = selectedStack.getMaxDamage();
            int currentDamage = maxDamage - selectedStack.getDamageValue();

            graphics.drawString(this.font, "§4" + rawName, p2X + 5, panelY + 18, 0, false);
            graphics.drawString(this.font, "§0Durabilidad:", p2X + 5, panelY + 32, 0, false);
            graphics.drawString(this.font, "§1" + currentDamage + " / " + maxDamage, p2X + 5, panelY + 42, 0, false);
            graphics.drawString(this.font, "§0Costo: §63 HIERRO", p2X + 5, panelY + 54, 0, false);

            boolean hasIron = playerIronCount >= 3;
            graphics.drawString(this.font, hasIron ? "§2✓ Materiales OK" : "§4✗ Falta Hierro (" + playerIronCount + "/3)", p2X + 5, panelY + 66, 0, false);
        } else {
            graphics.drawString(this.font, "§8Selecciona una", p2X + 5, panelY + 22, 0, false);
            graphics.drawString(this.font, "§8pieza de la", p2X + 5, panelY + 34, 0, false);
            graphics.drawString(this.font, "§8lista izquierda.", p2X + 5, panelY + 46, 0, false);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
