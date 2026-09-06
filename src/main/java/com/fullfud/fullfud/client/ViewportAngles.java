package com.fullfud.fullfud.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Mutable yaw/pitch/roll carrier, standing in for Forge's {@code ViewportEvent.ComputeCameraAngles}.
 *
 * <p>{@code mixin.client.GameRendererMixin} seeds one of these from the camera each frame, hands it to
 * the FPV and Shahed handlers in turn — the two former listeners on that event — and then writes the
 * result back onto the camera. Roll has no home on {@code Camera}: Forge folded it into the pose stack
 * ahead of the vanilla pitch rotation, and the mixin does the same.
 */
@Environment(EnvType.CLIENT)
public final class ViewportAngles {

    private float yaw;
    private float pitch;
    private float roll;

    public ViewportAngles(final float yaw, final float pitch, final float roll) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public float roll() {
        return roll;
    }

    public void setYaw(final float yaw) {
        this.yaw = yaw;
    }

    public void setPitch(final float pitch) {
        this.pitch = pitch;
    }

    public void setRoll(final float roll) {
        this.roll = roll;
    }
}
