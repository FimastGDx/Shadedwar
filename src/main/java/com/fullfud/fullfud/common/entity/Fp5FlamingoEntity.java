package com.fullfud.fullfud.common.entity;

import com.fullfud.fullfud.common.item.MonitorItem;
import com.fullfud.fullfud.core.FullfudRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;
import java.util.UUID;

public class Fp5FlamingoEntity extends Entity implements GeoEntity {
    public static final float SCALE = 2.25F;

    private static final EntityDataAccessor<Boolean> DATA_ON_LAUNCHER = SynchedEntityData.defineId(Fp5FlamingoEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_SERVER_YAW = SynchedEntityData.defineId(Fp5FlamingoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SERVER_PITCH = SynchedEntityData.defineId(Fp5FlamingoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SERVER_ROLL = SynchedEntityData.defineId(Fp5FlamingoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDimensions FLAMINGO_SIZE = EntityDimensions.scalable(1.9375F * SCALE, 1.2F * SCALE);

    private static final String TAG_ON_LAUNCHER = "OnLauncher";
    private static final String TAG_LAUNCHER_UUID = "Launcher";
    private static final String TAG_LAUNCHED = "Launched";
    private static final String TAG_HAS_TARGET = "HasTarget";
    private static final String TAG_TARGET_X = "TargetX";
    private static final String TAG_TARGET_Y = "TargetY";
    private static final String TAG_TARGET_Z = "TargetZ";
    private static final String TAG_FLIGHT_PHASE = "FlightPhase";
    private static final String TAG_CRUISE_ALTITUDE = "CruiseAltitude";
    private static final String TAG_CLIMB_X = "ClimbWaypointX";
    private static final String TAG_CLIMB_Z = "ClimbWaypointZ";
    private static final String TAG_FLIGHT_SPEED = "FlightSpeed";
    private static final String TAG_LAUNCH_ORIGIN_X = "LaunchOriginX";
    private static final String TAG_LAUNCH_ORIGIN_Z = "LaunchOriginZ";
    private static final String TAG_LAUNCH_LOCKED_YAW = "LaunchLockedYaw";
    private static final String TAG_BODY_ROLL = "BodyRoll";
    private static final String TAG_YAW_RATE = "YawRate";
    private static final String TAG_LAUNCH_TICKS = "LaunchTicks";
    private static final String TAG_PITCH_RATE = "PitchRate";

    private static final float HITBOX_WIDTH = 1.9375F * SCALE;
    private static final float HITBOX_LENGTH = 5.5F * SCALE;
    private static final float HITBOX_HEIGHT = 1.2F * SCALE;
    private static final double LAUNCHER_MOUNT_VERTICAL_OFFSET = 0.0D;
    private static final double LAUNCHER_MOUNT_FORWARD_OFFSET = 0.15D * SCALE;
    private static final float MODEL_FORWARD_YAW_OFFSET = 180.0F;
    private static final float MODEL_BASE_PITCH_DEGREES = 12.5F;

    private static final int FLIGHT_PHASE_IDLE = 0;
    private static final int FLIGHT_PHASE_CLIMB = 1;
    private static final int FLIGHT_PHASE_CRUISE = 2;
    private static final int FLIGHT_PHASE_TERMINAL = 3;

    private static final double CLIMB_TARGET_FORWARD = 24.0D;
    private static final double INITIAL_STRAIGHT_DISTANCE = 200.0D;
    private static final double TURN_AUTHORITY_RAMP_DISTANCE = 160.0D;
    private static final double CLIMB_ALTITUDE_MIN = 20.0D;
    private static final double CLIMB_ALTITUDE_RANDOM = 31.0D;
    private static final double TARGET_ALTITUDE_BUFFER = 8.0D;
    private static final double CLIMB_ALTITUDE_RESPONSE_DISTANCE = 18.0D;
    private static final double CRUISE_ALTITUDE_RESPONSE_DISTANCE = 40.0D;
    private static final int BOOSTER_BURN_TICKS = 14;
    private static final int BOOSTER_DECAY_TICKS = 32;
    private static final double BOOSTER_PEAK_SPEED = 4.6D;
    private static final double BOOSTER_SETTLE_SPEED = 1.05D;
    private static final double BOOSTER_ACCEL_STEP = 0.36D;
    private static final double BOOSTER_DECAY_STEP = 0.16D;
    private static final double CLIMB_SPEED_TARGET = 1.35D;
    private static final double CRUISE_SPEED_TARGET = 2.0D;
    private static final double TERMINAL_SPEED_TARGET = 2.28D;
    private static final double MIN_SPEED_STEP = 0.015D;
    private static final double MAX_SPEED_STEP = 0.06D;
    private static final float CLIMB_PITCH_RATE_LIMIT = 0.85F;
    private static final float CRUISE_PITCH_RATE_LIMIT = 0.45F;
    private static final float TERMINAL_PITCH_RATE_LIMIT = 1.2F;
    private static final float PITCH_RATE_RESPONSE = 0.09F;
    private static final float CLIMB_BANK_LIMIT = 4.0F;
    private static final float CRUISE_BANK_LIMIT = 8.0F;
    private static final float TERMINAL_BANK_LIMIT = 12.0F;
    private static final float BANK_RESPONSE_STEP = 0.55F;
    private static final double BANK_LATERAL_FACTOR = 0.07D;
    private static final double CRUISE_STEER_BLEND = 0.08D;
    private static final double TERMINAL_STEER_BLEND = 0.16D;
    private static final float MAX_CLIMB_YAW_RATE = 0.35F;
    private static final float MAX_CRUISE_YAW_RATE = 0.55F;
    private static final float MAX_TERMINAL_YAW_RATE = 0.75F;
    private static final float YAW_RATE_RESPONSE = 0.08F;
    private static final double CLIMB_PHASE_THRESHOLD = 3.0D;
    private static final double TERMINAL_HORIZONTAL_THRESHOLD = 14.0D;
    private static final double TERMINAL_DISTANCE_THRESHOLD = 1.2D;
    private static final float TERMINAL_DIVE_PITCH = 45.0F;
    private static final float TERMINAL_MAX_PITCH = 72.0F;
    private static final double TERMINAL_GROUND_OFFSET = 0.15D;
    private static final double TERRAIN_IMPACT_BUFFER = 0.35D;
    private static final float TNT_POWER = 4.0F;

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int mountedLauncherId = -1;
    private UUID mountedLauncherUuid;
    private boolean dropItemOnRemove = true;
    private boolean launched;
    private boolean hasTarget;
    private BlockPos targetPos = BlockPos.ZERO;
    private int flightPhase = FLIGHT_PHASE_IDLE;
    private double cruiseAltitude;
    private double climbWaypointX;
    private double climbWaypointZ;
    private double flightSpeed;
    private double launchOriginX;
    private double launchOriginZ;
    private float launchLockedYaw;
    private float bodyRoll;
    private float bodyRollO;
    private float yawRate;
    private float pitchRate;
    private int launchTicks;
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;

    public Fp5FlamingoEntity(final EntityType<? extends Fp5FlamingoEntity> type, final Level level) {
        super(type, level);
        this.blocksBuilding = true;
        this.noPhysics = true;
        this.setNoGravity(true);
        updateBoundingBox();
    }

    public static Optional<Fp5FlamingoEntity> find(final ServerLevel level, final UUID flamingoId) {
        if (level == null || flamingoId == null) {
            return Optional.empty();
        }
        final Entity entity = level.getEntity(flamingoId);
        return entity instanceof Fp5FlamingoEntity flamingo && flamingo.isAlive()
            ? Optional.of(flamingo)
            : Optional.empty();
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_ON_LAUNCHER, false);
        entityData.define(DATA_SERVER_YAW, getYRot());
        entityData.define(DATA_SERVER_PITCH, getXRot());
        entityData.define(DATA_SERVER_ROLL, 0.0F);
    }

    @Override
    protected void readAdditionalSaveData(final CompoundTag tag) {
        final boolean onLauncher = tag.getBoolean(TAG_ON_LAUNCHER);
        entityData.set(DATA_ON_LAUNCHER, onLauncher);
        if (onLauncher && tag.hasUUID(TAG_LAUNCHER_UUID)) {
            mountedLauncherUuid = tag.getUUID(TAG_LAUNCHER_UUID);
            mountedLauncherId = -1;
        } else {
            mountedLauncherId = -1;
            mountedLauncherUuid = null;
        }

        launched = tag.getBoolean(TAG_LAUNCHED);
        hasTarget = tag.getBoolean(TAG_HAS_TARGET);
        if (hasTarget) {
            targetPos = new BlockPos(tag.getInt(TAG_TARGET_X), tag.getInt(TAG_TARGET_Y), tag.getInt(TAG_TARGET_Z));
        } else {
            targetPos = BlockPos.ZERO;
        }
        flightPhase = tag.getInt(TAG_FLIGHT_PHASE);
        cruiseAltitude = tag.getDouble(TAG_CRUISE_ALTITUDE);
        climbWaypointX = tag.getDouble(TAG_CLIMB_X);
        climbWaypointZ = tag.getDouble(TAG_CLIMB_Z);
        flightSpeed = tag.getDouble(TAG_FLIGHT_SPEED);
        launchOriginX = tag.getDouble(TAG_LAUNCH_ORIGIN_X);
        launchOriginZ = tag.getDouble(TAG_LAUNCH_ORIGIN_Z);
        launchLockedYaw = tag.getFloat(TAG_LAUNCH_LOCKED_YAW);
        bodyRoll = tag.getFloat(TAG_BODY_ROLL);
        bodyRollO = bodyRoll;
        yawRate = tag.getFloat(TAG_YAW_RATE);
        launchTicks = tag.getInt(TAG_LAUNCH_TICKS);
        pitchRate = tag.getFloat(TAG_PITCH_RATE);

        if (isOnLauncher()) {
            noPhysics = true;
            setNoGravity(true);
            setDeltaMovement(Vec3.ZERO);
        } else if (launched) {
            noPhysics = false;
            setNoGravity(true);
        }
        updateBoundingBox();
    }

    @Override
    protected void addAdditionalSaveData(final CompoundTag tag) {
        tag.putBoolean(TAG_ON_LAUNCHER, isOnLauncher());
        if (mountedLauncherUuid != null) {
            tag.putUUID(TAG_LAUNCHER_UUID, mountedLauncherUuid);
        }
        tag.putBoolean(TAG_LAUNCHED, launched);
        tag.putBoolean(TAG_HAS_TARGET, hasTarget);
        if (hasTarget) {
            tag.putInt(TAG_TARGET_X, targetPos.getX());
            tag.putInt(TAG_TARGET_Y, targetPos.getY());
            tag.putInt(TAG_TARGET_Z, targetPos.getZ());
        }
        tag.putInt(TAG_FLIGHT_PHASE, flightPhase);
        tag.putDouble(TAG_CRUISE_ALTITUDE, cruiseAltitude);
        tag.putDouble(TAG_CLIMB_X, climbWaypointX);
        tag.putDouble(TAG_CLIMB_Z, climbWaypointZ);
        tag.putDouble(TAG_FLIGHT_SPEED, flightSpeed);
        tag.putDouble(TAG_LAUNCH_ORIGIN_X, launchOriginX);
        tag.putDouble(TAG_LAUNCH_ORIGIN_Z, launchOriginZ);
        tag.putFloat(TAG_LAUNCH_LOCKED_YAW, launchLockedYaw);
        tag.putFloat(TAG_BODY_ROLL, bodyRoll);
        tag.putFloat(TAG_YAW_RATE, yawRate);
        tag.putInt(TAG_LAUNCH_TICKS, launchTicks);
        tag.putFloat(TAG_PITCH_RATE, pitchRate);
    }

    @Override
    public void tick() {
        super.tick();
        yRotO = getYRot();
        xRotO = getXRot();
        bodyRollO = bodyRoll;

        if (level().isClientSide()) {
            bodyRoll = entityData.get(DATA_SERVER_ROLL);
        }

        if (isOnLauncher()) {
            setDeltaMovement(Vec3.ZERO);
            final Fp5LauncherEntity launcher = resolveLauncher();
            if (launcher != null) {
                keepMountedOnLauncher(launcher);
            } else if (!level().isClientSide()) {
                dropAsItem();
                dropItemOnRemove = false;
                discard();
                return;
            }
            updateBoundingBox();
            return;
        }

        if (!level().isClientSide() && launched) {
            tickAutopilot();
        }

        handleClientSync();
        updateBoundingBox();
    }

    @Override
    public void lerpTo(final double x,
                       final double y,
                       final double z,
                       final float yaw,
                       final float pitch,
                       final int posRotationIncrements,
                       final boolean teleport) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpSteps = 10;
    }

    @Override
    public InteractionResult interact(final Player player, final InteractionHand hand) {
        final ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof MonitorItem) {
            if (level().isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (player instanceof ServerPlayer serverPlayer) {
                MonitorItem.linkAndOpenFp5Monitor(serverPlayer, held, this);
            }
            return InteractionResult.sidedSuccess(level().isClientSide());
        }
        if (!isOnLauncher()) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        final Fp5LauncherEntity launcher = resolveLauncher();
        return launcher != null ? launcher.interact(player, hand) : InteractionResult.FAIL;
    }

    @Override
    public boolean hurt(final DamageSource source, final float amount) {
        if (level().isClientSide || !isAlive()) {
            return false;
        }
        if (launched) {
            detonate(position());
            return true;
        }
        final Fp5LauncherEntity launcher = resolveLauncher();
        if (launcher != null) {
            launcher.onStoredFlamingoRemoved(this);
        }
        dropAsItem();
        dropItemOnRemove = false;
        discard();
        return true;
    }

    @Override
    public void remove(final RemovalReason reason) {
        if (!level().isClientSide && reason.shouldDestroy() && dropItemOnRemove) {
            final Fp5LauncherEntity launcher = resolveLauncher();
            if (launcher != null) {
                launcher.onStoredFlamingoRemoved(this);
            }
            dropAsItem();
            dropItemOnRemove = false;
        }
        super.remove(reason);
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

    @Override
    protected void checkFallDamage(final double y, final boolean onGround, final BlockState state, final BlockPos pos) {
    }

    public boolean isOnLauncher() {
        return entityData.get(DATA_ON_LAUNCHER);
    }

    public boolean isLaunched() {
        return launched;
    }

    public UUID getLauncherUuid() {
        return mountedLauncherUuid;
    }

    public BlockPos getMonitorTarget() {
        return hasTarget ? targetPos : blockPosition();
    }

    public void mountLauncher(final Fp5LauncherEntity launcher) {
        mountedLauncherId = launcher.getId();
        mountedLauncherUuid = launcher.getUUID();
        entityData.set(DATA_ON_LAUNCHER, true);
        launched = false;
        hasTarget = false;
        targetPos = BlockPos.ZERO;
        flightPhase = FLIGHT_PHASE_IDLE;
        cruiseAltitude = 0.0D;
        climbWaypointX = 0.0D;
        climbWaypointZ = 0.0D;
        flightSpeed = 0.0D;
        launchOriginX = getX();
        launchOriginZ = getZ();
        launchLockedYaw = getYRot();
        bodyRoll = 0.0F;
        bodyRollO = 0.0F;
        yawRate = 0.0F;
        pitchRate = 0.0F;
        launchTicks = 0;
        keepMountedOnLauncher(launcher);
    }

    public boolean launchToCoordinates(final BlockPos targetPos) {
        if (level().isClientSide() || !isAlive() || launched || !isOnLauncher() || targetPos == null) {
            return false;
        }

        final Fp5LauncherEntity launcher = resolveLauncher();
        if (launcher != null) {
            launcher.onStoredFlamingoRemoved(this);
        }

        entityData.set(DATA_ON_LAUNCHER, false);
        mountedLauncherId = -1;
        mountedLauncherUuid = null;
        launched = true;
        hasTarget = true;
        this.targetPos = targetPos.immutable();
        launchOriginX = getX();
        launchOriginZ = getZ();
        launchLockedYaw = getYRot();
        bodyRoll = 0.0F;
        bodyRollO = 0.0F;
        yawRate = 0.0F;
        pitchRate = 0.0F;
        launchTicks = 0;
        final double targetGroundY = sampleSurfaceY(targetPos.getX() + 0.5D, targetPos.getZ() + 0.5D);
        flightPhase = FLIGHT_PHASE_CLIMB;
        cruiseAltitude = Math.max(
            getY() + CLIMB_ALTITUDE_MIN + random.nextInt((int) CLIMB_ALTITUDE_RANDOM),
            targetGroundY + TARGET_ALTITUDE_BUFFER
        );

        final Vec3 launchDirection = getCourseDirection();
        climbWaypointX = getX() + launchDirection.x * CLIMB_TARGET_FORWARD;
        climbWaypointZ = getZ() + launchDirection.z * CLIMB_TARGET_FORWARD;
        flightSpeed = 0.3D;

        noPhysics = false;
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        return true;
    }

    public ItemStack createItemStack() {
        return new ItemStack(FullfudRegistries.FP5_FLAMINGO_ITEM.get());
    }

    public void suppressItemDrop() {
        dropItemOnRemove = false;
    }

    private void keepMountedOnLauncher(final Fp5LauncherEntity launcher) {
        noPhysics = true;
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        bodyRoll = 0.0F;
        bodyRollO = 0.0F;
        yawRate = 0.0F;
        pitchRate = 0.0F;
        launchTicks = 0;
        updateLauncherPose(launcher);
    }

    private void handleClientSync() {
        if (level() instanceof ServerLevel && tickCount % 2 == 0) {
            entityData.set(DATA_SERVER_YAW, getYRot());
            entityData.set(DATA_SERVER_PITCH, getXRot());
            entityData.set(DATA_SERVER_ROLL, bodyRoll);
        }
        if (isControlledByLocalInstance()) {
            lerpSteps = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
        }
        if (lerpSteps <= 0) {
            return;
        }

        final double interpolatedX = getX() + (lerpX - getX()) / (double) lerpSteps;
        final double interpolatedY = getY() + (lerpY - getY()) / (double) lerpSteps;
        final double interpolatedZ = getZ() + (lerpZ - getZ()) / (double) lerpSteps;
        final float diffY = Mth.wrapDegrees(entityData.get(DATA_SERVER_YAW) - getYRot());
        final float diffX = Mth.wrapDegrees(entityData.get(DATA_SERVER_PITCH) - getXRot());

        setYRot(getYRot() + 0.1F * diffY);
        setXRot(getXRot() + 0.1F * diffX);
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());
        setPos(interpolatedX, interpolatedY, interpolatedZ);

        --lerpSteps;
    }

    private Fp5LauncherEntity resolveLauncher() {
        if (mountedLauncherId > 0) {
            final Entity entity = level().getEntity(mountedLauncherId);
            if (entity instanceof Fp5LauncherEntity launcher && launcher.isAlive()) {
                return launcher;
            }
            mountedLauncherId = -1;
        }
        if (mountedLauncherUuid != null && level() instanceof ServerLevel serverLevel) {
            final Entity entity = serverLevel.getEntity(mountedLauncherUuid);
            if (entity instanceof Fp5LauncherEntity launcher && launcher.isAlive()) {
                mountedLauncherId = launcher.getId();
                return launcher;
            }
            mountedLauncherUuid = null;
        }
        return null;
    }

    private void updateLauncherPose(final Fp5LauncherEntity launcher) {
        final float yaw = launcher.getYRot();
        final Vec3 forward = Vec3.directionFromRotation(0.0F, yaw).normalize();
        final Vec3 anchor = launcher.position()
            .add(0.0D, LAUNCHER_MOUNT_VERTICAL_OFFSET, 0.0D)
            .add(forward.scale(LAUNCHER_MOUNT_FORWARD_OFFSET));
        setPos(anchor.x, anchor.y, anchor.z);
        setCourseOrientation(yaw, -MODEL_BASE_PITCH_DEGREES);
    }

    private void tickAutopilot() {
        if (!hasTarget) {
            launched = false;
            flightPhase = FLIGHT_PHASE_IDLE;
            setDeltaMovement(Vec3.ZERO);
            yawRate = 0.0F;
            pitchRate = 0.0F;
            launchTicks = 0;
            return;
        }

        launchTicks++;

        final Vec3 currentPos = position();
        final Vec3 impactTarget = resolveImpactPoint();
        final Vec3 phaseTarget = resolvePhaseTarget(impactTarget);
        final Vec3 flightMotion = computeFlightMotion(currentPos, phaseTarget, impactTarget);
        final HitResult blockHit = level().clip(new ClipContext(
            currentPos,
            currentPos.add(flightMotion),
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            this
        ));
        if (blockHit.getType() != HitResult.Type.MISS) {
            detonate(blockHit.getLocation());
            return;
        }

        move(MoverType.SELF, flightMotion);
        setDeltaMovement(flightMotion);
        advanceFlightPhase(impactTarget);

        if (position().distanceTo(impactTarget) <= TERMINAL_DISTANCE_THRESHOLD) {
            detonate(position());
            return;
        }

        if (hasReachedTerrain()) {
            detonate(new Vec3(getX(), sampleSurfaceY(getX(), getZ()), getZ()));
            return;
        }

        if (getY() <= level().getMinBuildHeight() + 1) {
            detonate(position());
        }
    }

    private Vec3 resolvePhaseTarget(final Vec3 impactTarget) {
        if (!hasTarget) {
            return position();
        }
        return switch (flightPhase) {
            case FLIGHT_PHASE_CLIMB -> new Vec3(climbWaypointX, cruiseAltitude, climbWaypointZ);
            case FLIGHT_PHASE_CRUISE -> new Vec3(targetPos.getX() + 0.5D, cruiseAltitude, targetPos.getZ() + 0.5D);
            case FLIGHT_PHASE_TERMINAL -> impactTarget;
            default -> position();
        };
    }

    private Vec3 resolveImpactPoint() {
        final double targetX = targetPos.getX() + 0.5D;
        final double targetZ = targetPos.getZ() + 0.5D;
        return new Vec3(targetX, sampleSurfaceY(targetX, targetZ), targetZ);
    }

    private Vec3 computeFlightMotion(final Vec3 currentPos, final Vec3 phaseTarget, final Vec3 impactTarget) {
        final Vec3 delta = phaseTarget.subtract(currentPos);
        final boolean launchTurnLocked = isLaunchTurnLocked();
        final float targetYaw = Mth.wrapDegrees((float) (-(Mth.atan2(delta.x, delta.z) * Mth.RAD_TO_DEG)) + MODEL_FORWARD_YAW_OFFSET);
        final float desiredYaw = launchTurnLocked ? launchLockedYaw : targetYaw;
        final float desiredPitch = switch (flightPhase) {
            case FLIGHT_PHASE_CLIMB -> computeAltitudeHoldPitch(cruiseAltitude - currentPos.y, CLIMB_ALTITUDE_RESPONSE_DISTANCE, 30.0F);
            case FLIGHT_PHASE_TERMINAL -> computeTerminalPitch(currentPos, impactTarget);
            default -> computeAltitudeHoldPitch(cruiseAltitude - currentPos.y, CRUISE_ALTITUDE_RESPONSE_DISTANCE, 12.0F);
        };

        final float currentYaw = getYRot();
        final float currentCoursePitch = getCoursePitch();
        final float turnAuthority = flightPhase == FLIGHT_PHASE_TERMINAL ? 1.0F : getTurnAuthority();
        final float pitchRateLimit = switch (flightPhase) {
            case FLIGHT_PHASE_CLIMB -> CLIMB_PITCH_RATE_LIMIT;
            case FLIGHT_PHASE_TERMINAL -> TERMINAL_PITCH_RATE_LIMIT;
            default -> CRUISE_PITCH_RATE_LIMIT;
        };
        final float bankLimit = switch (flightPhase) {
            case FLIGHT_PHASE_CLIMB -> CLIMB_BANK_LIMIT;
            case FLIGHT_PHASE_TERMINAL -> TERMINAL_BANK_LIMIT;
            default -> CRUISE_BANK_LIMIT;
        };
        final float yawError = Mth.wrapDegrees(desiredYaw - currentYaw);
        final float effectiveBankLimit = flightPhase == FLIGHT_PHASE_TERMINAL ? bankLimit : bankLimit * turnAuthority;
        final float targetBank = launchTurnLocked
            ? 0.0F
            : Mth.clamp(yawError * 0.16F, -effectiveBankLimit, effectiveBankLimit);
        final float nextRoll = (float) approachValue(bodyRoll, targetBank, BANK_RESPONSE_STEP);
        final float maxYawRate = switch (flightPhase) {
            case FLIGHT_PHASE_CLIMB -> MAX_CLIMB_YAW_RATE;
            case FLIGHT_PHASE_TERMINAL -> MAX_TERMINAL_YAW_RATE;
            default -> MAX_CRUISE_YAW_RATE;
        };
        final float desiredYawRate = launchTurnLocked
            ? 0.0F
            : (float) (Math.sin(Math.toRadians(nextRoll)) * maxYawRate * Mth.clamp(flightSpeed / CRUISE_SPEED_TARGET, 0.45D, 1.2D) * turnAuthority);
        yawRate = (float) approachValue(yawRate, desiredYawRate, YAW_RATE_RESPONSE);
        if (!launchTurnLocked && Math.abs(yawError) < 0.5F) {
            yawRate = (float) approachValue(yawRate, 0.0D, YAW_RATE_RESPONSE * 0.6F);
        }
        final float nextYaw = launchTurnLocked ? launchLockedYaw : currentYaw + yawRate;
        final float pitchError = desiredPitch - currentCoursePitch;
        final float desiredPitchRate = Mth.clamp(pitchError * 0.08F, -pitchRateLimit, pitchRateLimit);
        final float pitchRateResponse = flightPhase == FLIGHT_PHASE_TERMINAL ? PITCH_RATE_RESPONSE * 1.25F : PITCH_RATE_RESPONSE;
        pitchRate = (float) approachValue(pitchRate, desiredPitchRate, pitchRateResponse);
        if (Math.abs(pitchError) < 0.25F) {
            pitchRate = (float) approachValue(pitchRate, 0.0D, pitchRateResponse * 0.5F);
        }
        final float nextPitch = Mth.clamp(currentCoursePitch + pitchRate, -85.0F, 85.0F);
        bodyRoll = nextRoll;
        setCourseOrientation(nextYaw, nextPitch);

        final double speedTarget = resolveSpeedTarget();
        flightSpeed = approachValue(flightSpeed, speedTarget, computeSpeedStep(speedTarget));
        final Vec3 forward = Vec3.directionFromRotation(nextPitch, nextYaw + MODEL_FORWARD_YAW_OFFSET);
        final Vec3 right = Vec3.directionFromRotation(0.0F, nextYaw + MODEL_FORWARD_YAW_OFFSET - 90.0F);
        final double bankRadians = Math.toRadians(nextRoll);
        final Vec3 desiredMotion = forward.add(right.scale(Math.sin(bankRadians) * BANK_LATERAL_FACTOR)).normalize();
        final Vec3 previousMotion = getDeltaMovement().lengthSqr() > 1.0E-6D ? getDeltaMovement().normalize() : desiredMotion;
        final double steerBlend = launchTurnLocked
            ? 1.0D
            : (flightPhase == FLIGHT_PHASE_TERMINAL ? TERMINAL_STEER_BLEND : Mth.lerp(turnAuthority, 0.03D, CRUISE_STEER_BLEND));
        return previousMotion.lerp(desiredMotion, steerBlend).normalize().scale(flightSpeed);
    }

    private void advanceFlightPhase(final Vec3 impactTarget) {
        if (!hasTarget) {
            return;
        }
        if (flightPhase == FLIGHT_PHASE_CLIMB) {
            final Vec3 climbTarget = new Vec3(climbWaypointX, cruiseAltitude, climbWaypointZ);
            if (position().distanceTo(climbTarget) <= CLIMB_PHASE_THRESHOLD || getY() >= cruiseAltitude - 1.0D) {
                flightPhase = FLIGHT_PHASE_CRUISE;
            }
        } else if (flightPhase == FLIGHT_PHASE_CRUISE) {
            if (isLaunchTurnLocked()) {
                return;
            }
            final double dx = impactTarget.x - getX();
            final double dz = impactTarget.z - getZ();
            final double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            final double verticalDrop = Math.max(getY() - impactTarget.y, 0.0D);
            final double requiredDiveDistance = Math.max(
                TERMINAL_HORIZONTAL_THRESHOLD,
                verticalDrop / Math.tan(Math.toRadians(TERMINAL_DIVE_PITCH))
            );
            if (horizontalDistance <= requiredDiveDistance) {
                flightPhase = FLIGHT_PHASE_TERMINAL;
            }
        }
    }

    private float computeAltitudeHoldPitch(final double altitudeError, final double responseDistance, final float maxPitch) {
        final float desiredPitch = (float) (-(Mth.atan2(altitudeError, Math.max(responseDistance, 1.0D)) * Mth.RAD_TO_DEG));
        return Mth.clamp(desiredPitch, -maxPitch, maxPitch);
    }

    private float computeTerminalPitch(final Vec3 currentPos, final Vec3 impactTarget) {
        final Vec3 delta = impactTarget.subtract(currentPos);
        final double horizontalDistance = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        final float impactPitch = (float) (-(Mth.atan2(delta.y, Math.max(horizontalDistance, 1.0E-6D)) * Mth.RAD_TO_DEG));
        return Mth.clamp(Math.max(impactPitch, TERMINAL_DIVE_PITCH), TERMINAL_DIVE_PITCH, TERMINAL_MAX_PITCH);
    }

    private boolean hasReachedTerrain() {
        if (flightPhase != FLIGHT_PHASE_TERMINAL) {
            return false;
        }
        if (getDeltaMovement().y > -0.01D) {
            return false;
        }
        return getY() <= sampleSurfaceY(getX(), getZ()) + TERRAIN_IMPACT_BUFFER;
    }

    private double sampleSurfaceY(final double x, final double z) {
        final int surfaceY = level().getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
        return surfaceY + TERMINAL_GROUND_OFFSET;
    }

    private boolean isLaunchTurnLocked() {
        if (flightPhase == FLIGHT_PHASE_TERMINAL) {
            return false;
        }
        final double dx = getX() - launchOriginX;
        final double dz = getZ() - launchOriginZ;
        return Math.sqrt(dx * dx + dz * dz) < INITIAL_STRAIGHT_DISTANCE;
    }

    private float getTurnAuthority() {
        final double dx = getX() - launchOriginX;
        final double dz = getZ() - launchOriginZ;
        final double distanceFromLaunch = Math.sqrt(dx * dx + dz * dz);
        if (distanceFromLaunch <= INITIAL_STRAIGHT_DISTANCE) {
            return 0.0F;
        }
        return (float) Mth.clamp((distanceFromLaunch - INITIAL_STRAIGHT_DISTANCE) / TURN_AUTHORITY_RAMP_DISTANCE, 0.0D, 1.0D);
    }

    public float getVisualRoll(final float partialTick) {
        return Mth.lerp(partialTick, bodyRollO, bodyRoll);
    }

    private double resolveSpeedTarget() {
        if (launchTicks <= BOOSTER_BURN_TICKS) {
            return BOOSTER_PEAK_SPEED;
        }
        if (launchTicks <= BOOSTER_BURN_TICKS + BOOSTER_DECAY_TICKS) {
            final double decayProgress = (double) (launchTicks - BOOSTER_BURN_TICKS) / (double) BOOSTER_DECAY_TICKS;
            return Mth.lerp(decayProgress, BOOSTER_PEAK_SPEED, BOOSTER_SETTLE_SPEED);
        }
        return switch (flightPhase) {
            case FLIGHT_PHASE_CLIMB -> CLIMB_SPEED_TARGET;
            case FLIGHT_PHASE_TERMINAL -> TERMINAL_SPEED_TARGET;
            default -> CRUISE_SPEED_TARGET;
        };
    }

    private double computeSpeedStep(final double speedTarget) {
        if (launchTicks <= BOOSTER_BURN_TICKS) {
            return BOOSTER_ACCEL_STEP;
        }
        if (launchTicks <= BOOSTER_BURN_TICKS + BOOSTER_DECAY_TICKS) {
            return BOOSTER_DECAY_STEP;
        }
        return Mth.clamp(Math.abs(speedTarget - flightSpeed) * 0.08D, MIN_SPEED_STEP, MAX_SPEED_STEP);
    }

    private float getCoursePitch() {
        return getXRot() - MODEL_BASE_PITCH_DEGREES;
    }

    private Vec3 getCourseDirection() {
        final float motionYaw = getYRot() + MODEL_FORWARD_YAW_OFFSET;
        final Vec3 forward = Vec3.directionFromRotation(getCoursePitch(), motionYaw);
        return forward.lengthSqr() > 1.0E-6D ? forward.normalize() : Vec3.directionFromRotation(-MODEL_BASE_PITCH_DEGREES, motionYaw).normalize();
    }

    private void setCourseOrientation(final float yaw, final float pitch) {
        final float entityPitch = Mth.clamp(pitch + MODEL_BASE_PITCH_DEGREES, -89.0F, 89.0F);
        setRot(yaw, entityPitch);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
    }

    private void detonate(final Vec3 impactPos) {
        if (level().isClientSide() || isRemoved()) {
            return;
        }
        dropItemOnRemove = false;
        launched = false;
        entityData.set(DATA_ON_LAUNCHER, false);
        mountedLauncherId = -1;
        mountedLauncherUuid = null;
        setPos(impactPos.x, impactPos.y, impactPos.z);
        spawnTntEffect();
        discard();
    }

    private void spawnTntEffect() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        final PrimedTnt tnt = new PrimedTnt(serverLevel, getX(), getY(), getZ(), null);
        tnt.setFuse(0);
        serverLevel.addFreshEntity(tnt);
        serverLevel.explode(tnt, getX(), getY(), getZ(), TNT_POWER, Level.ExplosionInteraction.TNT);
        tnt.discard();
    }

    private void dropAsItem() {
        spawnAtLocation(createItemStack());
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

    @Override
    public EntityDimensions getDimensions(final Pose pose) {
        return FLAMINGO_SIZE;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(1.0D * SCALE, 0.5D * SCALE, 1.0D * SCALE);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "fp5_flamingo", 0, state -> {
            state.setAndContinue(IDLE_ANIMATION);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    private static double approachValue(final double current, final double target, final double maxStep) {
        if (current < target) {
            return Math.min(current + maxStep, target);
        }
        return Math.max(current - maxStep, target);
    }
}
