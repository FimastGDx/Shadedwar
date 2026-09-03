package com.fullfud.fullfud.mixin.client;

import com.fullfud.fullfud.client.FpvClientHandler;
import com.fullfud.fullfud.client.ShahedClientHandler;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stands in for {@code PlayLevelSoundEvent.AtEntity}, which both handlers used to silence the pilot's own
 * body — footsteps, hurt grunts, item swings — while the camera sits on a drone somewhere else.
 *
 * <p>Forge fired that event from this method on both sides; the two listeners only ever cancelled
 * client-side sounds of the local player, so this mixin is declared client-only and the predicates stay
 * where they were. A dedicated server never sees it and keeps playing the sound for everyone else.
 */
@Mixin(Level.class)
public class LevelSoundMixin {

    @Inject(
        method = "playSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 1
    )
    private void fullfud$silencePilotBody(@Nullable final Player player, final Entity entity, final SoundEvent sound, final SoundSource source, final float volume, final float pitch, final CallbackInfo callback) {
        if (FpvClientHandler.shouldCancelEntitySound(entity, source)
            || ShahedClientHandler.shouldCancelEntitySound(entity, source)) {
            callback.cancel();
        }
    }
}
