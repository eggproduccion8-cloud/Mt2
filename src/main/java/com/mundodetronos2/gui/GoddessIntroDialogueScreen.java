package com.mundodetronos2.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class GoddessIntroDialogueScreen extends Screen {

    private int currentStep = 1;

    // Alpha de desvanecimiento para entrada suave del texto
    private float textAlpha = 0.0f;
    private String currentDiosaText = "";
    private String playerReplyText = "";

    private boolean waitingForChoice = false;
    private boolean textIsGiant = false;
    private int rumbleTicks = 0;

    // Diálogos del Lore con mucho entusiasmo de anime Isekai (Sin negritas)
    private final String[][] dialogues = {
        {
            "¡Oho! ¡Un alma intrépida ha cruzado el Velo de la Eternidad! ¡Hacía siglos que nadie pisaba mi santuario sagrado! Dime... ¿quién osa perturbar el descanso de la Diosa María?",
            "¡Soy un aventurero de otro mundo!",
            "No tiene importancia quién sea..."
        },
        {
            "¡Increíble! ¡Un reclamante de los antiguos tronos del Overworld! ¡Y dime, valiente buscador de leyendas, cómo es que has logrado cruzar las puertas prohibidas de mi dimensión estelar?",
            "¡He venido por el Mandato Sagrado del Rey a recuperar la corona y todo aquello que nos fue arrebatado!"
        },
        {
            "¡Jajaja! ¡Los reyes mortales prometen imperios celestiales sobre tronos de ceniza! En este mundo isekai de guerras infinitas, ¡el verdadero poder se forja con la propia alma!",
            "¡El rey nos abandonó a nuestra suerte, pero mi reino resistirá y se alzará de las cenizas!"
        },
        {
            "¡Ese es el fervor ardiente que me complace presenciar! Si deseas reclamar tu gloria, ¡debes fusionar tu hálito vital con el núcleo del trono sagrado!",
            "¡Estoy listo para entregar todo mi espíritu a cambio del poder de los héroes antiguos!"
        },
        {
            "¡Maravilloso! ¡Una determinación digna de las canciones de los bardos del reino! ¡Dime qué camino andará tu alma: la furia indomable, el acero eterno o la magia cósmica!",
            "¡Guiaré a mi reino con honor y defenderé nuestro trono con mi propia vida!"
        },
        {
            "¡Ten en cuenta que el vínculo es inquebrantable! Si tu trono cae ante tus rivales, ¡perderás una de tus sagradas vidas del alma! ¿Aceptarás este pacto de sangre?",
            "¡Lo acepto! ¡Ningún invasor profanará nuestro hogar mientras me quede un hálito de fuerza!"
        },
        {
            "¡Muchos necios pretendieron el mismo destino y sus almas ahora vagan eternamente como estrellas rosadas en este vacío infinito! ¡Pero tú posees una chispa inusual!",
            "¡Yo no fallaré! ¡La Rosa Sagrada que ofrecí es el pacto inquebrantable de mi fe en tu poder!"
        },
        {
            "¡Sí! ¡El perfume de la Rosa Sagrada ha impregnado el éter celestial! ¡Tu pacto ha sido sellado con mi bendición divina!",
            "¡Muéstrame el camino de los antiguos caminos sagrados de los tronos, Diosa María!"
        },
        {
            "¡Que tu nombre resuene por toda la eternidad! ¡Prepárate para recibir el don supremo de tu nueva clase de combate!",
            "¡Estoy preparado para mi nueva vida!"
        },
        {
            "¡Que las almas caídas y la luz rosada guíen tu gloriosa cruzada! ¡Camina hacia el Altar del Destino y reclama tu carnet de aspirante!",
            "[¡Aproximarse con orgullo al Altar!]"
        }
    };

    public GoddessIntroDialogueScreen() {
        super(Component.literal("Introducción de la Diosa María"));
        updateDialogueStep();
        com.mundodetronos2.client.ClientEvents.cinematicActive = true;
    }

    private void updateDialogueStep() {
        if (currentStep <= dialogues.length) {
            String[] stepData = dialogues[currentStep - 1];
            currentDiosaText = "Diosa María: " + stepData[0];

            if (currentStep == 1) {
                waitingForChoice = true;
                playerReplyText = "";
            } else {
                waitingForChoice = false;
                playerReplyText = stepData[1];
            }
        }
        textAlpha = 0.0f; // Reiniciar desvanecimiento para aparecer el nuevo texto suavemente
    }

    @Override
    protected void init() {
        this.clearWidgets();
        setupButtons();
    }

    private void setupButtons() {
        this.clearWidgets();
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int buttonWidth = 160;
        int buttonHeight = 22;

        if (waitingForChoice) {
            // Respuestas del paso 1 con botones transparentes elegantes (Sin negritas)
            this.addRenderableWidget(new TransparentButton(centerX - buttonWidth - 15, centerY + 65, buttonWidth, buttonHeight, Component.literal("Soy un jugador"), btn -> {
                handleChoice("Soy un jugador");
            }));

            this.addRenderableWidget(new TransparentButton(centerX + 15, centerY + 65, buttonWidth, buttonHeight, Component.literal("No importa"), btn -> {
                handleChoice("No importa");
            }));
        } else {
            // Siguiente diálogo con botón transparente elegante (Sin negritas)
            String btnText = currentStep == dialogues.length ? "Aproximarse al Altar" : "Continuar";
            this.addRenderableWidget(new TransparentButton(centerX - 80, centerY + 65, 160, buttonHeight, Component.literal(btnText), btn -> {
                if (textAlpha < 1.0f) {
                    // Completar desvanecimiento instantáneamente
                    textAlpha = 1.0f;
                } else {
                    // Partículas al avanzar diálogo
                    spawnDialogueActionParticles();

                    if (currentStep == dialogues.length) {
                        // Cerrar para que el jugador camine hacia el altar
                        this.onClose();
                    } else {
                        currentStep++;
                        updateDialogueStep();
                        setupButtons();
                    }
                }
            }));
        }
    }

    private void spawnDialogueActionParticles() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            double px = mc.player.getX();
            double py = mc.player.getY() + 1.0D;
            double pz = mc.player.getZ();
            for (int i = 0; i < 20; i++) {
                mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.WITCH,
                        px + (Math.random() - 0.5D) * 1.5D,
                        py + (Math.random() - 0.5D) * 1.5D,
                        pz + (Math.random() - 0.5D) * 1.5D,
                        (Math.random() - 0.5D) * 0.2D,
                        Math.random() * 0.1D,
                        (Math.random() - 0.5D) * 0.2D);
            }
        }
    }

    private void handleChoice(String choice) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Retumbido fuerte y sacudida de pantalla
        rumbleTicks = 35;
        textIsGiant = false; // Quitar negritas pesadas

        // Sonidos divinos potentes
        if (mc.level != null) {
            mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0F, 0.4F, false);
            mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.5F, 0.5F, false);
        }

        // Respuesta con el nombre real del jugador (Sin negrita)
        String playerName = mc.player.getGameProfile().getName();
        playerReplyText = "¡Soy " + playerName.toUpperCase() + "!";

        waitingForChoice = false;

        // Chispas de almas al seleccionar
        spawnDialogueActionParticles();

        // Re-crear botón de continuar
        setupButtons();
    }

    @Override
    public void tick() {
        if (rumbleTicks > 0) {
            rumbleTicks--;
        }

        // Efecto de desvanecimiento suave para aparecer el texto (fade-in)
        if (textAlpha < 1.0f) {
            textAlpha += 0.08f;
            if (textAlpha > 1.0f) textAlpha = 1.0f;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Pantalla completamente negra de fondo
        graphics.fill(0, 0, this.width, this.height, 0xFF000000);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Efecto sacudida por retumbido
        int offsetX = 0;
        int offsetY = 0;
        if (rumbleTicks > 0) {
            offsetX = (int) ((Math.random() - 0.5) * 8.0);
            offsetY = (int) ((Math.random() - 0.5) * 8.0);
        }

        // Título de la Diosa María con efecto de brillo (Sin negrita en el subtítulo)
        graphics.drawCenteredString(this.font, "§d✦ DIOSA MARÍA ✦", centerX + offsetX, centerY - 80 + offsetY, 0xFFFFFFFF);

        // Texto Diosa María (Desvanecido suavemente) envuelto perfectamente dentro de la pantalla
        int textY = centerY - 50;

        // Calcular color con el alpha del fade-in
        int alphaValue = (int) (textAlpha * 255.0f);
        int textColorWithAlpha = (alphaValue << 24) | 0x00E0C0E0; // Rosa suave místico

        // Envolver líneas de texto largo a máximo 280 píxeles para evitar salirse de la pantalla
        java.util.List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(Component.literal(currentDiosaText), 280);
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            int lineW = this.font.width(line);
            graphics.drawString(this.font, line, centerX - lineW / 2 + offsetX, textY + offsetY, textColorWithAlpha, false);
            textY += 13;
        }

        // Renderizar la respuesta del jugador centrada y formateada (Sin negrita)
        if (!playerReplyText.isEmpty() && textAlpha >= 1.0f) {
            int replyY = textY + 14;

            // Renderizar la respuesta del jugador de forma clara e integrada
            String displayReply = "§e" + playerReplyText;
            int replyW = this.font.width(displayReply);

            // Si la respuesta se sale de la pantalla, envolverla
            if (replyW > 280) {
                java.util.List<net.minecraft.util.FormattedCharSequence> replyLines = this.font.split(Component.literal(displayReply), 280);
                for (net.minecraft.util.FormattedCharSequence rLine : replyLines) {
                    int rLineW = this.font.width(rLine);
                    graphics.drawString(this.font, rLine, centerX - rLineW / 2 + offsetX, replyY + offsetY, 0xFFFFFFFF, true);
                    replyY += 13;
                }
            } else {
                graphics.drawString(this.font, displayReply, centerX - replyW / 2 + offsetX, replyY + offsetY, 0xFFFFFFFF, true);
            }
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

    // --- CLASE DE BOTÓN TRANSPARENTE ELEGANTE ---
    public static class TransparentButton extends Button {
        public TransparentButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            if (!this.visible) return;

            boolean hovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;

            // Colores elegantes dorados/rosados
            int borderColor = hovered ? 0xFFD4AF37 : 0xAA8A6F8A;
            int bgColor = hovered ? 0x2AD4AF37 : 0x1A000000;

            // Dibujar bordes finos
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColor);
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, borderColor);
            graphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, borderColor);
            graphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.height, borderColor);
            graphics.fill(this.getX() + this.width - 1, this.getY(), this.getX() + this.width, this.getY() + this.height, borderColor);

            int textColor = hovered ? 0xFFFFFFFF : 0xFFDDDDDD;
            graphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, textColor);
        }
    }
}
