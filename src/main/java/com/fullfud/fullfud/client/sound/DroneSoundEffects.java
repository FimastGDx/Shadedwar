package com.fullfud.fullfud.client.sound;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class DroneSoundEffects {
    private static final double SPEED_OF_SOUND = 110.0D;
    private static final double MAX_RADIAL_SPEED_RATIO = 0.35D;
    private static final float MIN_DOPPLER_PITCH = 0.88F;
    private static final float MAX_DOPPLER_PITCH = 1.18F;

    private DroneSoundEffects() {
    }

    public enum SoundProfile {
        FPV(0.035D, 4.0D, 1.2D, 0.08D, 0.12F, 0.75D, 28.0D, 140.0D, 0.38F, 0.85D),
        SHAHED(0.08D, 2.6D, 1.05D, 0.18D, 0.3F, 0.9D, 80.0D, 420.0D, 0.6F, 0.65D);

        private final double nearFieldRatio;
        private final double rolloffScale;
        private final double rolloffPower;
        private final double hfStartRatio;
        private final float minDistanceGainHF;
        private final double hfDistancePower;
        private final double altitudeThreshold;
        private final double altitudeMax;
        private final float minAltitudeGainHF;
        private final double altitudePower;

        SoundProfile(
            final double nearFieldRatio,
            final double rolloffScale,
            final double rolloffPower,
            final double hfStartRatio,
            final float minDistanceGainHF,
            final double hfDistancePower,
            final double altitudeThreshold,
            final double altitudeMax,
            final float minAltitudeGainHF,
            final double altitudePower
        ) {
            this.nearFieldRatio = nearFieldRatio;
            this.rolloffScale = rolloffScale;
            this.rolloffPower = rolloffPower;
            this.hfStartRatio = hfStartRatio;
            this.minDistanceGainHF = minDistanceGainHF;
            this.hfDistancePower = hfDistancePower;
            this.altitudeThreshold = altitudeThreshold;
            this.altitudeMax = altitudeMax;
            this.minAltitudeGainHF = minAltitudeGainHF;
            this.altitudePower = altitudePower;
        }
    }

    public static float computeDistanceGainHF(
        final double distance,
        final double maxAudibleDistance,
        final SoundProfile profile
    ) {
        if (maxAudibleDistance <= 0.0D) {
            return 1.0F;
        }
        final double hfStart = Math.max(12.0D, maxAudibleDistance * profile.hfStartRatio);
        if (distance <= hfStart) {
            return 1.0F;
        }
        if (distance >= maxAudibleDistance) {
            return profile.minDistanceGainHF;
        }
        double hfRange = maxAudibleDistance - hfStart;
        if (hfRange <= 0.0D) {
            hfRange = 1.0D;
        }
        final double normalized = Mth.clamp((distance - hfStart) / hfRange, 0.0D, 1.0D);
        final float airAbsorption = (float) Math.pow(normalized, profile.hfDistancePower);
        return Mth.lerp(airAbsorption, 1.0F, profile.minDistanceGainHF);
    }

    public static float computeDopplerPitch(final Vec3 dronePos, final Vec3 droneVelocity, final Vec3 playerPos) {
        final Vec3 toPlayer = playerPos.subtract(dronePos);
        final double distance = toPlayer.length();
        if (distance < 0.01D) {
            return 1.0F;
        }
        final Vec3 dirToPlayer = toPlayer.scale(1.0D / distance);
        final double radialVelocity = Mth.clamp(
            droneVelocity.dot(dirToPlayer),
            -SPEED_OF_SOUND * MAX_RADIAL_SPEED_RATIO,
            SPEED_OF_SOUND * MAX_RADIAL_SPEED_RATIO
        );
        final double denominator = SPEED_OF_SOUND - radialVelocity;
        if (denominator <= 0.1D) {
            return MAX_DOPPLER_PITCH;
        }
        final float dopplerPitch = (float) (SPEED_OF_SOUND / denominator);
        return Mth.clamp(dopplerPitch, MIN_DOPPLER_PITCH, MAX_DOPPLER_PITCH);
    }

    public static float computeAltitudeGainHF(final double droneY, final double playerY, final SoundProfile profile) {
        final double altitudeDiff = Math.abs(droneY - playerY);
        if (altitudeDiff <= profile.altitudeThreshold) {
            return 1.0F;
        }
        if (altitudeDiff >= profile.altitudeMax) {
            return profile.minAltitudeGainHF;
        }
        final double normalized = (altitudeDiff - profile.altitudeThreshold) / (profile.altitudeMax - profile.altitudeThreshold);
        final float altitudePenalty = (float) Math.pow(Mth.clamp(normalized, 0.0D, 1.0D), profile.altitudePower);
        return Mth.lerp(altitudePenalty, 1.0F, profile.minAltitudeGainHF);
    }

    public static float computeCombinedGainHF(
        final double distance,
        final double droneY,
        final double playerY,
        final double maxAudibleDistance,
        final SoundProfile profile
    ) {
        final float distanceGainHF = computeDistanceGainHF(distance, maxAudibleDistance, profile);
        final float altitudeGainHF = computeAltitudeGainHF(droneY, playerY, profile);
        return distanceGainHF * altitudeGainHF;
    }

    public static float computeDistanceVolumeFactor(
        final double distance,
        final double maxAudibleDistance,
        final SoundProfile profile
    ) {
        if (maxAudibleDistance <= 0.0D) {
            return 1.0F;
        }
        final double nearField = Math.max(4.0D, maxAudibleDistance * profile.nearFieldRatio);
        if (distance <= nearField) {
            return 1.0F;
        }
        if (distance >= maxAudibleDistance) {
            return 0.0F;
        }
        double attenRange = maxAudibleDistance - nearField;
        if (attenRange <= 0.0D) {
            attenRange = 1.0D;
        }
        final double normalized = Mth.clamp((distance - nearField) / attenRange, 0.0D, 1.0D);
        final double rolloff = 1.0D / (1.0D + profile.rolloffScale * Math.pow(normalized, profile.rolloffPower));
        final double edgeFade = 1.0D - Math.pow(normalized, 2.4D);
        return (float) Mth.clamp(rolloff * edgeFade, 0.0D, 1.0D);
    }
}
