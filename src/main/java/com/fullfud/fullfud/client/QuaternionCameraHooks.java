package com.fullfud.fullfud.client;

import com.fullfud.fullfud.common.entity.FpvDroneEntity;
import com.fullfud.fullfud.common.item.FpvGogglesItem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * Writes the drone's orientation onto the vanilla {@link Camera}.
 *
 * <p>1.21.4 derives the whole view rotation from {@code camera.rotation()} — {@code GameRenderer
 * .renderLevel} conjugates it into a {@code Matrix4f} and passes that to the cull frustum, the level
 * renderer and the held-item renderer — so replacing that one quaternion is all it takes, where 1.20.1
 * needed the rotation multiplied onto a pose stack as well. The look/up/left vectors are kept in step
 * because fog, particles and frustum culling read them directly.
 */
public final class QuaternionCameraHooks {
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private QuaternionCameraHooks() {
    }

    public static boolean applyDroneCamera(final Camera camera, final float partialTick) {
        if (camera == null) {
            return false;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) {
            return false;
        }

        final FpvDroneEntity drone;
        if (camera.getEntity() instanceof FpvDroneEntity cameraDrone) {
            drone = cameraDrone;
        } else {
            drone = FpvClientHandler.resolveActiveControlledDrone(minecraft);
        }
        if (drone == null) {
            return false;
        }

        final UUID controller = drone.getControllerId();
        if (controller == null || !controller.equals(minecraft.player.getUUID())) {
            return false;
        }
        if (!hasLinkedGoggles(minecraft, drone)) {
            return false;
        }

        final Quaternionf cameraQuaternion = FpvClientHandler.resolveRenderCameraQuaternion(drone, partialTick);
        final Vector3f forward = new Vector3f(0.0F, 0.0F, 1.0F);
        final Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
        final Vector3f localX = new Vector3f(1.0F, 0.0F, 0.0F);
        cameraQuaternion.transform(forward);
        cameraQuaternion.transform(up);
        cameraQuaternion.transform(localX);

        final Matrix3f viewMatrix = new Matrix3f();
        viewMatrix.setColumn(0, -localX.x, -localX.y, -localX.z);
        viewMatrix.setColumn(1, up.x, up.y, up.z);
        viewMatrix.setColumn(2, -forward.x, -forward.y, -forward.z);

        // viewMatrix is the rotation vanilla used to multiply onto the pose stack. 1.21.4 applies the
        // conjugate of camera.rotation() instead, so store the conjugate of that same rotation.
        camera.rotation().set(viewMatrix.getNormalizedRotation(new Quaternionf())).conjugate();
        camera.getLookVector().set(forward);
        camera.getUpVector().set(up);
        camera.getLeftVector().set(localX);
        return true;
    }

    /**
     * Banks the camera by {@code rollDegrees}, which vanilla has no field for. Post-multiplying the
     * negated roll onto the camera quaternion is exactly equivalent to the leading
     * {@code mulPose(Axis.ZP.rotationDegrees(roll))} the 1.20.1 mixin used, since the engine applies the
     * conjugate of this quaternion.
     */
    public static void applyCameraRoll(final Camera camera, final float rollDegrees) {
        if (camera == null || rollDegrees == 0.0F || !Float.isFinite(rollDegrees)) {
            return;
        }
        final Quaternionf rotation = camera.rotation().rotateZ(-rollDegrees * DEG_TO_RAD);
        rotation.transform(camera.getLookVector().set(0.0F, 0.0F, -1.0F));
        rotation.transform(camera.getUpVector().set(0.0F, 1.0F, 0.0F));
        rotation.transform(camera.getLeftVector().set(-1.0F, 0.0F, 0.0F));
    }

    private static boolean hasLinkedGoggles(final Minecraft minecraft, final FpvDroneEntity drone) {
        if (minecraft == null || minecraft.player == null || drone == null) {
            return false;
        }
        final ItemStack head = minecraft.player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(head.getItem() instanceof FpvGogglesItem)) {
            return false;
        }
        return FpvGogglesItem.getLinked(head).map(drone.getUUID()::equals).orElse(true);
    }
}
