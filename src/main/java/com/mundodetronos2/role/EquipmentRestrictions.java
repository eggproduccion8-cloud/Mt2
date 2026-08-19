package com.mundodetronos2.role;

import net.minecraft.world.item.*;

public class EquipmentRestrictions {

    public static boolean isRestrictedItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString().toLowerCase();
        String name = stack.getHoverName().getString().toLowerCase();

        // 1. Excepciones universales (Permitidos para TODOS los jugadores sin importar rol)
        if (item.isEdible()
            || id.contains("seed")
            || id.contains("crafting_table") || id.contains("chest") || id.contains("door")
            || id.contains("sapling") || id.contains("wheat") || id.contains("carrot") || id.contains("potato")
            || name.contains("mesa de crafteo") || name.contains("mesa de fabricación") || name.contains("cofre") || name.contains("puerta") || name.contains("semilla")) {
            return false;
        }

        // 2. Restringir equipamiento, armas, pociones, herramientas y consumibles especiales por defecto
        if (item instanceof SwordItem ||
            item instanceof ArmorItem ||
            item instanceof ShieldItem ||
            item instanceof BowItem ||
            item instanceof CrossbowItem ||
            item instanceof TridentItem ||
            item instanceof DiggerItem ||
            item instanceof PotionItem ||
            item instanceof ThrowablePotionItem) {
            return true;
        }

        // 3. Si el objeto está registrado en la lista de permisos de algún rol
        return EquipmentManager.isItemRegisteredInAnyRole(id);
    }

    public static boolean isItemAuthorized(ItemStack stack, String playerRole) {
        if (stack == null || stack.isEmpty()) return false;

        // El kit inicial o cualquier item con NBT 'any' siempre está permitido
        if (stack.hasTag()) {
            String authRole = stack.getTag().getString("AuthorizedRole");
            if (authRole.equalsIgnoreCase("any")) {
                return true;
            }
        }

        if (!isRestrictedItem(stack)) return true;

        // Si el jugador no tiene rol, no puede usar equipamiento restringido (salvo que sea 'any')
        if (playerRole == null || playerRole.equalsIgnoreCase("none") || playerRole.isEmpty()) {
            return false;
        }

        // Consultar el EquipmentManager para ver si el ID del objeto está registrado para este rol
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return EquipmentManager.isItemAllowed(playerRole, itemId);
    }
}
