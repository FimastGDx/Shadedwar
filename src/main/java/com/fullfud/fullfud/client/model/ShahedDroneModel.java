package com.fullfud.fullfud.client.model;

import com.fullfud.fullfud.FullfudMod;
import com.fullfud.fullfud.common.entity.ShahedColor;
import com.fullfud.fullfud.common.entity.ShahedDroneEntity;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public class ShahedDroneModel extends GeoModel<ShahedDroneEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "geo/shahed_136.geo.json");
    private static final ResourceLocation MODEL_ON_LAUNCHER = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "geo/shahed_136onlauncher.geo.json");
    private static final ResourceLocation TEXTURE_WHITE = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "textures/entity/shahed_136.png");
    private static final ResourceLocation TEXTURE_BLACK = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "textures/entity/shahed_136_black.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "animations/shahed_136.animation.json");

    @Override
    public ResourceLocation getModelResource(final ShahedDroneEntity animatable, @Nullable final GeoRenderer<ShahedDroneEntity> renderer) {
        return animatable.isOnLauncher() ? MODEL_ON_LAUNCHER : MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(final ShahedDroneEntity animatable, @Nullable final GeoRenderer<ShahedDroneEntity> renderer) {
        return animatable.getColor() == ShahedColor.BLACK ? TEXTURE_BLACK : TEXTURE_WHITE;
    }

    @Override
    public ResourceLocation getAnimationResource(final ShahedDroneEntity animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(final ShahedDroneEntity animatable, final long instanceId, final AnimationState<ShahedDroneEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        final GeoBone root = this.getAnimationProcessor().getBone("bone");
        if (root == null) {
            return;
        }
        final float partialTick = animationState.getPartialTick();
        final float basePitchRad = (float) Math.toRadians(animatable.getVisualPitch(partialTick));
        root.setRotX(-basePitchRad);
        root.setRotZ(0.0F);
    }
}
