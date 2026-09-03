package com.fullfud.fullfud.client.model;

import com.fullfud.fullfud.FullfudMod;
import com.fullfud.fullfud.common.entity.Fp5FlamingoEntity;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public class Fp5FlamingoModel extends GeoModel<Fp5FlamingoEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "geo/fp5flamingo.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "textures/entity/fp5flamingo.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "animations/fp5flamingo.animation.json");

    @Override
    public ResourceLocation getModelResource(final Fp5FlamingoEntity animatable, @Nullable final GeoRenderer<Fp5FlamingoEntity> renderer) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(final Fp5FlamingoEntity animatable, @Nullable final GeoRenderer<Fp5FlamingoEntity> renderer) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(final Fp5FlamingoEntity animatable) {
        return ANIMATION;
    }
}
