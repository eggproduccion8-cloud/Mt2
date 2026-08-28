package com.mundodetronos2.gui;

import com.mundodetronos2.network.NetworkManager;
import com.mundodetronos2.skills.SkillNode;
import com.mundodetronos2.skills.SkillTree;
import com.mundodetronos2.skills.SkillTreeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class RoleSkillTreeScreen extends Screen {

    private final String roleId;
    private final int playerLevel;
    private int skillPoints = 0;
    private List<String> unlockedSkills = new ArrayList<>();

    public RoleSkillTreeScreen(String roleId, int playerLevel) {
        super(Component.literal("Árbol de Habilidades"));
        this.roleId = roleId;
        this.playerLevel = playerLevel;
    }

    public void updateData(int skillPoints, List<String> unlockedSkills) {
        this.skillPoints = skillPoints;
        this.unlockedSkills = unlockedSkills;
    }

    @Override
    protected void init() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Botón Cerrar (Estilo elegante pergamino) posicionado en la parte inferior para evitar amontonamiento
        this.addRenderableWidget(new TransparentButton(
            centerX - 40, this.height - 35, 80, 16,
            Component.literal("Cerrar"), btn -> this.onClose()
        ));

        SkillTree tree = SkillTreeManager.getTree(roleId);
        if (tree == null) return;

        // Spacing de cuadrícula adaptado para pantalla completa
        int gridStartX = centerX - 90;
        int gridStartY = centerY - 65;

        for (SkillNode node : tree.getNodes().values()) {
            int bx = gridStartX + node.getX() * 90;
            int by = gridStartY + node.getY() * 36;

            boolean isUnlocked = unlockedSkills.contains(node.getId().toLowerCase());
            boolean isAvailable = !isUnlocked && playerLevel >= node.getRequiredLevel() && skillPoints >= node.getCost();

            if (isAvailable) {
                for (String req : node.getPrerequisites()) {
                    if (!unlockedSkills.contains(req.toLowerCase())) {
                        isAvailable = false;
                        break;
                    }
                }
            }

            final boolean canBuy = isAvailable;
            final String skillId = node.getId();
            String symbol = getSkillIconSymbol(node.getId(), node.getName());

            // Crear botón de nodo interactivo personalizado
            this.addRenderableWidget(new SkillNodeButton(
                bx, by, 24, 24,
                Component.literal(""), node, isUnlocked, isAvailable, symbol,
                btn -> {
                    if (canBuy) {
                        NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SUnlockSkillNodePacket(roleId, skillId));
                        this.onClose();
                    }
                }
            ));
        }
    }

    private static String getSkillIconSymbol(String id, String name) {
        String n = name.toLowerCase();
        if (n.contains("furia") || n.contains("frenesí") || n.contains("sangre")) return "🩸";
        if (n.contains("escudo") || n.contains("coraza") || n.contains("defensa")) return "🛡";
        if (n.contains("ataque") || n.contains("fuerza") || n.contains("golpe")) return "⚔";
        if (n.contains("flecha") || n.contains("lluvia") || n.contains("tiro")) return "🏹";
        if (n.contains("mago") || n.contains("arcana") || n.contains("rayos") || n.contains("fuego") || n.contains("ígneo") || n.contains("centella")) return "🧙";
        if (n.contains("cantar") || n.contains("sanación") || n.contains("milagro") || n.contains("alivio") || n.contains("bendición") || n.contains("plegaria")) return "✨";
        if (n.contains("vuelo") || n.contains("ala")) return "🐉";
        return "✦";
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Renderizar un oscurecimiento sutil del fondo del mundo
        graphics.fill(0, 0, this.width, this.height, 0x88000000);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Ocupa prácticamente toda la pantalla (Fullscreen layout)
        int menuWidth = this.width - 40;
        int menuHeight = this.height - 40;
        int x = centerX - menuWidth / 2;
        int y = centerY - menuHeight / 2 - 10;

        // --- DISEÑO DE PARCHMENT FULLSCREEN RPG ---
        graphics.fill(x - 4, y - 4, x + menuWidth + 4, y + menuHeight + 4, 0xDD362819); // Madera
        graphics.fill(x - 2, y - 2, x + menuWidth + 2, y + menuHeight + 2, 0xFFD4AF37); // Dorado
        graphics.fill(x, y, x + menuWidth, y + menuHeight, 0xEEF3E5C8); // Pergamino rústico traslúcido
        graphics.fill(x + 3, y + 3, x + menuWidth - 3, y + menuHeight - 3, 0xEEEBDAB3);

        int innerBorderColor = 0xFF8F7051;
        graphics.fill(x + 5, y + 5, x + menuWidth - 5, y + 6, innerBorderColor);
        graphics.fill(x + 5, y + menuHeight - 6, x + menuWidth - 5, y + menuHeight - 5, innerBorderColor);
        graphics.fill(x + 5, y + 5, x + 6, y + menuHeight - 5, innerBorderColor);
        graphics.fill(x + menuWidth - 6, y + 5, x + menuWidth - 5, y + menuHeight - 5, innerBorderColor);

        // Título del pergamino (Sin sombras)
        String titleText = "ÁRBOL DE HABILIDADES: " + roleId.toUpperCase();
        com.mundodetronos2.client.ClientEvents.drawFlatCenteredString(graphics, this.font, "§4§l" + titleText, centerX, y + 12, 0);

        String infoText = "§0Nivel Requerido: §1" + playerLevel + " §0| Puntos de Habilidad: §2" + skillPoints;
        com.mundodetronos2.client.ClientEvents.drawFlatCenteredString(graphics, this.font, infoText, centerX, y + 24, 0);

        // Dibujar las líneas que conectan los nodos
        SkillTree tree = SkillTreeManager.getTree(roleId);
        if (tree != null) {
            int gridStartX = centerX - 90;
            int gridStartY = centerY - 65;

            for (SkillNode node : tree.getNodes().values()) {
                int nx = gridStartX + node.getX() * 90 + 12; // centrado en el nodo de 24px
                int ny = gridStartY + node.getY() * 36 + 12;

                for (String prereqId : node.getPrerequisites()) {
                    SkillNode parent = tree.getNode(prereqId);
                    if (parent != null) {
                        int px = gridStartX + parent.getX() * 90 + 12;
                        int py = gridStartY + parent.getY() * 36 + 12;

                        // Dibujar línea fina rústica entre nodos conectores
                        graphics.fill(px, py, nx, ny + 1, 0xFF8F7051);
                    }
                }
            }
        }

        super.render(graphics, mouseX, mouseY, partialTicks);

        // Dibujar Tooltips detallados sobre los nodos al pasar el cursor
        if (tree != null) {
            int gridStartX = centerX - 90;
            int gridStartY = centerY - 65;

            for (SkillNode node : tree.getNodes().values()) {
                int bx = gridStartX + node.getX() * 90;
                int by = gridStartY + node.getY() * 36;

                if (mouseX >= bx && mouseY >= by && mouseX < bx + 24 && mouseY < by + 24) {
                    boolean isUnlocked = unlockedSkills.contains(node.getId().toLowerCase());
                    boolean isAvailable = !isUnlocked && playerLevel >= node.getRequiredLevel() && skillPoints >= node.getCost();

                    if (isAvailable) {
                        for (String req : node.getPrerequisites()) {
                            if (!unlockedSkills.contains(req.toLowerCase())) {
                                isAvailable = false;
                                break;
                            }
                        }
                    }

                    List<Component> tooltipText = new ArrayList<>();
                    tooltipText.add(Component.literal("§6§l" + node.getName()));
                    tooltipText.add(Component.literal("§7" + node.getDescription()));
                    tooltipText.add(Component.literal("§bNivel Requerido: §e" + node.getRequiredLevel()));
                    tooltipText.add(Component.literal("§bCosto: §a" + node.getCost() + " Skill Point" + (node.getCost() > 1 ? "s" : "")));

                    String statusStr = "§cBloqueado";
                    if (isUnlocked) statusStr = "§a✓ Desbloqueado";
                    else if (isAvailable) statusStr = "§6★ Disponible";
                    tooltipText.add(Component.literal("§7Estado: " + statusStr));

                    graphics.renderComponentTooltip(this.font, tooltipText, mouseX, mouseY);
                }
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // --- BOTÓN DE NODO PERSONALIZADO TRANSPARENTE ---
    public static class SkillNodeButton extends Button {
        private final SkillNode node;
        private final boolean isUnlocked;
        private final boolean isAvailable;
        private final String iconSymbol;

        public SkillNodeButton(int x, int y, int width, int height, Component message, SkillNode node, boolean isUnlocked, boolean isAvailable, String iconSymbol, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.node = node;
            this.isUnlocked = isUnlocked;
            this.isAvailable = isAvailable;
            this.iconSymbol = iconSymbol != null ? iconSymbol : "✦";
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            if (!this.visible) return;

            boolean hovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;

            int borderColor;
            int bgColor;

            if (isUnlocked) {
                borderColor = 0xFF55FF55; // Verde brillante desbloqueado
                bgColor = hovered ? 0x4055FF55 : 0x2055FF55;
            } else if (isAvailable) {
                borderColor = 0xFFFFAA00; // Dorado disponible
                bgColor = hovered ? 0x40FFAA00 : 0x20FFAA00;
            } else {
                borderColor = 0xFF8F7051; // Café rústico bloqueado
                bgColor = hovered ? 0x208F7051 : 0x0A8F7051;
            }

            // Dibujar bordes finos del nodo
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColor);
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, borderColor);
            graphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, borderColor);
            graphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.height, borderColor);
            graphics.fill(this.getX() + this.width - 1, this.getY(), this.getX() + this.width, this.getY() + this.height, borderColor);

            // Icono central (Sin sombras)
            Minecraft mc = Minecraft.getInstance();
            com.mundodetronos2.client.ClientEvents.drawFlatCenteredString(graphics, mc.font, iconSymbol, this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, 0xFFFFFFFF);

            // Nombre debajo del nodo
            String nameStr = node.getName();
            if (nameStr.length() > 14) nameStr = nameStr.substring(0, 12) + "...";
            com.mundodetronos2.client.ClientEvents.drawFlatCenteredString(graphics, mc.font, "§0" + nameStr, this.getX() + this.width / 2, this.getY() + this.height + 2, 0);
        }
    }
}
