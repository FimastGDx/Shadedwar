package dev.lazurite.lattice.impl.client.mixin.fix.render;

import dev.lazurite.lattice.api.player.LatticePlayer;
import dev.lazurite.lattice.api.point.ViewPoint;
import dev.lazurite.lattice.impl.ViewPointHelper;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fixes some hard-coded {@link LocalPlayer} positions to use {@link ViewPoint} instead.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    /**
     * Uses the {@link ViewPoint}'s position instead of the {@link LocalPlayer}'s position.
     */
    @Redirect(
            method = "setupRender",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getX()D"
            )
    )
    public final double setupRender_getX(LocalPlayer player) {
        final ViewPoint viewPoint = ViewPointHelper.resolveViewPoint(player);
        return viewPoint != null ? viewPoint.getX() : player.getX();
    }

    /**
     * Uses the {@link ViewPoint}'s position instead of the {@link LocalPlayer}'s position.
     */
    @Redirect(
            method = "setupRender",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getY()D"
            )
    )
    public final double setupRender_getY(LocalPlayer player) {
        final ViewPoint viewPoint = ViewPointHelper.resolveViewPoint(player);
        return viewPoint != null ? viewPoint.getY() : player.getY();
    }

    /**
     * Uses the {@link ViewPoint}'s position instead of the {@link LocalPlayer}'s position.
     */
    @Redirect(
            method = "setupRender",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getZ()D"
            )
    )
    public final double setupRender_getZ(LocalPlayer player) {
        final ViewPoint viewPoint = ViewPointHelper.resolveViewPoint(player);
        return viewPoint != null ? viewPoint.getZ() : player.getZ();
    }

}
