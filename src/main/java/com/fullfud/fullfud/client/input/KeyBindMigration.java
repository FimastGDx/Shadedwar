package com.fullfud.fullfud.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Moves this mod's key binds off keys vanilla already uses, once, on an existing profile.
 *
 * <p>Changing a default in code is not enough. {@code options.txt} records every mapping by name, so a
 * player who has already launched the mod keeps whatever was written the first time — and the collision
 * matters more than a normal one would: {@link KeyMapping} keys its lookup on the {@link InputConstants.Key},
 * one mapping per key, so binding E for a drone did not merely double up with the inventory, it took the
 * inventory out of service entirely.
 *
 * <p>Runs on the first client tick and writes a version marker next to the controller profile, so it does
 * not fight the player afterwards: rebinding a drone key back onto E is then their business, not ours.
 */
@Environment(EnvType.CLIENT)
public final class KeyBindMigration {

    /** Bump when a later release again needs to push players off a default. */
    private static final int CURRENT_VERSION = 1;
    private static final String FILE_NAME = "keybinds.version";
    private static final String MOD_CATEGORY = "key.categories.fullfud";

    private static boolean done;

    private KeyBindMigration() {
    }

    /** Safe to call every tick; does its work at most once per launch. */
    public static void runOnce() {
        if (done) {
            return;
        }
        done = true;

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) {
            done = false;
            return;
        }
        final Path marker = resolveMarkerPath(minecraft);
        if (marker != null && readVersion(marker) >= CURRENT_VERSION) {
            return;
        }
        if (resolveCollisions(minecraft.options)) {
            KeyMapping.resetMapping();
            minecraft.options.save();
        }
        writeVersion(marker);
    }

    /** @return whether anything was rebound */
    private static boolean resolveCollisions(final Options options) {
        boolean changed = false;
        for (final KeyMapping mapping : options.keyMappings) {
            if (!MOD_CATEGORY.equals(mapping.getCategory()) || mapping.isUnbound()) {
                continue;
            }
            if (!collidesWithForeignMapping(options, mapping)) {
                continue;
            }
            // The new default is the intended home; if even that is taken — a resource pack or another mod
            // moved something — leave the bind empty rather than keep breaking a vanilla control.
            mapping.setKey(mapping.getDefaultKey());
            if (collidesWithForeignMapping(options, mapping)) {
                mapping.setKey(InputConstants.UNKNOWN);
            }
            changed = true;
        }
        return changed;
    }

    /**
     * {@code same} compares the currently bound key, which is the only way to read it here: 1.21.4 keeps
     * {@code KeyMapping.key} private and exposes no getter.
     */
    private static boolean collidesWithForeignMapping(final Options options, final KeyMapping mapping) {
        if (mapping.isUnbound()) {
            return false;
        }
        for (final KeyMapping other : options.keyMappings) {
            if (other != mapping && !MOD_CATEGORY.equals(other.getCategory()) && !other.isUnbound()
                && other.same(mapping)) {
                return true;
            }
        }
        return false;
    }

    private static Path resolveMarkerPath(final Minecraft minecraft) {
        if (minecraft.gameDirectory == null) {
            return null;
        }
        return minecraft.gameDirectory.toPath().resolve("config").resolve("fullfud").resolve(FILE_NAME);
    }

    private static int readVersion(final Path path) {
        try {
            if (!Files.exists(path)) {
                return 0;
            }
            return Integer.parseInt(Files.readString(path).trim());
        } catch (final IOException | NumberFormatException ignored) {
            return 0;
        }
    }

    private static void writeVersion(final Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, Integer.toString(CURRENT_VERSION));
        } catch (final IOException ignored) {
            // Worst case the migration runs again next launch, which is harmless.
        }
    }
}
