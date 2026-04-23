package com.fullfud.fullfud.client.screen;

import com.fullfud.fullfud.common.entity.drone.FpvDroneConfig;
import com.fullfud.fullfud.core.network.FullfudNetwork;
import com.fullfud.fullfud.core.network.packet.UpdateFpvDroneConfigPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Locale;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class FpvConfiguratorScreen extends Screen {
    private static final int CHART_WIDTH = 170;
    private static final int CHART_HEIGHT = 120;

    private final UUID droneId;
    private final FpvDroneConfig workingConfig;

    private FpvHudButton yawButton;
    private FpvHudButton pitchButton;
    private FpvHudButton rollButton;
    private FpvHudButton mode3dButton;
    private EditBox droneNameField;
    private int selectedAxis = FpvDroneConfig.CHANNEL_ROLL;
    private boolean dirty;

    public FpvConfiguratorScreen(final UUID droneId, final FpvDroneConfig config) {
        super(Component.translatable("screen.fullfud.fpv_configurator.title"));
        this.droneId = droneId;
        this.workingConfig = config == null ? FpvDroneConfig.defaults() : config.copy();
    }

    @Override
    protected void init() {
        final int left = width / 2 - 180;
        final int top = 48;
        final int right = left + 200;

        yawButton = addRenderableWidget(new FpvHudButton(left, top, 56, 20, Component.translatable("screen.fullfud.fpv_configurator.rates.yaw"), () -> selectedAxis = FpvDroneConfig.CHANNEL_YAW));
        pitchButton = addRenderableWidget(new FpvHudButton(left + 60, top, 56, 20, Component.translatable("screen.fullfud.fpv_configurator.rates.pitch"), () -> selectedAxis = FpvDroneConfig.CHANNEL_PITCH));
        rollButton = addRenderableWidget(new FpvHudButton(left + 120, top, 56, 20, Component.translatable("screen.fullfud.fpv_configurator.rates.roll"), () -> selectedAxis = FpvDroneConfig.CHANNEL_ROLL));

        addRenderableWidget(new AxisSlider(
            left, top + 34, 176, 20,
            "screen.fullfud.fpv_configurator.rate",
            0.0F, 2.55F,
            workingConfig::getRcRate,
            workingConfig::setRcRate
        ));
        addRenderableWidget(new AxisSlider(
            left, top + 60, 176, 20,
            "screen.fullfud.fpv_configurator.super",
            0.0F, 1.0F,
            workingConfig::getSuperRate,
            workingConfig::setSuperRate
        ));
        addRenderableWidget(new AxisSlider(
            left, top + 86, 176, 20,
            "screen.fullfud.fpv_configurator.expo",
            0.0F, 1.0F,
            workingConfig::getExpo,
            workingConfig::setExpo
        ));

        addRenderableWidget(new PhysicsSlider(
            right, top + 14, 176, 20,
            "screen.fullfud.fpv_configurator.motor_kv",
            500.0F, 30000.0F,
            workingConfig::getMotorKv,
            workingConfig::setMotorKv
        ));
        addRenderableWidget(new PhysicsSlider(
            right, top + 40, 176, 20,
            "screen.fullfud.fpv_configurator.prop_diameter",
            1.0F, 12.0F,
            workingConfig::getPropDiameterInch,
            workingConfig::setPropDiameterInch
        ));
        addRenderableWidget(new PhysicsSlider(
            right, top + 66, 176, 20,
            "screen.fullfud.fpv_configurator.prop_pitch",
            0.8F, 8.0F,
            workingConfig::getPropPitchInch,
            workingConfig::setPropPitchInch
        ));
        addRenderableWidget(new PhysicsSlider(
            right, top + 92, 176, 20,
            "screen.fullfud.fpv_configurator.drag",
            0.5F, 2.0F,
            workingConfig::getDragCoefficient,
            workingConfig::setDragCoefficient
        ));
        addRenderableWidget(new PhysicsSlider(
            right, top + 118, 176, 20,
            "screen.fullfud.fpv_configurator.thrust",
            0.5F, 2.0F,
            workingConfig::getThrustMultiplier,
            workingConfig::setThrustMultiplier
        ));

        mode3dButton = addRenderableWidget(new FpvHudButton(right, top + 146, 176, 20, Component.empty(), () -> {
            workingConfig.setFlightMode3d(!workingConfig.isFlightMode3d());
            dirty = true;
            updateMode3dLabel();
        }));

        droneNameField = addRenderableWidget(new EditBox(font, right + 6, top + 190, 164, 14, Component.translatable("screen.fullfud.fpv_configurator.drone_name")));
        droneNameField.setMaxLength(20);
        droneNameField.setValue(workingConfig.getDroneName());
        droneNameField.setBordered(false);
        droneNameField.setTextColor(FpvHudUi.TEXT);
        droneNameField.setTextColorUneditable(FpvHudUi.TEXT_MUTED);
        droneNameField.setResponder(value -> {
            workingConfig.setDroneName(value);
            dirty = true;
        });

        addRenderableWidget(new FpvHudButton(left, height - 54, 84, 20, Component.translatable("screen.fullfud.fpv_configurator.turn_preset.slow"), () -> {
            workingConfig.applyTurnPresetSlow();
            dirty = true;
        }));
        addRenderableWidget(new FpvHudButton(left + 92, height - 54, 84, 20, Component.translatable("screen.fullfud.fpv_configurator.turn_preset.balanced"), () -> {
            workingConfig.applyTurnPresetBalanced();
            dirty = true;
        }));
        addRenderableWidget(new FpvHudButton(right, height - 54, 84, 20, Component.translatable("screen.fullfud.fpv_configurator.turn_preset.fast"), () -> {
            workingConfig.applyTurnPresetFast();
            dirty = true;
        }));
        addRenderableWidget(new FpvHudButton(right + 92, height - 54, 84, 20, Component.translatable("screen.fullfud.fpv_configurator.turn_preset.extreme"), () -> {
            workingConfig.applyTurnPresetExtreme();
            dirty = true;
        }));
        addRenderableWidget(new FpvHudButton(width / 2 - 50, height - 28, 100, 20, Component.translatable("gui.done"), this::saveAndClose));

        updateMode3dLabel();
        updateAxisButtons();
        setInitialFocus(droneNameField);
    }

    @Override
    public void tick() {
        super.tick();
        updateAxisButtons();
        updateMode3dLabel();
    }

    private void updateAxisButtons() {
        if (yawButton == null) {
            return;
        }
        yawButton.setSelected(selectedAxis == FpvDroneConfig.CHANNEL_YAW);
        pitchButton.setSelected(selectedAxis == FpvDroneConfig.CHANNEL_PITCH);
        rollButton.setSelected(selectedAxis == FpvDroneConfig.CHANNEL_ROLL);
    }

    private void updateMode3dLabel() {
        if (mode3dButton == null) {
            return;
        }
        mode3dButton.setSelected(workingConfig.isFlightMode3d());
        mode3dButton.setMessage(Component.translatable(
            "screen.fullfud.fpv_configurator.flight_mode_3d",
            Component.translatable(
                workingConfig.isFlightMode3d()
                    ? "screen.fullfud.fpv_configurator.state.on"
                    : "screen.fullfud.fpv_configurator.state.off"
            )
        ));
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        FpvHudUi.renderBackdrop(graphics, width, height);
        super.render(graphics, mouseX, mouseY, partialTick);

        final int left = width / 2 - 180;
        final int right = left + 200;
        final int top = 36;
        final int chartX = right;
        final int chartY = 252;

        FpvHudUi.renderPanel(graphics, left - 12, top, 200, 178);
        FpvHudUi.renderPanel(graphics, right - 12, top, 200, 206);
        FpvHudUi.renderPanel(graphics, chartX - 12, chartY - 24, 200, CHART_HEIGHT + 54);
        FpvHudUi.renderPanel(graphics, right, 238, 176, 20);

        graphics.drawCenteredString(font, title, width / 2, 16, FpvHudUi.TEXT);
        FpvHudUi.drawMutedCentered(graphics, font, Component.translatable("screen.fullfud.fpv_configurator.subtitle"), width / 2, 30);

        FpvHudUi.renderSectionHeader(graphics, font, Component.translatable("screen.fullfud.fpv_configurator.rates"), left, 38, 176);
        FpvHudUi.renderSectionHeader(graphics, font, Component.translatable("screen.fullfud.fpv_configurator.physics"), right, 38, 176);
        graphics.drawString(font, Component.translatable("screen.fullfud.fpv_configurator.drone_name"), right, 222, FpvHudUi.TEXT);
        graphics.drawString(font, Component.translatable("screen.fullfud.fpv_configurator.chart"), chartX, chartY - 12, FpvHudUi.TEXT);
        graphics.drawString(font, Component.translatable("screen.fullfud.fpv_configurator.turn_presets"), left, height - 78, FpvHudUi.TEXT_ACCENT);

        renderRateChart(graphics, chartX, chartY);

        graphics.drawString(
            font,
            Component.translatable(
                "screen.fullfud.fpv_configurator.live_input",
                Component.translatable(axisTitleKey(selectedAxis)),
                String.format(Locale.ROOT, "%.0f", FpvDroneConfig.shapeRate(
                    1.0F,
                    workingConfig.getRcRate(selectedAxis),
                    workingConfig.getSuperRate(selectedAxis),
                    workingConfig.getExpo(selectedAxis)
                ))
            ),
            chartX,
            chartY + CHART_HEIGHT + 8,
            FpvHudUi.TEXT_MUTED
        );
    }

    private void renderRateChart(final GuiGraphics graphics, final int x, final int y) {
        graphics.fill(x, y, x + CHART_WIDTH, y + CHART_HEIGHT, 0xCC0E131A);
        graphics.fill(x, y + CHART_HEIGHT / 2, x + CHART_WIDTH, y + CHART_HEIGHT / 2 + 1, FpvHudUi.BORDER);
        graphics.fill(x, y, x + 1, y + CHART_HEIGHT, FpvHudUi.BORDER);
        graphics.fill(x + CHART_WIDTH - 1, y, x + CHART_WIDTH, y + CHART_HEIGHT, FpvHudUi.BORDER);
        graphics.fill(x, y, x + CHART_WIDTH, y + 1, FpvHudUi.BORDER);
        graphics.fill(x, y + CHART_HEIGHT - 1, x + CHART_WIDTH, y + CHART_HEIGHT, FpvHudUi.BORDER);

        final float maxDeg = Math.max(1.0F, FpvDroneConfig.shapeRate(
            1.0F,
            workingConfig.getRcRate(selectedAxis),
            workingConfig.getSuperRate(selectedAxis),
            workingConfig.getExpo(selectedAxis)
        ));
        int previousX = x;
        int previousY = mapRateToChartY(y, maxDeg, FpvDroneConfig.shapeRate(
            -1.0F,
            workingConfig.getRcRate(selectedAxis),
            workingConfig.getSuperRate(selectedAxis),
            workingConfig.getExpo(selectedAxis)
        ));
        for (int i = 1; i <= CHART_WIDTH - 1; i++) {
            final float input = (i / (float) (CHART_WIDTH - 1)) * 2.0F - 1.0F;
            final float rate = FpvDroneConfig.shapeRate(
                input,
                workingConfig.getRcRate(selectedAxis),
                workingConfig.getSuperRate(selectedAxis),
                workingConfig.getExpo(selectedAxis)
            );
            final int currentX = x + i;
            final int currentY = mapRateToChartY(y, maxDeg, rate);
            drawLine(graphics, previousX, previousY, currentX, currentY, FpvHudUi.SLIDER_TRACK);
            previousX = currentX;
            previousY = currentY;
        }
    }

    private int mapRateToChartY(final int chartY, final float maxDeg, final float rate) {
        final float normalized = 1.0F - ((rate / maxDeg) + 1.0F) * 0.5F;
        return chartY + Math.round(Mth.clamp(normalized, 0.0F, 1.0F) * (CHART_HEIGHT - 1));
    }

    private void drawLine(final GuiGraphics graphics, final int x0, final int y0, final int x1, final int y1, final int color) {
        final int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        if (steps <= 0) {
            graphics.fill(x0, y0, x0 + 1, y0 + 1, color);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            final float t = i / (float) steps;
            final int px = Math.round(Mth.lerp(t, x0, x1));
            final int py = Math.round(Mth.lerp(t, y0, y1));
            graphics.fill(px, py, px + 1, py + 1, color);
        }
    }

    private String axisTitleKey(final int axis) {
        return switch (axis) {
            case FpvDroneConfig.CHANNEL_YAW -> "screen.fullfud.fpv_configurator.rates.yaw";
            case FpvDroneConfig.CHANNEL_PITCH -> "screen.fullfud.fpv_configurator.rates.pitch";
            default -> "screen.fullfud.fpv_configurator.rates.roll";
        };
    }

    private void saveAndClose() {
        workingConfig.setDroneName(droneNameField.getValue());
        if (dirty && droneId != null) {
            FullfudNetwork.getChannel().sendToServer(new UpdateFpvDroneConfigPacket(droneId, workingConfig.save()));
        }
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    @Override
    public void onClose() {
        saveAndClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private interface AxisFloatGetter {
        float get(int axis);
    }

    private interface AxisFloatSetter {
        void set(int axis, float value);
    }

    private interface FloatGetter {
        float get();
    }

    private interface FloatSetter {
        void set(float value);
    }

    private final class AxisSlider extends AbstractSliderButton {
        private final String labelKey;
        private final float minValue;
        private final float maxValue;
        private final AxisFloatGetter getter;
        private final AxisFloatSetter setter;

        private AxisSlider(
            final int x,
            final int y,
            final int width,
            final int height,
            final String labelKey,
            final float minValue,
            final float maxValue,
            final AxisFloatGetter getter,
            final AxisFloatSetter setter
        ) {
            super(x, y, width, height, Component.empty(), 0.0D);
            this.labelKey = labelKey;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.getter = getter;
            this.setter = setter;
            refresh();
        }

        @Override
        public void renderWidget(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
            refresh();
            FpvHudUi.renderSlider(graphics, getX(), getY(), width, height, isHoveredOrFocused(), active, value, getMessage());
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(labelKey, String.format(Locale.ROOT, "%.2f", currentValue())));
        }

        @Override
        protected void applyValue() {
            setter.set(selectedAxis, currentValue());
            dirty = true;
        }

        private void refresh() {
            final float current = getter.get(selectedAxis);
            value = Mth.clamp((current - minValue) / (double) (maxValue - minValue), 0.0D, 1.0D);
            updateMessage();
        }

        private float currentValue() {
            return minValue + (float) value * (maxValue - minValue);
        }
    }

    private final class PhysicsSlider extends AbstractSliderButton {
        private final String labelKey;
        private final float minValue;
        private final float maxValue;
        private final FloatGetter getter;
        private final FloatSetter setter;

        private PhysicsSlider(
            final int x,
            final int y,
            final int width,
            final int height,
            final String labelKey,
            final float minValue,
            final float maxValue,
            final FloatGetter getter,
            final FloatSetter setter
        ) {
            super(x, y, width, height, Component.empty(), 0.0D);
            this.labelKey = labelKey;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.getter = getter;
            this.setter = setter;
            refresh();
        }

        @Override
        public void renderWidget(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
            refresh();
            FpvHudUi.renderSlider(graphics, getX(), getY(), width, height, isHoveredOrFocused(), active, value, getMessage());
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(labelKey, String.format(Locale.ROOT, "%.2f", currentValue())));
        }

        @Override
        protected void applyValue() {
            setter.set(currentValue());
            dirty = true;
        }

        private void refresh() {
            final float current = getter.get();
            value = Mth.clamp((current - minValue) / (double) (maxValue - minValue), 0.0D, 1.0D);
            updateMessage();
        }

        private float currentValue() {
            return minValue + (float) value * (maxValue - minValue);
        }
    }
}
