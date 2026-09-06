package com.fullfud.fullfud.common.entity.drone;

import com.fullfud.fullfud.common.item.RebBatteryItem;
import com.fullfud.fullfud.common.item.WarheadItem;
import com.fullfud.fullfud.core.FullfudRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.function.Predicate;

/**
 * The service bay behind a drone's hatch: one power cell, one charge, and however many cargo slots the
 * airframe has.
 *
 * <p>Both drone types own one of these, and it is the single place their loadout lives. That matters
 * for the round trip through the item form: the whole bay goes into the stack's vanilla
 * {@code CONTAINER} component when the drone is picked up, which also gives the item a contents
 * tooltip for free. The only piece of loadout state that is <em>not</em> in here is fuel already in a
 * Shahed's tank, because that is a continuous mass the flight model reads, not a stack.
 *
 * <p>Slot order is fixed so the menu and the entity agree without either passing indices around.
 */
public class DroneServiceBay extends SimpleContainer {
    public static final int SLOT_POWER = 0;
    public static final int SLOT_WARHEAD = 1;
    public static final int CARGO_START = 2;

    /** Cargo airframes carry a single chest row; everything else carries none. */
    public static final int CARGO_SLOTS = 9;

    private final int maxWarheadTier;
    private final Runnable onChanged;
    private final Predicate<Player> validity;

    public DroneServiceBay(
        final int cargoSlots,
        final int maxWarheadTier,
        final Runnable onChanged,
        final Predicate<Player> validity
    ) {
        super(CARGO_START + Math.max(0, cargoSlots));
        this.maxWarheadTier = maxWarheadTier;
        this.onChanged = onChanged;
        this.validity = validity;
    }

    /** Closes the screen once the drone is gone or the player has walked away from it. */
    @Override
    public boolean stillValid(final Player player) {
        return this.validity == null || this.validity.test(player);
    }

    public int cargoSlots() {
        return getContainerSize() - CARGO_START;
    }

    public ItemStack powerStack() {
        return getItem(SLOT_POWER);
    }

    public ItemStack warheadStack() {
        return getItem(SLOT_WARHEAD);
    }

    public WarheadCharge warhead() {
        return WarheadItem.chargeOf(warheadStack());
    }

    /**
     * Whether a slot will take a stack. The menu asks this and so does {@code Container.canPlaceItem},
     * so hoppers and dispensers cannot route around it either.
     */
    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        return switch (slot) {
            case SLOT_POWER -> isPowerItem(stack);
            case SLOT_WARHEAD -> WarheadItem.chargeOf(stack).tier() > 0
                && WarheadItem.chargeOf(stack).tier() <= this.maxWarheadTier;
            default -> slot >= CARGO_START && slot < getContainerSize();
        };
    }

    /** A battery for an FPV, a fuel canister for a Shahed; the bay takes either and the drone picks. */
    private static boolean isPowerItem(final ItemStack stack) {
        return stack.getItem() instanceof RebBatteryItem
            || stack.is(FullfudRegistries.SHAHED_FUEL_ITEM.get());
    }

    public int maxWarheadTier() {
        return this.maxWarheadTier;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.onChanged != null) {
            this.onChanged.run();
        }
    }

    public ListTag save(final HolderLookup.Provider registries) {
        return createTag(registries);
    }

    public void load(final ListTag tag, final HolderLookup.Provider registries) {
        clearContent();
        fromTag(tag, registries);
    }

    /** Snapshot for the item form. Slot order is preserved, so a restore puts everything back where it was. */
    public ItemContainerContents toComponent() {
        return ItemContainerContents.fromItems(getItems());
    }

    public void fromComponent(final ItemContainerContents contents) {
        final NonNullList<ItemStack> target = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        contents.copyInto(target);
        for (int slot = 0; slot < getContainerSize(); slot++) {
            setItem(slot, target.get(slot));
        }
    }

    /**
     * Stores the bay on a drone's item form. Uses the vanilla component rather than one of our own, so
     * the stack picks up the contents tooltip and the shulker-box-style "drop the contents on death"
     * behaviour for free.
     */
    public void writeToStack(final ItemStack stack) {
        if (isEmpty()) {
            stack.remove(DataComponents.CONTAINER);
        } else {
            stack.set(DataComponents.CONTAINER, toComponent());
        }
    }

    public void readFromStack(final ItemStack stack) {
        final ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents != null) {
            fromComponent(contents);
        }
    }
}
