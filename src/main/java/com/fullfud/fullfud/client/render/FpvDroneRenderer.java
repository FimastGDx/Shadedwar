package com.fullfud.fullfud.client.render;

import com.fullfud.fullfud.client.model.FpvDroneModel;
import com.fullfud.fullfud.common.entity.FpvDroneEntity;
import com.fullfud.fullfud.core.config.FullfudClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class FpvDroneRenderer extends GeoEntityRenderer<FpvDroneEntity> {
    public FpvDroneRenderer(final EntityRendererProvider.Context context) {
        super(context, new FpvDroneModel());
        this.shadowRadius = 0.2F;
    }

    // See Fp5FlamingoRenderer#render for why the entity and the partial tick are read off the renderer
    // now. getVisualYaw is the drone's own interpolation of the yaw vanilla used to pass in.
    @Override
    public void render(final EntityRenderState renderState, final PoseStack poseStack, final MultiBufferSource bufferSource, final int packedLight) {
        final FpvDroneEntity entity = this.animatable;
        final float partialTick = this.partialTick;
        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getVisualYaw(partialTick)));

        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getVisualPitch(partialTick)));

        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getVisualRoll(partialTick)));

        poseStack.translate(0.0D, -0.05D, 0.0D);

        super.render(renderState, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(final FpvDroneEntity entity, final Frustum frustum, final double x, final double y, final double z) {
        if (super.shouldRender(entity, frustum, x, y, z)) {
            return true;
        }
        final double distSq = x * x + y * y + z * z;
        final double max = Math.max(1.0D, FullfudClientConfig.CLIENT.fpvRenderDistanceCap.get());
        return distSq <= max * max && frustum.isVisible(getBoundingBoxForCulling(entity));
    }
}
