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

        // 3. Position option buttons grouped on the right side
        int btnW = Math.min(200, this.width - 40);
        int btnH = 20;
        int btnX = this.boxX + this.boxW - btnW - 12;
        int startBtnY = this.boxY + 26;

        for (int i = 0; i < optionTexts.size(); i++) {
            final int index = i;
            String optionText = optionTexts.get(i);
            if (optionText.length() > 32) {
                optionText = optionText.substring(0, 29) + "...";
            }

            this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(
                btnX, startBtnY + (i * 22), btnW, btnH,
                Component.literal("➤ " + optionText),
                btn -> {
                    NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SSelectDialogueOptionPacket(npcId, npcType, nodeId, index));
                }
            ));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Subtle dark gradient background overlay (world in third person remains visible)
        graphics.fill(0, 0, this.width, this.height, 0x44000000);

        // Clean transparent MMORPG dialogue panel
        graphics.fill(boxX - 2, boxY - 2, boxX + boxW + 2, boxY + boxH + 2, 0xAA111111);
        graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xDD222222);

        // Gold title accent line
        graphics.fill(boxX + 6, boxY + 20, boxX + boxW - 6, boxY + 21, 0xFFD4AF37);

        // Title NPC Name
        String npcTitle = "💬 " + npcName.toUpperCase();
        graphics.drawString(this.font, npcTitle, boxX + 12, boxY + 8, 0xFFFFD700, false);

        // Render Wrapped Dialogue Lines
        int textX = boxX + 12;
        int textY = boxY + 28;
        for (FormattedCharSequence line : wrappedLines) {
            graphics.drawString(this.font, line, textX, textY, 0xFFFFFFFF, false);
            textY += 11;
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
