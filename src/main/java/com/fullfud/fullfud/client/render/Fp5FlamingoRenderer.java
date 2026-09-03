package com.fullfud.fullfud.client.render;

import com.fullfud.fullfud.client.model.Fp5FlamingoModel;
import com.fullfud.fullfud.common.entity.Fp5FlamingoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Fp5FlamingoRenderer extends GeoEntityRenderer<Fp5FlamingoEntity> {
    public Fp5FlamingoRenderer(final EntityRendererProvider.Context context) {
        super(context, new Fp5FlamingoModel());
        this.shadowRadius = 0.0F;
    }

    /**
     * 1.21.2 rebuilt entity rendering around a render state: the entity, the yaw and the partial tick are
     * no longer parameters. GeckoLib still keeps the animatable and the partial tick on the renderer
     * ({@code this.animatable}/{@code this.partialTick}, both filled in {@code extractRenderState}), so the
     * transforms below read from there instead. The yaw that vanilla used to pass in was an interpolation
     * of {@code yRotO}/{@code getYRot()}, which is what this entity already computed for itself.
     */
    @Override
    public void render(final EntityRenderState renderState, final PoseStack poseStack, final MultiBufferSource bufferSource, final int packedLight) {
        final Fp5FlamingoEntity entity = this.animatable;
        final float partialTick = this.partialTick;
        final float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        final float pitch = Mth.rotLerp(partialTick, entity.xRotO, entity.getXRot());
        final float roll = entity.getVisualRoll(partialTick);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.scale(Fp5FlamingoEntity.SCALE, Fp5FlamingoEntity.SCALE, Fp5FlamingoEntity.SCALE);
        super.render(renderState, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    /** The culling box lived on the entity until 1.21.2 moved the decision to the renderer. */
    @Override
    protected AABB getBoundingBoxForCulling(final Fp5FlamingoEntity entity) {
        return super.getBoundingBoxForCulling(entity)
            .inflate(1.0D * Fp5FlamingoEntity.SCALE, 0.5D * Fp5FlamingoEntity.SCALE, 1.0D * Fp5FlamingoEntity.SCALE);
    }
}
