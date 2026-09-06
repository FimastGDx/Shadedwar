package com.fullfud.fullfud.client.warning;

import com.fullfud.fullfud.common.entity.FpvDroneEntity;
import com.fullfud.fullfud.common.entity.ShahedDroneEntity;
import com.fullfud.fullfud.core.config.FullfudClientConfig;
import com.fullfud.fullfud.core.network.packet.ShahedStatusPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Decides which alerts are up, for both drone screens.
 *
 * <p>Everything here is read on the client from state it already has: the FPV goggles hold the drone entity
 * itself, the Shahed monitor holds the entity plus its telemetry packet. Nothing is sent from the server for
 * this, so an alert can never lag the video feed it is drawn on.
 */
public final class DroneWarningSources {
    /** Shortest obstacle-check range, so the alert also fires on something the drone is only creeping up on. */
    private static final double COLLISION_MIN_DISTANCE = 2.0D;
    private static final double COLLISION_MAX_DISTANCE = 48.0D;
    /** Below this ground speed the obstacle check follows the nose instead of the flight path. */
    private static final double COLLISION_MIN_SPEED = 0.5D;

    private static final float FPV_OVERBANK_DEGREES = 70.0F;
    /**
     * A deliberate dive is a normal manoeuvre now that the mouse aims the nose — at the aim limit and full speed
     * the drone descends around 15 m/s on purpose — so this has to mean "falling", not "descending fast".
     */
    private static final double FPV_SINK_RATE_MS = -25.0D;

    /** The airframe stalls at 17 degrees of incidence, so the alert has to come up before that. */
    private static final float SHAHED_STALL_AOA_DEGREES = 15.0F;
    private static final float SHAHED_OVERBANK_DEGREES = 55.0F;
    private static final float SHAHED_SINK_RATE_MS = -25.0F;
    private static final double SHAHED_TERRAIN_CLEARANCE = 14.0D;

    private static UUID trackedId;
    private static Vec3 trackedPos = Vec3.ZERO;
    private static Vec3 trackedVelocity = Vec3.ZERO;
    private static long trackedNanos;

    private DroneWarningSources() {
    }

    /** Alerts for the FPV goggles. {@code partialTick} is only used for the airframe attitude. */
    public static List<DroneWarning> collectFpv(
        final Minecraft minecraft,
        final FpvDroneEntity drone,
        final float partialTick
    ) {
        if (!FullfudClientConfig.CLIENT.warningsEnabled.get() || minecraft == null || drone == null) {
            return List.of();
        }
        final List<DroneWarning> out = new ArrayList<>(4);
        final Vec3 velocity = trackVelocity(drone);

        if (drone.getBatteryPercent() <= FullfudClientConfig.CLIENT.warningsLowBatteryPercent.get()) {
            out.add(DroneWarning.caution("LOW_BATTERY"));
        }
        if (Math.round(drone.getSignalQuality() * 100.0F) <= FullfudClientConfig.CLIENT.warningsLowSignalPercent.get()) {
            out.add(DroneWarning.caution("LOW_SIGNAL"));
        }

        if (collisionAhead(minecraft, drone, velocity)) {
            out.add(DroneWarning.warn("BLOCK_COLLISION"));
        }
        if (Math.abs(drone.getVisualRoll(partialTick)) > FPV_OVERBANK_DEGREES) {
            out.add(DroneWarning.warn("OVERBANK"));
        }
        if (velocity.y <= FPV_SINK_RATE_MS) {
            out.add(DroneWarning.warn("SINK_RATE"));
        }
        return out;
    }

