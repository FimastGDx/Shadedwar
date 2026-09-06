package com.fullfud.fullfud.core;

import com.fullfud.fullfud.common.entity.FpvDroneEntity;
import com.fullfud.fullfud.common.entity.ShahedDroneEntity;
import com.fullfud.fullfud.common.menu.ShahedMonitorMenu;
import com.fullfud.fullfud.core.data.PersistentData;
import dev.lazurite.lattice.impl.api.level.InternalLatticeServerLevel;
import dev.lazurite.lattice.impl.api.player.InternalLatticeServerPlayer;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Recovery layer for a player whose view is attached to a drone.
 *
 * <p>Registered from {@code FullfudMod}: on Forge this class was an {@code @Mod.EventBusSubscriber}
 * found by annotation scanning, which Fabric does not do. Forge's per-player {@code PlayerTickEvent}
 * has no Fabric counterpart, so the end-of-tick pass walks the player list once per server tick
 * instead; that is the same cadence and the same ordering relative to entity ticking.
 *
 * <p>{@code forceChunkTracking}/{@code forceChunkRefresh} reach into {@code ServerChunkCache} and
 * {@code ChunkMap} by cached reflection with signature-shape matching, and everything here is wrapped
 * in {@code catch (Throwable ignored)}. This file fails silently by design: if remote view behaviour
 * regresses, it will not throw.
 */
public final class RemoteControlFailsafe {
    private RemoteControlFailsafe() { }

    private static java.lang.reflect.Method chunkUpdateMethod;
    private static java.lang.reflect.Field chunkMapField;
    private static java.lang.reflect.Method chunkMapUpdatePlayerStatusMethod;

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private static final String TAG_ORIGIN_DIM = "OriginDim";
    private static final String TAG_ORIGIN_X = "OriginX";
    private static final String TAG_ORIGIN_Y = "OriginY";
    private static final String TAG_ORIGIN_Z = "OriginZ";

    /** Displacement from the session origin, squared, that the body is allowed before it is teleported back. */
    private static final double PIN_TOLERANCE_SQ = 0.5D * 0.5D;
    private static final long PIN_LOG_INTERVAL_MS = 5000L;

