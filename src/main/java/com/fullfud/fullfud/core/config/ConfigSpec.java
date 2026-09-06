package com.fullfud.fullfud.core.config;

import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Stand-in for Forge's {@code net.minecraftforge.common.ForgeConfigSpec}.
 *
 * <p>Fabric ships no config framework, so this reproduces just the slice the mod used: a builder with
 * {@code push}/{@code pop}/{@code comment}/{@code define}/{@code defineInRange}/{@code defineEnum},
 * and value handles whose {@code get()} is read uncached at every use site — which is exactly how the
 * Forge version behaved, so no use site needed changing.
 *
 * <p>The on-disk format is the flat-key subset of TOML that Forge's own writer produced: {@code #}
 * comments, {@code [section]} headers, {@code key = value}. Section headers are honoured on read, so a
 * config written by the Forge build loads unchanged. Unknown keys are ignored and the file is rewritten
 * after every load, so values added by an update appear with their documentation.
 */
public final class ConfigSpec {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final String fileName;
    private final List<ConfigValue<?>> values;
    private final Map<String, ConfigValue<?>> byPath;

    private ConfigSpec(final String fileName, final List<ConfigValue<?>> values) {
        this.fileName = fileName;
        this.values = List.copyOf(values);
        this.byPath = new HashMap<>();
        for (final ConfigValue<?> value : this.values) {
            this.byPath.put(value.path(), value);
        }
    }

    /** Reads the file when present, then writes it back so new keys and clamped values are visible. */
    public void load() {
        final Path path = FabricLoader.getInstance().getConfigDir().resolve(this.fileName);
        if (Files.exists(path)) {
            try {
                parse(Files.readAllLines(path, StandardCharsets.UTF_8));
            } catch (final IOException | RuntimeException e) {
                LOGGER.warn("Could not read {}, keeping defaults", path, e);
            }
        }
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, render(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            LOGGER.warn("Could not write {}", path, e);
        }
    }

