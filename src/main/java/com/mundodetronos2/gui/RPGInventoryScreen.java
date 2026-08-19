package com.mundodetronos2.gui;

import com.mundodetronos2.client.ClientEvents;
import com.mundodetronos2.client.ClientPacketHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public class RPGInventoryScreen extends AbstractContainerScreen<RPGInventoryMenu> {

    public enum Tab {
        PERSONAJE,
        INVENTARIO,
        FABRICACION,
        MOCHILA,
        INFORMACION
    }

    private Tab currentTab = Tab.PERSONAJE;

    public RPGInventoryScreen(RPGInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        // Ocupar pantalla completa dinámicamente según this.width y this.height
        this.leftPos = 0;
        this.topPos = 0;
        this.imageWidth = this.width;
        this.imageHeight = this.height;

        updateSlotPositions();
    }

    private void setTab(Tab tab) {
        this.currentTab = tab;
        updateSlotPositions();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Click izquierdo para cambio de pestaña
            int startY = 12;
            int tabWidth = 85;
            int tabHeight = 18;
            Tab[] tabs = Tab.values();
            int totalTabsW = tabs.length * (tabWidth + 6);
            int startX = (this.width - totalTabsW) / 2;

            for (int i = 0; i < tabs.length; i++) {
                int tx = startX + i * (tabWidth + 6);
                if (mouseX >= tx && mouseX <= tx + tabWidth && mouseY >= startY && mouseY <= startY + tabHeight) {
                    setTab(tabs[i]);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void updateSlotPositions() {
        int w = this.width;
        int h = this.height;

        boolean isPersonaje = currentTab == Tab.PERSONAJE;
        boolean isInventory = currentTab == Tab.INVENTARIO;
        boolean isCrafting = currentTab == Tab.FABRICACION;
        boolean isBackpack = currentTab == Tab.MOCHILA;

        // 1. ARMADURA (0..3) -> Pestaña PERSONAJE
        int armorX = w / 2 - 130;
        int armorY = h / 2 - 80;
        for (int i = 0; i < 4; i++) {
            this.menu.setSlotState(i, armorX + 1, armorY + i * 22 + 1, isPersonaje);
        }

        // 2. SEGUNDA MANO (4) -> Pestaña PERSONAJE
        int offhandX = w / 2 - 130;
        int offhandY = h / 2 + 15;
        this.menu.setSlotState(4, offhandX + 1, offhandY + 1, isPersonaje);

        // 3. CRAFTEO 3x3 (5 RESULTADO, 6..14 GRILLA) -> Pestaña FABRICACIÓN
        int craftGridX = w / 2 - 70;
        int craftGridY = h / 2 - 85;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.menu.setSlotState(6 + col + row * 3, craftGridX + col * 20 + 1, craftGridY + row * 20 + 1, isCrafting);
            }
        }
        this.menu.setSlotState(5, craftGridX + 105 + 1, craftGridY + 20 + 1, isCrafting);

        // 4. MOCHILA (15..59) -> Pestaña MOCHILA
        int backpackX = w / 2 - 81;
        int backpackY = h / 2 - 95;
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 9; col++) {
                this.menu.setSlotState(15 + col + row * 9, backpackX + col * 18 + 1, backpackY + row * 18 + 1, isBackpack);
            }
        }

        // 5. INVENTARIO PRINCIPAL 27 SLOTS (60..86) -> Pestañas PERSONAJE, INVENTARIO, FABRICACIÓN
        int invX = w / 2 - 81;
        int invY = h / 2 + 10;
        if (isInventory) invY = h / 2 - 40;
        boolean invActive = isPersonaje || isInventory || isCrafting;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.menu.setSlotState(60 + col + row * 9, invX + col * 18 + 1, invY + row * 18 + 1, invActive);
            }
        }

        // 6. HOTBAR 9 SLOTS (87..95) -> PERSONAJE, INVENTARIO, FABRICACIÓN, MOCHILA
        int hotbarX = w / 2 - 81;
        int hotbarY = h / 2 + 70;
        if (isInventory) hotbarY = h / 2 + 20;
        if (isBackpack) hotbarY = h / 2 + 10;
        boolean hotbarActive = isPersonaje || isInventory || isCrafting || isBackpack;

        for (int col = 0; col < 9; col++) {
            this.menu.setSlotState(87 + col, hotbarX + col * 18 + 1, hotbarY + 1, hotbarActive);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // NO renderizar background vanilla opaco. El mundo de Minecraft es 100% visible detrás.
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Sin etiquetas vanilla redundantes
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int w = this.width;
        int h = this.height;

        // Fondo transparente gris oscuro que difumina/oscurece suavemente el mundo de Minecraft detrás del inventario
        graphics.fill(0, 0, w, h, 0x66121418);

        Player player = this.minecraft.player;
        String name = player != null ? player.getGameProfile().getName() : "JUGADOR";
        String roleStr = ClientEvents.getClientPlayerRole().toUpperCase();
        if (roleStr.equals("NONE") || roleStr.isEmpty()) roleStr = "ASPIRANTE";

        int level = ClientPacketHandler.hudPlayerLevel;
        int currentXp = ClientPacketHandler.hudCurrentXp;
        int neededXp = ClientPacketHandler.hudNeededXp;

        // 1. BARRA DE NAVEGACIÓN SUPERIOR (Pestañas MMORPG limpias y transparentes)
        int tabWidth = 85;
        int tabHeight = 18;
        Tab[] tabs = Tab.values();
        int totalTabsW = tabs.length * (tabWidth + 6);
        int startX = (w - totalTabsW) / 2;
        int tabY = 12;

        for (int i = 0; i < tabs.length; i++) {
            Tab tab = tabs[i];
            int tx = startX + i * (tabWidth + 6);
            boolean isSelected = (tab == currentTab);
            boolean isHovered = mouseX >= tx && mouseX <= tx + tabWidth && mouseY >= tabY && mouseY <= tabY + tabHeight;

            int bgCol = isSelected ? 0x88182430 : (isHovered ? 0x55222222 : 0x33000000);
            int borderCol = isSelected ? 0xFF88CCFF : (isHovered ? 0xAAFFFFFF : 0x44FFFFFF);

            graphics.fill(tx, tabY, tx + tabWidth, tabY + tabHeight, bgCol);
            graphics.fill(tx, tabY, tx + tabWidth, tabY + 1, borderCol);
            graphics.fill(tx, tabY + tabHeight - 1, tx + tabWidth, tabY + tabHeight, borderCol);

            if (isSelected) {
                graphics.fill(tx, tabY + tabHeight - 2, tx + tabWidth, tabY + tabHeight, 0xFF88CCFF);
            }

            String tabName = tab.name();
            if (tabName.equals("FABRICACION")) tabName = "FABRICACIÓN";
            if (tabName.equals("INFORMACION")) tabName = "INFORMACIÓN";

            int tw = this.font.width(tabName);
            graphics.drawString(this.font, tabName, tx + (tabWidth - tw) / 2, tabY + 5, isSelected ? 0xFF88CCFF : 0xFFCCCCCC, false);
        }

        // 2. SECCIONES / CONTENIDO SEGÚN LA PESTAÑA ACTIVA
        if (currentTab == Tab.PERSONAJE) {
            // Panel izquierdo: Armadura y Segunda Mano
            int armorX = w / 2 - 130;
            int armorY = h / 2 - 80;

            drawTranslucentPanel(graphics, armorX - 6, armorY - 6, 32, 100, "EQUIPO");
            for (int i = 0; i < 4; i++) {
                drawSlotFrame(graphics, armorX, armorY + i * 22, 0x8888CCFF);
            }

            int offhandX = w / 2 - 130;
            int offhandY = h / 2 + 15;
            drawSlotFrame(graphics, offhandX, offhandY, 0x8888CCFF);

            // Modelo 3D del jugador en el centro
            int entityX = w / 2 - 40;
            int entityY = h / 2 + 5;
            if (player != null) {
                InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, entityX, entityY, 48, (float)(entityX) - mouseX, (float)(entityY - 50) - mouseY, player);
            }

            // Panel derecho: Estadísticas RPG compactas
            int statsX = w / 2 + 20;
            int statsY = h / 2 - 80;
            drawTranslucentPanel(graphics, statsX, statsY, 120, 80, "ESTADÍSTICAS");

            graphics.drawString(this.font, "Heroe: " + name, statsX + 6, statsY + 18, 0xFFFFFFFF, false);
            graphics.drawString(this.font, "Rol: " + roleStr, statsX + 6, statsY + 30, 0xFF88FF88, false);
            graphics.drawString(this.font, "Nivel: " + level, statsX + 6, statsY + 42, 0xFF88CCFF, false);
            graphics.drawString(this.font, "XP: " + currentXp + " / " + neededXp, statsX + 6, statsY + 54, 0xFFFFFF88, false);

            // Panel inferior: Inventario principal + Hotbar
            int invX = w / 2 - 81;
            int invY = h / 2 + 10;
            drawInventoryGrid(graphics, invX, invY);

        } else if (currentTab == Tab.INVENTARIO) {
            int invX = w / 2 - 81;
            int invY = h / 2 - 40;
            drawTranslucentPanel(graphics, invX - 10, invY - 20, 182, 90, "INVENTARIO PRINCIPAL");
            drawInventoryGrid(graphics, invX, invY);

        } else if (currentTab == Tab.FABRICACION) {
            int craftGridX = w / 2 - 70;
            int craftGridY = h / 2 - 85;

            drawTranslucentPanel(graphics, craftGridX - 15, craftGridY - 20, 170, 80, "MESA DE FABRICACIÓN");

            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    drawSlotFrame(graphics, craftGridX + col * 20, craftGridY + row * 20, 0x44FFFFFF);
                }
            }
            graphics.drawString(this.font, "➔", craftGridX + 75, craftGridY + 24, 0xFF88CCFF, true);
            drawSlotFrame(graphics, craftGridX + 105, craftGridY + 20, 0xFF55FF55);

            int invX = w / 2 - 81;
            int invY = h / 2 + 10;
            drawInventoryGrid(graphics, invX, invY);

        } else if (currentTab == Tab.MOCHILA) {
            int tier = this.menu.getBackpackTier();
            int backpackX = w / 2 - 81;
            int backpackY = h / 2 - 95;
            int maxUnlocked = tier * 15;

            drawTranslucentPanel(graphics, backpackX - 10, backpackY - 20, 182, 120, "MOCHILA (TIER " + tier + ")");

            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 9; col++) {
                    int slotIdx = col + row * 9;
                    if (slotIdx < maxUnlocked) {
                        drawSlotFrame(graphics, backpackX + col * 18, backpackY + row * 18, 0x44FFFFFF);
                    } else {
                        drawSlotFrame(graphics, backpackX + col * 18, backpackY + row * 18, 0xAA441111);
                        graphics.drawString(this.font, "🔒", backpackX + col * 18 + 5, backpackY + row * 18 + 4, 0xFF662222, false);
                    }
                }
            }

            int hotbarY = h / 2 + 10;
            for (int col = 0; col < 9; col++) {
                drawSlotFrame(graphics, backpackX + col * 18, hotbarY, 0x8888CCFF);
            }

        } else if (currentTab == Tab.INFORMACION) {
            int infoX = w / 2 - 160;
            int infoY = h / 2 - 70;

            drawTranslucentPanel(graphics, infoX, infoY, 150, 100, "DATOS DEL JUGADOR");
            graphics.drawString(this.font, "Nombre: " + name, infoX + 10, infoY + 22, 0xFFFFFFFF, false);
            graphics.drawString(this.font, "Rol: " + roleStr, infoX + 10, infoY + 36, 0xFF88FF88, false);
            graphics.drawString(this.font, "Nivel de Rol: " + level, infoX + 10, infoY + 50, 0xFF88CCFF, false);
            graphics.drawString(this.font, "Experiencia: " + currentXp + " / " + neededXp, infoX + 10, infoY + 64, 0xFFFFFF88, false);

            drawTranslucentPanel(graphics, infoX + 170, infoY, 150, 100, "DATOS DEL REINO");
            graphics.drawString(this.font, "Vidas de Trono: " + ClientPacketHandler.hudThroneLives, infoX + 180, infoY + 22, 0xFFFFFFFF, false);
            graphics.drawString(this.font, "Puntos Compartidos: " + ClientPacketHandler.hudSharedPoints, infoX + 180, infoY + 36, 0xFF88FF88, false);
            graphics.drawString(this.font, "Misión: " + ClientPacketHandler.hudActiveMissionTitle, infoX + 180, infoY + 50, 0xFF88CCFF, false);
            graphics.drawString(this.font, "Progreso: " + ClientPacketHandler.hudActiveMissionProgress, infoX + 180, infoY + 64, 0xFFFFFF88, false);
        }
    }

    private void drawTranslucentPanel(GuiGraphics graphics, int px, int py, int pw, int ph, String title) {
        // Panel translúcido suave (0x55000000 / 0x66000000) permitiendo ver el mundo detrás
        graphics.fill(px, py, px + pw, py + ph, 0x66000000);
        graphics.fill(px, py, px + pw, py + 1, 0x44FFFFFF);
        graphics.fill(px, py, px + 1, py + ph, 0x44FFFFFF);
        graphics.fill(px + pw - 1, py, px + pw, py + ph, 0x22FFFFFF);
        graphics.fill(px, py + ph - 1, px + pw, py + ph, 0x22FFFFFF);

        if (title != null && !title.isEmpty()) {
            graphics.drawString(this.font, title, px + 6, py + 5, 0xFF88CCFF, false);
            graphics.fill(px + 4, py + 15, px + pw - 4, py + 16, 0x22FFFFFF);
        }
    }

    private void drawInventoryGrid(GuiGraphics graphics, int invX, int invY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotFrame(graphics, invX + col * 18, invY + row * 18, 0x44FFFFFF);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotFrame(graphics, invX + col * 18, invY + 60, 0x8888CCFF);
        }
    }

    private void drawSlotFrame(GuiGraphics graphics, int x, int y, int borderColor) {
        // Marco de slot transparente (SIN relleno negro sólido)
        graphics.fill(x, y, x + 18, y + 1, borderColor);
        graphics.fill(x, y, x + 1, y + 18, borderColor);
        graphics.fill(x + 17, y, x + 18, y + 18, 0x22FFFFFF);
        graphics.fill(x, y + 17, x + 18, y + 18, 0x22FFFFFF);
    }
}
