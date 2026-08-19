package com.mundodetronos2.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class HeraldoTutorialScreen extends Screen {

    private final String npcName;
    private int currentPage = 0;

    private static final String[] PAGES = {
            "§4§lPASO 1: EQUIPOS§r\n\nPresiona §1§lK§r para abrir el menú principal.\n\nAhí podrás §cCrear un Equipo§r o buscar equipos existentes para unirte.\n\n¡Un equipo te permite registrar bases!",
            "§4§lPASO 2: EL TRONO§r\n\nComo líder de equipo, puedes colocar un bloque (ej: Netherite) y usar §c/tronos setthrone§r para registrar tu trono.\n\n¡Defiéndelo de asaltos enemigos con tu vida!",
            "§4§lPASO 3: LA OFRENDA§r\n\nPara hablar con la Diosa María, necesitas una §dOfrenda de la Diosa María§r.\n\nConsigue un §6Pollo§r e intercámbiaselo al NPC §eSacerdote§r para recibirla.",
            "§4§lPASO 4: EL PORTAL§r\n\nCon la ofrenda en tu inventario, haz clic derecho en el §6Bloque de Oro§r (Portal de la Diosa) para viajar al templo en la dimensión de roles.",
            "§4§lPASO 5: LA DIOSA§r\n\nAl ingresar al templo, la Diosa María hablará contigo y te revelará los 7 roles espirituales disponibles en este mundo sagrado.",
            "§4§lPASO 6: TU ROL§r\n\nInteractúa con el Altar de Huevo de Dragón para elegir tu rol definitivo.\n\nCompleta el minijuego QTE de la barra espaciadora para ser investido.",
            "§4§lPASO 7: TU CARNET§r\n\nAl elegir tu rol, recibirás un carnet único vinculado a tu alma.\n\nEste carnet te otorga libre entrada al portal de oro de forma gratuita.",
            "§4§lPASO 8: HABILIDADES§r\n\nPresiona §d§lM§r para abrir el árbol de habilidades de tu rol.\n\n¡Usa tus Skill Points ganados en misiones para aprender destrezas supremas!"
    };

    public HeraldoTutorialScreen(String npcName) {
        super(Component.literal("Guía del Heraldo"));
        this.npcName = npcName;
    }

    @Override
    protected void init() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Botón Anterior
        this.addRenderableWidget(Button.builder(Component.literal("←"), btn -> {
            if (currentPage > 0) {
                currentPage--;
                this.init();
            }
        })
        .bounds(centerX - 95, centerY + 50, 30, 20)
        .build());

        // Botón Siguiente / Cerrar
        String nextText = currentPage < PAGES.length - 1 ? "→" : "Salir";
        this.addRenderableWidget(Button.builder(Component.literal(nextText), btn -> {
            if (currentPage < PAGES.length - 1) {
                currentPage++;
                this.init();
            } else {
                this.onClose();
            }
        })
        .bounds(centerX + 65, centerY + 50, 30, 20)
        .build());

        // Botón Volver al diálogo
        this.addRenderableWidget(Button.builder(Component.literal("Volver"), btn -> {
            this.minecraft.setScreen(new NpcDialogueScreen(npcName));
        })
        .bounds(centerX - 40, centerY + 50, 80, 20)
        .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int menuWidth = 240;
        int menuHeight = 160;
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

        // Indicador de página
        String pageStr = "Pág " + (currentPage + 1) + " / " + PAGES.length;
        graphics.drawCenteredString(this.font, "§d" + pageStr, centerX, centerY - 65, 0);

        // Contenido de la página (multi-línea de forma segura)
        String content = PAGES[currentPage];
        int textY = centerY - 45;
        for (String line : content.split("\n")) {
            graphics.drawCenteredString(this.font, line, centerX, textY, 0);
            textY += 12;
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
