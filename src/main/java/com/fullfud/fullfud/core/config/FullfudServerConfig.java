package com.fullfud.fullfud.core.config;

public final class FullfudServerConfig {
    public static final ConfigSpec SPEC;
    public static final Server SERVER;

    static {
        final ConfigSpec.Builder builder = new ConfigSpec.Builder();
        SERVER = new Server(builder);
        SPEC = builder.build("fullfud-server.toml");
    }

    private FullfudServerConfig() {
    }

    public static final class Server {
        public final ConfigSpec.BooleanValue disableExplosionBlockDamage;
        public final ConfigSpec.BooleanValue fpvSimpleFlight;
        public final ConfigSpec.DoubleValue fpvSimpleMaxSpeed;
        public final ConfigSpec.DoubleValue fpvSimpleAcceleration;
        public final ConfigSpec.DoubleValue fpvSimpleClimbRate;
        public final ConfigSpec.DoubleValue fpvSimpleYawDegreesPerSecond;
        public final ConfigSpec.DoubleValue fpvSimpleNeutralThrottle;
        public final ConfigSpec.DoubleValue fpvSimpleTiltDegrees;
        public final ConfigSpec.DoubleValue fpvSimpleAimPitchLimit;
        public final ConfigSpec.BooleanValue fpvAngleMode;
        public final ConfigSpec.DoubleValue fpvAngleModeMaxTiltDegrees;
        public final ConfigSpec.DoubleValue fpvAngleModeGain;
        public final ConfigSpec.DoubleValue fpvAngleModeMaxDegreesPerSecond;
        public final ConfigSpec.DoubleValue fpvAngleModeYawDegreesPerSecond;
        public final ConfigSpec.BooleanValue fpvLevelAssist;
        public final ConfigSpec.DoubleValue fpvLevelAssistGain;
        public final ConfigSpec.DoubleValue fpvLevelAssistMaxDegreesPerSecond;
        public final ConfigSpec.DoubleValue fpvLevelAssistInputThreshold;
        public final ConfigSpec.BooleanValue shahedBankControl;
        public final ConfigSpec.BooleanValue shahedDirectTurn;
        public final ConfigSpec.DoubleValue shahedDirectTurnDegreesPerSecond;
        public final ConfigSpec.DoubleValue shahedMaxBankDegrees;
        public final ConfigSpec.DoubleValue shahedMaxPitchDegrees;
        public final ConfigSpec.DoubleValue shahedAttitudeGain;
        public final ConfigSpec.DoubleValue shahedMaxTurnRateDegreesPerSecond;
        public final ConfigSpec.BooleanValue fp5SimpleGuidance;
        public final ConfigSpec.DoubleValue fp5TurnDegreesPerSecond;
        public final ConfigSpec.DoubleValue fp5PitchDegreesPerSecond;
        public final ConfigSpec.BooleanValue fp5HonourTargetAltitude;
        public final ConfigSpec.BooleanValue fpvDiagnosticLog;

