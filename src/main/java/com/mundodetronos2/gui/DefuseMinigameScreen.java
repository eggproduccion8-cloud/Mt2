package com.mundodetronos2.gui;

import com.mundodetronos2.network.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.*;

public class DefuseMinigameScreen extends Screen {

    private final BlockPos chargePos;
    private int correctKeysPressed = 0;
    private int expectedKeyCode;
    private String expectedKeyName = "";

    private static final Map<Integer, String> KEY_NAMES = new HashMap<>();
    static {
        KEY_NAMES.put(org.lwjgl.glfw.GLFW.GLFW_KEY_W, "W");
        KEY_NAMES.put(org.lwjgl.glfw.GLFW.GLFW_KEY_A, "A");
        KEY_NAMES.put(org.lwjgl.glfw.GLFW.GLFW_KEY_S, "S");
        KEY_NAMES.put(org.lwjgl.glfw.GLFW.GLFW_KEY_D, "D");
        KEY_NAMES.put(org.lwjgl.glfw.GLFW.GLFW_KEY_Q, "Q");
        KEY_NAMES.put(org.lwjgl.glfw.GLFW.GLFW_KEY_E, "E");
        KEY_NAMES.put(org.lwjgl.glfw.GLFW.GLFW_KEY_R, "R");
        KEY_NAMES.put(org.lwjgl.glfw.GLFW.GLFW_KEY_F, "F");
        KEY_NAMES.put(org.lwjgl.glfw.GLFW.GLFW_KEY_G, "G");
        KEY_NAMES.put(org.lwjgl.glfw.GLFW.GLFW_KEY_Z, "Z");
        KEY_NAMES.put(org.lwjgl.glfw.GLFW.GLFW_KEY_X, "X");
        KEY_NAMES.put(org.lwjgl.glfw.GLFW.GLFW_KEY_C, "C");
        KEY_NAMES.put(org.lwjgl.glfw.GLFW.GLFW_KEY_V, "V");
        KEY_NAMES.put(org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE, "ESPACIO");
        KEY_NAMES.put(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT, "SHIFT");
    }

    private static final List<Integer> KEY_CODES = new ArrayList<>(KEY_NAMES.keySet());
    private static final Random RANDOM = new Random();

    public DefuseMinigameScreen(BlockPos chargePos) {
        super(Component.literal("Desactivar Carga"));
        this.chargePos = chargePos;
        selectNextKey();
    }

    private void selectNextKey() {
        int index = RANDOM.nextInt(KEY_CODES.size());
        this.expectedKeyCode = KEY_CODES.get(index);
        this.expectedKeyName = KEY_NAMES.get(this.expectedKeyCode);
    }

    @Override
    protected void init() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Botón Salir (por si quieren abandonar)
        this.addRenderableWidget(Button.builder(Component.literal("Abandonar"), btn -> this.onClose())
                .bounds(centerX - 50, centerY + 55, 100, 20)
                .build());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        // Verificar si la tecla presionada es la correcta
        if (keyCode == expectedKeyCode) {
            correctKeysPressed++;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.get(), 1.4F + (correctKeysPressed * 0.05F)));

            if (correctKeysPressed >= 15) {
                // Éxito completo! Enviar paquete al servidor
                NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SDefuseSuccessPacket(chargePos));
                this.onClose();
            } else {
                selectNextKey();
            }
        } else {
            // Error: reiniciar a 0 las 15 teclas y cambiar la tecla actual
            correctKeysPressed = 0;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.get(), 0.6F));
            selectNextKey();
        }

        return true;
    }

    @Override
    public void tick() {
        super.tick();
        // Si ya no hay un ataque activo en el cliente, cerrar la pantalla (la carga explotó o fue desactivada)
        if (!com.mundodetronos2.client.ClientPacketHandler.isAttackActive()) {
            this.onClose();
        }
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

        // Título del Minijuego
        graphics.drawCenteredString(this.font, "§4§lDESACTIVACIÓN DE CARGA", centerX, centerY - 65, 0);

        // Cuenta regresiva de la carga en curso
        int seconds = com.mundodetronos2.client.ClientPacketHandler.getAttackSecondsLeft();
        String timerStr = "TIEMPO RESTANTE: 00:" + String.format("%02d", seconds);
        graphics.drawCenteredString(this.font, "§c§l" + timerStr, centerX, centerY - 45, 0);

        // Instrucción de la tecla
        graphics.drawCenteredString(this.font, "§0PRESIONA LA TECLA:", centerX, centerY - 15, 0);
        graphics.drawCenteredString(this.font, "§6§l" + expectedKeyName, centerX, centerY + 2, 0);

        // Progreso de desactivación
        String progressStr = "PROGRESO: " + correctKeysPressed + " / 15";
        graphics.drawCenteredString(this.font, "§1" + progressStr, centerX, centerY + 25, 0);

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
