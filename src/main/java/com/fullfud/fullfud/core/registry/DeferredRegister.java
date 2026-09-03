package com.fullfud.fullfud.core.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Stand-in for Forge's {@code net.minecraftforge.registries.DeferredRegister}.
 *
 * <p>Fabric has no deferred registration phase — {@link Registry#register} can be called straight
 * from a {@code ModInitializer}. This class exists only so {@code FullfudRegistries} and
 * {@code FullfudCreativeTabs} keep their original shape, including {@link #getEntries()}, which is
 * what makes every new item show up in the creative tab automatically.
 *
 * <p>Registration order is the declaration order of the static fields, exactly as on Forge.
 */
public final class DeferredRegister<T> {

    private record Pending<T>(RegistryObject<T> object, Supplier<? extends T> supplier) { }

    private final Registry<T> registry;
    private final String namespace;
    private final List<Pending<T>> pending = new ArrayList<>();
    private final Set<ResourceLocation> ids = new HashSet<>();
    private boolean flushed;

    private DeferredRegister(final Registry<T> registry, final String namespace) {
        this.registry = registry;
        this.namespace = namespace;
    }

    public static <T> DeferredRegister<T> create(final Registry<T> registry, final String namespace) {
        return new DeferredRegister<>(registry, namespace);
    }

    @SuppressWarnings("unchecked")
    public <I extends T> RegistryObject<I> register(final String name, final Supplier<? extends I> supplier) {
        if (this.flushed) {
            throw new IllegalStateException("Cannot register " + name + " after " + this.namespace + " has been flushed");
        }
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(this.namespace, name);
        if (!this.ids.add(id)) {
            throw new IllegalArgumentException("Duplicate registration of " + id);
        }
        final RegistryObject<I> object = new RegistryObject<>(id);
        this.pending.add(new Pending<>((RegistryObject<T>) object, supplier));
        return object;
    }

    /** Every entry declared so far, in declaration order. */
    public Collection<RegistryObject<T>> getEntries() {
        final List<RegistryObject<T>> objects = new ArrayList<>(this.pending.size());
        for (final Pending<T> entry : this.pending) {
            objects.add(entry.object());
        }
        return Collections.unmodifiableList(objects);
    }

    /** Performs the actual vanilla registration. Call once, from a mod initializer. */
    public void register() {
        if (this.flushed) {
            return;
        }
        this.flushed = true;
        for (final Pending<T> entry : this.pending) {
            final Holder.Reference<T> holder = Registry.registerForHolder(this.registry, entry.object().getId(), entry.supplier().get());
            entry.object().bind(holder.value(), holder);
        }
    }
}
