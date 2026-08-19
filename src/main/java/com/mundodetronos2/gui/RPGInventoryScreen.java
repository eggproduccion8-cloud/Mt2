package com.mundodetronos2.gui;

import com.mundodetronos2.client.ClientEvents;
import com.mundodetronos2.client.ClientPacketHandler;
import com.mundodetronos2.role.EquipmentRestrictions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.List;

public class RPGInventoryScreen extends AbstractContainerScreen<RPGInventoryMenu> {

    public static final ResourceLocation LOGO_T2 = new ResourceLocation("mundodetronos2", "textures/gui/t2.png");
    public static final ResourceLocation LOGO_EGG = new ResourceLocation("mundodetronos2", "textures/gui/egg.png");
    public static final ResourceLocation BUTTON_TEX = new ResourceLocation("mundodetronos2", "textures/gui/button.png");
    public static final ResourceLocation SLOTS_TEX = new ResourceLocation("mundodetronos2", "textures/gui/slots.png");
    public static final ResourceLocation SLOTS_HOVER_TEX = new ResourceLocation("mundodetronos2", "textures/gui/slots_hover.png");
    public static final ResourceLocation WEIGHT_ICON = new ResourceLocation("mundodetronos2", "textures/gui/weight_icon.png");

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

        int searchX = this.width - 230;
        int searchY = 55;
        this.searchBox = new EditBox(this.font, searchX, searchY, 180, 18, Component.literal("Buscar receta..."));
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
            int navY = 16;
            int tabWidth = 120;
            int tabHeight = 24;
            Tab[] tabs = Tab.values();
            int startX = (this.width - (tabs.length * (tabWidth + 20))) / 2;

            for (int i = 0; i < tabs.length; i++) {
                int tx = startX + i * (tabWidth + 20);
                if (mouseX >= tx && mouseX <= tx + tabWidth && mouseY >= navY && mouseY <= navY + tabHeight) {
                    setTab(tabs[i]);
                    return true;
                }
            }

            // Recipe paging buttons in FABRICACION tab
            if (currentTab == Tab.FABRICACION) {
                int recipeX = this.width - 240;
                int recipeY = 50;
                int panelHeight = this.height - 80;
                int navButtonsY = recipeY + panelHeight - 25;

                // Next page button
                if (mouseX >= recipeX + 160 && mouseX <= recipeX + 210 && mouseY >= navButtonsY - 5 && mouseY <= navButtonsY + 15) {
                    if ((recipePageIndex + 1) * 3 < filteredRecipes.size()) {
                        recipePageIndex++;
                    }
                    return true;
                }
                // Prev page button
                if (mouseX >= recipeX + 10 && mouseX <= recipeX + 60 && mouseY >= navButtonsY - 5 && mouseY <= navButtonsY + 15) {
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

        // 1. ARMADURA (0..3: Casco, Pechera, Pantalones, Botas) -> Pestaña PERSONAJE (Lado izquierdo)
        int armorX = 40;
        int armorY = h / 2 - 80;
        for (int i = 0; i < 4; i++) {
            this.menu.setSlotState(i, armorX + 5, armorY + i * 36 + 5, isPersonaje);
        }

        // 2. SEGUNDA MANO (4) -> Pestaña PERSONAJE (Lado izquierdo debajo de la armadura)
        int offhandX = 40;
        int offhandY = armorY + 4 * 36 + 10;
        this.menu.setSlotState(4, offhandX + 5, offhandY + 5, isPersonaje);

        // 3. CRAFTEO 3x3 (5 RESULTADO, 6..14 GRILLA) -> Pestaña FABRICACIÓN (Centro Izquierdo)
        int craftGridX = 60;
        int craftGridY = h / 2 - 110;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.menu.setSlotState(6 + col + row * 3, craftGridX + col * 26 + 5, craftGridY + row * 26 + 5, isCrafting);
            }
        }
        this.menu.setSlotState(5, craftGridX + 160 + 5, craftGridY + 26 + 5, isCrafting);

