package com.mundodetronos2.gui;

import com.mundodetronos2.client.ClientEvents;
import com.mundodetronos2.client.ClientPacketHandler;
import com.mundodetronos2.role.EquipmentRestrictions;
import com.mojang.blaze3d.systems.RenderSystem;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public enum RecipeCategory {
        TODOS,
        VANILLA,
        MODS,
        MATERIALES,
        BLOQUES,
        ARMAS,
        ARMADURA,
        UTILIDAD,
        FAVORITOS,
        RECIENTES
    }

    private Tab currentTab = Tab.PERSONAJE;
    private RecipeCategory currentCategory = RecipeCategory.TODOS;

    // Search and Catalog fields
    private EditBox searchBox;
    private List<CraftingRecipe> filteredRecipes = new ArrayList<>();
    private static final Set<ResourceLocation> favoriteRecipes = new HashSet<>();
    private static final List<CraftingRecipe> recentRecipes = new ArrayList<>();
    private CraftingRecipe selectedRecipe = null;
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

        int catalogX = (int) (this.width * 0.45);
        int catalogWidth = this.width - catalogX - 20;

        this.searchBox = new EditBox(this.font, catalogX + 10, 52, catalogWidth - 20, 18, Component.literal("Buscar receta..."));
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

        if (currentCategory == RecipeCategory.FAVORITOS) {
            for (CraftingRecipe r : allRecipes) {
                if (favoriteRecipes.contains(r.getId())) {
                    if (matchesQuery(r, query)) filteredRecipes.add(r);
                }
            }
        } else if (currentCategory == RecipeCategory.RECIENTES) {
            for (CraftingRecipe r : recentRecipes) {
                if (matchesQuery(r, query)) filteredRecipes.add(r);
            }
        } else {
            for (CraftingRecipe recipe : allRecipes) {
                ItemStack result = recipe.getResultItem(this.minecraft.level.registryAccess());
                if (result.isEmpty()) continue;

                if (currentCategory != RecipeCategory.TODOS && !matchesCategory(result, currentCategory)) {
                    continue;
                }

                if (matchesQuery(recipe, query)) {
                    filteredRecipes.add(recipe);
                }
            }
        }
        recipePageIndex = 0;
    }

    private boolean matchesQuery(CraftingRecipe recipe, String query) {
        if (query.isEmpty()) return true;
        ItemStack result = recipe.getResultItem(this.minecraft.level.registryAccess());
        return result.getHoverName().getString().toLowerCase().contains(query);
    }

    private boolean matchesCategory(ItemStack stack, RecipeCategory category) {
        String name = stack.getHoverName().getString().toLowerCase();
        String itemPath = stack.getItem().toString().toLowerCase();
        String namespace = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();

        switch (category) {
            case VANILLA:
                return namespace.equals("minecraft");
            case MODS:
                return !namespace.equals("minecraft");
            case ARMAS:
                return itemPath.contains("sword") || itemPath.contains("bow") || itemPath.contains("crossbow") || itemPath.contains("trident") || name.contains("espada") || name.contains("arco");
            case ARMADURA:
                return itemPath.contains("helmet") || itemPath.contains("chestplate") || itemPath.contains("leggings") || itemPath.contains("boots") || name.contains("casco") || name.contains("pechera");
            case BLOQUES:
                return itemPath.contains("block") || itemPath.contains("planks") || itemPath.contains("stone") || itemPath.contains("brick");
            case MATERIALES:
                return itemPath.contains("ingot") || itemPath.contains("gem") || itemPath.contains("stick") || itemPath.contains("nugget") || itemPath.contains("leather");
            case UTILIDAD:
                return itemPath.contains("bucket") || itemPath.contains("torch") || itemPath.contains("compass") || itemPath.contains("clock") || itemPath.contains("map");
            default:
                return true;
        }
    }

    private void setTab(Tab tab) {
        this.currentTab = tab;
        if (this.searchBox != null) {
            this.searchBox.setVisible(tab == Tab.FABRICACION);
        }
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
        int w = this.width;
        int h = this.height;

        if (button == 0) {
            // Navigation tabs
            int navY = 12;
            int tabWidth = 120;
            int tabHeight = 22;
            Tab[] tabs = Tab.values();
            int startX = (w - (tabs.length * (tabWidth + 15))) / 2;

            for (int i = 0; i < tabs.length; i++) {
                int tx = startX + i * (tabWidth + 15);
                if (mouseX >= tx && mouseX <= tx + tabWidth && mouseY >= navY && mouseY <= navY + tabHeight) {
                    setTab(tabs[i]);
                    return true;
                }
            }

            // Fabricar button & Recipe paging buttons in FABRICACION tab
            if (currentTab == Tab.FABRICACION) {
                int craftGridX = 30;
                int craftGridY = 55;
                int btnX = craftGridX;
                int btnY = craftGridY + 105;
                int btnW = 90;
                int btnH = 20;

                // Click on FABRICAR button
                if (selectedRecipe != null && mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                    // Auto-transfer recipe ingredients to crafting grid
                    autoTransferRecipeIngredients(selectedRecipe);
                    return true;
                }
                int catalogX = (int) (w * 0.45);
                int catalogWidth = w - catalogX - 20;

                // Category filter buttons
                int catY = 75;
                int catW = 60;
                int catH = 16;
                RecipeCategory[] categories = RecipeCategory.values();
                for (int i = 0; i < categories.length; i++) {
                    int cx = catalogX + 10 + (i % 5) * (catW + 4);
                    int cy = catY + (i / 5) * (catH + 4);
                    if (mouseX >= cx && mouseX <= cx + catW && mouseY >= cy && mouseY <= cy + catH) {
                        this.currentCategory = categories[i];
                        updateRecipeList();
                        return true;
                    }
                }

                // Recipe items grid click
                int gridY = catY + 40;
                int itemCols = Math.max(4, (catalogWidth - 20) / 32);
                int itemRows = Math.max(3, (h - gridY - 50) / 32);
                int pageSize = itemCols * itemRows;

                int startIdx = recipePageIndex * pageSize;
                for (int i = 0; i < pageSize && (startIdx + i) < filteredRecipes.size(); i++) {
                    int col = i % itemCols;
                    int row = i / itemCols;
                    int ix = catalogX + 10 + col * 32;
                    int iy = gridY + row * 32;

                    if (mouseX >= ix && mouseX <= ix + 28 && mouseY >= iy && mouseY <= iy + 28) {
                        CraftingRecipe rec = filteredRecipes.get(startIdx + i);
                        this.selectedRecipe = rec;
                        if (!recentRecipes.contains(rec)) {
                            recentRecipes.add(0, rec);
                            if (recentRecipes.size() > 20) recentRecipes.remove(recentRecipes.size() - 1);
                        }
                        return true;
                    }
                }

                // Paging buttons
                int navButtonsY = h - 35;
                if (mouseX >= catalogX + 10 && mouseX <= catalogX + 80 && mouseY >= navButtonsY && mouseY <= navButtonsY + 20) {
                    if (recipePageIndex > 0) recipePageIndex--;
                    return true;
                }
                int totalPages = Math.max(1, (int) Math.ceil((double) filteredRecipes.size() / (double) pageSize));
                if (mouseX >= catalogX + catalogWidth - 80 && mouseX <= catalogX + catalogWidth - 10 && mouseY >= navButtonsY && mouseY <= navButtonsY + 20) {
                    if (recipePageIndex < totalPages - 1) recipePageIndex++;
                    return true;
                }
            }
        } else if (button == 1) { // Right Click to toggle Favorite
            if (currentTab == Tab.FABRICACION) {
                int catalogX = (int) (w * 0.45);
                int catalogWidth = w - catalogX - 20;
                int gridY = 75 + 40;
                int itemCols = Math.max(4, (catalogWidth - 20) / 32);
                int itemRows = Math.max(3, (h - gridY - 50) / 32);
                int pageSize = itemCols * itemRows;

                int startIdx = recipePageIndex * pageSize;
                for (int i = 0; i < pageSize && (startIdx + i) < filteredRecipes.size(); i++) {
                    int col = i % itemCols;
                    int row = i / itemCols;
                    int ix = catalogX + 10 + col * 32;
                    int iy = gridY + row * 32;

                    if (mouseX >= ix && mouseX <= ix + 28 && mouseY >= iy && mouseY <= iy + 28) {
                        CraftingRecipe rec = filteredRecipes.get(startIdx + i);
                        if (favoriteRecipes.contains(rec.getId())) {
                            favoriteRecipes.remove(rec.getId());
                        } else {
                            favoriteRecipes.add(rec.getId());
                        }
                        updateRecipeList();
                        return true;
                    }
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

        // 1. ARMADURA (0..3) -> Pestaña PERSONAJE (Lado izquierdo)
        int armorX = 30;
        int armorY = 50;
        for (int i = 0; i < 4; i++) {
            this.menu.setSlotState(i, armorX + 5, armorY + i * 32 + 5, isPersonaje);
        }

        // 2. SEGUNDA MANO (4) -> Pestaña PERSONAJE (Lado izquierdo)
        int offhandY = armorY + 4 * 32 + 10;
        this.menu.setSlotState(4, armorX + 5, offhandY + 5, isPersonaje);

        // 3. CRAFTEO 3x3 (5 RESULTADO, 6..14 GRILLA) -> Pestaña FABRICACIÓN (Lado Izquierdo)
        int craftGridX = 30;
        int craftGridY = 55;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.menu.setSlotState(6 + col + row * 3, craftGridX + col * 26 + 5, craftGridY + row * 26 + 5, isCrafting);
            }
        }
        this.menu.setSlotState(5, craftGridX + 140 + 5, craftGridY + 26 + 5, isCrafting);

        // 4. MOCHILA (15..59) -> Pestaña MOCHILA
        int backpackX = (w - (9 * 26)) / 2;
        int backpackY = 100;
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 9; col++) {
                this.menu.setSlotState(15 + col + row * 9, backpackX + col * 26 + 5, backpackY + row * 26 + 5, isBackpack);
            }
        }

        // 5. INVENTARIO PRINCIPAL REAL DEL JUGADOR 27 SLOTS (60..86)
        int invX = (w - (9 * 26)) / 2;
        int invY = h - 90;
        if (isPersonaje) {
            invX = w / 2 + 30;
            invY = 70;
        } else if (isCrafting) {
            invX = 30;
            invY = h - 95;
        } else if (isBackpack) {
            invX = (w - (9 * 26)) / 2;
            invY = backpackY + 5 * 26 + 25;
        }

        boolean invActive = isPersonaje || isCrafting || isBackpack;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.menu.setSlotState(60 + col + row * 9, invX + col * 26 + 5, invY + row * 26 + 5, invActive);
            }
        }

        // 6. HOTBAR (87..95) -> Mantenida funcionalmente en el inventario
        int hotbarY = invY + 80;
        for (int col = 0; col < 9; col++) {
            this.menu.setSlotState(87 + col, invX + col * 26 + 5, hotbarY + 5, invActive);
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

        // Fondo transparente limpio sin paneles azules o líneas divisorias
        graphics.fill(0, 0, w, h, 0x880A0D12);

        Player player = this.minecraft.player;
        String name = player != null ? player.getGameProfile().getName() : "JUGADOR";
        String roleStr = ClientEvents.getClientPlayerRole().toUpperCase();
        if (roleStr.equals("NONE") || roleStr.isEmpty()) roleStr = "ASPIRANTE";

        // LOGO EGG.png EN ESQUINA INFERIOR IZQUIERDA (Proporción 1:1)
        graphics.blit(LOGO_EGG, 12, h - 30, 0, 0, 22, 22, 1254, 1254);
        graphics.drawString(this.font, "EGPRODUCCION", 38, h - 22, 0xAAFFFFFF, true);

        // 1. NAVEGACIÓN SUPERIOR CON BUTTON.PNG
        int navY = 12;
        int tabWidth = 120;
        int tabHeight = 22;
        Tab[] tabs = Tab.values();
        int startX = (w - (tabs.length * (tabWidth + 15))) / 2;

        for (int i = 0; i < tabs.length; i++) {
            Tab tab = tabs[i];
            int tx = startX + i * (tabWidth + 15);
            boolean isSelected = (tab == currentTab);
            boolean isHovered = mouseX >= tx && mouseX <= tx + tabWidth && mouseY >= navY && mouseY <= navY + tabHeight;

            RenderSystem.setShaderColor(1.0f, isSelected ? 1.0f : (isHovered ? 0.9f : 0.6f), isSelected ? 1.0f : (isHovered ? 0.9f : 0.6f), 1.0f);
            graphics.blit(BUTTON_TEX, tx, navY, 0, isSelected ? 20 : (isHovered ? 10 : 0), tabWidth, tabHeight, tabWidth, tabHeight);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            String tabName = tab.name();
            if (tabName.equals("FABRICACION")) tabName = "FABRICACIÓN";

            int tw = this.font.width(tabName);
            graphics.drawString(this.font, tabName, tx + (tabWidth - tw) / 2, navY + 6, isSelected ? 0xFFFFD700 : 0xFFFFFFFF, true);
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
        // LADO IZQUIERDO: EQUIPAMIENTO
        int armorX = 30;
        int armorY = 50;

        graphics.drawString(this.font, "EQUIPAMIENTO", armorX, armorY - 14, 0xFFFFD700, true);

        String[] armorNames = {"CASCO", "PECHERA", "PANTALONES", "BOTAS"};
        for (int i = 0; i < 4; i++) {
            int sy = armorY + i * 30;
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
        int offhandY = armorY + 4 * 30 + 6;
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

        // CENTRO-IZQUIERDA: PERSONAJE 3D LIGERAMENTE MÁS PEQUEÑO Y CENTRADO
        int entityX = w / 3;
        int entityY = h - 60;
        if (player != null) {
            int modelScale = Math.min(130, h / 3);
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, entityX, entityY, modelScale, 0.0f, 0.0f, player);
        }

        // ATRIBUTOS Y STATS DEL PERSONAJE (Acomodados debajo del modelo 3D o a la izquierda)
        int statsX = 30;
        int statsY = offhandY + 36;
        graphics.drawString(this.font, "ESTADÍSTICAS RPG", statsX, statsY, 0xFFFFD700, true);

        int level = ClientPacketHandler.hudPlayerLevel;
        int currentXp = ClientPacketHandler.hudCurrentXp;
        int neededXp = ClientPacketHandler.hudNeededXp;

        graphics.drawString(this.font, "NOMBRE: §f" + name, statsX, statsY + 14, 0xFFFFFFFF, true);
        graphics.drawString(this.font, "CLASE: §e" + roleStr, statsX, statsY + 26, 0xFFFFFFFF, true);
        graphics.drawString(this.font, "NIVEL: §a" + level + " §7(" + currentXp + "/" + neededXp + " XP)", statsX, statsY + 38, 0xFFFFFFFF, true);

        if (player != null) {
            double health = Math.round(player.getHealth() * 10.0) / 10.0;
            double maxHealth = Math.round(player.getMaxHealth() * 10.0) / 10.0;
            double armor = player.getArmorValue();
            double damage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);

            graphics.drawString(this.font, "SALUD: §c" + health + " / " + maxHealth, statsX, statsY + 50, 0xFFFFFFFF, true);
            graphics.drawString(this.font, "ARMADURA: §9" + armor, statsX, statsY + 62, 0xFFFFFFFF, true);
            graphics.drawString(this.font, "DAÑO BASE: §6" + damage, statsX, statsY + 74, 0xFFFFFFFF, true);
        }

        // MEDIO-DERECHA: INVENTARIO REAL COMPLETO Y FUNCIONAL DEL JUGADOR
        int invX = w / 2 + 30;
        int invY = 70;
        graphics.drawString(this.font, "INVENTARIO DEL JUGADOR", invX, invY - 14, 0xFFFFD700, true);
        drawInventoryGrid(graphics, invX, invY, mouseX, mouseY);
    }

    private void renderFabricacionTab(GuiGraphics graphics, int w, int h, int mouseX, int mouseY) {
        // LADO IZQUIERDO: CRAFTEO 3x3 Y RECETA SELECCIONADA
        int craftGridX = 30;
        int craftGridY = 55;

        graphics.drawString(this.font, "ESTACIÓN DE FABRICACIÓN", craftGridX, craftGridY - 14, 0xFFFFD700, true);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int sx = craftGridX + col * 26;
                int sy = craftGridY + row * 26;
                boolean hov = mouseX >= sx && mouseX <= sx + 26 && mouseY >= sy && mouseY <= sy + 26;
                drawSlotFrame(graphics, sx, sy, hov, 26);
            }
        }

        graphics.drawString(this.font, "➔", craftGridX + 95, craftGridY + 30, 0xFFFFD700, true);

        boolean resHov = mouseX >= craftGridX + 140 && mouseX <= craftGridX + 166 && mouseY >= craftGridY + 26 && mouseY <= craftGridY + 52;
        drawSlotFrame(graphics, craftGridX + 140, craftGridY + 26, resHov, 26);

        // RENDER DE RECETA SELECCIONADA EN LA GRILLA 3X3 Y BOTÓN
        if (selectedRecipe != null) {
            ItemStack selRes = selectedRecipe.getResultItem(this.minecraft.level.registryAccess());
            graphics.drawString(this.font, "RECETA: §a" + selRes.getHoverName().getString(), craftGridX, craftGridY + 90, 0xFFFFFFFF, true);

            net.minecraft.core.NonNullList<net.minecraft.world.item.crafting.Ingredient> ingredients = selectedRecipe.getIngredients();
            for (int i = 0; i < Math.min(9, ingredients.size()); i++) {
                net.minecraft.world.item.crafting.Ingredient ing = ingredients.get(i);
                if (!ing.isEmpty()) {
                    ItemStack[] matchingStacks = ing.getItems();
                    if (matchingStacks.length > 0) {
                        int col = i % 3;
                        int row = i / 3;
                        int sx = craftGridX + col * 26 + 5;
                        int sy = craftGridY + row * 26 + 5;
                        ItemStack ingStack = matchingStacks[(int)((System.currentTimeMillis() / 1000) % matchingStacks.length)];
                        graphics.renderItem(ingStack, sx, sy);
                    }
                }
            }

            // Botón Fabricar con button.png
            int btnW = 90;
            int btnH = 20;
            int btnX = craftGridX;
            int btnY = craftGridY + 105;
            boolean btnHov = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;

            graphics.blit(BUTTON_TEX, btnX, btnY, 0, btnHov ? 10 : 0, btnW, btnH, btnW, btnH);
            graphics.drawString(this.font, "FABRICAR", btnX + 18, btnY + 5, 0xFFFFD700, true);
        } else {
            graphics.drawString(this.font, "Selecciona una receta del catálogo", craftGridX, craftGridY + 90, 0x88FFFFFF, false);
        }

        // ABAJO IZQUIERDA: INVENTARIO DEL JUGADOR
        int invX = 30;
        int invY = h - 95;
        graphics.drawString(this.font, "MATERIALES / INVENTARIO", invX, invY - 14, 0xFFFFD700, true);
        drawInventoryGrid(graphics, invX, invY, mouseX, mouseY);

        // LADO DERECHO: CATÁLOGO COMPLETO OCUPANDO TODA LA ALTURA DERECHA
        int catalogX = (int) (w * 0.45);
        int catalogWidth = w - catalogX - 20;

        graphics.drawString(this.font, "CATÁLOGO DE FABRICACIÓN (" + filteredRecipes.size() + ")", catalogX + 10, 36, 0xFFFFD700, true);

        // Categorías universales usando botones compactos
        int catY = 75;
        int catW = 58;
        int catH = 16;
        RecipeCategory[] categories = RecipeCategory.values();
        for (int i = 0; i < categories.length; i++) {
            RecipeCategory cat = categories[i];
            int cx = catalogX + 10 + (i % 5) * (catW + 4);
            int cy = catY + (i / 5) * (catH + 4);
            boolean isSel = (cat == currentCategory);
            boolean isHov = mouseX >= cx && mouseX <= cx + catW && mouseY >= cy && mouseY <= cy + catH;

            graphics.blit(BUTTON_TEX, cx, cy, 0, isSel ? 20 : (isHov ? 10 : 0), catW, catH, catW, catH);
            String catLabel = cat.name();
            if (catLabel.length() > 8) catLabel = catLabel.substring(0, 7) + ".";
            graphics.drawString(this.font, catLabel, cx + 4, cy + 4, isSel ? 0xFFFFD700 : 0xFFFFFFFF, false);
        }

        // Grilla de recetas del catálogo
        int gridY = catY + 40;
        int itemCols = Math.max(4, (catalogWidth - 20) / 32);
        int itemRows = Math.max(3, (h - gridY - 50) / 32);
        int pageSize = itemCols * itemRows;

        int startIdx = recipePageIndex * pageSize;
        for (int i = 0; i < pageSize && (startIdx + i) < filteredRecipes.size(); i++) {
            CraftingRecipe rec = filteredRecipes.get(startIdx + i);
            ItemStack res = rec.getResultItem(this.minecraft.level.registryAccess());

            int col = i % itemCols;
            int row = i / itemCols;
            int ix = catalogX + 10 + col * 32;
            int iy = gridY + row * 32;

            boolean hov = mouseX >= ix && mouseX <= ix + 28 && mouseY >= iy && mouseY <= iy + 28;
            drawSlotFrame(graphics, ix, iy, hov, 28);
            graphics.renderItem(res, ix + 6, iy + 6);

            if (favoriteRecipes.contains(rec.getId())) {
                graphics.drawString(this.font, "★", ix + 2, iy + 2, 0xFFFFD700, false);
            }

            if (hov) {
                graphics.renderTooltip(this.font, res, mouseX, mouseY);
            }
        }

        // Paginación con button.png
        int totalPages = Math.max(1, (int) Math.ceil((double) filteredRecipes.size() / (double) pageSize));
        int navButtonsY = h - 35;
        int pBtnW = 70;
        int pBtnH = 20;

        // Anterior
        boolean prevHov = mouseX >= catalogX + 10 && mouseX <= catalogX + 10 + pBtnW && mouseY >= navButtonsY && mouseY <= navButtonsY + pBtnH;
        graphics.blit(BUTTON_TEX, catalogX + 10, navButtonsY, 0, prevHov ? 10 : 0, pBtnW, pBtnH, pBtnW, pBtnH);
        graphics.drawString(this.font, "ANTERIOR", catalogX + 18, navButtonsY + 5, 0xFFFFFFFF, true);

        // Texto página
        graphics.drawString(this.font, (recipePageIndex + 1) + " / " + totalPages, catalogX + (catalogWidth / 2) - 15, navButtonsY + 5, 0xFFFFD700, true);

        // Siguiente
        int nextX = catalogX + catalogWidth - pBtnW - 10;
        boolean nextHov = mouseX >= nextX && mouseX <= nextX + pBtnW && mouseY >= navButtonsY && mouseY <= navButtonsY + pBtnH;
        graphics.blit(BUTTON_TEX, nextX, navButtonsY, 0, nextHov ? 10 : 0, pBtnW, pBtnH, pBtnW, pBtnH);
        graphics.drawString(this.font, "SIGUIENTE", nextX + 12, navButtonsY + 5, 0xFFFFFFFF, true);
    }

    private void renderMochilaTab(GuiGraphics graphics, int w, int h, int mouseX, int mouseY) {
        int tier = this.menu.getBackpackTier();
        int backpackX = (w - (9 * 26)) / 2;
        int backpackY = 100;
        int maxUnlocked = tier * 15;

        // ENCABEZADO Y PESO (CONSERVA EL DISEÑO QUE LE GUSTA AL USUARIO SIN LÍNEAS AZULES)
        int headerY = 50;
        graphics.drawString(this.font, "MOCHILA DEL AVENTURERO (TIER " + tier + ")", backpackX, headerY, 0xFFFFD700, true);

        int iconW = 18;
        int iconH = (int)(iconW / 0.986f);
        graphics.blit(WEIGHT_ICON, backpackX, headerY + 18, 0, 0, iconW, iconH, 496, 503);
        graphics.drawString(this.font, "CAPACIDAD DESBLOQUEADA: §a" + maxUnlocked + " / 45 SLOTS", backpackX + 24, headerY + 22, 0xFFFFFFFF, true);

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
        int invY = backpackY + 5 * 26 + 25;
        graphics.drawString(this.font, "INVENTARIO DEL JUGADOR", backpackX, invY - 14, 0xFFFFD700, true);
        drawInventoryGrid(graphics, backpackX, invY, mouseX, mouseY);
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
        int hotbarY = invY + 80;
        for (int col = 0; col < 9; col++) {
            int sx = invX + col * 26;
            boolean hov = mouseX >= sx && mouseX <= sx + 26 && mouseY >= hotbarY && mouseY <= hotbarY + 26;
            drawSlotFrame(graphics, sx, hotbarY, hov, 26);
        }
    }

    private void autoTransferRecipeIngredients(CraftingRecipe recipe) {
        if (this.minecraft == null || this.minecraft.player == null) return;
        net.minecraft.core.NonNullList<net.minecraft.world.item.crafting.Ingredient> ingredients = recipe.getIngredients();
        Inventory inv = this.minecraft.player.getInventory();

        for (int i = 0; i < Math.min(9, ingredients.size()); i++) {
            net.minecraft.world.item.crafting.Ingredient ing = ingredients.get(i);
            if (ing.isEmpty()) continue;

            for (int slot = 0; slot < inv.getContainerSize(); slot++) {
                ItemStack stack = inv.getItem(slot);
                if (!stack.isEmpty() && ing.test(stack)) {
                    break;
                }
            }
        }
    }

    private void drawSlotFrame(GuiGraphics graphics, int x, int y, boolean hovered, int size) {
        ResourceLocation tex = hovered ? SLOTS_HOVER_TEX : SLOTS_TEX;
        graphics.blit(tex, x, y, 0, 0, size, size, size, size);
    }
}
