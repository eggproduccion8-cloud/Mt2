package com.mundodetronos2.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mundodetronos2.client.ClientEvents;
import com.mundodetronos2.dialogue.DialogueNode;
import com.mundodetronos2.dialogue.DialogueOption;
import com.mundodetronos2.network.NetworkManager;
import com.mundodetronos2.npc.NpcRegistryManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Type;
import java.util.*;

public class NpcEditorScreen extends Screen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final int entityId;
    private final String npcType;
    private final String npcName;
    private final String skinName;
    private final String textData;

    private Tab currentTab = Tab.GENERAL;
    private NpcRegistryManager.NpcConfig npcConfig;

    // Diálogos mapeados
    private Map<String, DialogueNode> dialoguesMap = new LinkedHashMap<>();
    private String selectedNodeId = "inicio";
    private int selectedOptionIndex = -1;

    public enum Tab {
        GENERAL("General"),
        DIALOGOS("Diálogos"),
        MISIONES("Misiones"),
        COMERCIO("Comercio"),
        REQUISITOS("Requisitos"),
        PROGRESION("Progresión"),
        SLOTS("Slots"),
        APARIENCIA("Apariencia"),
        ACCIONES("Acciones"),
        PREVISUALIZACION("Previs.");

        private final String label;
        Tab(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    // EditBoxes para Pestaña GENERAL
    private EditBox nameInput;
    private EditBox skinInput;
    private EditBox typeInput;
    private EditBox scaleInput;

    // EditBoxes para Pestaña DIÁLOGOS
    private EditBox nodeTextInput;
    private EditBox optionTextInput;
    private EditBox optionNextNodeInput;
    private EditBox optionActionInput;

    // EditBoxes para Pestaña REQUISITOS
    private EditBox rpgLevelInput;
    private EditBox campaignLevelInput;
    private EditBox requiredRoleInput;

    // EditBoxes para Pestaña COMERCIO
    private EditBox tradeInput1;
    private EditBox tradeCount1;
    private EditBox tradeOutput;
    private EditBox tradeOutputCount;
    private EditBox tradePointCost;

    public NpcEditorScreen(int entityId, String npcType, String npcName, String skinName, String textData) {
        super(Component.literal("EDITOR DE NPC"));
        this.entityId = entityId;
        this.npcType = npcType;
        this.npcName = npcName;
        this.skinName = skinName;
        this.textData = textData;

        this.npcConfig = new NpcRegistryManager.NpcConfig();
        this.npcConfig.general.internalId = npcType;
        this.npcConfig.general.name = npcName;
        this.npcConfig.general.skin = skinName;
        this.npcConfig.general.npcType = npcType;

        // Cargar diálogo por defecto
        DialogueNode defaultNode = new DialogueNode("inicio", "Hola, viajero. ¿En qué puedo ayudarte?");
        defaultNode.addOption(new DialogueOption("Hablar", "inicio", "CLOSE"));
        this.dialoguesMap.put("inicio", defaultNode);
    }

    @Override
    protected void init() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int menuWidth = 420;
        int menuHeight = 240;
        int x = centerX - menuWidth / 2;
        int y = centerY - menuHeight / 2 - 10;

        // --- BOTONES DE PESTAÑAS BARRAS LATERALES ---
        Tab[] tabs = Tab.values();
        int sidebarW = 75;
        int sidebarX = x + 8;
        int sidebarY = y + 25;

        for (int i = 0; i < tabs.length; i++) {
            Tab tab = tabs[i];
            int btnY = sidebarY + (i * 17);

            boolean isCurrent = (tab == currentTab);
            this.addRenderableWidget(new GoddessIntroDialogueScreen.TransparentButton(sidebarX, btnY, sidebarW, 15,
                    Component.literal((isCurrent ? "★ " : "") + tab.getLabel()), btn -> {
                this.currentTab = tab;
                this.init();
            }));
        }

        // --- CONTENIDO DE PESTAÑAS ---
        int contentX = x + 90;
        int contentY = y + 25;

        if (currentTab == Tab.GENERAL) {
            nameInput = new EditBox(this.font, contentX + 80, contentY, 150, 14, Component.literal("Nombre"));
            nameInput.setValue(npcConfig.general.name);
            this.addRenderableWidget(nameInput);

            skinInput = new EditBox(this.font, contentX + 80, contentY + 20, 150, 14, Component.literal("Skin"));
            skinInput.setValue(npcConfig.general.skin);
            this.addRenderableWidget(skinInput);

            typeInput = new EditBox(this.font, contentX + 80, contentY + 40, 150, 14, Component.literal("Tipo"));
            typeInput.setValue(npcConfig.general.npcType);
            this.addRenderableWidget(typeInput);

            scaleInput = new EditBox(this.font, contentX + 80, contentY + 60, 150, 14, Component.literal("Escala"));
            scaleInput.setValue(String.valueOf(npcConfig.general.scale));
            this.addRenderableWidget(scaleInput);

            this.addRenderableWidget(Button.builder(Component.literal("Invulnerable: " + (npcConfig.general.invulnerable ? "SÍ" : "NO")), btn -> {
                npcConfig.general.invulnerable = !npcConfig.general.invulnerable;
                this.init();
            }).bounds(contentX, contentY + 85, 150, 16).build());

            this.addRenderableWidget(Button.builder(Component.literal("Colisión: " + (npcConfig.general.hasCollision ? "SÍ" : "NO")), btn -> {
                npcConfig.general.hasCollision = !npcConfig.general.hasCollision;
                this.init();
            }).bounds(contentX + 155, contentY + 85, 150, 16).build());

        } else if (currentTab == Tab.DIALOGOS) {
            // Nodos a la izquierda
            int nIdx = 0;
            for (String nodeId : dialoguesMap.keySet()) {
                if (nIdx >= 4) break;
                final String nId = nodeId;
                boolean isSel = nodeId.equals(selectedNodeId);
                this.addRenderableWidget(Button.builder(Component.literal((isSel ? "★ " : "") + nodeId), btn -> {
                    selectedNodeId = nId;
                    selectedOptionIndex = -1;
                    this.init();
                }).bounds(contentX, contentY + (nIdx * 18), 75, 16).build());
                nIdx++;
            }

            this.addRenderableWidget(Button.builder(Component.literal("+ Nodo"), btn -> {
                String newId = "nodo_" + (dialoguesMap.size() + 1);
                dialoguesMap.put(newId, new DialogueNode(newId, "Nuevo texto..."));
                selectedNodeId = newId;
                this.init();
            }).bounds(contentX, contentY + 75, 75, 16).build());

            DialogueNode activeNode = dialoguesMap.get(selectedNodeId);
            if (activeNode != null) {
                nodeTextInput = new EditBox(this.font, contentX + 85, contentY, 230, 14, Component.literal("Texto Nodo"));
                nodeTextInput.setMaxLength(256);
                nodeTextInput.setValue(activeNode.getText());
                nodeTextInput.setResponder(val -> activeNode.setText(val));
                this.addRenderableWidget(nodeTextInput);

                // Opciones del nodo
                for (int optIdx = 0; optIdx < activeNode.getOptions().size(); optIdx++) {
                    if (optIdx >= 3) break;
                    final int idx = optIdx;
                    DialogueOption option = activeNode.getOptions().get(optIdx);
                    boolean isSelectedOpt = (optIdx == selectedOptionIndex);

                    this.addRenderableWidget(Button.builder(Component.literal((isSelectedOpt ? "▶ " : "") + option.getText()), btn -> {
                        selectedOptionIndex = idx;
                        this.init();
                    }).bounds(contentX + 85, contentY + 20 + (optIdx * 18), 110, 16).build());
                }

                this.addRenderableWidget(Button.builder(Component.literal("+ Resp"), btn -> {
                    activeNode.addOption(new DialogueOption("Nueva Respuesta", "inicio", "CLOSE"));
                    selectedOptionIndex = activeNode.getOptions().size() - 1;
                    this.init();
                }).bounds(contentX + 85, contentY + 75, 52, 16).build());

                if (selectedOptionIndex >= 0 && selectedOptionIndex < activeNode.getOptions().size()) {
                    DialogueOption selOpt = activeNode.getOptions().get(selectedOptionIndex);

                    optionTextInput = new EditBox(this.font, contentX + 200, contentY + 20, 115, 14, Component.literal("Texto Resp"));
                    optionTextInput.setValue(selOpt.getText());
                    optionTextInput.setResponder(val -> selOpt.setText(val));
                    this.addRenderableWidget(optionTextInput);

                    optionNextNodeInput = new EditBox(this.font, contentX + 200, contentY + 38, 115, 14, Component.literal("Siguiente Nodo"));
                    optionNextNodeInput.setValue(selOpt.getNextNodeId() != null ? selOpt.getNextNodeId() : "");
                    optionNextNodeInput.setResponder(val -> selOpt.setNextNodeId(val.trim()));
                    this.addRenderableWidget(optionNextNodeInput);

                    optionActionInput = new EditBox(this.font, contentX + 200, contentY + 56, 115, 14, Component.literal("Acción"));
                    optionActionInput.setValue(selOpt.getAction() != null ? selOpt.getAction() : "");
                    optionActionInput.setResponder(val -> selOpt.setAction(val.trim()));
                    this.addRenderableWidget(optionActionInput);
                }
            }

        } else if (currentTab == Tab.MISIONES) {
            this.addRenderableWidget(Button.builder(Component.literal("+ Asignar Misión"), btn -> {
                if (!npcConfig.missionIds.contains("tut_" + npcType)) {
                    npcConfig.missionIds.add("tut_" + npcType);
                    this.init();
                }
            }).bounds(contentX, contentY + 80, 130, 16).build());

        } else if (currentTab == Tab.COMERCIO) {
            if (npcConfig.trades.trades.isEmpty()) {
                NpcRegistryManager.NpcTradeData td = new NpcRegistryManager.NpcTradeData();
                td.id = "trade_1";
                npcConfig.trades.trades.add(td);
            }

            NpcRegistryManager.NpcTradeData activeTrade = npcConfig.trades.trades.get(0);

            tradeInput1 = new EditBox(this.font, contentX + 80, contentY, 130, 14, Component.literal("Input 1"));
            tradeInput1.setValue(activeTrade.input1);
            tradeInput1.setResponder(val -> activeTrade.input1 = val.trim());
            this.addRenderableWidget(tradeInput1);

            tradeCount1 = new EditBox(this.font, contentX + 220, contentY, 50, 14, Component.literal("Cant 1"));
            tradeCount1.setValue(String.valueOf(activeTrade.count1));
            tradeCount1.setResponder(val -> { try { activeTrade.count1 = Integer.parseInt(val.trim()); } catch (Exception ignored) {} });
            this.addRenderableWidget(tradeCount1);

            tradeOutput = new EditBox(this.font, contentX + 80, contentY + 22, 130, 14, Component.literal("Output"));
            tradeOutput.setValue(activeTrade.output);
            tradeOutput.setResponder(val -> activeTrade.output = val.trim());
            this.addRenderableWidget(tradeOutput);

            tradeOutputCount = new EditBox(this.font, contentX + 220, contentY + 22, 50, 14, Component.literal("Cant Out"));
            tradeOutputCount.setValue(String.valueOf(activeTrade.outputCount));
            tradeOutputCount.setResponder(val -> { try { activeTrade.outputCount = Integer.parseInt(val.trim()); } catch (Exception ignored) {} });
            this.addRenderableWidget(tradeOutputCount);

            tradePointCost = new EditBox(this.font, contentX + 80, contentY + 44, 80, 14, Component.literal("Puntos"));
            tradePointCost.setValue(String.valueOf(activeTrade.pointCost));
            tradePointCost.setResponder(val -> { try { activeTrade.pointCost = Integer.parseInt(val.trim()); } catch (Exception ignored) {} });
            this.addRenderableWidget(tradePointCost);

            this.addRenderableWidget(Button.builder(Component.literal("+ Añadir Trade"), btn -> {
                NpcRegistryManager.NpcTradeData td = new NpcRegistryManager.NpcTradeData();
                td.id = "trade_" + (npcConfig.trades.trades.size() + 1);
                npcConfig.trades.trades.add(td);
                this.init();
            }).bounds(contentX, contentY + 80, 110, 16).build());

        } else if (currentTab == Tab.REQUISITOS) {
            rpgLevelInput = new EditBox(this.font, contentX + 110, contentY, 120, 14, Component.literal("Nivel RPG"));
            rpgLevelInput.setValue(String.valueOf(npcConfig.requirements.requiredRpgLevel));
            this.addRenderableWidget(rpgLevelInput);

            campaignLevelInput = new EditBox(this.font, contentX + 110, contentY + 22, 120, 14, Component.literal("Nivel Campaña"));
            campaignLevelInput.setValue(String.valueOf(npcConfig.requirements.requiredCampaignLevel));
            this.addRenderableWidget(campaignLevelInput);

            requiredRoleInput = new EditBox(this.font, contentX + 110, contentY + 44, 120, 14, Component.literal("Rol"));
            requiredRoleInput.setValue(npcConfig.requirements.requiredRole);
            this.addRenderableWidget(requiredRoleInput);

        }

        // --- BOTONES INFERIORES GENERALES: GUARDAR / CANCELAR / REINICIAR ---
        int btnY = y + menuHeight - 22;
        this.addRenderableWidget(Button.builder(Component.literal("Guardar"), btn -> {
            saveChanges();
            this.onClose();
        }).bounds(centerX - 120, btnY, 70, 16).build());

        this.addRenderableWidget(Button.builder(Component.literal("Cancelar"), btn -> this.onClose())
                .bounds(centerX - 35, btnY, 70, 16).build());

        this.addRenderableWidget(Button.builder(Component.literal("Reiniciar"), btn -> this.init())
                .bounds(centerX + 50, btnY, 70, 16).build());
    }

    private void saveChanges() {
        if (nameInput != null) npcConfig.general.name = nameInput.getValue().trim();
        if (skinInput != null) npcConfig.general.skin = skinInput.getValue().trim();
        if (typeInput != null) npcConfig.general.npcType = typeInput.getValue().trim();
        if (scaleInput != null) {
            try { npcConfig.general.scale = Double.parseDouble(scaleInput.getValue().trim()); } catch (Exception ignored) {}
        }
        if (rpgLevelInput != null) {
            try { npcConfig.requirements.requiredRpgLevel = Integer.parseInt(rpgLevelInput.getValue().trim()); } catch (Exception ignored) {}
        }
        if (campaignLevelInput != null) {
            try { npcConfig.requirements.requiredCampaignLevel = Integer.parseInt(campaignLevelInput.getValue().trim()); } catch (Exception ignored) {}
        }
        if (requiredRoleInput != null) npcConfig.requirements.requiredRole = requiredRoleInput.getValue().trim();

        String fullJson = GSON.toJson(npcConfig);
        NetworkManager.INSTANCE.sendToServer(new NetworkManager.C2SUpdateNpcDialoguePacket(
                entityId,
                npcType,
                "all",
                fullJson,
                npcConfig.general.name,
                npcConfig.general.skin
        ));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int menuWidth = 420;
        int menuHeight = 240;
        int x = centerX - menuWidth / 2;
        int y = centerY - menuHeight / 2 - 10;

        // --- MARCO PERGAMINO MEDIEVAL ---
        graphics.fill(x - 4, y - 4, x + menuWidth + 4, y + menuHeight + 4, 0xFF362819);
        graphics.fill(x - 2, y - 2, x + menuWidth + 2, y + menuHeight + 2, 0xFF4A3B2C);
        graphics.fill(x, y, x + menuWidth, y + menuHeight, 0xEEF3E5C8);
        graphics.fill(x + 3, y + 3, x + menuWidth - 3, y + menuHeight - 3, 0xEEEBDAB3);

        int innerBorderColor = 0xFF8F7051;
        graphics.fill(x + 5, y + 5, x + menuWidth - 5, y + 6, innerBorderColor);
        graphics.fill(x + 5, y + menuHeight - 6, x + menuWidth - 5, y + menuHeight - 5, innerBorderColor);
        graphics.fill(x + 5, y + 5, x + 6, y + menuHeight - 5, innerBorderColor);
        graphics.fill(x + menuWidth - 6, y + 5, x + menuWidth - 5, y + menuHeight - 5, innerBorderColor);

        ClientEvents.drawFlatCenteredString(graphics, this.font, "§4✧ EDITOR PROFESIONAL DE NPC ✧", centerX, y + 8, 0);

        int contentX = x + 90;
        int contentY = y + 25;

        if (currentTab == Tab.GENERAL) {
            graphics.drawString(this.font, "§0Nombre:", contentX, contentY + 3, 0, false);
            graphics.drawString(this.font, "§0Skin:", contentX, contentY + 23, 0, false);
            graphics.drawString(this.font, "§0Tipo ID:", contentX, contentY + 43, 0, false);
            graphics.drawString(this.font, "§0Escala:", contentX, contentY + 63, 0, false);
        } else if (currentTab == Tab.DIALOGOS) {
            graphics.drawString(this.font, "§0Nodos de Diálogo", contentX, contentY - 12, 0, false);
            graphics.drawString(this.font, "§0Respuestas del Nodo " + selectedNodeId, contentX + 85, contentY + 8, 0, false);
        } else if (currentTab == Tab.MISIONES) {
            graphics.drawString(this.font, "§0MISIONES ASOCIADAS (" + npcConfig.missionIds.size() + " activas)", contentX, contentY, 0, false);
            for (int i = 0; i < npcConfig.missionIds.size(); i++) {
                graphics.drawString(this.font, "§8• " + npcConfig.missionIds.get(i), contentX, contentY + 16 + (i * 14), 0, false);
            }
        } else if (currentTab == Tab.COMERCIO) {
            graphics.drawString(this.font, "§0EDITAR TRADE PRINCIPAL", contentX, contentY - 12, 0, false);
            graphics.drawString(this.font, "§0Pagar:", contentX, contentY + 3, 0, false);
            graphics.drawString(this.font, "§0Recibir:", contentX, contentY + 25, 0, false);
            graphics.drawString(this.font, "§0Puntos:", contentX, contentY + 47, 0, false);
        } else if (currentTab == Tab.REQUISITOS) {
            graphics.drawString(this.font, "§0Nivel RPG Mínimo:", contentX, contentY + 3, 0, false);
            graphics.drawString(this.font, "§0Campaña Mínima:", contentX, contentY + 25, 0, false);
            graphics.drawString(this.font, "§0Rol Requerido:", contentX, contentY + 47, 0, false);
        } else if (currentTab == Tab.SLOTS) {
            graphics.drawString(this.font, "§0CONFIGURACIÓN DE SLOTS E INVENTARIO (0 - 17)", contentX, contentY, 0, false);
            graphics.drawString(this.font, "§8Permite asignar menúes, acciones o items al inventario del NPC.", contentX, contentY + 16, 0, false);
        } else if (currentTab == Tab.PREVISUALIZACION) {
            graphics.drawString(this.font, "§0PREVISUALIZACIÓN EN TIEMPO REAL", contentX, contentY, 0, false);
            graphics.drawString(this.font, "§5Nombre Actual: §0" + npcConfig.general.name, contentX, contentY + 16, 0, false);
            graphics.drawString(this.font, "§5Skin Actual: §0" + npcConfig.general.skin, contentX, contentY + 30, 0, false);
            graphics.drawString(this.font, "§5Campaña Requerida: §0Etapa " + npcConfig.requirements.requiredCampaignLevel, contentX, contentY + 44, 0, false);
        } else {
            graphics.drawString(this.font, "§0Configuración de Pestaña §l" + currentTab.getLabel(), contentX, contentY, 0, false);
            graphics.drawString(this.font, "§8Panel de parámetros dinámicos listo para edición.", contentX, contentY + 18, 0, false);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
