package com.fullfud.fullfud.core.network.packet;

import com.fullfud.fullfud.core.network.FullfudNetwork;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/**
 * The pilot pressed the detonate key. Carries only the drone's UUID; whether the drone actually blows up
 * is decided entirely server-side — the sender must be its controller and a charge must be installed.
 */
public record FpvDetonatePacket(UUID droneId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FpvDetonatePacket> TYPE =
        new CustomPacketPayload.Type<>(FullfudNetwork.id("fpv_detonate"));

    public static final StreamCodec<FriendlyByteBuf, FpvDetonatePacket> STREAM_CODEC =
        CustomPacketPayload.codec(FpvDetonatePacket::write, FpvDetonatePacket::decode);

    public static FpvDetonatePacket decode(final FriendlyByteBuf buffer) {
        return new FpvDetonatePacket(buffer.readUUID());
    }

    public void write(final FriendlyByteBuf buffer) {
        buffer.writeUUID(droneId);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
