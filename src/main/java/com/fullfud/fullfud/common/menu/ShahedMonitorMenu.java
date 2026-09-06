package com.fullfud.fullfud.common.menu;

import com.fullfud.fullfud.common.entity.ShahedDroneEntity;
import com.fullfud.fullfud.core.FullfudRegistries;
import com.fullfud.fullfud.core.data.PersistentData;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

public class ShahedMonitorMenu extends AbstractContainerMenu {
    /** Screen-opening payload; replaces the hand-rolled {@code FriendlyByteBuf} reads of the Forge version. */
    public record Data(UUID droneId, int droneEntityId) {
        public static final StreamCodec<io.netty.buffer.ByteBuf, Data> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, Data::droneId,
            ByteBufCodecs.VAR_INT, Data::droneEntityId,
            Data::new
        );
    }

    private final UUID droneId;
    private final int droneEntityId;

    public ShahedMonitorMenu(final int containerId, final Inventory inventory, final Data data) {
        this(containerId, inventory, data == null ? null : data.droneId(), data == null ? -1 : data.droneEntityId());
    }

    public ShahedMonitorMenu(final int containerId, final Inventory inventory, final UUID droneId, final int droneEntityId) {
        super(FullfudRegistries.SHAHED_MONITOR_MENU.get(), containerId);
        this.droneId = droneId;
        this.droneEntityId = droneEntityId;
    }

    public UUID getDroneId() {
        return droneId;
    }

    public int getDroneEntityId() {
        return droneEntityId;
    }

    @Override
    public boolean stillValid(final Player player) {
        if (droneId == null || player == null || !player.isAlive()) {
            return false;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }
        return findDrone(serverPlayer, droneId).isPresent();
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(final Player player) {
        super.removed(player);
        if (!(player instanceof ServerPlayer serverPlayer) || droneId == null) {
            return;
        }
        if (findDrone(serverPlayer, droneId).map(drone -> {
            drone.removeViewer(serverPlayer);
            drone.endRemoteControl(serverPlayer);
            return true;
        }).orElse(false)) {
            return;
        }

        final CompoundTag root = PersistentData.of(serverPlayer);
        if (root.contains(ShahedDroneEntity.PLAYER_REMOTE_TAG, Tag.TAG_COMPOUND)) {
            final CompoundTag tag = root.getCompound(ShahedDroneEntity.PLAYER_REMOTE_TAG);
            ShahedDroneEntity.forceRestoreFromPersistentData(serverPlayer, tag);
            root.remove(ShahedDroneEntity.PLAYER_REMOTE_TAG);
        }
    }

    private static Optional<ShahedDroneEntity> findDrone(final ServerPlayer player, final UUID droneId) {
        final ServerLevel currentLevel = player.serverLevel();
        final Optional<ShahedDroneEntity> local = ShahedDroneEntity.find(currentLevel, droneId);
        if (local.isPresent()) {
            return local;
        }
        if (player.getServer() == null) {
            return Optional.empty();
        }
        for (final ServerLevel level : player.getServer().getAllLevels()) {
            if (level == currentLevel) {
                continue;
            }
            final Optional<ShahedDroneEntity> found = ShahedDroneEntity.find(level, droneId);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }
}
