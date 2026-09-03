package com.fullfud.fullfud.core.network.packet;

import com.fullfud.fullfud.core.network.FullfudNetwork;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record OpenFpvConfiguratorPacket(UUID droneId, CompoundTag configTag) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenFpvConfiguratorPacket> TYPE =
        new CustomPacketPayload.Type<>(FullfudNetwork.id("open_fpv_configurator"));

    public static final StreamCodec<FriendlyByteBuf, OpenFpvConfiguratorPacket> STREAM_CODEC =
        CustomPacketPayload.codec(OpenFpvConfiguratorPacket::write, OpenFpvConfiguratorPacket::decode);
    public static OpenFpvConfiguratorPacket decode(final FriendlyByteBuf buffer) {
        return new OpenFpvConfiguratorPacket(buffer.readUUID(), buffer.readNbt());
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
