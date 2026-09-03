package com.fullfud.fullfud.core.network.packet;

import com.fullfud.fullfud.core.network.FullfudNetwork;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record UpdateFpvDroneConfigPacket(UUID droneId, CompoundTag configTag) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<UpdateFpvDroneConfigPacket> TYPE =
        new CustomPacketPayload.Type<>(FullfudNetwork.id("update_fpv_drone_config"));

    public static final StreamCodec<FriendlyByteBuf, UpdateFpvDroneConfigPacket> STREAM_CODEC =
        CustomPacketPayload.codec(UpdateFpvDroneConfigPacket::write, UpdateFpvDroneConfigPacket::decode);
    public static UpdateFpvDroneConfigPacket decode(final FriendlyByteBuf buffer) {
        return new UpdateFpvDroneConfigPacket(buffer.readUUID(), buffer.readNbt());
    }

    public void write(final FriendlyByteBuf buffer) {
        buffer.writeUUID(droneId);
        buffer.writeNbt(configTag);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
