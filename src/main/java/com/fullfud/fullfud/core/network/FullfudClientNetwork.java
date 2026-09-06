package com.fullfud.fullfud.core.network;

import com.fullfud.fullfud.client.DroneAudioClientHandler;
import com.fullfud.fullfud.client.FpvClientHandler;
import com.fullfud.fullfud.client.ShahedClientHandler;
import com.fullfud.fullfud.common.entity.drone.FpvDroneConfig;
import com.fullfud.fullfud.core.network.packet.DroneAudioLoopPacket;
import com.fullfud.fullfud.core.network.packet.DroneAudioOneShotPacket;
import com.fullfud.fullfud.core.network.packet.OpenFpvConfiguratorPacket;
import com.fullfud.fullfud.core.network.packet.ShahedGhostUpdatePacket;
import com.fullfud.fullfud.core.network.packet.ShahedLinkPacket;
import com.fullfud.fullfud.core.network.packet.ShahedStatusPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client side of the mod's networking: the six client-bound receivers plus the send-to-server helper.
 * Kept apart from {@link FullfudNetwork} so the side split stays honest — {@code ClientPlayNetworking}
 * must never be loaded on a dedicated server.
 *
 * <p>The client handlers are called straight from here. On Forge the client-bound packets went through
 * bridge methods in {@code core.network.handler} that wrapped the call in
 * {@code DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)}; this class only exists on a client, so the
 * hop had nothing left to guard against and the bridges are gone.
 */
@Environment(EnvType.CLIENT)
public final class FullfudClientNetwork {

    private FullfudClientNetwork() {
    }

    /**
     * Called from the client initializer. Handlers run on the render thread, as on Forge. The
     * payload codecs themselves are declared in {@link FullfudNetwork#init()}, which runs on both
     * sides.
     */
    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(ShahedStatusPacket.TYPE,
            (packet, context) -> ShahedClientHandler.handleStatusPacket(packet));
        ClientPlayNetworking.registerGlobalReceiver(ShahedGhostUpdatePacket.TYPE,
            (packet, context) -> ShahedClientHandler.handleGhostPacket(packet));
        ClientPlayNetworking.registerGlobalReceiver(ShahedLinkPacket.TYPE,
            (packet, context) -> ShahedClientHandler.handleLinkPacket(packet));
        ClientPlayNetworking.registerGlobalReceiver(OpenFpvConfiguratorPacket.TYPE,
            (packet, context) -> FpvClientHandler.openConfigurator(
                packet.droneId(), FpvDroneConfig.fromTag(packet.configTag())));
        ClientPlayNetworking.registerGlobalReceiver(DroneAudioLoopPacket.TYPE,
            (packet, context) -> DroneAudioClientHandler.handleLoop(packet));
        ClientPlayNetworking.registerGlobalReceiver(DroneAudioOneShotPacket.TYPE,
            (packet, context) -> DroneAudioClientHandler.handleOneShot(packet));
    }

    /**
     * Replacement for {@code FullfudNetwork.getChannel().sendToServer(packet)}. Silently drops the
     * packet when the server has not registered the channel, which is what the Forge channel did on a
     * protocol mismatch.
     */
    public static void sendToServer(final CustomPacketPayload packet) {
        if (ClientPlayNetworking.canSend(packet.type())) {
            ClientPlayNetworking.send(packet);
        }
    }
}
