package com.mundodetronos2.gui;

import com.mundodetronos2.client.ClientEvents;
import com.mundodetronos2.client.ClientPacketHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;

public class RPGInventoryScreen extends AbstractContainerScreen<RPGInventoryMenu> {

    public static final ResourceLocation LOGO_T2 = new ResourceLocation("mundodetronos2", "textures/gui/t2.png");
    public static final ResourceLocation LOGO_EGG = new ResourceLocation("mundodetronos2", "textures/gui/egg.png");
    public static final ResourceLocation BUTTON_TEX = new ResourceLocation("mundodetronos2", "textures/gui/button.png");

    public enum Tab {
        PERSONAJE,
        FABRICACION,
        MOCHILA
    }

    private Tab currentTab = Tab.PERSONAJE;

    // JEI-like Recipe Viewer fields
    private EditBox searchBox;
    private List<CraftingRecipe> filteredRecipes = new ArrayList<>();
    private int recipePageIndex = 0;
    private String lastSearchQuery = "";

    public RPGInventoryScreen(RPGInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = 0;
        this.topPos = 0;
        this.imageWidth = this.width;
        this.imageHeight = this.height;

        int searchX = this.width / 2 + 105;
        int searchY = this.height / 2 - 80;
        this.searchBox = new EditBox(this.font, searchX, searchY, 110, 16, Component.literal("Buscar receta..."));
        this.searchBox.setHighlightPos(0);
        this.searchBox.setTextColor(0xFFFFFFFF);
        this.searchBox.setHint(Component.literal("Buscar receta..."));
        this.addRenderableWidget(this.searchBox);

        updateSlotPositions();
        updateRecipeList();
    }

    private void updateRecipeList() {
        if (this.minecraft == null || this.minecraft.level == null) return;
        List<CraftingRecipe> allRecipes = this.minecraft.level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING);
        String query = searchBox != null ? searchBox.getValue().toLowerCase().trim() : "";

