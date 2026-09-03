package com.fullfud.fullfud.client.render;

import com.fullfud.fullfud.client.model.ShahedLauncherModel;
import com.fullfud.fullfud.common.entity.ShahedLauncherEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ShahedLauncherRenderer extends GeoEntityRenderer<ShahedLauncherEntity> {

    public ShahedLauncherRenderer(final EntityRendererProvider.Context context) {
        super(context, new ShahedLauncherModel());
        this.shadowRadius = 0.0F;
    }

    // See Fp5FlamingoRenderer#render for why the entity and the yaw are read off the renderer now.
    @Override
    public void render(final EntityRenderState renderState, final PoseStack poseStack, final MultiBufferSource bufferSource, final int packedLight) {
        final ShahedLauncherEntity entity = this.animatable;
        final float yaw = Mth.rotLerp(this.partialTick, entity.yRotO, entity.getYRot());
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        super.render(renderState, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
