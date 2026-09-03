package com.fullfud.fullfud.common.entity;

import com.fullfud.fullfud.common.entity.drone.DroneServiceBay;
import com.fullfud.fullfud.common.entity.drone.WarheadCharge;
import com.fullfud.fullfud.common.item.MonitorItem;
import com.fullfud.fullfud.common.item.ScrewdriverItem;
import com.fullfud.fullfud.common.menu.DroneServiceMenu;
import com.fullfud.fullfud.core.BlastLightRefresh;
import com.fullfud.fullfud.core.EntityDrops;
import com.fullfud.fullfud.core.FullfudAdvancements;
import com.fullfud.fullfud.core.FullfudDataComponents;
import com.fullfud.fullfud.core.FullfudRegistries;
import com.fullfud.fullfud.core.DroneExplosionEffects;
import com.fullfud.fullfud.core.DroneExplosionLimiter;
import com.fullfud.fullfud.core.RemoteControlFailsafe;
import com.fullfud.fullfud.core.RemotePlayerProtection;
import com.fullfud.fullfud.core.data.PersistentData;
import com.fullfud.fullfud.core.data.ShahedLinkData;
import com.fullfud.fullfud.core.network.FullfudNetwork;
import com.fullfud.fullfud.core.ChunkLoadManager;
import com.fullfud.fullfud.core.config.FullfudServerConfig;
import com.fullfud.fullfud.core.network.packet.DroneAudioLoopPacket;
import com.fullfud.fullfud.core.network.packet.DroneAudioOneShotPacket;
import com.fullfud.fullfud.core.network.packet.ShahedControlPacket;
import com.fullfud.fullfud.core.network.packet.ShahedGhostUpdatePacket;
import com.fullfud.fullfud.core.network.packet.ShahedLinkPacket;
import com.fullfud.fullfud.core.network.packet.ShahedStatusPacket;
import com.fullfud.fullfud.common.menu.ShahedMonitorMenu;
import dev.lazurite.lattice.api.player.LatticeServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ShahedDroneEntity extends Entity implements GeoEntity {
    public static final String PLAYER_REMOTE_TAG = "fullfud_shahed_remote";

    private static final String PLAYER_TAG_DRONE = "Drone";
    private static final String PLAYER_TAG_ORIGIN_DIM = "OriginDim";
    private static final String PLAYER_TAG_ORIGIN_X = "OriginX";
    private static final String PLAYER_TAG_ORIGIN_Y = "OriginY";
    private static final String PLAYER_TAG_ORIGIN_Z = "OriginZ";
    private static final String PLAYER_TAG_ORIGIN_YAW = "OriginYaw";
    private static final String PLAYER_TAG_ORIGIN_PITCH = "OriginPitch";
    private static final String PLAYER_TAG_ORIGIN_GM = "OriginGM";
    private static final EntityDataAccessor<Float> DATA_THRUST = SynchedEntityData.defineId(ShahedDroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(ShahedDroneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_ON_LAUNCHER = SynchedEntityData.defineId(ShahedDroneEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_ROLL = SynchedEntityData.defineId(ShahedDroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SERVER_YAW = SynchedEntityData.defineId(ShahedDroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SERVER_PITCH = SynchedEntityData.defineId(ShahedDroneEntity.class, EntityDataSerializers.FLOAT);

    private static final String TAG_THRUST = "Thrust";
    private static final String TAG_MOTION = "Motion";
    private static final String TAG_OWNER = "Owner";
    private static final String TAG_OWNER_VIEW = "OwnerView";
    private static final String TAG_ARMED = "Armed";
    private static final String TAG_LAUNCH_Y = "LaunchY";
    private static final String TAG_BODY_YAW = "BodyYaw";
    private static final String TAG_BODY_PITCH = "BodyPitch";
    private static final String TAG_BODY_ROLL = "BodyRoll";
    private static final String TAG_SERVER_YAW = "ServerYaw";
    private static final String TAG_SERVER_PITCH = "ServerPitch";
    private static final String TAG_REMOTE_INIT = "RemoteInit";
    private static final String TAG_COLOR = "Color";
    private static final String TAG_ON_LAUNCHER = "OnLauncher";
    private static final String TAG_LAUNCHER_UUID = "LauncherUUID";
    private static final String TAG_PROJECTILE_HITS = "ProjectileHits";
    private static final String TAG_DAMAGE_TARGET_SPEED = "DamageTargetSpeed";
    private static final String TAG_LINEAR_VELOCITY = "LinearVelocity";
    private static final String TAG_FUEL = "FuelMass";
    private static final String TAG_SERVICE_BAY = "ServiceBay";
    private static final String TAG_SPEED_SCALE = "SpeedScale";
    
    private static final String TAG_SESS_X = "SessX";
    private static final String TAG_SESS_Y = "SessY";
    private static final String TAG_SESS_Z = "SessZ";
    private static final String TAG_SESS_YAW = "SessYaw";
    private static final String TAG_SESS_PITCH = "SessPitch";
    private static final String TAG_SESS_DIM = "SessDim";
    private static final String TAG_SESS_GM = "SessGM";
    private static final String TAG_CONTROLLER = "ControllerUUID";
    private static final String TAG_KEEP_CHUNKS = "KeepChunks";

    private static final int STATUS_INTERVAL = 1;
    private static final int GHOST_BROADCAST_INTERVAL_TICKS = 2;
    private static final double GHOST_BROADCAST_RANGE_BLOCKS = 10000.0D;
    private static final int CONTROL_TIMEOUT_TICKS = 20;
    private static final double REMOTE_PROTECTION_RADIUS = 24.0D;
    private static final float FORCE_RELEASE_FAILSAFE_THRUST = 0.60F;
    private static final double TICK_SECONDS = 1.0D / 20.0D;
    private static final double BASE_MASS_KG = 210.0D;
    /** Public because the monitor's LOW_FUEL alert needs the full tank to turn the telemetry figure into a percentage. */
    public static final double FUEL_CAPACITY_KG = 45.0D;
    private static final double FUEL_CONSUMPTION_PER_SEC = 0.9286D;
    private static final double MAX_THRUST_FORCE = 1500.0D;
    private static final double THRUST_CURVE_EXPONENT = 2.0D;
    private static final double RHO_SEA_LEVEL = 1.225D;
    private static final double ATMOSPHERE_SCALE_HEIGHT = 8500.0D;
    private static final double WING_AREA = 4.0D;
    private static final double CL_ALPHA = 2.5D;
    private static final double CL_MAX = 1.5D;
    private static final double CL_ZERO = 0.25D;
    private static final double CD_MIN = 0.052D;
    private static final double DRAG_FACTOR = 0.055D;
    private static final double CY_BETA = -0.85D;
    private static final double GRAVITY = 9.81D;
    private static final double MAX_ROLL_RATE = 60.0D;
    private static final double MAX_PITCH_RATE = 45.0D;
    private static final double ROLL_ACCEL = 120.0D;
    private static final double PITCH_ACCEL = 90.0D;
    private static final double GROUND_FRICTION = 0.62D;
    private static final double MAX_AIRSPEED = 72.222D;
    private static final double INITIAL_LAUNCH_SPEED = 55.5D;
    private static final double ENGINE_IDLE_THRUST = 0.0D;
    private static final double ENGINE_SPOOL_RATE = 0.05D;
    private static final double STALL_ANGLE = Math.toRadians(17.0D);
    private static final double PROJECTILE_DAMAGE_DECEL_PER_SEC = 24.0D;
    private static final double DAMAGE_SMOKE_PARTICLES_PER_TICK = 7.0D / 20.0D;
    private static final double DAMAGE_SMOKE_SPREAD = 0.7D;
    private static final double SLOW_SPEED_SCALE = 0.5D;
    /** Airspeed floor for the bank-to-turn rate, so a slow or stalled airframe cannot pivot on the spot. */
    private static final double MIN_TURN_AIRSPEED = 20.0D;
    /** How fast the monitor's mouse contribution to the commanded attitude falls back to centre, per second. */
    private static final double MOUSE_ATTITUDE_CENTER_PER_SEC = 2.5D;
    private static final int SHAHED_CHUNK_RADIUS = 4;
    /**
     * How long an armed airframe may sit inside a block before it is written off as wedged and detonated.
     * Half a second: long enough that a single frame of overlap coming out of a chunk load does not count.
     */
    private static final int WEDGED_DETONATE_TICKS = 10;
    /**
     * How far the hull is grown before asking whether it is against terrain. Contact counts, so this is
     * generous rather than exact: an airframe stopped flush against a wall reads the same as one inside it.
     */
    private static final double WEDGE_CONTACT_MARGIN = 0.02D;
    /**
     * Squared per-tick travel below which the airframe counts as going nowhere: 0.1 blocks a tick, two
     * blocks a second, against a cruise of roughly thirty-six.
     */
    private static final double WEDGE_STALL_EPS_SQR = 0.01D;
    private static final EntityDimensions SHAHEED_DIMENSIONS = EntityDimensions.scalable(3.0F, 1.0F);
    private final Map<UUID, Integer> viewerDistances = new HashMap<>();
    private float controlForward;
    private float controlStrafe;
    private float controlVertical;
    private float inputMousePitchDelta;
    private float inputMouseRollDelta;
    private double mousePitchOffsetDeg;
    private double mouseRollOffsetDeg;
    private Vec3 linearVelocity = Vec3.ZERO;
    private Vec3 lastFlightStart = Vec3.ZERO;
    private double crippledHorizontalTargetSpeed = -1.0D;
    private double damageSmokeAccumulator;
    private int projectileHitCount;
    private int controlTimeout;
    private int menuGraceTicks;
    private int wedgedTicks;
    /** Set on any tick the flight was held for terrain that had not loaded — see {@link #isWedgedInTerrain}. */
    private boolean terrainStalled;
    private double rollRate;
    private double pitchRate;
    private UUID ownerUUID;
    private int ownerViewDistance = 8;
    private int desiredChunkRadius;
    private float jammerOverride;
    private boolean jammerSuppressControls;
    private static final double JAMMER_HARD_RADIUS = 300.0D;
    private static final double JAMMER_MAX_RADIUS = 600.0D;
    private boolean armed;
    private boolean detonating;
    private double launchBaselineY;
    private double speedScale = 1.0D;
    private UUID controllingPlayer;
    private ControlSession controlSession;
    // Optional mode: keep drone chunks loaded even without a controlling player.
    private boolean keepChunksLoadedWithoutPlayer;

    private boolean lastEngineActiveAudio;
    private float lastThrustAudio;

    private static final byte AUDIO_TYPE_SHAHED = 1;
    private static final byte AUDIO_KIND_START = 1;
    private static final byte AUDIO_KIND_STOP = 2;
    private static final float ENGINE_ACTIVE_THRESHOLD = 0.02F;
    private static final float ENGINE_IDLE_AUDIO_MIX = 0.22F;
    private static final double SHAHED_AUDIO_RANGE_BLOCKS = 800.0D;
    // Explosion tuning: approximate TNT-equivalent power for visual/terrain effects (Explosion Overhaul uses power).
    private static final float SHAHED_FIREBALL_POWER = 15.0F;
    /** One canister is a third of the tank, so a full sortie takes three. */
    private static final double FUEL_PER_CANISTER_KG = FUEL_CAPACITY_KG / 3.0D;
    /** How far a player may drift from an open service bay before it closes. 8 blocks. */
    private static final double SERVICE_REACH_SQR = 64.0D;
    // A fresh airframe arrives dry. Fuel has to be crafted and loaded through the service bay, and
    // computeEffectiveThrust already returns nothing at zero mass, so an unfuelled Shahed simply will
    // not fly rather than needing a separate check.
    private double fuelMass = 0.0D;
    private DroneServiceBay serviceBay;
    private FlightTelemetry telemetry = FlightTelemetry.ZERO;
    private double bodyYaw;
    private double bodyPitch;
    private double bodyRoll;
    private double bodyPitchO;
    private double bodyRollO;

    private final Quaternionf rotationQuaternion = new Quaternionf();
    private final Vector3f eulerAngles = new Vector3f();
    private final Vector3f axisVector = new Vector3f();
    private double engineOutput;
    private boolean remoteInitialized;
    private ChunkPos lastSentViewCenter;
    private int viewPointResyncCooldown;
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.model.idle");
    private static final RawAnimation RUN_ANIMATION = RawAnimation.begin().thenLoop("animation.model.running");
    private static final double LAUNCHER_VERTICAL_OFFSET = 0.25D;
    private static final double LAUNCHER_FORWARD_OFFSET = 2.0D;
    private static final double LAUNCHER_UP_OFFSET = 10.0D;
    private static final double LAUNCHER_LAUNCH_SPEED = 260.0D / 3.6D;
    private static final float LAUNCHER_LAUNCH_PITCH = -12.5F;
    private int mountedLauncherId = -1;
    private UUID mountedLauncherUuid;

    private int lerpSteps;
    private double xO;
    private double yO;
    private double zO;

    public ShahedDroneEntity(final EntityType<? extends ShahedDroneEntity> entityType, final Level level) {
        super(entityType, level);
        this.noPhysics = false;
        this.setNoGravity(true);
        this.launchBaselineY = this.getY();
        this.bodyYaw = this.getYRot();
        this.bodyPitch = 0.0D;
        this.bodyPitchO = 0.0D;
        this.bodyRoll = 0.0D;
        this.bodyRollO = 0.0D;
        this.setXRot((float) bodyPitch);
        this.engineOutput = 0.0D;
        this.remoteInitialized = false;
        updateBoundingBox();
        this.refreshDimensions();
    }

    public static Optional<ShahedDroneEntity> find(final ServerLevel level, final UUID uuid) {
        final Entity entity = level.getEntity(uuid);
        if (entity instanceof ShahedDroneEntity drone) {
            return Optional.of(drone);
        }
        return Optional.empty();
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        builder.define(DATA_THRUST, 0.25F);
        builder.define(DATA_COLOR, ShahedColor.WHITE.getId());
        builder.define(DATA_ON_LAUNCHER, false);
        builder.define(DATA_ROLL, 0.0F);
        builder.define(DATA_SERVER_YAW, getYRot());
        builder.define(DATA_SERVER_PITCH, getXRot());
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide()) {
            updateJammerState();
            // Only before arming: topping the tank up mid-flight would be free range.
            if (!armed) {
                installFuelFromBay();
            }
        }

        if (isOnLauncher()) {
            if (!level().isClientSide) {
                handleLauncherAttachment();
                broadcastEngineAudio();
            }
            return;
        }

        bodyPitchO = bodyPitch;
        bodyRollO = bodyRoll;

        if (level().isClientSide()) {
            this.bodyRoll = this.entityData.get(DATA_ROLL);
        }

        if (!level().isClientSide() || isControlledByLocalInstance()) {
            if (!level().isClientSide() && controllingPlayer == null) {
                bodyPitch = bodyPitch * 0.9f;
                bodyRoll = bodyRoll * 0.9f;
            }
            this.setXRot((float) bodyPitch);
            updateControlTimeout();
            if (!level().isClientSide()) {
                lastFlightStart = position();
            }
            updateFlight();
            
            if (!level().isClientSide()) {
                this.entityData.set(DATA_ROLL, (float) bodyRoll);
            }
        }

        handleClientSync();

        if (level().isClientSide() && !isControlledByLocalInstance()) {
            this.bodyPitch = this.getXRot();
            this.bodyYaw = this.getYRot();
            updateBoundingBox();
        }

        if (level().isClientSide() && isControlledByLocalInstance()) {
            this.setYRot((float) bodyYaw);
            this.setXRot((float) bodyPitch);
        }

        if (!level().isClientSide()) {
            updateLaunchState();
            final ServerPlayer cp = getControllingPlayer();
            if (armed) {
                final Vec3 blockImpact = resolveBlockImpactOrigin();
                if (blockImpact != null) {
                    detonate(blockImpact);
                    return;
                }
                if (isWedgedInTerrain()) {
                    detonate(position());
                    return;
                }
                List<Entity> collisions = level().getEntities(this, getBoundingBox().inflate(0.3D), e -> !e.isSpectator() && e.isPickable());
                for (Entity entity : collisions) {
                    if (controllingPlayer != null && entity instanceof ServerPlayer sp && controllingPlayer.equals(sp.getUUID())) continue;
                    if (level() instanceof ServerLevel serverLevel) {
                        DroneExplosionEffects.applyDirectImpactVehicleDamage(serverLevel, this, cp, entity);
                    }
                    detonate(resolveEntityImpactOrigin(lastFlightStart, position(), entity));
                    return;
                }
            }
            if (isSignalLostFor(cp)) {
                releaseCameraFor(cp);
            }
            ensureChunkTicket();
            updateControllerBinding();
            if (cp != null) {
                syncRemoteController(cp);
                syncViewCenter(cp);
            }
            broadcastEngineAudio();
            if (tickCount % GHOST_BROADCAST_INTERVAL_TICKS == 0) {
                broadcastGhostState();
            }
            if (tickCount % STATUS_INTERVAL == 0) {
                broadcastStatus();
            }
        }
    }

    private void broadcastEngineAudio() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (tickCount % 4 != 0) {
            return;
        }

        final float engineMix = getAudioEngineMix();
        final boolean engineActive = engineMix > ENGINE_ACTIVE_THRESHOLD;

        final double range = SHAHED_AUDIO_RANGE_BLOCKS;
        final double rangeSqr = range * range;

        if (engineActive != lastEngineActiveAudio) {
            final byte kind = engineActive ? AUDIO_KIND_START : AUDIO_KIND_STOP;
            final float strength = engineActive ? engineMix : lastThrustAudio;
            for (final ServerPlayer player : serverLevel.players()) {
                final boolean controlling = controllingPlayer != null && controllingPlayer.equals(player.getUUID());
                final double distSqr = controlling ? 0.0D : player.distanceToSqr(this);
                if (!controlling && distSqr > rangeSqr) {
                    continue;
                }
                final float distanceFactor = distanceFactor(distSqr, range, 1.4D);
                float volume = (0.25F + 0.75F * strength) * distanceFactor;
                if (!controlling && isOccluded(serverLevel, player)) {
                    volume *= 0.45F;
                }
                if (volume <= 0.001F) {
                    continue;
                }
                final float pitch = 0.9F + 0.2F * strength;
                FullfudNetwork.sendToPlayer(player,
                    new DroneAudioOneShotPacket(AUDIO_TYPE_SHAHED, kind, getUUID(), getX(), getY(), getZ(), volume, pitch));
            }
        }

        if (engineActive) {
            final Vec3 motion = getDeltaMovement();
            final double speed = motion.length();
            final float speedFactor = (float) Mth.clamp(speed / 1.8D, 0.0D, 1.0D);
            final float flightVolumeMult = 1.0F + speedFactor * 0.35F;
            final float pitch = 0.85F + engineMix * 0.35F + speedFactor * 0.12F;
            final float base = (0.3F + engineMix * 0.7F) * flightVolumeMult;

            for (final ServerPlayer player : serverLevel.players()) {
                final boolean controlling = controllingPlayer != null && controllingPlayer.equals(player.getUUID());
                final double distSqr = controlling ? 0.0D : player.distanceToSqr(this);
                if (!controlling && distSqr > rangeSqr) {
                    continue;
                }
                final float distanceFactor = distanceFactor(distSqr, range, 1.4D);
                float volume = base * distanceFactor;
                if (!controlling && isOccluded(serverLevel, player)) {
                    volume *= 0.45F;
                }
                FullfudNetwork.sendToPlayer(player,
                    new DroneAudioLoopPacket(AUDIO_TYPE_SHAHED, getUUID(), getX(), getY(), getZ(), volume, pitch, true));
            }
        } else if (lastEngineActiveAudio) {
            for (final ServerPlayer player : serverLevel.players()) {
                final boolean controlling = controllingPlayer != null && controllingPlayer.equals(player.getUUID());
                final double distSqr = controlling ? 0.0D : player.distanceToSqr(this);
                if (!controlling && distSqr > rangeSqr) {
                    continue;
                }
                FullfudNetwork.sendToPlayer(player,
                    new DroneAudioLoopPacket(AUDIO_TYPE_SHAHED, getUUID(), getX(), getY(), getZ(), 0.0F, 1.0F, false));
            }
        }

        lastEngineActiveAudio = engineActive;
        lastThrustAudio = engineMix;
    }

    private static float distanceFactor(final double distSqr, final double range, final double exponent) {
        final double dist = Math.sqrt(Math.max(0.0D, distSqr));
        final double norm = Mth.clamp(dist / range, 0.0D, 1.0D);
        return (float) Math.pow(1.0D - norm, exponent);
    }

    private boolean isOccluded(final ServerLevel level, final ServerPlayer player) {
        if (level == null || player == null) {
            return false;
        }
        final Vec3 from = position().add(0.0D, 0.75D, 0.0D);
        final Vec3 to = player.position().add(0.0D, player.getEyeHeight(), 0.0D);
        final BlockHitResult result = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return result.getType() != HitResult.Type.MISS;
    }

    // 1.21.2 dropped lerpTo's trailing teleport flag; the interpolation is otherwise unchanged.
    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int posRotationIncrements) {
        this.xO = x;
        this.yO = y;
        this.zO = z;
        this.lerpSteps = 10;
    }

    public float getVisualRoll(float partialTick) {
        return (float) Mth.lerp(partialTick, bodyRollO, bodyRoll);
    }

    public float getVisualPitch(float partialTick) {
        return (float) Mth.lerp(partialTick, bodyPitchO, bodyPitch);
    }

    private void handleClientSync() {
        if (level() instanceof ServerLevel && tickCount % 2 == 0) {
            entityData.set(DATA_SERVER_YAW, getYRot());
            entityData.set(DATA_SERVER_PITCH, getXRot());
        }
        if (isControlledByLocalInstance()) {
            lerpSteps = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
        }
        if (lerpSteps <= 0) {
            return;
        }

        final double interpolatedX = getX() + (xO - getX()) / (double) lerpSteps;
        final double interpolatedY = getY() + (yO - getY()) / (double) lerpSteps;
        final double interpolatedZ = getZ() + (zO - getZ()) / (double) lerpSteps;

        final float diffY = Mth.wrapDegrees(entityData.get(DATA_SERVER_YAW) - this.getYRot());
        final float diffX = Mth.wrapDegrees(entityData.get(DATA_SERVER_PITCH) - this.getXRot());

        this.setYRot(this.getYRot() + 0.1f * diffY);
        this.setXRot(this.getXRot() + 0.1f * diffX);

        setPos(interpolatedX, interpolatedY, interpolatedZ);

        --lerpSteps;
    }

    public void applyClientGhostState(final double x,
                                      final double y,
                                      final double z,
                                      final double velocityX,
                                      final double velocityY,
                                      final double velocityZ,
                                      final float yaw,
                                      final float pitch,
                                      final float roll,
                                      final float thrust,
                                      final int colorId,
                                      final boolean onLauncher) {
        this.setPos(x, y, z);
        this.setYRot(Mth.wrapDegrees(yaw));
        this.yRotO = this.getYRot();
        this.setXRot(Mth.clamp(pitch, -90.0F, 90.0F));
        this.xRotO = this.getXRot();
        this.bodyYaw = this.getYRot();
        this.bodyPitch = this.getXRot();
        this.bodyPitchO = this.bodyPitch;
        this.bodyRoll = Mth.wrapDegrees(roll);
        this.bodyRollO = this.bodyRoll;
        this.entityData.set(DATA_ROLL, (float) this.bodyRoll);
        this.entityData.set(DATA_THRUST, Mth.clamp(thrust, 0.0F, 1.0F));
        this.entityData.set(DATA_COLOR, ShahedColor.byId(colorId).getId());
        this.entityData.set(DATA_ON_LAUNCHER, onLauncher);
        this.noPhysics = onLauncher;
        this.setNoGravity(onLauncher);
        this.setDeltaMovement(velocityX, velocityY, velocityZ);
        this.linearVelocity = new Vec3(velocityX / TICK_SECONDS, velocityY / TICK_SECONDS, velocityZ / TICK_SECONDS);
        this.lerpSteps = 0;
        updateBoundingBox();
    }

    private void updateControlTimeout() {
        if (controlTimeout > 0) {
            controlTimeout--;
        } else {
            controlForward = 0.0F;
            controlStrafe = 0.0F;
            controlVertical = 0.0F;
            inputMousePitchDelta = 0.0F;
            inputMouseRollDelta = 0.0F;
            mousePitchOffsetDeg = 0.0D;
            mouseRollOffsetDeg = 0.0D;
        }

        if (menuGraceTicks > 0) {
            menuGraceTicks--;
        }
    }

    private void updateFlight() {
        final double dt = TICK_SECONDS;
        final double scale = resolveSpeedScale();
        float throttle = Mth.clamp(getThrust(), 0.0F, 1.0F);
        
        if (fuelMass > 0.0D && throttle > 0.0F) {
            double burn = Math.pow(throttle, 1.5D) * FUEL_CONSUMPTION_PER_SEC * dt;
            if (throttle < 0.8F) {
                burn *= 0.6D; 
            }
            fuelMass = Math.max(0.0D, fuelMass - burn);
        }
        if (fuelMass <= 0.0D) {
            fuelMass = 0.0D;
            if (throttle > 0.0F) {
                throttle = 0.0F;
                setThrust(0.0F);
            }
        }
        
        final double totalMass = BASE_MASS_KG + fuelMass;
        engineOutput += (throttle - engineOutput) * ENGINE_SPOOL_RATE;
        final double thrustForce = fuelMass <= 0.0D ? 0.0D : computeEffectiveThrust(engineOutput) * scale;

        integrateAttitude(dt);

        final Vec3 forward = directionFromQuaternion(0.0F, 0.0F, 1.0F);
        final Vec3 localUp = directionFromQuaternion(0.0F, 1.0F, 0.0F);

        final double pitchRad = Math.toRadians(bodyPitch);

        final double speed = linearVelocity.length();
        final double altitude = this.getY();
        final double airDensity = sampleAirDensity(altitude);
        final double aeroScale = scale > 0.0D ? (1.0D / scale) : 1.0D;
        final double q = 0.5D * airDensity * speed * speed * aeroScale;

        final double velocityPitch = speed > 0.01 ? Math.atan2(linearVelocity.y, Math.sqrt(linearVelocity.x * linearVelocity.x + linearVelocity.z * linearVelocity.z)) : -pitchRad;
        
        final double aoa = -pitchRad - velocityPitch;
        
        final double cl = applyStall(resolveLiftCoefficient(aoa), aoa);
        final double liftMagnitude = cl * q * WING_AREA;
        final Vec3 liftVector = localUp.scale(liftMagnitude);

        final double cd = CD_MIN + DRAG_FACTOR * cl * cl;
        final Vec3 dragVector = linearVelocity.normalize().scale(-cd * q * WING_AREA);
        
        final Vec3 thrustVector = forward.scale(thrustForce);
        final Vec3 gravityVector = new Vec3(0, -totalMass * GRAVITY * scale, 0);

        Vec3 netForce = thrustVector.add(liftVector).add(dragVector).add(gravityVector);
        
        final Vec3 acceleration = netForce.scale(1.0D / totalMass);
        linearVelocity = linearVelocity.add(acceleration.scale(dt));

        applySideslipDamping(dt, forward);

        final double maxAirSpeed = MAX_AIRSPEED * scale;
        final double speedCapSq = maxAirSpeed * maxAirSpeed;
        if (linearVelocity.lengthSqr() > speedCapSq) {
            linearVelocity = linearVelocity.normalize().scale(maxAirSpeed);
        }

        applyProjectileDamageEffects(dt);

        final Vec3 velPerTick = linearVelocity.scale(TICK_SECONDS);
        terrainStalled = !isFlightPathTerrainReady(velPerTick);
        final Vec3 movePerTick = terrainStalled ? Vec3.ZERO : velPerTick;
        this.setDeltaMovement(movePerTick);
        this.hasImpulse = true;
        this.move(MoverType.SELF, movePerTick);
        updateBoundingBox();
        resolveCollisionVelocity();

        final float displayScale = (float) (scale > 0.0D ? (1.0D / scale) : 1.0D);
        final double actualVerticalSpeed = dt > 1.0E-6D
            ? (this.getY() - lastFlightStart.y) / dt
            : linearVelocity.y;
        telemetry = new FlightTelemetry(
            (float) speed * displayScale,
            (float) Math.sqrt(linearVelocity.x * linearVelocity.x + linearVelocity.z * linearVelocity.z) * displayScale,
            (float) actualVerticalSpeed,
            (float) Math.toDegrees(aoa),
            0.0F,
            throttle,
            (float) fuelMass,
            (float) airDensity
        );
    }

    private void integrateAttitude(final double dt) {
        if (FullfudServerConfig.SERVER.shahedBankControl.get()) {
            integrateAttitudeBanked(dt);
            return;
        }
        integrateAttitudeRates(dt);
    }

    /**
     * Bank-to-turn attitude model: A/D picks a bank angle, W/S picks a pitch attitude, and the bank is what
     * changes heading.
     *
     * <p>The rate model below is aerobatically honest but does not fly: banking tilts the lift vector sideways,
     * {@link #applySideslipDamping} then removes exactly that sideways velocity, and nothing ever yaws the
     * airframe — so a banked Shahed tracks almost straight ahead. A real aircraft turns because the sideways
     * lift accelerates it into the turn <em>and</em> the fin drags the nose around after it; the standard
     * coordinated-turn rate for that is {@code g*tan(bank)/airspeed}, which is what gets integrated into the
     * heading here. Sideslip damping stops being a fight and becomes the thing that keeps the velocity pointing
     * where the nose does.
     *
     * <p>Angles are integrated in Euler space rather than through a body-frame quaternion delta, because with an
     * attitude command the roll/pitch coupling of the delta form is a disturbance the controller has to cancel
     * rather than a behaviour anyone asked for. The quaternion is rebuilt from the result, so everything
     * downstream (lift direction, camera, renderer) is unchanged.
     */
    private void integrateAttitudeBanked(final double dt) {
        final double rateScale = resolveSpeedScale();
        final double maxBank = FullfudServerConfig.SERVER.shahedMaxBankDegrees.get();
        final double maxPitch = FullfudServerConfig.SERVER.shahedMaxPitchDegrees.get();
        final double gain = FullfudServerConfig.SERVER.shahedAttitudeGain.get();

        // The monitor sends mouse movement as a rotation delta with no centre of its own, so it rides on top of
        // the key command and bleeds away; without that, one drag would leave the Shahed banked for good.
        final double decay = Math.max(0.0D, 1.0D - MOUSE_ATTITUDE_CENTER_PER_SEC * dt);
        mousePitchOffsetDeg = Mth.clamp(
            mousePitchOffsetDeg * decay + Math.toDegrees(inputMousePitchDelta), -maxPitch, maxPitch);
        mouseRollOffsetDeg = Mth.clamp(
            mouseRollOffsetDeg * decay + Math.toDegrees(inputMouseRollDelta), -maxBank, maxBank);
        inputMousePitchDelta = 0.0F;
        inputMouseRollDelta = 0.0F;

        final double commandedRoll = Mth.clamp(controlStrafe * maxBank + mouseRollOffsetDeg, -maxBank, maxBank);
        final double commandedPitch = Mth.clamp(controlForward * maxPitch + mousePitchOffsetDeg, -maxPitch, maxPitch);

        final double maxRollRate = Math.toRadians(MAX_ROLL_RATE * rateScale);
        final double maxPitchRate = Math.toRadians(MAX_PITCH_RATE * rateScale);
        final double targetRollRate = Mth.clamp(
            Math.toRadians(commandedRoll - bodyRoll) * gain, -maxRollRate, maxRollRate);
        final double targetPitchRate = Mth.clamp(
            Math.toRadians(commandedPitch - bodyPitch) * gain, -maxPitchRate, maxPitchRate);
        rollRate = approach(rollRate, targetRollRate, Math.toRadians(ROLL_ACCEL * rateScale) * dt);
        pitchRate = approach(pitchRate, targetPitchRate, Math.toRadians(PITCH_ACCEL * rateScale) * dt);

        bodyRoll = Mth.clamp(bodyRoll + Math.toDegrees(rollRate) * dt, -maxBank, maxBank);
        bodyPitch = Mth.clamp(bodyPitch + Math.toDegrees(pitchRate) * dt, -85.0D, 85.0D);

        final double turnRate = FullfudServerConfig.SERVER.shahedDirectTurn.get()
            ? directTurnRate(bodyRoll, maxBank)
            : coordinatedTurnRate(bodyRoll);
        final double yawDeltaDegrees = Math.toDegrees(turnRate) * dt;
        bodyYaw = Mth.wrapDegrees(bodyYaw + yawDeltaDegrees);
        if (FullfudServerConfig.SERVER.shahedDirectTurn.get()) {
            steerVelocityWithHeading(yawDeltaDegrees);
        }

        if (!Double.isFinite(bodyRoll)) {
            bodyRoll = 0.0D;
        }
        if (!Double.isFinite(bodyPitch)) {
            bodyPitch = 0.0D;
        }
        if (!Double.isFinite(bodyYaw)) {
            bodyYaw = this.getYRot();
        }

        syncQuaternionFromBodyAngles();

        this.setXRot((float) bodyPitch);
        this.setYRot((float) bodyYaw);
        this.setYHeadRot((float) bodyYaw);
    }

    /**
     * Heading rate the bank produces when direct turning is on: a fixed rate scaled by how far into the bank the
     * airframe is, so the turn starts as the wings drop and stops as they level.
     *
     * <p>Nothing about it is aerodynamic — {@link #coordinatedTurnRate} is the honest version, and at cruise
     * speed it needs a circle several hundred blocks across. This one turns inside a few dozen, which is what
     * makes the drone steerable from a monitor screen.
     */
    private double directTurnRate(final double bankDegrees, final double maxBankDegrees) {
        final double bankFraction = Mth.clamp(bankDegrees / Math.max(1.0D, maxBankDegrees), -1.0D, 1.0D);
        final double rate = Math.toRadians(FullfudServerConfig.SERVER.shahedDirectTurnDegreesPerSecond.get()) * bankFraction;
        return Double.isFinite(rate) ? rate : 0.0D;
    }

    /**
     * Carries the horizontal velocity around with the heading, so a turn changes where the drone is going and not
     * only where it is pointing. Without this the flight path only bends as fast as {@link #applySideslipDamping}
     * scrubs off the sideways component, which reads as the airframe sliding sideways through its own turn.
     */
    private void steerVelocityWithHeading(final double yawDeltaDegrees) {
        if (yawDeltaDegrees == 0.0D || !Double.isFinite(yawDeltaDegrees)) {
            return;
        }
        final double radians = Math.toRadians(yawDeltaDegrees);
        final double cos = Math.cos(radians);
        final double sin = Math.sin(radians);
        // Minecraft yaw grows clockwise from +Z, so a heading of psi points at (-sin psi, cos psi): adding to the
        // yaw rotates the horizontal components this way round.
        linearVelocity = new Vec3(
            linearVelocity.x * cos - linearVelocity.z * sin,
            linearVelocity.y,
            linearVelocity.z * cos + linearVelocity.x * sin
        );
    }

    /**
     * Heading rate, in radians per second, that a bank of {@code bankDegrees} produces at the current airspeed.
     * Positive bank is right wing down, and in Minecraft's yaw convention turning right means increasing yaw.
     */
    private double coordinatedTurnRate(final double bankDegrees) {
        final double airspeed = Math.max(MIN_TURN_AIRSPEED, linearVelocity.length());
        final double rate = GRAVITY * Math.tan(Math.toRadians(Mth.clamp(bankDegrees, -80.0D, 80.0D))) / airspeed;
        final double cap = Math.toRadians(FullfudServerConfig.SERVER.shahedMaxTurnRateDegreesPerSecond.get());
        return Double.isFinite(rate) ? Mth.clamp(rate, -cap, cap) : 0.0D;
    }

    private void integrateAttitudeRates(final double dt) {
        syncQuaternionFromBodyAngles();

        final double rateScale = resolveSpeedScale();
        final double targetRollRateDegPerSec = controlStrafe * MAX_ROLL_RATE * rateScale;
        rollRate = approach(rollRate, Math.toRadians(targetRollRateDegPerSec), Math.toRadians(ROLL_ACCEL * rateScale) * dt);

        final double targetPitchRateDegPerSec = controlForward * MAX_PITCH_RATE * rateScale;
        pitchRate = approach(pitchRate, Math.toRadians(targetPitchRateDegPerSec), Math.toRadians(PITCH_ACCEL * rateScale) * dt);

        final float pitchDeltaRad = (float) (pitchRate * dt) + inputMousePitchDelta;
        final float rollDeltaRad = (float) (rollRate * dt) + inputMouseRollDelta;
        inputMousePitchDelta = 0.0F;
        inputMouseRollDelta = 0.0F;

        final Quaternionf delta = new Quaternionf()
            .rotateX(pitchDeltaRad)
            .rotateZ(rollDeltaRad);

        rotationQuaternion.mul(delta);
        rotationQuaternion.normalize();

        rotationQuaternion.getEulerAnglesYXZ(eulerAngles);

        double newYaw = Math.toDegrees(-eulerAngles.y);
        double newPitch = Math.toDegrees(eulerAngles.x);
        double newRoll = Math.toDegrees(eulerAngles.z);

        if (!Double.isFinite(newYaw)) {
            newYaw = bodyYaw;
        }
        if (!Double.isFinite(newPitch)) {
            newPitch = bodyPitch;
        }
        if (!Double.isFinite(newRoll)) {
            newRoll = bodyRoll;
        }

        bodyYaw = Mth.wrapDegrees(newYaw);
        bodyPitch = Mth.clamp(newPitch, -85.0D, 85.0D);
        bodyRoll = Mth.wrapDegrees(newRoll);

        syncQuaternionFromBodyAngles();

        this.setXRot((float) bodyPitch);
        this.setYRot((float) bodyYaw);
        this.setYHeadRot((float) bodyYaw);
    }

    private void applySideslipDamping(final double dt, final Vec3 forward) {
        if (forward == null) {
            return;
        }
        if (linearVelocity.lengthSqr() <= 1.0E-6D) {
            return;
        }

        final Vec3 worldUp = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = forward.cross(worldUp);
        if (right.lengthSqr() <= 1.0E-6D) {
            return;
        }
        right = right.normalize();

        final double slipSpeed = linearVelocity.dot(right);
        final double dampingPerSec = 4.0D;
        final double slipDelta = slipSpeed * dampingPerSec * dt;
        linearVelocity = linearVelocity.add(right.scale(-slipDelta));
    }

    private void syncQuaternionFromBodyAngles() {
        final float yawRad = (float) Math.toRadians(-bodyYaw);
        final float pitchRad = (float) Math.toRadians(bodyPitch);
        final float rollRad = (float) Math.toRadians(bodyRoll);
        rotationQuaternion.identity().rotateYXZ(yawRad, pitchRad, rollRad);
    }

    private Vec3 directionFromQuaternion(final float x, final float y, final float z) {
        axisVector.set(x, y, z);
        rotationQuaternion.transform(axisVector);
        return new Vec3(axisVector.x, axisVector.y, axisVector.z).normalize();
    }

    private void applyProjectileDamageEffects(final double dt) {
        if (projectileHitCount <= 0) {
            return;
        }
        final double horizontalSpeed = Math.sqrt(linearVelocity.x * linearVelocity.x + linearVelocity.z * linearVelocity.z);
        if (crippledHorizontalTargetSpeed < 0.0D) {
            crippledHorizontalTargetSpeed = horizontalSpeed;
        }
        final double slowdown = PROJECTILE_DAMAGE_DECEL_PER_SEC * dt;
        crippledHorizontalTargetSpeed = Math.max(0.0D, crippledHorizontalTargetSpeed - slowdown);
        if (horizontalSpeed > crippledHorizontalTargetSpeed) {
            if (horizontalSpeed <= 1.0E-4D || crippledHorizontalTargetSpeed <= 0.0D) {
                linearVelocity = new Vec3(0.0D, linearVelocity.y, 0.0D);
            } else {
                final Vec3 horizontal = new Vec3(linearVelocity.x, 0.0D, linearVelocity.z).normalize();
                final Vec3 clamped = horizontal.scale(crippledHorizontalTargetSpeed);
                linearVelocity = new Vec3(clamped.x, linearVelocity.y, clamped.z);
            }
        }
        spawnDamageSmokeParticles();
    }

    private void spawnDamageSmokeParticles() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        damageSmokeAccumulator += DAMAGE_SMOKE_PARTICLES_PER_TICK;
        final int spawnCount = (int) damageSmokeAccumulator;
        damageSmokeAccumulator -= spawnCount;
        if (spawnCount <= 0) {
            return;
        }
        final RandomSource random = this.random;
        for (int i = 0; i < spawnCount; i++) {
            final boolean large = random.nextFloat() < 0.65F;
            final ParticleOptions particle = large ? ParticleTypes.CAMPFIRE_SIGNAL_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE;
            final double offsetX = (random.nextDouble() - 0.5D) * DAMAGE_SMOKE_SPREAD;
            final double offsetZ = (random.nextDouble() - 0.5D) * DAMAGE_SMOKE_SPREAD;
            final double height = 0.6D + random.nextDouble() * 0.6D;
            final double px = getX() + offsetX;
            final double py = getY() + height;
            final double pz = getZ() + offsetZ;
            final double driftX = (random.nextDouble() - 0.5D) * 0.02D;
            final double driftY = 0.02D + random.nextDouble() * 0.05D;
            final double driftZ = (random.nextDouble() - 0.5D) * 0.02D;
            serverLevel.sendParticles(particle, px, py, pz, 1, driftX, driftY, driftZ, 0.0D);
        }
    }

    private static double sampleAirDensity(final double altitude) {
        return RHO_SEA_LEVEL * Math.exp(-Math.max(0.0D, altitude) / ATMOSPHERE_SCALE_HEIGHT);
    }

    private static double resolveLiftCoefficient(final double aoaRad) {
        return Mth.clamp(CL_ZERO + CL_ALPHA * aoaRad, -CL_MAX, CL_MAX);
    }

    private static double applyStall(final double cl, final double aoaRad) {
        final double absAoa = Math.abs(aoaRad);
        if (absAoa <= STALL_ANGLE) {
            return cl;
        }
        final double excess = Math.min(absAoa - STALL_ANGLE, STALL_ANGLE);
        final double stallFactor = Math.max(0.0D, 1.0D - (excess / STALL_ANGLE));
        return cl * stallFactor * stallFactor;
    }

    private double computeEffectiveThrust(final double engineLevel) {
        final double normalizedLevel = Mth.clamp(engineLevel, 0.0D, 1.0D);
        final double throttleResponse = Math.pow(normalizedLevel, THRUST_CURVE_EXPONENT);
        return ENGINE_IDLE_THRUST + throttleResponse * (MAX_THRUST_FORCE - ENGINE_IDLE_THRUST);
    }

    private Vec3 forwardGroundVector() {
        final float yawRad = this.getYRot() * ((float) Math.PI / 180F);
        final double x = -Mth.sin(yawRad);
        final double z = Mth.cos(yawRad);
        return new Vec3(x, 0.0D, z).normalize();
    }

    /**
     * Whether the terrain this tick's movement would cross has finished loading.
     *
     * <p>Vanilla's swept collision is exact at any speed, but only against blocks it can see:
     * {@code getBlockCollisions} skips columns whose chunk is not yet at full status, and treats them as
     * empty air. A Shahed cruising into freshly generated land outruns its own chunk ticket — 3.6 blocks
     * a tick against a nine-by-nine region that still has to be generated — so it flies into a chunk that
     * has no collision shapes yet, and by the time the chunk arrives the airframe is already inside a
     * wall. That is the village-church case: through the blocks, then wedged inside the building.
     *
     * <p>Holding position for the tick or two the chunk needs is the cheap fix. The ticket is guaranteed
     * to be in place while this matters — {@code shouldKeepChunksLoaded} is true for anything armed — so
     * the stall always ends, and it is invisible next to a drone stuck in a roof.
     *
     * <p>The test covers the whole swept envelope rather than a few sample points on the centre line. Three
     * samples at the destination left a gap the size of the airframe: the hull spans up to four blocks and
     * this tick's travel adds three and a half more, so a wing can cross a column no sample ever touched —
     * and one unloaded column is all it takes, because that is where the wall it flew through was.
     */
    private boolean isFlightPathTerrainReady(final Vec3 velPerTick) {
        if (!(level() instanceof ServerLevel)) {
            return true;
        }
        final AABB swept = getBoundingBox().expandTowards(velPerTick).inflate(1.0D, 0.0D, 1.0D);
        return level().hasChunksAt(
            Mth.floor(swept.minX),
            Mth.floor(swept.minZ),
            Mth.floor(swept.maxX),
            Mth.floor(swept.maxZ)
        );
    }

    private void resolveCollisionVelocity() {
        if (this.verticalCollision && linearVelocity.y < 0.0D) {
            linearVelocity = new Vec3(linearVelocity.x * GROUND_FRICTION, 0.0D, linearVelocity.z * GROUND_FRICTION);
        }
        if (this.horizontalCollision) {
            linearVelocity = new Vec3(linearVelocity.x * GROUND_FRICTION, linearVelocity.y, linearVelocity.z * GROUND_FRICTION);
        }
    }

    private void ensureFlightAltitude() {
        if (level() == null) {
            return;
        }
        final int x = Mth.floor(getX());
        final int z = Mth.floor(getZ());
        final int terrainY = level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        final double desiredY = terrainY + 10.0D;
        if (this.getY() >= desiredY) {
            return;
        }
        this.setPos(getX(), desiredY, getZ());
        this.setDeltaMovement(Vec3.ZERO);
        this.linearVelocity = Vec3.ZERO;
        this.bodyPitch = 0.0D;
        this.bodyPitchO = 0.0D;
        this.bodyRoll = 0.0D;
        this.bodyRollO = 0.0D;
        this.setXRot((float) bodyPitch);
        this.launchBaselineY = desiredY;
        updateBoundingBox();
    }

    @Override
    protected void readAdditionalSaveData(final CompoundTag tag) {
        setThrust(tag.getFloat(TAG_THRUST));
        if (tag.contains(TAG_LINEAR_VELOCITY, Tag.TAG_LIST)) {
            final ListTag list = tag.getList(TAG_LINEAR_VELOCITY, Tag.TAG_DOUBLE);
            this.linearVelocity = new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
        } else if (tag.contains(TAG_MOTION, Tag.TAG_LIST)) {
            final ListTag legacy = tag.getList(TAG_MOTION, Tag.TAG_DOUBLE);
            this.linearVelocity = new Vec3(legacy.getDouble(0) * 20.0D, legacy.getDouble(1) * 20.0D, legacy.getDouble(2) * 20.0D);
        } else {
            this.linearVelocity = Vec3.ZERO;
        }
        if (tag.hasUUID(TAG_OWNER)) {
            this.ownerUUID = tag.getUUID(TAG_OWNER);
        }
        this.ownerViewDistance = Math.max(2, tag.getInt(TAG_OWNER_VIEW));
        this.armed = tag.getBoolean(TAG_ARMED);
        this.launchBaselineY = tag.contains(TAG_LAUNCH_Y) ? tag.getDouble(TAG_LAUNCH_Y) : this.getY();
        this.fuelMass = tag.contains(TAG_FUEL) ? tag.getDouble(TAG_FUEL) : 0.0D;
        if (tag.contains(TAG_SERVICE_BAY, Tag.TAG_LIST)) {
            getServiceBay().load(tag.getList(TAG_SERVICE_BAY, Tag.TAG_COMPOUND), registryAccess());
        }
        this.speedScale = tag.contains(TAG_SPEED_SCALE, Tag.TAG_DOUBLE) ? tag.getDouble(TAG_SPEED_SCALE) : 1.0D;
        this.bodyYaw = tag.contains(TAG_BODY_YAW) ? tag.getDouble(TAG_BODY_YAW) : this.getYRot();
        this.bodyPitch = tag.contains(TAG_BODY_PITCH) ? tag.getDouble(TAG_BODY_PITCH) : this.getXRot();
        this.bodyRoll = tag.contains(TAG_BODY_ROLL) ? tag.getDouble(TAG_BODY_ROLL) : 0.0D;
        this.entityData.set(DATA_SERVER_YAW, tag.contains(TAG_SERVER_YAW) ? tag.getFloat(TAG_SERVER_YAW) : this.getYRot());
        this.entityData.set(DATA_SERVER_PITCH, tag.contains(TAG_SERVER_PITCH) ? tag.getFloat(TAG_SERVER_PITCH) : this.getXRot());
        this.bodyPitchO = this.bodyPitch;
        this.bodyRollO = this.bodyRoll;
        this.remoteInitialized = tag.getBoolean(TAG_REMOTE_INIT);
        if (tag.contains(TAG_COLOR)) {
            setColor(ShahedColor.byId(tag.getInt(TAG_COLOR)));
        }
        final boolean onLauncher = tag.getBoolean(TAG_ON_LAUNCHER);
        entityData.set(DATA_ON_LAUNCHER, onLauncher);
        if (onLauncher && tag.hasUUID(TAG_LAUNCHER_UUID)) {
            mountedLauncherUuid = tag.getUUID(TAG_LAUNCHER_UUID);
            mountedLauncherId = -1;
        } else {
            mountedLauncherUuid = null;
            mountedLauncherId = -1;
        }
        this.projectileHitCount = tag.getInt(TAG_PROJECTILE_HITS);
        this.crippledHorizontalTargetSpeed = tag.contains(TAG_DAMAGE_TARGET_SPEED) ? tag.getDouble(TAG_DAMAGE_TARGET_SPEED) : -1.0D;
        if (this.projectileHitCount <= 0) {
            this.crippledHorizontalTargetSpeed = -1.0D;
        }
        this.damageSmokeAccumulator = 0.0D;
        
        if (tag.hasUUID(TAG_CONTROLLER)) {
            this.controllingPlayer = tag.getUUID(TAG_CONTROLLER);
            if (tag.contains(TAG_SESS_X)) {
                Vec3 origin = new Vec3(tag.getDouble(TAG_SESS_X), tag.getDouble(TAG_SESS_Y), tag.getDouble(TAG_SESS_Z));
                float yaw = tag.getFloat(TAG_SESS_YAW);
                float pitch = tag.getFloat(TAG_SESS_PITCH);
                ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString(TAG_SESS_DIM)));
                GameType gm = GameType.byId(tag.getInt(TAG_SESS_GM));
                this.controlSession = new ControlSession(dim, origin, yaw, pitch, gm);
            }
        }
        keepChunksLoadedWithoutPlayer = tag.getBoolean(TAG_KEEP_CHUNKS);

        updateBoundingBox();
    }

    @Override
    protected void addAdditionalSaveData(final CompoundTag tag) {
        tag.putFloat(TAG_THRUST, getThrust());
        final ListTag list = new ListTag();
        list.add(DoubleTag.valueOf(linearVelocity.x));
        list.add(DoubleTag.valueOf(linearVelocity.y));
        list.add(DoubleTag.valueOf(linearVelocity.z));
        tag.put(TAG_LINEAR_VELOCITY, list);
        if (ownerUUID != null) {
            tag.putUUID(TAG_OWNER, ownerUUID);
            tag.putInt(TAG_OWNER_VIEW, ownerViewDistance);
        }
        tag.putBoolean(TAG_ARMED, armed);
        tag.putDouble(TAG_LAUNCH_Y, launchBaselineY);
        tag.putDouble(TAG_FUEL, fuelMass);
        tag.put(TAG_SERVICE_BAY, getServiceBay().save(registryAccess()));
        tag.putDouble(TAG_SPEED_SCALE, speedScale);
        tag.putDouble(TAG_BODY_YAW, bodyYaw);
        tag.putDouble(TAG_BODY_PITCH, bodyPitch);
        tag.putDouble(TAG_BODY_ROLL, bodyRoll);
        tag.putFloat(TAG_SERVER_YAW, this.entityData.get(DATA_SERVER_YAW));
        tag.putFloat(TAG_SERVER_PITCH, this.entityData.get(DATA_SERVER_PITCH));
        tag.putBoolean(TAG_REMOTE_INIT, remoteInitialized);
        tag.putInt(TAG_COLOR, getColor().getId());
        if (isOnLauncher() && mountedLauncherUuid != null) {
            tag.putBoolean(TAG_ON_LAUNCHER, true);
            tag.putUUID(TAG_LAUNCHER_UUID, mountedLauncherUuid);
        }
        if (projectileHitCount > 0) {
            tag.putInt(TAG_PROJECTILE_HITS, projectileHitCount);
            tag.putDouble(TAG_DAMAGE_TARGET_SPEED, Math.max(0.0D, crippledHorizontalTargetSpeed));
        }
        
        if (controllingPlayer != null) {
            tag.putUUID(TAG_CONTROLLER, controllingPlayer);
        }
        if (controlSession != null) {
            tag.putDouble(TAG_SESS_X, controlSession.originPos.x);
            tag.putDouble(TAG_SESS_Y, controlSession.originPos.y);
            tag.putDouble(TAG_SESS_Z, controlSession.originPos.z);
            tag.putFloat(TAG_SESS_YAW, controlSession.originYaw);
            tag.putFloat(TAG_SESS_PITCH, controlSession.originPitch);
            tag.putString(TAG_SESS_DIM, controlSession.originDimension.location().toString());
            tag.putInt(TAG_SESS_GM, controlSession.originalGameType.getId());
        }
        tag.putBoolean(TAG_KEEP_CHUNKS, keepChunksLoadedWithoutPlayer);
    }

    @Override
    public InteractionResult interact(final Player player, final InteractionHand hand) {
        final ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.getItem() instanceof ScrewdriverItem) {
            if (level().isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (armed) {
                player.displayClientMessage(Component.translatable("message.fullfud.shahed.armed"), true);
                return InteractionResult.FAIL;
            }
            if (!(player instanceof ServerPlayer serverPlayer)
                || !DroneServiceMenu.open(serverPlayer, this, getServiceBay(), false)) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.CONSUME;
        }
        if (heldItem.getItem() instanceof MonitorItem) {
            if (!level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                if (!assignOwner(serverPlayer)) {
                    player.displayClientMessage(Component.translatable("message.fullfud.monitor.in_use"), true);
                    return InteractionResult.FAIL;
                }
                MonitorItem.setLinkedDrone(heldItem, this.getUUID());
                FullfudNetwork.sendToPlayer(serverPlayer, new ShahedLinkPacket(this.getUUID(), true));
                player.displayClientMessage(Component.translatable("message.fullfud.monitor.linked"), true);
            }
            return level().isClientSide ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }
        if (heldItem.isEmpty() && !level().isClientSide) {
            if (armed) {
                player.displayClientMessage(Component.translatable("message.fullfud.shahed.armed"), true);
                return InteractionResult.FAIL;
            }
            final ItemStack droneStack = createItemStack();
            if (!player.addItem(droneStack)) {
                EntityDrops.spawnAtLocation(this, droneStack);
            }
            player.displayClientMessage(Component.translatable("message.fullfud.shahed.picked_up"), true);
            discard();
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    public void addViewer(final ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (ownerUUID != null && !ownerUUID.equals(player.getUUID())) {
            return;
        }
        viewerDistances.put(player.getUUID(), resolveViewDistance(player));
        sendStatusTo(player);
        recalcDesiredChunkRadius();
        ensureChunkTicket();
    }

    public void removeViewer(final ServerPlayer player) {
        viewerDistances.remove(player.getUUID());
        recalcDesiredChunkRadius();
        ensureChunkTicket();
    }

    public void applyControl(final ShahedControlPacket packet, final ServerPlayer sender) {
        if (sender == null || controllingPlayer == null || !controllingPlayer.equals(sender.getUUID())) {
            return;
        }
        if (!isRemoteStateValidFor(sender)) {
            return;
        }
        if (!(sender.containerMenu instanceof ShahedMonitorMenu menu)
            || menu.getDroneId() == null
            || !menu.getDroneId().equals(this.getUUID())) {
            return;
        }

        if (packet.thrustDelta() == Float.NEGATIVE_INFINITY) {
            inputMousePitchDelta = 0.0F;
            inputMouseRollDelta = 0.0F;
            releaseCameraFor(sender);
            return;
        }
        if (!Float.isFinite(packet.forward())
            || !Float.isFinite(packet.strafe())
            || !Float.isFinite(packet.vertical())
            || !Float.isFinite(packet.thrustDelta())
            || !Float.isFinite(packet.mousePitchDelta())
            || !Float.isFinite(packet.mouseRollDelta())) {
            return;
        }
        if (!canReceiveControl()) {
            return;
        }
        this.controlForward = Mth.clamp(packet.forward(), -1.0F, 1.0F);
        this.controlStrafe = Mth.clamp(packet.strafe(), -1.0F, 1.0F);
        this.controlVertical = Mth.clamp(packet.vertical(), -1.0F, 1.0F);
        this.inputMousePitchDelta = Mth.clamp(packet.mousePitchDelta(), -0.5F, 0.5F);
        this.inputMouseRollDelta = Mth.clamp(packet.mouseRollDelta(), -0.5F, 0.5F);
        this.controlTimeout = CONTROL_TIMEOUT_TICKS;
        final float thrustDelta = Mth.clamp(packet.thrustDelta(), -0.05F, 0.05F);
        final float newThrust = Mth.clamp(getThrust() + thrustDelta, 0.0F, 1.0F);
        setThrust(newThrust);
    }

    private boolean isRemoteStateValidFor(final ServerPlayer sender) {
        if (sender == null) {
            return false;
        }
        final CompoundTag root = PersistentData.of(sender);
        if (!root.contains(PLAYER_REMOTE_TAG, Tag.TAG_COMPOUND)) {
            return tryRestoreRemoteTag(sender);
        }
        final CompoundTag tag = root.getCompound(PLAYER_REMOTE_TAG);
        if (!tag.hasUUID(PLAYER_TAG_DRONE)) {
            return tryRestoreRemoteTag(sender);
        }
        if (!tag.getUUID(PLAYER_TAG_DRONE).equals(this.getUUID())) {
            return tryRestoreRemoteTag(sender);
        }
        return true;
    }

    private boolean tryRestoreRemoteTag(final ServerPlayer sender) {
        if (sender == null || controllingPlayer == null || !controllingPlayer.equals(sender.getUUID())) {
            return false;
        }
        if (controlSession == null) {
            return false;
        }
        writeRemoteTag(sender);
        return true;
    }

    private boolean canReceiveControl() {
        return !jammerSuppressControls;
    }

    private void updateJammerState() {
        jammerOverride = 0.0F;
        jammerSuppressControls = false;
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        final double maxRadius = JAMMER_MAX_RADIUS;
        final AABB searchBox = new AABB(
            getX() - maxRadius, getY() - 5.0D, getZ() - maxRadius,
            getX() + maxRadius, getY() + 5.0D, getZ() + maxRadius
        );
        for (final RebEmitterEntity emitter : serverLevel.getEntitiesOfClass(RebEmitterEntity.class, searchBox)) {
            // Only a transmitting emitter counts; the default listening mode is a warning post, not a weapon.
            if (!emitter.isJamming()) {
                continue;
            }
            final double dx = emitter.getX() - getX();
            final double dz = emitter.getZ() - getZ();
            final double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            final float strength = computeJammerStrength(horizontalDist);
            if (strength > jammerOverride) {
                jammerOverride = strength;
            }
        }
        if (jammerOverride >= 1.0F) {
            jammerSuppressControls = true;
            setThrust(0.0F);
        }
    }

    private static float computeJammerStrength(final double horizontalDist) {
        if (horizontalDist >= JAMMER_MAX_RADIUS) {
            return 0.0F;
        }
        if (horizontalDist <= JAMMER_HARD_RADIUS) {
            return 1.0F;
        }
        final double normalized = (horizontalDist - JAMMER_HARD_RADIUS) / (JAMMER_MAX_RADIUS - JAMMER_HARD_RADIUS);
        return (float) (1.0D - 0.99D * normalized);
    }

    private void broadcastStatus() {
        if (viewerDistances.isEmpty() || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (final UUID viewerId : Set.copyOf(viewerDistances.keySet())) {
            final ServerPlayer viewer = serverLevel.getServer().getPlayerList().getPlayer(viewerId);
            if (viewer == null) {
                viewerDistances.remove(viewerId);
                continue;
            }
            sendStatusTo(viewer);
        }
    }

    private void broadcastGhostState() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        final double rangeSqr = GHOST_BROADCAST_RANGE_BLOCKS * GHOST_BROADCAST_RANGE_BLOCKS;
        final ShahedGhostUpdatePacket packet = new ShahedGhostUpdatePacket(
            this.getUUID(),
            this.getX(),
            this.getY(),
            this.getZ(),
            this.getDeltaMovement().x,
            this.getDeltaMovement().y,
            this.getDeltaMovement().z,
            this.getYRot(),
            this.getXRot(),
            (float) bodyRoll,
            getThrust(),
            getColor().getId(),
            isOnLauncher()
        );
        for (final ServerPlayer player : serverLevel.players()) {
            if (player == null || player.isRemoved()) {
                continue;
            }
            if (player.distanceToSqr(this) > rangeSqr) {
                continue;
            }
            FullfudNetwork.sendToPlayer(player, packet);
        }
    }

    private void sendStatusTo(final ServerPlayer viewer) {
        final double distance = computeSignalDistance(viewer);
        final float noise = Math.max(computeNoise(distance), jammerOverride);
        final boolean signalLost = distance > 10000.0D;
        final FlightTelemetry data = telemetry == null ? FlightTelemetry.ZERO : telemetry;
        final ShahedStatusPacket packet = new ShahedStatusPacket(
                this.getUUID(),
                this.getX(),
                this.getY(),
                this.getZ(),
                this.getYRot(),
                this.getXRot(),
                getThrust(),
                noise,
                signalLost,
                data.airSpeed(),
                data.groundSpeed(),
                data.verticalSpeed(),
                data.angleOfAttack(),
                data.slipAngle(),
                data.fuelKg(),
                data.airDensity()
        );
        FullfudNetwork.sendToPlayer(viewer, packet);
    }

    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "flight", state -> {
            if (shouldUseRunningAnimation()) {
                state.setAndContinue(RUN_ANIMATION);
            } else {
                state.setAndContinue(IDLE_ANIMATION);
            }
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    private boolean shouldUseRunningAnimation() {
        return getThrust() > 0.2F || linearVelocity.lengthSqr() > 4.0D;
    }

    private void updateBoundingBox() {
        final float width = 3.0F;
        final float height = 1.0F;
        final float halfWidth = width * 0.5F;
        final Vec3 center = position();
        final double yawRad = Math.toRadians(getYRot());
        final double cos = Math.cos(yawRad);
        final double sin = Math.sin(yawRad);
        final Vec3[] corners = new Vec3[] {
                new Vec3(-halfWidth, 0, -halfWidth),
                new Vec3(halfWidth, 0, -halfWidth),
                new Vec3(halfWidth, 0, halfWidth),
                new Vec3(-halfWidth, 0, halfWidth)
        };
        double minX = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (Vec3 corner : corners) {
            final double rotX = corner.x * cos - corner.z * sin;
            final double rotZ = corner.x * sin + corner.z * cos;
            minX = Math.min(minX, center.x + rotX);
            minZ = Math.min(minZ, center.z + rotZ);
            maxX = Math.max(maxX, center.x + rotX);
            maxZ = Math.max(maxZ, center.z + rotZ);
        }
        final double minY = getY();
        final double maxY = getY() + height;
        final AABB box = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        setBoundingBox(box);
    }

    private double computeSignalDistance(final ServerPlayer viewer) {
        if (controllingPlayer != null && controllingPlayer.equals(viewer.getUUID()) && controlSession != null) {
            if (controlSession.originPos == null) {
                return 0.0D;
            }
            if (!level().dimension().equals(controlSession.originDimension)) {
                return Double.POSITIVE_INFINITY;
            }
            return Math.sqrt(controlSession.originPos.distanceToSqr(this.position()));
        }
        return Math.sqrt(viewer.distanceToSqr(this));
    }

    private static float computeNoise(final double distance) {
        if (distance <= 0.0D) {
            return 0.0F;
        }
        return (float) Math.min(distance / 10000.0D, 0.5D);
    }

    private void setThrust(final float thrust) {
        this.entityData.set(DATA_THRUST, thrust);
    }

    public float getThrust() {
        return this.entityData.get(DATA_THRUST);
    }

    public float getAudioEngineMix() {
        float mix = Mth.clamp(getThrust(), 0.0F, 1.0F);
        mix = Math.max(mix, (float) Mth.clamp(engineOutput, 0.0D, 1.0D));
        if (remoteInitialized || armed) {
            mix = Math.max(mix, 0.18F);
        }
        if (isOnLauncher()) {
            mix = Math.max(mix, ENGINE_IDLE_AUDIO_MIX);
        }
        return Mth.clamp(mix, 0.0F, 1.0F);
    }

    @Override
    public EntityDimensions getDimensions(final Pose pose) {
        return SHAHEED_DIMENSIONS;
    }

    // Entity.hurt is final void since 1.21.2 and only forwards to hurtServer on a ServerLevel, so the
    // former client-side guard is implicit. Entity.hurtServer is abstract, meaning there is no super to
    // fall through to: 1.20.1's Entity.hurt did nothing but markHurt() and return false, so that is
    // spelled out here.
    @Override
    public boolean hurtServer(final ServerLevel level, final DamageSource source, final float amount) {
        if (source.getDirectEntity() instanceof Projectile) {
            handleProjectileImpact(source.getDirectEntity());
            return true;
        }
        if (isInvulnerableToBase(source)) {
            return false;
        }
        markHurt();
        return false;
    }

    private void handleProjectileImpact(@org.jetbrains.annotations.Nullable final Entity directEntity) {
        projectileHitCount++;
        if (projectileHitCount >= 2) {
            detonate(directEntity != null ? directEntity.position() : position());
            return;
        }
        final double horizontalSpeed = Math.sqrt(linearVelocity.x * linearVelocity.x + linearVelocity.z * linearVelocity.z);
        crippledHorizontalTargetSpeed = Math.max(horizontalSpeed, 0.0D);
        damageSmokeAccumulator = 0.0D;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    public ShahedColor getColor() {
        return ShahedColor.byId(entityData.get(DATA_COLOR));
    }

    public void setColor(final ShahedColor color) {
        entityData.set(DATA_COLOR, color.getId());
    }

    public void setSpeedScale(final double scale) {
        if (!Double.isFinite(scale) || scale <= 0.0D) {
            this.speedScale = 1.0D;
            return;
        }
        this.speedScale = Mth.clamp(scale, 0.1D, 2.0D);
    }

    public double getSpeedScale() {
        return speedScale;
    }

    public ItemStack createItemStack() {
        final ItemStack stack = new ItemStack(resolveDropItem());
        // The loadout travels with the airframe. Tank fuel is a mass rather than a stack, so it needs its
        // own component; the canisters, charge and anything else in the bay ride in CONTAINER.
        getServiceBay().writeToStack(stack);
        if (fuelMass > 0.0D) {
            stack.set(FullfudDataComponents.DRONE_FUEL_KG, fuelMass);
        }
        return stack;
    }

    /** The other half of {@link #createItemStack()}, called by the launcher when it loads a munition. */
    public void restoreLoadout(final ItemStack stack) {
        getServiceBay().readFromStack(stack);
        final Double fuel = stack.get(FullfudDataComponents.DRONE_FUEL_KG);
        if (fuel != null && Double.isFinite(fuel)) {
            this.fuelMass = Mth.clamp(fuel, 0.0D, FUEL_CAPACITY_KG);
        }
    }

    public DroneServiceBay getServiceBay() {
        if (this.serviceBay == null) {
            this.serviceBay = new DroneServiceBay(
                0,
                WarheadCharge.SHAHED_MAX.tier(),
                null,
                this::canServiceFrom
            );
        }
        return this.serviceBay;
    }

    /** The charge currently bolted in, or {@link WarheadCharge#NONE} for an anti-personnel impact. */
    public WarheadCharge getWarhead() {
        return getServiceBay().warhead();
    }

    public double getFuelMass() {
        return this.fuelMass;
    }

    private boolean canServiceFrom(final Player player) {
        return isAlive()
            && (ownerUUID == null || ownerUUID.equals(player.getUUID()))
            && player.distanceToSqr(this) <= SERVICE_REACH_SQR;
    }

    /**
     * Empties fuel canisters out of the power slot and into the tank, one per tick, while the airframe is
     * sitting still. Only whole canisters go in, so the tank never ends up with a fractional third and the
     * player can leave a spare in the slot without it evaporating.
     */
    private void installFuelFromBay() {
        final DroneServiceBay bay = getServiceBay();
        final ItemStack power = bay.powerStack();
        if (power.isEmpty() || !power.is(FullfudRegistries.SHAHED_FUEL_ITEM.get())) {
            return;
        }
        if (this.fuelMass + FUEL_PER_CANISTER_KG > FUEL_CAPACITY_KG + 1.0E-6D) {
            return;
        }
        power.shrink(1);
        bay.setChanged();
        this.fuelMass = Math.min(FUEL_CAPACITY_KG, this.fuelMass + FUEL_PER_CANISTER_KG);
    }

    private net.minecraft.world.item.Item resolveDropItem() {
        if (isSlowVariant()) {
            return getColor() == ShahedColor.BLACK
                ? FullfudRegistries.SHAHED_BLACK_ITEM_SLOW.get()
                : FullfudRegistries.SHAHED_ITEM_SLOW.get();
        }
        return getColor() == ShahedColor.BLACK
            ? FullfudRegistries.SHAHED_BLACK_ITEM.get()
            : FullfudRegistries.SHAHED_ITEM.get();
    }

    private boolean isSlowVariant() {
        return Math.abs(resolveSpeedScale() - SLOW_SPEED_SCALE) <= 1.0E-3D;
    }

    private double resolveSpeedScale() {
        return speedScale > 0.0D ? speedScale : 1.0D;
    }

    public void setLaunchVelocity(final Vec3 velocity) {
        this.linearVelocity = velocity;
        setDeltaMovement(linearVelocity.scale(TICK_SECONDS));
    }

    public boolean isOnLauncher() {
        return entityData.get(DATA_ON_LAUNCHER);
    }

    public UUID getLauncherUuid() {
        return mountedLauncherUuid;
    }

    public void mountLauncher(final ShahedLauncherEntity launcher) {
        mountedLauncherId = launcher.getId();
        mountedLauncherUuid = launcher.getUUID();
        entityData.set(DATA_ON_LAUNCHER, true);
        setNoGravity(true);
        linearVelocity = Vec3.ZERO;
        setDeltaMovement(Vec3.ZERO);
        updateLauncherPose(launcher);
    }

    public void launchFromLauncher(final ShahedLauncherEntity launcher) {
        final float yaw = launcher.getYRot();
        final Vec3 forward = Vec3.directionFromRotation(0.0F, yaw).normalize();
        final Vec3 base = launcher.position().add(0.0D, LAUNCHER_VERTICAL_OFFSET, 0.0D);
        final Vec3 spawn = base.add(forward.scale(LAUNCHER_FORWARD_OFFSET)).add(0.0D, LAUNCHER_UP_OFFSET, 0.0D);
        setPos(spawn.x, spawn.y, spawn.z);
        final double scale = resolveSpeedScale();
        setThrust(1.0F);
        this.engineOutput = 1.0D;
        this.remoteInitialized = true;
        releaseFromLauncher(new Vec3(forward.x * LAUNCHER_LAUNCH_SPEED * scale, 0.0D, forward.z * LAUNCHER_LAUNCH_SPEED * scale), yaw);
    }

    public void releaseFromLauncher(final Vec3 velocity, final float launcherYaw) {
        entityData.set(DATA_ON_LAUNCHER, false);
        setNoGravity(false);
        mountedLauncherId = -1;
        mountedLauncherUuid = null;
        setLaunchVelocity(velocity);
        final float yaw = launcherYaw;
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
        this.bodyYaw = yaw;
        this.bodyPitch = -10.0D;
        this.bodyRoll = 0.0D;
        this.bodyRollO = 0.0D;
        setXRot((float) this.bodyPitch);
    }

    private void handleLauncherAttachment() {
        setDeltaMovement(Vec3.ZERO);
        final ShahedLauncherEntity launcher = resolveLauncher();
        if (launcher != null) {
            updateLauncherPose(launcher);
            return;
        }
        if (!level().isClientSide) {
            final ItemStack stack = createItemStack();
            EntityDrops.spawnAtLocation(this, stack);
            discard();
        }
    }

    private ShahedLauncherEntity resolveLauncher() {
        if (mountedLauncherId > 0) {
            final Entity entity = level().getEntity(mountedLauncherId);
            if (entity instanceof ShahedLauncherEntity launcher) {
                return launcher;
            }
        }
        if (mountedLauncherUuid != null && level() instanceof ServerLevel serverLevel) {
            final Entity entity = serverLevel.getEntity(mountedLauncherUuid);
            if (entity instanceof ShahedLauncherEntity launcher) {
                mountedLauncherId = launcher.getId();
                return launcher;
            }
        }
        return null;
    }

    private void updateLauncherPose(final ShahedLauncherEntity launcher) {
        final Vec3 anchor = launcher.position().add(0.0D, LAUNCHER_VERTICAL_OFFSET, 0.0D);
        setPos(anchor.x, anchor.y, anchor.z);
        final float yaw = launcher.getYRot();
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
        this.bodyYaw = yaw;
        this.bodyPitch = 0.0D;
        this.bodyRoll = 0.0D;
        this.bodyRollO = 0.0D;
        setXRot(0.0F);
    }

    @Override
    public void remove(final RemovalReason reason) {
        if (!level().isClientSide()) {
            final ServerPlayer controller = getControllingPlayer();
            if (controller != null) {
                forceReturnCamera(controller);
                endRemoteControl(controller);
            } else if (controllingPlayer != null && level() instanceof ServerLevel serverLevel) {
                final ServerPlayer offlineController = serverLevel.getServer().getPlayerList().getPlayer(controllingPlayer);
                if (offlineController != null) {
                    forceReturnCamera(offlineController);
                    endRemoteControl(offlineController);
                } else {
                    endRemoteControl(null);
                }
            }
            if (level() instanceof ServerLevel serverLevel) {
                ShahedLinkData.get(serverLevel).unlink(getUUID());
            }
            viewerDistances.clear();
            releaseChunkTicket();
        }
        super.remove(reason);
    }

    /**
     * Was an {@code onAddedToWorld} override — a method Forge added to {@code Entity}. Vanilla has no
     * equivalent, so {@code ChunkLoadEvents} calls this from {@code ServerEntityEvents.ENTITY_LOAD},
     * which fires from the same place Forge's hook did. That event is server-only, hence no client half.
     */
    public void onAddedToServerLevel() {
        if (!level().isClientSide() && ownerUUID != null && level() instanceof ServerLevel serverLevel) {
            ShahedLinkData.get(serverLevel).link(getUUID(), ownerUUID);
            recalcDesiredChunkRadius();
            ensureChunkTicket();
        }
    }

    public boolean assignOwner(final ServerPlayer player) {
        if (player == null) {
            return false;
        }
        final UUID playerId = player.getUUID();
        if (this.ownerUUID != null && !this.ownerUUID.equals(playerId)) {
            return false;
        }
        this.ownerUUID = playerId;
        this.ownerViewDistance = resolveViewDistance(player);
        if (level() instanceof ServerLevel serverLevel) {
            ShahedLinkData.get(serverLevel).link(getUUID(), ownerUUID);
        }
        recalcDesiredChunkRadius();
        ensureChunkTicket();
        return true;
    }

    public Optional<UUID> getOwnerUUID() {
        return Optional.ofNullable(ownerUUID);
    }

    public boolean isKeepChunksLoadedWithoutPlayer() {
        return keepChunksLoadedWithoutPlayer;
    }

    public void setKeepChunksLoadedWithoutPlayer(final boolean keep) {
        this.keepChunksLoadedWithoutPlayer = keep;
        if (!keep) {
            releaseChunkTicket();
        }
    }

    private int resolveViewDistance(final ServerPlayer player) {
        return Math.max(2, player.serverLevel().getServer().getPlayerList().getViewDistance());
    }

    private void recalcDesiredChunkRadius() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        final int serverCap = Math.max(2, serverLevel.getServer().getPlayerList().getViewDistance());
        int desired = 0;
        for (final int distance : viewerDistances.values()) {
            desired = Math.max(desired, Mth.clamp(distance, 2, serverCap));
        }
        if (ownerUUID != null) {
            desired = Math.max(desired, Mth.clamp(ownerViewDistance, 2, serverCap));
        }
        if (desired == 0) {
            desired = serverCap;
        }
        this.desiredChunkRadius = desired;
    }

    private void ensureChunkTicket() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!shouldKeepChunksLoaded()) {
            releaseChunkTicket();
            return;
        }
        ChunkLoadManager.ensureChunksLoaded(serverLevel, getId(), chunkPosition(), SHAHED_CHUNK_RADIUS);
    }

    private void releaseChunkTicket() {
        if (level() instanceof ServerLevel serverLevel) {
            ChunkLoadManager.releaseChunks(serverLevel, getId());
        }
    }

    private ServerPlayer getControllingPlayer() {
        if (controllingPlayer == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getServer().getPlayerList().getPlayer(controllingPlayer);
    }

    private void updateControllerBinding() {
        final ServerPlayer player = getControllingPlayer();
        if (player == null) {
            return;
        }

        if (!(player.containerMenu instanceof com.fullfud.fullfud.common.menu.ShahedMonitorMenu menu)
            || menu.getDroneId() == null
            || !menu.getDroneId().equals(this.getUUID())) {
            if (menuGraceTicks <= 0) {
                endRemoteControl(player);
            }
            return;
        }

        
    }

    private void updateLaunchState() {
        if (!armed) {
            final double altitudeGain = this.getY() - launchBaselineY;
            final double verticalSpeed = Math.abs(linearVelocity.y);
            if (altitudeGain > 1.5D || verticalSpeed > 5.0D) {
                armed = true;
            }
        }
    }

    /**
     * Where this tick's flight ran into terrain, or {@code null} if it did not.
     *
     * <p>Sampling only the block the drone currently occupies is not enough at cruise: the airframe
     * covers up to {@code MAX_AIRSPEED} m/s, some 3.6 blocks per tick, so a one-block church wall fits
     * entirely between two samples — the drone passes through it and parks inside the building. Sweeping
     * the segment the drone actually flew closes that gap, and the hit location gives the blast the
     * wall's coordinates instead of the interior's.
     *
     * <p>Three rays rather than one because the hull is three blocks wide: a wingtip can reach a wall
     * the centre line misses. The nearest of the three wins, so the blast lands where the airframe first
     * touched.
     */
    @org.jetbrains.annotations.Nullable
    private Vec3 resolveBlockImpactOrigin() {
        if (!hasDangerousSpeed()) {
            return null;
        }
        // move() already stopped us: no need to look for what we hit, we are against it.
        if (this.horizontalCollision || this.verticalCollision) {
            return position();
        }

        final Vec3 to = position();
        final Vec3 from = lastFlightStart;
        if (from != null && from.distanceToSqr(to) > 1.0E-6D) {
            final double yawRad = Math.toRadians(getYRot());
            final Vec3 span = new Vec3(Math.cos(yawRad), 0.0D, Math.sin(yawRad));
            Vec3 nearest = null;
            double nearestDistSqr = Double.MAX_VALUE;
            for (final double offset : new double[] { 0.0D, -1.3D, 1.3D }) {
                final Vec3 lateral = span.scale(offset).add(0.0D, 0.5D, 0.0D);
                final BlockHitResult hit = level().clip(new ClipContext(
                    from.add(lateral), to.add(lateral), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                if (hit.getType() == HitResult.Type.MISS) {
                    continue;
                }
                final double distSqr = from.distanceToSqr(hit.getLocation());
                if (distSqr < nearestDistSqr) {
                    nearestDistSqr = distSqr;
                    nearest = hit.getLocation();
                }
            }
            if (nearest != null) {
                return nearest;
            }
        }

        final BlockState state = level().getBlockState(blockPosition());
        final BlockState below = level().getBlockState(blockPosition().below());
        if (!state.isAir() || !below.isAir()) {
            return to;
        }
        final int x = Mth.floor(getX());
        final int z = Mth.floor(getZ());
        final int terrainY = level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        return this.getY() <= terrainY + 0.15D ? to : null;
    }

    private boolean hasDangerousSpeed() {
        return linearVelocity.lengthSqr() > 1.0D;
    }

    /**
     * Failsafe for an airframe that has stopped flying against the world instead of exploding on it.
     *
     * <p>Every speed-based check answers no once a stuck drone has bled off its velocity against
     * {@code GROUND_FRICTION}, which is how a Shahed ends up hanging silently inside a roof forever. Three
     * conditions together end the flight regardless of what the telemetry says: the hull is touching
     * terrain, the airframe went nowhere this tick, and that is not the deliberate hold from
     * {@link #isFlightPathTerrainReady}. Held for {@link #WEDGED_DETONATE_TICKS} ticks, that is a wreck.
     *
     * <p>Two holes in the first version, both of which left drones hanging. It sampled only the block under
     * {@code position()} — but the hull is three blocks across (four and a quarter of axis-aligned envelope
     * when the heading is diagonal) and one block tall, so a drone caught on a church roof or between two
     * walls has its centre point in plain air more often than not, and the counter reset every tick. And it
     * asked for penetration, when the airframe {@code move} politely stopped flush against a wall at a speed
     * under the {@link #hasDangerousSpeed} bar is just as finished: contact is the test, not overlap.
     *
     * <p>The travel condition is what keeps that from being trigger-happy. A launch climbing out through a
     * canopy has leaf shapes inside the envelope for the whole climb, and a drone banking past a wall clips
     * it for a tick or two — both are still moving, so neither counts.
     */
    private boolean isWedgedInTerrain() {
        if (isOnLauncher() || noPhysics) {
            wedgedTicks = 0;
            return false;
        }
        if (terrainStalled) {
            wedgedTicks = 0;
            return false;
        }
        if (lastFlightStart != null && lastFlightStart.distanceToSqr(position()) > WEDGE_STALL_EPS_SQR) {
            wedgedTicks = 0;
            return false;
        }
        final AABB hull = getBoundingBox().inflate(WEDGE_CONTACT_MARGIN);
        for (final VoxelShape shape : level().getBlockCollisions(this, hull)) {
            if (!shape.isEmpty()) {
                return ++wedgedTicks >= WEDGED_DETONATE_TICKS;
            }
        }
        wedgedTicks = 0;
        return false;
    }

    private void detonate() {
        detonate(position());
    }

    private void detonate(final Vec3 impactOrigin) {
        if (level().isClientSide()) {
            return;
        }
        if (detonating || isRemoved()) {
            return;
        }
        detonating = true;
        setPos(impactOrigin.x, impactOrigin.y, impactOrigin.z);
        final Vec3 explosionDirection = resolveExplosionDirection();
        final ServerPlayer controller = getControllingPlayer();
        if (controller != null) {
            forceReturnCamera(controller);
            endRemoteControl(controller);
        } else {
            endRemoteControl(null);
        }
        spawnTntEffect(controller, getWarhead(), explosionDirection);
        grantStrikeAdvancement(controller);
        discard();
    }

    /**
     * A Shahed usually goes off with nobody watching through it, so the credit follows the owner rather than
     * the current viewer. Only a charged airframe counts — an empty one falling out of the sky is a crash.
     */
    private void grantStrikeAdvancement(@org.jetbrains.annotations.Nullable final ServerPlayer controller) {
        if (!getWarhead().isPresent()) {
            return;
        }
        if (controller != null) {
            FullfudAdvancements.grant(controller, FullfudAdvancements.SHAHED_STRIKE);
            return;
        }
        if (ownerUUID != null && level() instanceof ServerLevel serverLevel) {
            FullfudAdvancements.grant(serverLevel.getServer().getPlayerList().getPlayer(ownerUUID),
                FullfudAdvancements.SHAHED_STRIKE);
        }
    }

    private void spawnTntEffect(
        @org.jetbrains.annotations.Nullable final ServerPlayer controller,
        final WarheadCharge charge,
        @org.jetbrains.annotations.Nullable final Vec3 explosionDirection
    ) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        final PrimedTnt tnt = new PrimedTnt(serverLevel, getX(), getY(), getZ(), controller);
        tnt.setFuse(0);
        RemotePlayerProtection.markHazard(tnt, this);
        // A charge is what breaks blocks. Without one the airframe still hits hard enough to kill, which
        // the mod's own blast model below does, but the terrain is left standing.
        if (!charge.isPresent()) {
            DroneExplosionLimiter.markNoBlockDamage(tnt);
        }
        DroneExplosionLimiter.markNoEntityDamage(tnt);
        serverLevel.addFreshEntity(tnt);
        serverLevel.explode(
            tnt,
            getX(),
            getY(),
            getZ(),
            charge.isPresent() ? charge.power() : SHAHED_FIREBALL_POWER,
            charge.incendiary(),
            charge.isPresent()
                ? net.minecraft.world.level.Level.ExplosionInteraction.TNT
                : net.minecraft.world.level.Level.ExplosionInteraction.MOB
        );
        DroneExplosionEffects.afterShahedExplosion(
            serverLevel, tnt, controller, explosionDirection, charge.blastScale());
        if (charge.isPresent()) {
            BlastLightRefresh.schedule(serverLevel, position(), charge.power());
        }
        tnt.discard();
    }

    private Vec3 resolveExplosionDirection() {
        syncQuaternionFromBodyAngles();
        final Vec3 forward = directionFromQuaternion(0.0F, 0.0F, 1.0F);
        if (forward.lengthSqr() > 1.0E-6D) {
            return forward.normalize();
        }
        final Vec3 look = this.getLookAngle();
        if (look.lengthSqr() > 1.0E-6D) {
            return look.normalize();
        }
        if (linearVelocity.lengthSqr() > 1.0E-6D) {
            return linearVelocity.normalize();
        }
        return new Vec3(0.0D, 0.0D, 1.0D);
    }

    private Vec3 resolveEntityImpactOrigin(final Vec3 start, final Vec3 end, final Entity target) {
        if (target == null || start.distanceToSqr(end) < 1.0E-6D) {
            return end;
        }
        return target.getBoundingBox().inflate(0.05D).clip(start, end).orElse(end);
    }

    public void initializePlacement(final double yPosition) {
        this.launchBaselineY = yPosition;
        this.armed = false;
        this.linearVelocity = Vec3.ZERO;
        this.setDeltaMovement(Vec3.ZERO);
        this.controlForward = 0.0F;
        this.controlStrafe = 0.0F;
        this.controlVertical = 0.0F;
        this.inputMousePitchDelta = 0.0F;
        this.inputMouseRollDelta = 0.0F;
        this.mousePitchOffsetDeg = 0.0D;
        this.mouseRollOffsetDeg = 0.0D;
        this.rollRate = 0.0D;
        this.pitchRate = 0.0D;
        // Fuel is deliberately not touched here: it arrives with the item form through restoreLoadout, or
        // is poured in through the service bay, and resetting it would silently empty a loaded tank.
        this.telemetry = FlightTelemetry.ZERO;
        this.bodyYaw = this.getYRot();
        this.bodyPitch = 0.0D;
        this.bodyPitchO = 0.0D;
        this.bodyRoll = 0.0D;
        this.bodyRollO = 0.0D;
        this.setXRot((float) bodyPitch);
        this.engineOutput = 0.0D;
        this.remoteInitialized = false;
    }

    public boolean beginRemoteControl(final ServerPlayer player) {
        if (player == null) {
            return false;
        }
        if (ownerUUID != null && !ownerUUID.equals(player.getUUID())) {
            return false;
        }
        if (ownerUUID == null && !assignOwner(player)) {
            return false;
        }
        if (controllingPlayer != null && !controllingPlayer.equals(player.getUUID())) {
            return false;
        }
        if (!isWithinPlayerChunkRange(player)) {
            player.displayClientMessage(Component.translatable("message.fullfud.fpv.out_of_range"), true);
            return false;
        }
        if (isOnLauncher()) {
            final ShahedLauncherEntity launcher = resolveLauncher();
            if (launcher != null) {
                launchFromLauncher(launcher);
            } else {
                entityData.set(DATA_ON_LAUNCHER, false);
            }
        }
        if (controlSession == null) {
            controlSession = new ControlSession(player.level().dimension(), player.position(), player.getYRot(), player.getXRot(), player.gameMode.getGameModeForPlayer());
        }
        if (!remoteInitialized) {
            ensureFlightAltitude();
            final double scale = resolveSpeedScale();
            final double launchSpeed = Math.min(MAX_AIRSPEED * scale * 0.95D, INITIAL_LAUNCH_SPEED * scale);
            this.linearVelocity = forwardGroundVector().scale(launchSpeed);
            setThrust(1.0F);
            this.engineOutput = 1.0D;
            this.remoteInitialized = true;
            this.bodyPitch = -10.0D; 
            this.setXRot((float) bodyPitch);
        }
        controllingPlayer = player.getUUID();
        menuGraceTicks = 40;
        writeRemoteTag(player);
        RemoteControlFailsafe.restoreLegacyRemotePlayerState(player);
        RemotePlayerProtection.touch(player, this, REMOTE_PROTECTION_RADIUS);
        setViewPoint(player, this);
        syncRemoteController(player);
        syncViewCenter(player);
        return true;
    }

    private boolean isWithinPlayerChunkRange(final ServerPlayer player) {
        if (player == null || !(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (player.level() != level()) {
            return false;
        }
        final int viewDistance = Math.max(2, serverLevel.getServer().getPlayerList().getViewDistance());
        final ChunkPos playerChunk = player.chunkPosition();
        final ChunkPos droneChunk = this.chunkPosition();
        final int dx = Math.abs(playerChunk.x - droneChunk.x);
        final int dz = Math.abs(playerChunk.z - droneChunk.z);
        return Math.max(dx, dz) <= viewDistance;
    }

    public void endRemoteControl(final ServerPlayer player) {
        if (controlSession == null) {
            if (player != null) {
                RemoteControlFailsafe.restoreLegacyRemotePlayerState(player);
                RemotePlayerProtection.clear(player);
                clearRemoteTag(player);
            }
            return;
        }

        if (player != null && controllingPlayer != null && !controllingPlayer.equals(player.getUUID())) {
            return;
        }

        final ControlSession endedSession = controlSession;
        if (player != null) {
            restoreRemoteController(player, endedSession);
            clearRemoteTag(player);
        }
        
        controllingPlayer = null;
        controlSession = null;
    }

    private void writeRemoteTag(final ServerPlayer player) {
        if (player == null || controlSession == null) {
            return;
        }
        final CompoundTag tag = new CompoundTag();
        tag.putUUID(PLAYER_TAG_DRONE, this.getUUID());
        tag.putString(PLAYER_TAG_ORIGIN_DIM, controlSession.originDimension.location().toString());
        tag.putDouble(PLAYER_TAG_ORIGIN_X, controlSession.originPos.x);
        tag.putDouble(PLAYER_TAG_ORIGIN_Y, controlSession.originPos.y);
        tag.putDouble(PLAYER_TAG_ORIGIN_Z, controlSession.originPos.z);
        tag.putFloat(PLAYER_TAG_ORIGIN_YAW, controlSession.originYaw);
        tag.putFloat(PLAYER_TAG_ORIGIN_PITCH, controlSession.originPitch);
        if (controlSession.originalGameType != null) {
            tag.putInt(PLAYER_TAG_ORIGIN_GM, controlSession.originalGameType.getId());
        }
        PersistentData.of(player).put(PLAYER_REMOTE_TAG, tag);
    }

    private static void clearRemoteTag(final ServerPlayer player) {
        if (player == null) {
            return;
        }
        final CompoundTag root = PersistentData.of(player);
        root.remove(PLAYER_REMOTE_TAG);
    }

    public void forceReleaseControlFor(final UUID playerId) {
        if (playerId == null || controllingPlayer == null || !controllingPlayer.equals(playerId)) {
            return;
        }
        applyForcedReleaseFailsafe();
        controllingPlayer = null;
        controlSession = null;
    }

    private void applyForcedReleaseFailsafe() {
        controlForward = 0.0F;
        controlStrafe = 0.0F;
        controlVertical = 0.0F;
        inputMousePitchDelta = 0.0F;
        inputMouseRollDelta = 0.0F;
        mousePitchOffsetDeg = 0.0D;
        mouseRollOffsetDeg = 0.0D;
        controlTimeout = 0;
        menuGraceTicks = 0;
        rollRate = 0.0D;
        pitchRate = 0.0D;
        if (isOnLauncher() && !remoteInitialized && !armed) {
            setThrust(0.0F);
            engineOutput = 0.0D;
            linearVelocity = Vec3.ZERO;
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        setThrust(FORCE_RELEASE_FAILSAFE_THRUST);
        engineOutput = Math.min(engineOutput, FORCE_RELEASE_FAILSAFE_THRUST);
    }

    public static void forceReleaseFromPersistentData(final MinecraftServer server, final UUID playerId, final CompoundTag tag) {
        if (server == null || playerId == null || tag == null || !tag.hasUUID(PLAYER_TAG_DRONE)) {
            return;
        }
        final UUID droneId = tag.getUUID(PLAYER_TAG_DRONE);
        for (final ServerLevel level : server.getAllLevels()) {
            final Entity entity = level.getEntity(droneId);
            if (entity instanceof ShahedDroneEntity drone) {
                drone.forceReleaseControlFor(playerId);
                return;
            }
        }
    }

    public static void forceRestoreFromPersistentData(final ServerPlayer player, final CompoundTag tag) {
        if (player == null || player.getServer() == null || tag == null) {
            return;
        }

        forceReleaseFromPersistentData(player.getServer(), player.getUUID(), tag);

        restorePlayerFromRemoteTag(player, tag);
    }

    private void releaseCameraFor(final ServerPlayer player) {
        clearViewPoint(player);
    }

    private boolean isSignalLostFor(final ServerPlayer p) {
        return p == null || Math.sqrt(p.distanceToSqr(this)) > 10000.0D;
    }

    private void forceReturnCamera(final ServerPlayer player) {
        if (player == null) {
            return;
        }
        clearViewPoint(player);
    }

    private static void setViewPoint(final ServerPlayer player, final Entity entity) {
        if (player == null || entity == null || !(player instanceof LatticeServerPlayer lattice)) {
            return;
        }
        if (entity instanceof dev.lazurite.lattice.api.point.ViewPoint viewPoint) {
            lattice.setViewPoint(viewPoint);
        }
        lattice.setCameraWithoutViewPoint(entity);
    }

    private static void clearViewPoint(final ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (player instanceof LatticeServerPlayer lattice) {
            lattice.removeViewPoint();
            lattice.setCameraWithoutViewPoint(player);
            if (player instanceof dev.lazurite.lattice.api.point.ViewPoint viewPoint) {
                lattice.setViewPoint(viewPoint);
            }
            RemoteControlFailsafe.ensureLatticePlayerRegistered(player);
        } else {
            player.setCamera(player);
        }
    }

    private void syncViewCenter(final ServerPlayer player) {
        if (player == null || !(level() instanceof ServerLevel)) {
            return;
        }
        final ChunkPos chunkPos = this.chunkPosition();
        final boolean centerChanged = lastSentViewCenter == null || !lastSentViewCenter.equals(chunkPos);
        if (centerChanged) {
            player.connection.send(new ClientboundSetChunkCacheCenterPacket(chunkPos.x, chunkPos.z));
            lastSentViewCenter = chunkPos;
        }
        if (centerChanged || (tickCount % 20 == 0)) {
            RemoteControlFailsafe.forceChunkTracking(player);
        }
    }

    private void resetViewCenter(final ServerPlayer player) {
        if (player == null) {
            return;
        }
        final ChunkPos chunkPos = player.chunkPosition();
        player.connection.send(new ClientboundSetChunkCacheCenterPacket(chunkPos.x, chunkPos.z));
        lastSentViewCenter = null;
        RemoteControlFailsafe.resetViewpointChunksToPlayer(player);
        RemoteControlFailsafe.forceChunkTracking(player);
        RemoteControlFailsafe.forceChunkRefresh(player);
    }

    private boolean shouldKeepChunksLoaded() {
        return keepChunksLoadedWithoutPlayer || controllingPlayer != null || armed || !viewerDistances.isEmpty();
    }

    private void syncRemoteController(final ServerPlayer player) {
        if (player == null) {
            return;
        }
        RemotePlayerProtection.touch(player, this, REMOTE_PROTECTION_RADIUS);
        if (!(player instanceof LatticeServerPlayer lattice)) {
            return;
        }
        if (viewPointResyncCooldown > 0) {
            viewPointResyncCooldown--;
        }
        final dev.lazurite.lattice.api.point.ViewPoint current = lattice.getViewPoint();
        if (current != this && viewPointResyncCooldown <= 0) {
            setViewPoint(player, this);
            lastSentViewCenter = null;
            RemoteControlFailsafe.forceChunkRefresh(player);
            viewPointResyncCooldown = 20;
        }
    }

    private void restoreRemoteController(final ServerPlayer player, final ControlSession session) {
        if (player == null || session == null) {
            return;
        }
        clearViewPoint(player);
        RemoteControlFailsafe.restoreLegacyRemotePlayerState(player);
        RemotePlayerProtection.clear(player);
        final MinecraftServer server = player.getServer();
        final ServerLevel targetLevel = server != null ? server.getLevel(session.originDimension) : player.serverLevel();
        if (targetLevel != null) {
            final ChunkPos chunkPos = new ChunkPos(BlockPos.containing(session.originPos));
            ChunkLoadManager.warmTeleportDestination(targetLevel, chunkPos, player.getId());
            // 1.21.2 folded the relative-movement flags and the reset-camera behaviour into teleportTo's
            // signature. An empty Set is an absolute teleport, and the trailing true is the setCamera(this)
            // that the old 6-arg overload did unconditionally.
            player.teleportTo(targetLevel, session.originPos.x, session.originPos.y, session.originPos.z, Set.of(), session.originYaw, session.originPitch, true);
            player.fallDistance = 0.0F;
        }
        resetViewCenter(player);
    }

    private static void restorePlayerFromRemoteTag(final ServerPlayer player, final CompoundTag tag) {
        if (player == null || tag == null) {
            return;
        }

        clearViewPoint(player);
        RemoteControlFailsafe.restoreLegacyRemotePlayerState(player);
        RemotePlayerProtection.clear(player);
        if (player.getServer() != null && tag.contains(PLAYER_TAG_ORIGIN_DIM, Tag.TAG_STRING)) {
            final ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString(PLAYER_TAG_ORIGIN_DIM));
            final ServerLevel targetLevel = dimensionId != null ? player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId)) : null;
            if (targetLevel != null) {
                final double x = tag.getDouble(PLAYER_TAG_ORIGIN_X);
                final double y = tag.getDouble(PLAYER_TAG_ORIGIN_Y);
                final double z = tag.getDouble(PLAYER_TAG_ORIGIN_Z);
                final float yaw = tag.getFloat(PLAYER_TAG_ORIGIN_YAW);
                final float pitch = tag.getFloat(PLAYER_TAG_ORIGIN_PITCH);
                final ChunkPos ticketChunk = new ChunkPos(BlockPos.containing(x, y, z));
                ChunkLoadManager.warmTeleportDestination(targetLevel, ticketChunk, player.getId());
                player.teleportTo(targetLevel, x, y, z, Set.of(), yaw, pitch, true);
                player.fallDistance = 0.0F;
            }
        }
        final ChunkPos chunkPos = player.chunkPosition();
        player.connection.send(new ClientboundSetChunkCacheCenterPacket(chunkPos.x, chunkPos.z));
        RemoteControlFailsafe.resetViewpointChunksToPlayer(player);
        RemoteControlFailsafe.forceChunkTracking(player);
        RemoteControlFailsafe.forceChunkRefresh(player);
    }

    private record OrientationBasis(Vec3 forward, Vec3 up, Vec3 right) { }

    private record FlightTelemetry(float airSpeed, float groundSpeed, float verticalSpeed, float angleOfAttack, float slipAngle, float throttle, float fuelKg, float airDensity) {
        static final FlightTelemetry ZERO = new FlightTelemetry(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    }

    private static double approach(final double current, final double target, final double maxStep) {
        final double delta = Mth.clamp(target - current, -maxStep, maxStep);
        return current + delta;
    }

    private static final class ControlSession {
        final ResourceKey<Level> originDimension;
        final Vec3 originPos;
        final float originYaw;
        final float originPitch;
        final GameType originalGameType;

        private ControlSession(final ResourceKey<Level> originDimension, final Vec3 originPos, final float originYaw, final float originPitch, final GameType originalGameType) {
            this.originDimension = originDimension;
            this.originPos = originPos;
            this.originYaw = originYaw;
            this.originPitch = originPitch;
            this.originalGameType = originalGameType;
        }
    }

}
