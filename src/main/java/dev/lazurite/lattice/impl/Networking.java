package dev.lazurite.lattice.impl;

import dev.lazurite.lattice.api.point.ViewPoint;
import dev.lazurite.lattice.impl.network.SetViewPointPacket;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Lattice's single packet. On Forge this needed its own {@code lattice:main} {@code SimpleChannel};
 * on Fabric the payload carries its own channel id, but the type still has to be declared once on
 * both sides before anyone may send it or listen for it — {@code ClientPlayNetworking
 * .registerGlobalReceiver} throws outright if it is not. {@link #init()} does that and is called from
 * the common entrypoint, which Fabric runs before the client one.
 */
public final class Networking {

    public static void init() {
        PayloadTypeRegistry.playS2C().register(SetViewPointPacket.TYPE, SetViewPointPacket.STREAM_CODEC);
    }

    public static void sendSetViewPointPacket(final ServerPlayer serverPlayer, final ViewPoint viewPoint) {
        ServerPlayNetworking.send(serverPlayer, SetViewPointPacket.fromViewPoint(viewPoint));
    }

    private Networking() { }
}
