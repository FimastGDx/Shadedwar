package dev.lazurite.lattice.impl.mixin.fix.misc;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    /**
     * Cancels {@link ServerPlayer#absMoveTo(double, double, double, float, float)} in {@link ServerPlayer#tick()}.
     */
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;absMoveTo(DDDFF)V"
            )
    )
    public void absMoveTo(ServerPlayer serverPlayer, double d, double e, double f, float g, float h) { }

    /**
     * Cancels {@link ServerPlayer#teleportTo(ServerLevel, double, double, double, Set, float, float, boolean)}
     * in {@link ServerPlayer#setCamera(Entity)}. Since 1.21.2 the relative-movement flags are
     * {@link Relative} rather than {@code RelativeMovement}, and the call carries a trailing
     * "set camera" boolean.
     */
    @Redirect(
            method = "setCamera",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z"
            )
    )
    public boolean teleportTo(ServerPlayer serverPlayer, ServerLevel level, double d, double e, double f, Set<Relative> movement, float yaw, float pitch, boolean setCamera) {
        return true;
    }

}
