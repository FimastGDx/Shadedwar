package com.fullfud.fullfud.core.network;

import com.fullfud.fullfud.FullfudMod;
import com.fullfud.fullfud.core.network.handler.Fp5NetworkHandlers;
import com.fullfud.fullfud.core.network.handler.FpvNetworkHandlers;
import com.fullfud.fullfud.core.network.handler.ShahedNetworkHandlers;
import com.fullfud.fullfud.core.network.packet.DroneAudioLoopPacket;
import com.fullfud.fullfud.core.network.packet.DroneAudioOneShotPacket;
import com.fullfud.fullfud.core.network.packet.Fp5LaunchPacket;
import com.fullfud.fullfud.core.network.packet.FpvControlPacket;
import com.fullfud.fullfud.core.network.packet.FpvDetonatePacket;
import com.fullfud.fullfud.core.network.packet.FpvReleasePacket;
import com.fullfud.fullfud.core.network.packet.OpenFpvConfiguratorPacket;
import com.fullfud.fullfud.core.network.packet.ShahedControlPacket;
import com.fullfud.fullfud.core.network.packet.ShahedGhostUpdatePacket;
import com.fullfud.fullfud.core.network.packet.ShahedLinkPacket;
import com.fullfud.fullfud.core.network.packet.ShahedStatusPacket;
import com.fullfud.fullfud.core.network.packet.UpdateFpvDroneConfigPacket;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server side of the mod's networking. Client-bound receivers live in {@link FullfudClientNetwork}.
 *
 * <p>Forge's single {@code fullfud:main} {@code SimpleChannel} is gone: on Fabric every payload
 * carries its own channel id, so the implicit sequential packet ids — and the hazard of shifting them
 * by inserting a packet in the middle — no longer exist.
 */
public final class FullfudNetwork {

    /**
     * Wire-protocol generation, prefixed onto every channel id. Forge checked this during the
     * {@code SimpleChannel} handshake; here it means a client built against a different generation
     * simply has none of our channels registered, so {@code canSend} reports false instead of the
     * two sides mis-decoding each other. Bump it whenever a packet's field layout changes.
     */
    public static final String PROTOCOL_VERSION = "5";

    private FullfudNetwork() {
    }

    /** Channel id for a packet, namespaced by mod id and protocol generation. */
    public static ResourceLocation id(final String name) {
        return ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, PROTOCOL_VERSION + "/" + name);
    }

    /**
     * Declares every payload's codec to both sides, then registers the server-bound receivers.
     *
     * <p>The codec registration is direction-scoped and must happen in common init: a payload the
     * receiving side has not registered for that direction is rejected before decode, which is the
     * mechanism {@link #PROTOCOL_VERSION} rides on. Fabric's typed handlers already run on the
     * server thread, so Forge's {@code enqueueWork} has no counterpart here, and the direction
     * checks the packets used to perform are structural: a C2S packet can only arrive through this
     * class.
     *
     * <p>The handlers themselves are unchanged and still re-resolve the target entity by UUID from
     * {@code player.serverLevel()} rather than trusting anything client-supplied.
     */
    public static void init() {
        PayloadTypeRegistry.playC2S().register(ShahedControlPacket.TYPE, ShahedControlPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(Fp5LaunchPacket.TYPE, Fp5LaunchPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(FpvControlPacket.TYPE, FpvControlPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(FpvReleasePacket.TYPE, FpvReleasePacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(FpvDetonatePacket.TYPE, FpvDetonatePacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(UpdateFpvDroneConfigPacket.TYPE, UpdateFpvDroneConfigPacket.STREAM_CODEC);

        PayloadTypeRegistry.playS2C().register(ShahedStatusPacket.TYPE, ShahedStatusPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ShahedGhostUpdatePacket.TYPE, ShahedGhostUpdatePacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ShahedLinkPacket.TYPE, ShahedLinkPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(OpenFpvConfiguratorPacket.TYPE, OpenFpvConfiguratorPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(DroneAudioLoopPacket.TYPE, DroneAudioLoopPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(DroneAudioOneShotPacket.TYPE, DroneAudioOneShotPacket.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ShahedControlPacket.TYPE,
            (packet, context) -> ShahedNetworkHandlers.handleControl(packet, context.player()));
        ServerPlayNetworking.registerGlobalReceiver(Fp5LaunchPacket.TYPE,
            (packet, context) -> Fp5NetworkHandlers.handleLaunch(packet, context.player()));
        ServerPlayNetworking.registerGlobalReceiver(FpvControlPacket.TYPE,
            (packet, context) -> FpvNetworkHandlers.handleControl(packet, context.player()));
        ServerPlayNetworking.registerGlobalReceiver(FpvReleasePacket.TYPE,
            (packet, context) -> FpvNetworkHandlers.handleRelease(packet, context.player()));
        ServerPlayNetworking.registerGlobalReceiver(FpvDetonatePacket.TYPE,
            (packet, context) -> FpvNetworkHandlers.handleDetonate(packet, context.player()));
        ServerPlayNetworking.registerGlobalReceiver(UpdateFpvDroneConfigPacket.TYPE,
            (packet, context) -> FpvNetworkHandlers.handleUpdateConfigurator(packet, context.player()));
    }

    /** Replacement for {@code PacketDistributor.PLAYER.with(() -> player)}. */
    public static void sendToPlayer(final ServerPlayer player, final CustomPacketPayload packet) {
        ServerPlayNetworking.send(player, packet);
    }
}
