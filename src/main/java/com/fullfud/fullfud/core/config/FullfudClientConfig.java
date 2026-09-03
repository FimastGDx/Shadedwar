package com.fullfud.fullfud.core.config;

public final class FullfudClientConfig {
    public static final ConfigSpec SPEC;
    public static final Client CLIENT;

    static {
        final ConfigSpec.Builder builder = new ConfigSpec.Builder();
        CLIENT = new Client(builder);
        SPEC = builder.build("fullfud-client.toml");
    }

    private FullfudClientConfig() { }

    public enum GamepadThrottleMode {
        LEFT_STICK_Y,
        RIGHT_TRIGGER
    }

    public static final class Client {
        public final ConfigSpec.BooleanValue fpvControllerEnabled;
        public final ConfigSpec.BooleanValue fpvControllerPreferGamepadMapping;
        public final ConfigSpec.IntValue fpvControllerJoystickId;

        public final ConfigSpec.DoubleValue fpvControllerDeadzone;
        public final ConfigSpec.DoubleValue fpvControllerThrottleSlew;

        public final ConfigSpec.EnumValue<GamepadThrottleMode> fpvControllerGamepadThrottleMode;

        public final ConfigSpec.BooleanValue fpvControllerDiagnosticLog;

        public final ConfigSpec.IntValue fpvControllerRawRollAxis;
        public final ConfigSpec.IntValue fpvControllerRawPitchAxis;
        public final ConfigSpec.IntValue fpvControllerRawYawAxis;
        public final ConfigSpec.IntValue fpvControllerRawThrottleAxis;

        public final ConfigSpec.BooleanValue fpvControllerInvertRoll;
        public final ConfigSpec.BooleanValue fpvControllerInvertPitch;
        public final ConfigSpec.BooleanValue fpvControllerInvertYaw;
        public final ConfigSpec.BooleanValue fpvControllerInvertThrottle;

        public final ConfigSpec.IntValue fpvControllerArmButton;
        public final ConfigSpec.BooleanValue fpvKeyboardSimpleFlight;
        public final ConfigSpec.BooleanValue fpvKeyboardInvertPitch;
        public final ConfigSpec.DoubleValue fpvKeyboardAxisMax;
        public final ConfigSpec.DoubleValue fpvKeyboardAxisRampSeconds;
        public final ConfigSpec.DoubleValue fpvKeyboardAxisReturnSeconds;
        public final ConfigSpec.BooleanValue fpvKeyboardThrottleHold;
        public final ConfigSpec.DoubleValue fpvKeyboardThrottleRampSeconds;
        public final ConfigSpec.BooleanValue fpvKeyboardAltitudeHold;
        public final ConfigSpec.DoubleValue fpvKeyboardAltitudeHoldGain;
        public final ConfigSpec.DoubleValue fpvKeyboardAltitudeHoldDeadzone;
        public final ConfigSpec.BooleanValue fpvKeyboardMouseSteer;
        public final ConfigSpec.DoubleValue fpvKeyboardMouseSensitivity;
        public final ConfigSpec.BooleanValue fpvKeyboardStrafeWithAd;
        public final ConfigSpec.BooleanValue fpvCameraForceFirstPerson;
        public final ConfigSpec.BooleanValue fpvCameraForceFov;
        public final ConfigSpec.IntValue fpvCameraFov;
        public final ConfigSpec.DoubleValue fpvCameraMouseSensitivity;
        public final ConfigSpec.BooleanValue fpvCameraMouseLookEnabled;
        public final ConfigSpec.BooleanValue fpvCameraControllerPriority;
        public final ConfigSpec.BooleanValue fpvCameraReleaseOnPause;
        public final ConfigSpec.BooleanValue fpvSuppressSpectatorHotbarKeys;
        public final ConfigSpec.BooleanValue fpvPostShaderEnabled;
        public final ConfigSpec.DoubleValue fpvPostShaderTimeScale;
        public final ConfigSpec.BooleanValue fpvHudEnabled;
        public final ConfigSpec.BooleanValue fpvHideVanillaHud;
        public final ConfigSpec.BooleanValue fpvHideHand;
        public final ConfigSpec.IntValue fpvRenderDistanceCap;
        public final ConfigSpec.BooleanValue fpvUseLocalEntityAudio;
        public final ConfigSpec.DoubleValue fpvSoundMaxDistance;

