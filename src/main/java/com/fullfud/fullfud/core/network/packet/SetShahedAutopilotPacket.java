package com.fullfud.fullfud.core.network.packet;

import com.fullfud.fullfud.core.network.FullfudNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/**
 * C2S: the autopilot settings the player entered in the configurator screen. The server re-resolves the
 * drone by UUID and owner-gates the write, so the {@code CompoundTag} carries data only, never trust.
 */
public record SetShahedAutopilotPacket(UUID droneId, CompoundTag settingsTag) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SetShahedAutopilotPacket> TYPE =
        new CustomPacketPayload.Type<>(FullfudNetwork.id("set_shahed_autopilot"));

    public static final StreamCodec<FriendlyByteBuf, SetShahedAutopilotPacket> STREAM_CODEC =
        CustomPacketPayload.codec(SetShahedAutopilotPacket::write, SetShahedAutopilotPacket::decode);

    public static SetShahedAutopilotPacket decode(final FriendlyByteBuf buffer) {
        return new SetShahedAutopilotPacket(buffer.readUUID(), buffer.readNbt());
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
