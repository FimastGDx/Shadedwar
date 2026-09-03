package com.fullfud.fullfud.client.model;

import com.fullfud.fullfud.FullfudMod;
import com.fullfud.fullfud.common.entity.RebEmitterEntity;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public class RebEmitterModel extends GeoModel<RebEmitterEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "geo/reb_emitter.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "textures/entity/reb_emitter.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "animations/reb.animation.json");

    @Override
    public ResourceLocation getModelResource(final RebEmitterEntity animatable, @Nullable final GeoRenderer<RebEmitterEntity> renderer) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(final RebEmitterEntity animatable, @Nullable final GeoRenderer<RebEmitterEntity> renderer) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(final RebEmitterEntity animatable) {
        return ANIMATION;
    }
}
