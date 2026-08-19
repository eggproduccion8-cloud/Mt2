package com.mundodetronos2.gui;

import com.mundodetronos2.client.HudLayoutManager;
import com.mundodetronos2.client.HudLayoutManager.Anchor;
import com.mundodetronos2.client.HudLayoutManager.ComponentConfig;
import com.mundodetronos2.client.HudLayoutManager.ComponentId;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

public class HudEditorScreen extends Screen {

    public static final ResourceLocation LOGO_T2 = new ResourceLocation("mundodetronos2", "textures/gui/t2.png");
    public static final ResourceLocation LOGO_EGG = new ResourceLocation("mundodetronos2", "textures/gui/egg.png");

    private final Map<ComponentId, ComponentConfig> tempConfig = new EnumMap<>(ComponentId.class);
    private ComponentId selectedComponent = null;
    private boolean isDragging = false;
    private int dragStartX = 0;
    private int dragStartY = 0;
    private int initialOffsetX = 0;
    private int initialOffsetY = 0;

    public HudEditorScreen() {
        super(Component.literal("Editor de HUD MMO"));
        for (ComponentId id : ComponentId.values()) {
            tempConfig.put(id, HudLayoutManager.getConfig(id).copy());
        }
    }

    private static boolean isEditableHudComponent(ComponentId id) {
        return id != ComponentId.PLAYER_CARD
            && id != ComponentId.KINGDOM_CARD
            && id != ComponentId.RPG_INVENTORY
            && id != ComponentId.CHAT_BOX;
    }

    @Override
    protected void init() {
        super.init();
        int btnW = 90;
        int btnH = 20;
        int startX = this.width - btnW - 12;
        int startY = 12;

        // Botón GUARDAR
        this.addRenderableWidget(Button.builder(Component.literal("§aGUARDAR"), button -> {
            for (Map.Entry<ComponentId, ComponentConfig> entry : tempConfig.entrySet()) {
                ComponentConfig live = HudLayoutManager.getConfig(entry.getKey());
                live.anchor = entry.getValue().anchor;
                live.offsetX = entry.getValue().offsetX;
                live.offsetY = entry.getValue().offsetY;
                live.scale = entry.getValue().scale;
                live.visible = entry.getValue().visible;
                live.width = entry.getValue().width;
                live.height = entry.getValue().height;
            }
            HudLayoutManager.save();
            this.onClose();
        }).bounds(startX, startY, btnW, btnH).build());

        // Botón CANCELAR
        this.addRenderableWidget(Button.builder(Component.literal("§cCANCELAR"), button -> {
            this.onClose();
        }).bounds(startX, startY + 24, btnW, btnH).build());

        // Botón RESTABLECER
        this.addRenderableWidget(Button.builder(Component.literal("§eRESTABLECER"), button -> {
            HudLayoutManager.resetToDefaults();
            for (ComponentId id : ComponentId.values()) {
                tempConfig.put(id, HudLayoutManager.getConfig(id).copy());
            }
        }).bounds(startX, startY + 48, btnW, btnH).build());
    }

    private int getComponentBoxX(ComponentConfig config) {
        int baseX = HudLayoutManager.getRenderX(config, this.width, this.height);
        if (config.anchor == Anchor.BOTTOM_CENTER || config.anchor == Anchor.TOP_CENTER || config.anchor == Anchor.CENTER) {
            return baseX - (int)(config.width * config.scale) / 2;
        }
        return baseX;
    }

