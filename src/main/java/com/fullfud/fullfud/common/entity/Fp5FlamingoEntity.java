package com.fullfud.fullfud.common.entity;

import com.fullfud.fullfud.common.item.MonitorItem;
import com.fullfud.fullfud.core.BlastLightRefresh;
import com.fullfud.fullfud.core.ChunkLoadManager;
import com.fullfud.fullfud.core.EntityDrops;
import com.fullfud.fullfud.core.FullfudRegistries;
import com.fullfud.fullfud.core.config.FullfudServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
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
    private static final String TAG_LATERAL_SLIP = "LateralSlip";
    private static final String TAG_GUIDANCE_SCALE = "GuidanceScale";

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
    /**
     * Range at which the launch profile above is used in full. Shorter shots divide it down (see
     * {@code guidanceScale}) so the missile always has room left to turn onto the target and dive.
     */
    private static final double FULL_GUIDANCE_PROFILE_RANGE = 4.0D * INITIAL_STRAIGHT_DISTANCE;
    private static final double MIN_GUIDANCE_SCALE = 0.05D;
    /** Chunk radius held around a missile in flight; below 2 the chunk stops ticking entities. */
    private static final int FLAMINGO_CHUNK_RADIUS = 3;
    /** Bank the model rolls into while simple guidance is turning at its full rate. Cosmetic. */
    private static final float SIMPLE_GUIDANCE_BANK_LIMIT = 28.0F;
    private static final float SIMPLE_GUIDANCE_BANK_STEP = 2.5F;
    private static final double CLIMB_ALTITUDE_MIN = 20.0D;
    private static final double CLIMB_ALTITUDE_RANDOM = 31.0D;
    private static final double TARGET_ALTITUDE_BUFFER = 8.0D;
    private static final double CLIMB_ALTITUDE_RESPONSE_DISTANCE = 18.0D;
    private static final double CRUISE_ALTITUDE_RESPONSE_DISTANCE = 40.0D;
    private static final int BOOSTER_KICK_TICKS = 12;
    private static final int BOOSTER_BURN_TICKS = 120;
    private static final int BOOSTER_DECAY_TICKS = 40;
    private static final int POST_BOOST_RECOVERY_TICKS = 90;
    private static final double BOOSTER_PEAK_SPEED = 5.2D;
    private static final double BOOSTER_SUSTAIN_SPEED = 3.45D;
    private static final double BOOSTER_SETTLE_SPEED = 1.85D;
    private static final double BOOSTER_ACCEL_STEP = 0.36D;
    private static final double BOOSTER_SUSTAIN_STEP = 0.11D;
    private static final double BOOSTER_DECAY_STEP = 0.16D;
    private static final double POST_BOOST_ACCEL_STEP = 0.05D;
    private static final double CLIMB_SPEED_TARGET = 2.15D;
    private static final double CRUISE_SPEED_TARGET = 2.95D;
    private static final double TERMINAL_SPEED_TARGET = 3.65D;
    private static final double MIN_SPEED_STEP = 0.02D;
    private static final double MAX_SPEED_STEP = 0.09D;
    private static final float CLIMB_PITCH_RATE_LIMIT = 0.85F;
    private static final float CRUISE_PITCH_RATE_LIMIT = 0.45F;
    private static final float TERMINAL_PITCH_RATE_LIMIT = 1.2F;
    private static final float PITCH_RATE_RESPONSE = 0.09F;
    private static final float CLIMB_BANK_LIMIT = 4.0F;
    private static final float CRUISE_BANK_LIMIT = 8.0F;
    private static final float TERMINAL_BANK_LIMIT = 12.0F;
    private static final float BANK_RESPONSE_STEP = 0.42F;
    private static final double CLIMB_LATERAL_SLIP_LIMIT = 0.018D;
    private static final double CRUISE_LATERAL_SLIP_LIMIT = 0.052D;
    private static final double TERMINAL_LATERAL_SLIP_LIMIT = 0.032D;
    private static final double SLIP_BUILD_STEP = 0.0028D;
    private static final double SLIP_DECAY_STEP = 0.0042D;
    private static final double CRUISE_STEER_BLEND = 0.08D;
    private static final double TERMINAL_STEER_BLEND = 0.16D;
    private static final float MAX_CLIMB_YAW_RATE = 0.35F;
    private static final float MAX_CRUISE_YAW_RATE = 0.55F;
    private static final float MAX_TERMINAL_YAW_RATE = 0.75F;
    private static final float CLIMB_YAW_ACCEL = 0.032F;
    private static final float CRUISE_YAW_ACCEL = 0.046F;
    private static final float TERMINAL_YAW_ACCEL = 0.065F;
    private static final float CLIMB_YAW_DAMPING = 0.016F;
    private static final float CRUISE_YAW_DAMPING = 0.022F;
    private static final float TERMINAL_YAW_DAMPING = 0.03F;
    private static final float YAW_MISMATCH_BRAKE = 0.06F;
    private static final double CLIMB_PHASE_THRESHOLD = 3.0D;
    private static final double TERMINAL_HORIZONTAL_THRESHOLD = 14.0D;
    private static final double TERMINAL_DISTANCE_THRESHOLD = 1.2D;
    private static final float TERMINAL_DIVE_PITCH = 45.0F;
    private static final float TERMINAL_MAX_PITCH = 72.0F;
    /** How far above the aim point the missile stops forcing a dive angle and simply points at it. */
    private static final double TERMINAL_DIVE_FLOOR_MARGIN = 2.0D;
    /** Multiplier on the simple-guidance turn and pitch rates once the missile is in its dive. */
    private static final float TERMINAL_RATE_BOOST = 4.0F;
    /**
     * Proximity fuse radius. The missile moves three to four blocks a tick, so it can never be made to land exactly
     * on a point; past this range the warhead goes off at the closest approach instead of letting the missile sail
     * by and come round for another attempt.
     */
    private static final double TERMINAL_FUSE_RADIUS = 12.0D;
    private static final double TERMINAL_GROUND_OFFSET = 0.15D;
    private static final double TERRAIN_IMPACT_BUFFER = 0.35D;
    private static final float TNT_POWER = 4.0F;
    private static final int GUIDANCE_DELAY_TICKS = 42;
    private static final int GUIDANCE_RAMP_TICKS = 90;
    private static final double EXHAUST_REAR_OFFSET = 2.45D * SCALE;
    private static final double EXHAUST_VERTICAL_OFFSET = 0.16D * SCALE;
    private static final double EXHAUST_SIDE_SPREAD = 0.18D * SCALE;
    // 1.21.2 replaced DustParticleOptions' Vector3f colour with a packed RGB int. 0xF5F5F5 is the old
    // (0.96, 0.96, 0.96) rounded to bytes.
    private static final int BOOSTER_SMOKE_COLOR = 0xF5F5F5;
    private static final DustParticleOptions BOOSTER_SMOKE_PARTICLE = new DustParticleOptions(BOOSTER_SMOKE_COLOR, 2.5F);

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
    private double lateralSlip;
    /** Distance to the aim point on the previous tick, for the proximity fuse. Not persisted: reset means "no pass yet". */
    private double previousImpactDistance = Double.MAX_VALUE;
    /**
     * Fraction of the full launch profile this shot uses: it scales both the straight-out leg and the guidance
     * delay, so a target 120 blocks away is not still flying dead ahead when it passes overhead.
     */
    private double guidanceScale = 1.0D;
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
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        builder.define(DATA_ON_LAUNCHER, false);
        builder.define(DATA_SERVER_YAW, getYRot());
        builder.define(DATA_SERVER_PITCH, getXRot());
        builder.define(DATA_SERVER_ROLL, 0.0F);
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
        lateralSlip = tag.getDouble(TAG_LATERAL_SLIP);
        guidanceScale = tag.contains(TAG_GUIDANCE_SCALE)
            ? Mth.clamp(tag.getDouble(TAG_GUIDANCE_SCALE), MIN_GUIDANCE_SCALE, 1.0D)
            : 1.0D;

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
        tag.putDouble(TAG_LATERAL_SLIP, lateralSlip);
        tag.putDouble(TAG_GUIDANCE_SCALE, guidanceScale);
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

    // 1.21.2 dropped lerpTo's trailing teleport flag; the interpolation is otherwise unchanged.
    @Override
    public void lerpTo(final double x,
                       final double y,
                       final double z,
                       final float yaw,
                       final float pitch,
                       final int posRotationIncrements) {
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
            return level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
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

    // Entity.hurt is final void since 1.21.2 and forwards here only on a ServerLevel, so the former
    // isClientSide half of the guard is implicit.
    @Override
    public boolean hurtServer(final ServerLevel level, final DamageSource source, final float amount) {
        if (!isAlive()) {
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
        releaseChunkTicket();
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
        lateralSlip = 0.0D;
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
        lateralSlip = 0.0D;
        launchTicks = 0;
        previousImpactDistance = Double.MAX_VALUE;
        final double targetGroundY = sampleSurfaceY(targetPos.getX() + 0.5D, targetPos.getZ() + 0.5D);        // Cruise has to be above the aim point, not merely above the ground under it: a target on a tower or in open
        // air sits higher than the terrain, and cruising below it would turn the terminal dive into a climb.
        final double aimY = FullfudServerConfig.SERVER.fp5HonourTargetAltitude.get()
            ? Math.max(targetPos.getY() + 0.5D, targetGroundY)
            : targetGroundY;
        flightPhase = FLIGHT_PHASE_CLIMB;
        cruiseAltitude = Math.max(
            getY() + CLIMB_ALTITUDE_MIN + random.nextInt((int) CLIMB_ALTITUDE_RANDOM),
            aimY + TARGET_ALTITUDE_BUFFER
        );

        // The straight-out leg and the guidance delay were sized for a strategic shot. Kept absolute they eat
        // the whole flight of a short one: the missile would fly its 200 blocks locked on the launcher's
        // heading, pass the target on the way, and then never dive because the terminal phase was gated on the
        // same lock — which is exactly "it flies off and disappears".
        final double targetDistance = Math.sqrt(
            Mth.square(targetPos.getX() + 0.5D - getX()) + Mth.square(targetPos.getZ() + 0.5D - getZ()));
        guidanceScale = Mth.clamp(targetDistance / FULL_GUIDANCE_PROFILE_RANGE, MIN_GUIDANCE_SCALE, 1.0D);

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
        lateralSlip = 0.0D;
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
            lateralSlip = 0.0D;
            launchTicks = 0;
            return;
        }

        launchTicks++;

        // Nothing else keeps the chunks under the missile alive. Once it outran the launching player's view
        // distance its chunk stopped ticking entities, so it froze in mid-air short of the target instead of
        // arriving: the same ticket the Shahed holds fixes it.
        ensureChunkTicket();

        final Vec3 currentPos = position();
        final Vec3 impactTarget = resolveImpactPoint();
        final Vec3 phaseTarget = resolvePhaseTarget(impactTarget);
        final Vec3 flightMotion = computeFlightMotion(currentPos, phaseTarget, impactTarget);
        spawnExhaustParticles();

        // A missile in the dive covers three or four blocks per tick, so the arrival test has to look ahead by a
        // whole step: checking only the position it lands on lets it skip over the target point and carry on.
        final double impactDistance = currentPos.distanceTo(impactTarget);
        if (flightPhase == FLIGHT_PHASE_TERMINAL) {
            if (impactDistance <= flightMotion.length() + TERMINAL_DISTANCE_THRESHOLD) {
                detonate(impactTarget);
                return;
            }
            // Proximity fuse. Guidance can bring the missile close but not onto a point, and once it is past the
            // target the bearing swings behind it and it flies a wide circle round the aim point for as long as it
            // exists. Detonating at the closest approach ends the pass that got closest instead.
            if (impactDistance <= TERMINAL_FUSE_RADIUS && impactDistance > previousImpactDistance) {
                detonate(impactTarget);
                return;
            }
            previousImpactDistance = impactDistance;
        } else {
            previousImpactDistance = Double.MAX_VALUE;
        }
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

        if (getY() <= level().getMinY() + 1) {
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
        final double surfaceY = sampleSurfaceY(targetX, targetZ);
        if (!FullfudServerConfig.SERVER.fp5HonourTargetAltitude.get()) {
            return new Vec3(targetX, surfaceY, targetZ);
        }
        // The typed Y decides the aim point, so a rooftop, a tower or a height in open air is a valid target rather
        // than a request the missile silently rewrites into "hit the ground here". The one thing that cannot be
        // honoured is a Y under the terrain: the missile would detonate on the surface above it in any case, so an
        // aim point below the surface is lifted to it instead of pointing the nose into rock.
        return new Vec3(targetX, Math.max(targetPos.getY() + 0.5D, surfaceY), targetZ);
    }

    private Vec3 computeFlightMotion(final Vec3 currentPos, final Vec3 phaseTarget, final Vec3 impactTarget) {
        if (FullfudServerConfig.SERVER.fp5SimpleGuidance.get()) {
            return computeSimpleGuidanceMotion(currentPos, phaseTarget, impactTarget);
        }
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
        final float yawAccelGain = switch (flightPhase) {
            case FLIGHT_PHASE_CLIMB -> CLIMB_YAW_ACCEL;
            case FLIGHT_PHASE_TERMINAL -> TERMINAL_YAW_ACCEL;
            default -> CRUISE_YAW_ACCEL;
        };
        final float yawDamping = switch (flightPhase) {
            case FLIGHT_PHASE_CLIMB -> CLIMB_YAW_DAMPING;
            case FLIGHT_PHASE_TERMINAL -> TERMINAL_YAW_DAMPING;
            default -> CRUISE_YAW_DAMPING;
        };
        if (launchTurnLocked) {
            yawRate = (float) approachValue(yawRate, 0.0D, YAW_MISMATCH_BRAKE);
        } else {
            final float yawAcceleration = (float) (Math.sin(Math.toRadians(nextRoll)) * yawAccelGain * Mth.clamp(flightSpeed / CRUISE_SPEED_TARGET, 0.45D, 1.2D) * turnAuthority);
            yawRate += yawAcceleration;
            yawRate -= yawRate * yawDamping;
            if (Math.abs(yawError) < 0.5F) {
                yawRate = (float) approachValue(yawRate, 0.0D, yawDamping);
            } else if (Math.signum(yawRate) != 0.0F && Math.signum(yawRate) != Math.signum(yawError)) {
                yawRate = (float) approachValue(yawRate, 0.0D, YAW_MISMATCH_BRAKE);
            }
        }
        yawRate = Mth.clamp(yawRate, -maxYawRate, maxYawRate);
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
        final double slipLimit = switch (flightPhase) {
            case FLIGHT_PHASE_CLIMB -> CLIMB_LATERAL_SLIP_LIMIT;
            case FLIGHT_PHASE_TERMINAL -> TERMINAL_LATERAL_SLIP_LIMIT;
            default -> CRUISE_LATERAL_SLIP_LIMIT;
        };
        final double bankRadians = Math.toRadians(nextRoll);
        final double desiredLateralSlip = launchTurnLocked
            ? 0.0D
            : Mth.clamp(Math.sin(bankRadians) * slipLimit * Mth.clamp(flightSpeed / CRUISE_SPEED_TARGET, 0.45D, 1.25D) * turnAuthority, -slipLimit, slipLimit);
        final double slipStep = Math.abs(desiredLateralSlip) > Math.abs(lateralSlip) ? SLIP_BUILD_STEP : SLIP_DECAY_STEP;
        lateralSlip = approachValue(lateralSlip, desiredLateralSlip, slipStep);
        final Vec3 desiredMotion = forward.add(right.scale(lateralSlip)).normalize();
        final Vec3 previousMotion = getDeltaMovement().lengthSqr() > 1.0E-6D ? getDeltaMovement().normalize() : desiredMotion;
        final double steerBlend = launchTurnLocked
            ? 1.0D
            : (flightPhase == FLIGHT_PHASE_TERMINAL ? TERMINAL_STEER_BLEND : Mth.lerp(turnAuthority, 0.03D, CRUISE_STEER_BLEND));
        return previousMotion.lerp(desiredMotion, steerBlend).normalize().scale(flightSpeed);
    }

    /**
     * Direct guidance: turn onto the bearing to the current phase target as fast as the configured rate allows and
     * fly exactly where the nose points.
     *
     * <p>What it replaces models a cruise missile honestly — a locked launch heading, turn authority that ramps in
     * with distance and time, a bank that has to build before the heading moves, and a velocity vector that lags
     * the airframe. The result is a turn circle of several hundred blocks, so a missile launched at anything but a
     * distant target sails past it and keeps going. Here the missile simply steers, and the only thing left of the
     * flight model is the booster speed profile and the climb-cruise-dive phase sequence.
     */
    private Vec3 computeSimpleGuidanceMotion(final Vec3 currentPos, final Vec3 phaseTarget, final Vec3 impactTarget) {
        final Vec3 delta = phaseTarget.subtract(currentPos);
        final boolean terminal = flightPhase == FLIGHT_PHASE_TERMINAL;
        final float targetYaw = Mth.wrapDegrees(
            (float) (-(Mth.atan2(delta.x, delta.z) * Mth.RAD_TO_DEG)) + MODEL_FORWARD_YAW_OFFSET);
        final float desiredPitch = switch (flightPhase) {
            case FLIGHT_PHASE_CLIMB -> computeAltitudeHoldPitch(cruiseAltitude - currentPos.y, CLIMB_ALTITUDE_RESPONSE_DISTANCE, 30.0F);
            // Point straight at the target rather than through computeTerminalPitch, whose 45-degree floor exists to
            // dress the impact up as a dive. Simple guidance is supposed to fly where it looks, and forcing a dive
            // steeper than the geometry is what made the missile pass the aim point and orbit it.
            case FLIGHT_PHASE_TERMINAL -> computeBearingPitch(currentPos, impactTarget);
            default -> computeAltitudeHoldPitch(cruiseAltitude - currentPos.y, CRUISE_ALTITUDE_RESPONSE_DISTANCE, 20.0F);
        };

        // At cruise speed the missile covers some 70 blocks a second, so the configured 90 deg/s bends its path on a
        // radius of about 45 blocks. That is wider than the whole endgame: the last few hundred blocks need several
        // times the authority or the missile simply circles the target it can see.
        final float rateBoost = terminal ? TERMINAL_RATE_BOOST : 1.0F;
        final float yawStep = (float) (FullfudServerConfig.SERVER.fp5TurnDegreesPerSecond.get() / 20.0D) * rateBoost;
        final float pitchStep = (float) (FullfudServerConfig.SERVER.fp5PitchDegreesPerSecond.get() / 20.0D) * rateBoost;
        final float yawError = Mth.wrapDegrees(targetYaw - getYRot());
        final float appliedYaw = Mth.clamp(yawError, -yawStep, yawStep);
        final float nextYaw = Mth.wrapDegrees(getYRot() + appliedYaw);
        final float currentCoursePitch = getCoursePitch();
        final float appliedPitch = Mth.clamp(desiredPitch - currentCoursePitch, -pitchStep, pitchStep);
        final float nextPitch = Mth.clamp(currentCoursePitch + appliedPitch, -85.0F, 85.0F);

        yawRate = appliedYaw;
        pitchRate = appliedPitch;
        lateralSlip = 0.0D;
        final float targetBank = Mth.clamp(
            yawStep > 0.0F ? (appliedYaw / yawStep) * SIMPLE_GUIDANCE_BANK_LIMIT : 0.0F,
            -SIMPLE_GUIDANCE_BANK_LIMIT,
            SIMPLE_GUIDANCE_BANK_LIMIT
        );
        bodyRoll = (float) approachValue(bodyRoll, targetBank, SIMPLE_GUIDANCE_BANK_STEP);
        setCourseOrientation(nextYaw, nextPitch);

        final double speedTarget = resolveSpeedTarget();
        flightSpeed = approachValue(flightSpeed, speedTarget, computeSpeedStep(speedTarget));
        return Vec3.directionFromRotation(nextPitch, nextYaw + MODEL_FORWARD_YAW_OFFSET).scale(flightSpeed);
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
            // Deliberately not gated on isLaunchTurnLocked(): a short shot is still inside its straight-out leg
            // when it arrives, and refusing to dive there is what let it overfly the target and keep going.
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

    /** Course pitch that points the nose exactly at {@code impactTarget}, nose-up included. */
    private float computeBearingPitch(final Vec3 currentPos, final Vec3 impactTarget) {
        final Vec3 delta = impactTarget.subtract(currentPos);
        final double horizontalDistance = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        final float bearingPitch = (float) (-(Mth.atan2(delta.y, Math.max(horizontalDistance, 1.0E-6D)) * Mth.RAD_TO_DEG));
        return Mth.clamp(bearingPitch, -85.0F, 85.0F);
    }

    private float computeTerminalPitch(final Vec3 currentPos, final Vec3 impactTarget) {
        final Vec3 delta = impactTarget.subtract(currentPos);
        final double horizontalDistance = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        final float impactPitch = (float) (-(Mth.atan2(delta.y, Math.max(horizontalDistance, 1.0E-6D)) * Mth.RAD_TO_DEG));
        if (delta.y >= -TERMINAL_DIVE_FLOOR_MARGIN) {
            // Level with the aim point, or below it. The dive floor is there to make the last seconds look like a
            // dive rather than a glide; applied here it would drive the missile straight past a target it has come
            // down to, or under one it has to climb to, so follow the geometry instead — nose-up included.
            return Mth.clamp(impactPitch, -TERMINAL_MAX_PITCH, TERMINAL_MAX_PITCH);
        }
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
        return Math.sqrt(dx * dx + dz * dz) < INITIAL_STRAIGHT_DISTANCE * guidanceScale;
    }

    private float getTurnAuthority() {
        final double straightDistance = INITIAL_STRAIGHT_DISTANCE * guidanceScale;
        final double dx = getX() - launchOriginX;
        final double dz = getZ() - launchOriginZ;
        final double distanceFromLaunch = Math.sqrt(dx * dx + dz * dz);
        if (distanceFromLaunch <= straightDistance) {
            return 0.0F;
        }
        final double rampDistance = Math.max(1.0D, TURN_AUTHORITY_RAMP_DISTANCE * guidanceScale);
        final double distanceAuthority = Mth.clamp((distanceFromLaunch - straightDistance) / rampDistance, 0.0D, 1.0D);
        final double delayTicks = GUIDANCE_DELAY_TICKS * guidanceScale;
        final double rampTicks = Math.max(1.0D, GUIDANCE_RAMP_TICKS * guidanceScale);
        final double timeAuthority = launchTicks <= delayTicks
            ? 0.0D
            : Mth.clamp((launchTicks - delayTicks) / rampTicks, 0.0D, 1.0D);
        return (float) (distanceAuthority * timeAuthority);
    }

    public float getVisualRoll(final float partialTick) {
        return Mth.lerp(partialTick, bodyRollO, bodyRoll);
    }

    private double resolveSpeedTarget() {
        if (launchTicks <= BOOSTER_KICK_TICKS) {
            return BOOSTER_PEAK_SPEED;
        }
        if (launchTicks <= BOOSTER_BURN_TICKS) {
            final double sustainProgress = (double) (launchTicks - BOOSTER_KICK_TICKS) / (double) Math.max(BOOSTER_BURN_TICKS - BOOSTER_KICK_TICKS, 1);
            return Mth.lerp(sustainProgress, BOOSTER_PEAK_SPEED, BOOSTER_SUSTAIN_SPEED);
        }
        if (launchTicks <= BOOSTER_BURN_TICKS + BOOSTER_DECAY_TICKS) {
            final double decayProgress = (double) (launchTicks - BOOSTER_BURN_TICKS) / (double) BOOSTER_DECAY_TICKS;
            return Mth.lerp(decayProgress, BOOSTER_SUSTAIN_SPEED, BOOSTER_SETTLE_SPEED);
        }
        final double phaseTarget = switch (flightPhase) {
            case FLIGHT_PHASE_CLIMB -> CLIMB_SPEED_TARGET;
            case FLIGHT_PHASE_TERMINAL -> resolveTerminalSpeedTarget();
            default -> CRUISE_SPEED_TARGET;
        };
        if (launchTicks <= BOOSTER_BURN_TICKS + BOOSTER_DECAY_TICKS + POST_BOOST_RECOVERY_TICKS) {
            final double recoveryProgress = (double) (launchTicks - BOOSTER_BURN_TICKS - BOOSTER_DECAY_TICKS) / (double) POST_BOOST_RECOVERY_TICKS;
            return Mth.lerp(Mth.clamp(recoveryProgress, 0.0D, 1.0D), BOOSTER_SETTLE_SPEED, phaseTarget);
        }
        return phaseTarget;
    }

    private double computeSpeedStep(final double speedTarget) {
        if (launchTicks <= BOOSTER_KICK_TICKS) {
            return BOOSTER_ACCEL_STEP;
        }
        if (launchTicks <= BOOSTER_BURN_TICKS) {
            return BOOSTER_SUSTAIN_STEP;
        }
        if (launchTicks <= BOOSTER_BURN_TICKS + BOOSTER_DECAY_TICKS) {
            return BOOSTER_DECAY_STEP;
        }
        if (launchTicks <= BOOSTER_BURN_TICKS + BOOSTER_DECAY_TICKS + POST_BOOST_RECOVERY_TICKS) {
            return POST_BOOST_ACCEL_STEP;
        }
        return Mth.clamp(Math.abs(speedTarget - flightSpeed) * 0.09D, MIN_SPEED_STEP, MAX_SPEED_STEP);
    }

    private double resolveTerminalSpeedTarget() {
        final double diveFactor = Mth.clamp((-getCoursePitch() - TERMINAL_DIVE_PITCH) / 25.0D, 0.0D, 1.0D);
        return Mth.lerp(diveFactor, TERMINAL_SPEED_TARGET, TERMINAL_SPEED_TARGET + 0.55D);
    }

    private void spawnExhaustParticles() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        final Vec3 forward = getCourseDirection();
        final Vec3 right = Vec3.directionFromRotation(0.0F, getYRot() + MODEL_FORWARD_YAW_OFFSET - 90.0F).normalize();
        final Vec3 exhaustCenter = position()
            .subtract(forward.scale(EXHAUST_REAR_OFFSET))
            .add(0.0D, EXHAUST_VERTICAL_OFFSET, 0.0D);
        final Vec3 exhaustVelocity = forward.scale(-0.42D).add(getDeltaMovement().scale(0.08D));

        if (launchTicks <= BOOSTER_BURN_TICKS) {
            final double rollFactor = Math.max(0.35D, Math.abs(Math.sin(Math.toRadians(bodyRoll))));
            final double plumeSpread = 0.16D + rollFactor * 0.1D;
            spawnExhaustParticleBurst(serverLevel, BOOSTER_SMOKE_PARTICLE, exhaustCenter, right, exhaustVelocity.scale(0.85D), 6, plumeSpread, 0.05D);
            spawnExhaustParticleBurst(serverLevel, ParticleTypes.CLOUD, exhaustCenter, right, exhaustVelocity.scale(0.62D), 4, plumeSpread + 0.06D, 0.04D);
            if ((tickCount & 1) == 0) {
                spawnExhaustParticleBurst(serverLevel, ParticleTypes.CLOUD, exhaustCenter, right, exhaustVelocity.scale(0.35D), 3, plumeSpread + 0.1D, 0.03D);
            }
        }
    }

    private void spawnExhaustParticleBurst(final ServerLevel serverLevel,
                                           final ParticleOptions particle,
                                           final Vec3 exhaustCenter,
                                           final Vec3 right,
                                           final Vec3 baseVelocity,
                                           final int count,
                                           final double sideSpread,
                                           final double randomVelocitySpread) {
        for (int i = 0; i < count; ++i) {
            final double lateral = (random.nextDouble() - 0.5D) * 2.0D * Math.min(sideSpread, EXHAUST_SIDE_SPREAD);
            final double vertical = (random.nextDouble() - 0.5D) * 0.12D;
            final Vec3 spawnPos = exhaustCenter.add(right.scale(lateral)).add(0.0D, vertical, 0.0D);
            final Vec3 velocity = baseVelocity.add(
                (random.nextDouble() - 0.5D) * randomVelocitySpread,
                (random.nextDouble() - 0.5D) * randomVelocitySpread,
                (random.nextDouble() - 0.5D) * randomVelocitySpread
            );
            serverLevel.sendParticles(particle, spawnPos.x, spawnPos.y, spawnPos.z, 1, velocity.x, velocity.y, velocity.z, 0.0D);
        }
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
        BlastLightRefresh.schedule(serverLevel, position(), TNT_POWER);
        tnt.discard();
    }

    private void dropAsItem() {
        EntityDrops.spawnAtLocation(this, createItemStack());
    }

    private void ensureChunkTicket() {
        if (level() instanceof ServerLevel serverLevel) {
            ChunkLoadManager.ensureChunksLoaded(serverLevel, getId(), chunkPosition(), FLAMINGO_CHUNK_RADIUS);
        }
    }

    private void releaseChunkTicket() {
        if (level() instanceof ServerLevel serverLevel) {
            ChunkLoadManager.releaseChunks(serverLevel, getId());
        }
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
