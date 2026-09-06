package com.fullfud.fullfud.core.network.packet;

import com.fullfud.fullfud.core.network.FullfudNetwork;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record DroneAudioOneShotPacket(byte droneType,
                                     byte soundKind,
                                     UUID droneId,
                                     double x,
                                     double y,
                                     double z,
                                     float volume,
                                     float pitch) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DroneAudioOneShotPacket> TYPE =
        new CustomPacketPayload.Type<>(FullfudNetwork.id("drone_audio_one_shot"));

    public static final StreamCodec<FriendlyByteBuf, DroneAudioOneShotPacket> STREAM_CODEC =
        CustomPacketPayload.codec(DroneAudioOneShotPacket::write, DroneAudioOneShotPacket::decode);

    public static DroneAudioOneShotPacket decode(final FriendlyByteBuf buffer) {
        final byte type = buffer.readByte();
        final byte kind = buffer.readByte();
        final UUID id = buffer.readUUID();
        final double x = buffer.readDouble();
        final double y = buffer.readDouble();
        final double z = buffer.readDouble();
        final float volume = buffer.readFloat();
        final float pitch = buffer.readFloat();
        return new DroneAudioOneShotPacket(type, kind, id, x, y, z, volume, pitch);
    }

    public void write(final FriendlyByteBuf buffer) {
        buffer.writeByte(droneType);
        buffer.writeByte(soundKind);
        buffer.writeUUID(droneId);
        buffer.writeDouble(x);
        buffer.writeDouble(y);
        buffer.writeDouble(z);
        buffer.writeFloat(volume);
        buffer.writeFloat(pitch);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

