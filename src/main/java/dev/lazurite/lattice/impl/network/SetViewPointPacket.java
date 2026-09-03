package dev.lazurite.lattice.impl.network;

import dev.lazurite.lattice.api.point.ViewPoint;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public final class SetViewPointPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SetViewPointPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("lattice", "set_viewpoint"));

    public static final StreamCodec<FriendlyByteBuf, SetViewPointPacket> STREAM_CODEC =
        CustomPacketPayload.codec(SetViewPointPacket::write, SetViewPointPacket::decode);

    private final boolean isEntity;
    private final int entityId;

    public SetViewPointPacket(boolean isEntity, int entityId) {
        this.isEntity = isEntity;
        this.entityId = entityId;
    }

    public static SetViewPointPacket fromViewPoint(ViewPoint viewPoint) {
        if (viewPoint instanceof Entity entity) {
            return new SetViewPointPacket(true, entity.getId());
        }
        return new SetViewPointPacket(false, -1);
    }

    public static SetViewPointPacket decode(FriendlyByteBuf buf) {
        boolean isEntity = buf.readBoolean();
        int entityId = isEntity ? buf.readVarInt() : -1;
        return new SetViewPointPacket(isEntity, entityId);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(this.isEntity);
        if (this.isEntity) {
            buf.writeVarInt(this.entityId);
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public boolean isEntity() {
        return isEntity;
    }

    public int getEntityId() {
        return entityId;
    }
}
