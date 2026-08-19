package com.mundodetronos2.gui;

import com.mundodetronos2.network.NetworkManager;
import com.mundodetronos2.role.RoleDefinition;
import com.mundodetronos2.role.RoleRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class RoleSelectionScreen extends Screen {

    public RoleSelectionScreen() {
        super(Component.translatable("gui.mundodetronos2.role_selection.title"));
    }

    @Override
    protected void init() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int menuWidth = 300;
        int menuHeight = 210;
        int x = centerX - menuWidth / 2;
        int y = centerY - menuHeight / 2 - 10;

        // Botón [X] para cerrar en la esquina superior derecha del pergamino
        this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(
            x + menuWidth - 22, y + 8, 14, 14,
            Component.literal("X"), btn -> this.onClose()
        ));

        // Cuadrícula dinámica para los 7 roles
        int gridStartX = centerX - 132;
        int gridStartY = centerY - 45;

        String[][] layout = {
            {"berserker", "0", "0"},
            {"warrior", "1", "0"},
            {"mage", "2", "0"},
            {"archer", "0", "1"},
            {"paladin", "1", "1"},
            {"draconico", "2", "1"},
            {"clerigo", "1", "2"} // Centrado en la tercera fila
        };

        for (String[] cell : layout) {
            String roleId = cell[0];
            int col = Integer.parseInt(cell[1]);
            int row = Integer.parseInt(cell[2]);

            RoleDefinition def = RoleRegistry.getRole(roleId);
            if (def == null) continue;

            int bx;
            if (row == 2) {
                bx = centerX - 40; // Fila centrada
            } else {
                bx = gridStartX + col * (80 + 12);
            }
            int by = gridStartY + row * (40 + 8);

            this.addRenderableWidget(new RoleButton(bx, by, 80, 40, def, getIconForItem(roleId), btn -> {
                NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SSelectRolePacket(roleId));
                this.onClose();
            }));
        }
    }

    private net.minecraft.world.item.ItemStack getIconForItem(String roleId) {
        switch (roleId.toLowerCase()) {
            case "berserker": return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHERITE_AXE);
            case "warrior": return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
            case "mage": return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.AMETHYST_SHARD);
            case "archer": return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOW);
            case "paladin": return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SHIELD);
            case "draconico": return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DRAGON_BREATH);
            case "clerigo": return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLDEN_APPLE);
            default: return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int menuWidth = 300;
        int menuHeight = 210;
        int x = centerX - menuWidth / 2;
        int y = centerY - menuHeight / 2 - 10;

        // --- DISEÑO DE PERGAMINO MEDIEVAL ANTES DE LA REVOLUCIÓN ---
        // 1. Borde de madera oscura del marco del menú
        graphics.fill(x - 4, y - 4, x + menuWidth + 4, y + menuHeight + 4, 0xFF362819);
        graphics.fill(x - 2, y - 2, x + menuWidth + 2, y + menuHeight + 2, 0xFF4A3B2C);

        // 2. Fondo de papel pergamino antiguo/cálido (Warm Rustic Beige)
        graphics.fill(x, y, x + menuWidth, y + menuHeight, 0xFFF3E5C8);
        graphics.fill(x + 3, y + 3, x + menuWidth - 3, y + menuHeight - 3, 0xFFEBDAB3);

        // Borde interior de marco fino marrón rústico
        int innerBorderColor = 0xFF8F7051;
        graphics.fill(x + 5, y + 5, x + menuWidth - 5, y + 6, innerBorderColor);
        graphics.fill(x + 5, y + menuHeight - 6, x + menuWidth - 5, y + menuHeight - 5, innerBorderColor);
        graphics.fill(x + 5, y + 5, x + 6, y + menuHeight - 5, innerBorderColor);
        graphics.fill(x + menuWidth - 6, y + 5, x + menuWidth - 5, y + menuHeight - 5, innerBorderColor);

        // Título celestial de tinta oscura con diseño elegante (Sin sombras)
        graphics.drawCenteredString(this.font, "§4§l✦ DIOSA MARÍA ✦", centerX, centerY - 82, 0);
        graphics.drawCenteredString(this.font, "§8¿QUÉ CAMINO ELEGIRÁS?", centerX, centerY - 69, 0);

        super.render(graphics, mouseX, mouseY, partialTicks);

        // Renderizar Tooltip si se pasa el ratón sobre algún rol
        for (net.minecraft.client.gui.components.Renderable widget : this.renderables) {
            if (widget instanceof RoleButton btn) {
                if (mouseX >= btn.getX() && mouseY >= btn.getY() && mouseX < btn.getX() + btn.getWidth() && mouseY < btn.getY() + btn.getHeight()) {
                    List<Component> tooltipText = new ArrayList<>();
                    tooltipText.add(Component.literal("§l" + btn.role.getDisplayName().getString()).withStyle(net.minecraft.ChatFormatting.GOLD));
                    tooltipText.add(btn.role.getDescription());
                    graphics.renderComponentTooltip(this.font, tooltipText, mouseX, mouseY);
                }
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // --- CLASE DE BOTÓN DE ROL PERSONALIZADO ---
    public static class RoleButton extends Button {
        private final RoleDefinition role;
        private final net.minecraft.world.item.ItemStack itemIcon;
        private final int btnColor;

        public RoleButton(int x, int y, int width, int height, RoleDefinition role, net.minecraft.world.item.ItemStack itemIcon, OnPress onPress) {
            super(x, y, width, height, Component.literal(role.getDisplayName().getString()), onPress, DEFAULT_NARRATION);
            this.role = role;
            this.itemIcon = itemIcon;
            this.btnColor = role.getColor();
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            if (!this.visible) return;

            boolean hovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;

            // Borde brillante con color de la clase si está hovered
            int borderColor = hovered ? 0xFFFFFFFF : (0xFF000000 | btnColor);
            int bgColor = hovered ? (0x44000000 | btnColor) : 0x1A000000;

            // Dibujar fondo y bordes
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColor);
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, borderColor);
            graphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, borderColor);
            graphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.height, borderColor);
            graphics.fill(this.getX() + this.width - 1, this.getY(), this.getX() + this.width, this.getY() + this.height, borderColor);

            // Icono del rol
            graphics.renderFakeItem(itemIcon, this.getX() + (this.width - 16) / 2, this.getY() + 4);

            // Texto de la clase
            int textColor = hovered ? 0xFFFFFFFF : 0xFFDDDDDD;
            graphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), this.getX() + this.width / 2, this.getY() + 24, textColor);
        }
    }
}
