package com.fullfud.fullfud.core;

import com.fullfud.fullfud.common.entity.FpvDroneEntity;
import com.fullfud.fullfud.common.entity.ShahedDroneEntity;
import com.fullfud.fullfud.core.data.PersistentData;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

/**
 * Ignores the body's own hands while its owner is flying a drone.
 *
 * <p>Registered from {@code FullfudMod}: on Forge this class was an {@code @Mod.EventBusSubscriber}
 * found by annotation scanning, which Fabric does not do. The six Forge listeners become five
 * callbacks — Fabric has no separate "specific" entity-interact variant, and both Forge handlers did
 * the same unconditional cancel, so {@code EntityInteract} and {@code EntityInteractSpecific} merge
 * into {@code UseEntityCallback}. Cancelling is {@code InteractionResult.FAIL}, which stops the
 * vanilla chain without letting it fall through to the next handler. Since 1.21.2 that is the return
 * type of all five callbacks, {@code UseItemCallback} included — {@code InteractionResultHolder} and
 * its item-carrying results are gone.
 */
public final class RemoteInteractionBlocker {
    private RemoteInteractionBlocker() {
    }

    public static void register() {
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) ->
            isRemoteControlled(player) ? InteractionResult.FAIL : InteractionResult.PASS);
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) ->
            isRemoteControlled(player) ? InteractionResult.FAIL : InteractionResult.PASS);
        UseItemCallback.EVENT.register((player, level, hand) ->
            isRemoteControlled(player) ? InteractionResult.FAIL : InteractionResult.PASS);
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) ->
            isRemoteControlled(player) ? InteractionResult.FAIL : InteractionResult.PASS);
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) ->
            isRemoteControlled(player) ? InteractionResult.FAIL : InteractionResult.PASS);
    }

    private static boolean isRemoteControlled(final Player player) {
        if (player == null) {
            return false;
        }
        final CompoundTag root = PersistentData.of(player);
        return root.contains(FpvDroneEntity.PLAYER_REMOTE_TAG, Tag.TAG_COMPOUND)
            || root.contains(ShahedDroneEntity.PLAYER_REMOTE_TAG, Tag.TAG_COMPOUND);
    }
}
