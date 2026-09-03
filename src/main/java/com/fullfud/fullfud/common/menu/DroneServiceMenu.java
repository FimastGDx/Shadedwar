package com.fullfud.fullfud.common.menu;

import com.fullfud.fullfud.common.entity.drone.DroneServiceBay;
import com.fullfud.fullfud.core.FullfudRegistries;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * The drone service bay, opened by right-clicking a drone with a screwdriver.
 *
 * <p>This is the mod's first menu with real slots — the two monitor menus are slotless camera session
 * handles. The container it wraps is the drone's own {@link DroneServiceBay} on the server and a
 * throwaway of matching size on the client, which is the ordinary pattern: vanilla syncs the contents
 * through the menu, so the client copy never needs to know where the real one lives.
 *
 * <p>Slot count varies with the airframe, so it is part of the opening payload rather than a constant.
 */
public class DroneServiceMenu extends AbstractContainerMenu {

    /** Opening payload. {@code cargoSlots} decides the slot layout, so client and server must agree on it. */
    public record Data(UUID droneId, int cargoSlots, int maxWarheadTier, boolean fpv) {
        public static final StreamCodec<io.netty.buffer.ByteBuf, Data> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, Data::droneId,
            ByteBufCodecs.VAR_INT, Data::cargoSlots,
            ByteBufCodecs.VAR_INT, Data::maxWarheadTier,
            ByteBufCodecs.BOOL, Data::fpv,
            Data::new
        );
    }

    private static final int POWER_SLOT_X = 44;
    private static final int WARHEAD_SLOT_X = 114;
    private static final int BAY_SLOT_Y = 22;
    private static final int CARGO_ROW_Y = 48;

    private final Container bay;
    private final UUID droneId;
    private final int cargoSlots;
    private final int maxWarheadTier;
    private final boolean fpv;

    /** Client-side constructor: the bay is a stand-in of the right size, filled in by the sync. */
    public DroneServiceMenu(final int containerId, final Inventory inventory, final Data data) {
        this(
            containerId,
            inventory,
            new SimpleContainer(DroneServiceBay.CARGO_START + (data == null ? 0 : data.cargoSlots())),
            data == null ? null : data.droneId(),
            data == null ? 0 : data.cargoSlots(),
            data == null ? 0 : data.maxWarheadTier(),
            data != null && data.fpv()
        );
    }

    public DroneServiceMenu(
        final int containerId,
        final Inventory inventory,
        final Container bay,
        final UUID droneId,
        final int cargoSlots,
        final int maxWarheadTier,
        final boolean fpv
    ) {
        super(FullfudRegistries.DRONE_SERVICE_MENU.get(), containerId);
        this.bay = bay;
        this.droneId = droneId;
        this.cargoSlots = Math.max(0, cargoSlots);
        this.maxWarheadTier = maxWarheadTier;
        this.fpv = fpv;

        addSlot(new BaySlot(bay, DroneServiceBay.SLOT_POWER, POWER_SLOT_X, BAY_SLOT_Y));
        addSlot(new BaySlot(bay, DroneServiceBay.SLOT_WARHEAD, WARHEAD_SLOT_X, BAY_SLOT_Y));
        for (int index = 0; index < this.cargoSlots; index++) {
            addSlot(new Slot(bay, DroneServiceBay.CARGO_START + index, 8 + index * 18, CARGO_ROW_Y));
        }

        final int inventoryTop = playerInventoryTop(this.cargoSlots);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + row * 9 + column, 8 + column * 18, inventoryTop + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, inventoryTop + 58));
        }
    }

    /** The cargo row is only drawn when there is one, so everything below it moves up when there is not. */
    public static int playerInventoryTop(final int cargoSlots) {
        return cargoSlots > 0 ? 80 : 62;
    }

    public static int screenHeight(final int cargoSlots) {
        return playerInventoryTop(cargoSlots) + 82;
    }

    /**
     * Opens the bay for a player. Both drone entities call this from their {@code interact} when the held
     * item is a screwdriver, so the payload is built in exactly one place and client and server cannot
     * disagree about the slot count.
     */
    public static boolean open(
        final ServerPlayer player,
        final Entity drone,
        final DroneServiceBay bay,
        final boolean fpv
    ) {
        if (player == null || drone == null || !drone.isAlive()) {
            return false;
        }
        final Data data = new Data(drone.getUUID(), bay.cargoSlots(), bay.maxWarheadTier(), fpv);
        player.openMenu(new ExtendedScreenHandlerFactory<Data>() {
            @Override
            public Component getDisplayName() {
                return drone.getName();
            }

            @Override
            public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player ply) {
                return new DroneServiceMenu(
                    containerId,
                    inventory,
                    bay,
                    drone.getUUID(),
                    bay.cargoSlots(),
                    bay.maxWarheadTier(),
                    fpv
                );
            }

            @Override
            public Data getScreenOpeningData(final ServerPlayer serverPlayer) {
                return data;
            }
        });
        return true;
    }

    public UUID getDroneId() {
        return this.droneId;
    }

    public int getCargoSlots() {
        return this.cargoSlots;
    }

    public int getMaxWarheadTier() {
        return this.maxWarheadTier;
    }

    public boolean isFpv() {
        return this.fpv;
    }

    private int bayEnd() {
        return DroneServiceBay.CARGO_START + this.cargoSlots;
    }

    @Override
    public boolean stillValid(final Player player) {
        return this.bay.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        final Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();
        final int bayEnd = bayEnd();
        if (index < bayEnd) {
            if (!moveItemStackTo(stack, bayEnd, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, bayEnd, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    /** Honours {@link DroneServiceBay#canPlaceItem}, so a tier-4 charge cannot be shoved into an FPV. */
    private static final class BaySlot extends Slot {
        private BaySlot(final Container container, final int index, final int x, final int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(final ItemStack stack) {
            return this.container.canPlaceItem(this.index, stack);
        }

        @Override
        public int getMaxStackSize() {
            return this.index == DroneServiceBay.SLOT_WARHEAD ? 1 : super.getMaxStackSize();
        }
    }
}
