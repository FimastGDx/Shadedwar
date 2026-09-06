package com.fullfud.fullfud.client.screen;

import com.fullfud.fullfud.common.entity.drone.WarheadCharge;
import com.fullfud.fullfud.common.menu.DroneServiceMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The screwdriver-opened loading bay.
 *
 * <p>Drawn from filled rectangles rather than a background texture: the panel has to grow by a row when
 * the airframe is a hauler, and the two labelled bay slots sit at their own coordinates, so a fixed
 * 176×166 sheet would only ever fit one of the two layouts. The colours are vanilla's container greys so
 * it still reads as an inventory.
 */
@Environment(EnvType.CLIENT)
public class DroneServiceScreen extends AbstractContainerScreen<DroneServiceMenu> {

    private static final int PANEL_FILL = 0xFFC6C6C6;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int PANEL_DARK = 0xFF555555;
    private static final int SLOT_FILL = 0xFF8B8B8B;
    private static final int SLOT_LIGHT = 0xFFFFFFFF;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int LABEL_COLOR = 0xFF404040;

    public DroneServiceScreen(final DroneServiceMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = DroneServiceMenu.screenHeight(menu.getCargoSlots());
        this.inventoryLabelY = DroneServiceMenu.playerInventoryTop(menu.getCargoSlots()) - 12;
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partialTick, final int mouseX, final int mouseY) {
        panel(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
        for (final var slot : this.menu.slots) {
            slotWell(graphics, this.leftPos + slot.x, this.topPos + slot.y);
        }
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        // Above the two bay slots, so an empty bay still says what goes where. An FPV only takes tier 2, and
        // the cap rides in the label rather than in a line of its own because the panel has no spare row.
        graphics.drawString(this.font, Component.translatable("container.fullfud.drone_service.power"),
            30, 12, LABEL_COLOR, false);
        final Component warheadLabel = this.menu.getMaxWarheadTier() > 0
            && this.menu.getMaxWarheadTier() < WarheadCharge.SHAHED_MAX.tier()
            ? Component.translatable("container.fullfud.drone_service.warhead_capped", this.menu.getMaxWarheadTier())
            : Component.translatable("container.fullfud.drone_service.warhead");
        graphics.drawString(this.font, warheadLabel, 100, 12, LABEL_COLOR, false);
        if (this.menu.getCargoSlots() > 0) {
            graphics.drawString(this.font, Component.translatable("container.fullfud.drone_service.cargo"),
                8, 38, LABEL_COLOR, false);
        }
    }

    /** A vanilla-looking bevelled panel: light top-left edge, dark bottom-right edge. */
    private static void panel(final GuiGraphics graphics, final int x, final int y, final int width, final int height) {
        graphics.fill(x, y, x + width, y + height, PANEL_FILL);
        graphics.fill(x, y, x + width, y + 1, PANEL_LIGHT);
        graphics.fill(x, y, x + 1, y + height, PANEL_LIGHT);
        graphics.fill(x, y + height - 1, x + width, y + height, PANEL_DARK);
        graphics.fill(x + width - 1, y, x + width, y + height, PANEL_DARK);
    }

    /** The 18×18 recess behind a 16×16 slot, bevelled the other way round. */
    private static void slotWell(final GuiGraphics graphics, final int slotX, final int slotY) {
        final int x = slotX - 1;
        final int y = slotY - 1;
        graphics.fill(x, y, x + 18, y + 18, SLOT_FILL);
        graphics.fill(x, y, x + 18, y + 1, SLOT_DARK);
        graphics.fill(x, y, x + 1, y + 18, SLOT_DARK);
        graphics.fill(x, y + 17, x + 18, y + 18, SLOT_LIGHT);
        graphics.fill(x + 17, y, x + 18, y + 18, SLOT_LIGHT);
    }
}
