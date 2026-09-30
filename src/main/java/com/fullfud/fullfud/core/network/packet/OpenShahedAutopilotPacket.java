package com.fullfud.fullfud.core.network.packet;

import com.fullfud.fullfud.core.network.FullfudNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/**
 * S2C: tells the client to open the Shahed autopilot configurator for a drone, seeded with its current
 * settings. Mirrors {@link OpenFpvConfiguratorPacket}; the client answers with {@link SetShahedAutopilotPacket}.
 */
public record OpenShahedAutopilotPacket(UUID droneId, CompoundTag settingsTag) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenShahedAutopilotPacket> TYPE =
        new CustomPacketPayload.Type<>(FullfudNetwork.id("open_shahed_autopilot"));

    public static final StreamCodec<FriendlyByteBuf, OpenShahedAutopilotPacket> STREAM_CODEC =
        CustomPacketPayload.codec(OpenShahedAutopilotPacket::write, OpenShahedAutopilotPacket::decode);

    public static OpenShahedAutopilotPacket decode(final FriendlyByteBuf buffer) {
        return new OpenShahedAutopilotPacket(buffer.readUUID(), buffer.readNbt());
    }

    public void write(final FriendlyByteBuf buffer) {
        buffer.writeUUID(droneId);
        buffer.writeNbt(settingsTag);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
