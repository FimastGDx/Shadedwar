package com.fullfud.fullfud.core.network.packet;

import com.fullfud.fullfud.core.network.FullfudNetwork;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record ShahedControlPacket(
    UUID droneId,
    float forward,
    float strafe,
    float vertical,
    float thrustDelta,
    float mousePitchDelta,
    float mouseRollDelta
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ShahedControlPacket> TYPE =
        new CustomPacketPayload.Type<>(FullfudNetwork.id("shahed_control"));

    public static final StreamCodec<FriendlyByteBuf, ShahedControlPacket> STREAM_CODEC =
        CustomPacketPayload.codec(ShahedControlPacket::write, ShahedControlPacket::decode);

    public static ShahedControlPacket decode(final FriendlyByteBuf buffer) {
        final UUID droneId = buffer.readUUID();
        final float forward = buffer.readFloat();
        final float strafe = buffer.readFloat();
        final float vertical = buffer.readFloat();
        final float thrustDelta = buffer.readFloat();
        final float mousePitchDelta = buffer.readFloat();
        final float mouseRollDelta = buffer.readFloat();
        return new ShahedControlPacket(droneId, forward, strafe, vertical, thrustDelta, mousePitchDelta, mouseRollDelta);
    }

    public void write(final FriendlyByteBuf buffer) {
        buffer.writeUUID(droneId);
        buffer.writeFloat(forward);
        buffer.writeFloat(strafe);
        buffer.writeFloat(vertical);
        buffer.writeFloat(thrustDelta);
        buffer.writeFloat(mousePitchDelta);
        buffer.writeFloat(mouseRollDelta);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
