package com.fullfud.fullfud.common.entity.drone;

import net.minecraft.nbt.CompoundTag;

public final class FpvDroneConfig {
    public static final int CHANNEL_THROTTLE = 0;
    public static final int CHANNEL_YAW = 1;
    public static final int CHANNEL_PITCH = 2;
    public static final int CHANNEL_ROLL = 3;
    public static final int CHANNEL_COUNT = 4;

    private static final String TAG_RATES = "Rates";
    private static final String TAG_PHYSICS = "Physics";

    private final float[] rcRate = {1.0F, 1.15F, 1.15F, 1.15F};
    private final float[] superRate = {0.7F, 0.67F, 0.67F, 0.67F};
    private final float[] expo = {0.0F, 0.0F, 0.0F, 0.0F};

    private float motorKv = 1350.0F;
    private float propDiameterInch = 7.0F;
    private float propPitchInch = 4.0F;
    private float dragCoefficient = 1.08F;
    private float thrustMultiplier = 1.0F;
    private boolean flightMode3d;
    private String droneName = "7 Inch 6S";

    public static FpvDroneConfig defaults() {
        return new FpvDroneConfig();
    }

    public static FpvDroneConfig fromPreset(final DronePreset preset) {
        final FpvDroneConfig config = new FpvDroneConfig();
        if (preset == null) {
            return config;
        }
        config.setRcRate(CHANNEL_THROTTLE, 1.0F);
        config.setSuperRate(CHANNEL_THROTTLE, 0.7F);
        config.setExpo(CHANNEL_THROTTLE, 0.0F);
        config.setRcRate(CHANNEL_YAW, preset.yawRate);
        config.setSuperRate(CHANNEL_YAW, preset.yawSuper);
        config.setExpo(CHANNEL_YAW, preset.yawExpo);
        config.setRcRate(CHANNEL_PITCH, preset.pitchRate);
        config.setSuperRate(CHANNEL_PITCH, preset.pitchSuper);
        config.setExpo(CHANNEL_PITCH, preset.pitchExpo);
        config.setRcRate(CHANNEL_ROLL, preset.rollRate);
        config.setSuperRate(CHANNEL_ROLL, preset.rollSuper);
        config.setExpo(CHANNEL_ROLL, preset.rollExpo);
        config.setMotorKv(preset.motorKv);
        config.setPropDiameterInch(preset.propDiameterInch);
        config.setPropPitchInch(preset.propPitchInch);
        config.setDragCoefficient(preset.dragCoefficient);
        config.setThrustMultiplier(preset.thrustMultiplier);
        config.setFlightMode3d(preset.flightMode3d);
        config.setDroneName(preset.displayName);
        return config;
    }

    public FpvDroneConfig copy() {
        final FpvDroneConfig copy = new FpvDroneConfig();
        copy.load(save());
        return copy;
    }

    public float getRcRate(final int channel) {
        return channel >= 0 && channel < CHANNEL_COUNT ? rcRate[channel] : 1.0F;
    }

    public void setRcRate(final int channel, final float value) {
        if (channel >= 0 && channel < CHANNEL_COUNT) {
            rcRate[channel] = clamp(value, 0.0F, 2.55F);
        }
    }

    public float getSuperRate(final int channel) {
        return channel >= 0 && channel < CHANNEL_COUNT ? superRate[channel] : 0.7F;
    }

    public void setSuperRate(final int channel, final float value) {
        if (channel >= 0 && channel < CHANNEL_COUNT) {
            superRate[channel] = clamp(value, 0.0F, 1.0F);
        }
    }

    public float getExpo(final int channel) {
        return channel >= 0 && channel < CHANNEL_COUNT ? expo[channel] : 0.0F;
    }

    public void setExpo(final int channel, final float value) {
        if (channel >= 0 && channel < CHANNEL_COUNT) {
            expo[channel] = clamp(value, 0.0F, 1.0F);
        }
    }

