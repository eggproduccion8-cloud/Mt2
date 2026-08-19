package com.mundodetronos2.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class GoddessDeathRebirthScreen extends Screen {

    private int ticks = 0;
    private int typewriterTicks = 0;
    private int typewriterCharIndex = 0;
    private String goddessMessage = "Tu viaje terrenal no ha terminado aún, alma valiente...\n\nDespierta y reclama el trono de tu reino.";

    private float heartbeatPulseAlpha = 0.0f;
    private int ticksSinceLastHeartbeat = 0;

    public GoddessDeathRebirthScreen() {
        super(Component.literal("Resurrección de la Diosa María"));
        com.mundodetronos2.client.ClientEvents.cinematicActive = true;
    }

    @Override
    protected void init() {
        this.clearWidgets();
    }

    @Override
    public void tick() {
        ticks++;
        typewriterTicks++;
        ticksSinceLastHeartbeat++;

        Minecraft mc = Minecraft.getInstance();

        // Atenuar pulso visual progresivamente
        if (heartbeatPulseAlpha > 0.0f) {
            heartbeatPulseAlpha -= 0.05f;
            if (heartbeatPulseAlpha < 0.0f) heartbeatPulseAlpha = 0.0f;
        }

        // Tipear texto lentamente
        if (typewriterTicks % 2 == 0 && typewriterCharIndex < goddessMessage.length()) {
            typewriterCharIndex++;
            char c = goddessMessage.charAt(typewriterCharIndex - 1);
            if (c != ' ' && c != '\n' && mc.player != null) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.STONE_BUTTON_CLICK_ON, 1.4F));
            }
        }

        // Latidos ralentizándose y calmándose a medida que renace (de 15 ticks a 30 ticks)
        int heartbeatInterval = 15 + (int) ((ticks / 120.0f) * 15.0f);
        if (ticksSinceLastHeartbeat >= heartbeatInterval) {
            ticksSinceLastHeartbeat = 0;
            heartbeatPulseAlpha = 0.9f;

            if (mc.player != null) {
                // Tono más bajo y relajado para denotar calma tras la muerte
                mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.8F, 0.4F, false);
            }
        }

        // Chispas flotantes alrededor
        if (mc.level != null && mc.player != null && Math.random() < 0.4) {
            mc.level.addParticle(ParticleTypes.WITCH,
                    mc.player.getX() + (Math.random() - 0.5D) * 4.0D,
                    mc.player.getY() + 1.0D + (Math.random() - 0.5D) * 2.0D,
                    mc.player.getZ() + (Math.random() - 0.5D) * 4.0D,
                    0.0D, 0.02D, 0.0D);
        }

        // Cerrar después de 6 segundos (120 ticks) coincidiendo con el fin del espectador fantasma
        if (ticks >= 120) {
            this.onClose();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int w = this.width;
        int h = this.height;
        int centerX = w / 2;
        int centerY = h / 2;

        // Fondo cósmico oscuro con estrellas lentas
        graphics.fill(0, 0, w, h, 0xFF08060A);

        // Estrellas titilantes decorativas en la pantalla
        for (int i = 0; i < 8; i++) {
            int starX = (int) (Math.sin(ticks * 0.02 + i) * centerX * 0.7 + centerX);
            int starY = (int) (Math.cos(ticks * 0.015 + i * 2) * centerY * 0.7 + centerY);
            float starAlpha = (float) (Math.sin(ticks * 0.05 + i) + 1.0) / 2.0f;
            int starColor = ((int) (starAlpha * 120.0f) << 24) | 0xD4AF37; // Estrella dorada
            graphics.fill(starX, starY, starX + 2, starY + 2, starColor);
        }

        // Renderizar vignette de latido de corazón de resurrección
        if (heartbeatPulseAlpha > 0.0f) {
            int color = ((int) (heartbeatPulseAlpha * 90.0f) << 24) | 0x800080; // Violeta místico
            int border = (int) (20 * heartbeatPulseAlpha);
            graphics.fill(0, 0, w, border, color);
            graphics.fill(0, h - border, w, h, color);
            graphics.fill(0, border, border, h - border, color);
            graphics.fill(w - border, border, w, h - border, color);
        }

        // Título celestial de la Diosa María
        graphics.drawCenteredString(this.font, "§d§l✦ DIOSA MARÍA ✦", centerX, centerY - 45, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, "§7- MUNDO DE TRONOS 2 -", centerX, centerY - 32, 0xFF8A6F8A);

        // Texto de resurrección con auto-salto de línea
        String currentText = goddessMessage.substring(0, typewriterCharIndex);
        String[] lines = currentText.split("\n");
        int textY = centerY;
        for (String line : lines) {
            int lineW = this.font.width(line);
            graphics.drawString(this.font, "§d§o" + line, centerX - lineW / 2, textY, 0xFFFFFFFF, true);
            textY += 13;
        }

        // Párpados de ojo abriéndose lentamente al final de los 6 segundos
        if (ticks > 90) {
            float alpha = (ticks - 90) / 30.0f; // Último segundo
            int eyeLidHeight = (int) (h * 0.5f * (1.0f - alpha));
            graphics.fill(0, 0, w, eyeLidHeight, 0xFF000000); // Párpado superior
            graphics.fill(0, h - eyeLidHeight, w, h, 0xFF000000); // Párpado inferior
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void onClose() {
        com.mundodetronos2.client.ClientEvents.cinematicActive = false;
        super.onClose();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
