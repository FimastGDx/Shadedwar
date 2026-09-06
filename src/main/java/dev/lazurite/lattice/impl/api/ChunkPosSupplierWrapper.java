package dev.lazurite.lattice.impl.api;

import dev.lazurite.lattice.api.supplier.ChunkPosSupplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public interface ChunkPosSupplierWrapper extends ChunkPosSupplier {
    ChunkPosSupplier getChunkPosSupplier();
    ServerLevel getServerLevel();

    void setLastChunkPos(final ChunkPos chunkPos);
    ChunkPos getLastChunkPos();

    void setLastLastChunkPos(final ChunkPos chunkPos);
    ChunkPos getLastLastChunkPos();

    /**
     * The chunk where lattice currently holds an extra {@link net.minecraft.server.level.DistanceManager}
     * player entry on this player's behalf, or {@code null} when it holds none.
     *
     * <p>The distance manager keeps a {@code Set} of players per chunk rather than a count, so the entry
     * has to be tracked exactly: a second entry in the chunk the player's own body occupies is
     * indistinguishable from vanilla's, and removing it would unload the chunk the player is standing in.
     */
    void setViewPointTicketChunkPos(final ChunkPos chunkPos);
    ChunkPos getViewPointTicketChunkPos();

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isInSameChunk(final ServerPlayer serverPlayer);

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean wasInSameChunk(final ServerPlayer serverPlayer, final boolean useLastLast);
}
