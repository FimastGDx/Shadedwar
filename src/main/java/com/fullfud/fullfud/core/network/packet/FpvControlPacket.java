package com.fullfud.fullfud.core.network.packet;

import com.fullfud.fullfud.core.network.FullfudNetwork;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record FpvControlPacket(UUID droneId,
                               float pitchInput,
                               float rollInput,
                               float yawInput,
                               float rollRate,
                               float rollSuper,
                               float rollExpo,
                               float pitchRate,
                               float pitchSuper,
                               float pitchExpo,
                               float yawRate,
                               float yawSuper,
                               float yawExpo,
                               float mousePitchDelta,
                               float mouseRollDelta,
                               float throttle,
                               byte armAction) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FpvControlPacket> TYPE =
        new CustomPacketPayload.Type<>(FullfudNetwork.id("fpv_control"));

    public static final StreamCodec<FriendlyByteBuf, FpvControlPacket> STREAM_CODEC =
        CustomPacketPayload.codec(FpvControlPacket::write, FpvControlPacket::decode);

    public static FpvControlPacket decode(final FriendlyByteBuf buffer) {
        final UUID droneId = buffer.readUUID();
        final float pitch = buffer.readFloat();
        final float roll = buffer.readFloat();
        final float yaw = buffer.readFloat();
        final float rollRate = buffer.readFloat();
        final float rollSuper = buffer.readFloat();
        final float rollExpo = buffer.readFloat();
        final float pitchRate = buffer.readFloat();
        final float pitchSuper = buffer.readFloat();
        final float pitchExpo = buffer.readFloat();
        final float yawRate = buffer.readFloat();
        final float yawSuper = buffer.readFloat();
        final float yawExpo = buffer.readFloat();
        final float mousePitchDelta = buffer.readFloat();
        final float mouseRollDelta = buffer.readFloat();
        final float throttle = buffer.readFloat();
        final byte arm = buffer.readByte();
        return new FpvControlPacket(
            droneId,
            pitch,
            roll,
            yaw,
            rollRate,
            rollSuper,
            rollExpo,
            pitchRate,
            pitchSuper,
            pitchExpo,
            yawRate,
            yawSuper,
            yawExpo,
            mousePitchDelta,
            mouseRollDelta,
            throttle,
            arm
        );
    }

    public void write(final FriendlyByteBuf buffer) {
        buffer.writeUUID(droneId);
        buffer.writeFloat(pitchInput);
        buffer.writeFloat(rollInput);
        buffer.writeFloat(yawInput);
        buffer.writeFloat(rollRate);
        buffer.writeFloat(rollSuper);
        buffer.writeFloat(rollExpo);
        buffer.writeFloat(pitchRate);
        buffer.writeFloat(pitchSuper);
        buffer.writeFloat(pitchExpo);
        buffer.writeFloat(yawRate);
        buffer.writeFloat(yawSuper);
        buffer.writeFloat(yawExpo);
        buffer.writeFloat(mousePitchDelta);
        buffer.writeFloat(mouseRollDelta);
        buffer.writeFloat(throttle);
        buffer.writeByte(armAction);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
