package dev.lazurite.lattice.impl;

import dev.lazurite.lattice.api.player.LatticePlayer;
import dev.lazurite.lattice.api.point.ViewPoint;
import dev.lazurite.lattice.impl.api.level.InternalLatticeServerLevel;
import dev.lazurite.lattice.impl.api.player.InternalLatticeServerPlayer;
import dev.lazurite.lattice.impl.mixin.access.IChunkMapMixin;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public final class ViewPointHelper {
    private ViewPointHelper() {
    }

    @Nullable
    public static ViewPoint resolveViewPoint(@Nullable final Object candidate) {
        if (candidate instanceof final LatticePlayer latticePlayer) {
            final ViewPoint viewPoint = latticePlayer.getViewPoint();
            if (viewPoint != null) {
                return viewPoint;
            }
        }
        if (candidate instanceof final ViewPoint viewPoint) {
            return viewPoint;
        }
        if (candidate instanceof final Entity entity) {
            return new EntityBackedViewPoint(entity);
        }
        return null;
    }

    public static int resolveViewDistance(@Nullable final ServerLevel serverLevel) {
        if (serverLevel != null && serverLevel.getChunkSource().chunkMap instanceof IChunkMapMixin chunkMapMixin) {
            return chunkMapMixin.getViewDistance();
        }
        if (serverLevel != null && serverLevel.getServer() != null) {
            return serverLevel.getServer().getPlayerList().getViewDistance();
        }
        return 10;
    }

    @Nullable
    public static InternalLatticeServerPlayer resolveInternalServerPlayer(@Nullable final ServerPlayer player) {
        return player instanceof InternalLatticeServerPlayer internal ? internal : null;
    }

    @Nullable
    public static InternalLatticeServerLevel resolveInternalServerLevel(@Nullable final ServerLevel level) {
        return level instanceof InternalLatticeServerLevel internal ? internal : null;
    }

    private static final class EntityBackedViewPoint implements ViewPoint {
        private final Entity entity;

        private EntityBackedViewPoint(final Entity entity) {
            this.entity = entity;
        }

        @Override
        public double getX() {
            return entity.getX();
        }

        @Override
        public double getY() {
            return entity.getY();
        }

        @Override
        public double getZ() {
            return entity.getZ();
        }

        @Override
        public float getYRot() {
            return entity.getYRot();
        }

        @Override
        public float getXRot() {
            return entity.getXRot();
        }

        @Override
        public int getDistance() {
            if (entity.level() instanceof final ServerLevel serverLevel) {
                return resolveViewDistance(serverLevel);
            }
            return 0;
        }
    }
}