    private void parse(final List<String> lines) {
        String section = "";
        for (final String raw : lines) {
            final String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                final String name = line.substring(1, line.length() - 1).trim();
                section = name.isEmpty() ? "" : name + ".";
                continue;
            }
            final int equals = line.indexOf('=');
            if (equals < 0) {
                continue;
            }
            final String key = section + line.substring(0, equals).trim();
            final ConfigValue<?> value = this.byPath.get(key);
            if (value == null) {
                LOGGER.debug("Ignoring unknown config key {}", key);
                continue;
            }
            final String text = unquote(line.substring(equals + 1).trim());
            if (!value.parse(text)) {
                LOGGER.warn("Ignoring unusable value for {}: {}", key, text);
            }
        }
    }

    private String render() {
        final StringBuilder out = new StringBuilder();
        String section = null;
        for (final ConfigValue<?> value : this.values) {
            if (!value.section.equals(section)) {
                section = value.section;
                if (out.length() > 0) {
                    out.append('\n');
                }
                out.append('[').append(section).append("]\n");
            }
            for (final String comment : value.comments) {
                out.append("# ").append(comment).append('\n');
            }
            out.append(value.key).append(" = ").append(value.render()).append('\n');
        }
        return out.toString();
    }

    private static String unquote(final String text) {
        if (text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }
    /** A single value. Mirrors {@code ForgeConfigSpec.ConfigValue} down to {@code get()}. */
    public abstract static class ConfigValue<T> implements Supplier<T> {
        final String section;
        final String key;
        final List<String> comments;
        final T defaultValue;
        T value;

        ConfigValue(final String section, final String key, final List<String> comments, final T defaultValue) {
            this.section = section;
            this.key = key;
            this.comments = List.copyOf(comments);
            this.defaultValue = defaultValue;
            this.value = defaultValue;
        }

        @Override
        public T get() {
            return this.value;
        }

        public void set(final T value) {
            this.value = value;
        }

        public T getDefault() {
            return this.defaultValue;
        }

        public String path() {
            return this.section.isEmpty() ? this.key : this.section + "." + this.key;
        }

        /** @return false when the text is unusable, leaving the previous value in place. */
        abstract boolean parse(String text);

        abstract String render();
    }

    public static final class BooleanValue extends ConfigValue<Boolean> {
        BooleanValue(final String section, final String key, final List<String> comments, final boolean defaultValue) {
            super(section, key, comments, defaultValue);
        }

        @Override
        boolean parse(final String text) {
            if ("true".equalsIgnoreCase(text)) {
                this.value = Boolean.TRUE;
            } else if ("false".equalsIgnoreCase(text)) {
                this.value = Boolean.FALSE;
            } else {
                return false;
            }
            return true;
        }

        @Override
        String render() {
            return this.value.toString();
        }
    }
    public static final class IntValue extends ConfigValue<Integer> {
        private final int min;
        private final int max;

        IntValue(final String section, final String key, final List<String> comments,
                 final int defaultValue, final int min, final int max) {
            super(section, key, comments, defaultValue);
            this.min = min;
            this.max = max;
        }

        @Override
        boolean parse(final String text) {
            final int parsed;
            try {
                parsed = Integer.parseInt(text);
            } catch (final NumberFormatException e) {
                return false;
            }
            this.value = Math.max(this.min, Math.min(this.max, parsed));
            return true;
        }

        @Override
        String render() {
            return this.value.toString();
        }
    }

    public static final class DoubleValue extends ConfigValue<Double> {
        private final double min;
        private final double max;

        DoubleValue(final String section, final String key, final List<String> comments,
                    final double defaultValue, final double min, final double max) {
            super(section, key, comments, defaultValue);
            this.min = min;
            this.max = max;
        }

        @Override
        boolean parse(final String text) {
            final double parsed;
            try {
                parsed = Double.parseDouble(text);
            } catch (final NumberFormatException e) {
                return false;
            }
            if (!Double.isFinite(parsed)) {
                return false;
            }
            this.value = Math.max(this.min, Math.min(this.max, parsed));
            return true;
        }

        @Override
        String render() {
            return this.value.toString();
        }
    }
    public static final class EnumValue<E extends Enum<E>> extends ConfigValue<E> {
        private final Class<E> type;

        EnumValue(final String section, final String key, final List<String> comments, final E defaultValue) {
            super(section, key, comments, defaultValue);
            this.type = defaultValue.getDeclaringClass();
        }

        @Override
        boolean parse(final String text) {
            try {
                this.value = Enum.valueOf(this.type, text.trim().toUpperCase(Locale.ROOT));
            } catch (final IllegalArgumentException e) {
                return false;
            }
            return true;
        }

        @Override
        String render() {
            return "\"" + this.value.name() + "\"";
        }
    }

    /** Mirrors {@code ForgeConfigSpec.Builder}: the config classes' bodies are unchanged. */
    public static final class Builder {
        private final List<ConfigValue<?>> values = new ArrayList<>();
        private final List<String> path = new ArrayList<>();
        private final List<String> comments = new ArrayList<>();

        public Builder push(final String name) {
            this.path.add(name);
            return this;
        }

        public Builder pop() {
            if (!this.path.isEmpty()) {
                this.path.remove(this.path.size() - 1);
            }
            return this;
        }

        public Builder comment(final String comment) {
            this.comments.add(comment);
            return this;
        }

        public BooleanValue define(final String key, final boolean defaultValue) {
            return add(new BooleanValue(section(), key, drain(), defaultValue));
        }

        public IntValue defineInRange(final String key, final int defaultValue, final int min, final int max) {
            return add(new IntValue(section(), key, drain(), defaultValue, min, max));
        }

        public DoubleValue defineInRange(final String key, final double defaultValue, final double min, final double max) {
            return add(new DoubleValue(section(), key, drain(), defaultValue, min, max));
        }

        public <E extends Enum<E>> EnumValue<E> defineEnum(final String key, final E defaultValue) {
            return add(new EnumValue<>(section(), key, drain(), defaultValue));
        }

        /** @param fileName name inside the config folder, e.g. {@code fullfud-client.toml} */
        public ConfigSpec build(final String fileName) {
            return new ConfigSpec(fileName, this.values);
        }

        private <V extends ConfigValue<?>> V add(final V value) {
            this.values.add(value);
            return value;
        }

        private String section() {
            return String.join(".", this.path);
        }

        private List<String> drain() {
            final List<String> drained = List.copyOf(this.comments);
            this.comments.clear();
            return drained;
        }
    }
}
