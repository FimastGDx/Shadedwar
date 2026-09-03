package com.fullfud.fullfud.core;

import com.fullfud.fullfud.core.data.PersistentData;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;

/**
 * Shields a player whose view is attached to a drone from damage dealt by that drone.
 *
 * <p>Registered from {@code FullfudMod}: on Forge this class was an {@code @Mod.EventBusSubscriber}
 * found by annotation scanning, which Fabric does not do. The two Forge listeners
 * ({@code LivingAttackEvent} and {@code LivingHurtEvent}, both at {@code HIGHEST}) collapse into the
 * single {@code ServerLivingEntityEvents.ALLOW_DAMAGE} hook, which runs before the damage is applied
 * at all — so there is no second stage left to zero the amount on.
 *
 * <p>Void and fall damage are also cancelled for the duration of a session, whatever caused them: the
 * body cannot move or react while the pilot is looking through a drone, so those two are always the
 * mod's own doing rather than something the player could have avoided.
 */
public final class RemotePlayerProtection {
    private static final String ROOT_TAG = "fullfud_remote_protection";
    private static final String TAG_EXPIRES_AT = "ExpiresAt";
    private static final String TAG_DRONE_UUID = "Drone";
    private static final String TAG_DRONE_DIM = "DroneDim";
    private static final String TAG_HAZARD_DRONE_UUID = "fullfud_protected_drone";
    private static final String TAG_HAZARD_DRONE_DIM = "fullfud_protected_drone_dim";
    private static final long DURATION_TICKS = 20L;

    private RemotePlayerProtection() {
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
            !(entity instanceof ServerPlayer player) || !shouldCancelDamage(player, source));
    }

    public static void touch(final ServerPlayer player) {
        touch(player, null, 0.0D);
    }

    public static void touch(final ServerPlayer player, final Entity drone, final double radius) {
        if (player == null) {
            return;
        }
        final CompoundTag root = PersistentData.of(player);
        final CompoundTag protectionTag = root.contains(ROOT_TAG, Tag.TAG_COMPOUND)
            ? root.getCompound(ROOT_TAG)
            : new CompoundTag();
        protectionTag.putLong(TAG_EXPIRES_AT, currentTick(player) + DURATION_TICKS);
        if (drone != null) {
            protectionTag.putUUID(TAG_DRONE_UUID, drone.getUUID());
            protectionTag.putString(TAG_DRONE_DIM, drone.level().dimension().location().toString());
        }
        root.put(ROOT_TAG, protectionTag);
        player.fallDistance = 0.0F;
    }

    public static void markHazard(final Entity hazard, final Entity drone) {
        if (hazard == null || drone == null) {
            return;
        }
        final CompoundTag root = PersistentData.of(hazard);
        root.putUUID(TAG_HAZARD_DRONE_UUID, drone.getUUID());
        root.putString(TAG_HAZARD_DRONE_DIM, drone.level().dimension().location().toString());
    }

    public static void copyHazardTag(final Entity target, final Entity source) {
        if (target == null || source == null) {
            return;
        }
        final CompoundTag sourceTag = PersistentData.of(source);
        if (!sourceTag.hasUUID(TAG_HAZARD_DRONE_UUID) || !sourceTag.contains(TAG_HAZARD_DRONE_DIM, Tag.TAG_STRING)) {
            return;
        }
        final CompoundTag targetTag = PersistentData.of(target);
        targetTag.putUUID(TAG_HAZARD_DRONE_UUID, sourceTag.getUUID(TAG_HAZARD_DRONE_UUID));
        targetTag.putString(TAG_HAZARD_DRONE_DIM, sourceTag.getString(TAG_HAZARD_DRONE_DIM));
    }

    public static void tick(final ServerPlayer player) {
        if (player == null) {
            return;
        }
        final CompoundTag root = PersistentData.of(player);
        if (!root.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            return;
        }
        final CompoundTag protectionTag = root.getCompound(ROOT_TAG);
        if (currentTick(player) > protectionTag.getLong(TAG_EXPIRES_AT)) {
            clear(player);
            return;
        }
        player.fallDistance = 0.0F;
    }

    public static void clear(final ServerPlayer player) {
        if (player == null) {
            return;
        }
        player.fallDistance = 0.0F;
        PersistentData.of(player).remove(ROOT_TAG);
    }

    private static boolean shouldCancelDamage(final ServerPlayer player, final DamageSource source) {
        if (player == null || source == null) {
            return false;
        }
        if (source.is(DamageTypes.GENERIC_KILL)) {
            return false;
        }
        if (activeProtectionTag(player) == null) {
            return false;
        }
        // The pilot's body is parked and unable to react while the session runs (RemoteControlFailsafe pins it),
        // so anything that only happens to a body left standing in place is on us, not on the player. The void
        // one is not hypothetical: before the body was pinned it fell out of the world mid-flight.
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.FALL)) {
            return true;
        }
        final ProtectedDrone protectedDrone = resolveProtectedDrone(player);
        if (protectedDrone == null) {
            return false;
        }
        return matchesProtectedDrone(protectedDrone, source.getDirectEntity())
            || matchesProtectedDrone(protectedDrone, source.getEntity());
    }

    /** The protection tag while it is still live, or {@code null} — clearing it once it has expired. */
    private static CompoundTag activeProtectionTag(final ServerPlayer player) {
        final CompoundTag root = PersistentData.of(player);
        if (!root.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            return null;
        }
        final CompoundTag protectionTag = root.getCompound(ROOT_TAG);
        if (currentTick(player) > protectionTag.getLong(TAG_EXPIRES_AT)) {
            clear(player);
            return null;
        }
        return protectionTag;
    }

    private static ProtectedDrone resolveProtectedDrone(final ServerPlayer player) {
        final CompoundTag protectionTag = activeProtectionTag(player);
        if (protectionTag == null) {
            return null;
        }
        if (!protectionTag.hasUUID(TAG_DRONE_UUID) || !protectionTag.contains(TAG_DRONE_DIM, Tag.TAG_STRING)) {
            return null;
        }
        final ResourceLocation dimensionId = ResourceLocation.tryParse(protectionTag.getString(TAG_DRONE_DIM));
        if (dimensionId == null) {
            return null;
        }
        return new ProtectedDrone(protectionTag.getUUID(TAG_DRONE_UUID), dimensionId);
    }

    private static boolean matchesProtectedDrone(final ProtectedDrone protectedDrone, final Entity sourceEntity) {
        if (protectedDrone == null || sourceEntity == null) {
            return false;
        }
        if (sourceEntity.getUUID().equals(protectedDrone.droneId)
            && sourceEntity.level().dimension().location().equals(protectedDrone.dimensionId)) {
            return true;
        }
        final CompoundTag root = PersistentData.of(sourceEntity);
        return root.hasUUID(TAG_HAZARD_DRONE_UUID)
            && root.getUUID(TAG_HAZARD_DRONE_UUID).equals(protectedDrone.droneId)
            && root.contains(TAG_HAZARD_DRONE_DIM, Tag.TAG_STRING)
            && protectedDrone.dimensionId.toString().equals(root.getString(TAG_HAZARD_DRONE_DIM));
    }

    private static long currentTick(final ServerPlayer player) {
        return player.serverLevel().getGameTime();
    }

    private record ProtectedDrone(java.util.UUID droneId, ResourceLocation dimensionId) {
    }
}
