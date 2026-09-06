package com.fullfud.fullfud.core.network.handler;

import com.fullfud.fullfud.common.entity.FpvDroneEntity;
import com.fullfud.fullfud.common.entity.drone.FpvDroneConfig;
import com.fullfud.fullfud.core.network.packet.FpvControlPacket;
import com.fullfud.fullfud.core.network.packet.FpvDetonatePacket;
import com.fullfud.fullfud.core.network.packet.FpvReleasePacket;
import com.fullfud.fullfud.core.network.packet.UpdateFpvDroneConfigPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class FpvNetworkHandlers {
    private FpvNetworkHandlers() {
    }

    public static void handleControl(final FpvControlPacket packet, final ServerPlayer sender) {
        if (sender == null) {
            return;
        }
        final ServerLevel level = sender.serverLevel();
        if (level == null) {
            return;
        }
        final var entity = level.getEntity(packet.droneId());
        if (entity instanceof FpvDroneEntity drone) {
            drone.applyControl(packet, sender);
        }
    }

    public static void handleRelease(final FpvReleasePacket packet, final ServerPlayer sender) {
        if (sender == null) {
            return;
        }
        final ServerLevel level = sender.serverLevel();
        if (level == null) {
            return;
        }
        final var entity = level.getEntity(packet.droneId());
        if (entity instanceof FpvDroneEntity drone) {
            drone.requestRelease(sender);
        }
    }

    /**
     * The detonate key. The drone re-checks that the sender is its pilot and that a charge is installed,
     * so a forged packet can at worst blow up a drone the sender is already flying.
     */
    public static void handleDetonate(final FpvDetonatePacket packet, final ServerPlayer sender) {
        if (sender == null) {
            return;
        }
        final ServerLevel level = sender.serverLevel();
        if (level == null) {
            return;
        }
        final var entity = level.getEntity(packet.droneId());
        if (entity instanceof FpvDroneEntity drone) {
            drone.detonateManually(sender);
        }
    }

    public static void handleUpdateConfigurator(final UpdateFpvDroneConfigPacket packet, final ServerPlayer sender) {
        if (sender == null) {
            return;
        }
        final ServerLevel level = sender.serverLevel();
        if (level == null) {
            return;
        }
        final var entity = level.getEntity(packet.droneId());
        if (entity instanceof FpvDroneEntity drone && drone.canAccessPlayer(sender)) {
            drone.setDroneConfig(FpvDroneConfig.fromTag(packet.configTag()));
        }
    }
}
