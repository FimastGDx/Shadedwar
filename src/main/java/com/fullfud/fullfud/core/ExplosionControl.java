package com.fullfud.fullfud.core;

import com.fullfud.fullfud.core.config.FullfudServerConfig;
import net.minecraft.server.level.ServerLevel;

/**
 * Global explosion block-damage switch. Forge exposed this through an {@code ExplosionEvent.Detonate}
 * listener; vanilla has no such hook, so {@code mixin.ExplosionMixin} calls
 * {@link #isExplosionBlockDamageDisabled(ServerLevel)} and cancels the block pass itself.
 */
public final class ExplosionControl {
    private ExplosionControl() {
    }

    public static boolean isExplosionBlockDamageDisabled(final ServerLevel level) {
        if (FullfudServerConfig.SERVER.disableExplosionBlockDamage.get()) {
            return true;
        }
        return level != null && level.getGameRules().getBoolean(FullfudGameRules.DISABLE_EXPLOSION_BLOCK_DAMAGE);
    }
}
