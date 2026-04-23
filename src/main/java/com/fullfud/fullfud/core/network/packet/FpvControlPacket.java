package com.fullfud.fullfud.core.network.packet;

import com.fullfud.fullfud.core.network.handler.FpvNetworkHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

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
                               byte armAction) {

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

    public void encode(final FriendlyByteBuf buffer) {
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

    public void handle(final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        if (context.getSender() == null || !context.getDirection().getReceptionSide().isServer()) {
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> FpvNetworkHandlers.handleControl(this, context.getSender()));
        context.setPacketHandled(true);
    }
}
