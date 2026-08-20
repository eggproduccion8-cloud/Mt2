package com.mundodetronos2.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.*;

public class BlockModelRegistry {
    private static final Logger LOGGER = LogManager.getLogger();

    public static class Entry {
        public final String id;
        public BlockbenchModel model;
        public ResourceLocation textureLocation;

        public Entry(String id, ResourceLocation textureLocation) {
            this.id = id;
            this.textureLocation = textureLocation;
        }
    }

    private static final Map<String, Entry> REGISTRY = new HashMap<>();

    public static void initClient() {
        REGISTRY.clear();
        ResourceManager rm = Minecraft.getInstance().getResourceManager();

        registerBlockModel(rm, "sand_castle", "models/block/sand_castle.json", "textures/block/sand_castle.png");
        registerBlockModel(rm, "sand_castle_parts", "models/block/sand_castle_parts.json", "textures/block/sand_castle_parts.png");
        registerBlockModel(rm, "lvl1_crate", "models/block/lvl1_crate.json", "textures/block/lvl1_crate.png");
        registerBlockModel(rm, "lvl2_crate", "models/block/lvl2_crate.json", "textures/block/lvl2_crate.png");
        registerBlockModel(rm, "lvl3_crate", "models/block/lvl3_crate.json", "textures/block/lvl3_crate.png");
        registerBlockModel(rm, "lvl4_crate", "models/block/lvl4_crate.json", "textures/block/lvl4_crate.png");
        registerBlockModel(rm, "230426_bomb", "models/item/230426_bomb.json", "textures/item/230426_bomb.png");
    }

    private static void registerBlockModel(ResourceManager rm, String id, String modelPath, String texturePath) {
        ResourceLocation texLoc = new ResourceLocation("mundodetronos2", texturePath);
        Entry entry = new Entry(id, texLoc);

        ResourceLocation modelLoc = new ResourceLocation("mundodetronos2", modelPath);
        Optional<Resource> modelRes = rm.getResource(modelLoc);
        if (modelRes.isPresent()) {
            try (InputStream is = modelRes.get().open()) {
                entry.model = BlockbenchModel.parse(is);
                LOGGER.info("Loaded 3D block model: {} from {}", id, modelPath);
            } catch (Exception e) {
                LOGGER.error("Failed to load 3D block model: {}", modelLoc, e);
            }
        } else {
            LOGGER.warn("Resource missing for 3D block model: {}", modelLoc);
        }

        if (entry.model != null) {
            REGISTRY.put(id.toLowerCase(Locale.ROOT), entry);
        }
    }

    public static Entry get(String id) {
        if (id == null) return null;
        return REGISTRY.get(id.toLowerCase(Locale.ROOT));
    }
}
