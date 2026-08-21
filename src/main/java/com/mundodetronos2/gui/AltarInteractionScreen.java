package com.mundodetronos2.gui;

import com.mundodetronos2.network.NetworkManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AltarInteractionScreen extends Screen {

    private final boolean hasRole;

    public AltarInteractionScreen(boolean hasRole) {
        super(Component.literal("Altar de la Diosa María"));
        this.hasRole = hasRole;
    }

    @Override
    protected void init() {
        this.clearWidgets();

        int buttonWidth = 140;
        int buttonHeight = 20;
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (!hasRole) {
            this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(
                centerX - buttonWidth / 2, centerY - 35, buttonWidth, buttonHeight,
                Component.literal("Elegir Mi Rol"), btn -> {
                    this.minecraft.setScreen(new RoleSelectionScreen());
                }
            ));
        } else {
            this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(
                centerX - buttonWidth / 2, centerY - 35, buttonWidth, buttonHeight,
                Component.literal("Ver Habilidades"), btn -> {
                    NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SOpenSkillTreePacket());
                    this.onClose();
                }
            ));
        }

        // Obtener Guía
        this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(
            centerX - buttonWidth / 2, centerY - 10, buttonWidth, buttonHeight,
            Component.literal("Obtener Guía de Diosa"), btn -> {
                NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SClaimGoddessBookPacket());
                this.onClose();
            }
        ));

        this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(
            centerX - buttonWidth / 2, centerY + 15, buttonWidth, buttonHeight,
            Component.literal("Salir de la Dimensión"), btn -> {
                NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SExitRoleDimensionPacket());
                this.onClose();
            }
        ));

        // Close button
        this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(
            centerX - buttonWidth / 2, centerY + 45, buttonWidth, buttonHeight,
            Component.literal("Cerrar"), btn -> this.onClose()
        ));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int menuWidth = 220;
        int menuHeight = 160;
        int x = centerX - menuWidth / 2;
        int y = centerY - menuHeight / 2 - 10;

        // --- DISEÑO DE PERGAMINO MEDIEVAL ANTES DE LA REVOLUCIÓN ---
        graphics.fill(x - 4, y - 4, x + menuWidth + 4, y + menuHeight + 4, 0xFF362819);
        graphics.fill(x - 2, y - 2, x + menuWidth + 2, y + menuHeight + 2, 0xFF4A3B2C);
        graphics.fill(x, y, x + menuWidth, y + menuHeight, 0xFFF3E5C8);
        graphics.fill(x + 3, y + 3, x + menuWidth - 3, y + menuHeight - 3, 0xFFEBDAB3);

        int innerBorderColor = 0xFF8F7051;
        graphics.fill(x + 5, y + 5, x + menuWidth - 5, y + 6, innerBorderColor);
        graphics.fill(x + 5, y + menuHeight - 6, x + menuWidth - 5, y + menuHeight - 5, innerBorderColor);
        graphics.fill(x + 5, y + 5, x + 6, y + menuHeight - 5, innerBorderColor);
        graphics.fill(x + menuWidth - 6, y + 5, x + menuWidth - 5, y + menuHeight - 5, innerBorderColor);

        graphics.drawCenteredString(this.font, "§4§l✦ DIOSA MARÍA ✦", centerX, centerY - 55, 0);
        graphics.drawCenteredString(this.font, "§8¿QUÉ DESEAS HACER?", centerX, centerY - 42, 0);

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
