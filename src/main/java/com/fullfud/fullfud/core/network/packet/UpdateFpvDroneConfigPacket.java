package com.fullfud.fullfud.core.network.packet;

import com.fullfud.fullfud.core.network.handler.FpvNetworkHandlers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record UpdateFpvDroneConfigPacket(UUID droneId, CompoundTag configTag) {
    public static UpdateFpvDroneConfigPacket decode(final FriendlyByteBuf buffer) {
        return new UpdateFpvDroneConfigPacket(buffer.readUUID(), buffer.readNbt());
    }

    public void encode(final FriendlyByteBuf buffer) {
        buffer.writeUUID(droneId);
        buffer.writeNbt(configTag);
    }

    public void handle(final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        if (!context.getDirection().getReceptionSide().isServer()) {
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> FpvNetworkHandlers.handleUpdateConfigurator(this, context.getSender()));
        context.setPacketHandled(true);
    }
}
