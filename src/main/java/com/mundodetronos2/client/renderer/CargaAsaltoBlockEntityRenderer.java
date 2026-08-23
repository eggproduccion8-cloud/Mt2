package com.mundodetronos2.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mundodetronos2.block.CargaAsaltoBlockEntity;
import com.mundodetronos2.client.model.BlockbenchModel;
import com.mundodetronos2.client.model.BlockModelRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class CargaAsaltoBlockEntityRenderer implements BlockEntityRenderer<CargaAsaltoBlockEntity> {

    public CargaAsaltoBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CargaAsaltoBlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        int chargeLvl = blockEntity.getChargeLevel();

        if (chargeLvl == 2) {
            BlockModelRegistry.Entry entry = BlockModelRegistry.get("230426_bomb");
            if (entry != null && entry.model != null) {
                poseStack.pushPose();
                poseStack.translate(0.5D, 0.0D, 0.5D);
                poseStack.scale(-1.0F, 1.0F, -1.0F);
                poseStack.translate(-0.5D, 0.0D, -0.5D);

                VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(entry.textureLocation));
                BlockbenchModel model = entry.model;
                for (BlockbenchModel.BoneGroup rootBone : model.rootBones) {
                    renderBoneGroup(rootBone, poseStack, vc, packedLight, model.texWidth, model.texHeight);
                }

                poseStack.popPose();
                return;
            }
        }

        // Level 1: Vanilla TNT block texture rendering
        ResourceLocation tntTex = new ResourceLocation("minecraft", "textures/block/tnt_side.png");
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(tntTex));

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);

        Matrix4f poseMat = poseStack.last().pose();
        Matrix3f normMat = poseStack.last().normal();

        // Render small 0.5x0.5x0.5 cube
        float min = -0.25F;
        float max = 0.25F;
        float h = 0.5F;

        // North
        renderQuad(poseMat, normMat, vc, min, 0, min, max, 0, min, max, h, min, min, h, min, 0, 0, -1, packedLight);
        // South
        renderQuad(poseMat, normMat, vc, max, 0, max, min, 0, max, min, h, max, max, h, max, 0, 0, 1, packedLight);
        // West
        renderQuad(poseMat, normMat, vc, min, 0, max, min, 0, min, min, h, min, min, h, max, -1, 0, 0, packedLight);
        // East
        renderQuad(poseMat, normMat, vc, max, 0, min, max, 0, max, max, h, max, max, h, min, 1, 0, 0, packedLight);
        // Up
        renderQuad(poseMat, normMat, vc, min, h, max, max, h, max, max, h, min, min, h, min, 0, 1, 0, packedLight);

        poseStack.popPose();
    }

    private void renderQuad(Matrix4f pose, Matrix3f norm, VertexConsumer vc,
                            float x1, float y1, float z1,
                            float x2, float y2, float z2,
                            float x3, float y3, float z3,
                            float x4, float y4, float z4,
                            float nx, float ny, float nz, int light) {
        vc.vertex(pose, x1, y1, z1).color(255, 255, 255, 255).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, nx, ny, nz).endVertex();
        vc.vertex(pose, x2, y2, z2).color(255, 255, 255, 255).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, nx, ny, nz).endVertex();
        vc.vertex(pose, x3, y3, z3).color(255, 255, 255, 255).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, nx, ny, nz).endVertex();
        vc.vertex(pose, x4, y4, z4).color(255, 255, 255, 255).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, nx, ny, nz).endVertex();
    }

    private void renderBoneGroup(BlockbenchModel.BoneGroup bone, PoseStack poseStack, VertexConsumer vc, int packedLight, int texWidth, int texHeight) {
        poseStack.pushPose();

        float px = bone.pivotX / 16.0F;
        float py = bone.pivotY / 16.0F;
        float pz = bone.pivotZ / 16.0F;

        poseStack.translate(px, py, pz);

        if (bone.rotZ != 0) poseStack.mulPose(Axis.ZP.rotationDegrees(bone.rotZ));
        if (bone.rotY != 0) poseStack.mulPose(Axis.YP.rotationDegrees(bone.rotY));
        if (bone.rotX != 0) poseStack.mulPose(Axis.XP.rotationDegrees(bone.rotX));

        poseStack.translate(-px, -py, -pz);

        Matrix4f poseMat = poseStack.last().pose();
        Matrix3f normMat = poseStack.last().normal();

        for (BlockbenchModel.Cube cube : bone.cubes) {
            renderCube(cube, poseStack, poseMat, normMat, vc, packedLight, texWidth, texHeight);
        }

        for (BlockbenchModel.BoneGroup child : bone.children) {
            renderBoneGroup(child, poseStack, vc, packedLight, texWidth, texHeight);
        }

        poseStack.popPose();
    }

    private void renderCube(BlockbenchModel.Cube cube, PoseStack poseStack, Matrix4f poseMat, Matrix3f normMat, VertexConsumer vc, int packedLight, int texWidth, int texHeight) {
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

        renderFace(curPose, curNorm, vc, cube.faces.get("north"), x2, y1, z1, x1, y1, z1, x1, y2, z1, x2, y2, z1, 0, 0, -1, packedLight, texWidth, texHeight);
        renderFace(curPose, curNorm, vc, cube.faces.get("south"), x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, 0, 0, 1, packedLight, texWidth, texHeight);
        renderFace(curPose, curNorm, vc, cube.faces.get("west"), x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, -1, 0, 0, packedLight, texWidth, texHeight);
        renderFace(curPose, curNorm, vc, cube.faces.get("east"), x2, y1, z2, x2, y1, z1, x2, y2, z1, x2, y2, z2, 1, 0, 0, packedLight, texWidth, texHeight);
        renderFace(curPose, curNorm, vc, cube.faces.get("up"), x1, y2, z2, x2, y2, z2, x2, y2, z1, x1, y2, z1, 0, 1, 0, packedLight, texWidth, texHeight);
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

        float eps = 0.001F;
        float ox = nx * eps;
        float oy = ny * eps;
        float oz = nz * eps;

        float u1 = face.u1 / (float) texW;
        float v1 = face.v1 / (float) texH;
        float u2 = face.u2 / (float) texW;
        float v2 = face.v2 / (float) texH;

        float[] uvs = new float[]{u1, v2, u2, v2, u2, v1, u1, v1};

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

        vc.vertex(pose, x1 + ox, y1 + oy, z1 + oz).color(255, 255, 255, 255).uv(cu1, cv1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, nx, ny, nz).endVertex();
        vc.vertex(pose, x2 + ox, y2 + oy, z2 + oz).color(255, 255, 255, 255).uv(cu2, cv2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, nx, ny, nz).endVertex();
        vc.vertex(pose, x3 + ox, y3 + oy, z3 + oz).color(255, 255, 255, 255).uv(cu3, cv3).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, nx, ny, nz).endVertex();
        vc.vertex(pose, x4 + ox, y4 + oy, z4 + oz).color(255, 255, 255, 255).uv(cu4, cv4).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, nx, ny, nz).endVertex();
    }
}
