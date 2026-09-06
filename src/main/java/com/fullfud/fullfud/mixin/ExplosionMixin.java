package com.fullfud.fullfud.mixin;

import com.fullfud.fullfud.core.DroneExplosionLimiter;
import com.fullfud.fullfud.core.ExplosionControl;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Stands in for Forge's {@code ExplosionEvent.Detonate}, which vanilla has no equivalent of.
 *
 * <p>Both former listeners cleared a list the event exposed: {@code getAffectedEntities()} before
 * vanilla's damage loop, and {@code getAffectedBlocks()} after the ray casting filled it. In 1.21.2
 * {@code Explosion} became an interface and the whole implementation moved to
 * {@link ServerExplosion}, which no longer keeps a {@code toBlow} list to clear: {@code explode()}
 * computes the positions locally and hands them to {@code interactWithBlocks}. So the entity list is
 * still intercepted where it is produced — now inside {@code hurtEntities} rather than
 * {@code explode} — and block damage is suppressed by cancelling {@code interactWithBlocks}
 * outright, which is what an emptied block list amounted to.
 *
 * <p>Both injectors carry {@code require = 1}: the enclosing config is {@code defaultRequire: 0} for
 * the sake of the optional audio accessors, and explosion suppression must not fail quietly.
 */
@Mixin(ServerExplosion.class)
public abstract class ExplosionMixin {

    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    @Final
    private Entity source;

    @Redirect(
        method = "hurtEntities",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"
        ),
        require = 1
    )
    private List<Entity> fullfud$filterExplosionEntities(final ServerLevel level, final Entity source, final AABB bounds) {
        if (DroneExplosionLimiter.suppressesEntityDamage(this.source)) {
            return List.of();
        }
        return level.getEntities(source, bounds);
    }

    @Inject(method = "interactWithBlocks", at = @At("HEAD"), cancellable = true, require = 1)
    private void fullfud$suppressBlockDamage(final List<net.minecraft.core.BlockPos> positions, final CallbackInfo callback) {
        if (ExplosionControl.isExplosionBlockDamageDisabled(this.level)
            || DroneExplosionLimiter.suppressesBlockDamage(this.source)) {
            callback.cancel();
        }
    }
}
