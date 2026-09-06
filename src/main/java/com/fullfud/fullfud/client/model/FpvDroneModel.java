package com.fullfud.fullfud.client.model;

import com.fullfud.fullfud.FullfudMod;
import com.fullfud.fullfud.common.entity.FpvDroneEntity;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public class FpvDroneModel extends GeoModel<FpvDroneEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "geo/fpv_drone.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "textures/entity/fpv_drone.png");
    private static final ResourceLocation ANIM = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "animations/fpv.animation.json");

    @Override
    public ResourceLocation getModelResource(final FpvDroneEntity entity, @Nullable final GeoRenderer<FpvDroneEntity> renderer) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(final FpvDroneEntity entity, @Nullable final GeoRenderer<FpvDroneEntity> renderer) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(final FpvDroneEntity entity) {
        return ANIM;
    }

    @Override
    public void setCustomAnimations(FpvDroneEntity animatable, long instanceId, AnimationState<FpvDroneEntity> animationState) {
        GeoBone body = getAnimationProcessor().getBone("Body");

        if (body != null) {
            body.setRotX((float) Math.toRadians(-animatable.getXRot()));

            body.setRotZ((float) Math.toRadians(-animatable.getVisualRoll(animationState.getPartialTick())));
        }
    }
}