        // 4. MOCHILA (15..59) -> Pestaña MOCHILA (Utiliza gran parte del centro)
        int backpackX = (w - (9 * 26)) / 2;
        int backpackY = 110;
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 9; col++) {
                this.menu.setSlotState(15 + col + row * 9, backpackX + col * 26 + 5, backpackY + row * 26 + 5, isBackpack);
            }
        }

        // 5. INVENTARIO PRINCIPAL 27 SLOTS (60..86)
        int invX = w - 270;
        int invY = h / 2 + 10;
        if (isCrafting) {
            invX = 60;
            invY = h / 2 + 25;
        } else if (isBackpack) {
            invX = (w - (9 * 26)) / 2;
            invY = backpackY + 5 * 26 + 35;
        }

        boolean invActive = isPersonaje || isCrafting || isBackpack;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.menu.setSlotState(60 + col + row * 9, invX + col * 26 + 5, invY + row * 26 + 5, invActive);
            }
        }

        // 6. HOTBAR (87..95) -> Oculta completamente en la GUI I (pertenece solo al HUD)
        for (int col = 0; col < 9; col++) {
            this.menu.setSlotState(87 + col, -1000, -1000, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Desactivadas etiquetas vanilla
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int w = this.width;
        int h = this.height;

        // Fondo atmosférico translúcido integrado con el mundo de Minecraft
        graphics.fill(0, 0, w, h, 0x880A0D12);

        // Líneas decorativas MMORPG superiores e inferiores
        graphics.fill(0, 0, w, 2, 0xFF4488FF);
        graphics.fill(0, 42, w, 43, 0x664488FF);
        graphics.fill(0, h - 2, w, h, 0xFF4488FF);

        Player player = this.minecraft.player;
        String name = player != null ? player.getGameProfile().getName() : "JUGADOR";
        String roleStr = ClientEvents.getClientPlayerRole().toUpperCase();
        if (roleStr.equals("NONE") || roleStr.isEmpty()) roleStr = "ASPIRANTE";

        // LOGO OFICIAL t2.png (Proporción exacta 1672x940 -> 1.778)
        int logoW = 70;
        int logoH = (int) (logoW / 1.7787f);
        graphics.blit(LOGO_T2, 20, 8, 0, 0, logoW, logoH, logoW, logoH);

        // LOGO EGG.png EN ESQUINA INFERIOR IZQUIERDA (Proporción 1:1)
        graphics.blit(LOGO_EGG, 12, h - 32, 0, 0, 24, 24, 24, 24);
        graphics.drawString(this.font, "EGPRODUCCION", 42, h - 24, 0xAAFFFFFF, true);

        // 1. NAVEGACIÓN MMORPG SUPERIOR
        int navY = 16;
        int tabWidth = 120;
        int tabHeight = 24;
        Tab[] tabs = Tab.values();
        int startX = (w - (tabs.length * (tabWidth + 20))) / 2;

        for (int i = 0; i < tabs.length; i++) {
            Tab tab = tabs[i];
            int tx = startX + i * (tabWidth + 20);
            boolean isSelected = (tab == currentTab);
            boolean isHovered = mouseX >= tx && mouseX <= tx + tabWidth && mouseY >= navY && mouseY <= navY + tabHeight;

            if (isSelected) {
                graphics.fill(tx, navY + tabHeight - 2, tx + tabWidth, navY + tabHeight, 0xFFFFD700);
                graphics.fill(tx, navY, tx + tabWidth, navY + tabHeight, 0x33FFD700);
            } else if (isHovered) {
                graphics.fill(tx, navY, tx + tabWidth, navY + tabHeight, 0x22FFFFFF);
            }

            String tabName = tab.name();
            if (tabName.equals("FABRICACION")) tabName = "FABRICACIÓN";

            int tw = this.font.width(tabName);
            graphics.drawString(this.font, tabName, tx + (tabWidth - tw) / 2, navY + 6, isSelected ? 0xFFFFD700 : (isHovered ? 0xFFFFFFFF : 0xAAAAAAAA), true);
        }

        // 2. RENDERING ESPECÍFICO SEGÚN PESTAÑA
        if (currentTab == Tab.PERSONAJE) {
            renderPersonajeTab(graphics, w, h, mouseX, mouseY, player, name, roleStr);
        } else if (currentTab == Tab.FABRICACION) {
            renderFabricacionTab(graphics, w, h, mouseX, mouseY);
        } else if (currentTab == Tab.MOCHILA) {
            renderMochilaTab(graphics, w, h, mouseX, mouseY);
        }
    }

    private void renderPersonajeTab(GuiGraphics graphics, int w, int h, int mouseX, int mouseY, Player player, String name, String roleStr) {
        // IZQUIERDA: EQUIPAMIENTO
        int armorX = 40;
        int armorY = h / 2 - 80;

        drawSectionHeader(graphics, armorX, armorY - 22, 180, "EQUIPAMIENTO");

        String[] armorNames = {"CASCO", "PECHERA", "PANTALONES", "BOTAS"};
        for (int i = 0; i < 4; i++) {
            int sy = armorY + i * 36;
            boolean hovered = mouseX >= armorX && mouseX <= armorX + 26 && mouseY >= sy && mouseY <= sy + 26;
            drawSlotFrame(graphics, armorX, sy, hovered, 26);

            ItemStack armorStack = this.menu.getSlot(i).getItem();
            graphics.drawString(this.font, armorNames[i], armorX + 32, sy + 2, 0x88FFFFFF, false);
            if (!armorStack.isEmpty()) {
                if (!EquipmentRestrictions.isItemAuthorized(armorStack, roleStr)) {
                    graphics.drawString(this.font, "NO CLASS", armorX + 32, sy + 14, 0xFFFF5555, true);
                } else {
                    graphics.drawString(this.font, "EQUIPADO", armorX + 32, sy + 14, 0xFF55FF55, true);
                }
            } else {
                graphics.drawString(this.font, "VACÍO", armorX + 32, sy + 14, 0x44FFFFFF, false);
            }
        }

        // SEGUNDA MANO
        int offhandY = armorY + 4 * 36 + 10;
        boolean offHovered = mouseX >= armorX && mouseX <= armorX + 26 && mouseY >= offhandY && mouseY <= offhandY + 26;
        drawSlotFrame(graphics, armorX, offhandY, offHovered, 26);
        ItemStack offhandStack = this.menu.getSlot(4).getItem();
        graphics.drawString(this.font, "SEGUNDA MANO", armorX + 32, offhandY + 2, 0x88FFFFFF, false);
        if (!offhandStack.isEmpty()) {
            if (!EquipmentRestrictions.isItemAuthorized(offhandStack, roleStr)) {
                graphics.drawString(this.font, "NO CLASS", armorX + 32, offhandY + 14, 0xFFFF5555, true);
            } else {
                graphics.drawString(this.font, "EQUIPADO", armorX + 32, offhandY + 14, 0xFF55FF55, true);
            }
        } else {
            graphics.drawString(this.font, "VACÍO", armorX + 32, offhandY + 14, 0x44FFFFFF, false);
        }

        // CENTRO: PERSONAJE 3D PROTAGONISTA EN GRANDE
        int entityX = w / 2 - 40;
        int entityY = h - 60;
        if (player != null) {
            int modelScale = Math.min(100, h / 7);
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, entityX, entityY, modelScale, (float)(entityX) - mouseX, (float)(entityY - 110) - mouseY, player);
        }

        // DERECHA: ATRIBUTOS Y STATS DEL PERSONAJE
        int statsX = w - 270;
        int statsY = 60;
        drawSectionHeader(graphics, statsX, statsY - 15, 234, "ESTADÍSTICAS RPG");

        int level = ClientPacketHandler.hudPlayerLevel;
        int currentXp = ClientPacketHandler.hudCurrentXp;
        int neededXp = ClientPacketHandler.hudNeededXp;

        graphics.drawString(this.font, "NOMBRE: §f" + name, statsX, statsY + 10, 0xFF88CCFF, true);
        graphics.drawString(this.font, "CLASE: §e" + roleStr, statsX, statsY + 22, 0xFF88CCFF, true);
        graphics.drawString(this.font, "NIVEL: §a" + level + " §7(" + currentXp + "/" + neededXp + " XP)", statsX, statsY + 34, 0xFF88CCFF, true);

        if (player != null) {
            double health = Math.round(player.getHealth() * 10.0) / 10.0;
            double maxHealth = Math.round(player.getMaxHealth() * 10.0) / 10.0;
            double armor = player.getArmorValue();
            double damage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            double speed = Math.round(player.getAttributeValue(Attributes.MOVEMENT_SPEED) * 100.0) / 10.0;

            graphics.drawString(this.font, "SALUD: §c" + health + " / " + maxHealth, statsX, statsY + 50, 0xFFFFFFFF, true);
            graphics.drawString(this.font, "ARMADURA: §9" + armor, statsX, statsY + 62, 0xFFFFFFFF, true);
            graphics.drawString(this.font, "DAÑO BASE: §6" + damage, statsX, statsY + 74, 0xFFFFFFFF, true);
            graphics.drawString(this.font, "VELOCIDAD: §b" + speed, statsX, statsY + 86, 0xFFFFFFFF, true);
        }

        // DERECHA ABAJO: INVENTARIO PRINCIPAL
        int invX = w - 270;
        int invY = h / 2 + 10;
        drawSectionHeader(graphics, invX, invY - 15, 234, "INVENTARIO DEL JUGADOR");
        drawInventoryGrid(graphics, invX, invY, mouseX, mouseY);
    }

    private void renderFabricacionTab(GuiGraphics graphics, int w, int h, int mouseX, int mouseY) {
        // LADO IZQUIERDO: GRILLA DE CRAFTEO 3x3
        int craftGridX = 60;
        int craftGridY = h / 2 - 110;

        drawSectionHeader(graphics, craftGridX, craftGridY - 20, 234, "MESA DE FABRICACIÓN");

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int sx = craftGridX + col * 26;
                int sy = craftGridY + row * 26;
                boolean hov = mouseX >= sx && mouseX <= sx + 26 && mouseY >= sy && mouseY <= sy + 26;
                drawSlotFrame(graphics, sx, sy, hov, 26);
            }
        }

        graphics.drawString(this.font, "➔", craftGridX + 110, craftGridY + 30, 0xFFFFD700, true);

        boolean resHov = mouseX >= craftGridX + 160 && mouseX <= craftGridX + 186 && mouseY >= craftGridY + 26 && mouseY <= craftGridY + 52;
        drawSlotFrame(graphics, craftGridX + 160, craftGridY + 26, resHov, 26);

        ItemStack resultStack = this.menu.getSlot(5).getItem();
        if (!resultStack.isEmpty()) {
            graphics.drawString(this.font, resultStack.getHoverName().getString(), craftGridX + 60, craftGridY + 90, 0xFF55FF55, true);
        } else {
            graphics.drawString(this.font, "Coloca materiales para crear", craftGridX, craftGridY + 90, 0x66FFFFFF, false);
        }

        // ABAJO IZQUIERDA: INVENTARIO DEL JUGADOR
        int invX = 60;
        int invY = h / 2 + 25;
        drawSectionHeader(graphics, invX, invY - 15, 234, "MATERIALES / INVENTARIO");
        drawInventoryGrid(graphics, invX, invY, mouseX, mouseY);

        // LADO DERECHO: GUÍA DE RECETAS RPG INTEGRADA
        int recipeX = w - 240;
        int recipeY = 50;
        int panelWidth = 220;
        int panelHeight = h - 80;

        drawSectionHeader(graphics, recipeX, recipeY - 15, panelWidth, "RECETAS COMPATIBLES");

        int startIdx = recipePageIndex * 3;
        int cardY = recipeY + 30;

        for (int i = startIdx; i < Math.min(startIdx + 3, filteredRecipes.size()); i++) {
            CraftingRecipe rec = filteredRecipes.get(i);
            ItemStack res = rec.getResultItem(this.minecraft.level.registryAccess());

            graphics.fill(recipeX, cardY, recipeX + panelWidth, cardY + 50, 0x44000000);
            graphics.fill(recipeX, cardY, recipeX + panelWidth, cardY + 1, 0x22FFFFFF);

            // Vista previa grilla 3x3 de ingredientes
            var ingredients = rec.getIngredients();
            for (int ingIdx = 0; ingIdx < ingredients.size() && ingIdx < 9; ingIdx++) {
                int ingRow = ingIdx / 3;
                int ingCol = ingIdx % 3;
                var items = ingredients.get(ingIdx).getItems();
                if (items.length > 0) {
                    ItemStack ingStack = items[0];
                    int ix = recipeX + 6 + ingCol * 12;
                    int iy = cardY + 6 + ingRow * 12;
                    graphics.pose().pushPose();
                    graphics.pose().translate(ix, iy, 0);
                    graphics.pose().scale(0.6f, 0.6f, 1.0f);
                    graphics.renderItem(ingStack, 0, 0);
                    graphics.pose().popPose();
                }
            }

            graphics.drawString(this.font, "➔", recipeX + 50, cardY + 18, 0xFFFFD700, false);
            graphics.renderItem(res, recipeX + 70, cardY + 14);

            String itemName = res.getHoverName().getString();
            if (itemName.length() > 16) itemName = itemName.substring(0, 14) + "..";
            graphics.drawString(this.font, itemName, recipeX + 95, cardY + 18, 0xFFFFFFFF, false);

            cardY += 56;
        }

        // Paginación
        int totalPages = Math.max(1, (int) Math.ceil((double) filteredRecipes.size() / 3.0));
        int navButtonsY = recipeY + panelHeight - 25;

        graphics.fill(recipeX + 10, navButtonsY - 2, recipeX + 60, navButtonsY + 14, 0x44FFFFFF);
        graphics.drawString(this.font, "◀ ANTERIOR", recipeX + 14, navButtonsY + 2, 0xFFFFD700, false);

        graphics.drawString(this.font, (recipePageIndex + 1) + " / " + totalPages, recipeX + 95, navButtonsY + 2, 0xFF88CCFF, false);

        graphics.fill(recipeX + 160, navButtonsY - 2, recipeX + 210, navButtonsY + 14, 0x44FFFFFF);
        graphics.drawString(this.font, "SIGUIENTE ▶", recipeX + 164, navButtonsY + 2, 0xFFFFD700, false);
    }

    private void renderMochilaTab(GuiGraphics graphics, int w, int h, int mouseX, int mouseY) {
        int tier = this.menu.getBackpackTier();
        int backpackX = (w - (9 * 26)) / 2;
        int backpackY = 110;
        int maxUnlocked = tier * 15;

        // ENCABEZADO DE MOCHILA Y PESO
        int headerY = 55;
        drawSectionHeader(graphics, backpackX, headerY, 234, "MOCHILA DEL AVENTURERO (TIER " + tier + ")");

        // DIBUJAR ICONO DE PESO CON PROPORCIÓN EXACTA (496x503 -> 0.986)
        int iconW = 20;
        int iconH = (int)(iconW / 0.986f);
        graphics.blit(WEIGHT_ICON, backpackX, headerY + 20, 0, 0, iconW, iconH, iconW, iconH);

        graphics.drawString(this.font, "CAPACIDAD DESBLOQUEADA: §a" + maxUnlocked + " / 45 SLOTS", backpackX + 28, headerY + 24, 0xFFFFFFFF, true);

        // GRID DE MOCHILA (45 SLOTS)
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIdx = col + row * 9;
                int sx = backpackX + col * 26;
                int sy = backpackY + row * 26;
                boolean hov = mouseX >= sx && mouseX <= sx + 26 && mouseY >= sy && mouseY <= sy + 26;

                if (slotIdx < maxUnlocked) {
                    drawSlotFrame(graphics, sx, sy, hov, 26);
                } else {
                    drawSlotFrame(graphics, sx, sy, false, 26);
                    graphics.fill(sx + 1, sy + 1, sx + 25, sy + 25, 0x88330000);
                    graphics.drawString(this.font, "🔒", sx + 7, sy + 7, 0xFFFF5555, false);
                }
            }
        }

        // ABAJO: INVENTARIO PRINCIPAL
        int invY = backpackY + 5 * 26 + 35;
        drawSectionHeader(graphics, backpackX, invY - 15, 234, "INVENTARIO DEL JUGADOR");
        drawInventoryGrid(graphics, backpackX, invY, mouseX, mouseY);
    }

    private void drawSectionHeader(GuiGraphics graphics, int x, int y, int width, String title) {
        graphics.drawString(this.font, "§l" + title, x, y, 0xFFFFD700, true);
        graphics.fill(x, y + 11, x + width, y + 12, 0x884488FF);
    }

    private void drawInventoryGrid(GuiGraphics graphics, int invX, int invY, int mouseX, int mouseY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = invX + col * 26;
                int sy = invY + row * 26;
                boolean hov = mouseX >= sx && mouseX <= sx + 26 && mouseY >= sy && mouseY <= sy + 26;
                drawSlotFrame(graphics, sx, sy, hov, 26);
            }
        }
    }

    private void drawSlotFrame(GuiGraphics graphics, int x, int y, boolean hovered, int size) {
        ResourceLocation tex = hovered ? SLOTS_HOVER_TEX : SLOTS_TEX;
        graphics.blit(tex, x, y, 0, 0, size, size, size, size);
    }
}
