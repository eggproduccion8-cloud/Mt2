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

    private net.minecraft.client.CameraType previousCameraType = net.minecraft.client.CameraType.FIRST_PERSON;
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

        // Switch camera to 3rd person back perspective on opening dialogue
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.options != null) {
            this.previousCameraType = mc.options.getCameraType();
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        }

        // Responsive Box Dimension Calculations
        this.boxW = Math.max(260, this.width - 40);
        this.boxX = (this.width - this.boxW) / 2;

        int maxTextWidth = this.boxX + this.boxW - 40;
        this.wrappedLines = this.font.split(Component.literal(this.text), Math.max(120, maxTextWidth));

        int textHeight = this.wrappedLines.size() * 11;
        this.boxH = Math.max(60, 32 + textHeight + 12);
        this.boxY = this.height - this.boxH - 12;

        // Position option buttons grouped on the right side
        int btnW = Math.min(220, this.width - 40);
        int btnH = 20;
        int btnX = this.width - btnW - 20;
        int startBtnY = this.height / 2 - (optionTexts.size() * 24) / 2;

        for (int i = 0; i < optionTexts.size(); i++) {
            final int index = i;
            String optionText = optionTexts.get(i);

            this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(
                btnX, startBtnY + (i * 24), btnW, btnH,
                Component.literal("➤ " + optionText),
                btn -> {
                    NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SSelectDialogueOptionPacket(npcId, npcType, nodeId, index));
                }
            ));
        }
    }

    @Override
    public void onClose() {
        // Strictly restore 1st-person camera perspective upon closing dialogue
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.options != null) {
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
        }
        super.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Completely transparent background - 3rd person world view stays clear
        int textX = 20;
        int textY = this.height - (wrappedLines.size() * 12) - 30;

        // Clean NPC Name tag
        String npcTitle = "[" + npcName + "]:";
        graphics.drawString(this.font, npcTitle, textX, textY - 14, 0xFFFFAA00, true);

        // Clean Dialogue Text
        for (FormattedCharSequence line : wrappedLines) {
            graphics.drawString(this.font, line, textX, textY, 0xFFFFFFFF, true);
            textY += 12;
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
