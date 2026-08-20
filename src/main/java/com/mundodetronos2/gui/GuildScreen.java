package com.mundodetronos2.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mundodetronos2.network.NetworkManager;
import com.mundodetronos2.realm.RealmManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class GuildScreen extends Screen {

    private final NetworkManager.S2COpenMainGuiPacket initialData;
    private final String playerRole;
    private final int playerRoleLevel;

    private EditBox renameBox;
    private EditBox logoBox;
    private boolean showingRename = false;
    private boolean showingLogo = false;

    public GuildScreen(NetworkManager.S2COpenMainGuiPacket initialData, String playerRole, int playerRoleLevel) {
        super(Component.literal("Gremio de Mundo de Tronos"));
        this.initialData = initialData;
        this.playerRole = playerRole != null ? playerRole : "";
        this.playerRoleLevel = playerRoleLevel;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (!initialData.hasRealm()) {
            // UNASSIGNED PLAYER: 10 COLOR TEAM SELECTION GRID
            int startX = centerX - 200;
            int startY = centerY - 50;
            int btnW = 75;
            int btnH = 22;

            for (int i = 0; i < RealmManager.PREDEFINED_COLORS.length; i++) {
                final String colorKey = RealmManager.PREDEFINED_COLORS[i];
                int col = i % 5;
                int row = i / 5;
                int bx = startX + col * 82;
                int by = startY + row * 45;

                String colorDisplay = colorKey.substring(0, 1).toUpperCase() + colorKey.substring(1);
                com.mundodetronos2.realm.RealmData r = RealmManager.getRealmByColorKey(colorKey);
                int count = r != null ? r.getMembers().size() : 0;
                boolean isFull = count >= 6;

                Button btn = Button.builder(Component.literal(colorDisplay + "\n§7" + count + "/6"), b -> {
                    NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SCreateRealmPacket(colorKey));
                    this.onClose();
                }).bounds(bx, by, btnW, btnH + 10).build();

                if (isFull) {
                    btn.active = false;
                }
                this.addRenderableWidget(btn);
            }

            this.addRenderableWidget(Button.builder(Component.literal("§cSalir"), b -> this.onClose())
                    .bounds(centerX - 40, centerY + 65, 80, 20).build());
        } else {
            // GUILD DASHBOARD FOR TEAM MEMBERS
            int bx = centerX - 160;
            int by = centerY - 50;

            this.addRenderableWidget(Button.builder(Component.literal("§1Mi Equipo"), b -> {
                this.showingRename = false;
                this.showingLogo = false;
            }).bounds(bx, by, 90, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("§2Misiones"), b -> {
                NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SRequestNpcDialoguePacket("karla"));
                this.onClose();
            }).bounds(bx, by + 24, 90, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("§eMonedas"), b -> {
                Minecraft.getInstance().player.displayClientMessage(Component.literal("§e✦ Monedas Personales: §f" + com.mundodetronos2.client.ClientPacketHandler.hudSharedPoints), false);
            }).bounds(bx, by + 48, 90, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("§bInformación"), b -> {
                Minecraft.getInstance().player.displayClientMessage(Component.literal("§b[Gremio] Nombre: " + initialData.getRealmName() + " | Miembros: " + initialData.getMemberCount() + "/6"), false);
            }).bounds(bx, by + 72, 90, 20).build());

            if (initialData.isOwner()) {
                this.addRenderableWidget(Button.builder(Component.literal("§dCambiar Nombre"), b -> {
                    this.showingRename = !this.showingRename;
                    this.showingLogo = false;
                }).bounds(bx, by + 96, 90, 20).build());

                this.addRenderableWidget(Button.builder(Component.literal("§aCambiar Logo"), b -> {
                    this.showingLogo = !this.showingLogo;
                    this.showingRename = false;
                }).bounds(bx, by + 120, 90, 20).build());
            }

            this.addRenderableWidget(Button.builder(Component.literal("§cAbandonar"), b -> {
                NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SLeaveRealmPacket());
                this.onClose();
            }).bounds(bx, by + 144, 90, 20).build());

            this.renameBox = new EditBox(this.font, centerX - 40, centerY - 20, 140, 20, Component.literal("Nuevo Nombre"));
            this.addRenderableWidget(this.renameBox);

            this.logoBox = new EditBox(this.font, centerX - 40, centerY + 20, 140, 20, Component.literal("Nuevo Logo"));
            this.addRenderableWidget(this.logoBox);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (!initialData.hasRealm()) {
            graphics.drawCenteredString(this.font, "§6§l✦ GREMIO — SELECCIÓN DE EQUIPO ✦", centerX, centerY - 85, 0xFFFFFFFF);
            graphics.drawCenteredString(this.font, "§7Elige el equipo/color al que deseas pertenecer (Máximo 6 por equipo)", centerX, centerY - 72, 0xFFCCCCCC);
        } else {
            graphics.drawCenteredString(this.font, "§6§l✦ GREMIO DE MUNDO DE TRONOS ✦", centerX, centerY - 85, 0xFFFFFFFF);

            // Informacion del Equipo
            int x = centerX - 50;
            int y = centerY - 50;

            graphics.drawString(this.font, "§0Equipo: §1" + initialData.getRealmName(), x, y, 0, false);
            graphics.drawString(this.font, "§0Color: §5" + initialData.getRealmColor(), x, y + 14, 0, false);
            graphics.drawString(this.font, "§0Líder: §d" + (initialData.isOwner() ? "Tú eres el Líder" : "Líder Asignado"), x, y + 28, 0, false);
            graphics.drawString(this.font, "§0Miembros: §2" + initialData.getMemberCount() + " / 6", x, y + 42, 0, false);

            if (showingRename && renameBox != null) {
                this.renameBox.render(graphics, mouseX, mouseY, partialTicks);
            }
            if (showingLogo && logoBox != null) {
                this.logoBox.render(graphics, mouseX, mouseY, partialTicks);
            }
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
