package com.mundodetronos2.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class TransparentButton extends Button {
    public TransparentButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) return;

        boolean hovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;

        int borderColor = hovered ? 0xFFD4AF37 : 0xAA8A6F8A;
        int bgColor = hovered ? 0x2AD4AF37 : 0x1A000000;

        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColor);
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, borderColor);
        graphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, borderColor);
        graphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.height, borderColor);
        graphics.fill(this.getX() + this.width - 1, this.getY(), this.getX() + this.width, this.getY() + this.height, borderColor);

        int textColor = hovered ? 0xFFFFFFFF : 0xFFDDDDDD;
        graphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, textColor);
    }
}
