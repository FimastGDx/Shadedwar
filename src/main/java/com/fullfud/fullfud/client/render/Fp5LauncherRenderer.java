package com.fullfud.fullfud.client.render;

import com.fullfud.fullfud.client.model.Fp5LauncherModel;
import com.fullfud.fullfud.common.entity.Fp5LauncherEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Fp5LauncherRenderer extends GeoEntityRenderer<Fp5LauncherEntity> {
    public Fp5LauncherRenderer(final EntityRendererProvider.Context context) {
        super(context, new Fp5LauncherModel());
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(final Fp5LauncherEntity entity, final float entityYaw, final float partialTick, final PoseStack poseStack, final MultiBufferSource bufferSource, final int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        poseStack.scale(Fp5LauncherEntity.SCALE, Fp5LauncherEntity.SCALE, Fp5LauncherEntity.SCALE);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
