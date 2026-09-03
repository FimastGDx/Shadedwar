package com.fullfud.fullfud.mixin.client;

import com.fullfud.fullfud.client.FpvClientHandler;
import com.fullfud.fullfud.client.QuaternionCameraHooks;
import com.fullfud.fullfud.client.ShahedClientHandler;
import com.fullfud.fullfud.client.ViewportAngles;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Three Forge client hooks that Fabric API has no event for, all of which live in {@code GameRenderer}.
 *
 * <ul>
 *   <li>{@code TickEvent.RenderTickEvent} START/END → the head and tail of {@code render}. That is the
 *       same bracket Forge fired around the frame, so the FPV post-processing chain is still processed
 *       after the world has been drawn and before the framebuffer is flipped.</li>
 *   <li>{@code ViewportEvent.ComputeCameraAngles} → an inject right after {@code Camera.setup}, exactly
 *       where Forge fired it. Yaw and pitch go back onto the camera through {@link CameraAccessor}.</li>
 *   <li>The drone camera itself. On 1.20.1 this was a pair of {@code PoseStack.mulPose(Quaternionf)}
 *       redirects (and before that the JS coremod {@code fullfud_quaternion_camera.js}), because the
 *       view rotation was multiplied onto a pose stack inside {@code renderLevel}. 1.21.4 has no such
 *       calls: {@code renderLevel} builds the world rotation once, as
 *       {@code new Matrix4f().rotation(camera.rotation().conjugate(new Quaternionf()))}, and hands that
 *       matrix to {@code LevelRenderer.prepareCullFrustum}, {@code LevelRenderer.renderLevel} and
 *       {@code renderItemInHand}. So the camera quaternion is now the single place to write, and doing
 *       it here — before any of those three read it — covers the world, the cull frustum and the held
 *       item in one shot. Roll, which vanilla still has no notion of, is folded into the same
 *       quaternion.</li>
 * </ul>
 *
 * <p>{@code require = 1} throughout: a failed match is a startup error rather than a silently dead
 * camera.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow
    public abstract Camera getMainCamera();

    @Inject(method = "render", at = @At("HEAD"), require = 1)
    private void fullfud$renderTickStart(final DeltaTracker deltaTracker, final boolean renderLevel, final CallbackInfo callback) {
        FpvClientHandler.onRenderTickStart(deltaTracker.getGameTimeDeltaPartialTick(false));
    }

    @Inject(method = "render", at = @At("TAIL"), require = 1)
    private void fullfud$renderTickEnd(final DeltaTracker deltaTracker, final boolean renderLevel, final CallbackInfo callback) {
        FpvClientHandler.onRenderTickEnd(deltaTracker.getGameTimeDeltaPartialTick(false));
    }

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
            shift = At.Shift.AFTER
        ),
        require = 1
    )
    private void fullfud$computeCameraAngles(final DeltaTracker deltaTracker, final CallbackInfo callback) {
        final Camera camera = getMainCamera();
        // Camera.setup() has just stored the partial tick it was called with; use that rather than asking
        // the DeltaTracker again, so pause and frozen-tick behaviour matches vanilla's own camera.
        final float partialTick = camera.getPartialTickTime();
        final ViewportAngles angles = new ViewportAngles(camera.getYRot(), camera.getXRot(), 0.0F);
        FpvClientHandler.onCameraAngles(camera, partialTick, angles);
        ShahedClientHandler.onComputeCameraAngles(camera, partialTick, angles);
        ((CameraAccessor) camera).fullfud$setRotation(angles.yaw(), angles.pitch());
        if (!QuaternionCameraHooks.applyDroneCamera(camera, partialTick)) {
            QuaternionCameraHooks.applyCameraRoll(camera, angles.roll());
        }
    }
}
