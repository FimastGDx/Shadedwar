package dev.lazurite.lattice.impl.client;

import dev.lazurite.lattice.api.point.ViewPoint;
import dev.lazurite.lattice.impl.ViewPointHelper;
import dev.lazurite.lattice.impl.api.player.InternalLatticeLocalPlayer;
import dev.lazurite.lattice.impl.network.SetViewPointPacket;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

public final class LatticeClient {

    public static void init() {
        ClientEntityEvents.ENTITY_LOAD.register(LatticeClient::onEntityLoad);
        ClientTickEvents.START_CLIENT_TICK.register(LatticeClient::onStartClientTick);
        ClientPlayNetworking.registerGlobalReceiver(SetViewPointPacket.TYPE,
            (packet, context) -> handleSetViewPointPacket(packet));
    }

    private static void onEntityLoad(Entity entity, ClientLevel clientLevel) {
        if (entity instanceof LocalPlayer localPlayer && localPlayer instanceof InternalLatticeLocalPlayer internalPlayer) {
            internalPlayer.setViewPointEntityId(localPlayer.getId());
            final ViewPoint selfViewPoint = ViewPointHelper.resolveViewPoint(localPlayer);
            if (selfViewPoint != null) {
                internalPlayer.setViewPoint(selfViewPoint);
            }
        }
    }

    private static void onStartClientTick(Minecraft minecraft) {
        final var localPlayer = minecraft.player;
        final var clientLevel = minecraft.level;
        if (localPlayer == null || clientLevel == null) {
            return;
        }

        if (!(localPlayer instanceof InternalLatticeLocalPlayer internalLatticeLocalPlayer)) {
            return;
        }
        final var localPlayerId = localPlayer.getId();
        final var viewPointEntityId = internalLatticeLocalPlayer.getViewPointEntityId();

        if (viewPointEntityId != localPlayerId) {
            final var viewPoint = internalLatticeLocalPlayer.getViewPoint();

            if (viewPoint instanceof Entity entity) {
                if (viewPointEntityId != entity.getId()) {
                    final var viewPointEntity = clientLevel.getEntity(viewPointEntityId);

                    if (viewPointEntity != null) {
                        minecraft.setCameraEntity(viewPointEntity);
                    }
                }
            } else {
                internalLatticeLocalPlayer.setViewPointEntityId(localPlayerId);
            }
        }
    }

    public static void handleSetViewPointPacket(SetViewPointPacket msg) {
        final var localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) {
            return;
        }
        if (!(localPlayer instanceof InternalLatticeLocalPlayer internalLatticeLocalPlayer)) {
            return;
        }

        if (msg.isEntity()) {
            final var clientLevel = localPlayer.level();
            internalLatticeLocalPlayer.setViewPointEntityId(msg.getEntityId());

            final var entity = clientLevel.getEntity(msg.getEntityId());
            final ViewPoint entityViewPoint = ViewPointHelper.resolveViewPoint(entity);
            if (entityViewPoint != null) {
                internalLatticeLocalPlayer.setViewPoint(entityViewPoint);
            }
        } else {
            internalLatticeLocalPlayer.setViewPointEntityId(localPlayer.getId());
            final ViewPoint selfViewPoint = ViewPointHelper.resolveViewPoint(localPlayer);
            if (selfViewPoint != null) {
                internalLatticeLocalPlayer.setViewPoint(selfViewPoint);
            }
        }
    }
}
