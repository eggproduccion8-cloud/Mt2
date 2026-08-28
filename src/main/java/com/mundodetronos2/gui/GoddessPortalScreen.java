package com.mundodetronos2.gui;

import com.mundodetronos2.network.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class GoddessPortalScreen extends Screen {

    private boolean hasOffering = false;

    public GoddessPortalScreen() {
        super(Component.literal("Portal de la Diosa María"));
        checkPlayerInventoryForOffering();
    }

    private void checkPlayerInventoryForOffering() {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            for (ItemStack stack : player.getInventory().items) {
                if (stack.getItem() == Items.POPPY && stack.hasTag() && stack.getTag().getBoolean("IsGoddessOffering")) {
                    this.hasOffering = true;
                    break;
                }
            }
        }
    }

    @Override
    protected void init() {
        this.clearWidgets();

        int buttonWidth = 150;
        int buttonHeight = 20;
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Botones transparentes medievales elegantes
        TransparentButton enterBtn = new TransparentButton(
            centerX - buttonWidth / 2, centerY + 15, buttonWidth, buttonHeight,
            Component.literal("Depositar y Entrar"), btn -> {
                NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SEnterGoddessDimensionPacket());
                this.onClose();
            }
        );

        enterBtn.active = this.hasOffering;
        this.addRenderableWidget(enterBtn);

        this.addRenderableWidget(new TransparentButton(
            centerX - buttonWidth / 2, centerY + 40, buttonWidth, buttonHeight,
            Component.literal("Cerrar"), btn -> this.onClose()
        ));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int widthBox = 200;
        int heightBox = 110;
        int x = centerX - widthBox / 2;
        int y = centerY - heightBox / 2 - 10;

        // --- DISEÑO DE PERGAMINO MEDIEVAL ANTES DE LA REVOLUCIÓN ---
        // 1. Borde de madera oscura del marco
        graphics.fill(x - 4, y - 4, x + widthBox + 4, y + heightBox + 4, 0xFF362819);
        graphics.fill(x - 2, y - 2, x + widthBox + 2, y + heightBox + 2, 0xFF4A3B2C);

        // 2. Fondo de papel pergamino antiguo/cálido (Warm Rustic Beige)
        graphics.fill(x, y, x + widthBox, y + heightBox, 0xFFF3E5C8);
        graphics.fill(x + 3, y + 3, x + widthBox - 3, y + heightBox - 3, 0xFFEBDAB3);

        // Borde interior de marco fino marrón rústico
        int innerBorderColor = 0xFF8F7051;
        graphics.fill(x + 5, y + 5, x + widthBox - 5, y + 6, innerBorderColor);
        graphics.fill(x + 5, y + heightBox - 6, x + widthBox - 5, y + heightBox - 5, innerBorderColor);
        graphics.fill(x + 5, y + 5, x + 6, y + heightBox - 5, innerBorderColor);
        graphics.fill(x + widthBox - 6, y + 5, x + widthBox - 5, y + heightBox - 5, innerBorderColor);

        // Título de la interfaz de tinta oscura
        graphics.drawCenteredString(this.font, "§4§lPORTAL DE LA DIOSA", centerX, y + 15, 0);
        graphics.drawCenteredString(this.font, "§8Entrega tu Ofrenda para cruzar", centerX, y + 27, 0);

        // Estado de la ofrenda en el inventario
        if (this.hasOffering) {
            graphics.drawCenteredString(this.font, "§2✔ ¡Ofrenda de Rosa detectada!", centerX, y + 48, 0);
        } else {
            graphics.drawCenteredString(this.font, "§4✗ Falta la Ofrenda de la Diosa", centerX, y + 48, 0);
            graphics.drawCenteredString(this.font, "§8(Amapola: 'Ofrenda de la Diosa María')", centerX, y + 58, 0);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
