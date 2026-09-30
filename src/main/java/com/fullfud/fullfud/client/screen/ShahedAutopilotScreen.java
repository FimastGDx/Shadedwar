package com.fullfud.fullfud.client.screen;

import com.fullfud.fullfud.core.network.FullfudClientNetwork;
import com.fullfud.fullfud.core.network.packet.SetShahedAutopilotPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Locale;
import java.util.UUID;

/**
 * Autopilot configurator for a Shahed. Opened by right-clicking the drone with the FPV configurator item;
 * the server seeds it with the drone's current settings and, on save, receives them back via
 * {@link SetShahedAutopilotPacket} (which it owner-gates and re-resolves by UUID). Modelled on
 * {@link FpvConfiguratorScreen} but kept to plain vanilla widgets.
 */
@Environment(EnvType.CLIENT)
public class ShahedAutopilotScreen extends Screen {
    private static final int ROW_HEIGHT = 20;
    private static final int FIELD_WIDTH = 200;

    // NBT keys, mirroring ShahedDroneEntity's autopilot tag.
    private static final String TAG_AUTO_ENABLED = "AutoEnabled";
    private static final String TAG_AUTO_TX = "AutoTargetX";
    private static final String TAG_AUTO_TY = "AutoTargetY";
    private static final String TAG_AUTO_TZ = "AutoTargetZ";
    private static final String TAG_AUTO_ALT = "AutoCruiseAlt";
    private static final String TAG_AUTO_POWER = "AutoPower";
    private static final String TAG_AUTO_RECT = "AutoRectangular";

    private final UUID droneId;

    private double targetX;
    private double targetY;
    private double targetZ;
    private double cruiseAltitude;
    private float power;
    private boolean enabled;
    private boolean rectangular;

    /** 0 = coordinates/power page, 1 = flight-structure page. */
    private int page;

    private EditBox targetXField;
    private EditBox targetYField;
    private EditBox targetZField;
    private EditBox cruiseField;
    private Button enableButton;

    public ShahedAutopilotScreen(final UUID droneId, final CompoundTag settings) {
        super(Component.translatable("screen.fullfud.shahed_autopilot.title"));
        this.droneId = droneId;
        final CompoundTag tag = settings == null ? new CompoundTag() : settings;
        this.targetX = tag.getDouble(TAG_AUTO_TX);
        this.targetY = tag.getDouble(TAG_AUTO_TY);
        this.targetZ = tag.getDouble(TAG_AUTO_TZ);
        this.cruiseAltitude = tag.contains(TAG_AUTO_ALT) ? tag.getDouble(TAG_AUTO_ALT) : 120.0D;
        this.power = tag.contains(TAG_AUTO_POWER) ? Mth.clamp(tag.getFloat(TAG_AUTO_POWER), 0.0F, 1.0F) : 0.8F;
        this.enabled = tag.getBoolean(TAG_AUTO_ENABLED);
        this.rectangular = tag.getBoolean(TAG_AUTO_RECT);
    }

    @Override
    protected void init() {
        if (page == 0) {
            initCoordinatePage();
        } else {
            initFlightStructurePage();
        }
    }

