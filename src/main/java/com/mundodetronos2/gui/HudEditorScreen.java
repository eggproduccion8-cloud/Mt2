package com.mundodetronos2.gui;

import com.mundodetronos2.client.HudLayoutManager;
import com.mundodetronos2.client.HudLayoutManager.Anchor;
import com.mundodetronos2.client.HudLayoutManager.ComponentConfig;
import com.mundodetronos2.client.HudLayoutManager.ComponentId;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.EnumMap;
import java.util.Map;

public class HudEditorScreen extends Screen {

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

    @Override
    protected void init() {
        super.init();
        int btnW = 90;
        int btnH = 20;
        int startX = this.width - btnW - 12;
        int startY = 12;

        // Botón GUARDAR (Arriba a la derecha en columna vertical)
        this.addRenderableWidget(Button.builder(Component.literal("§aGUARDAR"), button -> {
            for (Map.Entry<ComponentId, ComponentConfig> entry : tempConfig.entrySet()) {
                ComponentConfig live = HudLayoutManager.getConfig(entry.getKey());
                live.offsetX = entry.getValue().offsetX;
                live.offsetY = entry.getValue().offsetY;
                live.scale = entry.getValue().scale;
                live.visible = entry.getValue().visible;
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

        // Título del Editor
        graphics.drawCenteredString(this.font, "§e§lEDITOR DE HUD MMORPG §7(Arrastra los componentes con el ratón)", this.width / 2, 12, 0xFFFFFFFF);

        // Renderizar contornos y etiquetas de cada componente editable
        for (ComponentId id : ComponentId.values()) {
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

            // Renderizar caja de delimitación
            graphics.fill(x, y, x + w, y + h, fillColor);
            graphics.fill(x - 1, y - 1, x + w + 1, y, borderColor);
            graphics.fill(x - 1, y + h, x + w + 1, y + h + 1, borderColor);
            graphics.fill(x - 1, y, x, y + h, borderColor);
            graphics.fill(x + w, y, x + w + 1, y + h, borderColor);

            // Nombre del componente
            graphics.drawString(this.font, id.getDisplayName(), x + 3, y + 3, borderColor, false);
        }

        // Informar del componente seleccionado
        if (selectedComponent != null) {
            ComponentConfig config = tempConfig.get(selectedComponent);
            String info = "Seleccionado: " + selectedComponent.getDisplayName() + " (Offset X: " + config.offsetX + ", Offset Y: " + config.offsetY + ")";
            graphics.drawString(this.font, info, 10, this.height - 45, 0xFFFFD700, false);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Click izquierdo para seleccionar/arrastrar
            for (ComponentId id : ComponentId.values()) {
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
