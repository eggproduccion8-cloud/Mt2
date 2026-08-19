package com.mundodetronos2.role;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;

public class EquipmentManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = new File("world/mundo_de_tronos2/equipment_permissions.json");

    private static final Map<String, List<AllowedItemSlot>> roleSlots = new HashMap<>();

    public static class AllowedItemSlot {
        public String id = "";
        public String nbt = "";

        public AllowedItemSlot() {}

        public AllowedItemSlot(String id, String nbt) {
            this.id = id;
            this.nbt = nbt;
        }

        public ItemStack toItemStack() {
            if (id == null || id.isEmpty()) return ItemStack.EMPTY;
            try {
                net.minecraft.resources.ResourceLocation rl = new net.minecraft.resources.ResourceLocation(id);
                net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(rl);
                if (item == net.minecraft.world.item.Items.AIR) return ItemStack.EMPTY;
                ItemStack stack = new ItemStack(item);
                if (nbt != null && !nbt.isEmpty()) {
                    stack.setTag(net.minecraft.nbt.TagParser.parseTag(nbt));
                }
                return stack;
            } catch (Exception e) {
                return ItemStack.EMPTY;
            }
        }

        public static AllowedItemSlot fromItemStack(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return new AllowedItemSlot();
            String idStr = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            String nbtStr = stack.hasTag() ? stack.getTag().toString() : "";
            return new AllowedItemSlot(idStr, nbtStr);
        }
    }

    public static String getNormalizedRole(String role) {
        if (role == null) return "none";
        String r = role.toLowerCase().trim();
        if (r.equals("warrior")) return "guerrero";
        if (r.equals("mage")) return "mago";
        if (r.equals("archer")) return "arquero";
        return r;
    }

    public static final int TOTAL_SLOTS = 200;

    public static synchronized void init() {
        roleSlots.clear();
        if (!FILE.exists()) {
            setupDefaults();
            save();
            return;
        }

        try (FileReader reader = new FileReader(FILE)) {
            @SuppressWarnings("unchecked")
            Map<String, List<Map<String, String>>> loaded = GSON.fromJson(reader, Map.class);
            if (loaded != null) {
                for (String rName : new String[]{"berserker", "guerrero", "mago", "arquero", "paladin", "draconico", "clerigo"}) {
                    List<AllowedItemSlot> slots = new ArrayList<>(TOTAL_SLOTS);
                    for (int i = 0; i < TOTAL_SLOTS; i++) {
                        slots.add(new AllowedItemSlot());
                    }
                    roleSlots.put(rName, slots);
                }

                for (Map.Entry<String, List<Map<String, String>>> entry : loaded.entrySet()) {
                    String normRole = getNormalizedRole(entry.getKey());
                    List<AllowedItemSlot> targetSlots = roleSlots.get(normRole);
                    if (targetSlots != null) {
                        List<Map<String, String>> loadedSlots = entry.getValue();
                        for (int i = 0; i < TOTAL_SLOTS && i < loadedSlots.size(); i++) {
                            Map<String, String> sMap = loadedSlots.get(i);
                            if (sMap != null) {
                                AllowedItemSlot slot = targetSlots.get(i);
                                slot.id = sMap.getOrDefault("id", "");
                                slot.nbt = sMap.getOrDefault("nbt", "");
                            }
                        }
                    }
                }
                LOGGER.info("Permisos de equipamiento (" + TOTAL_SLOTS + " casillas) cargados con éxito.");
            } else {
                setupDefaults();
                save();
            }
        } catch (Exception e) {
            LOGGER.error("Error al cargar equipment_permissions.json, restaurando valores por defecto.", e);
            setupDefaults();
            save();
        }
    }

    private static void setupDefaults() {
        for (String rName : new String[]{"berserker", "guerrero", "mago", "arquero", "paladin", "draconico", "clerigo"}) {
            List<AllowedItemSlot> slots = new ArrayList<>(TOTAL_SLOTS);
            for (int i = 0; i < TOTAL_SLOTS; i++) {
                slots.add(new AllowedItemSlot());
            }
            roleSlots.put(rName, slots);
        }

        // Guerrero defaults
        addDefaultItem("guerrero", 0, "minecraft:iron_sword");
        addDefaultItem("guerrero", 1, "minecraft:diamond_sword");
        addDefaultItem("guerrero", 2, "minecraft:shield");
        addDefaultItem("guerrero", 3, "minecraft:iron_helmet");
        addDefaultItem("guerrero", 4, "minecraft:iron_chestplate");
        addDefaultItem("guerrero", 5, "minecraft:iron_leggings");
        addDefaultItem("guerrero", 6, "minecraft:iron_boots");
        addDefaultItem("guerrero", 7, "minecraft:diamond_helmet");
        addDefaultItem("guerrero", 8, "minecraft:diamond_chestplate");
        addDefaultItem("guerrero", 9, "minecraft:diamond_leggings");
        addDefaultItem("guerrero", 10, "minecraft:diamond_boots");

        // Berserker defaults
        addDefaultItem("berserker", 0, "minecraft:iron_axe");
        addDefaultItem("berserker", 1, "minecraft:diamond_axe");
        addDefaultItem("berserker", 2, "minecraft:netherite_axe");
        addDefaultItem("berserker", 3, "minecraft:leather_helmet");
        addDefaultItem("berserker", 4, "minecraft:leather_chestplate");
        addDefaultItem("berserker", 5, "minecraft:leather_leggings");
        addDefaultItem("berserker", 6, "minecraft:leather_boots");

        // Mago defaults
        addDefaultItem("mago", 0, "minecraft:wooden_sword");
        addDefaultItem("mago", 1, "minecraft:golden_sword");
        addDefaultItem("mago", 2, "minecraft:bow");
        addDefaultItem("mago", 3, "minecraft:crossbow");
    }

    private static void addDefaultItem(String role, int index, String id) {
        List<AllowedItemSlot> slots = roleSlots.get(role);
        if (slots != null && index >= 0 && index < TOTAL_SLOTS) {
            AllowedItemSlot slot = slots.get(index);
            slot.id = id;
            slot.nbt = "";
        }
    }

    public static synchronized void save() {
        try {
            if (!FILE.getParentFile().exists()) {
                FILE.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(FILE)) {
                GSON.toJson(roleSlots, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Error al guardar equipment_permissions.json", e);
        }
    }

    public static synchronized List<AllowedItemSlot> getRoleSlots(String role) {
        String norm = getNormalizedRole(role);
        List<AllowedItemSlot> slots = roleSlots.get(norm);
        if (slots == null) {
            slots = new ArrayList<>(TOTAL_SLOTS);
            for (int i = 0; i < TOTAL_SLOTS; i++) {
                slots.add(new AllowedItemSlot());
            }
            roleSlots.put(norm, slots);
        }
        return slots;
    }

    public static synchronized void updateSlotOnServer(String role, int slotIndex, ItemStack stack) {
        String norm = getNormalizedRole(role);
        List<AllowedItemSlot> slots = getRoleSlots(norm);
        if (slotIndex >= 0 && slotIndex < TOTAL_SLOTS) {
            slots.set(slotIndex, AllowedItemSlot.fromItemStack(stack));
            save();
        }
    }

    public static synchronized boolean isItemAllowed(String role, String itemRegistryName) {
        if (role == null || itemRegistryName == null) return false;
        String norm = getNormalizedRole(role);
        List<AllowedItemSlot> slots = roleSlots.get(norm);
        if (slots == null) return false;
        String searchId = itemRegistryName.toLowerCase();
        for (AllowedItemSlot slot : slots) {
            if (slot.id != null && slot.id.equalsIgnoreCase(searchId)) {
                return true;
            }
        }
        return false;
    }

    public static synchronized boolean isItemRegisteredInAnyRole(String itemId) {
        if (itemId == null) return false;
        String searchId = itemId.toLowerCase();
        for (List<AllowedItemSlot> slots : roleSlots.values()) {
            for (AllowedItemSlot slot : slots) {
                if (slot.id != null && slot.id.equalsIgnoreCase(searchId)) {
                    return true;
                }
            }
        }
        return false;
    }
}
