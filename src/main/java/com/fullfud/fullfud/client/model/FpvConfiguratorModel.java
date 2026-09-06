package com.fullfud.fullfud.client.model;

import com.fullfud.fullfud.FullfudMod;
import com.fullfud.fullfud.common.item.FpvConfiguratorItem;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public class FpvConfiguratorModel extends GeoModel<FpvConfiguratorItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "geo/monitorshahed.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "textures/item/monitor.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "animations/fpv.animation.json");

    @Override
    public ResourceLocation getModelResource(final FpvConfiguratorItem object, @Nullable final GeoRenderer<FpvConfiguratorItem> renderer) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(final FpvConfiguratorItem object, @Nullable final GeoRenderer<FpvConfiguratorItem> renderer) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(final FpvConfiguratorItem animatable) {
        return ANIMATION;
    }
}
