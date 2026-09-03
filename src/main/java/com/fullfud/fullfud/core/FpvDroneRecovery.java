package com.fullfud.fullfud.core;

import com.fullfud.fullfud.common.entity.FpvDroneEntity;
import com.fullfud.fullfud.core.data.FpvDroneLocations;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Takes control of an FPV drone that is not in memory.
 *
 * <p>A drone left in a chunk nobody is near is not an entity any more — it is rows in a region file — so
 * {@code ServerLevel.getEntity} cannot find it and pressing the controller used to report the drone as
 * missing (and, worse, wipe the link). What it needs is a chunk ticket at the drone's last known position
 * and a couple of ticks of patience: entity sections stream in a moment behind the chunk itself.
 *
 * <p>The ticket goes through {@link ChunkLoadManager} keyed on the <em>player's</em> entity id, which no
 * drone can collide with, and is <em>handed over</em> rather than dropped once control starts: see
 * {@link #HANDOFF_HOLD_TICKS}.
 */
public final class FpvDroneRecovery {

    /** Ticks between attempts to resolve the drone once the ticket is in. */
    private static final int RETRY_INTERVAL_TICKS = 5;
    /** Give up after this many attempts. Two seconds is far longer than a chunk load needs. */
    private static final int MAX_ATTEMPTS = 8;
    /** Chunk radius held while waiting. Below 2 the chunk loads but does not tick. */
    private static final int RECOVERY_CHUNK_RADIUS = 2;
    /**
     * How long the ticket stays in after control has started.
     *
     * <p>A drone only installs its own chunk ticket from {@code tick()}, and it only ticks while something
     * keeps its chunk entity-ticking — which, at the moment control begins, is this ticket alone. Releasing
     * it here (as the first version did) unloaded the chunk out from under the pilot on the very next tick:
     * the drone was removed, {@code endRemoteControl} fired, and the flight ended a fraction of a second
     * after it started. Holding on for two seconds gives the drone its first tick, after which its own
     * ticket and the pilot's viewpoint keep the area alive.
     */
    private static final int HANDOFF_HOLD_TICKS = 40;

    /**
     * Which attempt currently owns the ticket for a given player, by UUID. Because the ticket is keyed on
     * the player, a second press of the controller takes the first attempt's ticket over — so only the
     * newest attempt is allowed to retry or release. Server thread only, hence the plain map.
     */
    private static final Map<UUID, Integer> ATTEMPT_GENERATIONS = new HashMap<>();

    private FpvDroneRecovery() {
    }

    /**
     * @return {@code false} when the drone is not recallable at all, i.e. the caller should treat the link
     *         as pointing at something that no longer exists
     */
    public static boolean beginControlWhenLoaded(final ServerPlayer player, final UUID droneId) {
        final MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        final FpvDroneLocations.Location location = FpvDroneLocations.get(server).find(droneId);
        if (location == null) {
            return false;
        }
        final ServerLevel level = server.getLevel(location.dimension());
        if (level == null) {
            return false;
        }
        if (level != player.serverLevel()) {
            player.displayClientMessage(Component.translatable("message.fullfud.fpv.other_dimension"), true);
            return true;
        }

        player.displayClientMessage(Component.translatable("message.fullfud.fpv.loading_drone"), true);
        final int generation = ATTEMPT_GENERATIONS.merge(player.getUUID(), 1, Integer::sum);
        attempt(player, droneId, level, new ChunkPos(location.pos()), 1, generation);
        return true;
    }

    private static void attempt(
        final ServerPlayer player,
        final UUID droneId,
        final ServerLevel level,
        final ChunkPos chunkPos,
        final int attempt,
        final int generation
    ) {
        ChunkLoadManager.ensureChunksLoaded(level, player.getId(), chunkPos, RECOVERY_CHUNK_RADIUS);
        DelayedTasks.schedule(level, RETRY_INTERVAL_TICKS, () -> {
            if (!isCurrent(player, generation)) {
                return;
            }
            if (player.hasDisconnected()) {
                finish(player, level, generation);
                return;
            }
            final Entity entity = level.getEntity(droneId);
            if (entity instanceof FpvDroneEntity drone && !drone.isRemoved()) {
                if (drone.beginControl(player)) {
                    DelayedTasks.schedule(level, HANDOFF_HOLD_TICKS, () -> finish(player, level, generation));
                } else {
                    // Refused — wrong dimension, no goggles, or somebody else is already flying it.
                    // beginControl has told the player which; there is nothing left to hold the chunk for.
                    finish(player, level, generation);
                }
                return;
            }
            if (attempt >= MAX_ATTEMPTS) {
                finish(player, level, generation);
                player.displayClientMessage(Component.translatable("message.fullfud.fpv.recall_failed"), true);
                return;
            }
            attempt(player, droneId, level, chunkPos, attempt + 1, generation);
        });
    }

    private static boolean isCurrent(final ServerPlayer player, final int generation) {
        final Integer current = ATTEMPT_GENERATIONS.get(player.getUUID());
        return current != null && current == generation;
    }

    private static void finish(final ServerPlayer player, final ServerLevel level, final int generation) {
        if (!ATTEMPT_GENERATIONS.remove(player.getUUID(), generation)) {
            return;
        }
        ChunkLoadManager.releaseChunks(level, player.getId());
    }
}
