package com.fullfud.fullfud.client.model;

import com.fullfud.fullfud.FullfudMod;
import com.fullfud.fullfud.common.entity.Fp5LauncherEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Fp5LauncherModel extends GeoModel<Fp5LauncherEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation(FullfudMod.MOD_ID, "geo/launcherfp5.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(FullfudMod.MOD_ID, "textures/entity/launcherfp5.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(FullfudMod.MOD_ID, "animations/shahed_136.animation.json");

    @Override
    public ResourceLocation getModelResource(final Fp5LauncherEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(final Fp5LauncherEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(final Fp5LauncherEntity animatable) {
        return ANIMATION;
    }
}
