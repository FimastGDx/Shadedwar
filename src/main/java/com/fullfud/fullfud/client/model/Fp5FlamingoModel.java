package com.fullfud.fullfud.client.model;

import com.fullfud.fullfud.FullfudMod;
import com.fullfud.fullfud.common.entity.Fp5FlamingoEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Fp5FlamingoModel extends GeoModel<Fp5FlamingoEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation(FullfudMod.MOD_ID, "geo/fp5flamingo.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(FullfudMod.MOD_ID, "textures/entity/fp5flamingo.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(FullfudMod.MOD_ID, "animations/fp5flamingo.animation.json");

    @Override
    public ResourceLocation getModelResource(final Fp5FlamingoEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(final Fp5FlamingoEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(final Fp5FlamingoEntity animatable) {
        return ANIMATION;
    }
}
