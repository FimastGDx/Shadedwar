package com.fullfud.fullfud.core.network.handler;

import com.fullfud.fullfud.common.entity.Fp5FlamingoEntity;
import com.fullfud.fullfud.core.network.packet.Fp5LaunchPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

public final class Fp5NetworkHandlers {
    private Fp5NetworkHandlers() {
    }

    public static void handleLaunch(final Fp5LaunchPacket packet, final ServerPlayer sender) {
        if (sender == null || packet == null || packet.flamingoId() == null) {
            return;
        }
        final BlockPos targetPos = new BlockPos(packet.targetX(), packet.targetY(), packet.targetZ());
        findFlamingo(sender, packet.flamingoId()).ifPresent(flamingo -> flamingo.launchToCoordinates(targetPos));
    }

    private static Optional<Fp5FlamingoEntity> findFlamingo(final ServerPlayer sender, final UUID flamingoId) {
        final ServerLevel currentLevel = sender.serverLevel();
        final Optional<Fp5FlamingoEntity> local = Fp5FlamingoEntity.find(currentLevel, flamingoId);
        if (local.isPresent()) {
            return local;
        }
        if (sender.getServer() == null) {
            return Optional.empty();
        }
        for (final ServerLevel level : sender.getServer().getAllLevels()) {
            if (level == currentLevel) {
                continue;
            }
            final Optional<Fp5FlamingoEntity> found = Fp5FlamingoEntity.find(level, flamingoId);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }
}

