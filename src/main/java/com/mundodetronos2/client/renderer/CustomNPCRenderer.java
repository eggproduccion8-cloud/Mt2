package com.mundodetronos2.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mundodetronos2.client.model.AnimationEngine;
import com.mundodetronos2.client.model.BlockbenchModel;
import com.mundodetronos2.client.model.NPCModelRegistry;
import com.mundodetronos2.entity.CustomNPCEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class CustomNPCRenderer<T extends CustomNPCEntity> extends EntityRenderer<T> {

    public CustomNPCRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        String tex = entity.getNpcTexture();
        if (tex == null || tex.isEmpty()) {
            tex = entity.getNpcModel();
        }
        return new ResourceLocation("mundodetronos2", "textures/entity/" + tex + ".png");
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        String modelId = entity.getNpcModel();
        NPCModelRegistry.ModelEntry entry = NPCModelRegistry.get(modelId);
        if (entry == null || entry.model == null) {
            entry = NPCModelRegistry.get("guard");
        }

        if (entry == null || entry.model == null) return;

        poseStack.pushPose();

        // Standard entity transformation
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0D, -1.5D, 0.0D);

        ResourceLocation texLoc = getTextureLocation(entity);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(texLoc));

        float animTime = (entity.tickCount + partialTicks) / 20.0F;

        // Determine active animation
        String activeAnimName = entity.getActualCurrentAnimation();
        AnimationEngine.AnimationData animData = null;
        if (entry.animationSet != null && activeAnimName != null && !activeAnimName.isEmpty()) {
            animData = entry.animationSet.animations.get(activeAnimName);
            if (animData == null) {
                animData = entry.animationSet.animations.get("idle");
            }
        }

        BlockbenchModel model = entry.model;
        for (BlockbenchModel.BoneGroup rootBone : model.rootBones) {
            renderBoneGroup(rootBone, poseStack, vc, packedLight, animData, animTime, model.texWidth, model.texHeight);
        }

        poseStack.popPose();
    }

    private void renderBoneGroup(BlockbenchModel.BoneGroup bone, PoseStack poseStack, VertexConsumer vc, int packedLight,
                                 AnimationEngine.AnimationData animData, float animTime, int texWidth, int texHeight) {
        poseStack.pushPose();

        float px = bone.pivotX / 16.0F;
        float py = bone.pivotY / 16.0F;
        float pz = bone.pivotZ / 16.0F;

        // 1. Move to the bone's pivot in model space
        poseStack.translate(px, py, pz);

        // 2. Base rotations
        float rotX = bone.rotX;
        float rotY = bone.rotY;
        float rotZ = bone.rotZ;

        // 3. Apply animation overrides if present
        if (animData != null && animData.boneChannels.containsKey(bone.name)) {
            AnimationEngine.BoneAnimationChannel channel = animData.boneChannels.get(bone.name);

            float[] animRot = AnimationEngine.interpolate(channel.rotations, animTime, animData.length, animData.loop, null);
            if (animRot != null) {
                rotX += animRot[0];
                rotY += animRot[1];
                rotZ += animRot[2];
            }

            float[] animPos = AnimationEngine.interpolate(channel.positions, animTime, animData.length, animData.loop, null);
            if (animPos != null) {
                poseStack.translate(animPos[0] / 16.0F, animPos[1] / 16.0F, animPos[2] / 16.0F);
            }

            float[] animScale = AnimationEngine.interpolate(channel.scales, animTime, animData.length, animData.loop, null);
            if (animScale != null) {
                poseStack.scale(animScale[0], animScale[1], animScale[2]);
            }
        }

        // Apply bone rotations (ZYX Euler order in Minecraft/Blockbench)
        if (rotZ != 0) poseStack.mulPose(Axis.ZP.rotationDegrees(rotZ));
        if (rotY != 0) poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
        if (rotX != 0) poseStack.mulPose(Axis.XP.rotationDegrees(rotX));

        // 4. Move back from pivot
        poseStack.translate(-px, -py, -pz);

        // 5. Render Cubes inside this bone
        Matrix4f poseMat = poseStack.last().pose();
        Matrix3f normMat = poseStack.last().normal();

        for (BlockbenchModel.Cube cube : bone.cubes) {
            renderCube(cube, poseStack, poseMat, normMat, vc, packedLight, texWidth, texHeight);
        }

        // 6. Render Child Bones (children inherit parent transform seamlessly)
        for (BlockbenchModel.BoneGroup child : bone.children) {
            renderBoneGroup(child, poseStack, vc, packedLight, animData, animTime, texWidth, texHeight);
        }

        poseStack.popPose();
    }

    private void renderCube(BlockbenchModel.Cube cube, PoseStack poseStack, Matrix4f poseMat, Matrix3f normMat,
                            VertexConsumer vc, int packedLight, int texWidth, int texHeight) {
        poseStack.pushPose();

        float ox = cube.originX / 16.0F;
        float oy = cube.originY / 16.0F;
        float oz = cube.originZ / 16.0F;

        if (cube.rotX != 0 || cube.rotY != 0 || cube.rotZ != 0) {
            poseStack.translate(ox, oy, oz);
            if (cube.rotZ != 0) poseStack.mulPose(Axis.ZP.rotationDegrees(cube.rotZ));
            if (cube.rotY != 0) poseStack.mulPose(Axis.YP.rotationDegrees(cube.rotY));
            if (cube.rotX != 0) poseStack.mulPose(Axis.XP.rotationDegrees(cube.rotX));
            poseStack.translate(-ox, -oy, -oz);
        }

        Matrix4f curPose = poseStack.last().pose();
        Matrix3f curNorm = poseStack.last().normal();

        float x1 = cube.minX / 16.0F;
        float y1 = cube.minY / 16.0F;
        float z1 = cube.minZ / 16.0F;
        float x2 = cube.maxX / 16.0F;
        float y2 = cube.maxY / 16.0F;
        float z2 = cube.maxZ / 16.0F;

        // Render each face with proper 4 quad corner coordinates
        // North face (-Z)
        renderFace(curPose, curNorm, vc, cube.faces.get("north"), x2, y1, z1, x1, y1, z1, x1, y2, z1, x2, y2, z1, 0, 0, -1, packedLight, texWidth, texHeight);
        // South face (+Z)
        renderFace(curPose, curNorm, vc, cube.faces.get("south"), x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, 0, 0, 1, packedLight, texWidth, texHeight);
        // West face (-X)
        renderFace(curPose, curNorm, vc, cube.faces.get("west"), x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, -1, 0, 0, packedLight, texWidth, texHeight);
        // East face (+X)
        renderFace(curPose, curNorm, vc, cube.faces.get("east"), x2, y1, z2, x2, y1, z1, x2, y2, z1, x2, y2, z2, 1, 0, 0, packedLight, texWidth, texHeight);
        // Up face (+Y)
        renderFace(curPose, curNorm, vc, cube.faces.get("up"), x1, y2, z2, x2, y2, z2, x2, y2, z1, x1, y2, z1, 0, 1, 0, packedLight, texWidth, texHeight);
        // Down face (-Y)
        renderFace(curPose, curNorm, vc, cube.faces.get("down"), x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, 0, -1, 0, packedLight, texWidth, texHeight);

        poseStack.popPose();
    }

    private void renderFace(Matrix4f pose, Matrix3f norm, VertexConsumer vc, BlockbenchModel.CubeFace face,
                            float x1, float y1, float z1,
                            float x2, float y2, float z2,
                            float x3, float y3, float z3,
                            float x4, float y4, float z4,
                            float nx, float ny, float nz, int light, int texW, int texH) {
        if (face == null) return;

        float u1 = face.u1 / (float) texW;
        float v1 = face.v1 / (float) texH;
        float u2 = face.u2 / (float) texW;
        float v2 = face.v2 / (float) texH;

        float[] uvs = new float[]{u1, v2, u2, v2, u2, v1, u1, v1};

        // Handle UV rotations (0, 90, 180, 270)
        int rot = (face.rotation % 360 + 360) % 360;
        int shift = (rot / 90) * 2;

        float cu1 = uvs[(0 + shift) % 8];
        float cv1 = uvs[(1 + shift) % 8];
        float cu2 = uvs[(2 + shift) % 8];
        float cv2 = uvs[(3 + shift) % 8];
        float cu3 = uvs[(4 + shift) % 8];
        float cv3 = uvs[(5 + shift) % 8];
        float cu4 = uvs[(6 + shift) % 8];
        float cv4 = uvs[(7 + shift) % 8];

        vc.vertex(pose, x1, y1, z1).color(255, 255, 255, 255).uv(cu1, cv1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, nx, ny, nz).endVertex();
        vc.vertex(pose, x2, y2, z2).color(255, 255, 255, 255).uv(cu2, cv2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, nx, ny, nz).endVertex();
        vc.vertex(pose, x3, y3, z3).color(255, 255, 255, 255).uv(cu3, cv3).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, nx, ny, nz).endVertex();
        vc.vertex(pose, x4, y4, z4).color(255, 255, 255, 255).uv(cu4, cv4).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, nx, ny, nz).endVertex();
    }
}
