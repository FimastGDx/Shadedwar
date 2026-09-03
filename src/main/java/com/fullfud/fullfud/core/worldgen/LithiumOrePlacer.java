package com.fullfud.fullfud.core.worldgen;

import com.fullfud.fullfud.FullfudMod;
import com.fullfud.fullfud.core.FullfudRegistries;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Random;

/**
 * Places lithium ore on chunk <em>load</em> rather than through a placed feature.
 *
 * <p>The obvious implementation — a {@code configured_feature} plus {@code placed_feature} added to the
 * overworld biomes — only ever runs while a chunk is being generated, and lithium is a required
 * ingredient for FPV batteries. A player who adds the mod to a world they have already been playing
 * would have to walk out past their explored area to find any, which is exactly what requirement 6 of
 * the survival brief rules out. So a retro-generation pass is mandatory either way.
 *
 * <p>Running <em>both</em> a feature and a retro pass is what invites double placement: the marker that
 * tells the retro pass "this chunk was generated with the feature" is not yet set when Fabric fires
 * {@link ServerChunkEvents#CHUNK_LOAD} for a freshly generated chunk. Rather than race that, there is
 * one code path for both cases. Every chunk gets its ore the first time it is loaded with the mod
 * installed, old and new alike, and the {@link #PLACED} attachment — persistent, so it survives a
 * restart with the chunk — makes that once-only.
 *
 * <p>The RNG is seeded from the level seed and the chunk coordinates the way vanilla seeds decoration,
 * so the same chunk gets the same veins no matter when it is first loaded.
 */
public final class LithiumOrePlacer {

    /**
     * Which generation of the placer has run in a chunk. Stored rather than a boolean so that changing
     * the vein parameters later can re-run over old chunks by bumping the constant, instead of needing
     * a second attachment.
     */
    private static final int GENERATION = 1;

    private static final AttachmentType<Integer> PLACED = AttachmentRegistry.createPersistent(
        ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "lithium_ore_placed"),
        Codec.INT
    );

    /** Height band from the brief. Both bounds inclusive. */
    private static final int MIN_Y = 10;
    private static final int MAX_Y = 20;

    /** Veins per chunk, inclusive range. Iron-like frequency, but each vein is much smaller. */
    private static final int MIN_VEINS = 3;
    private static final int MAX_VEINS = 5;

    /** Blocks per vein, inclusive range. "Smaller deposits" from the brief — vanilla iron veins are 9. */
    private static final int MIN_VEIN_SIZE = 2;
    private static final int MAX_VEIN_SIZE = 4;

    private LithiumOrePlacer() {
    }

    public static void register() {
        ServerChunkEvents.CHUNK_LOAD.register(LithiumOrePlacer::onChunkLoad);
    }

    private static void onChunkLoad(final ServerLevel level, final LevelChunk chunk) {
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return;
        }
        if (chunk.getAttachedOrElse(PLACED, 0) >= GENERATION) {
            return;
        }
        chunk.setAttached(PLACED, GENERATION);
        place(level, chunk);
    }

    private static void place(final ServerLevel level, final LevelChunk chunk) {
        final ChunkPos chunkPos = chunk.getPos();
        final Random random = new Random(
            level.getSeed() ^ (chunkPos.x * 341873128712L + chunkPos.z * 132897987541L) ^ 0x4C69746869L
        );
        final BlockState ore = FullfudRegistries.LITHIUM_ORE_BLOCK.get().defaultBlockState();
        final int veins = MIN_VEINS + random.nextInt(MAX_VEINS - MIN_VEINS + 1);
        boolean changed = false;
        for (int vein = 0; vein < veins; vein++) {
            final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(
                chunkPos.getMinBlockX() + random.nextInt(16),
                MIN_Y + random.nextInt(MAX_Y - MIN_Y + 1),
                chunkPos.getMinBlockZ() + random.nextInt(16)
            );
            final int size = MIN_VEIN_SIZE + random.nextInt(MAX_VEIN_SIZE - MIN_VEIN_SIZE + 1);
            for (int block = 0; block < size; block++) {
                changed |= replaceIfStone(chunk, cursor, ore);
                // Random walk, kept inside the chunk and the height band so no neighbour is touched:
                // a cross-chunk write during CHUNK_LOAD could hit a chunk that is not loaded.
                cursor.set(
                    clamp(cursor.getX() + random.nextInt(3) - 1, chunkPos.getMinBlockX(), chunkPos.getMaxBlockX()),
                    clamp(cursor.getY() + random.nextInt(3) - 1, MIN_Y, MAX_Y),
                    clamp(cursor.getZ() + random.nextInt(3) - 1, chunkPos.getMinBlockZ(), chunkPos.getMaxBlockZ())
                );
            }
        }
        if (changed) {
            chunk.markUnsaved();
        }
    }

    /**
     * Writes through the chunk rather than the level: no block update, no neighbour notification and no
     * client packet, which is what we want for a swap deep underground that no player can be watching.
     * Only the two ore-replaceable tags are eligible, so caves, water, spawners and player builds in the
     * band are all left alone.
     */
    private static boolean replaceIfStone(
        final LevelChunk chunk,
        final BlockPos pos,
        final BlockState ore
    ) {
        final BlockState existing = chunk.getBlockState(pos);
        if (!existing.is(BlockTags.STONE_ORE_REPLACEABLES) && !existing.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) {
            return false;
        }
        return chunk.setBlockState(pos, ore, false) != null;
    }

    private static int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }
}
