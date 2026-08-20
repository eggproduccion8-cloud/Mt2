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

        String[] knownModels = {
            "adventurer", "anchor", "archer", "blacksmith", "butcher", "farmer", "guard",
            "guardcyan", "guardgreen", "guardorange", "guardparts", "guardpink",
            "guardpurple", "guardred", "guardyellow", "king", "lootbag", "minecart",
            "miner", "npcgreeting", "pirate", "tap", "tavern", "throne", "wizard"
        };

        for (String id : knownModels) {
            ModelEntry entry = new ModelEntry(id);

            // Load .bbmodel
            ResourceLocation modelLoc = new ResourceLocation("mundodetronos2", "models/blockbench/" + id + ".bbmodel");
            Optional<Resource> modelRes = rm.getResource(modelLoc);
            if (modelRes.isPresent()) {
                try (InputStream is = modelRes.get().open()) {
                    entry.model = BlockbenchModel.parse(is);
                    LOGGER.info("Loaded custom Blockbench model for NPC: {}", id);
                } catch (Exception e) {
                    LOGGER.error("Failed to load model asset: {}", modelLoc, e);
                }
            }

            // Load .animation.json (e.g. guard.animation.json)
            // Note: guard color variants (guardred, guardcyan, etc.) share guard.animation.json if guardred.animation.json does not exist.
            String animId = id.startsWith("guard") && !id.equals("guardparts") ? "guard" : id;
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

            // Default texture location
            ResourceLocation texLoc = new ResourceLocation("mundodetronos2", "textures/entity/" + id + ".png");
            if (rm.getResource(texLoc).isPresent()) {
                entry.textures.add(id);
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
