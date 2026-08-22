package com.mundodetronos2.client.renderer;

import com.mundodetronos2.client.model.CustomNPCGeoModel;
import com.mundodetronos2.entity.CustomNPCEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CustomNPCGeoRenderer extends GeoEntityRenderer<CustomNPCEntity> {

    public CustomNPCGeoRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CustomNPCGeoModel());
        this.shadowRadius = 0.5F;
    }
}
