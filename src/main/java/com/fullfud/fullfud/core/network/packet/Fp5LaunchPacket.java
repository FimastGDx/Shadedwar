package com.fullfud.fullfud.core.network.packet;

import com.fullfud.fullfud.core.network.FullfudNetwork;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record Fp5LaunchPacket(UUID flamingoId, int targetX, int targetY, int targetZ) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<Fp5LaunchPacket> TYPE =
        new CustomPacketPayload.Type<>(FullfudNetwork.id("fp5_launch"));

    public static final StreamCodec<FriendlyByteBuf, Fp5LaunchPacket> STREAM_CODEC =
        CustomPacketPayload.codec(Fp5LaunchPacket::write, Fp5LaunchPacket::decode);

    public static Fp5LaunchPacket decode(final FriendlyByteBuf buffer) {
        return new Fp5LaunchPacket(
            buffer.readUUID(),
            buffer.readInt(),
            buffer.readInt(),
            buffer.readInt()
        );
    }

    public void write(final FriendlyByteBuf buffer) {
        buffer.writeUUID(flamingoId);
        buffer.writeInt(targetX);
        buffer.writeInt(targetY);
        buffer.writeInt(targetZ);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
