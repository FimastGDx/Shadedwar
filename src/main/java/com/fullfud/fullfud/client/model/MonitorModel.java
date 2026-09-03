package com.fullfud.fullfud.client.model;

import com.fullfud.fullfud.FullfudMod;
import com.fullfud.fullfud.common.item.MonitorItem;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public class MonitorModel extends GeoModel<MonitorItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "geo/monitorshahed.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "textures/item/monitor.png");
    private static final ResourceLocation ANIM = ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "animations/fpv.animation.json");

    @Override
    public ResourceLocation getModelResource(MonitorItem object, @Nullable final GeoRenderer<MonitorItem> renderer) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(MonitorItem object, @Nullable final GeoRenderer<MonitorItem> renderer) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(MonitorItem animatable) {
        return ANIM;
    }
}