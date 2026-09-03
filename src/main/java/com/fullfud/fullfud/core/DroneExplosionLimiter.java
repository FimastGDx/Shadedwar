package com.fullfud.fullfud.core;

import com.fullfud.fullfud.core.data.PersistentData;
import net.minecraft.world.entity.Entity;

/**
 * Per-exploder opt-outs from explosion damage, marked on the entity before it detonates.
 *
 * <p>On Forge these were two {@code ExplosionEvent.Detonate} listeners at opposite priorities. Vanilla
 * has no such event, so {@code mixin.ExplosionMixin} asks the two predicates below: the entity list is
 * replaced before the damage loop runs, and the block list is cleared after {@code explode()} has
 * filled it.
 */
public final class DroneExplosionLimiter {
    private DroneExplosionLimiter() { }

    private static final String TAG_NO_BLOCK_DAMAGE = "fullfud_no_block_damage";
    private static final String TAG_NO_ENTITY_DAMAGE = "fullfud_no_entity_damage";

    public static boolean suppressesBlockDamage(final Entity exploder) {
        return exploder != null && PersistentData.of(exploder).getBoolean(TAG_NO_BLOCK_DAMAGE);
    }

    public static boolean suppressesEntityDamage(final Entity exploder) {
        return exploder != null && PersistentData.of(exploder).getBoolean(TAG_NO_ENTITY_DAMAGE);
    }

    public static void markNoBlockDamage(final Entity entity) {
        if (entity == null) {
            return;
        }
        PersistentData.of(entity).putBoolean(TAG_NO_BLOCK_DAMAGE, true);
    }

    public static void markNoEntityDamage(final Entity entity) {
        if (entity == null) {
            return;
        }
        PersistentData.of(entity).putBoolean(TAG_NO_ENTITY_DAMAGE, true);
    }
}