        private Server(final ConfigSpec.Builder builder) {
            builder.push("world");

            disableExplosionBlockDamage = builder
                .comment("Globally disable block destruction from all explosions in every dimension, including TNT, creepers and mod explosions.")
                .define("disableExplosionBlockDamage", false);

            builder.pop();
            builder.push("flight");

            fpvSimpleFlight = builder
                .comment("Arcade FPV flight: the sticks command a velocity instead of a tilt, so a held key means 'move that way at a steady speed'")
                .comment("and releasing it stops the drone. The airframe still leans into the movement, but the lean is decoration.")
                .comment("This also makes the mouse stop disturbing the drone, since attitude is no longer something the pilot flies directly.")
                .comment("Turn it off for the simulated quadcopter (rotor model, momentum, angle or acro mode below).")
                .define("fpvSimpleFlight", true);

            fpvSimpleMaxSpeed = builder
                .comment("Top horizontal speed in blocks per second at full stick, in arcade flight.")
                .defineInRange("fpvSimpleMaxSpeed", 18.0D, 1.0D, 80.0D);

            fpvSimpleAcceleration = builder
                .comment("How hard arcade flight chases the commanded velocity, in blocks per second squared. Higher feels more like a cursor and less like an aircraft.")
                .defineInRange("fpvSimpleAcceleration", 30.0D, 1.0D, 200.0D);

            fpvSimpleClimbRate = builder
                .comment("Climb and descent speed in blocks per second at full throttle travel, in arcade flight.")
                .defineInRange("fpvSimpleClimbRate", 8.0D, 0.5D, 40.0D);

            fpvSimpleYawDegreesPerSecond = builder
                .comment("Turn rate in degrees per second at full yaw deflection, in arcade flight.")
                .defineInRange("fpvSimpleYawDegreesPerSecond", 120.0D, 5.0D, 720.0D);

            fpvSimpleNeutralThrottle = builder
                .comment("Throttle position that means 'hold this altitude' in arcade flight. Above it the drone climbs, below it it sinks;")
                .comment("the keyboard's altitude hold trims back to roughly this value on its own.")
                .defineInRange("fpvSimpleNeutralThrottle", 0.35D, 0.05D, 0.95D);

            fpvSimpleTiltDegrees = builder
                .comment("How far the airframe leans into its movement in arcade flight, at full stick. Pure decoration for the flight path —")
                .comment("but the camera is bolted to the airframe, so this is also how far the view tips over when you hold a direction key.")
                .comment("Arcade flight always sends full deflection from a key, so the simulated quadcopter's 30 degrees meant the camera")
                .comment("was permanently pitched right over while moving. Set 0 for a view that stays level.")
                .defineInRange("fpvSimpleTiltDegrees", 8.0D, 0.0D, 45.0D);

            fpvSimpleAimPitchLimit = builder
                .comment("How far off the horizon the mouse may aim the nose in arcade flight, in degrees. The drone flies along the")
                .comment("aimed direction, so this is also the steepest dive and climb reachable with the forward key. Set 0 to pin the")
                .comment("nose level and leave the mouse with the turn only.")
                .defineInRange("fpvSimpleAimPitchLimit", 55.0D, 0.0D, 85.0D);

            fpvAngleMode = builder
                .comment("Pitch and roll ask the FPV drone for a tilt instead of a rotation rate, so a held key means a fixed forward lean and releasing it levels off.")
                .comment("This is what makes the drone flyable from a keyboard. Turn it off for rate (acro) control with a proportional stick; 3D-mode drones ignore it either way.")
                .define("fpvAngleMode", true);

            fpvAngleModeMaxTiltDegrees = builder
                .comment("Tilt commanded at full pitch/roll deflection. Larger leans harder and accelerates faster, at the cost of fine control.")
                .defineInRange("fpvAngleModeMaxTiltDegrees", 30.0D, 1.0D, 80.0D);

            fpvAngleModeGain = builder
                .comment("Fraction of the remaining angle error taken out per second. Higher snaps to the commanded tilt, lower feels heavier.")
                .defineInRange("fpvAngleModeGain", 6.0D, 0.1D, 40.0D);

            fpvAngleModeMaxDegreesPerSecond = builder
                .comment("Upper bound on how fast angle mode may rotate the airframe toward the commanded tilt.")
                .defineInRange("fpvAngleModeMaxDegreesPerSecond", 240.0D, 10.0D, 720.0D);

            fpvAngleModeYawDegreesPerSecond = builder
                .comment("Yaw rate at full deflection while angle mode is on. The Betaflight curve gives about 700 deg/s, and a key is always at full deflection, which is unflyable.")
                .defineInRange("fpvAngleModeYawDegreesPerSecond", 90.0D, 5.0D, 720.0D);

            fpvLevelAssist = builder
                .comment("Level the FPV drone back toward the horizon while the pilot gives no pitch or roll input, like a flight controller's angle mode.")
                .comment("Pure acro (false) holds whatever tilt the sticks left behind, which is only practical with a proportional stick.")
                .define("fpvLevelAssist", true);

            fpvLevelAssistGain = builder
                .comment("Fraction of the remaining tilt removed per second. Higher is snappier and less lifelike.")
                .defineInRange("fpvLevelAssistGain", 2.5D, 0.0D, 20.0D);

            fpvLevelAssistMaxDegreesPerSecond = builder
                .comment("Upper bound on the levelling rate in degrees per second.")
                .defineInRange("fpvLevelAssistMaxDegreesPerSecond", 120.0D, 0.0D, 720.0D);

            fpvLevelAssistInputThreshold = builder
                .comment("Pitch/roll stick deflection below which the drone counts as hands-off and levelling may run.")
                .defineInRange("fpvLevelAssistInputThreshold", 0.08D, 0.0D, 1.0D);

            shahedBankControl = builder
                .comment("A/D holds the Shahed in a bank and the bank turns it, the way an aircraft actually turns; releasing the key rolls the wings level.")
                .comment("With this off, A/D commands a roll rate that keeps rolling while held and the airframe only changes heading if you also pitch while banked, which reads as 'it banks but flies straight'.")
                .define("shahedBankControl", true);

            shahedDirectTurn = builder
                .comment("Make the bank turn the Shahed at a fixed rate and drag its velocity around with it, instead of waiting for the")
                .comment("banked lift to bend the flight path. A/D then visibly turns left and right within a few dozen blocks.")
                .comment("Physically this is a cheat — a real airframe of this size needs a much wider circle — but it is the difference")
                .comment("between steering the drone and watching it drift. Turn it off for the coordinated-turn rate below.")
                .define("shahedDirectTurn", true);

            shahedDirectTurnDegreesPerSecond = builder
                .comment("Heading change per second at full bank when shahedDirectTurn is on. 60 turns the drone right around in six seconds.")
                .defineInRange("shahedDirectTurnDegreesPerSecond", 60.0D, 1.0D, 180.0D);

            shahedMaxBankDegrees = builder
                .comment("Bank angle commanded at full A/D deflection. Steeper turns tighter.")
                .defineInRange("shahedMaxBankDegrees", 45.0D, 5.0D, 80.0D);

            shahedMaxPitchDegrees = builder
                .comment("Pitch attitude commanded at full W/S deflection, nose-down positive.")
                .defineInRange("shahedMaxPitchDegrees", 25.0D, 5.0D, 80.0D);

            shahedAttitudeGain = builder
                .comment("Fraction of the remaining attitude error the Shahed takes out per second. Higher rolls into the bank more eagerly.")
                .defineInRange("shahedAttitudeGain", 2.5D, 0.1D, 20.0D);

            shahedMaxTurnRateDegreesPerSecond = builder
                .comment("Cap on the heading change a bank may produce. The uncapped rate is g*tan(bank)/airspeed, which only gets large when the airframe is nearly stalled.")
                .defineInRange("shahedMaxTurnRateDegreesPerSecond", 30.0D, 1.0D, 180.0D);

            fp5SimpleGuidance = builder
                .comment("Fly the FP5 Flamingo straight at its target: it turns onto the bearing at a fixed rate, points its velocity where its")
                .comment("nose points, and detonates on the target point itself.")
                .comment("The realistic profile it replaces launches on a locked heading with almost no turn authority, which makes a missile")
                .comment("aimed at anything other than a distant target circle away and never arrive.")
                .define("fp5SimpleGuidance", true);

            fp5TurnDegreesPerSecond = builder
                .comment("How fast simple guidance may swing the missile's heading onto the target bearing. High enough to turn around after launch.")
                .defineInRange("fp5TurnDegreesPerSecond", 90.0D, 5.0D, 360.0D);

            fp5PitchDegreesPerSecond = builder
                .comment("How fast simple guidance may change the missile's climb or dive angle.")
                .defineInRange("fp5PitchDegreesPerSecond", 60.0D, 5.0D, 360.0D);

            fp5HonourTargetAltitude = builder
                .comment("Aim the FP5 Flamingo at the Y typed into the monitor instead of at the ground under the target's X/Z.")
                .comment("This is what lets you hit a rooftop, a tower or an altitude in open air; the missile also cruises above the aim")
                .comment("point so it can descend onto it. A Y below the terrain surface is raised to the surface, because the missile")
                .comment("would strike the ground above it anyway.")
                .comment("Turn it off to always aim at the surface height, which ignores Y but forgives leaving the field at its default.")
                .define("fp5HonourTargetAltitude", true);

            builder.pop();
            builder.push("debug");

            fpvDiagnosticLog = builder
                .comment("Log one line per second per piloted FPV drone: the control values received, why a control packet was rejected, and the resulting throttle and velocity.")
                .comment("Diagnostic aid for the 1.21.4 port; set to false once flight behaves.")
                .define("fpvDiagnosticLog", true);

            builder.pop();
        }
    }
}
