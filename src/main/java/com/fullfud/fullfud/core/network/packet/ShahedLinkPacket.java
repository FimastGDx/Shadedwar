package com.fullfud.fullfud.core.network.packet;

import com.fullfud.fullfud.core.network.FullfudNetwork;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record ShahedLinkPacket(UUID droneId, boolean linked) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ShahedLinkPacket> TYPE =
        new CustomPacketPayload.Type<>(FullfudNetwork.id("shahed_link"));

    public static final StreamCodec<FriendlyByteBuf, ShahedLinkPacket> STREAM_CODEC =
        CustomPacketPayload.codec(ShahedLinkPacket::write, ShahedLinkPacket::decode);

    public static ShahedLinkPacket decode(final FriendlyByteBuf buffer) {
        final UUID droneId = buffer.readUUID();
        final boolean linked = buffer.readBoolean();
        return new ShahedLinkPacket(droneId, linked);
    }

    public void write(final FriendlyByteBuf buffer) {
        buffer.writeUUID(droneId);
        buffer.writeBoolean(linked);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