        filteredRecipes.clear();
        for (CraftingRecipe recipe : allRecipes) {
            ItemStack result = recipe.getResultItem(this.minecraft.level.registryAccess());
            if (result.isEmpty()) continue;
            String itemName = result.getHoverName().getString().toLowerCase();
            if (query.isEmpty() || itemName.contains(query)) {
                filteredRecipes.add(recipe);
            }
        }
        recipePageIndex = 0;
    }

    private void setTab(Tab tab) {
        this.currentTab = tab;
        updateSlotPositions();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (currentTab == Tab.FABRICACION && this.searchBox != null && this.searchBox.isFocused()) {
            if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                if (!lastSearchQuery.equals(this.searchBox.getValue())) {
                    lastSearchQuery = this.searchBox.getValue();
                    updateRecipeList();
                }
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (currentTab == Tab.FABRICACION && this.searchBox != null && this.searchBox.isFocused()) {
            if (this.searchBox.charTyped(codePoint, modifiers)) {
                if (!lastSearchQuery.equals(this.searchBox.getValue())) {
                    lastSearchQuery = this.searchBox.getValue();
                    updateRecipeList();
                }
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int startY = 32;
            int tabWidth = 100;
            int tabHeight = 22;
            Tab[] tabs = Tab.values();
            int totalTabsW = tabs.length * (tabWidth + 10);
            int startX = (this.width - totalTabsW) / 2;

            for (int i = 0; i < tabs.length; i++) {
                int tx = startX + i * (tabWidth + 10);
                if (mouseX >= tx && mouseX <= tx + tabWidth && mouseY >= startY && mouseY <= startY + tabHeight) {
                    setTab(tabs[i]);
                    return true;
                }
            }

            // Recipe paging buttons in FABRICACION tab
            if (currentTab == Tab.FABRICACION) {
                int panelX = this.width / 2 + 105;
                int panelY = this.height / 2 - 80;
                // Next page button
                if (mouseX >= panelX + 85 && mouseX <= panelX + 110 && mouseY >= panelY + 145 && mouseY <= panelY + 160) {
                    if ((recipePageIndex + 1) * 3 < filteredRecipes.size()) {
                        recipePageIndex++;
                    }
                    return true;
                }
                // Prev page button
                if (mouseX >= panelX && mouseX <= panelX + 25 && mouseY >= panelY + 145 && mouseY <= panelY + 160) {
                    if (recipePageIndex > 0) {
                        recipePageIndex--;
                    }
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
        boolean isCrafting = currentTab == Tab.FABRICACION;
        boolean isBackpack = currentTab == Tab.MOCHILA;

        if (this.searchBox != null) {
            this.searchBox.setVisible(isCrafting);
        }

        // 1. ARMADURA (0..3) -> Pestaña PERSONAJE (Lado izquierdo, cuadros grandes)
        int armorX = w / 2 - 180;
        int armorY = h / 2 - 70;
        for (int i = 0; i < 4; i++) {
            this.menu.setSlotState(i, armorX + 5, armorY + i * 26 + 5, isPersonaje);
        }

        // 2. SEGUNDA MANO (4) -> Pestaña PERSONAJE
        int offhandX = w / 2 - 180;
        int offhandY = h / 2 + 40;
        this.menu.setSlotState(4, offhandX + 5, offhandY + 5, isPersonaje);

        // 3. CRAFTEO 3x3 (5 RESULTADO, 6..14 GRILLA) -> Pestaña FABRICACIÓN
        int craftGridX = w / 2 - 140;
        int craftGridY = h / 2 - 80;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.menu.setSlotState(6 + col + row * 3, craftGridX + col * 20 + 1, craftGridY + row * 20 + 1, isCrafting);
            }
        }
        this.menu.setSlotState(5, craftGridX + 115 + 1, craftGridY + 20 + 1, isCrafting);

        // 4. MOCHILA (15..59) -> Pestaña MOCHILA
        int backpackX = w / 2 - 81;
        int backpackY = h / 2 - 90;
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 9; col++) {
                this.menu.setSlotState(15 + col + row * 9, backpackX + col * 18 + 1, backpackY + row * 18 + 1, isBackpack);
            }
        }

        // 5. INVENTARIO PRINCIPAL 27 SLOTS (60..86) -> PERSONAJE (Lado Derecho), FABRICACIÓN (Abajo)
        int invX = w / 2 + 30;
        int invY = h / 2 - 60;
        if (isCrafting) {
            invX = w / 2 - 140;
            invY = h / 2 + 5;
        }

        boolean invActive = isPersonaje || isCrafting;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.menu.setSlotState(60 + col + row * 9, invX + col * 18 + 1, invY + row * 18 + 1, invActive);
            }
        }

        // 6. HOTBAR 9 SLOTS (87..95) -> PERSONAJE, FABRICACIÓN, MOCHILA
        int hotbarX = w / 2 + 30;
        int hotbarY = h / 2 + 5, isBackpackHotbarY = h / 2 + 20;
        if (isCrafting) {
            hotbarX = w / 2 - 140;
            hotbarY = h / 2 + 65;
        } else if (isBackpack) {
            hotbarX = w / 2 - 81;
            hotbarY = isBackpackHotbarY;
        }

        boolean hotbarActive = isPersonaje || isCrafting || isBackpack;
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

        // LOGO OFICIAL t2.png EN LA PARTE SUPERIOR DE LA GUI
        graphics.blit(LOGO_T2, w / 2 - 40, 4, 0, 0, 80, 24, 80, 24);

        // LOGO EGG.png EN LA ESQUINA INFERIOR IZQUIERDA (CRÉDITOS PRODUCCIÓN)
        graphics.blit(LOGO_EGG, 10, h - 35, 0, 0, 30, 30, 30, 30);
        graphics.drawString(this.font, "Production Credits", 44, h - 22, 0xAAFFFFFF, true);

        // 1. BARRA DE NAVEGACIÓN SUPERIOR (Pestañas estilizadas con button.png)
        int tabWidth = 100;
        int tabHeight = 22;
        Tab[] tabs = Tab.values();
        int totalTabsW = tabs.length * (tabWidth + 10);
        int startX = (w - totalTabsW) / 2;
        int tabY = 32;

        for (int i = 0; i < tabs.length; i++) {
            Tab tab = tabs[i];
            int tx = startX + i * (tabWidth + 10);
            boolean isSelected = (tab == currentTab);
            boolean isHovered = mouseX >= tx && mouseX <= tx + tabWidth && mouseY >= tabY && mouseY <= tabY + tabHeight;

            RenderSystem.setShaderColor(1.0f, isSelected ? 1.0f : 0.7f, isSelected ? 1.0f : 0.7f, 1.0f);
            graphics.blit(BUTTON_TEX, tx, tabY, 0, isSelected ? 20 : (isHovered ? 10 : 0), tabWidth, tabHeight, tabWidth, tabHeight);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            String tabName = tab.name();
            if (tabName.equals("FABRICACION")) tabName = "FABRICACIÓN";

            int tw = this.font.width(tabName);
            graphics.drawString(this.font, tabName, tx + (tabWidth - tw) / 2, tabY + 6, isSelected ? 0xFFFFFF00 : 0xFFFFFFFF, true);
        }

        // 2. SECCIONES / CONTENIDO SEGÚN LA PESTAÑA ACTIVA
        if (currentTab == Tab.PERSONAJE) {
            // LADO IZQUIERDO: ARMADURA (Cuadros Grandes) Y SEGUNDA MANO
            int armorX = w / 2 - 180;
            int armorY = h / 2 - 70;

            drawTranslucentPanel(graphics, armorX - 6, armorY - 20, 48, 145, "EQUIPO");
            for (int i = 0; i < 4; i++) {
                drawLargeSlotFrame(graphics, armorX, armorY + i * 26, 0xFF88CCFF);
            }

            int offhandX = w / 2 - 180;
            int offhandY = h / 2 + 40;
            drawLargeSlotFrame(graphics, offhandX, offhandY, 0xFFFFD700);

            // CENTRO: MODELO 3D EN GRANDE DE ESPALDAS / MIRANDO AL INVENTARIO (Sin rotar con la cámara)
            int entityX = w / 2 - 50;
            int entityY = h / 2 + 60;
            if (player != null) {
                // Pose fija de espaldas mirando hacia el inventario
                InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, entityX, entityY, 70, -35.0f, 10.0f, player);
            }

            // LADO DERECHO: INVENTARIO PRINCIPAL Y HOTBAR
            int invX = w / 2 + 30;
            int invY = h / 2 - 60;
            drawTranslucentPanel(graphics, invX - 10, invY - 20, 182, 95, "INVENTARIO (" + name + ")");
            drawInventoryGrid(graphics, invX, invY);

        } else if (currentTab == Tab.FABRICACION) {
            int craftGridX = w / 2 - 140;
            int craftGridY = h / 2 - 80;

            drawTranslucentPanel(graphics, craftGridX - 10, craftGridY - 20, 220, 80, "FABRICACIÓN");

            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    drawSlotFrame(graphics, craftGridX + col * 20, craftGridY + row * 20, 0x88FFFFFF);
                }
            }
            graphics.drawString(this.font, "➔", craftGridX + 80, craftGridY + 24, 0xFF88CCFF, true);
            drawSlotFrame(graphics, craftGridX + 115, craftGridY + 20, 0xFF55FF55);

            int invX = w / 2 - 140;
            int invY = h / 2 + 5;
            drawTranslucentPanel(graphics, invX - 10, invY - 15, 182, 95, "INVENTARIO");
            drawInventoryGrid(graphics, invX, invY);

            // JEI-LIKE RECIPE VIEWER ON THE RIGHT SIDE
            int recipeX = w / 2 + 100;
            int recipeY = h / 2 - 80;
            drawTranslucentPanel(graphics, recipeX - 5, recipeY - 20, 125, 175, "GUÍA DE RECETAS");

            // Draw filtered recipes
            int startIdx = recipePageIndex * 3;
            int currentR = 0;
            for (int i = startIdx; i < Math.min(startIdx + 3, filteredRecipes.size()); i++) {
                CraftingRecipe rec = filteredRecipes.get(i);
                ItemStack res = rec.getResultItem(this.minecraft.level.registryAccess());
                int ry = recipeY + 25 + currentR * 38;

                graphics.fill(recipeX, ry, recipeX + 115, ry + 34, 0x44000000);
                graphics.renderItem(res, recipeX + 4, ry + 8);
                graphics.drawString(this.font, res.getHoverName().getString(), recipeX + 24, ry + 12, 0xFFFFFFFF, false);

                currentR++;
            }

            // Paging text & buttons
            int totalPages = Math.max(1, (int) Math.ceil((double) filteredRecipes.size() / 3.0));
            graphics.drawString(this.font, (recipePageIndex + 1) + "/" + totalPages, recipeX + 45, recipeY + 148, 0xFF88CCFF, false);
            graphics.drawString(this.font, "◀", recipeX + 8, recipeY + 148, 0xFFFFD700, false);
            graphics.drawString(this.font, "▶", recipeX + 92, recipeY + 148, 0xFFFFD700, false);

        } else if (currentTab == Tab.MOCHILA) {
            int tier = this.menu.getBackpackTier();
            int backpackX = w / 2 - 81;
            int backpackY = h / 2 - 90;
            int maxUnlocked = tier * 15;

            drawTranslucentPanel(graphics, backpackX - 10, backpackY - 20, 182, 120, "MOCHILA (TIER " + tier + ")");

            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 9; col++) {
                    int slotIdx = col + row * 9;
                    if (slotIdx < maxUnlocked) {
                        drawSlotFrame(graphics, backpackX + col * 18, backpackY + row * 18, 0x88FFFFFF);
                    } else {
                        drawSlotFrame(graphics, backpackX + col * 18, backpackY + row * 18, 0xAA441111);
                        graphics.drawString(this.font, "🔒", backpackX + col * 18 + 5, backpackY + row * 18 + 4, 0xFF662222, false);
                    }
                }
            }

            int hotbarY = h / 2 + 20;
            drawTranslucentPanel(graphics, backpackX - 10, hotbarY - 15, 182, 35, "HOTBAR");
            for (int col = 0; col < 9; col++) {
                drawSlotFrame(graphics, backpackX + col * 18, hotbarY, 0x8888CCFF);
            }
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
                drawSlotFrame(graphics, invX + col * 18, invY + row * 18, 0x66FFFFFF);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotFrame(graphics, invX + col * 18, invY + 60, 0xFF88CCFF);
        }
    }

    private void drawLargeSlotFrame(GuiGraphics graphics, int x, int y, int borderColor) {
        graphics.fill(x, y, x + 26, y + 26, 0x44000000);
        graphics.fill(x, y, x + 26, y + 1, borderColor);
        graphics.fill(x, y, x + 1, y + 26, borderColor);
        graphics.fill(x + 25, y, x + 26, y + 26, borderColor);
        graphics.fill(x, y + 25, x + 26, y + 26, borderColor);
    }

    private void drawSlotFrame(GuiGraphics graphics, int x, int y, int borderColor) {
        graphics.fill(x, y, x + 18, y + 1, borderColor);
        graphics.fill(x, y, x + 1, y + 18, borderColor);
        graphics.fill(x + 17, y, x + 18, y + 18, 0x22FFFFFF);
        graphics.fill(x, y + 17, x + 18, y + 18, 0x22FFFFFF);
    }
}
