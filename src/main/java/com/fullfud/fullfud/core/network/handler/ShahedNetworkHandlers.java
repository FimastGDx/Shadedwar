package com.fullfud.fullfud.core.network.handler;

import com.fullfud.fullfud.common.entity.ShahedDroneEntity;
import com.fullfud.fullfud.core.network.packet.ShahedControlPacket;
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



}
