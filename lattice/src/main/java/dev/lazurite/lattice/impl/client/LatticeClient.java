package dev.lazurite.lattice.impl.client;

import dev.lazurite.lattice.api.point.ViewPoint;
import dev.lazurite.lattice.impl.ViewPointHelper;
import dev.lazurite.lattice.impl.api.player.InternalLatticeLocalPlayer;
import dev.lazurite.lattice.impl.network.SetViewPointPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

public final class LatticeClient {

    public static void init() {

        MinecraftForge.EVENT_BUS.addListener(LatticeClient::onEntityJoinLevel);
        MinecraftForge.EVENT_BUS.addListener(LatticeClient::onClientTick);
    }

    private static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide) {
            return;
        }
        if (event.getEntity() instanceof LocalPlayer localPlayer && localPlayer instanceof InternalLatticeLocalPlayer internalPlayer) {
            internalPlayer.setViewPointEntityId(localPlayer.getId());
            final ViewPoint selfViewPoint = ViewPointHelper.resolveViewPoint(localPlayer);
            if (selfViewPoint != null) {
                internalPlayer.setViewPoint(selfViewPoint);
            }
        }
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        final var localPlayer = Minecraft.getInstance().player;
        final var clientLevel = Minecraft.getInstance().level;
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
                        Minecraft.getInstance().setCameraEntity(viewPointEntity);
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
