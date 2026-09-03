package com.fullfud.fullfud.client.model;

import com.fullfud.fullfud.FullfudMod;
import com.fullfud.fullfud.common.entity.ShahedLauncherEntity;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public class ShahedLauncherModel extends GeoModel<ShahedLauncherEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "geo/launcher.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "textures/entity/shahed_launcher.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "animations/shahed_136.animation.json");

    @Override
    public ResourceLocation getModelResource(final ShahedLauncherEntity animatable, @Nullable final GeoRenderer<ShahedLauncherEntity> renderer) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(final ShahedLauncherEntity animatable, @Nullable final GeoRenderer<ShahedLauncherEntity> renderer) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(final ShahedLauncherEntity animatable) {
        return ANIMATION;
    }
}
