package com.fullfud.fullfud.common.entity;

import com.fullfud.fullfud.common.item.RebBatteryItem;
import com.fullfud.fullfud.common.item.ScrewdriverItem;
import com.fullfud.fullfud.core.EntityDrops;
import com.fullfud.fullfud.core.FullfudAdvancements;
import com.fullfud.fullfud.core.FullfudRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;

public class RebEmitterEntity extends Entity implements GeoEntity {

    /**
     * What the emitter actually does with its power.
     *
     * <p>{@link #DETECT} is the default and the cheap one: the dish only listens, and the owner gets a
     * chat warning when something flies into range. {@link #JAM} adds the transmitter, which is what
     * actually degrades a drone's link — and burns the pack {@value #JAM_DRAIN_MULTIPLIER}× as fast.
     */
    public enum Mode {
        DETECT,
        JAM;

        public static Mode byId(final int id) {
            return id == 1 ? JAM : DETECT;
        }
    }

    private static final EntityDataAccessor<Boolean> DATA_HAS_BATTERY = SynchedEntityData.defineId(RebEmitterEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_CHARGE_TICKS = SynchedEntityData.defineId(RebEmitterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_STARTUP_DONE = SynchedEntityData.defineId(RebEmitterEntity.class, EntityDataSerializers.BOOLEAN);
    /** Synched because the monitor overlay and the renderer both want to show which mode is running. */
    private static final EntityDataAccessor<Integer> DATA_MODE = SynchedEntityData.defineId(RebEmitterEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation START_ANIMATION = RawAnimation.begin().then("animation.reb.start", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation IDLE_ANIMATION  = RawAnimation.begin().thenLoop("animation.reb.idle");

    private static final int STARTUP_DURATION_TICKS = 3 * 20;

    /** How long the transmitter may run on a full pack. */
    public static final int JAM_RUNTIME_TICKS = 10 * 60 * 20;
    /**
     * How much faster the pack empties with the transmitter on. Derived rather than written down so the
     * ten-minute jam runtime survives a change to the pack's capacity.
     */
    public static final int JAM_DRAIN_MULTIPLIER = RebBatteryItem.MAX_CHARGE_TICKS / JAM_RUNTIME_TICKS;
    /** Detection range, in blocks. The same figure the jamming falloff in the drones uses. */
    public static final double DETECTION_RADIUS = 300.0D;
    /** Scanning every tick over a 600-block box would be wasteful, and once a second is plenty. */
    private static final int DETECTION_INTERVAL_TICKS = 20;
    /** Silence between warnings, so a drone circling in range does not fill the chat. */
    private static final int DETECTION_MESSAGE_COOLDOWN_TICKS = 20 * 15;

    private static final String TAG_MODE = "Mode";
    private static final String TAG_OWNER = "Owner";

    private ItemStack battery = ItemStack.EMPTY;
    private int chargeTicks;
    private boolean fallingFromSupport;
    private boolean wasOnGround;
    private int energyTickCounter;
    private int startupTicks;
    private int detectionTickCounter;
    private int detectionMessageCooldown;
    @Nullable
    private UUID ownerUUID;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public RebEmitterEntity(final EntityType<? extends RebEmitterEntity> type, final Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        builder.define(DATA_HAS_BATTERY, false);
        builder.define(DATA_CHARGE_TICKS, 0);
        builder.define(DATA_STARTUP_DONE, false);
        builder.define(DATA_MODE, Mode.DETECT.ordinal());
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            final Vec3 motion = getDeltaMovement();
            setDeltaMovement(0.0D, motion.y, 0.0D);

            if (wasOnGround && !onGround()) {
                fallingFromSupport = true;
            }
            if (fallingFromSupport && onGround()) {
                dropContents();
                discard();
            }

            updateStartup();
            drainEnergy();
            updateDetection();
        }

        wasOnGround = onGround();
    }

    @Override
    protected void readAdditionalSaveData(final CompoundTag tag) {
        if (tag.contains("Battery")) {
            // 1.20.5 moved ItemStack (de)serialisation onto codecs that need the registries.
            // parseOptional yields EMPTY instead of throwing when the stored item no longer resolves.
            battery = ItemStack.parseOptional(registryAccess(), tag.getCompound("Battery"));
            chargeTicks = tag.getInt("ChargeTicks");
            entityData.set(DATA_HAS_BATTERY, !battery.isEmpty());
            entityData.set(DATA_CHARGE_TICKS, chargeTicks);
        }
        startupTicks = tag.getInt("StartupTicks");
        final boolean startupDone = tag.getBoolean("StartupDone");
        entityData.set(DATA_STARTUP_DONE, startupDone);
        if (startupDone) {
            startupTicks = STARTUP_DURATION_TICKS;
        }
        entityData.set(DATA_MODE, Mode.byId(tag.getInt(TAG_MODE)).ordinal());
        this.ownerUUID = tag.hasUUID(TAG_OWNER) ? tag.getUUID(TAG_OWNER) : null;
    }

    @Override
    protected void addAdditionalSaveData(final CompoundTag tag) {
        if (hasBattery()) {
            tag.put("Battery", battery.save(registryAccess()));
            tag.putInt("ChargeTicks", chargeTicks);
        }
        tag.putInt("StartupTicks", startupTicks);
        tag.putBoolean("StartupDone", entityData.get(DATA_STARTUP_DONE));
        tag.putInt(TAG_MODE, getMode().ordinal());
        if (this.ownerUUID != null) {
            tag.putUUID(TAG_OWNER, this.ownerUUID);
        }
    }

    // Entity.hurt is final void since 1.21.2 and forwards here only on a ServerLevel, so the former
    // isClientSide half of the guard is implicit.
    @Override
    public boolean hurtServer(final ServerLevel level, final DamageSource source, final float amount) {
        if (!isAlive()) return false;
        dropContents();
        discard();
        return true;
    }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean isPickable() { return true; }

    @Override
    public boolean isAttackable() { return true; }

    @Override
    public InteractionResult interact(final Player player, final InteractionHand hand) {
        final ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.getItem() instanceof ScrewdriverItem) {
            if (!level().isClientSide) {
                cycleMode(player);
            }
            return level().isClientSide ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }
        if (!hasBattery() && heldItem.getItem() == FullfudRegistries.REB_BATTERY_ITEM.get()) {
            if (!level().isClientSide) {
                insertBattery(heldItem, player);
            }
            return level().isClientSide ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }

        if (player.isCrouching() && hasBattery() && heldItem.isEmpty()) {
            if (!level().isClientSide) {
                final ItemStack extracted = removeBattery();
                if (!extracted.isEmpty() && !player.addItem(extracted)) {
                    EntityDrops.spawnAtLocation(this, extracted, 0.25F);
                }
            }
            return level().isClientSide ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    public boolean hasBattery() { return entityData.get(DATA_HAS_BATTERY); }
    public int getChargeTicks()  { return entityData.get(DATA_CHARGE_TICKS); }

    public Mode getMode() {
        return Mode.byId(entityData.get(DATA_MODE));
    }

    /**
     * Whether the transmitter is actually running. The drones ask this rather than {@code hasBattery},
     * so a listening-only emitter is invisible to their link budget.
     */
    public boolean isJamming() {
        return getMode() == Mode.JAM && hasBattery() && getChargeTicks() > 0 && hasFinishedStartup();
    }

    /** Whoever placed it. Only they get the detection warnings. */
    public void setOwner(@Nullable final UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }

    /**
     * The screwdriver toggle. Switching into {@link Mode#JAM} says out loud that the pack now empties
     * four times as fast, because nothing else on the emitter would tell the player that.
     */
    private void cycleMode(final Player player) {
        final Mode next = getMode() == Mode.DETECT ? Mode.JAM : Mode.DETECT;
        entityData.set(DATA_MODE, next.ordinal());
        // Restart the second counter so a mid-second switch does not charge jam rates for detect time.
        this.energyTickCounter = 0;
        player.displayClientMessage(Component.translatable(next == Mode.JAM
            ? "message.fullfud.reb.mode_jam"
            : "message.fullfud.reb.mode_detect"), true);
        if (next == Mode.JAM) {
            player.displayClientMessage(Component.translatable(
                "message.fullfud.reb.jam_drain_warning", JAM_DRAIN_MULTIPLIER)
                .withStyle(ChatFormatting.GOLD), false);
        }
    }

    private void insertBattery(final ItemStack stack, final Player player) {
        final ItemStack single = stack.copy();
        single.setCount(1);
        chargeTicks = RebBatteryItem.getChargeTicks(single);
        RebBatteryItem.setChargeTicks(single, chargeTicks);
        battery = single;
        syncBatteryState(true, chargeTicks);
        energyTickCounter = 0;

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    private ItemStack removeBattery() {
        if (!hasBattery()) return ItemStack.EMPTY;
        final ItemStack result = battery.copy();
        RebBatteryItem.setChargeTicks(result, chargeTicks);
        clearBattery();
        return result;
    }

    private void clearBattery() {
        battery = ItemStack.EMPTY;
        chargeTicks = 0;
        syncBatteryState(false, 0);
        energyTickCounter = 0;
    }

    private void dropContents() {
        if (level().isClientSide) return;
        EntityDrops.spawnAtLocation(this, new ItemStack(FullfudRegistries.REB_EMITTER_ITEM.get()));
        if (hasBattery()) {
            final ItemStack dropBattery = removeBattery();
            if (!dropBattery.isEmpty()) EntityDrops.spawnAtLocation(this, dropBattery);
        }
    }

    private void drainEnergy() {
        if (!hasBattery()) return;
        energyTickCounter++;
        if (energyTickCounter < 20) return;
        energyTickCounter = 0;
        // One second of wall clock costs one second of pack in DETECT and four in JAM: two in-game days
        // of listening, ten minutes of transmitting.
        final int cost = 20 * (getMode() == Mode.JAM ? JAM_DRAIN_MULTIPLIER : 1);
        setChargeTicks(Math.max(0, chargeTicks - cost));
        if (chargeTicks <= 0) {
            final ItemStack discharged = removeBattery();
            if (!discharged.isEmpty()) EntityDrops.spawnAtLocation(this, discharged);
        }
    }

    /**
     * The listening half, which runs in both modes: sweep for drones once a second and tell the owner.
     *
     * <p>Only the owner is told, and only in chat, because that is the whole point of the default mode —
     * an early-warning post rather than an area denial weapon. The message goes out at most once every
     * {@value #DETECTION_MESSAGE_COOLDOWN_TICKS} ticks even if the drone stays in range.
     */
    private void updateDetection() {
        if (detectionMessageCooldown > 0) {
            detectionMessageCooldown--;
        }
        if (!hasBattery() || getChargeTicks() <= 0 || !hasFinishedStartup()) {
            return;
        }
        if (++detectionTickCounter < DETECTION_INTERVAL_TICKS) {
            return;
        }
        detectionTickCounter = 0;
        if (detectionMessageCooldown > 0 || this.ownerUUID == null) {
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        final Player owner = serverLevel.getPlayerByUUID(this.ownerUUID);
        if (owner == null) {
            return;
        }
        final AABB box = getBoundingBox().inflate(DETECTION_RADIUS);
        final double radiusSqr = DETECTION_RADIUS * DETECTION_RADIUS;
        final List<Entity> contacts = serverLevel.getEntities(this, box,
            candidate -> isUav(candidate) && candidate.distanceToSqr(this) <= radiusSqr);
        if (contacts.isEmpty()) {
            return;
        }
        detectionMessageCooldown = DETECTION_MESSAGE_COOLDOWN_TICKS;
        owner.displayClientMessage(Component.translatable("message.fullfud.reb.uav_detected")
            .withStyle(ChatFormatting.RED), false);
        if (owner instanceof ServerPlayer serverOwner) {
            FullfudAdvancements.grant(serverOwner, FullfudAdvancements.REB_WARNING);
        }
    }

    /** Everything the mod flies. Not an interface on the entities themselves, because they share no base. */
    private static boolean isUav(final Entity entity) {
        return entity instanceof ShahedDroneEntity
            || entity instanceof FpvDroneEntity
            || entity instanceof Fp5FlamingoEntity;
    }

    private void setChargeTicks(final int value) {
        chargeTicks = value;
        entityData.set(DATA_CHARGE_TICKS, chargeTicks);
    }

    private void syncBatteryState(final boolean hasBattery, final int charge) {
        entityData.set(DATA_HAS_BATTERY, hasBattery);
        entityData.set(DATA_CHARGE_TICKS, charge);
    }

    private void updateStartup() {
        if (entityData.get(DATA_STARTUP_DONE)) return;
        startupTicks++;
        if (startupTicks >= STARTUP_DURATION_TICKS) {
            startupTicks = STARTUP_DURATION_TICKS;
            entityData.set(DATA_STARTUP_DONE, true);
        }
    }

    public boolean hasFinishedStartup() { return entityData.get(DATA_STARTUP_DONE); }

    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "reb", 0, state -> {
            if (!hasFinishedStartup()) state.setAndContinue(START_ANIMATION);
            else state.setAndContinue(IDLE_ANIMATION);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
