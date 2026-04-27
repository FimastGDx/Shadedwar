package dev.lazurite.lattice.impl.mixin;

import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class LatticeMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogManager.getLogger("lattice-mixin");
    private static final String COMPATIBILITY_MODE_PROPERTY = "fullfud.compatibilityMode";
    private static final String DISABLE_FIX_MIXINS_PROPERTY = "fullfud.disableLatticeFixMixins";
    private static final String FIX_RENDER_LEVEL_MIXIN =
        "dev.lazurite.lattice.impl.client.mixin.fix.render.LevelRendererMixin";
    private static final String FEATURE_RENDER_LEVEL_MIXIN =
        "dev.lazurite.lattice.impl.client.mixin.feature.render.LevelRendererMixin";
    private static final String COMMON_FIX_PREFIX = "dev.lazurite.lattice.impl.mixin.fix.";
    private static final String CLIENT_FIX_PREFIX = "dev.lazurite.lattice.impl.client.mixin.fix.";

    private boolean embeddiumPresent;
    private boolean disableFixMixins;
    private final Set<String> loggedSkips = new HashSet<>();

    @Override
    public void onLoad(final String mixinPackage) {
        // Embeddium (Sodium) rewrites LevelRenderer; our redirect in fix.render.LevelRendererMixin
        // collides with their mixin. If Embeddium/Oculus is present, skip that mixin.
        embeddiumPresent =
            isModLoaded("embeddium")
                || isModLoaded("oculus")
                || isModLoaded("rubidium")
                || isClassPresent("me.jellysquid.mods.sodium.mixin.core.render.world.WorldRendererMixin");
        disableFixMixins =
            Boolean.getBoolean(COMPATIBILITY_MODE_PROPERTY)
                || Boolean.getBoolean(DISABLE_FIX_MIXINS_PROPERTY);
        if (disableFixMixins) {
            LOGGER.warn("Lattice compatibility mode enabled, fix mixins will be skipped");
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(final String targetClassName, final String mixinClassName) {
        if (disableFixMixins && isFixMixin(mixinClassName)) {
            logSkip(mixinClassName, "compatibility mode");
            return false;
        }
        if (embeddiumPresent && isEmbeddiumSensitiveRenderMixin(mixinClassName)) {
            logSkip(mixinClassName, "Embeddium/Oculus/Rubidium compatibility");
            return false;
        }
        return true;
    }

    @Override
    public void acceptTargets(final Set<String> myTargets, final Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(
        final String targetClassName,
        final ClassNode targetClass,
        final String mixinClassName,
        final IMixinInfo mixinInfo
    ) {
    }

    @Override
    public void postApply(
        final String targetClassName,
        final ClassNode targetClass,
        final String mixinClassName,
        final IMixinInfo mixinInfo
    ) {
    }

    private static boolean isClassPresent(final String className) {
        try {
            Class.forName(className, false, LatticeMixinPlugin.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isModLoaded(final String modId) {
        try {
            final Class<?> modListClass = Class.forName("net.minecraftforge.fml.ModList");
            final Object modList = modListClass.getMethod("get").invoke(null);
            return (boolean) modListClass.getMethod("isLoaded", String.class).invoke(modList, modId);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isFixMixin(final String mixinClassName) {
        return mixinClassName.startsWith(COMMON_FIX_PREFIX) || mixinClassName.startsWith(CLIENT_FIX_PREFIX);
    }

    private static boolean isEmbeddiumSensitiveRenderMixin(final String mixinClassName) {
        return FIX_RENDER_LEVEL_MIXIN.equals(mixinClassName) || FEATURE_RENDER_LEVEL_MIXIN.equals(mixinClassName);
    }

    private void logSkip(final String mixinClassName, final String reason) {
        if (loggedSkips.add(mixinClassName)) {
            LOGGER.warn("Skipping mixin {} due to {}", mixinClassName, reason);
        }
    }
}
