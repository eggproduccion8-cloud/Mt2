package com.mundodetronos2.gui;

import com.mundodetronos2.client.ClientEvents;
import com.mundodetronos2.client.ClientPacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class RoleCardScreen extends Screen {

    private final UUID playerId;
    private final String playerName;
    private final String roleName;
    private final int rpgLevel;
    private final String realmName;
    private final String realmRole;
    private final int currentXp;
    private final int neededXp;

    private Tab currentTab = Tab.IDENTIDAD;

    public enum Tab {
        IDENTIDAD("Identidad"),
        PROGRESION("Progresión"),
        GUIA("Guía"),
        EQUIPO("Equipo"),
        LOGROS("Logros");

        private final String label;
        Tab(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public RoleCardScreen(UUID playerId, String playerName, String roleName, int rpgLevel, String realmName, String realmRole, int currentXp, int neededXp) {
        super(Component.literal("CARNET ASPIRANTE"));
        this.playerId = playerId;
        this.playerName = playerName;
        this.roleName = (roleName != null && !roleName.isEmpty() && !roleName.equalsIgnoreCase("none")) ? roleName : "viajero";
        this.rpgLevel = Math.max(1, rpgLevel);
        this.realmName = realmName != null ? realmName : "Ninguno";
        this.realmRole = realmRole != null ? realmRole : "N/A";
        this.currentXp = Math.max(0, currentXp);
        this.neededXp = neededXp > 0 ? neededXp : 150;
    }

    public RoleCardScreen(UUID playerId, String playerName, String roleName, int rpgLevel, String realmName, String realmRole) {
        this(playerId, playerName, roleName, rpgLevel, realmName, realmRole, ClientPacketHandler.hudCurrentXp, ClientPacketHandler.hudNeededXp);
    }

    @Override
    protected void init() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int cardWidth = 360;
        int cardHeight = 230;
        int x = centerX - cardWidth / 2;
        int y = centerY - cardHeight / 2 - 10;

        // Botones de Secciones/Pestañas inferiores
        Tab[] tabs = Tab.values();
        int btnW = 60;
        int btnH = 16;
        int btnY = y + cardHeight - 22;

        for (int i = 0; i < tabs.length; i++) {
            Tab tab = tabs[i];
            int btnX = x + 15 + (i * 68);
            this.addRenderableWidget(new TransparentButton(btnX, btnY, btnW, btnH, Component.literal(tab.getLabel()), btn -> {
                this.currentTab = tab;
                this.init();
            }));
        }

        // Botón Regresar/Cerrar
        this.addRenderableWidget(new TransparentButton(centerX - 40, y + cardHeight + 4, 80, 18, Component.literal("Cerrar"), btn -> {
            this.onClose();
        }));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int cardWidth = 360;
        int cardHeight = 230;
        int x = centerX - cardWidth / 2;
        int y = centerY - cardHeight / 2 - 10;

        // --- DISEÑO DE PERGAMINO MEDIEVAL ---
        graphics.fill(x - 4, y - 4, x + cardWidth + 4, y + cardHeight + 4, 0xFF362819);
        graphics.fill(x - 2, y - 2, x + cardWidth + 2, y + cardHeight + 2, 0xFF4A3B2C);
        graphics.fill(x, y, x + cardWidth, y + cardHeight, 0xEEF3E5C8);
        graphics.fill(x + 3, y + 3, x + cardWidth - 3, y + cardHeight - 3, 0xEEEBDAB3);

        int innerBorderColor = 0xFF8F7051;
        graphics.fill(x + 5, y + 5, x + cardWidth - 5, y + 6, innerBorderColor);
        graphics.fill(x + 5, y + cardHeight - 6, x + cardWidth - 5, y + cardHeight - 5, innerBorderColor);
        graphics.fill(x + 5, y + 5, x + 6, y + cardHeight - 5, innerBorderColor);
        graphics.fill(x + cardWidth - 6, y + 5, x + cardWidth - 5, y + cardHeight - 5, innerBorderColor);

        // Encabezado Principal
        ClientEvents.drawFlatCenteredString(graphics, this.font, "§4✧ CARNET ASPIRANTE ✧", centerX, y + 8, 0);

        String translatedRole = roleName.equalsIgnoreCase("viajero") ? "Viajero" : MainGuiScreen.getTranslatedRole(roleName);
        ClientEvents.drawFlatCenteredString(graphics, this.font, "§5TÍTULO: §0Aspirante  §5|  ROL: §1" + translatedRole.toUpperCase(), centerX, y + 19, 0);

        // Línea divisoria
        graphics.fill(x + 12, y + 29, x + cardWidth - 12, y + 30, 0x884A3B2C);

        // Renderizado contextual según la pestaña actual
        switch (currentTab) {
            case IDENTIDAD -> renderIdentidad(graphics, x, y);
            case PROGRESION -> renderProgresion(graphics, x, y);
            case GUIA -> renderGuia(graphics, x, y);
            case EQUIPO -> renderEquipo(graphics, x, y);
            case LOGROS -> renderLogros(graphics, x, y);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void renderIdentidad(GuiGraphics graphics, int x, int y) {
        ResourceLocation skinTexture = DefaultPlayerSkin.getDefaultSkin(playerId);
        try {
            if (playerId != null && Minecraft.getInstance().getConnection() != null) {
                net.minecraft.client.multiplayer.PlayerInfo info = Minecraft.getInstance().getConnection().getPlayerInfo(playerId);
                if (info != null) {
                    skinTexture = info.getSkinLocation();
                }
            }
        } catch (Exception ignored) {}

        int faceX = x + 15;
        int faceY = y + 36;
        int faceSize = 48;

        graphics.fill(faceX - 2, faceY - 2, faceX + faceSize + 2, faceY + faceSize + 2, 0xFF4A3B2C);
        graphics.fill(faceX - 1, faceY - 1, faceX + faceSize + 1, faceY + faceSize + 1, 0xFFD4AF37);
        PlayerFaceRenderer.draw(graphics, skinTexture, faceX, faceY, faceSize);

        int textX = faceX + faceSize + 12;
        graphics.drawString(this.font, "§0NOMBRE: §l" + playerName.toUpperCase(), textX, faceY + 2, 0, false);
        graphics.drawString(this.font, "§5TÍTULO: §0Aspirante de " + MainGuiScreen.getTranslatedRole(roleName), textX, faceY + 14, 0, false);
        graphics.drawString(this.font, "§1NIVEL: §2" + ClientPacketHandler.hudPlayerLevel, textX, faceY + 26, 0, false);

        int rY = faceY + 56;
        graphics.fill(x + 15, rY, x + 345, rY + 1, 0x884A3B2C);
        graphics.drawString(this.font, "§4REINO: §0" + realmName.toUpperCase(), x + 15, rY + 6, 0, false);
        graphics.drawString(this.font, "§5RANGO: §0" + realmRole, x + 180, rY + 6, 0, false);
    }

    private void renderProgresion(GuiGraphics graphics, int x, int y) {
        int py = y + 36;
        graphics.drawString(this.font, "§4✧ PROGRESIÓN RPG DEL ASPIRANTE ✧", x + 15, py, 0, false);

        int missingXp = Math.max(0, neededXp - currentXp);
        float pct = Math.min(1.0f, (float) currentXp / (float) neededXp);
        int pctInt = (int) (pct * 100);

        graphics.drawString(this.font, "§1NIVEL: §2" + ClientPacketHandler.hudPlayerLevel + " §8(Siguiente: " + (ClientPacketHandler.hudPlayerLevel + 1) + ")", x + 15, py + 16, 0, false);
        graphics.drawString(this.font, "§1Puntos de Experiencia: §0" + currentXp + " / " + neededXp + " §8(Falta: " + missingXp + " XP)", x + 15, py + 28, 0, false);

        int barX = x + 15;
        int barY = py + 42;
        int barW = 330;
        int barH = 10;
        graphics.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF4A3B2C);
        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFFE5D5B0);
        int fillW = (int) (barW * pct);
        if (fillW > 0) {
            graphics.fill(barX, barY, barX + fillW, barY + barH, 0xFF55FF55);
        }
        graphics.drawString(this.font, "§0Progreso: " + pctInt + "%", barX + barW - 75, barY + 1, 0, false);

        graphics.drawString(this.font, "§1Skill Points Disponibles: §6" + (rpgLevel - 1), x + 15, py + 60, 0, false);
    }

    private void renderGuia(GuiGraphics graphics, int x, int y) {
        int gy = y + 36;
        graphics.drawString(this.font, "§4✧ GUÍA DEL ASPIRANTE ✧", x + 15, gy, 0, false);

        graphics.drawString(this.font, "§2[✓] Reinos: §0Forma tu equipo desde la interfaz Mi Reino.", x + 15, gy + 16, 0, false);
        graphics.drawString(this.font, "§2[✓] Tronos: §0Representan el núcleo de protección de tu territorio.", x + 15, gy + 30, 0, false);
        graphics.drawString(this.font, "§1[ ] Roles y Habilidades: §0Selecciona tu Rol en el Altar de la Diosa.", x + 15, gy + 44, 0, false);
        graphics.drawString(this.font, "§1[ ] Asaltos: §0Usa Cargas de Asalto en muros durante el Evento.", x + 15, gy + 58, 0, false);
    }

    private void renderEquipo(GuiGraphics graphics, int x, int y) {
        int ey = y + 36;
        graphics.drawString(this.font, "§4✧ EQUIPAMIENTO ACTUAL ✧", x + 15, ey, 0, false);

        if (Minecraft.getInstance().player != null) {
            int slotY = ey + 20;
            net.minecraft.world.item.ItemStack helmet = Minecraft.getInstance().player.getInventory().getArmor(3);
            net.minecraft.world.item.ItemStack chest = Minecraft.getInstance().player.getInventory().getArmor(2);
            net.minecraft.world.item.ItemStack legs = Minecraft.getInstance().player.getInventory().getArmor(1);
            net.minecraft.world.item.ItemStack boots = Minecraft.getInstance().player.getInventory().getArmor(0);
            net.minecraft.world.item.ItemStack mainHand = Minecraft.getInstance().player.getMainHandItem();

            graphics.drawString(this.font, "§1Casco: §0" + (helmet.isEmpty() ? "Vacío" : helmet.getHoverName().getString()), x + 15, slotY, 0, false);
            graphics.drawString(this.font, "§1Peto: §0" + (chest.isEmpty() ? "Vacío" : chest.getHoverName().getString()), x + 15, slotY + 14, 0, false);
            graphics.drawString(this.font, "§1Pantalones: §0" + (legs.isEmpty() ? "Vacío" : legs.getHoverName().getString()), x + 15, slotY + 28, 0, false);
            graphics.drawString(this.font, "§1Botas: §0" + (boots.isEmpty() ? "Vacío" : boots.getHoverName().getString()), x + 15, slotY + 42, 0, false);
            graphics.drawString(this.font, "§1Mano Principal: §0" + (mainHand.isEmpty() ? "Vacío" : mainHand.getHoverName().getString()), x + 15, slotY + 56, 0, false);
        }
    }

    private void renderLogros(GuiGraphics graphics, int x, int y) {
        int ly = y + 36;
        graphics.drawString(this.font, "§4✧ ESTADÍSTICAS Y LOGROS ✧", x + 15, ly, 0, false);

        graphics.drawString(this.font, "§8• Nivel del Personaje: §2" + ClientPacketHandler.hudPlayerLevel, x + 15, ly + 16, 0, false);
        graphics.drawString(this.font, "§8• Pertenece a un Reino: §0" + (!realmName.equalsIgnoreCase("Ninguno") ? "Sí (" + realmName + ")" : "No"), x + 15, ly + 30, 0, false);
        graphics.drawString(this.font, "§8• Operador de Servidor: §0" + (Minecraft.getInstance().player != null && Minecraft.getInstance().player.hasPermissions(2) ? "Sí" : "No"), x + 15, ly + 44, 0, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
