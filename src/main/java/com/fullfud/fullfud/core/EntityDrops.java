package com.fullfud.fullfud.core;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

/**
 * 1.21.2 retyped {@code Entity.spawnAtLocation} to take the {@link ServerLevel} explicitly instead of
 * reading {@code this.level} and no-opping on the client. Every drop in this mod already sits behind a
 * server-side guard, so the level is resolved from the entity here rather than repeating the
 * {@code instanceof} at each of the fourteen call sites.
 */
public final class EntityDrops {
    private EntityDrops() {
    }

    @Nullable
    public static ItemEntity spawnAtLocation(final Entity entity, final ItemStack stack) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return entity.spawnAtLocation(serverLevel, stack);
    }

    @Nullable
    public static ItemEntity spawnAtLocation(final Entity entity, final ItemStack stack, final float yOffset) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return entity.spawnAtLocation(serverLevel, stack, yOffset);
    }
}
