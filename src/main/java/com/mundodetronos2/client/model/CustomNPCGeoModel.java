package com.mundodetronos2.client.model;

import com.mundodetronos2.entity.CustomNPCEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CustomNPCGeoModel extends GeoModel<CustomNPCEntity> {

    @Override
    public ResourceLocation getModelResource(CustomNPCEntity animatable) {
        String modelName = animatable.getNpcModel().toLowerCase().trim();
        if (modelName.startsWith("guard")) {
            // Guard variants reuse guard.geo.json
            modelName = "guard";
        }
        return new ResourceLocation("mundodetronos2", "geo/" + modelName + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CustomNPCEntity animatable) {
        String texName = animatable.getNpcTexture().toLowerCase().trim();
        if (texName.endsWith(".png")) {
            texName = texName.substring(0, texName.length() - 4);
        }
        return new ResourceLocation("mundodetronos2", "textures/entity/" + texName + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(CustomNPCEntity animatable) {
        String modelName = animatable.getNpcModel().toLowerCase().trim();
        if (modelName.startsWith("guard")) {
            modelName = "guard";
        }
        return new ResourceLocation("mundodetronos2", "animations/" + modelName + ".animation.json");
    }
}
