package com.mundodetronos2.gui;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.Optional;

public class RPGInventoryMenu extends AbstractContainerMenu {

    public interface IResizableSlot {
        void setPos(int x, int y);
    }

    private static java.lang.reflect.Field xField = null;
    private static java.lang.reflect.Field yField = null;

    static {
        try {
            int intCount = 0;
            for (java.lang.reflect.Field f : Slot.class.getDeclaredFields()) {
                if (f.getType() == int.class) {
                    f.setAccessible(true);
                    if (f.getName().equals("x") || f.getName().equals("f_40220_")) {
                        xField = f;
                    } else if (f.getName().equals("y") || f.getName().equals("f_40221_")) {
                        yField = f;
                    } else {
                        if (intCount == 1 && xField == null) xField = f;
                        if (intCount == 2 && yField == null) yField = f;
                    }
                    intCount++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class ResizableSlot extends Slot implements IResizableSlot {
        public int posX;
        public int posY;
        public boolean activeSlot = true;

        public ResizableSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
            this.posX = x;
            this.posY = y;
        }

        @Override
        public void setPos(int x, int y) {
            this.posX = x;
            this.posY = y;
            try {
                if (xField != null) xField.setInt(this, x);
                if (yField != null) yField.setInt(this, y);
            } catch (Exception ignored) {}
        }

        public void setActive(boolean active) {
            this.activeSlot = active;
        }

        @Override
        public boolean isActive() {
            return this.activeSlot;
        }
    }

    public static class ResizableResultSlot extends ResultSlot implements IResizableSlot {
        public int posX;
        public int posY;
        public boolean activeSlot = true;

        public ResizableResultSlot(Player player, CraftingContainer craftContainer, Container resultContainer, int slot, int x, int y) {
            super(player, craftContainer, resultContainer, slot, x, y);
            this.posX = x;
            this.posY = y;
        }

        @Override
        public void setPos(int x, int y) {
            this.posX = x;
            this.posY = y;
            try {
                if (xField != null) xField.setInt(this, x);
                if (yField != null) yField.setInt(this, y);
            } catch (Exception ignored) {}
        }

        public void setActive(boolean active) {
            this.activeSlot = active;
        }

        @Override
        public boolean isActive() {
            return this.activeSlot;
        }
    }

    private final CraftingContainer craftSlots;
    private final ResultContainer resultSlots = new ResultContainer();
    private final Container backpackContainer;
    private final Player player;
    private final int backpackTier; // 1 = 15 slots, 2 = 30 slots, 3 = 45 slots

    public RPGInventoryMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, 1);
    }

    public RPGInventoryMenu(int containerId, Inventory playerInventory, int tier) {
        super(com.mundodetronos2.init.ModMenuTypes.RPG_INVENTORY_MENU.get(), containerId);
        this.player = playerInventory.player;
        this.craftSlots = new TransientCraftingContainer(this, 3, 3);

        if (!this.player.level().isClientSide) {
            this.backpackContainer = com.mundodetronos2.backpack.BackpackManager.getBackpack(this.player);
            com.mundodetronos2.progression.PlayerProgressData pData = com.mundodetronos2.progression.ProgressionManager.getProgressData(this.player.getUUID());
            this.backpackTier = Math.max(1, Math.min(3, pData != null ? pData.getBackpackTier() : 1));
        } else {
            this.backpackContainer = new SimpleContainer(45);
            this.backpackTier = Math.max(1, Math.min(3, tier));
        }

        // 1. SLOTS DE ARMADURA (0..3)
        EquipmentSlot[] armorSlots = new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
        for (int i = 0; i < 4; i++) {
            final EquipmentSlot slotType = armorSlots[i];
            this.addSlot(new ResizableSlot(playerInventory, 39 - i, 8, 8 + i * 18) {
                @Override
                public int getMaxStackSize() { return 1; }
                @Override
                public boolean mayPlace(ItemStack stack) { return stack.canEquip(slotType, player); }
            });
        }

        // 2. SLOT DE SEGUNDA MANO (4)
        this.addSlot(new ResizableSlot(playerInventory, 40, 77, 62) {
            @Override
            public boolean mayPlace(ItemStack stack) { return true; }
        });

        // 3. SLOTS DE CRAFTEO 3x3 (5 RESULTADO, 6..14 GRILLA)
        this.addSlot(new ResizableResultSlot(playerInventory.player, this.craftSlots, this.resultSlots, 0, 154, 35));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new ResizableSlot(this.craftSlots, col + row * 3, 88 + col * 18, 17 + row * 18));
            }
        }

        // 4. SLOTS DE MOCHILA 45 SLOTS (15..59) -> Tier 1: 15, Tier 2: 30, Tier 3: 45
        int maxAllowedSlots = this.backpackTier * 15;
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 9; col++) {
                final int slotIdx = col + row * 9;
                this.addSlot(new ResizableSlot(this.backpackContainer, slotIdx, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return slotIdx < maxAllowedSlots;
                    }
                });
            }
        }

        // 5. SLOTS DE INVENTARIO PRINCIPAL (60..86)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new ResizableSlot(playerInventory, col + (row + 1) * 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // 6. SLOTS DE HOTBAR (87..95) - Posicionados funcionalmente en la grilla del inventario sin duplicar hotbar separada
        for (int col = 0; col < 9; col++) {
            this.addSlot(new ResizableSlot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public void setSlotState(int index, int x, int y, boolean active) {
        if (index >= 0 && index < this.slots.size()) {
            Slot slot = this.slots.get(index);
            if (slot instanceof IResizableSlot resizableSlot) {
                resizableSlot.setPos(x, y);
            }
            if (slot instanceof ResizableSlot rs) {
                rs.setActive(active);
            } else if (slot instanceof ResizableResultSlot rrs) {
                rrs.setActive(active);
            }
        }
    }

    @Override
    public void slotsChanged(Container container) {
        if (!this.player.level().isClientSide) {
            Optional<CraftingRecipe> recipe = this.player.level().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, this.craftSlots, this.player.level());
            if (recipe.isPresent()) {
                this.resultSlots.setItem(0, recipe.get().assemble(this.craftSlots, this.player.level().registryAccess()));
            } else {
                this.resultSlots.setItem(0, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.clearContainer(player, this.craftSlots);
        if (!player.level().isClientSide) {
            com.mundodetronos2.backpack.BackpackManager.saveBackpack(player.getUUID());
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem() && slot.isActive()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            if (index == 5) { // Crafting Result Slot
                if (!this.moveItemStackTo(stackInSlot, 60, 96, true)) {
                    if (!this.moveItemStackTo(stackInSlot, 15, 60, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                slot.onQuickCraft(stackInSlot, itemstack);
                slot.onTake(player, stackInSlot);
            } else if (index < 60) { // De equipamiento (0..4), crafteo (6..14) o mochila (15..59) hacia inventario / hotbar
                if (!this.moveItemStackTo(stackInSlot, 60, 96, true)) {
                    return ItemStack.EMPTY;
                }
            } else { // Del inventario (60..86) o hotbar (87..95)
                boolean moved = false;

                // 1. Intentar equipar en ranuras de Armadura (0..3)
                EquipmentSlot slotType = Mob.getEquipmentSlotForItem(stackInSlot);
                if (slotType.getType() == EquipmentSlot.Type.ARMOR) {
                    int armorSlotIndex = 39 - slotType.getIndex(); // 0: Head, 1: Chest, 2: Legs, 3: Feet
                    Slot armorSlot = this.slots.get(armorSlotIndex);
                    if (armorSlot.isActive() && !armorSlot.hasItem() && armorSlot.mayPlace(stackInSlot)) {
                        if (this.moveItemStackTo(stackInSlot, armorSlotIndex, armorSlotIndex + 1, false)) {
                            moved = true;
                        }
                    }
                }

                // 2. Intentar equipar en Escudo / Offhand (4)
                if (!moved && (stackInSlot.getItem() == Items.SHIELD || slotType == EquipmentSlot.OFFHAND)) {
                    Slot offhandSlot = this.slots.get(4);
                    if (offhandSlot.isActive() && !offhandSlot.hasItem() && offhandSlot.mayPlace(stackInSlot)) {
                        if (this.moveItemStackTo(stackInSlot, 4, 5, false)) {
                            moved = true;
                        }
                    }
                }

                // 3. Intentar mover a la mochila activa (15..maxBackpack)
                if (!moved) {
                    int maxBackpackIndex = 15 + (this.backpackTier * 15);
                    Slot firstBackpackSlot = this.slots.get(15);
                    if (firstBackpackSlot.isActive()) {
                        if (this.moveItemStackTo(stackInSlot, 15, maxBackpackIndex, false)) {
                            moved = true;
                        }
                    }
                }

                // 4. Mover entre Inventario Principal <-> Hotbar
                if (!moved) {
                    if (index >= 60 && index < 87) {
                        if (!this.moveItemStackTo(stackInSlot, 87, 96, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (index >= 87 && index < 96) {
                        if (!this.moveItemStackTo(stackInSlot, 60, 87, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stackInSlot);
        }
        return itemstack;
    }

    public int getBackpackTier() {
        return this.backpackTier;
    }
}
