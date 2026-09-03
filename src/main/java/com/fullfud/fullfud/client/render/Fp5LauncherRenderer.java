package com.fullfud.fullfud.client.render;

import com.fullfud.fullfud.client.model.Fp5LauncherModel;
import com.fullfud.fullfud.common.entity.Fp5LauncherEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Fp5LauncherRenderer extends GeoEntityRenderer<Fp5LauncherEntity> {
    public Fp5LauncherRenderer(final EntityRendererProvider.Context context) {
        super(context, new Fp5LauncherModel());
        this.shadowRadius = 0.0F;
    }

    // See Fp5FlamingoRenderer#render for why the entity and the yaw are read off the renderer now.
    @Override
    public void render(final EntityRenderState renderState, final PoseStack poseStack, final MultiBufferSource bufferSource, final int packedLight) {
        final Fp5LauncherEntity entity = this.animatable;
        final float yaw = Mth.rotLerp(this.partialTick, entity.yRotO, entity.getYRot());
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.scale(Fp5LauncherEntity.SCALE, Fp5LauncherEntity.SCALE, Fp5LauncherEntity.SCALE);
        super.render(renderState, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    /** The culling box lived on the entity until 1.21.2 moved the decision to the renderer. */
    @Override
    protected AABB getBoundingBoxForCulling(final Fp5LauncherEntity entity) {
        return super.getBoundingBoxForCulling(entity)
            .inflate(1.0D * Fp5LauncherEntity.SCALE, 0.5D * Fp5LauncherEntity.SCALE, 1.0D * Fp5LauncherEntity.SCALE);
    }
}
