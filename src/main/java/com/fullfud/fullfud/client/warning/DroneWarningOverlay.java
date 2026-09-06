package com.fullfud.fullfud.client.warning;

import com.fullfud.fullfud.core.FullfudRegistries;
import com.fullfud.fullfud.core.config.FullfudClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The flash clock and the alert audio, shared by every drone screen.
 *
 * <p>The flash phase is derived from the wall clock instead of from per-line state, which is what makes several
 * warnings on screen at once necessarily blink together: they all read the same phase in the same frame. The
 * tone is emitted on a flash rather than on a timer of its own, so the sound lands with the text lighting up.
 *
 * <p>Only the higher of the two levels present sounds. A caution therefore silences the warnings underneath it,
 * and when the caution clears the warning below it is treated as new and announces itself at once.
 */
public final class DroneWarningOverlay {
    /** Lines beyond this are dropped rather than pushed into the rest of the HUD. Cautions are kept first. */
    public static final int MAX_LINES = 4;

    private static final int WARN_RGB = 0xFFD21F;
    private static final int CAUTION_RGB = 0xFF2A22;
    private static final float TAU = (float) (Math.PI * 2.0D);
    /** Fraction of a cycle the text spends fully dark, as a share of the raw cosine wave. */
    private static final float BLINK_FLOOR = 0.12F;
    private static final float BLINK_RANGE = 0.55F;

    private static final Set<String> announced = new HashSet<>();

    private static float lastPhase;
    private static long lastSoundMillis;

    private DroneWarningOverlay() {
    }

    /**
     * Orders the collected warnings for display: cautions first, then warnings, each in the order the collector
     * produced them, with duplicates and anything past {@link #MAX_LINES} dropped.
     */
    public static List<DroneWarning> prioritise(final List<DroneWarning> collected) {
        if (collected == null || collected.isEmpty()) {
            return List.of();
        }
        final List<DroneWarning> ordered = new ArrayList<>(collected.size());
        final Set<String> seen = new HashSet<>();
        for (final DroneWarning.Level level : new DroneWarning.Level[]{DroneWarning.Level.CAUTION, DroneWarning.Level.WARN}) {
            for (final DroneWarning warning : collected) {
                if (warning == null || warning.level() != level || !seen.add(warning.text())) {
                    continue;
                }
                if (ordered.size() >= MAX_LINES) {
                    return ordered;
                }
                ordered.add(warning);
            }
        }
        return ordered;
    }

    /**
     * Advances the flash clock, plays the alert tone when one is due, and returns the brightness every line on
     * screen should be drawn at this frame, from 0 (dark part of the cycle) to 1.
     *
     * <p>Call once per frame per screen with the already-prioritised list; an empty list resets the state so the
     * next warning to appear announces itself immediately.
     */
    public static float update(final List<DroneWarning> active) {
        final long now = System.currentTimeMillis();
        final int periodMs = Math.max(80, FullfudClientConfig.CLIENT.warningsBlinkPeriodMs.get());
        final float phase = (now % periodMs) / (float) periodMs;
        // The phase running backwards means it wrapped since the previous frame, i.e. a new flash starts now.
        final boolean flashStarted = phase < lastPhase;
        lastPhase = phase;

        if (active == null || active.isEmpty()) {
            announced.clear();
            return 0.0F;
        }

        DroneWarning.Level top = DroneWarning.Level.WARN;
        for (final DroneWarning warning : active) {
            if (warning.level() == DroneWarning.Level.CAUTION) {
                top = DroneWarning.Level.CAUTION;
                break;
            }
        }

        final Set<String> current = new HashSet<>();
        boolean fresh = false;
        for (final DroneWarning warning : active) {
            current.add(warning.text());
            if (warning.level() == top && announced.add(warning.text())) {
                fresh = true;
            }
        }
        // Forgetting the alerts that have cleared is what lets the same one announce itself again if it returns.
        announced.retainAll(current);

        final int repeatMs = top == DroneWarning.Level.CAUTION
            ? FullfudClientConfig.CLIENT.warningsCautionRepeatMs.get()
            : FullfudClientConfig.CLIENT.warningsWarnRepeatMs.get();
        final boolean repeatDue = repeatMs > 0 && flashStarted && now - lastSoundMillis >= repeatMs;
        if (fresh || repeatDue) {
            play(top);
            lastSoundMillis = now;
        }

        final float wave = 0.5F + 0.5F * Mth.cos(phase * TAU);
        return Mth.clamp((wave - BLINK_FLOOR) / BLINK_RANGE, 0.0F, 1.0F);
    }

    /** Text colour for a level at the current flash brightness. */
    public static int textColor(final DroneWarning.Level level, final float brightness) {
        final int rgb = level == DroneWarning.Level.CAUTION ? CAUTION_RGB : WARN_RGB;
        return (alpha(brightness, 1.0F) << 24) | rgb;
    }

    /**
     * Backdrop colour for the MAX7456 renderer, which draws a box behind every glyph. It has to fade with the
     * text, or the dark half of the cycle leaves a row of black boxes sitting on the video feed.
     */
    public static int backgroundColor(final float brightness) {
        return alpha(brightness, 0.8F) << 24;
    }

    /** Drops the flash and audio state, so the next screen to open starts on a fresh announcement. */
    public static void reset() {
        announced.clear();
        lastSoundMillis = 0L;
        lastPhase = 0.0F;
    }

    private static int alpha(final float brightness, final float scale) {
        return Mth.clamp(Math.round(brightness * scale * 255.0F), 0, 255);
    }

    private static void play(final DroneWarning.Level level) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getSoundManager() == null) {
            return;
        }
        final float volume = (float) Mth.clamp(FullfudClientConfig.CLIENT.warningsSoundVolume.get(), 0.0D, 2.0D);
        if (volume <= 0.0F) {
            return;
        }
        final SoundEvent sound = level == DroneWarning.Level.CAUTION
            ? FullfudRegistries.ALERT_CAUTION.get()
            : FullfudRegistries.ALERT_WARN.get();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F, volume));
    }
}
