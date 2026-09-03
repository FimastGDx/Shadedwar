package com.fullfud.fullfud.core.network.packet;

import com.fullfud.fullfud.core.network.FullfudNetwork;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record ShahedStatusPacket(UUID droneId,
                                 double x,
                                 double y,
                                 double z,
                                 float yaw,
                                 float pitch,
                                 float thrust,
                                 float noiseLevel,
                                 boolean signalLost,
                                 float airSpeed,
                                 float groundSpeed,
                                 float verticalSpeed,
                                 float angleOfAttack,
                                 float slipAngle,
                                 float fuelKg,
                                 float airDensity) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ShahedStatusPacket> TYPE =
        new CustomPacketPayload.Type<>(FullfudNetwork.id("shahed_status"));

    public static final StreamCodec<FriendlyByteBuf, ShahedStatusPacket> STREAM_CODEC =
        CustomPacketPayload.codec(ShahedStatusPacket::write, ShahedStatusPacket::decode);

    public static ShahedStatusPacket decode(final FriendlyByteBuf buffer) {
        final UUID droneId = buffer.readUUID();
        final double x = buffer.readDouble();
        final double y = buffer.readDouble();
        final double z = buffer.readDouble();
        final float yaw = buffer.readFloat();
        final float pitch = buffer.readFloat();
        final float thrust = buffer.readFloat();
        final float noise = buffer.readFloat();
        final boolean signalLost = buffer.readBoolean();
        final float airSpeed = buffer.readFloat();
        final float groundSpeed = buffer.readFloat();
        final float verticalSpeed = buffer.readFloat();
        final float angleOfAttack = buffer.readFloat();
        final float slipAngle = buffer.readFloat();
        final float fuelKg = buffer.readFloat();
        final float airDensity = buffer.readFloat();
        return new ShahedStatusPacket(
            droneId,
            x,
            y,
            z,
            yaw,
            pitch,
            thrust,
            noise,
            signalLost,
            airSpeed,
            groundSpeed,
            verticalSpeed,
            angleOfAttack,
            slipAngle,
            fuelKg,
            airDensity
        );
    }

    public void write(final FriendlyByteBuf buffer) {
        buffer.writeUUID(droneId);
        buffer.writeDouble(x);
        buffer.writeDouble(y);
        buffer.writeDouble(z);
        buffer.writeFloat(yaw);
        buffer.writeFloat(pitch);
        buffer.writeFloat(thrust);
        buffer.writeFloat(noiseLevel);
        buffer.writeBoolean(signalLost);
        buffer.writeFloat(airSpeed);
        buffer.writeFloat(groundSpeed);
        buffer.writeFloat(verticalSpeed);
        buffer.writeFloat(angleOfAttack);
        buffer.writeFloat(slipAngle);
        buffer.writeFloat(fuelKg);
        buffer.writeFloat(airDensity);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
