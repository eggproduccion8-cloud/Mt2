package com.mundodetronos2.gui;

import com.mundodetronos2.client.InventoryLayoutManager;
import com.mundodetronos2.client.InventoryLayoutManager.Anchor;
import com.mundodetronos2.client.InventoryLayoutManager.ComponentConfig;
import com.mundodetronos2.client.InventoryLayoutManager.InventoryComponentId;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

public class InventoryEditorScreen extends Screen {

    public static final ResourceLocation LOGO_T2 = new ResourceLocation("mundodetronos2", "textures/gui/t2.png");
    public static final ResourceLocation LOGO_EGG = new ResourceLocation("mundodetronos2", "textures/gui/egg.png");

    private final RPGInventoryScreen.Tab targetTab;
    private final Map<InventoryComponentId, ComponentConfig> tempConfig = new EnumMap<>(InventoryComponentId.class);
    private InventoryComponentId selectedComponent = null;

    private boolean isDragging = false;
    private boolean isResizing = false;
    private int dragStartX = 0;
    private int dragStartY = 0;
    private int initialOffsetX = 0;
    private int initialOffsetY = 0;
    private float initialScale = 1.0f;

    public InventoryEditorScreen() {
        this(RPGInventoryScreen.Tab.PERSONAJE);
    }

    public InventoryEditorScreen(RPGInventoryScreen.Tab tab) {
        super(Component.literal("Editor de Inventario RPG"));
        this.targetTab = tab != null ? tab : RPGInventoryScreen.Tab.PERSONAJE;
        for (InventoryComponentId id : InventoryComponentId.values()) {
            tempConfig.put(id, InventoryLayoutManager.getConfig(id).copy());
        }
    }

    private boolean isComponentInCurrentTab(InventoryComponentId id) {
        switch (targetTab) {
            case PERSONAJE:
                return id == InventoryComponentId.MODEL_3D
                    || id == InventoryComponentId.EQUIPMENT_SLOTS
                    || id == InventoryComponentId.PLAYER_STATS
                    || id == InventoryComponentId.PLAYER_INVENTORY_GRID
                    || id == InventoryComponentId.LOGO_T2;
            case FABRICACION:
                return id == InventoryComponentId.CRAFTING_STATION_3X3
                    || id == InventoryComponentId.CRAFTING_CATALOG
                    || id == InventoryComponentId.PLAYER_INVENTORY_GRID
                    || id == InventoryComponentId.LOGO_T2;
            case MOCHILA:
                return id == InventoryComponentId.MOCHILA_CONTAINER
                    || id == InventoryComponentId.PLAYER_INVENTORY_GRID
                    || id == InventoryComponentId.LOGO_T2;
            default:
                return true;
        }
    }

