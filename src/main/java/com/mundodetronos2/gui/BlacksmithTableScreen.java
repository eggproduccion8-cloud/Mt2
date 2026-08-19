package com.mundodetronos2.gui;

import com.mundodetronos2.network.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class BlacksmithTableScreen extends Screen {

    private final String targetWord = "FORJAR";
    private int typedIndex = 0;

    public BlacksmithTableScreen() {
        super(Component.literal("Mesa del Herrero del Rey"));
    }

    @Override
    protected void init() {
        this.clearWidgets();
        typedIndex = 0; // Reiniciar secuencia al abrir
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (typedIndex < targetWord.length()) {
            char targetChar = targetWord.charAt(typedIndex);

            // Convertir keyCode a char para validación
            char pressedChar = Character.toUpperCase((char) keyCode);

            // También mapear el keyCode directamente para caracteres alfabéticos estándar
            // En GLFW, las letras A-Z se mapean de 65 a 90, lo cual coincide exactamente con sus ASCII
            if (keyCode >= 65 && keyCode <= 90) {
                pressedChar = (char) keyCode;
            }

            if (pressedChar == targetChar) {
                typedIndex++;

                // Reproducir golpe metálico de yunque con tono alto en el cliente
                Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.ANVIL_HIT, 1.4F + (typedIndex * 0.1F))
                );

                if (typedIndex >= targetWord.length()) {
                    // Secuencia completada: Reclamar el kit del rey en el servidor
                    NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SClaimBlacksmithKitPacket());
                    this.onClose();
                }
                return true;
            } else {
                // Sonido sutil de error si se presiona la tecla equivocada
                Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.STONE_HIT, 0.8F)
                );
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int cardWidth = 240;
        int cardHeight = 140;
        int x = centerX - cardWidth / 2;
        int y = centerY - cardHeight / 2 - 10;

        // --- DISEÑO DE PERGAMINO MEDIEVAL ANTES DE LA REVOLUCIÓN ---
        // 1. Borde de madera oscura del marco
        graphics.fill(x - 4, y - 4, x + cardWidth + 4, y + cardHeight + 4, 0xFF362819);
        graphics.fill(x - 2, y - 2, x + cardWidth + 2, y + cardHeight + 2, 0xFF4A3B2C);

        // 2. Fondo de papel pergamino antiguo/cálido (Warm Rustic Beige)
        graphics.fill(x, y, x + cardWidth, y + cardHeight, 0xFFF3E5C8);
        graphics.fill(x + 3, y + 3, x + cardWidth - 3, y + cardHeight - 3, 0xFFEBDAB3);

        // Borde interior de marco fino marrón rústico
        int innerBorderColor = 0xFF8F7051;
        graphics.fill(x + 5, y + 5, x + cardWidth - 5, y + 6, innerBorderColor);
        graphics.fill(x + 5, y + cardHeight - 6, x + cardWidth - 5, y + cardHeight - 5, innerBorderColor);
        graphics.fill(x + 5, y + 5, x + 6, y + cardHeight - 5, innerBorderColor);
        graphics.fill(x + cardWidth - 6, y + 5, x + cardWidth - 5, y + cardHeight - 5, innerBorderColor);

        // Título de la interfaz de tinta oscura (Sin sombra)
        graphics.drawString(this.font, "§4MESA DE TRABAJO DEL REY", centerX - this.font.width("§4MESA DE TRABAJO DEL REY") / 2, y + 10, 0, false);
        graphics.drawString(this.font, "§8Obtén tu equipamiento básico de supervivencia", centerX - this.font.width("§8Obtén tu equipamiento básico de supervivencia") / 2, y + 21, 0, false);

        // Línea divisoria de tinta
        graphics.fill(x + 12, y + 31, x + cardWidth - 12, y + 32, 0x884A3B2C);

        // Instrucción interactiva
        graphics.drawString(this.font, "§0Presiona la secuencia de letras para forjar:", centerX - 105, centerY - 15, 0, false);

        // Dibujar el estado actual de la palabra forjada
        int startX = centerX - 60;
        int wordY = centerY + 8;

        for (int i = 0; i < targetWord.length(); i++) {
            char c = targetWord.charAt(i);
            String displayChar = String.valueOf(c);

            int color;
            if (i < typedIndex) {
                displayChar = "§2" + displayChar; // Correcto (Verde)
                color = 0xFF228B22;
            } else if (i == typedIndex) {
                displayChar = "§6" + displayChar; // Siguiente a presionar (Oro)
                color = 0xFFD4AF37;
                // Efecto de pulso en la tecla activa
                if (System.currentTimeMillis() % 500 < 250) {
                    graphics.fill(startX + i * 20 - 1, wordY - 2, startX + i * 20 + 9, wordY + 9, 0x33D4AF37);
                }
            } else {
                displayChar = "§8" + displayChar; // Pendiente (Gris)
                color = 0xFF777777;
            }

            graphics.drawString(this.font, displayChar, startX + i * 20, wordY, 0, false);
            // Pequeña línea debajo de cada letra
            graphics.fill(startX + i * 20, wordY + 10, startX + i * 20 + 8, wordY + 11, 0x554A3B2C);
        }

        // Indicador de avance
        String progressStr = "Progreso de forjado: " + (int)((typedIndex / (float)targetWord.length()) * 100) + "%";
        graphics.drawString(this.font, "§5" + progressStr, centerX - this.font.width(progressStr) / 2, centerY + 36, 0, false);

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