    public float getMotorKv() {
        return motorKv;
    }

    public void setMotorKv(final float value) {
        motorKv = clamp(value, 500.0F, 30000.0F);
    }

    public float getPropDiameterInch() {
        return propDiameterInch;
    }

    public void setPropDiameterInch(final float value) {
        propDiameterInch = clamp(value, 1.0F, 12.0F);
    }

    public float getPropPitchInch() {
        return propPitchInch;
    }

    public void setPropPitchInch(final float value) {
        propPitchInch = clamp(value, 0.8F, 8.0F);
    }

    public float getDragCoefficient() {
        return dragCoefficient;
    }

    public void setDragCoefficient(final float value) {
        dragCoefficient = clamp(value, 0.5F, 2.0F);
    }

    public float getThrustMultiplier() {
        return thrustMultiplier;
    }

    public void setThrustMultiplier(final float value) {
        thrustMultiplier = clamp(value, 0.5F, 2.0F);
    }

    public boolean isFlightMode3d() {
        return flightMode3d;
    }

    public void setFlightMode3d(final boolean value) {
        flightMode3d = value;
    }

    public String getDroneName() {
        return droneName;
    }

    public void setDroneName(final String value) {
        if (value == null) {
            droneName = "7 Inch 6S";
            return;
        }
        final String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            droneName = "7 Inch 6S";
        } else {
            droneName = trimmed.length() > 20 ? trimmed.substring(0, 20) : trimmed;
        }
    }

    public void applyTurnPresetSlow() {
        applyTurnProfile(0.70F, 0.50F, 0.20F);
    }

    public void applyTurnPresetBalanced() {
        applyTurnProfile(1.15F, 0.67F, 0.0F);
    }

    public void applyTurnPresetFast() {
        applyTurnProfile(1.35F, 0.78F, 0.0F);
    }

    public void applyTurnPresetExtreme() {
        applyTurnProfile(1.60F, 0.85F, 0.0F);
    }

    public void resetTurnRatesToDefaults() {
        setRcRate(CHANNEL_YAW, defaults().getRcRate(CHANNEL_YAW));
        setRcRate(CHANNEL_PITCH, defaults().getRcRate(CHANNEL_PITCH));
        setRcRate(CHANNEL_ROLL, defaults().getRcRate(CHANNEL_ROLL));
        setSuperRate(CHANNEL_YAW, defaults().getSuperRate(CHANNEL_YAW));
        setSuperRate(CHANNEL_PITCH, defaults().getSuperRate(CHANNEL_PITCH));
        setSuperRate(CHANNEL_ROLL, defaults().getSuperRate(CHANNEL_ROLL));
        setExpo(CHANNEL_YAW, defaults().getExpo(CHANNEL_YAW));
        setExpo(CHANNEL_PITCH, defaults().getExpo(CHANNEL_PITCH));
        setExpo(CHANNEL_ROLL, defaults().getExpo(CHANNEL_ROLL));
    }

    public void resetToDefaults() {
        load(defaults().save());
    }

    private void applyTurnProfile(final float rcRateValue, final float superRateValue, final float expoValue) {
        setRcRate(CHANNEL_YAW, rcRateValue);
        setRcRate(CHANNEL_PITCH, rcRateValue);
        setRcRate(CHANNEL_ROLL, rcRateValue);
        setSuperRate(CHANNEL_YAW, superRateValue);
        setSuperRate(CHANNEL_PITCH, superRateValue);
        setSuperRate(CHANNEL_ROLL, superRateValue);
        setExpo(CHANNEL_YAW, expoValue);
        setExpo(CHANNEL_PITCH, expoValue);
        setExpo(CHANNEL_ROLL, expoValue);
    }

    public CompoundTag save() {
        final CompoundTag root = new CompoundTag();
        final CompoundTag rates = new CompoundTag();
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            rates.putFloat("RcRate" + i, rcRate[i]);
            rates.putFloat("SuperRate" + i, superRate[i]);
            rates.putFloat("Expo" + i, expo[i]);
        }
        root.put(TAG_RATES, rates);

        final CompoundTag physics = new CompoundTag();
        physics.putFloat("MotorKv", motorKv);
        physics.putFloat("PropDiameterInch", propDiameterInch);
        physics.putFloat("PropPitchInch", propPitchInch);
        physics.putFloat("DragCoefficient", dragCoefficient);
        physics.putFloat("ThrustMultiplier", thrustMultiplier);
        physics.putBoolean("FlightMode3d", flightMode3d);
        physics.putString("DroneName", droneName);
        root.put(TAG_PHYSICS, physics);
        return root;
    }

    public static FpvDroneConfig fromTag(final CompoundTag tag) {
        final FpvDroneConfig config = new FpvDroneConfig();
        config.load(tag);
        return config;
    }

    public void load(final CompoundTag root) {
        final FpvDroneConfig defaults = defaults();
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            rcRate[i] = defaults.rcRate[i];
            superRate[i] = defaults.superRate[i];
            expo[i] = defaults.expo[i];
        }
        motorKv = defaults.motorKv;
        propDiameterInch = defaults.propDiameterInch;
        propPitchInch = defaults.propPitchInch;
        dragCoefficient = defaults.dragCoefficient;
        thrustMultiplier = defaults.thrustMultiplier;
        flightMode3d = defaults.flightMode3d;
        droneName = defaults.droneName;

        if (root == null) {
            return;
        }
        if (root.contains(TAG_RATES, CompoundTag.TAG_COMPOUND)) {
            final CompoundTag rates = root.getCompound(TAG_RATES);
            for (int i = 0; i < CHANNEL_COUNT; i++) {
                if (rates.contains("RcRate" + i)) {
                    setRcRate(i, rates.getFloat("RcRate" + i));
                }
                if (rates.contains("SuperRate" + i)) {
                    setSuperRate(i, rates.getFloat("SuperRate" + i));
                }
                if (rates.contains("Expo" + i)) {
                    setExpo(i, rates.getFloat("Expo" + i));
                }
            }
        }
        if (root.contains(TAG_PHYSICS, CompoundTag.TAG_COMPOUND)) {
            final CompoundTag physics = root.getCompound(TAG_PHYSICS);
            if (physics.contains("MotorKv")) {
                setMotorKv(physics.getFloat("MotorKv"));
            }
            if (physics.contains("PropDiameterInch")) {
                setPropDiameterInch(physics.getFloat("PropDiameterInch"));
            }
            if (physics.contains("PropPitchInch")) {
                setPropPitchInch(physics.getFloat("PropPitchInch"));
            }
            if (physics.contains("DragCoefficient")) {
                setDragCoefficient(physics.getFloat("DragCoefficient"));
            }
            if (physics.contains("ThrustMultiplier")) {
                setThrustMultiplier(physics.getFloat("ThrustMultiplier"));
            }
            if (physics.contains("FlightMode3d")) {
                setFlightMode3d(physics.getBoolean("FlightMode3d"));
            }
            if (physics.contains("DroneName")) {
                setDroneName(physics.getString("DroneName"));
            }
        }
    }

    public static float shapeRate(float input, float rate, final float superRate, final float expo) {
        final float absInput = Math.abs(input);
        if (rate > 2.0F) {
            rate += 14.54F * (rate - 2.0F);
        }
        if (expo != 0.0F) {
            input = input * absInput * absInput * expo + input * (1.0F - expo);
        }
        float degPerSecond = 200.0F * rate * input;
        if (superRate != 0.0F) {
            final float superFactor = 1.0F / Math.max(0.01F, 1.0F - absInput * superRate);
            degPerSecond *= superFactor;
        }
        return degPerSecond;
    }

    private static float clamp(final float value, final float min, final float max) {
        return Math.max(min, Math.min(max, value));
    }
}
