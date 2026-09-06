package com.fullfud.fullfud.core.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ShahedLinkData extends SavedData {
    private static final String DATA_NAME = "fullfud_shahed_links";
    /**
     * 1.20.5 replaced {@code computeIfAbsent(Supplier, Function, String)} with a {@link SavedData.Factory}
     * that also names a {@link DataFixTypes}. A mod-owned tag has no registered fixers, and DFU is only
     * asked to walk it when a file predates the current data version, so the constant merely has to be
     * one whose type reference nothing meaningful hangs off — hence RANDOM_SEQUENCES rather than LEVEL.
     */
    private static final SavedData.Factory<ShahedLinkData> FACTORY = new SavedData.Factory<>(
        ShahedLinkData::new,
        ShahedLinkData::load,
        DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES
    );
    private final Map<UUID, UUID> droneOwners = new HashMap<>();

    public ShahedLinkData() {
    }

    private static ShahedLinkData load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ShahedLinkData data = new ShahedLinkData();
        final ListTag list = tag.getList("Links", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            final CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("Drone") && entry.hasUUID("Owner")) {
                data.droneOwners.put(entry.getUUID("Drone"), entry.getUUID("Owner"));
            }
        }
        return data;
    }

    public static ShahedLinkData get(final ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public void link(final UUID droneId, final UUID ownerId) {
        droneOwners.put(droneId, ownerId);
        setDirty();
    }

    public void unlink(final UUID droneId) {
        if (droneOwners.remove(droneId) != null) {
            setDirty();
        }
    }

    public Optional<UUID> owner(final UUID droneId) {
        return Optional.ofNullable(droneOwners.get(droneId));
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ListTag list = new ListTag();
        for (final Map.Entry<UUID, UUID> entry : droneOwners.entrySet()) {
            final CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID("Drone", entry.getKey());
            entryTag.putUUID("Owner", entry.getValue());
            list.add(entryTag);
        }
        tag.put("Links", list);
        return tag;
    }
}
