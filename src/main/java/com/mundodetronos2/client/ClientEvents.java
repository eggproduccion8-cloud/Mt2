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

    public static int eyeTransitionTicks = -1;
    public static int maxTransitionTicks = 60;

    public static boolean cinematicActive = false;

    public static boolean showBaseLimits = true;
    public static boolean showHud = true;
    public static boolean enableCustomChat = true;

    public static UUID lastDeadPlayerId = null;
    public static String lastDeadPlayerName = "";
    public static long deathAlertEndTime = 0;

    public static void triggerDeathAlert(UUID playerId, String playerName) {
        lastDeadPlayerId = playerId;
        lastDeadPlayerName = playerName;
        deathAlertEndTime = System.currentTimeMillis() + 6000L;
        addNotification("☠ MUERTE RPG", playerName.toUpperCase(), "Perdió 5 pts", true);
    }

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
        throneAlertEndTime = System.currentTimeMillis() + 8000L;
        addNotification("✦ TRONO CAÍDO", realmName.toUpperCase(), "Reconstrucción...", true);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (cinematicActive) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null && event.getAction() == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
            if (event.getKey() == KeyInit.TOGGLE_HUD_KEY.getKey().getValue()) {
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
                        || mc.screen instanceof com.mundodetronos2.gui.GoddessDeathRebirthScreen)) {
                        cinematicActive = false;
                    } else {
                        mc.options.hideGui = false;
                    }
                }
                String currentDim = mc.level.dimension().location().toString();
                if (!currentDim.equals(lastDimension)) {
                    lastDimension = currentDim;
                }

                if (currentDim.equals("mundodetronos2:role_dimension")) {
                    double px = mc.player.getX();
                    double py = mc.player.getY();
                    double pz = mc.player.getZ();
                    for (int i = 0; i < 5; i++) {
                        double rx = px + (mc.level.random.nextDouble() - 0.5D) * 32.0D;
                        double ry = py + 10.0D + mc.level.random.nextDouble() * 12.0D;
                        double rz = pz + (mc.level.random.nextDouble() - 0.5D) * 32.0D;

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
            event.setRed(0.98F);
            event.setGreen(0.88F);
            event.setBlue(0.92F);
        }
    }

    @SubscribeEvent
    public static void onRenderFog(net.minecraftforge.client.event.ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.dimension().location().toString().equals("mundodetronos2:role_dimension")) {
            event.setNearPlaneDistance(2.0F);
            event.setFarPlaneDistance(36.0F);
            event.setCanceled(true);
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

        ResourceLocation id = event.getOverlay().id();
        if (id.equals(VanillaGuiOverlay.CHAT_PANEL.id()) && enableCustomChat) {
            event.setCanceled(true);
        }
    }

    public static String getClientPlayerRole() {
        return com.mundodetronos2.client.ClientPacketHandler.hudPlayerRole;
    }

    public static void drawFlatCenteredString(GuiGraphics graphics, net.minecraft.client.gui.Font font, String text, int centerX, int y, int color) {
        int width = font.width(text);
        graphics.drawString(font, text, centerX - width / 2, y, color, false);
    }
}
