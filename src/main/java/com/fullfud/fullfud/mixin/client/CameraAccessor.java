package com.fullfud.fullfud.mixin.client;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Opens {@code Camera.setRotation}, which is {@code protected}.
 *
 * <p>Forge added a public {@code setAnglesInternal} for the sake of
 * {@code ViewportEvent.ComputeCameraAngles}; this invoker is that method. Writing yaw and pitch through
 * it also refreshes the camera's rotation quaternion and its look/up/left vectors, which is what the
 * drone camera relies on downstream.
 */
@Mixin(Camera.class)
public interface CameraAccessor {

    @Invoker("setRotation")
    void fullfud$setRotation(float yaw, float pitch);
}
