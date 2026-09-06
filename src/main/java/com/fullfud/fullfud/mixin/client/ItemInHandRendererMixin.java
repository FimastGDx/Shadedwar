package com.fullfud.fullfud.mixin.client;

import com.fullfud.fullfud.client.FpvClientHandler;
import com.fullfud.fullfud.client.ShahedClientHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stands in for the two {@code RenderHandEvent} listeners, which both did nothing but cancel: the pilot's
 * own arms must not hang in front of a drone feed. Forge fired that event from this method, so cancelling
 * it here drops both hands the same way.
 */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true, require = 1)
    private void fullfud$hideHandsWhilePiloting(final float partialTick, final PoseStack poseStack, final MultiBufferSource.BufferSource buffer, final LocalPlayer player, final int packedLight, final CallbackInfo callback) {
        if (FpvClientHandler.shouldHideHand() || ShahedClientHandler.shouldHideHand()) {
            callback.cancel();
        }
    }
}
