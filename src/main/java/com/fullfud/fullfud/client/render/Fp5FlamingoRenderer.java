package com.fullfud.fullfud.client.render;

import com.fullfud.fullfud.client.model.Fp5FlamingoModel;
import com.fullfud.fullfud.common.entity.Fp5FlamingoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Fp5FlamingoRenderer extends GeoEntityRenderer<Fp5FlamingoEntity> {
    public Fp5FlamingoRenderer(final EntityRendererProvider.Context context) {
        super(context, new Fp5FlamingoModel());
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(final Fp5FlamingoEntity entity, final float entityYaw, final float partialTick, final PoseStack poseStack, final MultiBufferSource bufferSource, final int packedLight) {
        final float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        final float pitch = Mth.rotLerp(partialTick, entity.xRotO, entity.getXRot());
        final float roll = entity.getVisualRoll(partialTick);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.scale(Fp5FlamingoEntity.SCALE, Fp5FlamingoEntity.SCALE, Fp5FlamingoEntity.SCALE);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
