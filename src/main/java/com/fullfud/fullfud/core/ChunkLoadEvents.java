package com.fullfud.fullfud.core;

import com.fullfud.fullfud.common.entity.ShahedDroneEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;

/**
 * Housekeeping for the drone chunk tickets. Registered from {@code FullfudMod}: on Forge this class was
 * an {@code @Mod.EventBusSubscriber} found by annotation scanning, which Fabric does not do.
 *
 * <p>The entity-load listener is new plumbing rather than a former listener: a Shahed used to re-link and
 * claim its ticket from an {@code onAddedToWorld} override, a method Forge added to {@code Entity}.
 */
public final class ChunkLoadEvents {
    private ChunkLoadEvents() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> ChunkLoadManager.cleanupStaleTickets());
        ServerWorldEvents.UNLOAD.register((server, level) -> ChunkLoadManager.clearLevel(level));
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof ShahedDroneEntity drone) {
                drone.onAddedToServerLevel();
            }
        });
    }
}
