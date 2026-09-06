package dev.lazurite.lattice.impl.mixin.fix.chunk;

import dev.lazurite.lattice.impl.ViewPointHelper;
import dev.lazurite.lattice.impl.api.ChunkPosSupplierWrapper;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

/**
 * Centres a player's chunk tracking on their view point instead of their body.
 *
 * <p>1.20.2 replaced {@code ChunkMap}'s per-chunk {@code updateChunkTracking(player, pos, packet, wasIn, isIn)}
 * calls and the rectangle diffing in {@code move} with a single {@link ChunkTrackingView} per player:
 * {@code updateChunkTracking} builds one centred on {@code player.chunkPosition()} and hands it to
 * {@code applyChunkTrackingView}, which sends the chunk-cache-centre packet, diffs the old view against the new
 * one and stores it on the player. Everything downstream reads that one view — which chunks get sent, which get
 * dropped, {@code isChunkTracked}, {@code isChunkOnTrackedBorder} and therefore {@code getPlayers}.
 *
 * <p>So the twenty-odd injectors this class needed on 1.20.1 collapse into two concerns: build the view around
 * the view point, and keep a second {@link DistanceManager} player entry there so the chunks the view asks for
 * are actually loaded and sendable.
 *
 * <p>One behavioural difference from the 1.20.1 version: it tracked the <em>union</em> of the body's and the
 * view point's square, because it drove the diffing itself. A {@code ChunkTrackingView} is a single square, and
 * the client cannot hold more than one either — its chunk cache is a square around the centre it was last told
 * about — so the body's square is dropped while a view point is bound. Entity visibility is unaffected; that
 * runs off {@code TrackedEntity.updatePlayer}, which lattice already widens to the nearer of body and view
 * point (see {@code fix/misc/TrackedEntityMixin}).
 */
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {

    @Shadow public abstract DistanceManager getDistanceManager();
    @Shadow protected abstract void updateChunkTracking(ServerPlayer serverPlayer);
    @Shadow protected abstract void applyChunkTrackingView(ServerPlayer serverPlayer, ChunkTrackingView chunkTrackingView);
    @Shadow protected abstract int getPlayerViewDistance(ServerPlayer serverPlayer);

    // region euclideanDistanceSquared
    /*
    Used in comparison with a constant (< 16384.0D).
    Returning the smallest number increases the odds that the comparison will pass.
    Same as || in boolean logic.
    */

    @ModifyVariable(
            method = "euclideanDistanceSquared",
            at = @At("STORE"),
            ordinal = 2
    )
    private static double euclideanDistanceSquared_STORE2(double f, ChunkPos chunkPos, Entity entity) {
        double d = SectionPos.sectionToBlockCoord(chunkPos.x, 8);
        final var viewPoint = ViewPointHelper.resolveViewPoint(entity);
        return viewPoint != null ? Math.min(f, d - viewPoint.getX()) : f;
    }

    @ModifyVariable(
            method = "euclideanDistanceSquared",
            at = @At("STORE"),
            ordinal = 3
    )
    private static double euclideanDistanceSquared_STORE3(double g, ChunkPos chunkPos, Entity entity) {
        double e = SectionPos.sectionToBlockCoord(chunkPos.z, 8);
        final var viewPoint = ViewPointHelper.resolveViewPoint(entity);
        return viewPoint != null ? Math.min(g, e - viewPoint.getZ()) : g;
    }

    // endregion euclideanDistanceSquared

    // region updateChunkTracking

    /**
     * Builds the tracking view around the view point rather than the body, and cancels vanilla so it cannot
     * immediately re-centre it. Bails out for players who view themselves, which is every player who is not
     * piloting something, so unbound players keep vanilla behaviour byte for byte.
     */
    @Inject(
            method = "updateChunkTracking",
            at = @At("HEAD"),
            cancellable = true
    )
    private void lattice$updateChunkTracking_HEAD(ServerPlayer serverPlayer, CallbackInfo ci) {
        final ChunkPosSupplierWrapper playerWrapper = lattice$getChunkPosSupplierWrapper(serverPlayer);
        if (playerWrapper == null) {
            return;
        }

        final ChunkPosSupplierWrapper viewPointWrapper = lattice$getViewPointWrapper(serverPlayer, playerWrapper);
        final ChunkPos viewPointChunkPos = viewPointWrapper != null ? viewPointWrapper.getChunkPos() : null;

        lattice$updateViewPointTicket(serverPlayer, playerWrapper, viewPointChunkPos);

        if (viewPointWrapper == null) {
            return;
        }

        // ViewPoint#getDistance lets a view point ask for less than the player's own view distance; it can never
        // ask for more, because the client drops anything outside the radius it was told about at login.
        final int viewDistance = Mth.clamp(viewPointWrapper.getDistance(), 2, this.getPlayerViewDistance(serverPlayer));

        if (!(serverPlayer.getChunkTrackingView() instanceof final ChunkTrackingView.Positioned positioned)
                || !positioned.center().equals(viewPointChunkPos)
                || positioned.viewDistance() != viewDistance) {
            this.applyChunkTrackingView(serverPlayer, ChunkTrackingView.of(viewPointChunkPos, viewDistance));
        }

        ci.cancel();
    }

    // endregion updateChunkTracking

    // region move

    /**
     * Vanilla only refreshes the tracking view when the player's own section changes, so a pilot standing still
     * while their drone flies would never get one. {@code move} is the one hook that already runs every tick for
     * such a player — vanilla calls it for every movement packet, and {@code RemoteControlFailsafe} calls it
     * explicitly for anyone piloting — and {@code updateChunkTracking} is a no-op when the view has not moved,
     * so re-running it unconditionally here is enough.
     */
    @Inject(
            method = "move",
            at = @At("TAIL")
    )
    private void lattice$move_TAIL(ServerPlayer serverPlayer, CallbackInfo ci) {
        this.updateChunkTracking(serverPlayer);
    }

    // endregion move

    // region updatePlayerStatus

    @Inject(
            method = "updatePlayerStatus",
            at = @At("HEAD")
    )
    private void lattice$updatePlayerStatus_HEAD(ServerPlayer serverPlayer, boolean added, CallbackInfo ci) {
        if (added) {
            final var internalLevel = ViewPointHelper.resolveInternalServerLevel(serverPlayer.serverLevel());
            if (internalLevel != null) {
                internalLevel.registerPlayer(serverPlayer);
            }
        }
    }

    @Inject(
            method = "updatePlayerStatus",
            at = @At("TAIL")
    )
    private void lattice$updatePlayerStatus_TAIL(ServerPlayer serverPlayer, boolean added, CallbackInfo ci) {
        if (added) {
            // The tail of updatePlayerStatus calls updateChunkTracking, which has already placed the ticket.
            return;
        }

        // Vanilla has dropped the body's distance manager entry by now; drop ours before the graph node that
        // remembers where it is goes away with the player.
        lattice$releaseViewPointTicket(serverPlayer);

        final var internalLevel = ViewPointHelper.resolveInternalServerLevel(serverPlayer.serverLevel());
        if (internalLevel != null) {
            internalLevel.unregisterPlayer(serverPlayer);
        }
    }

    // endregion updatePlayerStatus

    /**
     * Moves lattice's extra distance manager entry to {@code viewPointChunkPos}, or gives it up entirely when
     * there is no view point left to hold it for.
     *
     * <p>{@code playersPerChunk} is a set of players per chunk, not a count, so lattice must never hold an entry
     * in the chunk the body occupies: it would be the same set element vanilla owns, and releasing it later
     * would unload the chunk the player is standing in. That is also why the entry is only released while it is
     * still outside the body's chunk — once the body walks into it, vanilla owns it.
     */
    @Unique
    private void lattice$updateViewPointTicket(final ServerPlayer serverPlayer,
                                               final ChunkPosSupplierWrapper playerWrapper,
                                               final ChunkPos viewPointChunkPos) {
        final ChunkPos bodyChunkPos = serverPlayer.chunkPosition();
        final ChunkPos wanted = viewPointChunkPos == null || viewPointChunkPos.equals(bodyChunkPos) ? null : viewPointChunkPos;
        final ChunkPos held = playerWrapper.getViewPointTicketChunkPos();

        if (Objects.equals(held, wanted)) {
            return;
        }

        if (held != null && !held.equals(bodyChunkPos)) {
            lattice$safeRemovePlayer(SectionPos.of(held, 0), serverPlayer);
        }

        if (wanted != null) {
            lattice$safeAddPlayer(SectionPos.of(wanted, 0), serverPlayer);
        }

        playerWrapper.setViewPointTicketChunkPos(wanted);
    }

    @Unique
    private void lattice$releaseViewPointTicket(final ServerPlayer serverPlayer) {
        final ChunkPosSupplierWrapper playerWrapper = lattice$getChunkPosSupplierWrapper(serverPlayer);
        if (playerWrapper == null) {
            return;
        }
        lattice$updateViewPointTicket(serverPlayer, playerWrapper, null);
    }

    @Unique
    private void lattice$safeRemovePlayer(final SectionPos sectionPos, final ServerPlayer serverPlayer) {
        try {
            this.getDistanceManager().removePlayer(sectionPos, serverPlayer);
        } catch (RuntimeException ignored) {
            // Some modded mixes (or view-point swaps) can desync the internal set; avoid a hard crash.
        }
    }

    @Unique
    private void lattice$safeAddPlayer(final SectionPos sectionPos, final ServerPlayer serverPlayer) {
        try {
            this.getDistanceManager().addPlayer(sectionPos, serverPlayer);
        } catch (RuntimeException ignored) {
            // Defensive: avoid crashing if the internal set is unexpectedly null.
        }
    }

    @Unique
    private static ChunkPosSupplierWrapper lattice$getChunkPosSupplierWrapper(final ServerPlayer serverPlayer) {
        final var internal = ViewPointHelper.resolveInternalServerPlayer(serverPlayer);
        return internal != null ? internal.getChunkPosSupplierWrapper() : null;
    }

    /**
     * The view point's wrapper, or {@code null} when the player views themselves. The graph stores "no view
     * point" as a self-referential edge, so an unbound player's view point wrapper is their own.
     */
    @Unique
    private static ChunkPosSupplierWrapper lattice$getViewPointWrapper(final ServerPlayer serverPlayer,
                                                                       final ChunkPosSupplierWrapper playerWrapper) {
        final var internal = ViewPointHelper.resolveInternalServerPlayer(serverPlayer);
        if (internal == null) {
            return null;
        }
        final ChunkPosSupplierWrapper viewPointWrapper = internal.getViewpointChunkPosSupplierWrapper();
        if (viewPointWrapper == null || viewPointWrapper.equals(playerWrapper)) {
            return null;
        }
        return viewPointWrapper;
    }



}
