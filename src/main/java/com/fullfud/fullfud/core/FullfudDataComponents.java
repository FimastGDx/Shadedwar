package com.fullfud.fullfud.core;

import com.fullfud.fullfud.FullfudMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Replaces the raw {@code ItemStack} NBT keys the mod used through 1.20.1.
 *
 * <p>Every device-to-drone pairing used to be a UUID written under a string key
 * ({@code LinkedFpvDrone}, {@code LinkedShahed}, {@code LinkedFp5Flamingo}), and the REB battery kept
 * its remaining charge under {@code ChargeTicks}. 1.21.4 has no {@code ItemStack.getOrCreateTag()},
 * so each of those becomes a component type.
 *
 * <p>The goggles and the controller deliberately get <em>separate</em> types even though they shared
 * the key string {@code LinkedFpvDrone} before: nothing ever reads one through the other's accessor,
 * and separate types make that explicit instead of implied.
 *
 * <p>All four are declared with a stream codec, so the server's value reaches the client with the
 * stack itself. That removes the hand-rolled client-side tag writes' reason to exist, but they are
 * left in place — they only ever mirror what the server already sent.
 */
public final class FullfudDataComponents {

    /** {@code LinkedFpvDrone} on an {@code FpvGogglesItem} stack. */
    public static final DataComponentType<UUID> LINKED_FPV_DRONE_GOGGLES = uuid();

    /** {@code LinkedFpvDrone} on an {@code FpvControllerItem} stack. */
    public static final DataComponentType<UUID> LINKED_FPV_DRONE_CONTROLLER = uuid();

    /** {@code LinkedShahed} on a {@code MonitorItem} stack. Mutually exclusive with {@link #LINKED_FP5}. */
    public static final DataComponentType<UUID> LINKED_SHAHED = uuid();

    /** {@code LinkedFp5Flamingo} on a {@code MonitorItem} stack. Mutually exclusive with {@link #LINKED_SHAHED}. */
    public static final DataComponentType<UUID> LINKED_FP5 = uuid();

    /**
     * Remaining charge of a {@code RebBatteryItem}, in ticks.
     *
     * <p>On 1.20.1 the getter wrote the default back into the stack on every read. A component has a
     * real default, so the read is now pure.
     */
    public static final DataComponentType<Integer> REB_CHARGE_TICKS = DataComponentType.<Integer>builder()
        .persistent(Codec.INT)
        .networkSynchronized(ByteBufCodecs.VAR_INT)
        .build();

    /**
     * Fuel already inside a Shahed's tank, in kilograms, carried by the item form.
     *
     * <p>The rest of a drone's loadout round-trips through the vanilla {@code CONTAINER} component
     * because it is made of stacks. Tank fuel is not a stack — it is a continuous mass the flight model
     * reads — so it needs its own component to survive being picked back up.
     */
    public static final DataComponentType<Double> DRONE_FUEL_KG = DataComponentType.<Double>builder()
        .persistent(Codec.DOUBLE)
        .networkSynchronized(ByteBufCodecs.DOUBLE)
        .build();

    /** Remaining flight time of an FPV's installed pack, in ticks, carried by the item form. */
    public static final DataComponentType<Integer> DRONE_BATTERY_TICKS = DataComponentType.<Integer>builder()
        .persistent(Codec.INT)
        .networkSynchronized(ByteBufCodecs.VAR_INT)
        .build();

    private FullfudDataComponents() {
    }

    private static DataComponentType<UUID> uuid() {
        return DataComponentType.<UUID>builder()
            .persistent(UUIDUtil.CODEC)
            .networkSynchronized(UUIDUtil.STREAM_CODEC)
            .build();
    }

    /** Must run before {@code FullfudRegistries.register()}, which builds the item instances. */
    public static void register() {
        register("linked_fpv_drone_goggles", LINKED_FPV_DRONE_GOGGLES);
        register("linked_fpv_drone_controller", LINKED_FPV_DRONE_CONTROLLER);
        register("linked_shahed", LINKED_SHAHED);
        register("linked_fp5_flamingo", LINKED_FP5);
        register("reb_charge_ticks", REB_CHARGE_TICKS);
        register("drone_fuel_kg", DRONE_FUEL_KG);
        register("drone_battery_ticks", DRONE_BATTERY_TICKS);
    }

    private static void register(final String name, final DataComponentType<?> type) {
        Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, name),
            type
        );
    }
}
