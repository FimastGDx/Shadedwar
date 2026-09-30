package com.fullfud.fullfud.core.network.packet;

import com.fullfud.fullfud.core.network.FullfudNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/**
 * C2S: the pilot pressed the autopilot-toggle key (V) while flying the monitor feed. Carries only the drone
 * UUID; the server re-resolves it and gates the flip on the sender actually being the controlling pilot with
 * the monitor open, so — unlike {@link SetShahedAutopilotPacket} — this is allowed while the airframe is armed.
 */
public record ToggleShahedAutopilotPacket(UUID droneId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ToggleShahedAutopilotPacket> TYPE =
        new CustomPacketPayload.Type<>(FullfudNetwork.id("toggle_shahed_autopilot"));

    public static final StreamCodec<FriendlyByteBuf, ToggleShahedAutopilotPacket> STREAM_CODEC =
        CustomPacketPayload.codec(ToggleShahedAutopilotPacket::write, ToggleShahedAutopilotPacket::decode);

    public static ToggleShahedAutopilotPacket decode(final FriendlyByteBuf buffer) {
        return new ToggleShahedAutopilotPacket(buffer.readUUID());
    }

    public void write(final FriendlyByteBuf buffer) {
        buffer.writeUUID(droneId);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
