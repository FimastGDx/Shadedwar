package com.fullfud.fullfud.core.registry;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Stand-in for Forge's {@code net.minecraftforge.registries.RegistryObject}.
 *
 * <p>Kept deliberately API-compatible with the handful of methods this mod actually used, so the
 * ~56 {@code FullfudRegistries.X.get()} call sites did not have to change during the Fabric port.
 * Instances are created and bound by {@link DeferredRegister}.
 */
public final class RegistryObject<T> implements Supplier<T> {

    private final ResourceLocation id;
    private T value;
    private Holder<T> holder;

    RegistryObject(final ResourceLocation id) {
        this.id = id;
    }

    void bind(final T value, final Holder<T> holder) {
        this.value = value;
        this.holder = holder;
    }

    @Override
    public T get() {
        return Objects.requireNonNull(this.value, () -> "Registry object " + this.id + " read before registration");
    }

    /**
     * The registry holder, empty until registration has run. {@code DroneExplosionEffects} needs one to
     * build a {@code ClientboundSoundPacket} by hand.
     */
    public Optional<Holder<T>> getHolder() {
        return Optional.ofNullable(this.holder);
    }

    public ResourceLocation getId() {
        return this.id;
    }

    public boolean isPresent() {
        return this.value != null;
    }

    @Override
    public String toString() {
        return "RegistryObject[" + this.id + "]";
    }
}
