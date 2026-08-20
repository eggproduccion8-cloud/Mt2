package com.mundodetronos2.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BlockbenchModel {
    public int texWidth = 64;
    public int texHeight = 64;
    public final List<BoneGroup> rootBones = new ArrayList<>();
    public final Map<String, BoneGroup> boneByName = new HashMap<>();

    public static class CubeFace {
        public float u1, v1, u2, v2;
        public int rotation; // 0, 90, 180, 270

        public CubeFace(float u1, float v1, float u2, float v2, int rotation) {
            this.u1 = u1;
            this.v1 = v1;
            this.u2 = u2;
            this.v2 = v2;
            this.rotation = rotation;
        }
    }

    public static class Cube {
        public float minX, minY, minZ;
        public float maxX, maxY, maxZ;
        public float originX, originY, originZ;
        public float rotX, rotY, rotZ;
        public boolean visible = true;
        public final Map<String, CubeFace> faces = new HashMap<>();
    }

    public static class BoneGroup {
        public String name;
        public String uuid;
        public float pivotX, pivotY, pivotZ;
        public float rotX, rotY, rotZ;
        public final List<Cube> cubes = new ArrayList<>();
        public final List<BoneGroup> children = new ArrayList<>();
        public BoneGroup parent;
    }

    public static BlockbenchModel parse(InputStream inputStream) {
        BlockbenchModel model = new BlockbenchModel();
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            if (root.has("resolution") && root.get("resolution").isJsonObject()) {
                JsonObject res = root.getAsJsonObject("resolution");
                if (res.has("width")) model.texWidth = res.get("width").getAsInt();
                if (res.has("height")) model.texHeight = res.get("height").getAsInt();
            }

            Map<String, Cube> cubeByUuid = new HashMap<>();
            if (root.has("elements") && root.get("elements").isJsonArray()) {
                JsonArray elements = root.getAsJsonArray("elements");
                for (JsonElement elem : elements) {
                    if (!elem.isJsonObject()) continue;
                    JsonObject eObj = elem.getAsJsonObject();

                    Cube cube = new Cube();
                    String uuid = eObj.has("uuid") ? eObj.get("uuid").getAsString() : "";

                    if (eObj.has("visibility")) {
                        cube.visible = eObj.get("visibility").getAsBoolean();
                    }

                    // Skip invisible elements (hitboxes / bounding volumes)
                    if (!cube.visible) {
                        continue;
                    }

                    if (eObj.has("from")) {
                        JsonArray f = eObj.getAsJsonArray("from");
                        cube.minX = f.get(0).getAsFloat();
                        cube.minY = f.get(1).getAsFloat();
                        cube.minZ = f.get(2).getAsFloat();
                    }
                    if (eObj.has("to")) {
                        JsonArray t = eObj.getAsJsonArray("to");
                        cube.maxX = t.get(0).getAsFloat();
                        cube.maxY = t.get(1).getAsFloat();
                        cube.maxZ = t.get(2).getAsFloat();
                    }
                    if (eObj.has("origin")) {
                        JsonArray o = eObj.getAsJsonArray("origin");
                        cube.originX = o.get(0).getAsFloat();
                        cube.originY = o.get(1).getAsFloat();
                        cube.originZ = o.get(2).getAsFloat();
                    }
                    if (eObj.has("rotation")) {
                        JsonArray r = eObj.getAsJsonArray("rotation");
                        cube.rotX = r.get(0).getAsFloat();
                        cube.rotY = r.get(1).getAsFloat();
                        cube.rotZ = r.get(2).getAsFloat();
                    }

                    if (eObj.has("faces") && eObj.get("faces").isJsonObject()) {
                        JsonObject facesObj = eObj.getAsJsonObject("faces");
                        for (Map.Entry<String, JsonElement> entry : facesObj.entrySet()) {
                            JsonObject fObj = entry.getValue().getAsJsonObject();
                            JsonArray uv = fObj.getAsJsonArray("uv");
                            float u1 = uv.get(0).getAsFloat();
                            float v1 = uv.get(1).getAsFloat();
                            float u2 = uv.get(2).getAsFloat();
                            float v2 = uv.get(3).getAsFloat();
                            int rot = fObj.has("rotation") ? fObj.get("rotation").getAsInt() : 0;
                            cube.faces.put(entry.getKey(), new CubeFace(u1, v1, u2, v2, rot));
                        }
                    }

                    if (!uuid.isEmpty()) {
                        cubeByUuid.put(uuid, cube);
                    }
                }
            }

            if (root.has("outliner") && root.get("outliner").isJsonArray()) {
                JsonArray outliner = root.getAsJsonArray("outliner");
                for (JsonElement item : outliner) {
                    parseOutlinerNode(item, null, model, cubeByUuid);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return model;
    }

    private static void parseOutlinerNode(JsonElement element, BoneGroup parent, BlockbenchModel model, Map<String, Cube> cubeByUuid) {
        if (element.isJsonPrimitive()) {
            // It's a cube UUID attached directly
            String cubeUuid = element.getAsString();
            Cube cube = cubeByUuid.get(cubeUuid);
            if (cube != null && parent != null) {
                parent.cubes.add(cube);
            }
        } else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();

            // Check bone visibility if present
            if (obj.has("visibility") && !obj.get("visibility").getAsBoolean()) {
                return;
            }

            BoneGroup bone = new BoneGroup();
            bone.name = obj.has("name") ? obj.get("name").getAsString() : "unnamed";
            bone.uuid = obj.has("uuid") ? obj.get("uuid").getAsString() : "";
            bone.parent = parent;

            if (obj.has("origin")) {
                JsonArray o = obj.getAsJsonArray("origin");
                bone.pivotX = o.get(0).getAsFloat();
                bone.pivotY = o.get(1).getAsFloat();
                bone.pivotZ = o.get(2).getAsFloat();
            }
            if (obj.has("rotation")) {
                JsonArray r = obj.getAsJsonArray("rotation");
                bone.rotX = r.get(0).getAsFloat();
                bone.rotY = r.get(1).getAsFloat();
                bone.rotZ = r.get(2).getAsFloat();
            }

            if (parent == null) {
                model.rootBones.add(bone);
            } else {
                parent.children.add(bone);
            }
            model.boneByName.put(bone.name, bone);

            if (obj.has("children") && obj.get("children").isJsonArray()) {
                JsonArray children = obj.getAsJsonArray("children");
                for (JsonElement child : children) {
                    parseOutlinerNode(child, bone, model, cubeByUuid);
                }
            }
        }
    }
}