        public final ConfigSpec.IntValue shahedRenderDistanceCap;
        public final ConfigSpec.BooleanValue shahedGhostRenderEnabled;
        public final ConfigSpec.DoubleValue shahedGhostRenderRange;
        public final ConfigSpec.IntValue shahedGhostTimeoutTicks;
        public final ConfigSpec.BooleanValue shahedGhostRenderFullBright;
        public final ConfigSpec.BooleanValue shahedUseLocalEntityAudio;
        public final ConfigSpec.DoubleValue shahedLocalAudioActiveThreshold;
        public final ConfigSpec.DoubleValue shahedSoundMaxDistance;
        public final ConfigSpec.IntValue shahedStatusFreshnessMs;
        public final ConfigSpec.IntValue shahedMonitorControlIntervalTicks;
        public final ConfigSpec.DoubleValue shahedMonitorControlMaxDistance;
        public final ConfigSpec.DoubleValue shahedMonitorNoiseMaxDistance;
        public final ConfigSpec.DoubleValue shahedMonitorNoiseMaxOpacity;
        public final ConfigSpec.DoubleValue shahedMonitorJammerMaxRadius;
        public final ConfigSpec.DoubleValue shahedMonitorJammerFullStrengthRadius;
        public final ConfigSpec.DoubleValue shahedMonitorJammerEdgeStrength;
        public final ConfigSpec.DoubleValue shahedMonitorThrustDeltaStep;
        public final ConfigSpec.BooleanValue shahedMonitorCameraShakeEnabled;
        public final ConfigSpec.DoubleValue shahedMonitorEngineShakeScale;
        public final ConfigSpec.DoubleValue shahedMonitorDiveSpeedThreshold;
        public final ConfigSpec.DoubleValue shahedMonitorDiveSpeedRange;
        public final ConfigSpec.DoubleValue shahedMonitorDiveShakeScale;
        public final ConfigSpec.BooleanValue shahedMonitorPreviewEnabled;
        public final ConfigSpec.BooleanValue shahedMonitorReticleEnabled;

        public final ConfigSpec.BooleanValue droneAudioRemoteEnabled;
        public final ConfigSpec.BooleanValue droneAudioMuteRemoteFpvWhenLoaded;
        public final ConfigSpec.BooleanValue droneAudioMuteRemoteShahedWhenLoaded;
        public final ConfigSpec.DoubleValue droneAudioRemoteVolumeScale;
        public final ConfigSpec.IntValue droneAudioLoopTimeoutTicks;
        public final ConfigSpec.DoubleValue droneAudioLoopVolumeLerp;
        public final ConfigSpec.DoubleValue droneAudioLoopPitchLerp;
        public final ConfigSpec.DoubleValue droneAudioStopVolumeEpsilon;

        public final ConfigSpec.BooleanValue warningsEnabled;
        public final ConfigSpec.DoubleValue warningsSoundVolume;
        public final ConfigSpec.IntValue warningsBlinkPeriodMs;
        public final ConfigSpec.IntValue warningsWarnRepeatMs;
        public final ConfigSpec.IntValue warningsCautionRepeatMs;
        public final ConfigSpec.IntValue warningsLowBatteryPercent;
        public final ConfigSpec.IntValue warningsLowSignalPercent;
        public final ConfigSpec.IntValue warningsLowFuelPercent;
        public final ConfigSpec.DoubleValue warningsCollisionLookaheadSeconds;

