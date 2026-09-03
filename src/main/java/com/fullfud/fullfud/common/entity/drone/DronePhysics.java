package com.fullfud.fullfud.common.entity.drone;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class DronePhysics {

    private static final float DEG_TO_RAD = 0.017453292f;
    private static final float RAD_TO_DEG = 57.29578f;
    /** Stick travel arcade flight treats as centred, so a drifting gamepad axis cannot fly the drone on its own. */
    private static final float SIMPLE_INPUT_DEADZONE = 0.08f;
    /** Total arcade nose pitch, aim plus lean, is held short of vertical so the heading stays recoverable. */
    private static final float MAX_SIMPLE_PITCH_RAD = 85.0f * DEG_TO_RAD;
    private static final float PI = 3.1415927f;
    private static final float TAU = 6.2831855f;
    private static final float RPM_TO_RADS = 0.10471976f;
    private static final float INCH_TO_M = 0.0254f;
    private static final float AIR_DENSITY = 1.225f;
    private static final float GRAVITY_ACCEL = 9.80665f;
    private static final float ELECTRIC_CONSTANT = 12.566371f * (float) Math.pow(10.0d, -7.0d);
    private static final float TEMP_COEFF = 0.00393f;
    private static final float REF_TEMP_K = 293.15f;
    private static final float THERMAL_TIME_CONST = 2000.0f;
    private static final float MOTOR_INERTIA_DENSITY = 1220.0f;
    private static final float BELL_INERTIA_OUTER = 8050.0f;
    private static final float BELL_INERTIA_INNER = 7000.0f;
    private static final float MOTOR_HEAT_CAPACITY_SCALE = 425.5f;
    private static final float FRAME_PLATE_DENSITY = 1550.0f;
    private static final float ARM_DENSITY = 2700.0f;
    private static final float ANTENNA_DENSITY = 750.0f;
    private static final float RK4_TIMESTEP = 0.0078125f;
    /** How fast the mouse's contribution to the angle-mode tilt target falls back to centre, per second. */
    private static final float ANGLE_MODE_MOUSE_CENTER_PER_SEC = 2.5f;    private static final float MAX_VELOCITY = 500.0f;
    private static final float REFERENCE_THRUST_LOADING = 1900.0f;
    private static final int BLADE_SEGMENTS = 5;

    private float motorKv;
    private float motorWidthMm;
    private float motorHeightMm;
    private int batteryCells;
    private int batteryMah;
    private float propDiameterInch;
    private float propPitchInch;
    private int bladeCount;
    private float massGrams;
    private float frameWidthMm;
    private float frameHeightMm;
    private float frameLengthMm;
    private float propWidthMm;
    private float armWidthMm;
    private float armThicknessMm;
    private float antennaLengthMm;
    private float rollRateSetting;
    private float rollSuper;
    private float rollExpo;
    private float pitchRateSetting;
    private float pitchSuper;
    private float pitchExpo;
    private float yawRateSetting;
    private float yawSuper;
    private float yawExpo;
    private float motorCommandScale;
    private float dragCoefficientSetting;
    private float thrustMultiplier;
    private boolean flightMode3d;

    private float massKg;
    private float propRadiusM;
    private float propPitchM;
    private float motorRadiusM;
    private float motorHeightM;
    private float propWidthM;
    private float motorKvRadPerVolt;
    private float maxAngularVelocity;
    private float dragCoeff;
    private float electricalResistance;
    private float totalInertia;
    private float motorThermalCapacity;
    private float motorSurfaceArea;
    private final float[] motorAngularVelocity = new float[4];
    private final float[] motorHeatState = new float[4];
    private final float[] motorTempK = new float[4];
    private final float[] rotorPhase = new float[4];

    private final Vector3f velocity = new Vector3f();
    private final Vector3f forward = new Vector3f(0.0f, 0.0f, 1.0f);
    private final Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
    private final Vector3f right = new Vector3f(1.0f, 0.0f, 0.0f);

    private float pitchRate;
    private float rollRate;
    private float yawRate;

    /**
     * Arcade attitude, held here rather than read back from the orientation. The nose angle the mouse has aimed
     * at, plus the cosmetic lean, are what the orientation is rebuilt from every step, so recovering them from
     * that same orientation would feed the lean back into the aim and walk the nose off on its own.
     */
    private float simpleAimPitchRad;
    private float simpleLeanPitchRad;
    private float simpleLeanRollRad;

    private boolean angleMode;
    private float angleModeMaxTiltRad = 30.0f * DEG_TO_RAD;
    private float angleModeGain = 6.0f;
    private float angleModeMaxRateRad = 240.0f * DEG_TO_RAD;
    private float angleModeYawRateRad = 90.0f * DEG_TO_RAD;
    private float angleModeMousePitch;
    private float angleModeMouseRoll;

    public DronePhysics() {
        applyPreset(DronePreset.STANDARD_STRIKE);
    }

    public void applyPreset(final DronePreset preset) {
        this.motorKv = preset.motorKv;
        this.motorWidthMm = preset.motorWidthMm;
        this.motorHeightMm = preset.motorHeightMm;
        this.batteryCells = preset.batteryCells;
        this.batteryMah = preset.batteryMah;
        this.propDiameterInch = preset.propDiameterInch;
        this.propPitchInch = preset.propPitchInch;
        this.bladeCount = preset.blades;
        this.massGrams = preset.massGrams;
        this.frameWidthMm = preset.frameWidthMm;
        this.frameHeightMm = preset.frameHeightMm;
        this.frameLengthMm = preset.frameLengthMm;
        this.propWidthMm = preset.propWidthMm;
        this.armWidthMm = preset.armWidthMm;
        this.armThicknessMm = preset.armThicknessMm;
        this.antennaLengthMm = preset.antennaLengthMm;
        this.rollRateSetting = preset.rollRate;
        this.rollSuper = preset.rollSuper;
        this.rollExpo = preset.rollExpo;
        this.pitchRateSetting = preset.pitchRate;
        this.pitchSuper = preset.pitchSuper;
        this.pitchExpo = preset.pitchExpo;
        this.yawRateSetting = preset.yawRate;
        this.yawSuper = preset.yawSuper;
        this.yawExpo = preset.yawExpo;
        this.motorCommandScale = preset.motorCommandScale;
        this.dragCoefficientSetting = preset.dragCoefficient;
        this.thrustMultiplier = preset.thrustMultiplier;
        this.flightMode3d = preset.flightMode3d;
        recalculateDerived();
        reset();
    }

    public void applyConfig(final FpvDroneConfig config) {
        if (config == null) {
            return;
        }
        this.rollRateSetting = clampRate(config.getRcRate(FpvDroneConfig.CHANNEL_ROLL));
        this.rollSuper = clampUnit(config.getSuperRate(FpvDroneConfig.CHANNEL_ROLL));
        this.rollExpo = clampUnit(config.getExpo(FpvDroneConfig.CHANNEL_ROLL));
        this.pitchRateSetting = clampRate(config.getRcRate(FpvDroneConfig.CHANNEL_PITCH));
        this.pitchSuper = clampUnit(config.getSuperRate(FpvDroneConfig.CHANNEL_PITCH));
        this.pitchExpo = clampUnit(config.getExpo(FpvDroneConfig.CHANNEL_PITCH));
        this.yawRateSetting = clampRate(config.getRcRate(FpvDroneConfig.CHANNEL_YAW));
        this.yawSuper = clampUnit(config.getSuperRate(FpvDroneConfig.CHANNEL_YAW));
        this.yawExpo = clampUnit(config.getExpo(FpvDroneConfig.CHANNEL_YAW));
        this.motorKv = config.getMotorKv();
        this.propDiameterInch = config.getPropDiameterInch();
        this.propPitchInch = config.getPropPitchInch();
        this.dragCoefficientSetting = clamp(config.getDragCoefficient(), 0.5f, 2.0f);
        this.thrustMultiplier = clamp(config.getThrustMultiplier(), 0.5f, 2.0f);
        this.flightMode3d = config.isFlightMode3d();
        recalculateDerived();
    }

    public void setControlRates(
        final float rollRate,
        final float rollSuper,
        final float rollExpo,
        final float pitchRate,
        final float pitchSuper,
        final float pitchExpo,
        final float yawRate,
        final float yawSuper,
        final float yawExpo
    ) {
        this.rollRateSetting = clampRate(rollRate);
        this.rollSuper = clampUnit(rollSuper);
        this.rollExpo = clampUnit(rollExpo);
        this.pitchRateSetting = clampRate(pitchRate);
        this.pitchSuper = clampUnit(pitchSuper);
        this.pitchExpo = clampUnit(pitchExpo);
        this.yawRateSetting = clampRate(yawRate);
        this.yawSuper = clampUnit(yawSuper);
        this.yawExpo = clampUnit(yawExpo);
    }

    /**
     * Switches the pitch/roll channels between rate (acro) and angle mode.
     *
     * <p>In angle mode the stick asks for a tilt instead of a rotation rate, which is the only way a keyboard
     * can fly precisely: a key that is held down commands a fixed forward lean rather than an ever-increasing
     * one, and letting go returns the airframe to level. Yaw stays a rate command but a much slower, linear
     * one — the Betaflight curve puts ~700 deg/s at full deflection, and a key is always at full deflection.
     */
    public void setAngleMode(
        final boolean enabled,
        final float maxTiltDegrees,
        final float gain,
        final float maxDegreesPerSecond,
        final float yawDegreesPerSecond
    ) {
        this.angleMode = enabled;
        this.angleModeMaxTiltRad = clamp(maxTiltDegrees, 1.0f, 80.0f) * DEG_TO_RAD;
        this.angleModeGain = Math.max(0.1f, gain);
        this.angleModeMaxRateRad = clamp(maxDegreesPerSecond, 10.0f, 720.0f) * DEG_TO_RAD;
        this.angleModeYawRateRad = clamp(yawDegreesPerSecond, 5.0f, 720.0f) * DEG_TO_RAD;
        if (!enabled) {
            angleModeMousePitch = 0.0f;
            angleModeMouseRoll = 0.0f;
        }
    }

    /** Whether angle mode is both enabled and applicable — 3D mode flies inverted on purpose, so it is exempt. */
    public boolean isAngleModeActive() {
        return angleMode && !flightMode3d;
    }

    private void recalculateDerived() {
        massKg = massGrams / 1000.0f;
        propRadiusM = propDiameterInch * 0.5f * INCH_TO_M;
        propPitchM = propPitchInch * INCH_TO_M;
        motorRadiusM = motorWidthMm * 0.001f * 0.5f;
        motorHeightM = motorHeightMm * 0.001f;
        propWidthM = propWidthMm * 0.001f;
        motorKvRadPerVolt = motorKv * RPM_TO_RADS;
        maxAngularVelocity = motorKvRadPerVolt * batteryCells * 4.2f;

        final float referenceRadius = (float) Math.cbrt(3.0d * massKg / (REFERENCE_THRUST_LOADING * 4.0d * PI));
        final float diskArea = PI * referenceRadius * referenceRadius;
        dragCoeff = AIR_DENSITY * diskArea * dragCoefficientSetting / 2.0f;

        motorThermalCapacity = MOTOR_HEAT_CAPACITY_SCALE * motorHeatCapacityVolume();
        motorSurfaceArea = motorSurfaceArea();
        electricalResistance = motorResistance();
        totalInertia = totalRotorInertia();
    }

    public void reset() {
        for (int i = 0; i < 4; i++) {
            motorAngularVelocity[i] = 0.0f;
            motorHeatState[i] = 0.0f;
            motorTempK[i] = REF_TEMP_K;
            rotorPhase[i] = 0.0f;
        }
        velocity.zero();
        forward.set(0.0f, 0.0f, 1.0f);
        up.set(0.0f, 1.0f, 0.0f);
        right.set(1.0f, 0.0f, 0.0f);
        pitchRate = 0.0f;
        rollRate = 0.0f;
        yawRate = 0.0f;
    }

    public Vec3 simulate(
        float dt,
        final float throttle,
        final float rollInput,
        final float pitchInput,
        final float yawInput,
        final float mousePitchDelta,
        final float mouseRollDelta,
        final boolean flightMode3d
    ) {
        if (dt <= 0.0f) {
            return Vec3.ZERO;
        }

        dt = Math.min(dt, 0.1f);
        final float normalizedThrottle = flightMode3d
            ? Mth.clamp(throttle, -1.0f, 1.0f)
            : Mth.clamp(throttle, 0.0f, 1.0f);

        applyRotationInput(rollInput, pitchInput, yawInput, mousePitchDelta, mouseRollDelta, dt);

        final Vector3f displacement = new Vector3f();
        final int maxSteps = (int) (dt / RK4_TIMESTEP) + 10;
        float elapsed = 0.0f;
        for (int step = 0; step < maxSteps && dt - elapsed > 1.0e-5f; step++) {
            final float subDt = Math.min(RK4_TIMESTEP, dt - elapsed);
            integrateStep(normalizedThrottle, subDt, displacement);
            elapsed += subDt;
        }

        return new Vec3(displacement.x, displacement.y, displacement.z);
    }

    /**
     * Arcade flight: the sticks command a velocity in the drone's heading frame and the airframe's lean is
     * cosmetic. Returns the displacement for this step, like {@link #simulate}.
     *
     * <p>The rotor model this replaces is honest about a quadcopter — thrust only points along the airframe's
     * up axis, so moving means tilting, and stopping means tilting back — and that is precisely what makes it
     * unflyable from a keyboard: the drone keeps the momentum of every lean, and any stray mouse movement is a
     * lean of its own. Here the drone chases a velocity instead, so a held key is a direction and a released
     * key is a stop.
     *
     * <p>Attitude is rebuilt every step from the heading, the aimed nose angle and the commanded lean rather than
     * integrated, so nothing can accumulate and the quaternion handed to the renderer and the camera stays in
     * step with the direction of travel.
     *
     * @param rollInput   sideways travel across the heading, i.e. a strafe, not a bank
     * @param pitchInput  travel along the aimed nose direction
     * @param yawInput    turn rate from the keys, added to whatever the mouse asks for
     * @param mouseYawDelta   heading change in radians the mouse asked for over this tick
     * @param mousePitchDelta nose-angle change in radians the mouse asked for over this tick, positive down
     * @param aimPitchLimitDegrees how far off the horizon the mouse may aim the nose
     */
    public Vec3 simulateSimple(
        float dt,
        final float throttle,
        final float rollInput,
        final float pitchInput,
        final float yawInput,
        final float mouseYawDelta,
        final float mousePitchDelta,
        final float maxSpeed,
        final float acceleration,
        final float climbRate,
        final float yawDegreesPerSecond,
        final float neutralThrottle,
        final float tiltDegrees,
        final float aimPitchLimitDegrees
    ) {
        if (dt <= 0.0f) {
            return Vec3.ZERO;
        }
        dt = Math.min(dt, 0.1f);

        // A keyboard sends 0 or full deflection, but a gamepad axis that rests slightly off centre would otherwise
        // command a permanent bank and a permanent sideways drift — which looks exactly like the drone refusing to
        // fly where it is pointing.
        final float roll = deadzone(clamp(rollInput, -1.0f, 1.0f));
        final float pitch = deadzone(clamp(pitchInput, -1.0f, 1.0f));
        final float yaw = deadzone(clamp(yawInput, -1.0f, 1.0f));

        // Yaw is negated for the same reason the rate path negates it: positive input means turn right, and a
        // positive rotation about world up swings the nose the other way. The mouse is already a finished angle
        // for this tick, so it is added straight on rather than through a rate.
        final float keyYawRad = yaw * clamp(yawDegreesPerSecond, 1.0f, 720.0f) * DEG_TO_RAD * dt;
        final float headingRad = (float) Math.atan2(forward.x, forward.z) - keyYawRad - mouseYawDelta;

        final float aimLimitRad = clamp(aimPitchLimitDegrees, 0.0f, 85.0f) * DEG_TO_RAD;
        final float previousPitch = simpleAimPitchRad + simpleLeanPitchRad;
        final float previousRoll = simpleLeanRollRad;
        simpleAimPitchRad = clamp(simpleAimPitchRad + mousePitchDelta, -aimLimitRad, aimLimitRad);

        // The lean is cosmetic, so it uses its own limit rather than the rotor model's commanded tilt: the camera
        // rides on the airframe, and a lean sized for a stick that can be held half way is far too much for a key
        // that is always at the stop.
        final float leanRad = clamp(tiltDegrees, 0.0f, 45.0f) * DEG_TO_RAD;
        final float maxStep = angleModeMaxRateRad * dt;
        final float responsiveness = clamp(angleModeGain * dt, 0.0f, 1.0f);
        simpleLeanPitchRad += clamp((pitch * leanRad - simpleLeanPitchRad) * responsiveness, -maxStep, maxStep);
        simpleLeanRollRad += clamp((roll * leanRad - simpleLeanRollRad) * responsiveness, -maxStep, maxStep);

        // The aimed nose angle is applied without smoothing: it is the pilot's own pointing, and lagging it would
        // put the camera behind the mouse. Only the lean is rate limited. The total is held short of vertical
        // because the heading is read back out of the basis next step, and past 90 degrees of pitch it flips.
        final float tiltPitch = clamp(simpleAimPitchRad + simpleLeanPitchRad, -MAX_SIMPLE_PITCH_RAD, MAX_SIMPLE_PITCH_RAD);
        final float tiltRoll = simpleLeanRollRad;

        pitchRate = (tiltPitch - previousPitch) / dt;
        rollRate = (tiltRoll - previousRoll) / dt;
        yawRate = -(keyYawRad + mouseYawDelta) / dt;

        setBasisFromHeadingAndTilt(headingRad, tiltPitch, tiltRoll);

        final float speedLimit = Math.max(0.1f, maxSpeed);
        final float aimCos = (float) Math.cos(simpleAimPitchRad);
        final float aimSin = (float) Math.sin(simpleAimPitchRad);
        // Travel follows the aimed nose, so aiming down and holding forward is a dive. The strafe stays level:
        // it is the axis the pilot uses to line a target up, and tilting it would turn every sidestep into a climb.
        final Vector3f headingForward = new Vector3f(
            (float) Math.sin(headingRad) * aimCos,
            -aimSin,
            (float) Math.cos(headingRad) * aimCos
        );
        // The airframe's own right-hand side, which is forward x worldUp — note that the basis vector called
        // `right` points the other way, at the aircraft's left.
        final Vector3f headingRight = new Vector3f(
            -(float) Math.cos(headingRad),
            0.0f,
            (float) Math.sin(headingRad)
        );
        final Vector3f target = new Vector3f(headingForward).mul(pitch).add(new Vector3f(headingRight).mul(roll));
        if (target.lengthSquared() > 1.0f) {
            target.normalize();
        }
        target.mul(speedLimit);

        // Throttle is an absolute position, so it has to mean an absolute vertical speed: the neutral point is
        // "hold altitude", and the travel either side of it is the climb and the descent. It adds to whatever the
        // aimed nose is already asking for, so the throttle keys still work while diving.
        final float neutral = clamp(neutralThrottle, 0.05f, 0.95f);
        final float clampedThrottle = clamp(throttle, 0.0f, 1.0f);
        final float span = Math.max(0.05f, clampedThrottle >= neutral ? 1.0f - neutral : neutral);
        final float commandedClimb = clamp((clampedThrottle - neutral) / span, -1.0f, 1.0f) * Math.max(0.1f, climbRate);
        final float verticalLimit = Math.max(0.1f, climbRate) + speedLimit;
        target.y = clamp(target.y + commandedClimb, -verticalLimit, verticalLimit);

        final Vector3f delta = new Vector3f(target).sub(velocity);
        final float maxDelta = Math.max(0.1f, acceleration) * dt;
        if (delta.lengthSquared() > maxDelta * maxDelta) {
            delta.normalize().mul(maxDelta);
        }
        velocity.add(delta);
        if (!Float.isFinite(velocity.x) || !Float.isFinite(velocity.y) || !Float.isFinite(velocity.z)) {
            velocity.zero();
        }

        return new Vec3(velocity.x * dt, velocity.y * dt, velocity.z * dt);
    }

    /** Drops the aimed nose angle and the lean, so a drone picked up again starts level rather than mid-dive. */
    public void resetSimpleAttitude() {
        simpleAimPitchRad = 0.0f;
        simpleLeanPitchRad = 0.0f;
        simpleLeanRollRad = 0.0f;
    }

    /** Ignores stick travel small enough to be an axis that does not quite return to centre. */
    private static float deadzone(final float input) {
        return Math.abs(input) < SIMPLE_INPUT_DEADZONE ? 0.0f : input;
    }

    /** Rebuilds the basis from a heading and a lean, in that order: yaw about world up, then pitch, then roll. */
    private void setBasisFromHeadingAndTilt(final float headingRad, final float pitchRad, final float rollRad) {
        forward.set(0.0f, 0.0f, 1.0f);
        up.set(0.0f, 1.0f, 0.0f);
        right.set(1.0f, 0.0f, 0.0f);
        rotateBasis(new Vector3f(0.0f, 1.0f, 0.0f), headingRad);
        rotateBasis(right, pitchRad);
        rotateBasis(forward, rollRad);
        orthonormalizeBasis();
    }

    private void applyRotationInput(
        final float rollInput,
        final float pitchInput,
        final float yawInput,
        final float mousePitchDelta,
        final float mouseRollDelta,
        final float dt
    ) {
        if (isAngleModeActive()) {
            applyAngleModeRotation(rollInput, pitchInput, yawInput, mousePitchDelta, mouseRollDelta, dt);
            return;
        }

        rollRate = shapeRate(rollInput, rollRateSetting, rollSuper, rollExpo) * DEG_TO_RAD;
        pitchRate = shapeRate(pitchInput, pitchRateSetting, pitchSuper, pitchExpo) * DEG_TO_RAD;
        yawRate = -shapeRate(yawInput, yawRateSetting, yawSuper, yawExpo) * DEG_TO_RAD;

        final float yawAngle = yawRate * dt;
        final float pitchAngle = pitchRate * dt + mousePitchDelta;
        final float rollAngle = rollRate * dt + mouseRollDelta;

        if (yawAngle != 0.0f) {
            rotateBasis(up, yawAngle);
        }
        if (pitchAngle != 0.0f) {
            rotateBasis(right, pitchAngle);
        }
        if (rollAngle != 0.0f) {
            rotateBasis(forward, rollAngle);
        }

        orthonormalizeBasis();
    }

    /**
     * Angle-mode attitude command: the sticks pick a tilt and a proportional controller rotates the airframe
     * toward it, rate-limited so it still looks like a real quad settling into the lean.
     *
     * <p>Pitch is measured nose-down-positive, matching the input convention ({@code forward.y} is positive
     * with the nose up), and roll is measured right-wing-down-positive from how far the basis has tipped away
     * from world up. Both are read back from the basis every tick rather than integrated, so the controller
     * cannot drift away from the attitude the rest of the simulation sees.
     *
     * <p>The mouse is a delta device with no centre, so its contribution rides on top of the stick target and
     * bleeds back to zero at {@link #ANGLE_MODE_MOUSE_CENTER_PER_SEC}; that keeps "let go and it levels" true
     * whichever device the pilot is holding.
     */
    private void applyAngleModeRotation(
        final float rollInput,
        final float pitchInput,
        final float yawInput,
        final float mousePitchDelta,
        final float mouseRollDelta,
        final float dt
    ) {
        final float decay = Math.max(0.0f, 1.0f - ANGLE_MODE_MOUSE_CENTER_PER_SEC * dt);
        angleModeMousePitch = clamp(angleModeMousePitch * decay + mousePitchDelta, -angleModeMaxTiltRad, angleModeMaxTiltRad);
        angleModeMouseRoll = clamp(angleModeMouseRoll * decay + mouseRollDelta, -angleModeMaxTiltRad, angleModeMaxTiltRad);

        final float currentPitch = (float) Math.asin(clamp(-forward.y, -1.0f, 1.0f));
        final float currentRoll = (float) Math.atan2(right.y, up.y);

        final float targetPitch = clamp(
            clamp(pitchInput, -1.0f, 1.0f) * angleModeMaxTiltRad + angleModeMousePitch,
            -angleModeMaxTiltRad,
            angleModeMaxTiltRad
        );
        final float targetRoll = clamp(
            clamp(rollInput, -1.0f, 1.0f) * angleModeMaxTiltRad + angleModeMouseRoll,
            -angleModeMaxTiltRad,
            angleModeMaxTiltRad
        );

        final float maxStep = angleModeMaxRateRad * dt;
        final float responsiveness = clamp(angleModeGain * dt, 0.0f, 1.0f);
        final float pitchStep = clamp((targetPitch - currentPitch) * responsiveness, -maxStep, maxStep);
        final float rollStep = clamp((targetRoll - currentRoll) * responsiveness, -maxStep, maxStep);

        pitchRate = dt > 0.0f ? pitchStep / dt : 0.0f;
        rollRate = dt > 0.0f ? rollStep / dt : 0.0f;
        yawRate = -clamp(yawInput, -1.0f, 1.0f) * angleModeYawRateRad;

        final float yawAngle = yawRate * dt;
        if (yawAngle != 0.0f) {
            rotateBasis(up, yawAngle);
        }
        if (pitchStep != 0.0f) {
            rotateBasis(right, pitchStep);
        }
        if (rollStep != 0.0f) {
            rotateBasis(forward, rollStep);
        }

        orthonormalizeBasis();
    }

    private static float shapeRate(float input, float rate, final float superRate, final float expo) {
        final float absInput = Math.abs(input);
        if (rate > 2.0f) {
            rate += 14.54f * (rate - 2.0f);
        }

        if (expo != 0.0f) {
            input = input * absInput * absInput * expo + input * (1.0f - expo);
        }

        float degPerSecond = 200.0f * rate * input;
        if (superRate != 0.0f) {
            final float superFactor = 1.0f / Mth.clamp(1.0f - absInput * superRate, 0.01f, 1.0f);
            degPerSecond *= superFactor;
        }
        return degPerSecond;
    }

    private void rotateBasis(final Vector3f axis, final float angle) {
        if (angle == 0.0f || axis.lengthSquared() <= 1.0e-8f) {
            return;
        }

        final Vector3f normalizedAxis = new Vector3f(axis).normalize();
        final Quaternionf rotation = new Quaternionf().fromAxisAngleRad(normalizedAxis, angle);
        rotation.transform(forward);
        rotation.transform(up);
        rotation.transform(right);
    }

    private void orthonormalizeBasis() {
        forward.normalize();
        if (up.lengthSquared() <= 1.0e-8f) {
            up.set(0.0f, 1.0f, 0.0f);
        } else {
            up.normalize();
        }

        right.set(new Vector3f(up).cross(forward));
        if (right.lengthSquared() <= 1.0e-8f) {
            right.set(1.0f, 0.0f, 0.0f);
        } else {
            right.normalize();
        }

        up.set(new Vector3f(forward).cross(right));
        if (up.lengthSquared() <= 1.0e-8f) {
            up.set(0.0f, 1.0f, 0.0f);
        } else {
            up.normalize();
        }
    }

    private void integrateStep(final float throttle, final float dt, final Vector3f displacement) {
        final float throttleScale = Mth.lerp(throttle, 4.0f, 3.6f);
        final float motorCommand = throttleScale * batteryCells * throttle * motorCommandScale * thrustMultiplier;

        final float speed = velocity.length();
        final Vector3f totalForce = new Vector3f(0.0f, -GRAVITY_ACCEL * massKg, 0.0f);
        if (speed > 0.0f) {
            totalForce.add(new Vector3f(velocity).normalize().mul(-speed * speed * dragCoeff));
        }

        for (int i = 0; i < 4; i++) {
            final RotorForces rotorForces = calculateRotorForces(i, motorAngularVelocity[i]);
            totalForce.add(rotorForces.force());

            final float signedCommand = ((i & 1) == 0 ? -1.0f : 1.0f) * motorCommand;
            final float nextAngularVelocity = integrateMotorAngularVelocity(i, motorAngularVelocity[i], dt, signedCommand, motorTempK[i]);
            motorHeatState[i] = integrateMotorHeatState(i, motorHeatState[i], motorTempK[i], signedCommand, motorAngularVelocity[i], dt);
            motorTempK[i] = temperatureFromHeatState(motorHeatState[i]);
            motorAngularVelocity[i] = nextAngularVelocity;
        }

        velocity.add(new Vector3f(totalForce).div(massKg).mul(dt));
        final float clampedSpeed = velocity.length();
        if (clampedSpeed > MAX_VELOCITY) {
            velocity.normalize().mul(MAX_VELOCITY);
        }

        displacement.add(new Vector3f(velocity).mul(dt));
    }

    private RotorForces calculateRotorForces(final int rotor, final float angularVelocity) {
        final Vector3f totalForce = new Vector3f();
        final Vector3f totalTorque = new Vector3f();

        final float basePhase = rotorPhase[rotor];
        final float segmentWidth = propRadiusM / (float) BLADE_SEGMENTS;
        final float motorDiameterM = motorWidthMm * 0.001f;

        for (int blade = 0; blade < bladeCount; blade++) {
            final float bladeAngle = basePhase + TAU * (float) blade / (float) bladeCount;
            final Vector3f bladeDirection = rotateAroundAxis(bladeAngle, up, new Vector3f(right).mul(-1.0f));

            for (int segment = 0; segment < BLADE_SEGMENTS; segment++) {
                final float segmentRadius = ((float) segment + 0.5f) / (float) BLADE_SEGMENTS * propRadiusM;
                if (segmentRadius < motorRadiusM) {
                    continue;
                }

                float chord = propWidthM;
                if (segmentRadius >= 0.0f && segmentRadius < motorDiameterM) {
                    chord *= segmentRadius / motorDiameterM;
                }

                final Vector3f bladeVelocity = bladeElementVelocity(up, velocity, bladeDirection, angularVelocity, segmentRadius);
                final Vector3f relativeVelocity = bladeRelativeVelocity(bladeVelocity, bladeDirection);
                if (relativeVelocity.lengthSquared() <= 1.0e-8f) {
                    continue;
                }

                final float angleOfAttack = bladeAngleOfAttack(rotor, propPitchM, segmentRadius, relativeVelocity, up, bladeDirection);
                final float liftCoeff = liftCoefficient(angleOfAttack);
                final float dragCoeff = bladeDragCoefficient(angleOfAttack);
                final Vector3f lift = liftForce(relativeVelocity, liftCoeff, angleOfAttack, segmentWidth, chord);
                final Vector3f drag = dragForce(relativeVelocity, bladeDirection, dragCoeff, segmentWidth, chord);

                totalTorque.add(new Vector3f(bladeDirection).cross(lift).mul(segmentRadius));
                totalForce.add(lift).add(drag);
            }
        }

        return new RotorForces(totalForce, totalTorque);
    }

    private float integrateMotorAngularVelocity(
        final int rotor,
        final float angularVelocity,
        final float dt,
        final float commandTorque,
        final float tempK
    ) {
        final float k1 = motorAngularAcceleration(rotor, angularVelocity, commandTorque, tempK);
        final float k2 = motorAngularAcceleration(rotor, angularVelocity + dt * 0.5f * k1, commandTorque, tempK);
        final float k3 = motorAngularAcceleration(rotor, angularVelocity + dt * 0.5f * k2, commandTorque, tempK);
        final float k4 = motorAngularAcceleration(rotor, angularVelocity + dt * k3, commandTorque, tempK);

        float nextAngularVelocity = angularVelocity + dt * (k1 + 2.0f * k2 + 2.0f * k3 + k4) / 6.0f;
        if (!Float.isFinite(nextAngularVelocity)) {
            nextAngularVelocity = 0.0f;
        }

        if (Math.abs(nextAngularVelocity) > maxAngularVelocity) {
            nextAngularVelocity = Math.signum(nextAngularVelocity) * maxAngularVelocity;
        }

        return nextAngularVelocity;
    }

    private float motorAngularAcceleration(
        final int rotor,
        final float angularVelocity,
        final float commandTorque,
        final float tempK
    ) {
        final float resistance = resistanceAtTemperature(tempK);
        final float current = motorCurrent(resistance, commandTorque, angularVelocity);
        final float motorTorque = current / motorKvRadPerVolt;
        final float aeroTorque = calculateRotorForces(rotor, angularVelocity).torque().dot(up);
        return (motorTorque + aeroTorque) / totalInertia;
    }

    private float integrateMotorHeatState(
        final int rotor,
        final float heatState,
        final float tempK,
        final float commandTorque,
        final float angularVelocity,
        final float dt
    ) {
        float nextHeatState = heatState + thermalRelaxation(tempK, dt) + electricalHeating(commandTorque, angularVelocity, dt);
        if (!Float.isFinite(nextHeatState)) {
            nextHeatState = 0.0f;
            motorTempK[rotor] = REF_TEMP_K;
        }
        return nextHeatState;
    }

    private float thermalRelaxation(final float tempK, final float dt) {
        if (motorThermalCapacity <= 0.0f || motorSurfaceArea <= 0.0f) {
            return 0.0f;
        }

        final float tau = motorThermalCapacity / (THERMAL_TIME_CONST * motorSurfaceArea);
        final float relaxedTemp = REF_TEMP_K + (tempK - REF_TEMP_K) * (float) Math.exp(-dt / tau);
        return motorThermalCapacity * (relaxedTemp - tempK);
    }

    private float electricalHeating(final float commandTorque, final float angularVelocity, final float dt) {
        final float current = motorCurrent(electricalResistance, commandTorque, angularVelocity);
        final float power = current * current * electricalResistance;
        return power * dt;
    }

    private float temperatureFromHeatState(final float heatState) {
        if (motorThermalCapacity <= 0.0f) {
            return REF_TEMP_K;
        }
        return heatState / motorThermalCapacity + REF_TEMP_K;
    }

    private float resistanceAtTemperature(final float tempK) {
        return electricalResistance * (1.0f + TEMP_COEFF * (tempK - REF_TEMP_K));
    }

    private float motorCurrent(final float resistance, final float commandTorque, final float angularVelocity) {
        if (Math.abs(resistance) <= 1.0e-8f || Math.abs(motorKvRadPerVolt) <= 1.0e-8f) {
            return 0.0f;
        }
        final float backEmf = -angularVelocity / motorKvRadPerVolt;
        return (commandTorque + backEmf) / resistance;
    }

    private float motorResistance() {
        final float motorDiameterM = motorRadiusM * 2.0f;
        final float poles = poleCount(motorDiameterM);
        final float magnets = magnetCount(motorDiameterM);
        final float magnetPitch = magneticPitch(motorRadiusM, magnets);
        final float turns = motorTurns(motorRadiusM, motorHeightM, motorKvRadPerVolt, poles, magnets);
        final float coilPitch = coilPitch(motorRadiusM, poles);
        final float offsetA = -coilPitch * 0.5f;
        final float offsetB = coilPitch * 0.5f;
        final float offsetC = coilPitch + coilPitch * 0.5f;
        final float fluxA = magneticFlux(offsetA, magnetPitch, motorRadiusM, motorHeightM, turns, 1.0f);
        final float fluxB = magneticFlux(offsetB, magnetPitch, motorRadiusM, motorHeightM, turns, -1.0f);
        final float fluxC = magneticFlux(offsetC, magnetPitch, motorRadiusM, motorHeightM, turns, 1.0f);
        final float totalFlux = fluxA + fluxB + fluxC;
        return totalFlux * (2.0f * motorHeightM + 2.0f * magneticPitch(motorRadiusM, magnets)) * 39.3701f / 12.0f / 1000.0f * 26.0f;
    }

    private float totalRotorInertia() {
        final float propDiameterM = propRadiusM * 2.0f;
        final float bladeMass = propMass(propDiameterM);
        final float bladeInertia = (float) bladeCount / 12.0f * bladeMass * (4.0f * propRadiusM * propRadiusM + propWidthM * propWidthM);
        final float bellMass = bellMass();
        final float bellInertia = bellMass * motorRadiusM * motorRadiusM;
        return bladeInertia + bellInertia;
    }

    private float motorHeatCapacityVolume() {
        final float motorVolume = PI * motorRadiusM * motorRadiusM * motorHeightM;
        return 5527.0f * motorVolume;
    }

    private float motorSurfaceArea() {
        return TAU * motorRadiusM * motorHeightM + TAU * motorRadiusM * motorRadiusM;
    }

    private float propMass(final float propDiameterM) {
        final float thicknessM = 0.002f;
        return MOTOR_INERTIA_DENSITY * (propDiameterM * 0.5f * propWidthM * thicknessM);
    }

    private float bellMass() {
        final float outerRing = PI * motorHeightM * (motorRadiusM * motorRadiusM - (motorRadiusM - 0.001f) * (motorRadiusM - 0.001f));
        final float innerRing = PI * motorHeightM * ((motorRadiusM - 0.001f) * (motorRadiusM - 0.001f) - (motorRadiusM - 0.002f) * (motorRadiusM - 0.002f));
        return BELL_INERTIA_OUTER * outerRing + BELL_INERTIA_INNER * innerRing;
    }

    private static float poleCount(final float motorDiameterM) {
        return motorDiameterM >= 0.018f ? 14.0f : 12.0f;
    }

    private static float magnetCount(final float motorDiameterM) {
        return motorDiameterM >= 0.018f ? 12.0f : 9.0f;
    }

    private static float coilPitch(final float motorRadiusM, final float poles) {
        return TAU * motorRadiusM / poles - 5.0e-4f;
    }

    private static float magneticPitch(final float motorRadiusM, final float magnets) {
        return TAU * motorRadiusM / magnets - 5.0e-4f;
    }

    private static float windingConstant(final float motorRadiusM, final float motorHeightM, final float poles) {
        final float polePitch = coilPitch(motorRadiusM, poles);
        final float wireHeight = 0.001f;
        return 1.48f / ELECTRIC_CONSTANT * polePitch * motorHeightM * wireHeight;
    }

    private static float magneticFlux(
        final float offset,
        final float magnetPitch,
        final float motorRadiusM,
        final float motorHeightM,
        final float windingConstant,
        final float sign
    ) {
        final float wireRadius = 0.00159f;
        final float halfPitch = magnetPitch * 0.5f;
        final float radiusA = wireRadius * wireRadius + (offset - halfPitch) * (offset - halfPitch);
        final float radiusB = wireRadius * wireRadius + (offset + halfPitch) * (offset + halfPitch);
        final float fieldScale = ELECTRIC_CONSTANT * motorRadiusM * motorHeightM * sign * windingConstant / 12.566371f;
        final float tripleRadius = 3.0f * wireRadius * wireRadius;
        return (float) ((double) fieldScale
            * ((double) tripleRadius * (Math.pow(radiusB, -2.5d) - Math.pow(radiusA, -2.5d))
            - (Math.pow(radiusB, -1.5d) - Math.pow(radiusA, -1.5d))));
    }

    private static float motorTurns(
        final float motorRadiusM,
        final float motorHeightM,
        final float motorKvRadPerVolt,
        final float poles,
        final float magnets
    ) {
        final float magnetPitch = magneticPitch(motorRadiusM, magnets);
        final float coilPitch = coilPitch(motorRadiusM, poles);
        final float windingConstant = windingConstant(motorRadiusM, motorHeightM, poles);
        final float offsetA = -coilPitch * 0.5f;
        final float offsetB = coilPitch * 0.5f;
        final float offsetC = coilPitch + coilPitch * 0.5f;
        final float fluxA = magneticFlux(offsetA, magnetPitch, motorRadiusM, motorHeightM, windingConstant, 1.0f);
        final float fluxB = magneticFlux(offsetB, magnetPitch, motorRadiusM, motorHeightM, windingConstant, -1.0f);
        final float fluxC = magneticFlux(offsetC, magnetPitch, motorRadiusM, motorHeightM, windingConstant, 1.0f);
        final float totalFlux = fluxA + fluxB + fluxC;
        return (float) Math.round(1.0f / (totalFlux * motorKvRadPerVolt));
    }

    private static Vector3f rotateAroundAxis(final float angle, final Vector3f axis, final Vector3f vector) {
        final Quaternionf rotation = new Quaternionf().fromAxisAngleRad(new Vector3f(axis).normalize(), angle);
        return rotation.transform(vector);
    }

    private static Vector3f bladeElementVelocity(
        final Vector3f up,
        final Vector3f velocity,
        final Vector3f bladeDirection,
        final float angularVelocity,
        final float radius
    ) {
        return new Vector3f(up)
            .cross(bladeDirection)
            .normalize()
            .mul(angularVelocity * radius)
            .add(velocity);
    }

    private static Vector3f bladeRelativeVelocity(final Vector3f bladeVelocity, final Vector3f bladeDirection) {
        final Vector3f oppositeVelocity = new Vector3f(bladeVelocity).mul(-1.0f);
        final Vector3f parallelComponent = project(oppositeVelocity, bladeDirection);
        return oppositeVelocity.sub(parallelComponent);
    }

    private static Vector3f project(final Vector3f vector, final Vector3f onto) {
        final float denom = onto.lengthSquared();
        if (denom <= 1.0e-8f) {
            return new Vector3f();
        }
        return new Vector3f(onto).mul(vector.dot(onto) / denom);
    }

    private static float bladeAngleOfAttack(
        final int rotor,
        final float pitchMeters,
        final float radius,
        final Vector3f relativeVelocity,
        final Vector3f up,
        final Vector3f bladeDirection
    ) {
        final float bladePitchAngle = (float) Math.atan2(pitchMeters, TAU * radius);
        final Vector3f airflowDirection = new Vector3f(relativeVelocity).mul(-1.0f).normalize();
        final Vector3f referenceDirection = new Vector3f(up).cross(bladeDirection);
        if ((rotor & 1) == 0) {
            referenceDirection.mul(-1.0f);
        }

        float orientation = Math.signum(up.dot(airflowDirection));
        if (orientation == 0.0f) {
            orientation = 1.0f;
        }

        float relativeAngle = orientation * angleBetween(referenceDirection, airflowDirection);
        float angleOfAttack = bladePitchAngle - relativeAngle;
        if ((rotor & 1) == 0) {
            angleOfAttack *= -1.0f;
        }
        return angleOfAttack;
    }

    private static float angleBetween(final Vector3f a, final Vector3f b) {
        final float denom = (float) Math.sqrt(a.lengthSquared() * b.lengthSquared());
        if (denom <= 1.0e-8f) {
            return 0.0f;
        }
        return (float) Math.acos(Mth.clamp(a.dot(b) / denom, -1.0f, 1.0f));
    }

    private static float liftCoefficient(final float angleOfAttack) {
        final float minLift = 0.4f;
        final float maxLift = 1.8f;
        final float shapedMin = (float) Math.pow(minLift, 1.15d);
        final float shapedMax = (float) Math.pow(maxLift, 1.15d);
        final float blend = -Mth.cos(angleOfAttack * 2.0f) + 1.0f;
        final float value = blend * 0.5f * (shapedMax - shapedMin) + shapedMin;
        return (float) Math.pow(value, 0.86956525d);
    }

    private static float bladeDragCoefficient(float angleOfAttack) {
        if (angleOfAttack < 0.0f) {
            angleOfAttack = PI + angleOfAttack % PI;
        } else {
            angleOfAttack %= PI;
        }

        final float sigma = 7.5f * DEG_TO_RAD;
        float leftTail = (float) Math.sqrt(2.0d / PI) * angleOfAttack * angleOfAttack
            * (float) Math.exp(-(angleOfAttack * angleOfAttack) / (2.0f * sigma * sigma))
            / (sigma * sigma * sigma);
        leftTail /= 7.0f;

        final float mirrored = PI - angleOfAttack;
        float rightTail = -(float) Math.sqrt(2.0d / PI) * mirrored * mirrored
            * (float) Math.exp(-(mirrored * mirrored) / (2.0f * sigma * sigma))
            / (sigma * sigma * sigma);
        rightTail /= 7.0f;

        return Mth.sin(angleOfAttack * 2.0f) + leftTail + rightTail;
    }

    private static Vector3f liftForce(
        final Vector3f relativeVelocity,
        final float liftCoeff,
        final float angleOfAttack,
        final float segmentWidth,
        final float chord
    ) {
        final float speed = relativeVelocity.length();
        final float bladeThickness = 0.002f;
        final float projectedArea =
            Math.abs(segmentWidth * chord * Mth.sin(angleOfAttack))
                + Math.abs(segmentWidth * bladeThickness * Mth.cos(angleOfAttack));
        final float force = liftCoeff * AIR_DENSITY * speed * speed * projectedArea / 2.0f;
        return new Vector3f(relativeVelocity).normalize().mul(force);
    }

    private static Vector3f dragForce(
        final Vector3f relativeVelocity,
        final Vector3f bladeDirection,
        final float dragCoeff,
        final float segmentWidth,
        final float chord
    ) {
        final float speed = relativeVelocity.length();
        final float area = segmentWidth * chord;
        final float force = dragCoeff * AIR_DENSITY * speed * speed * area / 2.0f;
        final Vector3f oppositeVelocity = new Vector3f(relativeVelocity).mul(-1.0f).normalize();
        return new Vector3f(bladeDirection).normalize().cross(oppositeVelocity).mul(force);
    }

    public Vec3 handleCollision(final Vec3 attempted, final Vec3 actual) {
        final Vec3 blocked = attempted.subtract(actual);
        final double blockedLength = blocked.length();
        if (blockedLength < 1.0e-6d) {
            return new Vec3(velocity.x, velocity.y, velocity.z);
        }

        final Vec3 normal = blocked.normalize();
        final double speed = new Vec3(velocity.x, velocity.y, velocity.z).length();

        final double dot = velocity.x * normal.x + velocity.y * normal.y + velocity.z * normal.z;
        final Vec3 normalVelocity = normal.scale(dot);
        final Vec3 tangentVelocity = new Vec3(velocity.x, velocity.y, velocity.z).subtract(normalVelocity);

        final double bounceFactor = 0.35d;
        final double frictionFactor = 0.8d;

        final Vec3 newVelocity = speed < 1.0d
            ? tangentVelocity.scale(frictionFactor)
            : tangentVelocity.scale(frictionFactor).subtract(normalVelocity.scale(bounceFactor));

        velocity.set((float) newVelocity.x, (float) newVelocity.y, (float) newVelocity.z);
        return newVelocity;
    }

    public Quaternionf getOrientation() {
        final Vector3f zAxis = new Vector3f(forward).normalize();
        final Vector3f xAxis = new Vector3f(up).cross(zAxis).normalize();
        final Vector3f yAxis = new Vector3f(zAxis).cross(xAxis).normalize();
        return quaternionFromAxes(xAxis, yAxis, zAxis);
    }

    private static Quaternionf quaternionFromAxes(final Vector3f xAxis, final Vector3f yAxis, final Vector3f zAxis) {
        final float m00 = xAxis.x;
        final float m01 = yAxis.x;
        final float m02 = zAxis.x;
        final float m10 = xAxis.y;
        final float m11 = yAxis.y;
        final float m12 = zAxis.y;
        final float m20 = xAxis.z;
        final float m21 = yAxis.z;
        final float m22 = zAxis.z;

        final Quaternionf quaternion = new Quaternionf();
        final float trace = m00 + m11 + m22;
        if (trace >= 0.0f) {
            float s = (float) Math.sqrt(trace + 1.0f);
            quaternion.w = 0.5f * s;
            s = 0.5f / s;
            quaternion.x = (m21 - m12) * s;
            quaternion.y = (m02 - m20) * s;
            quaternion.z = (m10 - m01) * s;
        } else if (m00 > m11 && m00 > m22) {
            float s = (float) Math.sqrt(1.0f + m00 - m11 - m22);
            quaternion.x = 0.5f * s;
            s = 0.5f / s;
            quaternion.y = (m10 + m01) * s;
            quaternion.z = (m02 + m20) * s;
            quaternion.w = (m21 - m12) * s;
        } else if (m11 > m22) {
            float s = (float) Math.sqrt(1.0f + m11 - m00 - m22);
            quaternion.y = 0.5f * s;
            s = 0.5f / s;
            quaternion.x = (m10 + m01) * s;
            quaternion.z = (m21 + m12) * s;
            quaternion.w = (m02 - m20) * s;
        } else {
            float s = (float) Math.sqrt(1.0f + m22 - m00 - m11);
            quaternion.z = 0.5f * s;
            s = 0.5f / s;
            quaternion.x = (m02 + m20) * s;
            quaternion.y = (m21 + m12) * s;
            quaternion.w = (m10 - m01) * s;
        }
        return quaternion.normalize();
    }

    public void setOrientation(final Quaternionf q) {
        final Quaternionf normalized = new Quaternionf(q).normalize();
        forward.set(0.0f, 0.0f, 1.0f);
        normalized.transform(forward);
        up.set(0.0f, 1.0f, 0.0f);
        normalized.transform(up);
        right.set(1.0f, 0.0f, 0.0f);
        normalized.transform(right);
        orthonormalizeBasis();
    }

    public Vector3f getVelocity() {
        return new Vector3f(velocity);
    }

    public void setVelocity(final Vec3 velocity) {
        this.velocity.set((float) velocity.x, (float) velocity.y, (float) velocity.z);
    }

    public void setVelocity(final float x, final float y, final float z) {
        this.velocity.set(x, y, z);
    }

    public float[] getMotorRpm() {
        final float[] rpm = new float[motorAngularVelocity.length];
        for (int i = 0; i < motorAngularVelocity.length; i++) {
            rpm[i] = motorAngularVelocity[i] / RPM_TO_RADS;
        }
        return rpm;
    }

    public Vector3f getUp() {
        return new Vector3f(up);
    }

    public Vector3f getForward() {
        return new Vector3f(forward);
    }

    public float getMassKg() {
        return massKg;
    }

    public float getPitchRate() {
        return pitchRate * RAD_TO_DEG;
    }

    public float getRollRate() {
        return rollRate * RAD_TO_DEG;
    }

    public float getYawRate() {
        return yawRate * RAD_TO_DEG;
    }

    public boolean isFlightMode3d() {
        return flightMode3d;
    }

    /**
     * Rotates the airframe back toward the horizon while preserving heading, as a flight-controller
     * "angle mode" assist would. Acro behaviour is what the rate curves give you: whatever tilt the sticks
     * leave behind is held, which a proportional stick can trim out and a keyboard key cannot. Only the tilt
     * of {@code up} away from world up is removed — {@code forward}'s horizontal component, and therefore the
     * heading the pilot yawed to, is untouched.
     *
     * @param dt          step in seconds
     * @param gain        proportional gain per second; the fraction of the remaining tilt removed in one second
     * @param maxDegPerSec upper bound on the correction rate, so a fully inverted drone rolls back at a
     *                     believable speed instead of snapping level
     * @return the correction actually applied, in degrees
     */
    public float applyLevelAssist(final float dt, final float gain, final float maxDegPerSec) {
        if (dt <= 0.0f || gain <= 0.0f || maxDegPerSec <= 0.0f) {
            return 0.0f;
        }

        final Vector3f worldUp = new Vector3f(0.0f, 1.0f, 0.0f);
        final Vector3f currentUp = new Vector3f(up);
        if (currentUp.lengthSquared() <= 1.0e-8f) {
            return 0.0f;
        }
        currentUp.normalize();

        final float tilt = (float) Math.acos(clamp(currentUp.dot(worldUp), -1.0f, 1.0f));
        if (tilt < 1.0e-4f) {
            return 0.0f;
        }

        final Vector3f axis = new Vector3f(currentUp).cross(worldUp);
        if (axis.lengthSquared() <= 1.0e-8f) {
            // Exactly inverted: any horizontal axis levels it, so pick the one that keeps the nose where it is.
            axis.set(new Vector3f(forward).cross(worldUp));
            if (axis.lengthSquared() <= 1.0e-8f) {
                return 0.0f;
            }
        }
        axis.normalize();

        final float maxStep = maxDegPerSec * DEG_TO_RAD * dt;
        final float step = Math.min(tilt, Math.min(tilt * clamp(gain * dt, 0.0f, 1.0f), maxStep));
        if (step < 1.0e-5f) {
            return 0.0f;
        }

        final Quaternionf correction = new Quaternionf().fromAxisAngleRad(axis, step);
        correction.transform(forward);
        correction.transform(up);
        correction.transform(right);
        orthonormalizeBasis();
        return step * RAD_TO_DEG;
    }

    private static float clampRate(final float value) {
        return Math.max(0.0F, Math.min(2.55F, value));
    }

    private static float clampUnit(final float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static float clamp(final float value, final float min, final float max) {
        return Math.max(min, Math.min(max, value));
    }

    private record RotorForces(Vector3f force, Vector3f torque) {
    }
}
