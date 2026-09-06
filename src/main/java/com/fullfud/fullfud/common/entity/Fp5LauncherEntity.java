package com.fullfud.fullfud.common.entity;

import com.fullfud.fullfud.common.item.Fp5FlamingoItem;
import com.fullfud.fullfud.core.EntityDrops;
import net.minecraft.network.syncher.SynchedEntityData;
import com.fullfud.fullfud.common.item.MonitorItem;
import com.fullfud.fullfud.core.FullfudRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class Fp5LauncherEntity extends Entity implements GeoEntity {
    public static final float SCALE = 2.25F;
    private static final String TAG_STORED_FLAMINGO = "StoredFlamingo";
    private static final float HITBOX_WIDTH = 1.125F * SCALE;
    private static final float HITBOX_LENGTH = 7.3125F * SCALE;
    private static final float HITBOX_HEIGHT = 2.75F * SCALE;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int storedFlamingoId = -1;
    private UUID storedFlamingoUuid;

    public Fp5LauncherEntity(final EntityType<? extends Fp5LauncherEntity> type, final Level level) {
        super(type, level);
        this.blocksBuilding = true;
        this.noPhysics = true;
        this.setNoGravity(true);
        updateBoundingBox();
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(final CompoundTag tag) {
        if (tag.hasUUID(TAG_STORED_FLAMINGO)) {
            storedFlamingoUuid = tag.getUUID(TAG_STORED_FLAMINGO);
        }
        updateBoundingBox();
    }

    @Override
    protected void addAdditionalSaveData(final CompoundTag tag) {
        if (storedFlamingoUuid != null) {
            tag.putUUID(TAG_STORED_FLAMINGO, storedFlamingoUuid);
        }
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        resolveStoredFlamingo();
        updateBoundingBox();
    }

    @Override
    public InteractionResult interact(final Player player, final InteractionHand hand) {
        final ItemStack held = player.getItemInHand(hand);
        if (!(level() instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        if (held.getItem() instanceof Fp5FlamingoItem) {
            if (hasFlamingo()) {
                return InteractionResult.FAIL;
            }
            final Fp5FlamingoEntity flamingo = FullfudRegistries.FP5_FLAMINGO_ENTITY.get().create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
            if (flamingo == null) {
                return InteractionResult.FAIL;
            }
            flamingo.mountLauncher(this);
            serverLevel.addFreshEntity(flamingo);
            setStoredFlamingo(flamingo);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            return level().isClientSide ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }
        if (held.getItem() instanceof MonitorItem) {
            final Fp5FlamingoEntity flamingo = getStoredFlamingo();
            if (flamingo == null || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.FAIL;
            }
            MonitorItem.linkAndOpenFp5Monitor(serverPlayer, held, flamingo);
            return level().isClientSide ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }
        if (player.isShiftKeyDown() && held.isEmpty() && hasFlamingo()) {
            ejectStoredFlamingo(player);
            return level().isClientSide ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    // Entity.hurt is final void since 1.21.2 and forwards here only on a ServerLevel, so the former
    // isClientSide half of the guard is implicit.
    @Override
    public boolean hurtServer(final ServerLevel level, final DamageSource source, final float amount) {
        if (!isAlive()) {
            return false;
        }
        dropStoredFlamingoAsItem();
        dropSelf();
        discard();
        return true;
    }

    @Override
    public void remove(final RemovalReason reason) {
        if (!level().isClientSide && reason.shouldDestroy()) {
            dropStoredFlamingoAsItem();
        }
        super.remove(reason);
    }

    public void refreshLauncherBounds() {
        updateBoundingBox();
    }

    @Override
    protected void checkFallDamage(final double y, final boolean onGround, final BlockState state, final BlockPos pos) {
    }

    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    void onStoredFlamingoRemoved(final Fp5FlamingoEntity flamingo) {
        if (flamingo == null) {
            return;
        }
        if ((storedFlamingoUuid != null && storedFlamingoUuid.equals(flamingo.getUUID())) || storedFlamingoId == flamingo.getId()) {
            clearStoredFlamingo();
        }
    }

    private boolean hasFlamingo() {
        return getStoredFlamingo() != null;
    }

    private void resolveStoredFlamingo() {
        if (storedFlamingoId > 0) {
            final Entity entity = level().getEntity(storedFlamingoId);
            if (entity instanceof Fp5FlamingoEntity flamingo
                && flamingo.isAlive()
                && flamingo.isOnLauncher()
                && getUUID().equals(flamingo.getLauncherUuid())) {
                return;
            }
            storedFlamingoId = -1;
        }
        if (storedFlamingoUuid == null || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        final Entity entity = serverLevel.getEntity(storedFlamingoUuid);
        if (entity instanceof Fp5FlamingoEntity flamingo
            && flamingo.isAlive()
            && flamingo.isOnLauncher()
            && getUUID().equals(flamingo.getLauncherUuid())) {
            storedFlamingoId = flamingo.getId();
        } else {
            storedFlamingoUuid = null;
        }
    }

    private Fp5FlamingoEntity getStoredFlamingo() {
        if (storedFlamingoId > 0) {
            final Entity entity = level().getEntity(storedFlamingoId);
            if (entity instanceof Fp5FlamingoEntity flamingo
                && flamingo.isAlive()
                && flamingo.isOnLauncher()
                && getUUID().equals(flamingo.getLauncherUuid())) {
                return flamingo;
            }
            storedFlamingoId = -1;
        }
        if (storedFlamingoUuid != null && level() instanceof ServerLevel serverLevel) {
            final Entity entity = serverLevel.getEntity(storedFlamingoUuid);
            if (entity instanceof Fp5FlamingoEntity flamingo
                && flamingo.isAlive()
                && flamingo.isOnLauncher()
                && getUUID().equals(flamingo.getLauncherUuid())) {
                storedFlamingoId = flamingo.getId();
                return flamingo;
            }
            storedFlamingoUuid = null;
        }
        return null;
    }

    private void setStoredFlamingo(final Fp5FlamingoEntity flamingo) {
        storedFlamingoId = flamingo.getId();
        storedFlamingoUuid = flamingo.getUUID();
    }

    private void clearStoredFlamingo() {
        storedFlamingoId = -1;
        storedFlamingoUuid = null;
    }

    private void ejectStoredFlamingo(final Player player) {
        final Fp5FlamingoEntity flamingo = getStoredFlamingo();
        if (flamingo == null) {
            return;
        }
        final ItemStack stack = flamingo.createItemStack();
        if (!player.addItem(stack)) {
            EntityDrops.spawnAtLocation(this, stack);
        }
        flamingo.suppressItemDrop();
        flamingo.discard();
        clearStoredFlamingo();
    }

    private void dropStoredFlamingoAsItem() {
        final Fp5FlamingoEntity flamingo = getStoredFlamingo();
        if (flamingo == null) {
            return;
        }
        EntityDrops.spawnAtLocation(this, flamingo.createItemStack());
        flamingo.suppressItemDrop();
        flamingo.discard();
        clearStoredFlamingo();
    }

    private void dropSelf() {
        EntityDrops.spawnAtLocation(this, new ItemStack(FullfudRegistries.FP5_LAUNCHER_ITEM.get()));
    }

    private void updateBoundingBox() {
        final float halfWidth = HITBOX_WIDTH * 0.5F;
        final float halfLength = HITBOX_LENGTH * 0.5F;
        final Vec3 center = position();
        final double yawRad = Math.toRadians(getYRot());
        final double cos = Math.cos(yawRad);
        final double sin = Math.sin(yawRad);
        final Vec3[] corners = new Vec3[] {
            new Vec3(-halfWidth, 0.0D, -halfLength),
            new Vec3(halfWidth, 0.0D, -halfLength),
            new Vec3(halfWidth, 0.0D, halfLength),
            new Vec3(-halfWidth, 0.0D, halfLength)
        };
        double minX = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (final Vec3 corner : corners) {
            final double rotX = corner.x * cos - corner.z * sin;
            final double rotZ = corner.x * sin + corner.z * cos;
            minX = Math.min(minX, center.x + rotX);
            minZ = Math.min(minZ, center.z + rotZ);
            maxX = Math.max(maxX, center.x + rotX);
            maxZ = Math.max(maxZ, center.z + rotZ);
        }
        setBoundingBox(new AABB(minX, getY(), minZ, maxX, getY() + HITBOX_HEIGHT, maxZ));
    }
}
