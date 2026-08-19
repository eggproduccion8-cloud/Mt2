package com.mundodetronos2.backpack;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class BackpackManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, SimpleContainer> BACKPACKS = new HashMap<>();

    public static SimpleContainer getBackpack(Player player) {
        return getBackpack(player.getUUID());
    }

    public static SimpleContainer getBackpack(UUID playerId) {
        return BACKPACKS.computeIfAbsent(playerId, id -> {
            SimpleContainer container = new SimpleContainer(45);
            loadBackpack(id, container);
            return container;
        });
    }

    private static File getBackpackDir() {
        File dir;
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            dir = new File(server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile(), "mundo_de_tronos2/backpacks");
        } else {
            dir = new File("world/mundo_de_tronos2/backpacks");
        }
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static void saveBackpack(UUID playerId) {
        SimpleContainer container = BACKPACKS.get(playerId);
        if (container == null) return;

        try {
            File dir = getBackpackDir();
            File file = new File(dir, playerId.toString() + ".nbt");
            CompoundTag tag = new CompoundTag();
            ListTag itemsList = new ListTag();

            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (!stack.isEmpty()) {
                    CompoundTag itemTag = new CompoundTag();
                    itemTag.putByte("Slot", (byte) i);
                    stack.save(itemTag);
                    itemsList.add(itemTag);
                }
            }
            tag.put("Items", itemsList);

            net.minecraft.nbt.NbtIo.writeCompressed(tag, file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadBackpack(UUID playerId, SimpleContainer container) {
        try {
            File dir = getBackpackDir();
            File file = new File(dir, playerId.toString() + ".nbt");
            if (!file.exists()) return;

            CompoundTag tag = net.minecraft.nbt.NbtIo.readCompressed(file);
            if (tag.contains("Items")) {
                ListTag itemsList = tag.getList("Items", 10);
                for (int i = 0; i < itemsList.size(); i++) {
                    CompoundTag itemTag = itemsList.getCompound(i);
                    int slot = itemTag.getByte("Slot") & 255;
                    if (slot < container.getContainerSize()) {
                        container.setItem(slot, ItemStack.of(itemTag));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void onServerSave() {
        for (UUID id : BACKPACKS.keySet()) {
            saveBackpack(id);
        }
    }
}
