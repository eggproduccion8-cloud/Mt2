package com.mundodetronos2.client;

import com.mundodetronos2.config.ConfigManager;
import com.mundodetronos2.network.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "mundodetronos2", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents {

    public static class Notification {
        public String title;
        public String line2;
        public String line3;
        public long endTime;
        public boolean isDanger;

        public Notification(String title, String line2, String line3, int durationMs, boolean isDanger) {
            this.title = title;
            this.line2 = line2;
            this.line3 = line3;
            this.endTime = System.currentTimeMillis() + durationMs;
            this.isDanger = isDanger;
        }
    }

    public static final java.util.List<Notification> activeNotifications = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void addNotification(String title, String line2, String line3, boolean isDanger) {
        activeNotifications.add(new Notification(title, line2, line3, 3500, isDanger));
    }

    private static int clientTickCount = 0;

    // Ticks para simular el pestañeo (Apertura/Cierre de ojos)
    public static int eyeTransitionTicks = -1;
    public static int maxTransitionTicks = 60;

    // Estado de bloqueo de cinemática obligatoria
    public static boolean cinematicActive = false;

    // Límites visuales de la base
    public static boolean showBaseLimits = true;
    public static boolean showHud = true;

    // Alertas de muerte en tiempo real del HUD RPG
    public static UUID lastDeadPlayerId = null;
    public static String lastDeadPlayerName = "";
    public static long deathAlertEndTime = 0;

    public static void triggerDeathAlert(UUID playerId, String playerName) {
        lastDeadPlayerId = playerId;
        lastDeadPlayerName = playerName;
        deathAlertEndTime = System.currentTimeMillis() + 6000L; // 6 segundos de duración
        addNotification("☠ MUERTE RPG", playerName.toUpperCase(), "Perdió 5 pts", true);
    }

    // Alertas de caída de trono en tiempo real del HUD
    public static String throneAlertRealmName = "";
    public static UUID throneAlertAttackerId = null;
    public static String throneAlertAttackerName = "";
    public static int throneAlertOldLives = 0;
    public static int throneAlertNewLives = 0;
    public static long throneAlertEndTime = 0;

    public static void triggerThroneAlert(String realmName, UUID attackerId, String attackerName, int oldLives, int newLives) {
        throneAlertRealmName = realmName;
        throneAlertAttackerId = attackerId;
        throneAlertAttackerName = attackerName;
        throneAlertOldLives = oldLives;
        throneAlertNewLives = newLives;
        throneAlertEndTime = System.currentTimeMillis() + 8000L; // 8 segundos de duración
        addNotification("✦ TRONO CAÍDO", realmName.toUpperCase(), "Reconstrucción...", true);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (cinematicActive) return; // Bloquear KeyMappings durante cinemáticas

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null && event.getAction() == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
            if (event.getKey() == KeyInit.OPEN_SKILLS_KEY.getKey().getValue()) {
                String role = getClientPlayerRole();
                if (role.equalsIgnoreCase("none") || role.isEmpty()) {
                    if (mc.player != null) {
                        mc.player.displayClientMessage(Component.literal("§c⚠ Aún no tienes un rol asignado."), true);
                    }
                } else {
                    NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SOpenSkillTreePacket());
                }
            } else if (event.getKey() == KeyInit.OPEN_GUI_KEY.getKey().getValue()) {
                NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SOpenMainGuiPacket());
            } else if (event.getKey() == KeyInit.TOGGLE_HUD_KEY.getKey().getValue()) {
                showHud = !showHud;
                mc.player.displayClientMessage(Component.literal(showHud ? "§a[+] Coordenadas Visibles" : "§c[-] Coordenadas Ocultas"), true);
            } else if (event.getKey() == KeyInit.EDIT_HUD_KEY.getKey().getValue()) {
                mc.setScreen(new com.mundodetronos2.gui.HudEditorScreen());
            } else if (event.getKey() == KeyInit.OPEN_RPG_INVENTORY_KEY.getKey().getValue()) {
                NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SOpenRPGInventoryPacket());
            }
        }
    }

    private static String lastDimension = "";

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (eyeTransitionTicks >= 0) {
                eyeTransitionTicks--;
            }

            if (++clientTickCount % 20 == 0) {
                if (com.mundodetronos2.client.ClientPacketHandler.hudRemainingSeconds > 0) {
                    com.mundodetronos2.client.ClientPacketHandler.hudRemainingSeconds--;
                }
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) {
                if (cinematicActive) {
                    if (mc.screen == null || !(mc.screen instanceof com.mundodetronos2.gui.GoddessCinematicScreen
                        || mc.screen instanceof com.mundodetronos2.gui.GoddessIntroDialogueScreen
                        || mc.screen instanceof com.mundodetronos2.gui.GoddessDeathRebirthScreen)) {
                        cinematicActive = false;
                    } else {
                        // Forzar que el HUD vanilla no se oculte por F1 durante cinemáticas
                        mc.options.hideGui = false;
                    }
                }
                String currentDim = mc.level.dimension().location().toString();
                if (!currentDim.equals(lastDimension)) {
                    lastDimension = currentDim;
                }


                // Generar partículas de almas cayendo en la dimensión de la Diosa María
                if (currentDim.equals("mundodetronos2:role_dimension")) {
                    double px = mc.player.getX();
                    double py = mc.player.getY();
                    double pz = mc.player.getZ();
                    for (int i = 0; i < 5; i++) {
                        double rx = px + (mc.level.random.nextDouble() - 0.5D) * 32.0D;
                        double ry = py + 10.0D + mc.level.random.nextDouble() * 12.0D;
                        double rz = pz + (mc.level.random.nextDouble() - 0.5D) * 32.0D;

                        // Combinar End Rod y partículas mágicas para un aspecto místico de almas cayendo
                        net.minecraft.core.particles.SimpleParticleType pType = mc.level.random.nextBoolean() ?
                            net.minecraft.core.particles.ParticleTypes.END_ROD :
                            net.minecraft.core.particles.ParticleTypes.WITCH;

                        mc.level.addParticle(pType, rx, ry, rz,
                            (mc.level.random.nextDouble() - 0.5D) * 0.02D,
                            -0.06D - mc.level.random.nextDouble() * 0.04D,
                            (mc.level.random.nextDouble() - 0.5D) * 0.02D
                        );
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onScreenOpen(net.minecraftforge.client.event.ScreenEvent.Opening event) {
        // No interceptamos la pantalla de inventario vanilla. Tecla 'E' abre InventoryScreen vanilla normalmente.
    }

    @SubscribeEvent
    public static void onClientRightClickBlock(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
            net.minecraft.core.BlockPos pos = event.getPos();
            if (event.getLevel().getBlockState(pos).is(com.mundodetronos2.init.BlockInit.CARGA_ASALTO_BLOCK.get())) {
                event.setCanceled(true);
                event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
                ClientPacketHandler.handleOpenDefuseMinigame(pos);
            }
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.hasTag() && stack.getTag().contains("AuthorizedRole") && stack.getTag().contains("RoleItemID")) {
            String role = stack.getTag().getString("AuthorizedRole");
            String itemId = stack.getTag().getString("RoleItemID");
            String roleTranslated = "Guerrero";
            if (role.equalsIgnoreCase("berserker")) roleTranslated = "Berserker";
            else if (role.equalsIgnoreCase("mage")) roleTranslated = "Mago";
            else if (role.equalsIgnoreCase("arquero")) roleTranslated = "Arquero";
            else if (role.equalsIgnoreCase("paladin")) roleTranslated = "Paladín";
            else if (role.equalsIgnoreCase("draconico")) roleTranslated = "Dracónico";
            else if (role.equalsIgnoreCase("clerigo")) roleTranslated = "Clérigo";

            event.getToolTip().add(Component.literal("§a✔ Equipamiento Autorizado (" + roleTranslated + ")"));
            event.getToolTip().add(Component.literal("§7ID: " + itemId));
        }
    }

    @SubscribeEvent
    public static void onComputeFogColor(net.minecraftforge.client.event.ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.dimension().location().toString().equals("mundodetronos2:role_dimension")) {
            // Establecer un color blanco rosado claro mágico (R=0.98, G=0.88, B=0.92)
            event.setRed(0.98F);
            event.setGreen(0.88F);
            event.setBlue(0.92F);
        }
    }

    @SubscribeEvent
    public static void onRenderFog(net.minecraftforge.client.event.ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.dimension().location().toString().equals("mundodetronos2:role_dimension")) {
            // Configurar niebla mística de corto alcance para colorear el cielo entero
            event.setNearPlaneDistance(2.0F);
            event.setFarPlaneDistance(36.0F);
            event.setCanceled(true); // Forzar la renderización de nuestra niebla personalizada
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(net.minecraftforge.client.event.RenderLevelStageEvent event) {
        if (event.getStage() == net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;

            if (showBaseLimits && com.mundodetronos2.client.ClientPacketHandler.clientThroneX != 0) {
                String tDim = com.mundodetronos2.client.ClientPacketHandler.clientThroneDim;
                String currentDim = mc.level.dimension().location().toString();
                if (tDim.equalsIgnoreCase(currentDim)) {
                    double tx = com.mundodetronos2.client.ClientPacketHandler.clientThroneX;
                    double ty = com.mundodetronos2.client.ClientPacketHandler.clientThroneY;
                    double tz = com.mundodetronos2.client.ClientPacketHandler.clientThroneZ;

                    net.minecraft.world.phys.Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
                    com.mojang.blaze3d.vertex.PoseStack poseStack = event.getPoseStack();

                    poseStack.pushPose();
                    poseStack.translate(-camera.x, -camera.y, -camera.z);

                    net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(
                        tx - 75.0D, ty - 10.0D, tz - 75.0D,
                        tx + 75.0D, ty + 25.0D, tz + 75.0D
                    );

                    com.mojang.blaze3d.vertex.VertexConsumer buffer = mc.renderBuffers().bufferSource().getBuffer(net.minecraft.client.renderer.RenderType.lines());
                    net.minecraft.client.renderer.LevelRenderer.renderLineBox(poseStack, buffer, area, 0.83F, 0.68F, 0.21F, 0.6F);

                    poseStack.popPose();
                }
            }
        }
    }

    public static class ChatMessage {
        public String sender;
        public String text;
        public long timestamp;

        public ChatMessage(String sender, String text) {
            this.sender = sender;
            this.text = text;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static final java.util.List<ChatMessage> chatMessages = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void addChatMessage(String sender, String text) {
        chatMessages.add(new ChatMessage(sender, text));
        if (chatMessages.size() > 50) {
            chatMessages.remove(0);
        }
    }

    @SubscribeEvent
    public static void onClientChatReceived(net.minecraftforge.client.event.ClientChatReceivedEvent event) {
        // Intercept player chat only (exclude system messages)
        String raw = event.getMessage().getString();
        if (!raw.isEmpty()) {
            if (raw.contains("<") && raw.contains(">")) {
                int start = raw.indexOf("<");
                int end = raw.indexOf(">");
                String sender = raw.substring(start + 1, end);
                String msg = raw.substring(end + 1).trim();
                addChatMessage(sender, msg);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;

        // Cancelar overlays vanilla durante gameplay
        ResourceLocation id = event.getOverlay().id();
        if (id.equals(VanillaGuiOverlay.CHAT_PANEL.id())
            || id.equals(VanillaGuiOverlay.HOTBAR.id())
            || id.equals(VanillaGuiOverlay.PLAYER_HEALTH.id())
            || id.equals(VanillaGuiOverlay.FOOD_LEVEL.id())
            || id.equals(VanillaGuiOverlay.AIR_LEVEL.id())
            || id.equals(VanillaGuiOverlay.ARMOR_LEVEL.id())
            || id.equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        // Renderizado del HUD trasladado a ModHudOverlay (Registered NamedGuiOverlay "hud_mmorpg")
    }

    private static void drawNotificationsAndSiege(GuiGraphics graphics, int screenWidth) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int x = screenWidth - 145;
        int y = 10;

        // Panel de Misión Activa
        if (ClientPacketHandler.hudActiveMissionTitle != null && !ClientPacketHandler.hudActiveMissionTitle.isEmpty()) {
            graphics.drawString(mc.font, "MISIÓN ACTIVA", x, y, 0xFFFFD700, true);
            graphics.drawString(mc.font, ClientPacketHandler.hudActiveMissionTitle, x, y + 10, 0xFFFFFFFF, true);
            if (ClientPacketHandler.hudActiveMissionProgress != null && !ClientPacketHandler.hudActiveMissionProgress.isEmpty()) {
                graphics.drawString(mc.font, "Progreso: " + ClientPacketHandler.hudActiveMissionProgress, x, y + 20, 0xFF55FF55, true);
            }
            y += 34;
        }

        // 1. Si hay asedio activo, dibujar la tarjeta compacta arriba del todo a la derecha (Transparente)
        if (ClientPacketHandler.isAttackActive()) {
            int boxW = 135;
            int boxH = 52; // Taller para que quepa todo!

            graphics.fill(x - 3, y - 3, x + boxW + 3, y + boxH + 3, 0x33362819); // Madera café oscuro exterior muy trasparente
            graphics.fill(x - 1, y - 1, x + boxW + 1, y + boxH + 1, 0x88990000); // Rojo oscuro peligro
            graphics.fill(x, y, x + boxW, y + boxH, 0x44F3E5C8); // Pergamino muy translúcido

            long time = System.currentTimeMillis();
            String prefix = (time / 400) % 2 == 0 ? "§4⚔ " : "§c⚔ ";
            graphics.drawString(mc.font, prefix + "TRONO ATACADO", x + 5, y + 4, 0, false);

            String base = ClientPacketHandler.activeAttackBaseName;
            if (base.length() > 14) base = base.substring(0, 12) + "...";
            graphics.drawString(mc.font, "§0Base: §4" + base, x + 5, y + 14, 0, false);

            graphics.drawString(mc.font, "§0Vida: §d" + ClientPacketHandler.activeAttackThroneHp + "/" + ClientPacketHandler.activeAttackThroneMaxHp, x + 5, y + 24, 0, false);

            String attacker = ClientPacketHandler.activeAttackAttackerTeamName;
            if (attacker.length() > 14) attacker = attacker.substring(0, 12) + "...";
            graphics.drawString(mc.font, "§0Por: §5" + attacker, x + 5, y + 34, 0, false);

            String chargeTimeStr = "00:" + String.format("%02d", ClientPacketHandler.activeAttackSecondsLeft);
            graphics.drawString(mc.font, "§6Detona: " + chargeTimeStr, x + 5, y + 44, 0, false);

            y += boxH + 10;
        }

        // 2. Dibujar notificaciones en cola
        long now = System.currentTimeMillis();
        for (Notification notif : activeNotifications) {
            if (now > notif.endTime) {
                activeNotifications.remove(notif);
                continue;
            }

            int boxW = 135;
            int boxH = 32;

            int borderCol = notif.isDanger ? 0xFF990000 : 0xFFD4AF37;

            // Dibujar placa
            graphics.fill(x - 3, y - 3, x + boxW + 3, y + boxH + 3, 0xDD362819);
            graphics.fill(x - 1, y - 1, x + boxW + 1, y + boxH + 1, borderCol);
            graphics.fill(x, y, x + boxW, y + boxH, 0xCCF3E5C8);

            // Título
            graphics.drawString(mc.font, (notif.isDanger ? "§4" : "§6") + notif.title, x + 5, y + 4, 0, false);

            // Línea 2
            String l2 = notif.line2;
            if (l2.length() > 22) l2 = l2.substring(0, 20) + "...";
            graphics.drawString(mc.font, "§0" + l2, x + 5, y + 14, 0, false);

            // Línea 3 (si existe)
            if (notif.line3 != null && !notif.line3.isEmpty()) {
                String l3 = notif.line3;
                if (l3.length() > 22) l3 = l3.substring(0, 20) + "...";
                graphics.drawString(mc.font, "§0" + l3, x + 5, y + 24, 0, false);
            }

            y += boxH + 6;
        }
    }

    private static void drawThroneAlertHud(GuiGraphics graphics, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        int boxWidth = 160;
        int x = screenWidth - boxWidth - 12; // En medio a la derecha
        int y = screenHeight / 2 - 28;

        // Retrato del atacante (Face Skin)
        ResourceLocation skinTexture = DefaultPlayerSkin.getDefaultSkin(throneAlertAttackerId);
        try {
            if (mc.getConnection() != null && throneAlertAttackerId != null) {
                net.minecraft.client.multiplayer.PlayerInfo info = mc.getConnection().getPlayerInfo(throneAlertAttackerId);
                if (info != null) {
                    skinTexture = info.getSkinLocation();
                }
            }
        } catch (Exception ignored) {}

        PlayerFaceRenderer.draw(graphics, skinTexture, x, y, 26);

        // Mensaje del Evento
        graphics.drawString(mc.font, "¡EL TRONO HA CAÍDO!", x + 32, y, 0xFFFF5555, true);
        graphics.drawString(mc.font, "TRONO DE " + throneAlertRealmName.toUpperCase(), x + 32, y + 10, 0xFFFFAA00, true);
        graphics.drawString(mc.font, "Por: " + throneAlertAttackerName, x + 32, y + 20, 0xFFE0E0E0, true);
        graphics.drawString(mc.font, "VIDAS: " + throneAlertOldLives + " → " + throneAlertNewLives + " (-1)", x + 32, y + 30, 0xFFFF5555, true);
    }

    private static void drawDeathAlertHud(GuiGraphics graphics, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        int x = 12;
        int y = screenHeight / 2 - 25; // En medio a la izquierda

        // Retrato del jugador fallecido (Face Skin)
        ResourceLocation skinTexture = DefaultPlayerSkin.getDefaultSkin(lastDeadPlayerId);
        try {
            if (mc.getConnection() != null) {
                net.minecraft.client.multiplayer.PlayerInfo info = mc.getConnection().getPlayerInfo(lastDeadPlayerId);
                if (info != null) {
                    skinTexture = info.getSkinLocation();
                }
            }
        } catch (Exception ignored) {}

        PlayerFaceRenderer.draw(graphics, skinTexture, x, y, 26);

        // Mensaje del Evento RPG
        graphics.drawString(mc.font, lastDeadPlayerName.toUpperCase(), x + 32, y, 0xFFFF5555, true);
        graphics.drawString(mc.font, "ha perdido 5 pts", x + 32, y + 10, 0xFFCCCCCC, true);
        graphics.drawString(mc.font, "de su Reino.", x + 32, y + 20, 0xFFCCCCCC, true);
    }

    public static String getClientPlayerRole() {
        return com.mundodetronos2.client.ClientPacketHandler.hudPlayerRole;
    }

    public static void drawFlatCenteredString(GuiGraphics graphics, net.minecraft.client.gui.Font font, String text, int centerX, int y, int color) {
        int width = font.width(text);
        graphics.drawString(font, text, centerX - width / 2, y, color, false);
    }

    private static void drawCompassAndCoordinates(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int centerX = mc.getWindow().getGuiScaledWidth() / 2;
        int y1 = 6;
        int width = 140;

        // Obtener heading del jugador de 0 a 360 grados
        float yaw = mc.player.getYRot();
        float heading = (yaw % 360 + 360) % 360;

        // Puntos cardinales y sus correspondientes grados
        String[] points = { "S", "SO", "O", "NO", "N", "NE", "E", "SE" };
        int[] degrees = { 0, 45, 90, 135, 180, 225, 270, 315 };

        for (int i = 0; i < points.length; i++) {
            float diff = degrees[i] - heading;
            if (diff < -180) diff += 360;
            if (diff > 180) diff -= 360;

            int xPos = centerX + (int) diff;

            if (xPos >= centerX - width / 2 && xPos <= centerX + width / 2) {
                String label = points[i];
                int color = 0xFFCCCCCC; // Blanco/Gris transparente por defecto

                // Destacar N, S, E, O en dorado
                if (label.equals("N") || label.equals("S") || label.equals("E") || label.equals("O")) {
                    if (Math.abs(diff) < 10) {
                        color = 0xFFFFD700; // Dorado brillante
                    } else {
                        color = 0xFFD4AF37; // Dorado
                    }
                } else {
                    if (Math.abs(diff) < 10) {
                        color = 0xFFFFFFFF; // Blanco
                    }
                }

                int labelW = mc.font.width(label);
                graphics.drawString(mc.font, label, xPos - labelW / 2, y1, color, true);
            }
        }

        // Indicador de dirección central ▲
        graphics.drawString(mc.font, "▲", centerX - mc.font.width("▲") / 2, y1 + 10, 0xFFFFD700, true);

        // Coordenadas compactas debajo sin fondo
        String coordsStr = String.format("X: %d   Y: %d   Z: %d", mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ());
        drawFlatCenteredString(graphics, mc.font, coordsStr, centerX, y1 + 20, 0xFFE0E0E0);
    }

    private static void drawPermanentKingdomHud(GuiGraphics graphics) {
        if (!showHud) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        com.mundodetronos2.client.HudLayoutManager.ComponentConfig cardConfig = com.mundodetronos2.client.HudLayoutManager.getConfig(com.mundodetronos2.client.HudLayoutManager.ComponentId.PLAYER_CARD);
        if (!cardConfig.visible) return;

        int x = com.mundodetronos2.client.HudLayoutManager.getRenderX(com.mundodetronos2.client.HudLayoutManager.ComponentId.PLAYER_CARD, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        int y = com.mundodetronos2.client.HudLayoutManager.getRenderY(com.mundodetronos2.client.HudLayoutManager.ComponentId.PLAYER_CARD, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        int headSize = 32;

        ResourceLocation skinTexture = DefaultPlayerSkin.getDefaultSkin(mc.player.getUUID());
        try {
            if (mc.getConnection() != null) {
                net.minecraft.client.multiplayer.PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
                if (info != null) {
                    skinTexture = info.getSkinLocation();
                }
            }
        } catch (Exception ignored) {}

        // Resolver el Rol actual del jugador y su Tema
        String role = getClientPlayerRole();
        int roleColor = 0xFFFFD700; // Dorado por defecto
        String symbol = "ASPIRANTE";

        if (role.equalsIgnoreCase("berserker")) {
            roleColor = 0xFFFF5555;
            symbol = "BERSERKER";
        } else if (role.equalsIgnoreCase("guerrero")) {
            roleColor = 0xFFAAAAAA;
            symbol = "GUERRERO";
        } else if (role.equalsIgnoreCase("mago")) {
            roleColor = 0xFFBB55FF;
            symbol = "MAGO";
        } else if (role.equalsIgnoreCase("arquero")) {
            roleColor = 0xFF55FF55;
            symbol = "ARQUERO";
        } else if (role.equalsIgnoreCase("paladin")) {
            roleColor = 0xFFFFFF55;
            symbol = "PALADÍN";
        } else if (role.equalsIgnoreCase("draconico")) {
            roleColor = 0xFFFF9900;
            symbol = "DRACÓNICO";
        } else if (role.equalsIgnoreCase("clerigo")) {
            roleColor = 0xFF55FFFF;
            symbol = "CLÉRIGO";
        }

        // Cabeza del jugador a la izquierda (Sin fondos)
        PlayerFaceRenderer.draw(graphics, skinTexture, x, y + 2, headSize);

        // A la derecha (Sin panel café)
        int textX = x + headSize + 8;
        String name = mc.player.getGameProfile().getName();

        // NOMBRE
        graphics.drawString(mc.font, name, textX, y, 0xFFFFFFFF, true);

        // ROL
        graphics.drawString(mc.font, "ROL: " + symbol, textX, y + 10, roleColor, true);

        // TRONO
        graphics.drawString(mc.font, "TRONO: " + ClientPacketHandler.hudThroneLives, textX, y + 20, 0xFFFFD700, true);

        // TEAM VIDAS
        graphics.drawString(mc.font, "TEAM VIDAS: " + ClientPacketHandler.hudSharedPoints, textX, y + 30, 0xFF55FF55, true);

        // TIEMPO
        int totalSecs = ClientPacketHandler.hudRemainingSeconds;
        int hrs = totalSecs / 3600;
        int mins = (totalSecs % 3600) / 60;
        int secs = totalSecs % 60;
        String timeStr = String.format("%d:%02d:%02d", hrs, mins, secs);
        graphics.drawString(mc.font, "TIEMPO: " + timeStr, textX, y + 40, 0xFFFFFF55, true);
    }

    private static void drawEyeBlinkOverlay(GuiGraphics graphics, int width, int height) {
        float alpha = 0.0f;
        int half = maxTransitionTicks / 2;

        if (eyeTransitionTicks > half) {
            alpha = (float) (maxTransitionTicks - eyeTransitionTicks) / (float) half;
        } else {
            alpha = (float) eyeTransitionTicks / (float) half;
        }

        alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        int colorInt = ((int)(alpha * 255.0f) << 24) | 0x000000;

        graphics.fill(0, 0, width, height, colorInt);

        if (alpha > 0.85f) {
            graphics.drawString(Minecraft.getInstance().font, "La Diosa María te observa...", width / 2 - Minecraft.getInstance().font.width("La Diosa María te observa...") / 2, height / 2, 0xFFFFFFFF, false);
        }
    }

    private static void drawThroneHpBar(GuiGraphics graphics, int width, int height) {
        int barWidth = 140;
        int barHeight = 10;
        int x = (width - barWidth) / 2;
        int y = 25;

        // Si esta reparandose, dibujamos un pergamino espacioso en lugar de una barra pequeña para que respire
        if (ClientPacketHandler.targetThroneState.equalsIgnoreCase("REPAIRING")) {
            int cardW = 180;
            int cardH = 68;
            int cx = (width - cardW) / 2;
            int cy = 20;

            // Borde y fondo de pergamino limpio y medieval
            graphics.fill(cx - 3, cy - 3, cx + cardW + 3, cy + cardH + 3, 0xFF362819); // Madera
            graphics.fill(cx, cy, cx + cardW, cy + cardH, 0xEEF3E5C8); // Pergamino
            graphics.fill(cx + 2, cy + 2, cx + cardW - 2, cy + cardH - 2, 0xEEEBDAB3);

            Minecraft mc = Minecraft.getInstance();
            drawFlatCenteredString(graphics, mc.font, "§4§l✦ TRONO EN RECONSTRUCCIÓN ✦", cx + cardW / 2, cy + 6, 0);
            drawFlatCenteredString(graphics, mc.font, "§0VIDA DEL TRONO: §c0 / " + ClientPacketHandler.targetThroneMaxHealth, cx + cardW / 2, cy + 21, 0);
            drawFlatCenteredString(graphics, mc.font, "§0VIDAS DEL EQUIPO: §1" + ClientPacketHandler.hudThroneLives, cx + cardW / 2, cy + 34, 0);
            drawFlatCenteredString(graphics, mc.font, "§6§lEN RECONSTRUCCIÓN", cx + cardW / 2, cy + 48, 0);
            return;
        }

        // Fondo de la barra
        graphics.fill(x - 2, y - 2, x + barWidth + 2, y + barHeight + 2, 0xFF000000);
        graphics.fill(x, y, x + barWidth, y + barHeight, 0xFF444444);

        double ratio = (double) ClientPacketHandler.targetThroneHealth / Math.max(1, ClientPacketHandler.targetThroneMaxHealth);
        int fillWidth = (int) (barWidth * ratio);

        int barColor = 0xFFFF5555; // Rojo por defecto
        if (ClientPacketHandler.targetThroneState.equalsIgnoreCase("PROTECTED")) {
            barColor = 0xFF55FF55; // Verde protegido
        }

        if (fillWidth > 0) {
            graphics.fill(x, y, x + fillWidth, y + barHeight, barColor);
        }

        // Texto informativo
        Minecraft mc = Minecraft.getInstance();
        String title = "Trono: " + ClientPacketHandler.targetThroneRealmName;
        String hpText = ClientPacketHandler.targetThroneHealth + " / " + ClientPacketHandler.targetThroneMaxHealth + " HP";

        int titleWidth = mc.font.width(title);
        int hpWidth = mc.font.width(hpText);

        graphics.drawString(mc.font, title, (width - titleWidth) / 2, y - 11, 0xFFFFFFFF, false);
        graphics.drawString(mc.font, hpText, (width - hpWidth) / 2, y + 1, 0xFFFFFFFF, false);
    }
}
