package com.mundodetronos2.gui;

import com.mundodetronos2.network.NetworkManager;
import com.mundodetronos2.realm.InviteData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class MainGuiScreen extends Screen {
    private final NetworkManager.S2COpenMainGuiPacket initialData;
    private final List<NetworkManager.S2CRealmListPacket.RealmInfo> cachedRealms = new ArrayList<>();

    private final String roleName;
    private final int roleLevel;

    private Tab currentTab = Tab.MAIN;

    private EditBox realmNameInput;
    private String selectedColor = "#FF5555";

    private int inviteScrollOffset = 0;
    private int searchScrollOffset = 0;

    public enum Tab {
        MAIN,
        CREATE,
        SEARCH,
        MY_REALM,
        INVITES
    }

    public MainGuiScreen(NetworkManager.S2COpenMainGuiPacket initialData, String roleName, int roleLevel) {
        super(Component.literal("Mundo de Tronos 2"));
        this.initialData = initialData;
        this.roleName = roleName != null ? roleName : "";
        this.roleLevel = roleLevel;
        this.currentTab = Tab.MAIN;
    }

    @Override
    protected void init() {
        this.clearWidgets();

        int buttonWidth = 140;
        int buttonHeight = 20;
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (currentTab == Tab.MAIN) {
            if (initialData.hasRealm()) {
                this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(centerX - buttonWidth / 2, centerY - 40, buttonWidth, buttonHeight, Component.literal("Mi Equipo"), btn -> {
                    this.currentTab = Tab.MY_REALM;
                    this.init();
                }));
            } else {
                this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(centerX - buttonWidth / 2, centerY - 40, buttonWidth, buttonHeight, Component.literal("Crear Equipo"), btn -> {
                    this.currentTab = Tab.CREATE;
                    this.init();
                }));
            }

            this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(centerX - buttonWidth / 2, centerY - 12, buttonWidth, buttonHeight, Component.literal("Mi Perfil"), btn -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.setScreen(new RoleCardScreen(
                        mc.player.getUUID(),
                        mc.player.getGameProfile().getName(),
                        roleName,
                        roleLevel,
                        initialData.hasRealm() ? initialData.getRealmName() : "Ninguno",
                        initialData.hasRealm() ? (initialData.isOwner() ? "Líder" : "Miembro") : "N/A"
                    ));
                }
            }));

            this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(centerX - buttonWidth / 2, centerY + 16, buttonWidth, buttonHeight, Component.literal("Ver Invitaciones (" + initialData.getInvites().size() + ")"), btn -> {
                this.currentTab = Tab.INVITES;
                this.init();
            }));

            this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(centerX - buttonWidth / 2, centerY + 44, buttonWidth, buttonHeight, Component.literal("Cerrar"), btn -> {
                this.onClose();
            }));

        } else if (currentTab == Tab.CREATE) {
            this.realmNameInput = new EditBox(this.font, centerX - 100, centerY - 40, 200, 20, Component.literal("Nombre del Reino"));
            this.realmNameInput.setMaxLength(24);
            this.addRenderableWidget(this.realmNameInput);

            String[] colors = {"#FF5555", "#55FF55", "#5555FF", "#FFFF55", "#FF55FF", "#55FFFF", "#FFAA00"};
            int colorX = centerX - 100;
            for (int i = 0; i < colors.length; i++) {
                final String col = colors[i];
                this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(colorX + i * 28, centerY - 5, 24, 20, Component.literal("■"), btn -> {
                    this.selectedColor = col;
                }));
            }

            this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(centerX - 102, centerY + 30, 100, buttonHeight, Component.literal("Confirmar"), btn -> {
                String name = this.realmNameInput.getValue().trim();
                NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SCreateRealmPacket(name));
                this.onClose();
            }));

            this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(centerX + 2, centerY + 30, 100, buttonHeight, Component.literal("Volver"), btn -> {
                this.currentTab = Tab.MAIN;
                this.init();
            }));

        } else if (currentTab == Tab.SEARCH) {
            int listY = centerY - 50;
            for (int i = 0; i < 4; i++) {
                int index = i + searchScrollOffset;
                if (index < cachedRealms.size()) {
                    NetworkManager.S2CRealmListPacket.RealmInfo info = cachedRealms.get(index);
                    int itemY = listY + i * 26;

                    this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(centerX + 40, itemY, 60, 20, Component.literal("Unirse"), btn -> {
                        NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SRequestJoinRealmPacket(info.id));
                        this.onClose();
                    }));
                }
            }

            if (cachedRealms.size() > 4) {
                this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(centerX + 110, listY, 20, 20, Component.literal("▲"), btn -> {
                    if (searchScrollOffset > 0) {
                        searchScrollOffset--;
                        this.init();
                    }
                }));

                this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(centerX + 110, listY + 78, 20, 20, Component.literal("▼"), btn -> {
                    if (searchScrollOffset < cachedRealms.size() - 4) {
                        searchScrollOffset++;
                        this.init();
                    }
                }));
            }

            this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(centerX - 50, centerY + 65, 100, buttonHeight, Component.literal("Volver"), btn -> {
                this.currentTab = Tab.MAIN;
                this.init();
            }));

        } else if (currentTab == Tab.MY_REALM) {
            int buttonY = centerY + 35;

            if (initialData.hasRealm()) {
                this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(centerX - 70, buttonY, 140, buttonHeight, Component.literal("Salir del Reino"), btn -> {
                    NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SLeaveRealmPacket());
                    this.onClose();
                }));
                buttonY += 24;
            }

            this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(centerX - 70, buttonY, 140, buttonHeight, Component.literal("Volver"), btn -> {
                this.currentTab = Tab.MAIN;
                this.init();
            }));

        } else if (currentTab == Tab.INVITES) {
            int listY = centerY - 50;
            List<InviteData> invites = initialData.getInvites();

            for (int i = 0; i < 3; i++) {
                int index = i + inviteScrollOffset;
                if (index < invites.size()) {
                    InviteData inv = invites.get(index);
                    int itemY = listY + i * 30;

                    this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(centerX + 30, itemY, 20, 20, Component.literal("✓"), btn -> {
                        NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SAcceptInvitePacket(inv.getInviteId()));
                        this.onClose();
                    }));

                    this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(centerX + 55, itemY, 20, 20, Component.literal("✗"), btn -> {
                        NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SRejectInvitePacket(inv.getInviteId()));
                        this.onClose();
                    }));
                }
            }

            if (invites.size() > 3) {
                this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(centerX + 90, listY, 20, 20, Component.literal("▲"), btn -> {
                    if (inviteScrollOffset > 0) {
                        inviteScrollOffset--;
                        this.init();
                    }
                }));

                this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(centerX + 90, listY + 60, 20, 20, Component.literal("▼"), btn -> {
                    if (inviteScrollOffset < invites.size() - 3) {
                        inviteScrollOffset++;
                        this.init();
                    }
                }));
            }

            this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(centerX - 50, centerY + 65, 100, buttonHeight, Component.literal("Volver"), btn -> {
                this.currentTab = Tab.MAIN;
                this.init();
            }));
        }
    }

    public void updateRealmList(List<?> list) {
        this.cachedRealms.clear();
        for (Object obj : list) {
            if (obj instanceof NetworkManager.S2CRealmListPacket.RealmInfo) {
                this.cachedRealms.add((NetworkManager.S2CRealmListPacket.RealmInfo) obj);
            }
        }
        this.init();
    }

    public static String getTranslatedRole(String roleId) {
        if (roleId == null) return "Sin Elegir";
        String key = roleId.toLowerCase().trim();
        if (key.equals("guerrero") || key.equals("warrior")) return "Guerrero";
        if (key.equals("berserker")) return "Berserker";
        if (key.equals("mago") || key.equals("mage")) return "Mago";
        if (key.equals("arquero") || key.equals("archer")) return "Arquero";
        if (key.equals("paladin") || key.equals("paladín")) return "Paladín";
        if (key.equals("draconico") || key.equals("dracónico")) return "Dracónico";
        if (key.equals("clerigo") || key.equals("clérigo")) return "Clérigo";
        return roleId.toUpperCase();
    }

    private static class ScreenParticle {
        double x, y;
        double vx, vy;
        int color;
        int maxAge;
        int age;
    }
    private final List<ScreenParticle> particles = new ArrayList<>();

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);

        if (particles.size() < 40 && Math.random() < 0.4) {
            ScreenParticle p = new ScreenParticle();
            p.x = Math.random() * this.width;
            p.y = this.height + 10;
            p.vx = (Math.random() - 0.5) * 1.5;
            p.vy = -0.5 - Math.random() * 1.5;
            p.color = Math.random() < 0.5 ? 0xFFD4AF37 : 0xFFD48AD4;
            p.maxAge = 60 + (int) (Math.random() * 80);
            p.age = 0;
            particles.add(p);
        }

        for (int i = particles.size() - 1; i >= 0; i--) {
            ScreenParticle p = particles.get(i);
            p.age++;
            p.x += p.vx;
            p.y += p.vy;
            if (p.age >= p.maxAge || p.x < 0 || p.x > this.width || p.y < 0) {
                particles.remove(i);
                continue;
            }

            float alpha = 1.0f - ((float) p.age / p.maxAge);
            int colorWithAlpha = ((int) (alpha * 255) << 24) | (p.color & 0x00FFFFFF);
            graphics.fill((int) p.x, (int) p.y, (int) p.x + 2, (int) p.y + 2, colorWithAlpha);
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int menuWidth = 220;
        int menuHeight = 180;
        int x = centerX - menuWidth / 2;
        int y = centerY - menuHeight / 2 - 10;

        graphics.fill(x - 4, y - 4, x + menuWidth + 4, y + menuHeight + 4, 0xFF362819);
        graphics.fill(x - 2, y - 2, x + menuWidth + 2, y + menuHeight + 2, 0xFF4A3B2C);

        graphics.fill(x, y, x + menuWidth, y + menuHeight, 0xFFF3E5C8);
        graphics.fill(x + 3, y + 3, x + menuWidth - 3, y + menuHeight - 3, 0xFFEBDAB3);

        int innerBorderColor = 0xFF8F7051;
        graphics.fill(x + 5, y + 5, x + menuWidth - 5, y + 6, innerBorderColor);
        graphics.fill(x + 5, y + menuHeight - 6, x + menuWidth - 5, y + menuHeight - 5, innerBorderColor);
        graphics.fill(x + 5, y + 5, x + 6, y + menuHeight - 5, innerBorderColor);
        graphics.fill(x + menuWidth - 6, y + 5, x + menuWidth - 5, y + menuHeight - 5, innerBorderColor);

        if (currentTab == Tab.MAIN) {
            String title = "MUNDO DE TRONOS 2";
            String subtitle = "Prototipo Base v1.0.0";
            String footer = "Autor: WYLDUNE | Licencias de eggproduccion";

            graphics.drawString(this.font, "§4" + title, centerX - this.font.width(title) / 2, y + 16, 0, false);
            graphics.drawString(this.font, "§8" + subtitle, centerX - this.font.width(subtitle) / 2, y + 28, 0, false);
            graphics.drawString(this.font, "§8" + footer, centerX - this.font.width(footer) / 2, y + menuHeight - 18, 0, false);

        } else if (currentTab == Tab.CREATE) {
            String title = "Crear nuevo Reino";
            graphics.drawString(this.font, "§4" + title, centerX - this.font.width(title) / 2, y + 15, 0, false);
            graphics.drawString(this.font, "§0Nombre del reino:", centerX - 100, centerY - 52, 0, false);
            graphics.drawString(this.font, "§0Color del reino:", centerX - 100, centerY - 17, 0, false);

            int colorInt = Integer.parseInt(selectedColor.substring(1), 16);
            graphics.fill(centerX + 70, centerY - 18, centerX + 100, centerY - 3, 0xFF000000 | colorInt);

        } else if (currentTab == Tab.SEARCH) {
            String title = "Buscar Reinos";
            graphics.drawString(this.font, "§4" + title, centerX - this.font.width(title) / 2, y + 15, 0, false);

            int listY = centerY - 50;
            if (cachedRealms.isEmpty()) {
                String noReinos = "No hay reinos creados todavía.";
                graphics.drawString(this.font, "§8" + noReinos, centerX - this.font.width(noReinos) / 2, centerY - 10, 0, false);
            } else {
                for (int i = 0; i < 4; i++) {
                    int index = i + searchScrollOffset;
                    if (index < cachedRealms.size()) {
                        NetworkManager.S2CRealmListPacket.RealmInfo info = cachedRealms.get(index);
                        int itemY = listY + i * 26;

                        int cInt = Integer.parseInt(info.color.substring(1), 16);
                        graphics.fill(centerX - 100, itemY + 4, centerX - 92, itemY + 16, 0xFF000000 | cInt);
                        graphics.drawString(this.font, "§0" + info.name, centerX - 85, itemY + 6, 0, false);
                        graphics.drawString(this.font, "§5(" + info.members + "/" + info.maxPlayers + ")", centerX + 15, itemY + 6, 0, false);
                    }
                }
            }

        } else if (currentTab == Tab.MY_REALM) {
            String title = "MI REINO";
            graphics.drawString(this.font, "§4" + title, centerX - this.font.width(title) / 2, y + 14, 0, false);

            int infoY = y + 32;
            graphics.fill(x + 12, infoY, x + menuWidth - 12, infoY + 85, 0xFFE5D5B0);

            if (initialData.hasRealm()) {
                graphics.drawString(this.font, "§0Nombre: §2" + initialData.getRealmName(), x + 18, infoY + 10, 0, false);
                graphics.drawString(this.font, "§0Vidas: §4" + initialData.getCurrentLives() + " / " + initialData.getMaxLives(), x + 18, infoY + 26, 0, false);
                graphics.drawString(this.font, "§0Miembros: §1" + initialData.getMemberCount() + " / " + initialData.getMaxPlayers(), x + 18, infoY + 42, 0, false);
                graphics.drawString(this.font, "§0Rango: §5" + (initialData.isOwner() ? "Líder" : "Miembro"), x + 18, infoY + 58, 0, false);
            } else {
                String noReino = "No perteneces a ningún reino.";
                String noReinoSub = "¡Crea o busca uno en el menú principal!";
                graphics.drawString(this.font, "§8" + noReino, centerX - this.font.width(noReino) / 2, infoY + 28, 0, false);
                graphics.drawString(this.font, "§8" + noReinoSub, centerX - this.font.width(noReinoSub) / 2, infoY + 44, 0, false);
            }

        } else if (currentTab == Tab.INVITES) {
            String title = "Tus Invitaciones";
            graphics.drawString(this.font, "§4" + title, centerX - this.font.width(title) / 2, y + 15, 0, false);

            int listY = centerY - 50;
            List<InviteData> invites = initialData.getInvites();
            if (invites.isEmpty()) {
                String noInv = "No tienes invitaciones pendientes.";
                graphics.drawString(this.font, "§8" + noInv, centerX - this.font.width(noInv) / 2, centerY - 10, 0, false);
            } else {
                for (int i = 0; i < 3; i++) {
                    int index = i + inviteScrollOffset;
                    if (index < invites.size()) {
                        InviteData inv = invites.get(index);
                        int itemY = listY + i * 30;

                        graphics.drawString(this.font, "§0" + inv.getRealmName(), centerX - 100, itemY + 2, 0, false);
                        graphics.drawString(this.font, "§5De: " + inv.getSenderName(), centerX - 100, itemY + 11, 0, false);
                    }
                }
            }
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
