package com.fullfud.fullfud;

import com.fullfud.fullfud.core.ChunkLoadEvents;
import com.fullfud.fullfud.core.DelayedTasks;
import com.fullfud.fullfud.core.FullfudCreativeTabs;
import com.fullfud.fullfud.core.FullfudDataComponents;
import com.fullfud.fullfud.core.FullfudGameRules;
import com.fullfud.fullfud.core.FullfudRegistries;
import com.fullfud.fullfud.core.RemoteControlFailsafe;
import com.fullfud.fullfud.core.RemoteInteractionBlocker;
import com.fullfud.fullfud.core.RemotePlayerProtection;
import com.fullfud.fullfud.core.config.FullfudServerConfig;
import com.fullfud.fullfud.core.data.PersistentData;
import com.fullfud.fullfud.core.network.FullfudNetwork;
import com.fullfud.fullfud.core.worldgen.LithiumOrePlacer;
import net.fabricmc.api.ModInitializer;

/**
 * Common entrypoint, declared as {@code entrypoints.main} in {@code fabric.mod.json}.
 *
 * <p>Forge split this work between the mod constructor and {@code FMLCommonSetupEvent}; Fabric has a
 * single {@code onInitialize()} that runs before any world exists, which is early enough for all of it.
 * The four event registrations at the bottom used to be {@code @Mod.EventBusSubscriber} annotations
 * found by classpath scanning — Fabric does no such scanning, so they are listed here explicitly.
 * {@code ExplosionControl} and {@code DroneExplosionLimiter} are absent on purpose: they are now plain
 * predicates asked by {@code mixin.ExplosionMixin} rather than listeners.
 */
public class FullfudMod implements ModInitializer {
    public static final String MOD_ID = "fullfud";

    @Override
    public void onInitialize() {
        FullfudServerConfig.SPEC.load();

        FullfudGameRules.init();
        PersistentData.init();

        FullfudDataComponents.register();
        FullfudRegistries.register();
        FullfudCreativeTabs.register();

        FullfudNetwork.init();
        // Lattice's own payload type. Must be declared here, on the common side, because Fabric runs
        // the main entrypoint before the client one and LatticeClient's receiver would otherwise throw.
        dev.lazurite.lattice.impl.Networking.init();

        ChunkLoadEvents.register();
        DelayedTasks.register();
        RemoteControlFailsafe.register();
        RemotePlayerProtection.register();
        RemoteInteractionBlocker.register();
        LithiumOrePlacer.register();
    }
}
