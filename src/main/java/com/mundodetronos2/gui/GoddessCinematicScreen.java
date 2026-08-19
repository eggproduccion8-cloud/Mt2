package com.mundodetronos2.gui;

import com.mundodetronos2.network.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class GoddessCinematicScreen extends Screen {

    private final String selectedRole;
    private int selectionTicks = 100; // 5 segundos de selección aura
    private int typewriterTicks = 0;
    private int typewriterCharIndex = 0;

    // Cuenta regresiva reducida a 15 segundos (300 ticks)
    private int countdownTicks = 300;
    private int ticksSinceLastHeartbeat = 0;

    // Progreso del minijuego de la barra de espacio (Spacebar mash)
    private float mashProgress = 0.0f;
    private boolean minigameWon = false;

    // Blackout tras llegar al trono (100 ticks = 5 segundos)
    private int postTeleportTicks = 100;

    // Apertura lenta de ojos (120 ticks = 6 segundos de transición de aclaramiento)
    private int awakeningTicks = 120;

    private String textGoddess = "";
    private String textAwakening = "";
    private String postTeleportMessage = "Ve con mi bendición, protector del trono. El destino de tu reino está en tus manos...";

    // Intensidad del pulso visual de latido (vignette)
    private float heartbeatPulseAlpha = 0.0f;

    private CinematicStage currentStage = CinematicStage.SELECTION_AURA;

    public enum CinematicStage {
        SELECTION_AURA,         // 5 segundos de latido y temblor
        BLACKOUT_TYPEWRITER,      // Typewriter de la Diosa María
        COUNTDOWN_TO_TELEPORT,    // 15 segundos con latidos acelerados y minijuego de masear ESPACIO
        POST_TELEPORT_BLACKOUT,   // Pantalla negra tras llegar, latidos y un mensaje de la Diosa
        EYE_AWAKENING           // Apertura lenta de ojos (fade out) con efecto cinemático
    }

    public GoddessCinematicScreen(String selectedRole) {
        super(Component.literal("Cinemática de la Diosa"));
        this.selectedRole = selectedRole;

        this.textGoddess = "DIOSA MARÍA\n────────────────────────\n\nHas sido elegido.\n\nTu destino ha sido marcado.\n\nAbre los ojos...";
        com.mundodetronos2.client.ClientEvents.cinematicActive = true;

        String roleSpanish = "Guerrero";
        if (selectedRole.equalsIgnoreCase("berserker")) roleSpanish = "Berserker";
        else if (selectedRole.equalsIgnoreCase("mage")) roleSpanish = "Mago";
        else if (selectedRole.equalsIgnoreCase("arquero")) roleSpanish = "Arquero";
        else if (selectedRole.equalsIgnoreCase("paladin")) roleSpanish = "Paladín";
        else if (selectedRole.equalsIgnoreCase("draconico")) roleSpanish = "Dracónico";
        else if (selectedRole.equalsIgnoreCase("clerigo")) roleSpanish = "Clérigo";

        this.textAwakening = "Abres los ojos...\n\nTu destino ha sido revelado.\n\nEstás en tu reino, " + roleSpanish + ".";
    }

    @Override
    protected void init() {
        this.clearWidgets();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (currentStage == CinematicStage.COUNTDOWN_TO_TELEPORT && !minigameWon) {
            // Tecla ESPACIO (keyCode 32)
            if (keyCode == 32) {
                mashProgress += 5.5f; // Sumar progreso por cada pulsada
                if (mashProgress > 100.0f) mashProgress = 100.0f;

                // Reproducir un sonido místico de campana de cristal al masear la barra
                Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F + (mashProgress * 0.005F))
                );

                // Agregar partículas locales de éxito sutiles
                Minecraft mc = Minecraft.getInstance();
                if (mc.level != null && mc.player != null) {
                    mc.level.addParticle(ParticleTypes.HAPPY_VILLAGER,
                        mc.player.getX() + (Math.random() - 0.5) * 1.5,
                        mc.player.getY() + 1.2,
                        mc.player.getZ() + (Math.random() - 0.5) * 1.5,
                        0, 0.05, 0);
                }

                if (mashProgress >= 100.0f) {
                    minigameWon = true;
                    // Enviar confirmación definitiva al servidor
                    NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SCompleteRoleSelectionPacket(selectedRole));
                    currentStage = CinematicStage.POST_TELEPORT_BLACKOUT;
                }
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();

        // Disminuir intensidad de latido de forma progresiva
        if (heartbeatPulseAlpha > 0.0f) {
            heartbeatPulseAlpha -= 0.08f;
            if (heartbeatPulseAlpha < 0.0f) heartbeatPulseAlpha = 0.0f;
        }

        if (currentStage == CinematicStage.SELECTION_AURA) {
            selectionTicks--;

            // Latido cada 20 ticks (1 segundo)
            if (selectionTicks % 20 == 0) {
                heartbeatPulseAlpha = 1.0f;
                if (mc.player != null) {
                    mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                            SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.6f, 0.5f, false);
                }
            }

            // Partículas de aura con partículas de M en el altar (Witch/Sparks)
            if (mc.level != null && mc.player != null) {
                for (int i = 0; i < 4; i++) {
                    mc.level.addParticle(ParticleTypes.WITCH,
                            mc.player.getX() + (Math.random() - 0.5D) * 3.0D,
                            mc.player.getY() + 0.5D + (Math.random() - 0.5D) * 1.5D,
                            mc.player.getZ() + (Math.random() - 0.5D) * 3.0D,
                            0.0D, 0.02D, 0.0D);
                }
            }

            if (selectionTicks <= 0) {
                currentStage = CinematicStage.BLACKOUT_TYPEWRITER;
                if (mc.player != null) {
                    mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                            SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.5f, 0.6f, false);
                }
            }

        } else if (currentStage == CinematicStage.BLACKOUT_TYPEWRITER) {
            typewriterTicks++;

            // Escribir texto
            if (typewriterTicks % 2 == 0 && typewriterCharIndex < textGoddess.length()) {
                typewriterCharIndex++;
                char c = textGoddess.charAt(typewriterCharIndex - 1);
                if (c != ' ' && c != '\n' && c != '─' && mc.player != null) {
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.STONE_BUTTON_CLICK_ON, 1.8f));
                }
            }

            if (typewriterCharIndex >= textGoddess.length()) {
                currentStage = CinematicStage.COUNTDOWN_TO_TELEPORT;
                ticksSinceLastHeartbeat = 0;
                mashProgress = 15.0f; // Empezar con una base pequeña
            }

        } else if (currentStage == CinematicStage.COUNTDOWN_TO_TELEPORT) {
            countdownTicks--;
            ticksSinceLastHeartbeat++;

            // Decaer el progreso lentamente por tick para mayor dificultad
            if (mashProgress > 0.0f) {
                mashProgress -= 0.15f;
            }

            // Calcular intervalo de latido acelerando dinámicamente de 24 ticks (lento) a 4 ticks (frenético!)
            int currentInterval = 4 + (int) ((countdownTicks / 300.0f) * 20.0f);

            if (ticksSinceLastHeartbeat >= currentInterval) {
                ticksSinceLastHeartbeat = 0;
                heartbeatPulseAlpha = 1.0f; // Activar el pulso de la vignette

                if (mc.player != null) {
                    // El tono sube de frecuencia (pitch) a medida que acelera
                    float pitch = 0.5f + (1.0f - (countdownTicks / 300.0f)) * 0.5f;
                    mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                            SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 2.0F, pitch, false);
                }
            }

            // Si se acaba el tiempo y no llenó la barra: ¡Falla la investidura!
            if (countdownTicks <= 0 && !minigameWon) {
                this.onClose(); // Cerrar la GUI
                if (mc.player != null) {
                    mc.player.displayClientMessage(Component.literal("§c¡Has fallado la prueba de fe de la Diosa María! Debes seleccionar tu rol de nuevo."), true);
                }
            }

        } else if (currentStage == CinematicStage.POST_TELEPORT_BLACKOUT) {
            postTeleportTicks--;

            // Latidos residuales lentos mientras todo está completamente negro
            if (postTeleportTicks % 25 == 0) {
                heartbeatPulseAlpha = 0.8f;
                if (mc.player != null) {
                    mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                            SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.4f, 0.45f, false);
                }
            }

            if (postTeleportTicks <= 0) {
                currentStage = CinematicStage.EYE_AWAKENING;
                if (mc.player != null) {
                    mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                            SoundEvents.PLAYER_BREATH, SoundSource.PLAYERS, 1.2f, 0.8f, false);
                }
            }

        } else if (currentStage == CinematicStage.EYE_AWAKENING) {
            awakeningTicks--;
            if (awakeningTicks <= 0) {
                this.onClose(); // Finalizar de forma segura
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int width = this.width;
        int height = this.height;
        int centerX = width / 2;
        int centerY = height / 2;

        if (currentStage == CinematicStage.SELECTION_AURA) {
            // Fondo semitransparente que se oscurece
            float progress = (float) (100 - selectionTicks) / 100.0f;
            int alpha = (int) (progress * 225.0f);
            graphics.fill(0, 0, width, height, (alpha << 24) | 0x0A0A0E);

            // Shaking
            double shakeX = (Math.random() - 0.5D) * 3.0D;
            double shakeY = (Math.random() - 0.5D) * 3.0D;

            graphics.drawCenteredString(this.font, "§d✦ LA DIOSA MARÍA TE OBSERVA ✦", centerX + (int)shakeX, centerY - 20 + (int)shakeY, 0xFFFFFFFF);
            graphics.drawCenteredString(this.font, "§7Marcando tu destino en el altar...", centerX + (int)shakeX, centerY + (int)shakeY, 0xFFBBBBBB);

        } else if (currentStage == CinematicStage.BLACKOUT_TYPEWRITER) {
            graphics.fill(0, 0, width, height, 0xFF000000);

            String currentText = textGoddess.substring(0, typewriterCharIndex);
            String[] lines = currentText.split("\n");

            int textY = centerY - 50;
            for (String line : lines) {
                int lineW = this.font.width(line);
                graphics.drawString(this.font, "§d" + line, centerX - lineW / 2, textY, 0xFFFFFFFF, false);
                textY += 12;
            }

        } else if (currentStage == CinematicStage.COUNTDOWN_TO_TELEPORT) {
            graphics.fill(0, 0, width, height, 0xFF000000);

            // Renderizar la vignette roja de latido de corazón
            if (heartbeatPulseAlpha > 0.0f) {
                drawHeartbeatVignette(graphics, width, height, heartbeatPulseAlpha);
            }

            int secondsRemaining = (countdownTicks / 20) + 1;

            graphics.drawString(this.font, "§dPRUEBA DE DETERMINACIÓN", centerX - this.font.width("§dPRUEBA DE DETERMINACIÓN") / 2, centerY - 52, 0xFFFFFFFF, false);
            graphics.drawString(this.font, "§e¡PRESIONA LA TECLA [ESPACIO] RAPIDAMENTE!", centerX - this.font.width("§e¡PRESIONA LA TECLA [ESPACIO] RAPIDAMENTE!") / 2, centerY - 38, 0xFFFFFFFF, false);
            graphics.drawString(this.font, "§7Límite de la prueba: " + secondsRemaining + "s", centerX - this.font.width("§7Límite de la prueba: " + secondsRemaining + "s") / 2, centerY - 26, 0xFFFFFFFF, false);

            // Renderizar barra de progreso del minijuego (RPG Style)
            int barWidth = 160;
            int barHeight = 12;
            int bx = centerX - barWidth / 2;
            int by = centerY + 10;

            // Fondo de la barra
            graphics.fill(bx - 2, by - 2, bx + barWidth + 2, by + barHeight + 2, 0xFF362819); // Madera medieval
            graphics.fill(bx, by, bx + barWidth, by + barHeight, 0xFF141415); // Fondo

            // Progreso relleno (Color rosa mágico de la Diosa)
            int fillWidth = (int) (barWidth * (mashProgress / 100.0f));
            if (fillWidth > 0) {
                graphics.fill(bx, by, bx + fillWidth, by + barHeight, 0xFFD48AD4);
                // Brillo de progreso
                if (System.currentTimeMillis() % 400 < 200) {
                    graphics.fill(bx + fillWidth - 2, by, bx + fillWidth, by + barHeight, 0xFFFFFFFF);
                }
            }

            // Texto de porcentaje
            String progPercent = (int)mashProgress + "% / 100%";
            graphics.drawString(this.font, "§f" + progPercent, centerX - this.font.width(progPercent) / 2, by + 16, 0xFFFFFFFF, false);

        } else if (currentStage == CinematicStage.POST_TELEPORT_BLACKOUT) {
            // Pantalla completamente negra tras teletransportarse, con latidos
            graphics.fill(0, 0, width, height, 0xFF000000);

            if (heartbeatPulseAlpha > 0.0f) {
                drawHeartbeatVignette(graphics, width, height, heartbeatPulseAlpha * 0.7f);
            }

            graphics.drawString(this.font, "§d✦ DIOSA MARÍA ✦", centerX - this.font.width("§d✦ DIOSA MARÍA ✦") / 2, centerY - 30, 0, false);

            // Envolver líneas de texto largo del mensaje post-teleportación
            java.util.List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(Component.literal("§d§o" + postTeleportMessage), width - 60);
            int textY = centerY;
            for (net.minecraft.util.FormattedCharSequence line : lines) {
                int lineW = this.font.width(line);
                graphics.drawString(this.font, line, centerX - lineW / 2, textY, 0xFFFFFFFF, false);
                textY += 14;
            }

        } else if (currentStage == CinematicStage.EYE_AWAKENING) {
            // Transición realista de pestañeo de ojos (Sin sombras)
            float alpha = (float) awakeningTicks / 120.0f;
            int alphaColor = ((int) (alpha * 255.0f) << 24) | 0x000000;
            graphics.fill(0, 0, width, height, alphaColor);

            if (alpha > 0.1f) {
                int eyeLidHeight = (int) (height * 0.4f * alpha);
                graphics.fill(0, 0, width, eyeLidHeight, 0xFF000000); // Párpado superior
                graphics.fill(0, height - eyeLidHeight, width, height, 0xFF000000); // Párpado inferior
            }

            if (awakeningTicks > 20) {
                String[] lines = textAwakening.split("\n");
                int textY = centerY - 30;
                for (String line : lines) {
                    int lineW = this.font.width(line);
                    graphics.drawString(this.font, "§e§o" + line, centerX - lineW / 2, textY, 0xFFFFFFFF, false);
                    textY += 14;
                }
            }
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void drawHeartbeatVignette(GuiGraphics graphics, int w, int h, float alpha) {
        int color = ((int) (alpha * 120.0f) << 24) | 0x990015; // Carmesí oscuro translúcido
        int border = (int) (30 * alpha);

        graphics.fill(0, 0, w, border, color);
        graphics.fill(0, h - border, w, h, color);
        graphics.fill(0, border, border, h - border, color);
        graphics.fill(w - border, border, w, h - border, color);
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
