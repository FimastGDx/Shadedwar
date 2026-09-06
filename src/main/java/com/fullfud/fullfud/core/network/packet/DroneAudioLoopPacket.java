package com.fullfud.fullfud.core.network.packet;

import com.fullfud.fullfud.core.network.FullfudNetwork;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record DroneAudioLoopPacket(byte droneType,
                                  UUID droneId,
                                  double x,
                                  double y,
                                  double z,
                                  float volume,
                                  float pitch,
                                  boolean active) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DroneAudioLoopPacket> TYPE =
        new CustomPacketPayload.Type<>(FullfudNetwork.id("drone_audio_loop"));

    public static final StreamCodec<FriendlyByteBuf, DroneAudioLoopPacket> STREAM_CODEC =
        CustomPacketPayload.codec(DroneAudioLoopPacket::write, DroneAudioLoopPacket::decode);

    public static DroneAudioLoopPacket decode(final FriendlyByteBuf buffer) {
        final byte type = buffer.readByte();
        final UUID id = buffer.readUUID();
        final double x = buffer.readDouble();
        final double y = buffer.readDouble();
        final double z = buffer.readDouble();
        final float volume = buffer.readFloat();
        final float pitch = buffer.readFloat();
        final boolean active = buffer.readBoolean();
        return new DroneAudioLoopPacket(type, id, x, y, z, volume, pitch, active);
    }

    public void write(final FriendlyByteBuf buffer) {
        buffer.writeByte(droneType);
        buffer.writeUUID(droneId);
        buffer.writeDouble(x);
        buffer.writeDouble(y);
        buffer.writeDouble(z);
        buffer.writeFloat(volume);
        buffer.writeFloat(pitch);
        buffer.writeBoolean(active);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

