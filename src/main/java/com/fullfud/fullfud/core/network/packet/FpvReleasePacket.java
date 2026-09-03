package com.fullfud.fullfud.core.network.packet;

import com.fullfud.fullfud.core.network.FullfudNetwork;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record FpvReleasePacket(UUID droneId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FpvReleasePacket> TYPE =
        new CustomPacketPayload.Type<>(FullfudNetwork.id("fpv_release"));

    public static final StreamCodec<FriendlyByteBuf, FpvReleasePacket> STREAM_CODEC =
        CustomPacketPayload.codec(FpvReleasePacket::write, FpvReleasePacket::decode);
    public static FpvReleasePacket decode(final FriendlyByteBuf buffer) {
        return new FpvReleasePacket(buffer.readUUID());
    }

    public void write(final FriendlyByteBuf buffer) {
        buffer.writeUUID(droneId);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
