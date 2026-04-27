package com.fullfud.fullfud.core.network.packet;

import com.fullfud.fullfud.core.network.handler.Fp5NetworkHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record Fp5LaunchPacket(UUID flamingoId, int targetX, int targetY, int targetZ) {

    public static Fp5LaunchPacket decode(final FriendlyByteBuf buffer) {
        return new Fp5LaunchPacket(
            buffer.readUUID(),
            buffer.readInt(),
            buffer.readInt(),
            buffer.readInt()
        );
    }

    public void encode(final FriendlyByteBuf buffer) {
        buffer.writeUUID(flamingoId);
        buffer.writeInt(targetX);
        buffer.writeInt(targetY);
        buffer.writeInt(targetZ);
    }

    public void handle(final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        if (context.getSender() == null || !context.getDirection().getReceptionSide().isServer()) {
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> Fp5NetworkHandlers.handleLaunch(this, context.getSender()));
        context.setPacketHandled(true);
    }
}
