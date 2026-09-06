package com.fullfud.fullfud.client.model;

import com.fullfud.fullfud.FullfudMod;
import com.fullfud.fullfud.common.entity.Fp5LauncherEntity;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public class Fp5LauncherModel extends GeoModel<Fp5LauncherEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "geo/launcherfp5.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "textures/entity/launcherfp5.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "animations/shahed_136.animation.json");

    @Override
    public ResourceLocation getModelResource(final Fp5LauncherEntity animatable, @Nullable final GeoRenderer<Fp5LauncherEntity> renderer) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(final Fp5LauncherEntity animatable, @Nullable final GeoRenderer<Fp5LauncherEntity> renderer) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(final Fp5LauncherEntity animatable) {
        return ANIMATION;
    }
}