    @Override
    protected void init() {
        super.init();
        int btnW = 90;
        int btnH = 20;
        int startX = this.width - btnW - 12;
        int startY = 12;

        // GUARDAR
        this.addRenderableWidget(Button.builder(Component.literal("§aGUARDAR"), button -> {
            for (Map.Entry<InventoryComponentId, ComponentConfig> entry : tempConfig.entrySet()) {
                ComponentConfig live = InventoryLayoutManager.getConfig(entry.getKey());
                live.anchor = entry.getValue().anchor;
                live.offsetX = entry.getValue().offsetX;
                live.offsetY = entry.getValue().offsetY;
                live.scale = entry.getValue().scale;
                live.visible = entry.getValue().visible;
                live.width = entry.getValue().width;
                live.height = entry.getValue().height;
            }
            InventoryLayoutManager.save();
            this.onClose();
        }).bounds(startX, startY, btnW, btnH).build());

        // CANCELAR
        this.addRenderableWidget(Button.builder(Component.literal("§cCANCELAR"), button -> {
            this.onClose();
        }).bounds(startX, startY + 24, btnW, btnH).build());

        // RESTABLECER TODO
        this.addRenderableWidget(Button.builder(Component.literal("§eRESTABLECER"), button -> {
            InventoryLayoutManager.resetToDefaults();
            for (InventoryComponentId id : InventoryComponentId.values()) {
                tempConfig.put(id, InventoryLayoutManager.getConfig(id).copy());
            }
        }).bounds(startX, startY + 48, btnW, btnH).build());

        // Botones rápidos de escala para el elemento seleccionado
        int scaleX = 10;
        int scaleY = 10;
        float[] scales = {0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
        for (float s : scales) {
            String label = (int) (s * 100) + "%";
            this.addRenderableWidget(Button.builder(Component.literal(label), button -> {
                if (selectedComponent != null) {
                    ComponentConfig config = tempConfig.get(selectedComponent);
                    if (config != null) {
                        config.scale = s;
                    }
                }
            }).bounds(scaleX, scaleY, 45, 18).build());
            scaleX += 48;
        }
    }

    private int getComponentBoxX(ComponentConfig config) {
        return InventoryLayoutManager.getRenderX(config, this.width, this.height);
    }

    private int getComponentBoxY(ComponentConfig config) {
        return InventoryLayoutManager.getRenderY(config, this.width, this.height);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xBB000000);

        // Grilla
        int gridSpacing = 20;
        int gridColor = 0x15FFFFFF;
        for (int x = 0; x < this.width; x += gridSpacing) {
            graphics.fill(x, 0, x + 1, this.height, gridColor);
        }
        for (int y = 0; y < this.height; y += gridSpacing) {
            graphics.fill(0, y, this.width, y + 1, gridColor);
        }

        graphics.drawCenteredString(this.font, "EDITOR DE INVENTARIO RPG (" + targetTab.name() + ") - Tecla Y", this.width / 2, 8, 0xFFFFD700);

        // Renderizar componentes editables de la pestaña activa únicamente
        for (InventoryComponentId id : InventoryComponentId.values()) {
            if (!isComponentInCurrentTab(id)) continue;

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

            if (isSelected) {
                graphics.fill(x + w - 6, y + h - 6, x + w + 2, y + h + 2, 0xFFFFD700);
            }

            graphics.drawString(this.font, id.getDisplayName(), x + 4, y + 4, borderColor, false);
        }

        if (selectedComponent != null) {
            ComponentConfig config = tempConfig.get(selectedComponent);
            String info = "Seleccionado: " + selectedComponent.getDisplayName() + " | X: " + config.offsetX + ", Y: " + config.offsetY + " | Escala: " + String.format("%.2f", config.scale);
            graphics.drawString(this.font, info, 10, this.height - 35, 0xFFFFD700, false);
            graphics.drawString(this.font, "[Arrastrar esquina o usar Rueda del ratón para redimensionar]", 10, this.height - 22, 0x88FFFFFF, false);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (selectedComponent != null) {
                ComponentConfig config = tempConfig.get(selectedComponent);
                if (config != null) {
                    int w = (int) (config.width * config.scale);
                    int h = (int) (config.height * config.scale);
                    int x = getComponentBoxX(config);
                    int y = getComponentBoxY(config);

                    if (mouseX >= x + w - 8 && mouseX <= x + w + 4 && mouseY >= y + h - 8 && mouseY <= y + h + 4) {
                        isResizing = true;
                        dragStartX = (int) mouseX;
                        dragStartY = (int) mouseY;
                        initialScale = config.scale;
                        return true;
                    }
                }
            }

            for (InventoryComponentId id : InventoryComponentId.values()) {
                if (!isComponentInCurrentTab(id)) continue;

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
        if (selectedComponent != null && button == 0) {
            ComponentConfig config = tempConfig.get(selectedComponent);
            if (config != null) {
                if (isResizing) {
                    int deltaX = (int) mouseX - dragStartX;
                    float newScale = Math.max(0.25f, Math.min(4.0f, initialScale + (deltaX / 100.0f)));
                    config.scale = newScale;
                    return true;
                } else if (isDragging) {
                    int deltaX = (int) mouseX - dragStartX;
                    int deltaY = (int) mouseY - dragStartY;
                    config.offsetX = initialOffsetX + deltaX;
                    config.offsetY = initialOffsetY + deltaY;
                    return true;
                }
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
                    config.scale = Math.min(4.0f, config.scale + 0.05f);
                } else if (delta < 0) {
                    config.scale = Math.max(0.25f, config.scale - 0.05f);
                }
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDragging = false;
            isResizing = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