    private int getComponentBoxY(ComponentConfig config) {
        return HudLayoutManager.getRenderY(config, this.width, this.height);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Fondo translúcido suave
        graphics.fill(0, 0, this.width, this.height, 0xAA000000);

        // Grilla (20x20 pixels)
        int gridSpacing = 20;
        int gridColor = 0x15FFFFFF;
        for (int x = 0; x < this.width; x += gridSpacing) {
            graphics.fill(x, 0, x + 1, this.height, gridColor);
        }
        for (int y = 0; y < this.height; y += gridSpacing) {
            graphics.fill(0, y, this.width, y + 1, gridColor);
        }

        // LOGO OFICIAL t2.png
        int logoW = 140;
        int logoH = (int) (logoW / 1.7787f);
        graphics.blit(LOGO_T2, this.width / 2 - logoW / 2, 4, 0, 0, logoW, logoH, 1672, 940);

        // LOGO EGG.png EN ESQUINA INFERIOR IZQUIERDA
        graphics.blit(LOGO_EGG, 10, this.height - 32, 0, 0, 24, 24, 1254, 1254);
        graphics.drawString(this.font, "EGPRODUCCION", 38, this.height - 24, 0xAAFFFFFF, true);

        // Título del Editor
        graphics.drawCenteredString(this.font, "Editor de HUD (Tecla H)", this.width / 2, logoH + 8, 0xFFFFD700);

        // Renderizar contornos de componentes editables únicamente
        for (ComponentId id : ComponentId.values()) {
            if (!isEditableHudComponent(id)) continue;

            ComponentConfig config = tempConfig.get(id);
            if (config == null) continue;

            int w = (int) (config.width * config.scale);
            int h = (int) (config.height * config.scale);
            int x = getComponentBoxX(config);
            int y = getComponentBoxY(config);

            boolean isSelected = (selectedComponent == id);
            boolean isHovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;

            int borderColor = isSelected ? 0xFFFFD700 : (isHovered ? 0xFFFFFFFF : 0x88AAAAAA);
            int fillColor = isSelected ? 0x66FFD700 : 0x44000000;

            graphics.fill(x, y, x + w, y + h, fillColor);
            graphics.fill(x - 1, y - 1, x + w + 1, y, borderColor);
            graphics.fill(x - 1, y + h, x + w + 1, y + h + 1, borderColor);
            graphics.fill(x - 1, y, x, y + h, borderColor);
            graphics.fill(x + w, y, x + w + 1, y + h, borderColor);

            graphics.drawString(this.font, id.getDisplayName(), x + 3, y + 3, borderColor, false);
        }

        // Información del componente seleccionado y controles de Escala
        if (selectedComponent != null) {
            ComponentConfig config = tempConfig.get(selectedComponent);
            String info = "Seleccionado: " + selectedComponent.getDisplayName() + " (X: " + config.offsetX + ", Y: " + config.offsetY + ", Escala: " + String.format("%.2f", config.scale) + ")";
            graphics.drawString(this.font, info, 10, this.height - 45, 0xFFFFD700, false);
            graphics.drawString(this.font, "[+ / - o Rueda del ratón para ajustar escala]", 10, this.height - 32, 0x88FFFFFF, false);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (ComponentId id : ComponentId.values()) {
                if (!isEditableHudComponent(id)) continue;

                ComponentConfig config = tempConfig.get(id);
                if (config == null) continue;

                int w = (int) (config.width * config.scale);
                int h = (int) (config.height * config.scale);
                int x = getComponentBoxX(config);
                int y = getComponentBoxY(config);

                if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
                    selectedComponent = id;
                    isDragging = true;
                    dragStartX = (int) mouseX;
                    dragStartY = (int) mouseY;
                    initialOffsetX = config.offsetX;
                    initialOffsetY = config.offsetY;
                    return true;
                }
            }
            selectedComponent = null;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging && selectedComponent != null && button == 0) {
            ComponentConfig config = tempConfig.get(selectedComponent);
            if (config != null) {
                int deltaX = (int) mouseX - dragStartX;
                int deltaY = (int) mouseY - dragStartY;
                config.offsetX = initialOffsetX + deltaX;
                config.offsetY = initialOffsetY + deltaY;
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (selectedComponent != null) {
            ComponentConfig config = tempConfig.get(selectedComponent);
            if (config != null) {
                if (delta > 0) {
                    config.scale = Math.min(2.5f, config.scale + 0.05f);
                } else if (delta < 0) {
                    config.scale = Math.max(0.4f, config.scale - 0.05f);
                }
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selectedComponent != null) {
            ComponentConfig config = tempConfig.get(selectedComponent);
            if (config != null) {
                if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_EQUAL || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ADD) {
                    config.scale = Math.min(2.5f, config.scale + 0.05f);
                    return true;
                } else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_MINUS || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_SUBTRACT) {
                    config.scale = Math.max(0.4f, config.scale - 0.05f);
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
