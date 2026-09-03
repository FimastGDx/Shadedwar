package com.fullfud.fullfud.client.warning;

/**
 * One alert line on a drone screen. The text is a short fixed English token — {@code BLOCK_COLLISION},
 * {@code LOW_FUEL} — rather than a translated sentence, because the MAX7456 OSD font behind the FPV goggles
 * has uppercase ASCII and nothing else.
 *
 * <p>{@link Level#CAUTION} outranks {@link Level#WARN}: it is drawn above the warnings, in red, and while any
 * caution is up it is the only level that makes a sound.
 */
public record DroneWarning(String text, Level level) {

    public enum Level {
        WARN,
        CAUTION
    }

    public static DroneWarning warn(final String text) {
        return new DroneWarning(text, Level.WARN);
    }

    public static DroneWarning caution(final String text) {
        return new DroneWarning(text, Level.CAUTION);
    }
}
