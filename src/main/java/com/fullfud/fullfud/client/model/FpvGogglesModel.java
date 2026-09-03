package com.fullfud.fullfud.client.model;

import com.fullfud.fullfud.FullfudMod;
import com.fullfud.fullfud.common.item.FpvGogglesItem;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public class FpvGogglesModel extends GeoModel<FpvGogglesItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "geo/fpv_goggles.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "textures/models/armor/fpv_goggles.png");
    private static final ResourceLocation ANIM = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "animations/fpv_goggles.animation.json");

    @Override
    public ResourceLocation getModelResource(final FpvGogglesItem animatable, @Nullable final GeoRenderer<FpvGogglesItem> renderer) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(final FpvGogglesItem animatable, @Nullable final GeoRenderer<FpvGogglesItem> renderer) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(final FpvGogglesItem animatable) {
        return ANIM;
    }
}