    private void initCoordinatePage() {
        final int fieldX = width / 2 - FIELD_WIDTH / 2;
        int y = 50;

        targetXField = addField(fieldX, y, formatCoord(targetX));
        y += ROW_HEIGHT + 14;
        targetYField = addField(fieldX, y, formatCoord(targetY));
        y += ROW_HEIGHT + 14;
        targetZField = addField(fieldX, y, formatCoord(targetZ));
        y += ROW_HEIGHT + 14;
        cruiseField = addField(fieldX, y, formatCoord(cruiseAltitude));
        y += ROW_HEIGHT + 14;

        addRenderableWidget(new PowerSlider(fieldX, y, FIELD_WIDTH, ROW_HEIGHT));
        y += ROW_HEIGHT + 10;

        enableButton = addRenderableWidget(Button.builder(enableLabel(), b -> {
            enabled = !enabled;
            b.setMessage(enableLabel());
        }).bounds(fieldX, y, FIELD_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + 6;

        addRenderableWidget(Button.builder(
            Component.translatable("screen.fullfud.shahed_autopilot.flight_path_page"), b -> switchToPage(1))
            .bounds(fieldX, y, FIELD_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + 16;

        addSaveCancelRow(fieldX, y);
    }

    private void initFlightStructurePage() {
        final int fieldX = width / 2 - FIELD_WIDTH / 2;
        int y = 60;

        addRenderableWidget(Button.builder(structureLabel(), b -> {
            rectangular = !rectangular;
            b.setMessage(structureLabel());
        }).bounds(fieldX, y, FIELD_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + 16;

        addRenderableWidget(Button.builder(
            Component.translatable("screen.fullfud.shahed_autopilot.back"), b -> switchToPage(0))
            .bounds(fieldX, y, FIELD_WIDTH, ROW_HEIGHT).build());
        y += ROW_HEIGHT + 16;

        addSaveCancelRow(fieldX, y);
    }

    private void addSaveCancelRow(final int fieldX, final int y) {
        final int halfWidth = FIELD_WIDTH / 2 - 4;
        addRenderableWidget(Button.builder(
            Component.translatable("screen.fullfud.shahed_autopilot.save"), b -> saveAndClose())
            .bounds(fieldX, y, halfWidth, ROW_HEIGHT).build());
        addRenderableWidget(Button.builder(
            Component.translatable("gui.cancel"), b -> onClose())
            .bounds(fieldX + FIELD_WIDTH - halfWidth, y, halfWidth, ROW_HEIGHT).build());
    }

    /** Persist the coordinate edit boxes before tearing widgets down, so a page flip never loses typed values. */
    private void switchToPage(final int target) {
        commitFields();
        page = target;
        rebuildWidgets();
    }

    private void commitFields() {
        if (targetXField != null) {
            targetX = parseOr(targetXField, targetX);
            targetY = parseOr(targetYField, targetY);
            targetZ = parseOr(targetZField, targetZ);
            cruiseAltitude = parseOr(cruiseField, cruiseAltitude);
        }
    }

    private EditBox addField(final int x, final int y, final String value) {
        final EditBox box = new EditBox(font, x, y, FIELD_WIDTH, 16, Component.empty());
        box.setMaxLength(32);
        box.setValue(value);
        return addRenderableWidget(box);
    }

    private static String formatCoord(final double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private Component enableLabel() {
        return Component.translatable(enabled
            ? "screen.fullfud.shahed_autopilot.enabled"
            : "screen.fullfud.shahed_autopilot.disabled");
    }

    private Component structureLabel() {
        return Component.translatable("screen.fullfud.shahed_autopilot.flight_path",
            Component.translatable(rectangular
                ? "screen.fullfud.shahed_autopilot.flight_path.rectangular"
                : "screen.fullfud.shahed_autopilot.flight_path.default"));
    }

    private void saveAndClose() {
        commitFields();

        final CompoundTag tag = new CompoundTag();
        tag.putBoolean(TAG_AUTO_ENABLED, enabled);
        tag.putDouble(TAG_AUTO_TX, targetX);
        tag.putDouble(TAG_AUTO_TY, targetY);
        tag.putDouble(TAG_AUTO_TZ, targetZ);
        tag.putDouble(TAG_AUTO_ALT, cruiseAltitude);
        tag.putFloat(TAG_AUTO_POWER, power);
        tag.putBoolean(TAG_AUTO_RECT, rectangular);
        FullfudClientNetwork.sendToServer(new SetShahedAutopilotPacket(droneId, tag));
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    private static double parseOr(final EditBox box, final double fallback) {
        try {
            return Double.parseDouble(box.getValue().trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);

        final int labelX = width / 2 - FIELD_WIDTH / 2;
        if (page == 0) {
            drawLabel(graphics, "screen.fullfud.shahed_autopilot.target_x", labelX, 40);
            drawLabel(graphics, "screen.fullfud.shahed_autopilot.target_y", labelX, 40 + (ROW_HEIGHT + 14));
            drawLabel(graphics, "screen.fullfud.shahed_autopilot.target_z", labelX, 40 + (ROW_HEIGHT + 14) * 2);
            drawLabel(graphics, "screen.fullfud.shahed_autopilot.cruise_altitude", labelX, 40 + (ROW_HEIGHT + 14) * 3);
        } else {
            drawLabel(graphics, "screen.fullfud.shahed_autopilot.flight_path_hint", labelX, 44);
        }
    }

    private void drawLabel(final GuiGraphics graphics, final String key, final int x, final int y) {
        graphics.drawString(font, Component.translatable(key), x, y, 0xA0A0A0, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private final class PowerSlider extends AbstractSliderButton {
        private PowerSlider(final int x, final int y, final int width, final int height) {
            super(x, y, width, height, Component.empty(), power);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("screen.fullfud.shahed_autopilot.power",
                String.format(Locale.ROOT, "%d", Math.round(power * 100.0F))));
        }

        @Override
        protected void applyValue() {
            power = (float) Mth.clamp(value, 0.0D, 1.0D);
        }
    }
}
