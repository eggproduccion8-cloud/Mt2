package com.mundodetronos2.gui;

import com.mundodetronos2.network.NetworkManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class NpcTradeScreen extends Screen {

    private final String npcName;

    public NpcTradeScreen(String npcName) {
        super(Component.literal("Comerciar - " + npcName));
        this.npcName = npcName;
    }

    @Override
    protected void init() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Compras de Comercio
        // Trade 0: 1 Esmeralda + 10 Puntos -> Ofrenda Diosa María
        this.addRenderableWidget(Button.builder(Component.literal("Canjear"), btn -> {
            NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2STradeNpcPacket("", "trade_0"));
            this.onClose();
        })
        .bounds(centerX + 30, centerY - 35, 60, 20)
        .build());

        // Trade 1: 1 Ingot de Oro + 20 Puntos -> Manzana Dorada
        this.addRenderableWidget(Button.builder(Component.literal("Canjear"), btn -> {
            NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2STradeNpcPacket("", "trade_1"));
            this.onClose();
        })
        .bounds(centerX + 30, centerY - 5, 60, 20)
        .build());

        // Trade 2: 5 Ingot de Hierro + 5 Puntos -> Lapis Lázuli
        this.addRenderableWidget(Button.builder(Component.literal("Canjear"), btn -> {
            NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2STradeNpcPacket("", "trade_2"));
            this.onClose();
        })
        .bounds(centerX + 30, centerY + 25, 60, 20)
        .build());

        // Botón Volver
        this.addRenderableWidget(Button.builder(Component.literal("Volver"), btn -> {
            this.minecraft.setScreen(new NpcDialogueScreen(npcName));
        })
        .bounds(centerX - 50, centerY + 65, 100, 20)
        .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int menuWidth = 260;
        int menuHeight = 180;
        int x = centerX - menuWidth / 2;
        int y = centerY - menuHeight / 2 - 10;

        // --- DISEÑO DE PERGAMINO MEDIEVAL ---
        graphics.fill(x - 4, y - 4, x + menuWidth + 4, y + menuHeight + 4, 0xFF362819);
        graphics.fill(x - 2, y - 2, x + menuWidth + 2, y + menuHeight + 2, 0xFF4A3B2C);
        graphics.fill(x, y, x + menuWidth, y + menuHeight, 0xFFF3E5C8);
        graphics.fill(x + 3, y + 3, x + menuWidth - 3, y + menuHeight - 3, 0xFFEBDAB3);

        int innerBorderColor = 0xFF8F7051;
        graphics.fill(x + 5, y + 5, x + menuWidth - 5, y + 6, innerBorderColor);
        graphics.fill(x + 5, y + menuHeight - 6, x + menuWidth - 5, y + menuHeight - 5, innerBorderColor);
        graphics.fill(x + 5, y + 5, x + 6, y + menuHeight - 5, innerBorderColor);
        graphics.fill(x + menuWidth - 6, y + 5, x + menuWidth - 5, y + menuHeight - 5, innerBorderColor);

        // Título del Comercio
        graphics.drawCenteredString(this.font, "§4§lTIENDA DE " + npcName.toUpperCase(), centerX, centerY - 78, 0);

        // Trade 0 text
        graphics.drawString(this.font, "§01 Esmeralda + 10 Pts", centerX - 110, centerY - 30, 0, false);
        graphics.drawString(this.font, "§dOfrenda Diosa", centerX - 110, centerY - 20, 0, false);

        // Trade 1 text
        graphics.drawString(this.font, "§01 Lingote Oro + 20 Pts", centerX - 110, centerY, 0, false);
        graphics.drawString(this.font, "§6Manzana Dorada", centerX - 110, centerY + 10, 0, false);

        // Trade 2 text
        graphics.drawString(this.font, "§05 Lingotes Hierro + 5 Pts", centerX - 110, centerY + 30, 0, false);
        graphics.drawString(this.font, "§1Lapislázuli", centerX - 110, centerY + 40, 0, false);

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
