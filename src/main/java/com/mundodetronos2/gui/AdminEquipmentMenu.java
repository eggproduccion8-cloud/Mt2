package com.mundodetronos2.gui;

import com.mundodetronos2.init.ModMenuTypes;
import com.mundodetronos2.role.EquipmentManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class AdminEquipmentMenu extends AbstractContainerMenu {

    private final SimpleContainer container;
    private String currentRole;

    // Client-side constructor
    public AdminEquipmentMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, new SimpleContainer(80), "guerrero");
    }

    // Server-side constructor
    public AdminEquipmentMenu(int containerId, Inventory playerInventory, SimpleContainer container, String role) {
        super(ModMenuTypes.ADMIN_EQUIPMENT_MENU.get(), containerId);
        this.container = container;
        this.currentRole = role;

        // 1. Add 80 slots (10 columns x 8 rows) for the role equipment
        int startX = 8;
        int startY = 18;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 10; col++) {
                int index = row * 10 + col;
                this.addSlot(new Slot(container, index, startX + col * 18, startY + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        if (stack.isEmpty()) return true;
                        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                        // Check for duplicates in other slots
                        for (int i = 0; i < 80; i++) {
                            if (i != index) {
                                ItemStack existing = container.getItem(i);
                                if (!existing.isEmpty()) {
                                    String existingId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(existing.getItem()).toString();
                                    if (existingId.equalsIgnoreCase(itemId)) {
                                        if (playerInventory.player instanceof net.minecraft.server.level.ServerPlayer sp) {
                                            com.mundodetronos2.network.MessageManager.actionBar(sp, "§c⚠ Este objeto ya está permitido para este rol.");
                                        }
                                        return false;
                                    }
                                }
                            }
                        }
                        return true;
                    }

                    @Override
                    public void setChanged() {
                        super.setChanged();
                        if (playerInventory.player instanceof net.minecraft.server.level.ServerPlayer sp) {
                            EquipmentManager.updateSlotOnServer(currentRole, index, this.getItem());
                        }
                    }
                });
            }
        }

        // 2. Add player inventory (9 columns x 3 rows) - offset slightly for layout
        int playerInvY = 170;
        int inventoryStartX = 8 + 9; // 9px offset to center 9 slots (162px) under 10 slots (180px)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, inventoryStartX + col * 18, playerInvY + row * 18));
            }
        }

        // 3. Add player hotbar (9 slots)
        int hotbarY = 228;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, inventoryStartX + col * 18, hotbarY));
        }
    }

    public String getCurrentRole() {
        return currentRole;
    }

    public SimpleContainer getContainer() {
        return container;
    }

    public void switchRoleOnServer(String newRole) {
        this.currentRole = newRole;
        java.util.List<EquipmentManager.AllowedItemSlot> roleItems = EquipmentManager.getRoleSlots(newRole);
        for (int i = 0; i < 80; i++) {
            ItemStack stack = ItemStack.EMPTY;
            if (i < roleItems.size()) {
                stack = roleItems.get(i).toItemStack();
            }
            this.container.setItem(i, stack);
        }
        this.sendAllDataToRemote();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 80) {
                // Move from role slots to player inventory
                if (!this.moveItemStackTo(itemstack1, 80, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from player inventory to role slots
                String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(itemstack1.getItem()).toString();
                for (int i = 0; i < 80; i++) {
                    ItemStack existing = container.getItem(i);
                    if (!existing.isEmpty()) {
                        String existingId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(existing.getItem()).toString();
                        if (existingId.equalsIgnoreCase(itemId)) {
                            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                                com.mundodetronos2.network.MessageManager.actionBar(sp, "§c⚠ Este objeto ya está permitido para este rol.");
                            }
                            return ItemStack.EMPTY;
                        }
                    }
                }

                // Put copy of count=1 in first empty role slot
                boolean placed = false;
                for (int i = 0; i < 80; i++) {
                    Slot targetSlot = this.slots.get(i);
                    if (!targetSlot.hasItem()) {
                        ItemStack singleCopy = itemstack1.copy();
                        singleCopy.setCount(1);
                        targetSlot.setByPlayer(singleCopy);
                        itemstack1.shrink(1);
                        placed = true;
                        break;
                    }
                }
                if (!placed) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.hasPermissions(2);
    }
}
