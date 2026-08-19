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
        MATERIALES,
        BLOQUES,
        HERRAMIENTAS,
        ARMAS,
        ARMADURA,
        COMIDA,
        UTILIDAD,
        FAVORITOS,
        RECIENTES
    }

    private Tab currentTab = Tab.PERSONAJE;
    private RecipeCategory currentCategory = RecipeCategory.TODOS;

    // Search and Catalog fields
    private EditBox searchBox;
    private List<CraftingRecipe> filteredRecipes = new ArrayList<>();
    private List<String> availableMods = new ArrayList<>();
    private String currentModFilter = "TODOS";
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

        int catalogX = (int) (this.width * 0.42);
        int catalogWidth = this.width - catalogX - 15;

        this.searchBox = new EditBox(this.font, catalogX + 10, 32, catalogWidth - 20, 16, Component.literal("Buscar receta..."));
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

        // Build list of unique mods present
        availableMods.clear();
        availableMods.add("TODOS");
        availableMods.add("minecraft");
        for (CraftingRecipe r : allRecipes) {
            String modId = r.getId().getNamespace();
            if (!availableMods.contains(modId)) {
                availableMods.add(modId);
            }
        }

        filteredRecipes.clear();

        for (CraftingRecipe recipe : allRecipes) {
            ItemStack result = recipe.getResultItem(this.minecraft.level.registryAccess());
            if (result.isEmpty()) continue;

            String modId = recipe.getId().getNamespace();
            if (!currentModFilter.equalsIgnoreCase("TODOS")) {
                if (!modId.equalsIgnoreCase(currentModFilter)) {
                    continue;
                }
            }

            if (currentCategory == RecipeCategory.FAVORITOS) {
                if (!favoriteRecipes.contains(recipe.getId())) continue;
            } else if (currentCategory == RecipeCategory.RECIENTES) {
                if (!recentRecipes.contains(recipe)) continue;
            } else if (currentCategory != RecipeCategory.TODOS) {
                if (!matchesCategory(result, currentCategory)) continue;
            }

            if (matchesQuery(recipe, query)) {
                filteredRecipes.add(recipe);
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
        if (stack.isEmpty()) return false;
        net.minecraft.world.item.Item item = stack.getItem();
        String name = stack.getHoverName().getString().toLowerCase();
        String itemPath = item.toString().toLowerCase();

        switch (category) {
            case ARMAS:
                return item instanceof net.minecraft.world.item.SwordItem
                    || item instanceof net.minecraft.world.item.BowItem
                    || item instanceof net.minecraft.world.item.CrossbowItem
                    || item instanceof net.minecraft.world.item.TridentItem
                    || itemPath.contains("sword") || itemPath.contains("bow") || itemPath.contains("crossbow") || itemPath.contains("trident") || itemPath.contains("weapon") || name.contains("espada") || name.contains("arco") || name.contains("lanza") || name.contains("daga");
            case ARMADURA:
                return item instanceof net.minecraft.world.item.ArmorItem
                    || item instanceof net.minecraft.world.item.ShieldItem
                    || itemPath.contains("helmet") || itemPath.contains("chestplate") || itemPath.contains("leggings") || itemPath.contains("boots") || itemPath.contains("armor") || name.contains("casco") || name.contains("pechera") || name.contains("pantalones") || name.contains("botas");
            case HERRAMIENTAS:
                return item instanceof net.minecraft.world.item.DiggerItem
                    || item instanceof net.minecraft.world.item.ShearsItem
                    || item instanceof net.minecraft.world.item.FishingRodItem
                    || itemPath.contains("pickaxe") || itemPath.contains("axe") || itemPath.contains("shovel") || itemPath.contains("hoe") || itemPath.contains("shears");
            case COMIDA:
                return item.isEdible() || itemPath.contains("apple") || itemPath.contains("bread") || itemPath.contains("stew") || name.contains("manzana") || name.contains("pan") || name.contains("carne") || name.contains("sopa");
            case BLOQUES:
                return item instanceof net.minecraft.world.item.BlockItem || itemPath.contains("block") || itemPath.contains("planks") || itemPath.contains("stone") || itemPath.contains("brick");
            case MATERIALES:
                return itemPath.contains("ingot") || itemPath.contains("gem") || itemPath.contains("stick") || itemPath.contains("nugget") || itemPath.contains("leather") || itemPath.contains("diamond") || itemPath.contains("iron") || itemPath.contains("gold");
            case UTILIDAD:
                return itemPath.contains("bucket") || itemPath.contains("torch") || itemPath.contains("compass") || itemPath.contains("clock") || itemPath.contains("map");
            default:
                return true;
        }
    }

    private void setTab(Tab tab) {
        this.currentTab = tab;
        updateSlotPositions();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_H && (this.searchBox == null || !this.searchBox.isFocused())) {
            this.minecraft.setScreen(new com.mundodetronos2.gui.HudEditorScreen());
            return true;
        }
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
                int catalogX = (int) (w * 0.42);
                int catalogWidth = w - catalogX - 15;

                // Mod Filter Tabs
                int modY = 52;
                int modW = 58;
                int modH = 14;
                for (int i = 0; i < availableMods.size() && i < 10; i++) {
                    int mx = catalogX + 10 + (i % 5) * (modW + 4);
                    int my = modY + (i / 5) * (modH + 4);
                    if (mouseX >= mx && mouseX <= mx + modW && mouseY >= my && mouseY <= my + modH) {
                        this.currentModFilter = availableMods.get(i);
                        updateRecipeList();
                        return true;
                    }
                }

                // Category Filter Buttons
                int catY = modY + (availableMods.size() > 5 ? 32 : 18);
                int catW = 58;
                int catH = 14;
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
                int gridY = catY + 34;
                int itemCols = Math.max(4, (catalogWidth - 20) / 30);
                int itemRows = Math.max(3, (h - gridY - 45) / 30);
                int pageSize = itemCols * itemRows;

                int startIdx = recipePageIndex * pageSize;
                for (int i = 0; i < pageSize && (startIdx + i) < filteredRecipes.size(); i++) {
                    int col = i % itemCols;
                    int row = i / itemCols;
                    int ix = catalogX + 10 + col * 30;
                    int iy = gridY + row * 30;

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
                int navButtonsY = h - 30;
                if (mouseX >= catalogX + 10 && mouseX <= catalogX + 80 && mouseY >= navButtonsY && mouseY <= navButtonsY + 18) {
                    if (recipePageIndex > 0) recipePageIndex--;
                    return true;
                }
                int totalPages = Math.max(1, (int) Math.ceil((double) filteredRecipes.size() / (double) pageSize));
                if (mouseX >= catalogX + catalogWidth - 80 && mouseX <= catalogX + catalogWidth - 10 && mouseY >= navButtonsY && mouseY <= navButtonsY + 18) {
                    if (recipePageIndex < totalPages - 1) recipePageIndex++;
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
        com.mundodetronos2.client.HudLayoutManager.ComponentConfig invConfig = com.mundodetronos2.client.HudLayoutManager.getConfig(com.mundodetronos2.client.HudLayoutManager.ComponentId.RPG_INVENTORY);
        int customInvX = com.mundodetronos2.client.HudLayoutManager.getRenderX(invConfig, w, h);
        int customInvY = com.mundodetronos2.client.HudLayoutManager.getRenderY(invConfig, w, h);

        int invX = (w - (9 * 26)) / 2;
        int invY = h - 95;
        if (isPersonaje) {
            invX = customInvX;
            invY = customInvY;
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
            int sy = armorY + i * 32;
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
        int offhandY = armorY + 4 * 32 + 10;
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

        // CENTRO-IZQUIERDA: PERSONAJE 3D LIGERAMENTE MÁS CHICO Y CENTRADO
        int entityX = (w / 2) - 60;
        int entityY = h - 80;
        if (player != null) {
            int modelScale = Math.min(150, h / 4);
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, entityX, entityY, modelScale, 0.0f, 0.0f, player);
        }

        // DERECHA: ATRIBUTOS Y STATS DEL PERSONAJE / CARNET DE JUGADOR
        int statsX = w - 244;
        int statsY = 42;
        graphics.drawString(this.font, "CARNET DE JUGADOR", statsX, statsY, 0xFFFFD700, true);

        int level = ClientPacketHandler.hudPlayerLevel;
        int currentXp = ClientPacketHandler.hudCurrentXp;
        int neededXp = ClientPacketHandler.hudNeededXp;

        graphics.drawString(this.font, "NOMBRE: §f" + name, statsX, statsY + 12, 0xFFFFFFFF, true);
        graphics.drawString(this.font, "ROL: §e" + roleStr + "  §7|  §fNIVEL: §a" + level, statsX, statsY + 22, 0xFFFFFFFF, true);

        if (player != null) {
            double health = Math.round(player.getHealth() * 10.0) / 10.0;
            double maxHealth = Math.round(player.getMaxHealth() * 10.0) / 10.0;
            double armor = player.getArmorValue();
            double damage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            double speed = Math.round(player.getAttributeValue(Attributes.MOVEMENT_SPEED) * 100.0) / 10.0;

            graphics.drawString(this.font, "SALUD: §c" + health + "/" + maxHealth + "  §7|  §fARM: §9" + armor, statsX, statsY + 32, 0xFFFFFFFF, true);
            graphics.drawString(this.font, "DAÑO: §6" + damage + "  §7|  §fVEL: §b" + speed, statsX, statsY + 42, 0xFFFFFFFF, true);
            graphics.drawString(this.font, "XP: §a" + currentXp + " / " + neededXp, statsX, statsY + 52, 0xFFFFFFFF, true);
        }

        // MOSTRAR ÍCONO DE CARTA / CARNET DE ROL
        ItemStack carnetItem = new ItemStack(com.mundodetronos2.init.ItemInit.ROLE_CARD.get());
        graphics.renderItem(carnetItem, statsX + 180, statsY);

        // LADO DERECHO (ENMEDIO): INVENTARIO REAL COMPLETO Y FUNCIONAL DEL JUGADOR
        com.mundodetronos2.client.HudLayoutManager.ComponentConfig invConfig = com.mundodetronos2.client.HudLayoutManager.getConfig(com.mundodetronos2.client.HudLayoutManager.ComponentId.RPG_INVENTORY);
        int invX = com.mundodetronos2.client.HudLayoutManager.getRenderX(invConfig, w, h);
        int invY = com.mundodetronos2.client.HudLayoutManager.getRenderY(invConfig, w, h);

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

        // RENDER DE RECETA SELECCIONADA (FANTASMAS DE INGREDIENTES Y RESULTADO)
        if (selectedRecipe != null) {
            ItemStack selRes = selectedRecipe.getResultItem(this.minecraft.level.registryAccess());
            graphics.drawString(this.font, "RECETA: §a" + selRes.getHoverName().getString(), craftGridX, craftGridY + 90, 0xFFFFFFFF, true);

            // Renderizar el resultado en el slot de salida fantasma
            graphics.renderItem(selRes, craftGridX + 145, craftGridY + 31);
            graphics.renderItemDecorations(this.font, selRes, craftGridX + 145, craftGridY + 31);

            // Renderizar los ingredientes en la grilla 3x3
            net.minecraft.core.NonNullList<net.minecraft.world.item.crafting.Ingredient> ingredients = selectedRecipe.getIngredients();
            if (selectedRecipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped) {
                int sWidth = shaped.getRecipeWidth();
                int sHeight = shaped.getRecipeHeight();
                for (int r = 0; r < sHeight; r++) {
                    for (int c = 0; c < sWidth; c++) {
                        int index = r * sWidth + c;
                        if (index < ingredients.size()) {
                            net.minecraft.world.item.crafting.Ingredient ing = ingredients.get(index);
                            ItemStack[] matching = ing.getItems();
                            if (matching.length > 0) {
                                int itemIdx = (int) ((System.currentTimeMillis() / 1000) % matching.length);
                                ItemStack displayStack = matching[itemIdx];
                                int sx = craftGridX + c * 26 + 5;
                                int sy = craftGridY + r * 26 + 5;
                                graphics.renderItem(displayStack, sx, sy);
                                graphics.renderItemDecorations(this.font, displayStack, sx, sy);
                            }
                        }
                    }
                }
            } else { // Shapeless / Generic
                for (int i = 0; i < ingredients.size() && i < 9; i++) {
                    net.minecraft.world.item.crafting.Ingredient ing = ingredients.get(i);
                    ItemStack[] matching = ing.getItems();
                    if (matching.length > 0) {
                        int itemIdx = (int) ((System.currentTimeMillis() / 1000) % matching.length);
                        ItemStack displayStack = matching[itemIdx];
                        int col = i % 3;
                        int row = i / 3;
                        int sx = craftGridX + col * 26 + 5;
                        int sy = craftGridY + row * 26 + 5;
                        graphics.renderItem(displayStack, sx, sy);
                        graphics.renderItemDecorations(this.font, displayStack, sx, sy);
                    }
                }
            }
        } else {
            graphics.drawString(this.font, "Selecciona un ítem del catálogo", craftGridX, craftGridY + 90, 0x88FFFFFF, false);
        }

        // ABAJO IZQUIERDA: INVENTARIO DEL JUGADOR
        int invX = 30;
        int invY = h - 95;
        graphics.drawString(this.font, "MATERIALES / INVENTARIO", invX, invY - 14, 0xFFFFD700, true);
        drawInventoryGrid(graphics, invX, invY, mouseX, mouseY);

        // LADO DERECHO: CATÁLOGO DE PESTAÑAS POR MODS Y CATEGORÍAS
        int catalogX = (int) (w * 0.42);
        int catalogWidth = w - catalogX - 15;

        graphics.drawString(this.font, "CATÁLOGO (" + filteredRecipes.size() + ")", catalogX + 10, 20, 0xFFFFD700, true);

        // Pestañas por Mods
        int modY = 52;
        int modW = 58;
        int modH = 14;
        for (int i = 0; i < availableMods.size() && i < 10; i++) {
            String modId = availableMods.get(i);
            int mx = catalogX + 10 + (i % 5) * (modW + 4);
            int my = modY + (i / 5) * (modH + 4);
            boolean isSel = modId.equalsIgnoreCase(currentModFilter);
            boolean isHov = mouseX >= mx && mouseX <= mx + modW && mouseY >= my && mouseY <= my + modH;

            graphics.blit(BUTTON_TEX, mx, my, 0, isSel ? 20 : (isHov ? 10 : 0), modW, modH, modW, modH);
            String modLabel = modId.equalsIgnoreCase("minecraft") ? "VANILLA" : (modId.equalsIgnoreCase("mundodetronos2") ? "TRONOS 2" : modId.toUpperCase());
            if (modLabel.length() > 8) modLabel = modLabel.substring(0, 7) + ".";
            graphics.drawString(this.font, modLabel, mx + 3, my + 3, isSel ? 0xFFFFD700 : 0xFFFFFFFF, false);
        }

        // Categorías dentro del mod
        int catY = modY + (availableMods.size() > 5 ? 32 : 18);
        int catW = 58;
        int catH = 14;
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
            graphics.drawString(this.font, catLabel, cx + 3, cy + 3, isSel ? 0xFFFFD700 : 0xFFFFFFFF, false);
        }

        // Grilla de recetas del catálogo
        int gridY = catY + 34;
        int itemCols = Math.max(4, (catalogWidth - 20) / 30);
        int itemRows = Math.max(3, (h - gridY - 45) / 30);
        int pageSize = itemCols * itemRows;

        int startIdx = recipePageIndex * pageSize;
        for (int i = 0; i < pageSize && (startIdx + i) < filteredRecipes.size(); i++) {
            CraftingRecipe rec = filteredRecipes.get(startIdx + i);
            ItemStack res = rec.getResultItem(this.minecraft.level.registryAccess());

            int col = i % itemCols;
            int row = i / itemCols;
            int ix = catalogX + 10 + col * 30;
            int iy = gridY + row * 30;

            boolean hov = mouseX >= ix && mouseX <= ix + 28 && mouseY >= iy && mouseY <= iy + 28;
            drawSlotFrame(graphics, ix, iy, hov, 28);
            graphics.renderItem(res, ix + 6, iy + 6);

            if (hov) {
                graphics.renderTooltip(this.font, res, mouseX, mouseY);
            }
        }

        // Paginación con button.png
        int totalPages = Math.max(1, (int) Math.ceil((double) filteredRecipes.size() / (double) pageSize));
        int navButtonsY = h - 30;
        int pBtnW = 70;
        int pBtnH = 18;

        // Anterior
        boolean prevHov = mouseX >= catalogX + 10 && mouseX <= catalogX + 10 + pBtnW && mouseY >= navButtonsY && mouseY <= navButtonsY + pBtnH;
        graphics.blit(BUTTON_TEX, catalogX + 10, navButtonsY, 0, prevHov ? 10 : 0, pBtnW, pBtnH, pBtnW, pBtnH);
        graphics.drawString(this.font, "ANTERIOR", catalogX + 15, navButtonsY + 4, 0xFFFFFFFF, true);

        // Texto página
        graphics.drawString(this.font, (recipePageIndex + 1) + " / " + totalPages, catalogX + (catalogWidth / 2) - 15, navButtonsY + 4, 0xFFFFD700, true);

        // Siguiente
        int nextX = catalogX + catalogWidth - pBtnW - 10;
        boolean nextHov = mouseX >= nextX && mouseX <= nextX + pBtnW && mouseY >= navButtonsY && mouseY <= navButtonsY + pBtnH;
        graphics.blit(BUTTON_TEX, nextX, navButtonsY, 0, nextHov ? 10 : 0, pBtnW, pBtnH, pBtnW, pBtnH);
        graphics.drawString(this.font, "SIGUIENTE", nextX + 10, navButtonsY + 4, 0xFFFFFFFF, true);
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

    private void drawSlotFrame(GuiGraphics graphics, int x, int y, boolean hovered, int size) {
        ResourceLocation tex = hovered ? SLOTS_HOVER_TEX : SLOTS_TEX;
        graphics.blit(tex, x, y, 0, 0, size, size, size, size);
    }
}
