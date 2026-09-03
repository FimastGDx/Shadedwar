package com.fullfud.fullfud.core.data;

import com.fullfud.fullfud.FullfudMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Where every FPV drone was last seen, so a controller can call one back that is no longer in memory.
 *
 * <p>An FPV parked in an unloaded chunk does not exist as an entity — {@code ServerLevel.getEntity} returns
 * nothing for it — which is why re-entering a drone the pilot had flown out and left behind used to fail. The
 * entity itself is on disk with its chunk; all that is missing is somewhere to look up which chunk that is.
 *
 * <p>Kept on the overworld's data storage rather than per-level, because the lookup happens before the level
 * the drone is in is known. Records are written while the drone ticks and dropped when it is destroyed, not
 * when its chunk unloads — an unloaded drone is exactly the case this exists for.
 */
public final class FpvDroneLocations extends SavedData {

    /** Dimension and block position of one drone. */
    public record Location(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private static final String DATA_NAME = FullfudMod.MOD_ID + "_fpv_locations";

    private static final SavedData.Factory<FpvDroneLocations> FACTORY = new SavedData.Factory<>(
        FpvDroneLocations::new,
        FpvDroneLocations::load,
        DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES
    );

    private final Map<UUID, Location> locations = new HashMap<>();

    public FpvDroneLocations() {
    }

    private static FpvDroneLocations load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final FpvDroneLocations data = new FpvDroneLocations();
        final ListTag list = tag.getList("Drones", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            final CompoundTag entry = list.getCompound(i);
            if (!entry.hasUUID("Drone")) {
                continue;
            }
            final ResourceLocation dimensionId = ResourceLocation.tryParse(entry.getString("Dim"));
            if (dimensionId == null) {
                continue;
            }
            data.locations.put(
                entry.getUUID("Drone"),
                new Location(
                    ResourceKey.create(Registries.DIMENSION, dimensionId),
                    new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z"))
                )
            );
        }
        return data;
    }

    public static FpvDroneLocations get(@Nullable final MinecraftServer server) {
        if (server == null) {
            return new FpvDroneLocations();
        }
        final ServerLevel overworld = server.overworld();
        if (overworld == null) {
            return new FpvDroneLocations();
        }
        return overworld.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public void remember(final UUID droneId, final ResourceKey<Level> dimension, final BlockPos pos) {
        final Location previous = locations.get(droneId);
        if (previous != null && previous.dimension() == dimension && previous.pos().equals(pos)) {
            return;
        }
        locations.put(droneId, new Location(dimension, pos));
        setDirty();
    }

    public void forget(final UUID droneId) {
        if (locations.remove(droneId) != null) {
            setDirty();
        }
    }

    @Nullable
    public Location find(final UUID droneId) {
        return locations.get(droneId);
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ListTag list = new ListTag();
        for (final Map.Entry<UUID, Location> entry : locations.entrySet()) {
            final CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID("Drone", entry.getKey());
            entryTag.putString("Dim", entry.getValue().dimension().location().toString());
            entryTag.putInt("X", entry.getValue().pos().getX());
            entryTag.putInt("Y", entry.getValue().pos().getY());
            entryTag.putInt("Z", entry.getValue().pos().getZ());
            list.add(entryTag);
        }
        tag.put("Drones", list);
        return tag;
    }
}
