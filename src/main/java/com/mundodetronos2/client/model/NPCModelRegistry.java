package com.mundodetronos2.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.*;

public class NPCModelRegistry {
    private static final Logger LOGGER = LogManager.getLogger();

    public static class ModelEntry {
        public final String id;
        public BlockbenchModel model;
        public AnimationEngine.AnimationSet animationSet;
        public final Set<String> availableAnimations = new HashSet<>();
        public final Set<String> textures = new HashSet<>();

        public ModelEntry(String id) {
            this.id = id;
        }
    }

    private static final Map<String, ModelEntry> REGISTRY = new HashMap<>();

    public static void initClient() {
        REGISTRY.clear();
        ResourceManager rm = Minecraft.getInstance().getResourceManager();

        Map<ResourceLocation, Resource> modelResources = rm.listResources("models/blockbench", loc -> loc.getPath().endsWith(".bbmodel"));

        for (Map.Entry<ResourceLocation, Resource> entryRes : modelResources.entrySet()) {
            ResourceLocation modelLoc = entryRes.getKey();
            String path = modelLoc.getPath();
            String id = path.substring(path.lastIndexOf('/') + 1, path.length() - 8).toLowerCase(Locale.ROOT);

            ModelEntry entry = new ModelEntry(id);

            try (InputStream is = entryRes.getValue().open()) {
                entry.model = BlockbenchModel.parse(is);
                LOGGER.info("Loaded dynamic Blockbench model for NPC: {}", id);
            } catch (Exception e) {
                LOGGER.error("Failed to load model asset: {}", modelLoc, e);
                continue;
            }

            // Dedicated or fallback animation JSON file
            String animId = id.startsWith("guard") ? "guard" : id;
            ResourceLocation animLoc = new ResourceLocation("mundodetronos2", "animations/" + animId + ".animation.json");
            Optional<Resource> animRes = rm.getResource(animLoc);
            if (animRes.isPresent()) {
                try (InputStream is = animRes.get().open()) {
                    entry.animationSet = AnimationEngine.parseAnimation(is);
                    entry.availableAnimations.addAll(entry.animationSet.animations.keySet());
                    LOGGER.info("Loaded animations for NPC {}: {}", id, entry.availableAnimations);
                } catch (Exception e) {
                    LOGGER.error("Failed to load animation asset: {}", animLoc, e);
                }
            }

            // Mapped or default texture resolution
            String texName = id;
            if (id.equals("guardparts")) texName = "guard";
            else if (id.equals("npcgreeting")) texName = "helloeng";

            ResourceLocation texLoc = new ResourceLocation("mundodetronos2", "textures/entity/" + texName + ".png");
            if (rm.getResource(texLoc).isPresent()) {
                entry.textures.add(texName);
            } else {
                LOGGER.warn("NPC TEXTURE MISSING for model '{}' at location: {}", id, texLoc);
            }

            if (entry.model != null) {
                REGISTRY.put(id, entry);
            }
        }
    }

    public static ModelEntry get(String id) {
        if (id == null) return null;
        return REGISTRY.get(id.toLowerCase(Locale.ROOT));
    }

    public static Collection<String> getRegisteredModelIds() {
        return REGISTRY.keySet();
    }

    public static Map<String, ModelEntry> getAll() {
        return Collections.unmodifiableMap(REGISTRY);
    }
}
