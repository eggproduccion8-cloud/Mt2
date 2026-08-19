package com.mundodetronos2.gui;

import com.mundodetronos2.network.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public class NpcDialogueScreen extends Screen {

    private final int npcId;
    private final String npcType;
    private final String npcName;
    private final String text;
    private final String nodeId;
    private final String skinName;
    private final List<String> optionTexts;

    private ResourceLocation skinTexture;
    private List<FormattedCharSequence> wrappedLines = new ArrayList<>();
    private int boxX;
    private int boxY;
    private int boxW;
    private int boxH;

    public NpcDialogueScreen(String npcName) {
        this(0, "diosa_maria", npcName, "Hola", "inicio", "Wyldune", new ArrayList<>());
    }

    public NpcDialogueScreen(int npcId, String npcType, String npcName, String text, String nodeId, List<String> optionTexts) {
        this(npcId, npcType, npcName, text, nodeId, "Wyldune", optionTexts);
    }

    public NpcDialogueScreen(int npcId, String npcType, String npcName, String text, String nodeId, String skinName, List<String> optionTexts) {
        super(Component.literal(npcName));
        this.npcId = npcId;
        this.npcType = npcType != null ? npcType.toLowerCase().trim() : "diosa_maria";
        this.npcName = npcName;
        this.text = text != null ? text : "";
        this.nodeId = nodeId;
        this.skinName = skinName != null ? skinName : "Wyldune";
        this.optionTexts = optionTexts != null ? optionTexts : new ArrayList<>();
    }

    @Override
    protected void init() {
        this.clearWidgets();

        // 1. Resolve skin texture
        this.skinTexture = com.mundodetronos2.client.GoddessNPCRenderer.getSkinLocationByName(skinName);

        // 2. Responsive Box Dimension Calculations
        this.boxW = Math.max(260, this.width - 40);
        this.boxX = (this.width - this.boxW) / 2;

        int faceSize = 36;
        int textLeft = this.boxX + 12 + faceSize + 12;
        int maxTextWidth = this.boxX + this.boxW - textLeft - 12;

        this.wrappedLines = this.font.split(Component.literal("§0" + this.text), Math.max(120, maxTextWidth));

        int textHeight = this.wrappedLines.size() * 11;
        this.boxH = Math.max(80, 32 + textHeight + 12);
        this.boxY = this.height - this.boxH - 12;

        // 3. Position option buttons safely above or alongside the dialog box
        int btnW = Math.min(220, this.width - 40);
        int btnH = 18;
        int btnX = this.width - btnW - 24;
        int startBtnY = this.boxY - (optionTexts.size() * 22) - 8;

        for (int i = 0; i < optionTexts.size(); i++) {
            final int index = i;
            String optionText = optionTexts.get(i);
            if (optionText.length() > 38) {
                optionText = optionText.substring(0, 35) + "...";
            }

            this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(
                btnX, startBtnY + (i * 22), btnW, btnH,
                Component.literal("➤ " + optionText),
                btn -> {
                    NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SSelectDialogueOptionPacket(npcId, npcType, nodeId, index));
                    this.onClose();
                }
            ));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Dark translucent background overlay
        graphics.fill(0, 0, this.width, this.height, 0x77000000);

        // --- CAJA DE DIÁLOGO RPG RESPONSIVE ---
        graphics.fill(boxX - 3, boxY - 3, boxX + boxW + 3, boxY + boxH + 3, 0xEE362819); // Marco madera
        graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xEEF3E5C8); // Pergamino
        graphics.fill(boxX + 2, boxY + 2, boxX + boxW - 2, boxY + boxH - 2, 0xEEEBDAB3);

        // Frame dorado fino
        graphics.fill(boxX + 4, boxY + 4, boxX + boxW - 4, boxY + 5, 0xFFD4AF37);
        graphics.fill(boxX + 4, boxY + boxH - 5, boxX + boxW - 4, boxY + boxH - 4, 0xFFD4AF37);
        graphics.fill(boxX + 4, boxY + 4, boxX + 5, boxY + boxH - 4, 0xFFD4AF37);
        graphics.fill(boxX + boxW - 5, boxY + 4, boxX + boxW - 4, boxY + boxH - 4, 0xFFD4AF37);

        // Head Portrait
        int faceX = boxX + 12;
        int faceY = boxY + 12;
        int faceSize = 36;
        graphics.fill(faceX - 2, faceY - 2, faceX + faceSize + 2, faceY + faceSize + 2, 0xFF362819);
        graphics.fill(faceX - 1, faceY - 1, faceX + faceSize + 1, faceY + faceSize + 1, 0xFFD4AF37);

        if (skinTexture != null) {
            PlayerFaceRenderer.draw(graphics, skinTexture, faceX, faceY, faceSize);
        }

        // Title NPC
        String npcTitle = npcName.toUpperCase();
        graphics.drawString(this.font, "§6§l" + npcTitle, faceX + faceSize + 12, boxY + 12, 0, false);

        // Render Wrapped Lines without text overflow
        int textX = faceX + faceSize + 12;
        int textY = boxY + 26;
        for (FormattedCharSequence line : wrappedLines) {
            graphics.drawString(this.font, line, textX, textY, 0, false);
            textY += 11;
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
