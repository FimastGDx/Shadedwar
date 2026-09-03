package com.fullfud.fullfud.client.render;

import com.fullfud.fullfud.client.model.ShahedDroneModel;
import com.fullfud.fullfud.common.entity.ShahedDroneEntity;
import com.fullfud.fullfud.core.config.FullfudClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ShahedDroneRenderer extends GeoEntityRenderer<ShahedDroneEntity> {

    public ShahedDroneRenderer(final EntityRendererProvider.Context context) {
        super(context, new ShahedDroneModel());
        this.shadowRadius = 0.0F;
    }

    // See Fp5FlamingoRenderer#render for why the entity, the yaw and the partial tick are read off the
    // renderer now.
    @Override
    public void render(final EntityRenderState renderState, final PoseStack poseStack, final MultiBufferSource bufferSource, final int packedLight) {
        final ShahedDroneEntity entity = this.animatable;
        final float partialTick = this.partialTick;
        final float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getVisualRoll(partialTick)));

        poseStack.translate(0.0D, -0.25D, 0.0D);

        super.render(renderState, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    /** The culling box lived on the entity until 1.21.2 moved the decision to the renderer. */
    @Override
    protected AABB getBoundingBoxForCulling(final ShahedDroneEntity entity) {
        return super.getBoundingBoxForCulling(entity).inflate(1.5D, 1.5D, 1.5D);
    }

    @Override
    public boolean shouldRender(final ShahedDroneEntity entity, final Frustum frustum, final double x, final double y, final double z) {
        if (super.shouldRender(entity, frustum, x, y, z)) {
            return true;
        }
        final double distSq = x * x + y * y + z * z;
        final double max = Math.max(1.0D, FullfudClientConfig.CLIENT.shahedRenderDistanceCap.get());
        return distSq <= max * max && frustum.isVisible(getBoundingBoxForCulling(entity));
    }
}