    /**
     * Alerts for the Shahed monitor.
     *
     * @param signalQuality link quality from 0 to 1, as the monitor itself worked it out — distance, the drone's
     *                      own noise figure and any jammer in range all feed into the picture on screen, so the
     *                      alert has to use the same number rather than re-deriving one
     */
    public static List<DroneWarning> collectShahed(
        final Minecraft minecraft,
        final ShahedDroneEntity drone,
        final ShahedStatusPacket status,
        final float signalQuality,
        final float partialTick
    ) {
        if (!FullfudClientConfig.CLIENT.warningsEnabled.get() || minecraft == null || status == null) {
            return List.of();
        }
        final List<DroneWarning> out = new ArrayList<>(4);

        final float fuelPercent = (float) (status.fuelKg() / ShahedDroneEntity.FUEL_CAPACITY_KG * 100.0D);
        if (fuelPercent <= FullfudClientConfig.CLIENT.warningsLowFuelPercent.get()) {
            out.add(DroneWarning.caution("LOW_FUEL"));
        }
        if (Math.round(signalQuality * 100.0F) <= FullfudClientConfig.CLIENT.warningsLowSignalPercent.get()) {
            out.add(DroneWarning.caution("LOW_SIGNAL"));
        }

        if (Math.abs(status.angleOfAttack()) >= SHAHED_STALL_AOA_DEGREES) {
            out.add(DroneWarning.warn("STALL"));
        }
        if (drone != null && Math.abs(drone.getVisualRoll(partialTick)) > SHAHED_OVERBANK_DEGREES) {
            out.add(DroneWarning.warn("OVERBANK"));
        }
        if (status.verticalSpeed() <= SHAHED_SINK_RATE_MS) {
            out.add(DroneWarning.warn("SINK_RATE"));
        }
        if (drone != null) {
            final Vec3 velocity = trackVelocity(drone);
            if (collisionAhead(minecraft, drone, velocity)) {
                out.add(DroneWarning.warn("BLOCK_COLLISION"));
            } else if (groundBelow(minecraft, drone, SHAHED_TERRAIN_CLEARANCE)) {
                out.add(DroneWarning.warn("TERRAIN"));
            }
        }
        return out;
    }

    /** Forgets the tracked drone, so the next screen to open does not measure a velocity across the gap. */
    public static void reset() {
        trackedId = null;
        trackedVelocity = Vec3.ZERO;
        DroneWarningOverlay.reset();
    }

    /**
     * Velocity in blocks per second, measured from the entity's position rather than from
     * {@code getDeltaMovement()}: the client copy of a remotely simulated drone has its position interpolated
     * toward the server's, so the position delta is the signal that is actually maintained on this side.
     * Smoothed, because that interpolation arrives in uneven steps.
     *
     * <p>One slot is enough — only one drone screen can be up at a time.
     */
    private static Vec3 trackVelocity(final Entity entity) {
        final Vec3 position = entity.position();
        final long now = System.nanoTime();
        if (!entity.getUUID().equals(trackedId)) {
            trackedId = entity.getUUID();
            trackedPos = position;
            trackedNanos = now;
            trackedVelocity = Vec3.ZERO;
            return trackedVelocity;
        }
        final double elapsed = (now - trackedNanos) / 1.0E9D;
        if (elapsed <= 1.0E-4D) {
            return trackedVelocity;
        }
        trackedNanos = now;
        final Vec3 raw = position.subtract(trackedPos).scale(1.0D / elapsed);
        trackedPos = position;
        final double alpha = Mth.clamp(elapsed * 6.0D, 0.05D, 1.0D);
        trackedVelocity = trackedVelocity.add(raw.subtract(trackedVelocity).scale(alpha));
        return trackedVelocity;
    }

    private static boolean collisionAhead(final Minecraft minecraft, final Entity entity, final Vec3 velocity) {
        if (minecraft.level == null) {
            return false;
        }
        final double lookaheadSeconds = FullfudClientConfig.CLIENT.warningsCollisionLookaheadSeconds.get();
        if (lookaheadSeconds <= 0.0D) {
            return false;
        }
        final double speed = velocity.length();
        final Vec3 direction = speed > COLLISION_MIN_SPEED ? velocity.scale(1.0D / speed) : entity.getLookAngle();
        if (direction.lengthSqr() < 1.0E-6D) {
            return false;
        }
        final double distance = Mth.clamp(speed * lookaheadSeconds, COLLISION_MIN_DISTANCE, COLLISION_MAX_DISTANCE);
        final Vec3 from = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        return clipHits(minecraft, entity, from, from.add(direction.scale(distance)));
    }

    private static boolean groundBelow(final Minecraft minecraft, final Entity entity, final double clearance) {
        if (minecraft.level == null || clearance <= 0.0D) {
            return false;
        }
        final Vec3 from = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        return clipHits(minecraft, entity, from, from.subtract(0.0D, clearance, 0.0D));
    }

    private static boolean clipHits(final Minecraft minecraft, final Entity entity, final Vec3 from, final Vec3 to) {
        final HitResult hit = minecraft.level.clip(new ClipContext(
            from,
            to,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            entity
        ));
        return hit.getType() != HitResult.Type.MISS;
    }
}
