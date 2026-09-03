package com.fullfud.fullfud.core;

import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/**
 * Re-sends the server's lighting for the chunks a warhead just rearranged.
 *
 * <p>A charge clears a crater in one tick. The client is told about the blocks, and its own light engine is
 * supposed to follow, but after a blast of this size it routinely does not: the crater and everything around
 * it stays at the light level it had while the blocks were still there, which reads as pitch black shadow
 * until some later block change happens to relight the section. Placing a single block anywhere nearby
 * fixes it, which is the giveaway — the data was never wrong on the server, only on the client.
 *
 * <p>So rather than fight the client's incremental relighting, this hands it the authoritative arrays.
 * {@link ClientboundLightUpdatePacket} with null section masks means "every section of this chunk", and
 * {@code ClientPacketListener} both queues the data into the light engine and marks the section range dirty
 * for re-render — exactly the redraw that placing a block was triggering by accident.
 *
 * <p>Sent more than once because the server's own light engine finishes asynchronously: the first pass
 * covers the common case, the later ones cover a crater whose sky light was still propagating.
 */
public final class BlastLightRefresh {

    /** Ticks after the blast at which the light arrays are re-sent. */
    private static final int[] RESEND_DELAYS_TICKS = { 4, 20, 60 };

    /**
     * Blocks of slack added to the vanilla explosion radius. Vanilla clears roughly {@code power * 1.3},
     * and removing a wall relights the floor beside it, so the affected area is wider than the crater.
     */
    private static final double LIGHT_BLEED_BLOCKS = 8.0D;

    private BlastLightRefresh(){
    }

    /**
     * @param power the vanilla explosion radius parameter that was passed to {@code Level.explode}
     */
    public static void schedule(final ServerLevel level, final Vec3 center, final float power) {
        if (level == null || center == null || power <= 0.0F) {
            return;
        }
        final double radius = power * 1.3D + LIGHT_BLEED_BLOCKS;
        final int minChunkX = sectionOf(center.x - radius);
        final int maxChunkX = sectionOf(center.x + radius);
        final int minChunkZ = sectionOf(center.z - radius);
        final int maxChunkZ = sectionOf(center.z + radius);
        for (final int delay : RESEND_DELAYS_TICKS) {
            DelayedTasks.schedule(level, delay, () -> resend(level, minChunkX, maxChunkX, minChunkZ, maxChunkZ));
        }
    }

    private static int sectionOf(final double coordinate) {
        return (int) Math.floor(coordinate) >> 4;
    }

    private static void resend(
        final ServerLevel level,
        final int minChunkX,
        final int maxChunkX,
        final int minChunkZ,
        final int maxChunkZ
    ) {
        if (level.players().isEmpty()) {
            return;
        }
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                final ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                ClientboundLightUpdatePacket packet = null;
                for (final ServerPlayer player : level.players()) {
                    // A pilot's tracking view is centred on the drone rather than on the body (lattice), so
                    // this is the one question that answers "would this player's client accept the chunk".
                    if (!player.getChunkTrackingView().contains(chunkPos)) {
                        continue;
                    }
                    if (packet == null) {
                        packet = new ClientboundLightUpdatePacket(chunkPos, level.getLightEngine(), null, null);
                    }
                    player.connection.send(packet);
                }
            }
        }
    }
}
