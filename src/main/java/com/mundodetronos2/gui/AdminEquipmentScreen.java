package com.mundodetronos2.gui;

import com.mundodetronos2.network.NetworkManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class AdminEquipmentScreen extends AbstractContainerScreen<AdminEquipmentMenu> {

    public AdminEquipmentScreen(AdminEquipmentMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 196;
        this.imageHeight = 450;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        int startX = this.leftPos;
        int startY = this.topPos;

        // Add role select buttons above the container
        int btnW = 44;
        int btnH = 14;
        int spacing = 3;

        // Row 1: BERSERKER, GUERRERO, MAGO, ARQUERO
        String[] row1Roles = {"berserker", "warrior", "mage", "archer"};
        String[] row1Labels = {"Ber", "Gue", "Mag", "Arq"};
        for (int i = 0; i < 4; i++) {
            final String role = row1Roles[i];
            this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(
                startX + 6 + i * (btnW + spacing), startY - 32, btnW, btnH, Component.literal(row1Labels[i]), btn -> {
                    changeRole(role);
                }
            ));
        }

        // Row 2: PALADÍN, DRACÓNICO, CLÉRIGO
        String[] row2Roles = {"paladin", "draconico", "clerigo"};
        String[] row2Labels = {"Pal", "Dra", "Cle"};
        for (int i = 0; i < 3; i++) {
            final String role = row2Roles[i];
            this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(
                startX + 6 + i * (btnW + spacing), startY - 16, btnW, btnH, Component.literal(row2Labels[i]), btn -> {
                    changeRole(role);
                }
            ));
        }

        // Add [CERRAR] button
        this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(
            startX + 140, startY - 16, 50, btnH, Component.literal("Cerrar"), btn -> this.onClose()
        ));
    }

    private void changeRole(String role) {
        NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SChangeAdminRoleMenuPacket(role));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        this.renderBackground(graphics);

        int startX = this.leftPos;
        int startY = this.topPos;

        // Draw parchment styled border background (Parchment beige)
        graphics.fill(startX - 4, startY - 4, startX + this.imageWidth + 4, startY + this.imageHeight + 4, 0xFF362819); // Madera
        graphics.fill(startX - 2, startY - 2, startX + this.imageWidth + 2, startY + this.imageHeight + 2, 0xFF4A3B2C);
        graphics.fill(startX, startY, startX + this.imageWidth, startY + this.imageHeight, 0xFFF3E5C8); // Pergamino rústico
        graphics.fill(startX + 3, startY + 3, startX + this.imageWidth - 3, startY + this.imageHeight - 3, 0xFFEBDAB3);

        // Draw 200 slot box borders (10 columns x 20 rows)
        int slotStartY = startY + 18;
        for (int row = 0; row < 20; row++) {
            for (int col = 0; col < 10; col++) {
                int sx = startX + 8 + col * 18;
                int sy = slotStartY + row * 18;
                graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF8F7051);
                graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFFE5D5B0);
            }
        }

        // Draw player inventory slot box borders (9 columns x 3 rows)
        int invStartY = slotStartY + (20 * 18) + 12;
        int invStartX = startX + 8 + 9;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = invStartX + col * 18;
                int sy = invStartY + row * 18;
                graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF8F7051);
                graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFFE5D5B0);
            }
        }

        // Draw player hotbar slot box borders (9 slots)
        int hotbarStartY = invStartY + 58;
        for (int col = 0; col < 9; col++) {
            int sx = invStartX + col * 18;
            int sy = hotbarStartY;
            graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF8F7051);
            graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFFE5D5B0);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        String roleText = "EQUIPAMIENTO: " + this.menu.getCurrentRole().toUpperCase();
        graphics.drawString(this.font, "§4" + roleText, 8, 6, 0, false);

        String invText = "INVENTARIO";
        graphics.drawString(this.font, "§0" + invText, 17, this.inventoryLabelY - 10, 0, false);
    }
}
