package com.fullfud.fullfud.common.item;

import com.fullfud.fullfud.common.entity.FpvDroneEntity;
import com.fullfud.fullfud.common.entity.drone.FpvDroneConfig;
import com.fullfud.fullfud.common.entity.drone.DronePreset;
import com.fullfud.fullfud.core.FullfudRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.EntitySpawnReason;

public class FpvDroneItem extends Item {
    private final DronePreset preset;
    private final double signalRangeScale;
    private final double signalPenetrationScale;
    private final boolean cargo;

    public FpvDroneItem(final Properties properties) {
        this(properties, DronePreset.STANDARD_STRIKE, 1.0D, 1.0D);
    }

    public FpvDroneItem(
        final Properties properties,
        final DronePreset preset,
        final double signalRangeScale,
        final double signalPenetrationScale
    ) {
        this(properties, preset, signalRangeScale, signalPenetrationScale, false);
    }

    public FpvDroneItem(
        final Properties properties,
        final DronePreset preset,
        final double signalRangeScale,
        final double signalPenetrationScale,
        final boolean cargo
    ) {
        super(properties);
        this.preset = preset;
        this.signalRangeScale = signalRangeScale;
        this.signalPenetrationScale = signalPenetrationScale;
        this.cargo = cargo;
    }

    public DronePreset getPreset() {
        return this.preset;
    }

    public boolean isCargo() {
        return this.cargo;
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        final BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());
        final FpvDroneEntity drone = FullfudRegistries.FPV_DRONE_ENTITY.get().create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
        if (drone == null) {
            return InteractionResult.FAIL;
        }
        final Direction facing = context.getHorizontalDirection();
        drone.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY() + 0.05D, spawnPos.getZ() + 0.5D, facing.toYRot(), 0.0F);
        drone.setDronePreset(preset);
        drone.setDroneConfig(FpvDroneConfig.fromPreset(preset));
        drone.setSignalScales(signalRangeScale, signalPenetrationScale);
        // Cargo first: it decides how many slots the bay has, so the restore below would be truncated.
        drone.setCargo(cargo);
        drone.restoreLoadout(context.getItemInHand());
        serverLevel.addFreshEntity(drone);
        if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
            drone.setOwner(serverPlayer);
        }
        if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return serverLevel.isClientSide ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }
}
