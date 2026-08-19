package com.mundodetronos2.client;

import com.mundodetronos2.entity.GoddessNPCEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

public class GoddessNPCRenderer extends LivingEntityRenderer<GoddessNPCEntity, PlayerModel<GoddessNPCEntity>> {

    public GoddessNPCRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(GoddessNPCEntity entity) {
        return getSkinLocationByName(entity.getSkinName());
    }

    public static ResourceLocation getSkinLocationByName(String skinName) {
        if (skinName != null && !skinName.isEmpty()) {
            java.util.UUID uuid = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + skinName).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            try {
                net.minecraft.client.resources.SkinManager skinManager = Minecraft.getInstance().getSkinManager();
                com.mojang.authlib.GameProfile profile = new com.mojang.authlib.GameProfile(uuid, skinName);
                java.util.Map<com.mojang.authlib.minecraft.MinecraftProfileTexture.Type, com.mojang.authlib.minecraft.MinecraftProfileTexture> map = skinManager.getInsecureSkinInformation(profile);
                if (map.containsKey(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN)) {
                    return skinManager.registerTexture(map.get(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN), com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN);
                }
            } catch (Exception ignored) {}
            return DefaultPlayerSkin.getDefaultSkin(uuid);
        }
        return DefaultPlayerSkin.getDefaultSkin(java.util.UUID.randomUUID());
    }
}
