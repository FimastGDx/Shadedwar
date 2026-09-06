package com.fullfud.fullfud;

import com.fullfud.fullfud.client.FpvClientHandler;
import com.fullfud.fullfud.client.ShahedClientHandler;
import com.fullfud.fullfud.client.input.KeyBindMigration;
import com.fullfud.fullfud.core.config.FullfudClientConfig;
import com.fullfud.fullfud.core.network.FullfudClientNetwork;
import dev.lazurite.lattice.impl.client.LatticeClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Client entrypoint, declared as {@code entrypoints.client} in {@code fabric.mod.json}.
 *
 * <p>This replaces Forge's {@code DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)} block and
 * {@code FMLClientSetupEvent} in one: Fabric loads this class only on a physical client, so the
 * ~24 {@code DistExecutor} hops and {@code @Environment(EnvType.CLIENT)} annotations the Forge build needed
 * have no counterpart here.
 */
public final class FullfudClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FullfudClientConfig.SPEC.load();

        FullfudClientNetwork.init();
        LatticeClient.init();

        FpvClientHandler.registerClientEvents();
        ShahedClientHandler.registerClientEvents();

        // Not here and now: key binds are still being collected at this point and options.txt has not
        // necessarily been read, so the collision check has nothing to look at until the game is running.
        ClientTickEvents.END_CLIENT_TICK.register(client -> KeyBindMigration.runOnce());
    }
}
