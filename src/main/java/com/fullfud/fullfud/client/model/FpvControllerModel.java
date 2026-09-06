package com.fullfud.fullfud.client.model;

import com.fullfud.fullfud.FullfudMod;
import com.fullfud.fullfud.common.item.FpvControllerItem;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public class FpvControllerModel extends GeoModel<FpvControllerItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "geo/joystickfpv.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "textures/item/joystick.png");
    private static final ResourceLocation ANIM = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "animations/fpv.animation.json");

    @Override
    public ResourceLocation getModelResource(FpvControllerItem object, @Nullable final GeoRenderer<FpvControllerItem> renderer) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(FpvControllerItem object, @Nullable final GeoRenderer<FpvControllerItem> renderer) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(FpvControllerItem animatable) {
        return ANIM;
    }
}