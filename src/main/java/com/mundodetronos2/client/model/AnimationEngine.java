package com.mundodetronos2.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AnimationEngine {

    public static class Keyframe {
        public float time;
        public float[] vector = new float[3]; // x, y, z
    }

    public static class BoneAnimationChannel {
        public final List<Keyframe> rotations = new ArrayList<>();
        public final List<Keyframe> positions = new ArrayList<>();
        public final List<Keyframe> scales = new ArrayList<>();
    }

    public static class AnimationData {
        public String name;
        public boolean loop;
        public float length;
        public final Map<String, BoneAnimationChannel> boneChannels = new HashMap<>();
    }

    public static class AnimationSet {
        public final Map<String, AnimationData> animations = new HashMap<>();
    }

    public static AnimationSet parseAnimation(InputStream inputStream) {
        AnimationSet set = new AnimationSet();
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has("animations") && root.get("animations").isJsonObject()) {
                JsonObject anims = root.getAsJsonObject("animations");
                for (Map.Entry<String, JsonElement> entry : anims.entrySet()) {
                    String animName = entry.getKey();
                    JsonObject animObj = entry.getValue().getAsJsonObject();

                    AnimationData anim = new AnimationData();
                    anim.name = animName;
                    if (animObj.has("loop")) {
                        JsonElement loopElem = animObj.get("loop");
                        if (loopElem.isJsonPrimitive() && loopElem.getAsJsonPrimitive().isBoolean()) {
                            anim.loop = loopElem.getAsBoolean();
                        } else if (loopElem.isJsonPrimitive() && loopElem.getAsJsonPrimitive().isString()) {
                            anim.loop = "true".equalsIgnoreCase(loopElem.getAsString());
                        }
                    }
                    if (animObj.has("animation_length")) {
                        anim.length = animObj.get("animation_length").getAsFloat();
                    }

                    if (animObj.has("bones") && animObj.get("bones").isJsonObject()) {
                        JsonObject bones = animObj.getAsJsonObject("bones");
                        for (Map.Entry<String, JsonElement> boneEntry : bones.entrySet()) {
                            String boneName = boneEntry.getKey();
                            JsonObject boneObj = boneEntry.getValue().getAsJsonObject();

                            BoneAnimationChannel channel = new BoneAnimationChannel();
                            parseChannel(boneObj, "rotation", channel.rotations);
                            parseChannel(boneObj, "position", channel.positions);
                            parseChannel(boneObj, "scale", channel.scales);

                            anim.boneChannels.put(boneName, channel);
                        }
                    }

                    set.animations.put(animName, anim);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return set;
    }

    private static void parseChannel(JsonObject boneObj, String channelName, List<Keyframe> targetList) {
        if (!boneObj.has(channelName)) return;
        JsonElement elem = boneObj.get(channelName);

        if (elem.isJsonArray()) {
            Keyframe kf = new Keyframe();
            kf.time = 0f;
            kf.vector = parseVector(elem.getAsJsonArray());
            targetList.add(kf);
        } else if (elem.isJsonObject()) {
            JsonObject obj = elem.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String key = entry.getKey();
                float time = 0f;
                try {
                    time = Float.parseFloat(key);
                } catch (NumberFormatException ignored) {}

                Keyframe kf = new Keyframe();
                kf.time = time;

                JsonElement val = entry.getValue();
                if (val.isJsonArray()) {
                    kf.vector = parseVector(val.getAsJsonArray());
                } else if (val.isJsonObject()) {
                    JsonObject kfObj = val.getAsJsonObject();
                    if (kfObj.has("post")) {
                        kf.vector = parseVector(kfObj.get("post"));
                    } else if (kfObj.has("pre")) {
                        kf.vector = parseVector(kfObj.get("pre"));
                    }
                }
                targetList.add(kf);
            }
            targetList.sort(Comparator.comparingDouble(k -> k.time));
        }
    }

    private static float[] parseVector(JsonElement elem) {
        float[] res = new float[3];
        if (elem.isJsonArray()) {
            JsonArray arr = elem.getAsJsonArray();
            if (arr.size() >= 3) {
                res[0] = parseFlexibleFloat(arr.get(0));
                res[1] = parseFlexibleFloat(arr.get(1));
                res[2] = parseFlexibleFloat(arr.get(2));
            }
        }
        return res;
    }

    private static float parseFlexibleFloat(JsonElement elem) {
        if (elem.isJsonPrimitive()) {
            if (elem.getAsJsonPrimitive().isNumber()) {
                return elem.getAsFloat();
            } else if (elem.getAsJsonPrimitive().isString()) {
                try {
                    return Float.parseFloat(elem.getAsString());
                } catch (NumberFormatException ignored) {}
            }
        }
        return 0f;
    }

    public static float[] interpolate(List<Keyframe> keyframes, float animTime, float maxTime, boolean loop, float[] defaultVal) {
        if (keyframes.isEmpty()) return defaultVal;
        if (keyframes.size() == 1) return keyframes.get(0).vector;

        float maxKeyframeTime = keyframes.get(keyframes.size() - 1).time;
        float effectiveMaxTime = maxTime > 0 ? maxTime : maxKeyframeTime;

        if (loop && effectiveMaxTime > 0) {
            animTime = animTime % effectiveMaxTime;
            if (animTime < 0) animTime += effectiveMaxTime;
        }

        if (animTime <= keyframes.get(0).time) {
            return keyframes.get(0).vector;
        }
        if (animTime >= keyframes.get(keyframes.size() - 1).time) {
            return keyframes.get(keyframes.size() - 1).vector;
        }

        for (int i = 0; i < keyframes.size() - 1; i++) {
            Keyframe k1 = keyframes.get(i);
            Keyframe k2 = keyframes.get(i + 1);
            if (animTime >= k1.time && animTime <= k2.time) {
                float dt = k2.time - k1.time;
                if (Math.abs(dt) < 0.0001f) {
                    return k1.vector;
                }
                float factor = (animTime - k1.time) / dt;
                float[] res = new float[3];
                res[0] = k1.vector[0] + factor * (k2.vector[0] - k1.vector[0]);
                res[1] = k1.vector[1] + factor * (k2.vector[1] - k1.vector[1]);
                res[2] = k1.vector[2] + factor * (k2.vector[2] - k1.vector[2]);
                return res;
            }
        }

        return defaultVal;
    }
}