        private Client(final ConfigSpec.Builder builder) {
            builder.push("fpv");
            builder.push("controller");

            fpvControllerEnabled = builder
                .comment("Enable joystick/gamepad input for FPV drone control. Keep disabled if you use only keyboard and mouse.")
                .define("enabled", false);

            fpvControllerPreferGamepadMapping = builder
                .comment("If the device is recognized as a GLFW 'gamepad', use standard gamepad mapping; otherwise use raw joystick axes mapping.")
                .define("preferGamepadMapping", true);

            fpvControllerJoystickId = builder
                .comment("GLFW joystick id (0..15). Use -1 for auto-detect.")
                .defineInRange("joystickId", -1, -1, 15);

            fpvControllerDeadzone = builder
                .comment("Deadzone for pitch/roll/yaw (0..0.5).")
                .defineInRange("deadzone", 0.05D, 0.0D, 0.5D);

            fpvControllerThrottleSlew = builder
                .comment("Throttle smoothing per tick (0 = instant, 1 = very slow).")
                .defineInRange("throttleSlew", 0.25D, 0.0D, 1.0D);

            fpvControllerGamepadThrottleMode = builder
                .comment("Gamepad throttle source. LEFT_STICK_Y mimics FPV Mode 2; RIGHT_TRIGGER mimics common game controls.")
                .defineEnum("gamepadThrottleMode", GamepadThrottleMode.RIGHT_TRIGGER);

            fpvControllerDiagnosticLog = builder
                .comment("Log one line per second while piloting an FPV drone: key states, controller detection and the values sent to the server.")
                .comment("Diagnostic aid for the 1.21.4 port; set to false once flight behaves.")
                .define("diagnosticLog", true);

            builder.pop();
            builder.push("rawMapping");

            fpvControllerRawRollAxis = builder
                .comment("Raw joystick axis index for roll.")
                .defineInRange("rollAxis", 0, 0, 31);

            fpvControllerRawPitchAxis = builder
                .comment("Raw joystick axis index for pitch.")
                .defineInRange("pitchAxis", 1, 0, 31);

            fpvControllerRawYawAxis = builder
                .comment("Raw joystick axis index for yaw.")
                .defineInRange("yawAxis", 3, 0, 31);

            fpvControllerRawThrottleAxis = builder
                .comment("Raw joystick axis index for throttle.")
                .defineInRange("throttleAxis", 2, 0, 31);

            fpvControllerInvertRoll = builder
                .comment("Invert roll axis.")
                .define("invertRoll", false);

            fpvControllerInvertPitch = builder
                .comment("Invert pitch axis (typical when pushing stick up gives negative values).")
                .define("invertPitch", true);

            fpvControllerInvertYaw = builder
                .comment("Invert yaw axis (to match default keyboard mapping where positive yaw means 'left').")
                .define("invertYaw", true);

            fpvControllerInvertThrottle = builder
                .comment("Invert throttle axis before mapping [-1..1] -> [0..1].")
                .define("invertThrottle", true);

            fpvControllerArmButton = builder
                .comment("Raw joystick button index used for arm/disarm toggle.")
                .defineInRange("armButton", 0, 0, 63);

            builder.pop();

            builder.push("keyboard");

            fpvKeyboardSimpleFlight = builder
                .comment("Send full deflection from a held key and stop sending mouse movement as a control input.")
                .comment("axisMax below exists to tame the Betaflight rate curve, which only applies to the simulated quadcopter; with the")
                .comment("server's flight.fpvSimpleFlight on, a key is a direction rather than a rotation rate, so holding it back only")
                .comment("makes the drone sluggish. Turn this off together with flight.fpvSimpleFlight.")
                .define("simpleFlight", true);

            fpvKeyboardInvertPitch = builder
                .comment("Swap the pitch keys, so S pitches the nose down and flies forward while W flies backwards.")
                .comment("That was the behaviour up to BETA.373; the default now matches the mouse, where W is forward.")
                .define("invertPitch", false);

            fpvKeyboardAxisMax = builder
                .comment("Stick deflection a held pitch/yaw key is allowed to reach (0..1). A key is on or off, so at 1.0 it")
                .comment("commands the full Betaflight rate — roughly 700 deg/s with the default rates — the instant it is pressed.")
                .defineInRange("axisMax", 0.45D, 0.05D, 1.0D);

            fpvKeyboardAxisRampSeconds = builder
                .comment("Time in seconds for a held key to travel from centre to axisMax. This is the emulated stick throw; 0 restores the old instant response.")
                .defineInRange("axisRampSeconds", 0.30D, 0.0D, 2.0D);

            fpvKeyboardAxisReturnSeconds = builder
                .comment("Time in seconds for the emulated stick to spring back to centre after the key is released.")
                .defineInRange("axisReturnSeconds", 0.12D, 0.0D, 2.0D);

            fpvKeyboardThrottleHold = builder
                .comment("Treat the jump key as 'throttle up' and the sneak key as 'throttle down', holding the last value when neither is pressed.")
                .comment("With this off, jump pins the throttle to its keyboard maximum and releasing it cuts the motors dead.")
                .define("throttleHold", true);

            fpvKeyboardThrottleRampSeconds = builder
                .comment("Time in seconds for the held throttle to travel across its whole keyboard range.")
                .defineInRange("throttleRampSeconds", 1.2D, 0.05D, 5.0D);

            fpvKeyboardAltitudeHold = builder
                .comment("With throttleHold on, trim the throttle back toward hover whenever neither throttle key is held, so releasing the jump key stops the climb instead of freezing it.")
                .comment("Turn this off for a raw held throttle, where the pilot trims the climb out with the sneak key.")
                .define("altitudeHold", true);

            fpvKeyboardAltitudeHoldGain = builder
                .comment("Throttle units removed per second per block/second of vertical speed. Higher settles faster and can overshoot into a bobbing hover.")
                .defineInRange("altitudeHoldGain", 0.06D, 0.0D, 1.0D);

            fpvKeyboardAltitudeHoldDeadzone = builder
                .comment("Vertical speed in blocks per second that counts as level; below it the throttle is left alone.")
                .defineInRange("altitudeHoldDeadzone", 0.25D, 0.0D, 5.0D);

            fpvKeyboardMouseSteer = builder
                .comment("Steer arcade flight with the mouse: sideways movement turns the drone, forward and back aims the nose up and down,")
                .comment("and the drone flies where the nose points. The mouse cannot bank the airframe here, so it can only ever change")
                .comment("where the drone is heading — which is why it is safe to have it back. Turn it off to fly on keys alone.")
                .define("mouseSteer", true);

            fpvKeyboardMouseSensitivity = builder
                .comment("Multiplier on the game's own mouse sensitivity for arcade steering. Only affects flight.fpvSimpleFlight;")
                .comment("the simulated quadcopter keeps camera.mouseSensitivity.")
                .defineInRange("mouseSensitivity", 1.0D, 0.05D, 5.0D);

            fpvKeyboardStrafeWithAd = builder
                .comment("Make A and D slide the drone sideways instead of turning it, so holding W and A flies a straight diagonal")
                .comment("rather than a circle. Turning is then the mouse's job, with the dedicated yaw keys as the fallback.")
                .comment("Turn this off to put the turn back on A and D.")
                .define("strafeWithAd", true);

            builder.pop();

            builder.push("camera");

            fpvCameraForceFirstPerson = builder
                .comment("Force first person camera while controlling FPV drone.")
                .define("forceFirstPerson", true);

            fpvCameraForceFov = builder
                .comment("Force custom FOV while controlling FPV drone.")
                .define("forceFov", true);

            fpvCameraFov = builder
                .comment("FPV camera FOV when forceFov is enabled.")
                .defineInRange("fov", 110, 30, 170);

            fpvCameraMouseSensitivity = builder
                .comment("Mouse sensitivity multiplier for FPV pitch and roll control.")
                .defineInRange("mouseSensitivity", 0.015D, 0.001D, 0.1D);

            fpvCameraMouseLookEnabled = builder
                .comment("Enable mouse look input while controlling FPV drone.")
                .define("mouseLookEnabled", true);

            fpvCameraControllerPriority = builder
                .comment("When controller is present, controller pitch/roll/yaw override keyboard and mouse.")
                .define("controllerPriority", true);

            fpvCameraReleaseOnPause = builder
                .comment("Send release packet when pause menu is opened.")
                .define("releaseOnPause", true);

            fpvSuppressSpectatorHotbarKeys = builder
                .comment("Suppress hotbar keys while camera is attached to FPV drone.")
                .define("suppressSpectatorHotbarKeys", true);

            fpvPostShaderEnabled = builder
                .comment("Enable FPV post processing shader effects.")
                .define("postShaderEnabled", true);

            fpvPostShaderTimeScale = builder
                .comment("Time scale multiplier for FPV post shader animation.")
                .defineInRange("postShaderTimeScale", 0.02D, 0.0D, 1.0D);

            fpvHudEnabled = builder
                .comment("Draw custom FPV HUD overlay.")
                .define("hudEnabled", true);

            fpvHideVanillaHud = builder
                .comment("Hide vanilla HUD while controlling FPV drone.")
                .define("hideVanillaHud", true);

            fpvHideHand = builder
                .comment("Hide hand rendering while controlling FPV drone.")
                .define("hideHand", true);

            builder.pop();

            builder.push("render");

            fpvRenderDistanceCap = builder
                .comment("Additional FPV render distance cap in blocks when frustum fallback is used.")
                .defineInRange("renderDistanceCap", 256, 64, 10000);

            builder.pop();

            builder.push("audio");

            fpvUseLocalEntityAudio = builder
                .comment("Use local FPV entity audio with Doppler, low-pass filtering, and interior sound while controlling.")
                .define("useLocalEntityAudio", true);

            fpvSoundMaxDistance = builder
                .comment("Maximum distance in blocks at which local FPV engine sound can be heard. Set to 0 for no limit.")
                .defineInRange("soundMaxDistance", 250.0D, 0.0D, 5000.0D);

            builder.pop();
            builder.pop();

            builder.push("shahed");

            builder.push("render");

            shahedRenderDistanceCap = builder
                .comment("Additional Shahed render distance cap in blocks when frustum fallback is used.")
                .defineInRange("renderDistanceCap", 2000, 64, 10000);

            shahedGhostRenderEnabled = builder
                .comment("Enable client side Shahed ghost rendering outside normal entity tracking.")
                .define("ghostRenderEnabled", true);

            shahedGhostRenderRange = builder
                .comment("Maximum distance in blocks to draw Shahed ghost render.")
                .defineInRange("ghostRenderRange", 10000.0D, 128.0D, 20000.0D);

            shahedGhostTimeoutTicks = builder
                .comment("How long to keep ghost state without updates.")
                .defineInRange("ghostTimeoutTicks", 60, 1, 1200);

            shahedGhostRenderFullBright = builder
                .comment("Render Shahed ghost with full brightness.")
                .define("ghostRenderFullBright", true);

            builder.pop();

            builder.push("audio");

            shahedUseLocalEntityAudio = builder
                .comment("Use local Shahed entity audio system on client. Disable to use only remote audio packets.")
                .define("useLocalEntityAudio", true);

            shahedLocalAudioActiveThreshold = builder
                .comment("Local Shahed engine activity threshold.")
                .defineInRange("localAudioActiveThreshold", 0.02D, 0.0D, 1.0D);

            shahedSoundMaxDistance = builder
                .comment("Maximum distance in blocks at which local Shahed engine sound can be heard. Set to 0 for no limit.")
                .defineInRange("soundMaxDistance", 1000.0D, 0.0D, 5000.0D);

            builder.pop();

            builder.push("monitor");

            shahedStatusFreshnessMs = builder
                .comment("How long status packets are considered fresh in Shahed monitor UI.")
                .defineInRange("statusFreshnessMs", 2000, 100, 30000);

            shahedMonitorControlIntervalTicks = builder
                .comment("Interval in ticks between Shahed monitor control packets.")
                .defineInRange("controlIntervalTicks", 1, 1, 20);

            shahedMonitorControlMaxDistance = builder
                .comment("Maximum control distance from player to Shahed in blocks.")
                .defineInRange("controlMaxDistance", 10000.0D, 128.0D, 20000.0D);

            shahedMonitorNoiseMaxDistance = builder
                .comment("Distance in blocks at which monitor distance noise reaches maximum.")
                .defineInRange("noiseMaxDistance", 10000.0D, 128.0D, 20000.0D);

            shahedMonitorNoiseMaxOpacity = builder
                .comment("Maximum opacity contributed by distance based monitor noise.")
                .defineInRange("noiseMaxOpacity", 0.5D, 0.0D, 1.0D);

            shahedMonitorJammerMaxRadius = builder
                .comment("Maximum jammer radius in blocks.")
                .defineInRange("jammerMaxRadius", 600.0D, 32.0D, 5000.0D);

            shahedMonitorJammerFullStrengthRadius = builder
                .comment("Jammer radius in blocks where effect is full strength.")
                .defineInRange("jammerFullStrengthRadius", 300.0D, 0.0D, 5000.0D);

            shahedMonitorJammerEdgeStrength = builder
                .comment("Remaining jammer strength at max radius.")
                .defineInRange("jammerEdgeStrength", 0.01D, 0.0D, 1.0D);

            shahedMonitorThrustDeltaStep = builder
                .comment("Thrust delta step applied each control tick.")
                .defineInRange("thrustDeltaStep", 0.02D, 0.0D, 1.0D);

            shahedMonitorCameraShakeEnabled = builder
                .comment("Enable monitor camera shake from thrust and dive speed.")
                .define("cameraShakeEnabled", true);

            shahedMonitorEngineShakeScale = builder
                .comment("Engine shake multiplier.")
                .defineInRange("engineShakeScale", 1.2D, 0.0D, 10.0D);

            shahedMonitorDiveSpeedThreshold = builder
                .comment("Vertical speed threshold for dive shake in m/s.")
                .defineInRange("diveSpeedThreshold", 15.0D, 0.0D, 200.0D);

            shahedMonitorDiveSpeedRange = builder
                .comment("Vertical speed range over threshold used to ramp dive shake.")
                .defineInRange("diveSpeedRange", 40.0D, 0.01D, 200.0D);

            shahedMonitorDiveShakeScale = builder
                .comment("Dive shake multiplier.")
                .defineInRange("diveShakeScale", 2.5D, 0.0D, 10.0D);

            shahedMonitorPreviewEnabled = builder
                .comment("Draw mini drone preview in monitor overlay.")
                .define("previewEnabled", true);

            shahedMonitorReticleEnabled = builder
                .comment("Draw center reticle in monitor overlay.")
                .define("reticleEnabled", true);

            builder.pop();
            builder.pop();

            builder.push("droneAudio");

            droneAudioRemoteEnabled = builder
                .comment("Enable remote drone audio packets.")
                .define("remoteEnabled", true);

            droneAudioMuteRemoteFpvWhenLoaded = builder
                .comment("Mute remote FPV audio when the FPV entity is locally loaded to avoid duplicates.")
                .define("muteRemoteFpvWhenLoaded", true);

            droneAudioMuteRemoteShahedWhenLoaded = builder
                .comment("Mute remote Shahed audio when the Shahed entity is locally loaded.")
                .define("muteRemoteShahedWhenLoaded", true);

            droneAudioRemoteVolumeScale = builder
                .comment("Volume scale multiplier for remote drone audio packets.")
                .defineInRange("remoteVolumeScale", 1.0D, 0.0D, 4.0D);

            droneAudioLoopTimeoutTicks = builder
                .comment("Ticks after last remote loop update before sound stops.")
                .defineInRange("loopTimeoutTicks", 40, 1, 600);

            droneAudioLoopVolumeLerp = builder
                .comment("Smoothing factor for remote loop volume.")
                .defineInRange("loopVolumeLerp", 0.35D, 0.0D, 1.0D);

            droneAudioLoopPitchLerp = builder
                .comment("Smoothing factor for remote loop pitch.")
                .defineInRange("loopPitchLerp", 0.25D, 0.0D, 1.0D);

            droneAudioStopVolumeEpsilon = builder
                .comment("Stop remote loop when volume is below epsilon and target is zero.")
                .defineInRange("stopVolumeEpsilon", 0.001D, 0.0D, 1.0D);

            builder.pop();
            builder.push("warnings");

            warningsEnabled = builder
                .comment("Draw the flashing alert lines on the FPV goggles and the Shahed monitor: yellow WARN for a")
                .comment("flight hazard, red CAUTION for a system that is running out. Every line on screen flashes on")
                .comment("one shared clock, and only the higher of the two levels present makes a sound.")
                .define("enabled", true);

            warningsSoundVolume = builder
                .comment("Volume of the alert tones. 0 leaves the text flashing silently.")
                .defineInRange("soundVolume", 0.8D, 0.0D, 2.0D);

            warningsBlinkPeriodMs = builder
                .comment("Length of one flash cycle in milliseconds. The alert tone is emitted on a flash, not on a timer of its own.")
                .defineInRange("blinkPeriodMs", 700, 100, 3000);

            warningsWarnRepeatMs = builder
                .comment("Shortest gap between repeats of the WARN tone while the same warning stays up. 0 sounds it once per appearance.")
                .defineInRange("warnRepeatMs", 3000, 0, 60000);

            warningsCautionRepeatMs = builder
                .comment("Shortest gap between repeats of the CAUTION tone. Shorter than the WARN gap on purpose — caution outranks warn.")
                .defineInRange("cautionRepeatMs", 1600, 0, 60000);

            warningsLowBatteryPercent = builder
                .comment("Battery charge at or below which the FPV goggles show LOW_BATTERY.")
                .defineInRange("lowBatteryPercent", 20, 0, 100);

            warningsLowSignalPercent = builder
                .comment("Link quality at or below which either screen shows LOW_SIGNAL.")
                .defineInRange("lowSignalPercent", 35, 0, 100);

            warningsLowFuelPercent = builder
                .comment("Remaining Shahed fuel, as a percentage of a full tank, at or below which the monitor shows LOW_FUEL.")
                .defineInRange("lowFuelPercent", 15, 0, 100);

            warningsCollisionLookaheadSeconds = builder
                .comment("How far ahead along the flight path BLOCK_COLLISION looks, in seconds of flight. The check never")
                .comment("reaches less than two blocks, so it also fires on an obstacle the drone is merely creeping up on.")
                .defineInRange("collisionLookaheadSeconds", 1.2D, 0.0D, 5.0D);

            builder.pop();
        }
    }
}
