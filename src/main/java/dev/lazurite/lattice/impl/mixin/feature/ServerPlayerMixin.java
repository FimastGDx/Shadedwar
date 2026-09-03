package dev.lazurite.lattice.impl.mixin.feature;

import dev.lazurite.lattice.api.player.LatticePlayer;
import dev.lazurite.lattice.api.player.LatticeServerPlayer;
import dev.lazurite.lattice.api.point.ViewPoint;
import dev.lazurite.lattice.impl.ViewPointHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Shadow private @Nullable Entity camera;
    @Shadow public abstract Entity getCamera();

    /**
     * Set's {@link ServerPlayer#camera} to the {@link ViewPoint} of the desired target, if available.
     */
    @Inject(
            method = "setCamera",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.PUTFIELD,
                    target = "Lnet/minecraft/server/level/ServerPlayer;camera:Lnet/minecraft/world/entity/Entity;",
                    shift = At.Shift.AFTER
            )
    )
    @SuppressWarnings("EqualsBetweenInconvertibleTypes")
    public void setCamera_STORE(Entity entity, CallbackInfo ci) {
        if (entity instanceof Player player && !player.equals(this)) {
            final ViewPoint viewPoint = ViewPointHelper.resolveViewPoint(player);
            if (viewPoint instanceof Entity viewPointEntity) {
                this.camera = viewPointEntity;
            }
        }
    }

    /**
     * Sets the {@link ServerPlayer}'s {@link ViewPoint}.
     */
    @Inject(
            method = "setCamera",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V",
                    shift = At.Shift.AFTER
            )
    )
    public void setCamera_teleportTo(Entity entity, CallbackInfo ci) {
        if (!((Object) this instanceof LatticeServerPlayer latticeServerPlayer)) {
            return;
        }
        final ViewPoint cameraViewPoint = ViewPointHelper.resolveViewPoint(this.getCamera());
        if (cameraViewPoint != null) {
            latticeServerPlayer.setViewPoint(cameraViewPoint);
        }
    }

}
