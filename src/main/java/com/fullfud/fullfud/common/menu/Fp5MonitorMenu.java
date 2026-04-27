package com.fullfud.fullfud.common.menu;

import com.fullfud.fullfud.common.entity.Fp5FlamingoEntity;
import com.fullfud.fullfud.core.FullfudRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

public class Fp5MonitorMenu extends AbstractContainerMenu {
    private final UUID flamingoId;
    private final int flamingoEntityId;
    private final BlockPos targetPos;
    private final boolean launched;

    public Fp5MonitorMenu(final int containerId, final Inventory inventory, final FriendlyByteBuf buffer) {
        this(
            containerId,
            inventory,
            readFlamingoUuid(buffer),
            readEntityId(buffer),
            readTargetPos(buffer),
            readLaunched(buffer)
        );
    }

    public Fp5MonitorMenu(
        final int containerId,
        final Inventory inventory,
        final UUID flamingoId,
        final int flamingoEntityId,
        final BlockPos targetPos,
        final boolean launched
    ) {
        super(FullfudRegistries.FP5_MONITOR_MENU.get(), containerId);
        this.flamingoId = flamingoId;
        this.flamingoEntityId = flamingoEntityId;
        this.targetPos = targetPos == null ? BlockPos.ZERO : targetPos.immutable();
        this.launched = launched;
    }

    public UUID getFlamingoId() {
        return flamingoId;
    }

    public int getFlamingoEntityId() {
        return flamingoEntityId;
    }

    public BlockPos getTargetPos() {
        return targetPos;
    }

    public boolean isLaunched() {
        return launched;
    }

    @Override
    public boolean stillValid(final Player player) {
        if (flamingoId == null || player == null || !player.isAlive()) {
            return false;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }
        return findFlamingo(serverPlayer, flamingoId).isPresent();
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int slot) {
        return ItemStack.EMPTY;
    }

    private static Optional<Fp5FlamingoEntity> findFlamingo(final ServerPlayer player, final UUID flamingoId) {
        final ServerLevel currentLevel = player.serverLevel();
        final Optional<Fp5FlamingoEntity> local = Fp5FlamingoEntity.find(currentLevel, flamingoId);
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
            final Optional<Fp5FlamingoEntity> found = Fp5FlamingoEntity.find(level, flamingoId);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private static UUID readFlamingoUuid(final FriendlyByteBuf buffer) {
        if (buffer == null || buffer.readableBytes() < 16) {
            return null;
        }
        return buffer.readUUID();
    }

    private static int readEntityId(final FriendlyByteBuf buffer) {
        if (buffer == null || buffer.readableBytes() < 4) {
            return -1;
        }
        return buffer.readInt();
    }

    private static BlockPos readTargetPos(final FriendlyByteBuf buffer) {
        if (buffer == null || buffer.readableBytes() < 8) {
            return BlockPos.ZERO;
        }
        return buffer.readBlockPos();
    }

    private static boolean readLaunched(final FriendlyByteBuf buffer) {
        if (buffer == null || !buffer.isReadable()) {
            return false;
        }
        return buffer.readBoolean();
    }
}
