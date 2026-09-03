package com.fullfud.fullfud.core;

import com.fullfud.fullfud.FullfudMod;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Awards the advancements that no data-pack trigger can describe.
 *
 * <p>Most of the tree hangs off {@code minecraft:inventory_changed} and needs no code at all. Six nodes are
 * about something the player <em>did</em> with a drone rather than something they hold, so their JSON carries
 * a single {@code minecraft:impossible} criterion named {@value #CRITERION} and is completed from here.
 */
public final class FullfudAdvancements {

    /** The criterion name every code-granted advancement in {@code data/fullfud/advancement} uses. */
    private static final String CRITERION = "granted";

    public static final String ARM = "arm";
    public static final String KAMIKAZE = "kamikaze";
    public static final String LONG_FLIGHT = "long_flight";
    public static final String SHAHED_LAUNCH = "shahed_launch";
    public static final String SHAHED_STRIKE = "shahed_strike";
    public static final String REB_WARNING = "reb_warning";

    /** Distance from the pilot that counts as a long-range flight, in blocks. */
    public static final double LONG_FLIGHT_BLOCKS = 1000.0D;

    private FullfudAdvancements() {
    }

    /**
     * Completes one of the {@code minecraft:impossible} advancements for {@code player}. Awarding one that is
     * already held is a no-op inside {@code PlayerAdvancements}, so call sites do not have to track state.
     */
    public static void grant(final ServerPlayer player, final String path) {
        if (player == null) {
            return;
        }
        final MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        final AdvancementHolder holder = server.getAdvancements()
            .get(ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, path));
        if (holder == null) {
            return;
        }
        player.getAdvancements().award(holder, CRITERION);
    }
}
