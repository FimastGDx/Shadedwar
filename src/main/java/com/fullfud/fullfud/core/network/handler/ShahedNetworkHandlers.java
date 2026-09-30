package com.fullfud.fullfud.core.network.handler;

import com.fullfud.fullfud.common.entity.ShahedDroneEntity;
import com.fullfud.fullfud.core.network.packet.SetShahedAutopilotPacket;
import com.fullfud.fullfud.core.network.packet.ShahedControlPacket;
import com.fullfud.fullfud.core.network.packet.ToggleShahedAutopilotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class ShahedNetworkHandlers {
    private ShahedNetworkHandlers() {
    }

    public static void handleControl(final ShahedControlPacket packet, final ServerPlayer sender) {
        if (sender == null) {
            return;
        }
        final ServerLevel level = sender.serverLevel();
        ShahedDroneEntity.find(level, packet.droneId())
            .ifPresent(drone -> drone.applyControl(packet, sender));
    }

    public static void handleSetAutopilot(final SetShahedAutopilotPacket packet, final ServerPlayer sender) {
        if (sender == null) {
            return;
        }
        final ServerLevel level = sender.serverLevel();
        ShahedDroneEntity.find(level, packet.droneId()).ifPresent(drone -> {
            if (drone.canConfigureAutopilot(sender)) {
                drone.applyAutopilotTag(packet.settingsTag());
            }
        });
    }

    /**
     * In-flight autopilot toggle (the V key). Unlike {@link #handleSetAutopilot} this is allowed while the
     * airframe is armed — the drone's own {@code toggleAutopilot} re-checks that the sender is the controlling
     * pilot with the monitor open before flipping the flag.
     */
    public static void handleToggleAutopilot(final ToggleShahedAutopilotPacket packet, final ServerPlayer sender) {
        if (sender == null) {
            return;
        }
        final ServerLevel level = sender.serverLevel();
        ShahedDroneEntity.find(level, packet.droneId())
            .ifPresent(drone -> drone.toggleAutopilot(sender));
    }
}