    private static long lastPinLogMs;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (final ServerPlayer player : server.getPlayerList().getPlayers()) {
                onPlayerEndTick(player);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> onPlayerLoggedOut(handler.player));
    }

    private static void onPlayerEndTick(final ServerPlayer player) {
        RemotePlayerProtection.tick(player);
        final CompoundTag root = PersistentData.of(player);

        if (root.contains(FpvDroneEntity.PLAYER_REMOTE_TAG, Tag.TAG_COMPOUND)) {
            final CompoundTag tag = root.getCompound(FpvDroneEntity.PLAYER_REMOTE_TAG);
            if (FpvDroneEntity.isRemoteControlActive(player.getServer(), player.getUUID(), tag)) {
                clearLegacyRemotePlayerFlags(player);
                holdPilotBody(player, tag);
                RemotePlayerProtection.touch(player);
                forceChunkTracking(player);
            } else {
                FpvDroneEntity.forceRestoreFromPersistentData(player, tag);
                root.remove(FpvDroneEntity.PLAYER_REMOTE_TAG);
            }
        }

        if (root.contains(ShahedDroneEntity.PLAYER_REMOTE_TAG, Tag.TAG_COMPOUND)) {
            final CompoundTag tag = root.getCompound(ShahedDroneEntity.PLAYER_REMOTE_TAG);
            final boolean active = player.containerMenu instanceof ShahedMonitorMenu menu
                && tag.hasUUID("Drone")
                && menu.getDroneId() != null
                && menu.getDroneId().equals(tag.getUUID("Drone"));
            if (active) {
                clearLegacyRemotePlayerFlags(player);
                holdPilotBody(player, tag);
                RemotePlayerProtection.touch(player);
                forceChunkTracking(player);
            } else {
                ShahedDroneEntity.forceRestoreFromPersistentData(player, tag);
                root.remove(ShahedDroneEntity.PLAYER_REMOTE_TAG);
            }
        }
    }

    /**
     * Parks the pilot's body for the duration of a remote session.
     *
     * <p>The chunk tracking view follows the drone, so once the drone is more than a view distance away
     * neither side has chunks around the body any more: the client finds air wherever it tests for collision
     * and the body free-falls, and nothing in the session ever put it back. Holding it explicitly is cheaper
     * and more robust than trying to keep two chunk squares alive — the body is inert while piloting anyway,
     * since vanilla only feeds movement input to the entity the camera is attached to.
     *
     * <p>Gravity is switched back on by {@link #restoreLegacyRemotePlayerState}, which every release path
     * calls. The teleport is the backstop for a body that has already drifted, and it is deliberately
     * position-only: it keeps the rotation and leaves the camera on the drone.
     */
    private static void holdPilotBody(final ServerPlayer player, final CompoundTag tag) {
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;

        if (!tag.contains(TAG_ORIGIN_X, Tag.TAG_DOUBLE)
            || !tag.contains(TAG_ORIGIN_Y, Tag.TAG_DOUBLE)
            || !tag.contains(TAG_ORIGIN_Z, Tag.TAG_DOUBLE)) {
            return;
        }
        if (tag.contains(TAG_ORIGIN_DIM, Tag.TAG_STRING)) {
            final ResourceLocation originDimension = ResourceLocation.tryParse(tag.getString(TAG_ORIGIN_DIM));
            if (originDimension != null && !originDimension.equals(player.level().dimension().location())) {
                // Somewhere else entirely: the restore path owns cross-dimension recovery.
                return;
            }
        }

        final double x = tag.getDouble(TAG_ORIGIN_X);
        final double y = tag.getDouble(TAG_ORIGIN_Y);
        final double z = tag.getDouble(TAG_ORIGIN_Z);
        if (player.distanceToSqr(x, y, z) <= PIN_TOLERANCE_SQ) {
            return;
        }

        final long now = System.currentTimeMillis();
        if (now - lastPinLogMs >= PIN_LOG_INTERVAL_MS) {
            lastPinLogMs = now;
            LOGGER.warn("[fullfud] Pilot {} left their body behind at {} {} {} (session origin {} {} {}); pinning it back",
                player.getGameProfile().getName(),
                String.format(java.util.Locale.ROOT, "%.1f", player.getX()),
                String.format(java.util.Locale.ROOT, "%.1f", player.getY()),
                String.format(java.util.Locale.ROOT, "%.1f", player.getZ()),
                String.format(java.util.Locale.ROOT, "%.1f", x),
                String.format(java.util.Locale.ROOT, "%.1f", y),
                String.format(java.util.Locale.ROOT, "%.1f", z));
        }
        player.teleportTo(x, y, z);
    }

    private static void onPlayerLoggedOut(final ServerPlayer player) {
        if (player == null) {
            return;
        }
        final CompoundTag root = PersistentData.of(player);
        if (root.contains(FpvDroneEntity.PLAYER_REMOTE_TAG, Tag.TAG_COMPOUND)) {
            final CompoundTag tag = root.getCompound(FpvDroneEntity.PLAYER_REMOTE_TAG);
            RemotePlayerProtection.clear(player);
            FpvDroneEntity.forceReleaseFromPersistentData(player.getServer(), player.getUUID(), tag);
        }
        if (root.contains(ShahedDroneEntity.PLAYER_REMOTE_TAG, Tag.TAG_COMPOUND)) {
            final CompoundTag tag = root.getCompound(ShahedDroneEntity.PLAYER_REMOTE_TAG);
            RemotePlayerProtection.clear(player);
            ShahedDroneEntity.forceReleaseFromPersistentData(player.getServer(), player.getUUID(), tag);
        }
    }

    public static void forceChunkTracking(final ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        final ServerChunkCache chunkSource = serverLevel.getChunkSource();
        try {
            if (chunkUpdateMethod == null) {
                chunkUpdateMethod = resolveChunkUpdateMethod(chunkSource.getClass());
            }
            if (chunkUpdateMethod != null) {
                chunkUpdateMethod.invoke(chunkSource, player);
            }
        } catch (Throwable ignored) {
            // Best-effort: if reflection fails, we keep the player anchored without forcing chunk tracking.
        }
    }

    public static void forceChunkRefresh(final ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        final ServerChunkCache chunkSource = serverLevel.getChunkSource();
        try {
            final Object chunkMap = resolveChunkMap(chunkSource);
            if (chunkMap == null) {
                return;
            }
            if (chunkMapUpdatePlayerStatusMethod == null) {
                chunkMapUpdatePlayerStatusMethod = resolveChunkMapUpdatePlayerStatusMethod(chunkMap.getClass());
            }
            if (chunkMapUpdatePlayerStatusMethod != null) {
                chunkMapUpdatePlayerStatusMethod.invoke(chunkMap, player, true);
            }
        } catch (Throwable ignored) {
            // Best-effort: avoid crashing if reflective access fails.
        }
    }

    public static void resetViewpointChunksToPlayer(final ServerPlayer player) {
        if (!(player instanceof InternalLatticeServerPlayer lattice)) {
            return;
        }
        final ChunkPos pos = player.chunkPosition();
        final var viewWrapper = lattice.getViewpointChunkPosSupplierWrapper();
        if (viewWrapper != null) {
            viewWrapper.setLastChunkPos(pos);
            viewWrapper.setLastLastChunkPos(pos);
        }
        final var playerWrapper = lattice.getChunkPosSupplierWrapper();
        if (playerWrapper != null) {
            playerWrapper.setLastChunkPos(pos);
            playerWrapper.setLastLastChunkPos(pos);
        }
    }

    public static void ensureLatticePlayerRegistered(final ServerPlayer player) {
        if (player == null || !(player.level() instanceof InternalLatticeServerLevel latticeLevel)) {
            return;
        }
        latticeLevel.registerPlayer(player);
        latticeLevel.unbind(player);
    }

    public static void restoreLegacyRemotePlayerState(final ServerPlayer player) {
        restoreLegacyRemotePlayerState(player, true);
    }

    public static void clearLegacyRemotePlayerFlags(final ServerPlayer player) {
        restoreLegacyRemotePlayerState(player, false);
    }

    private static void restoreLegacyRemotePlayerState(final ServerPlayer player, final boolean syncToViewers) {
        if (player == null) {
            return;
        }
        player.setInvisible(false);
        player.setSilent(false);
        player.setNoGravity(false);
        player.noPhysics = false;
        player.fallDistance = 0.0F;
        if (!syncToViewers) {
            return;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        final List<Pair<EquipmentSlot, ItemStack>> equipment = new ArrayList<>();
        for (final EquipmentSlot slot : EquipmentSlot.values()) {
            equipment.add(Pair.of(slot, player.getItemBySlot(slot).copy()));
        }
        final ClientboundSetEquipmentPacket equipmentPacket = new ClientboundSetEquipmentPacket(player.getId(), equipment);
        // Since 1.21.2 the add-entity packet is built from the tracker rather than the entity alone.
        // The real ServerEntity lives in ChunkMap's private tracker map; a throwaway one seeded from
        // the player carries the same position and rotation, which is all this resend needs.
        final ServerEntity trackerView = new ServerEntity(serverLevel, player, 1, false, ignored -> { });
        for (final ServerPlayer viewer : serverLevel.players()) {
            if (viewer == player) {
                continue;
            }
            viewer.connection.send(player.getAddEntityPacket(trackerView));
            viewer.connection.send(equipmentPacket);
        }
    }

    private static java.lang.reflect.Method resolveChunkUpdateMethod(final Class<?> chunkSourceClass) {
        for (final java.lang.reflect.Method method : chunkSourceClass.getDeclaredMethods()) {
            final Class<?>[] params = method.getParameterTypes();
            if (params.length == 1 && params[0] == ServerPlayer.class && method.getReturnType() == void.class) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private static Object resolveChunkMap(final ServerChunkCache chunkSource) throws IllegalAccessException {
        if (chunkMapField == null) {
            for (final java.lang.reflect.Field field : chunkSource.getClass().getDeclaredFields()) {
                if (ChunkMap.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    chunkMapField = field;
                    break;
                }
            }
        }
        return chunkMapField == null ? null : chunkMapField.get(chunkSource);
    }

    private static java.lang.reflect.Method resolveChunkMapUpdatePlayerStatusMethod(final Class<?> chunkMapClass) {
        for (final java.lang.reflect.Method method : chunkMapClass.getDeclaredMethods()) {
            final Class<?>[] params = method.getParameterTypes();
            if (params.length == 2 && params[0] == ServerPlayer.class && params[1] == boolean.class && method.getReturnType() == void.class) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }
}
