package com.fullfud.fullfud.client.screen;

import com.fullfud.fullfud.client.input.ControllerCalibration;
import com.fullfud.fullfud.client.input.FpvControllerInput;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public class ControllerRatesScreen extends Screen {
    private static final int CHART_WIDTH = 170;
    private static final int CHART_HEIGHT = 120;

    private final ControllerCalibrationScreen parentScreen;
    private final ControllerCalibration calibration;

    private FpvHudButton yawButton;
    private FpvHudButton pitchButton;
    private FpvHudButton rollButton;
    private int selectedAxis = ControllerCalibration.AXIS_ROLL;
    private float previewInput;

    public ControllerRatesScreen(
        final ControllerCalibrationScreen parentScreen,
        final ControllerCalibration calibration
    ) {
        super(Component.translatable("screen.fullfud.calibration.rates.title"));
        this.parentScreen = parentScreen;
        this.calibration = calibration;
    }

    @Override
    protected void init() {
        final int left = width / 2 - 170;
        final int top = 42;
        final int sliderWidth = 176;

        yawButton = addRenderableWidget(new FpvHudButton(left, top, 56, 20, Component.translatable("screen.fullfud.calibration.axis.yaw"), () -> selectedAxis = ControllerCalibration.AXIS_YAW));
        pitchButton = addRenderableWidget(new FpvHudButton(left + 60, top, 56, 20, Component.translatable("screen.fullfud.calibration.axis.pitch"), () -> selectedAxis = ControllerCalibration.AXIS_PITCH));
        rollButton = addRenderableWidget(new FpvHudButton(left + 120, top, 56, 20, Component.translatable("screen.fullfud.calibration.axis.roll"), () -> selectedAxis = ControllerCalibration.AXIS_ROLL));

        addRenderableWidget(new RateSlider(
            left, top + 34, sliderWidth, 20,
            "screen.fullfud.calibration.rates.rate",
            0.0F, 2.55F,
            axis -> calibration.getRcRate(axis),
            (axis, value) -> calibration.setRcRate(axis, value)
        ));
        addRenderableWidget(new RateSlider(
            left, top + 60, sliderWidth, 20,
            "screen.fullfud.calibration.rates.super",
            0.0F, 1.0F,
            axis -> calibration.getSuperRate(axis),
            (axis, value) -> calibration.setSuperRate(axis, value)
        ));
        addRenderableWidget(new RateSlider(
            left, top + 86, sliderWidth, 20,
            "screen.fullfud.calibration.rates.expo",
            0.0F, 1.0F,
            axis -> calibration.getExpo(axis),
            (axis, value) -> calibration.setExpo(axis, value)
        ));

        addRenderableWidget(new FpvHudButton(left, height - 54, 84, 20, Component.translatable("screen.fullfud.calibration.rates.slow"), () -> {
            calibration.applySlowPreset();
            parentScreen.persistWorkingCalibration();
            initSelectedAxisButtons();
        }));
        addRenderableWidget(new FpvHudButton(left + 92, height - 54, 84, 20, Component.translatable("screen.fullfud.calibration.rates.fast"), () -> {
            calibration.applyFastPreset();
            parentScreen.persistWorkingCalibration();
            initSelectedAxisButtons();
        }));
        addRenderableWidget(new FpvHudButton(width / 2 - 50, height - 28, 100, 20, Component.translatable("gui.done"), this::closeToParent));

        initSelectedAxisButtons();
    }

    @Override
    public void tick() {
        super.tick();
        initSelectedAxisButtons();
        previewInput = samplePreviewInput();
    }

    private void initSelectedAxisButtons() {
        if (yawButton != null) {
            yawButton.setSelected(selectedAxis == ControllerCalibration.AXIS_YAW);
            pitchButton.setSelected(selectedAxis == ControllerCalibration.AXIS_PITCH);
            rollButton.setSelected(selectedAxis == ControllerCalibration.AXIS_ROLL);
        }
    }

    private float samplePreviewInput() {
        final FpvControllerInput.RawJoystickState rawState = FpvControllerInput.snapshotRawState(calibration);
        if (rawState == null) {
            return 0.0F;
        }
        return switch (selectedAxis) {
            case ControllerCalibration.AXIS_YAW -> calibration.normalizeMappedAxis(rawState.axes(), ControllerCalibration.AXIS_YAW);
            case ControllerCalibration.AXIS_PITCH -> calibration.normalizeMappedAxis(rawState.axes(), ControllerCalibration.AXIS_PITCH);
            default -> calibration.normalizeMappedAxis(rawState.axes(), ControllerCalibration.AXIS_ROLL);
        };
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        FpvHudUi.renderBackdrop(graphics, width, height);
        super.render(graphics, mouseX, mouseY, partialTick);

        final int cx = width / 2;
        final int chartX = width / 2 + 20;
        final int chartY = 64;

        FpvHudUi.renderPanel(graphics, width / 2 - 182, 12, 364, 126);
        FpvHudUi.renderPanel(graphics, chartX - 12, chartY - 24, 200, CHART_HEIGHT + 54);
        graphics.drawCenteredString(font, title, cx, 16, FpvHudUi.TEXT);
        FpvHudUi.drawMutedCentered(graphics, font, Component.translatable("screen.fullfud.calibration.rates.subtitle"), cx, 30);
        graphics.drawString(font, Component.translatable("screen.fullfud.calibration.rates.preview"), chartX, chartY - 12, FpvHudUi.TEXT);
        graphics.drawString(
            font,
            Component.translatable(
                "screen.fullfud.calibration.rates.live_input",
                Component.translatable(axisTitleKey(selectedAxis)),
                String.format(Locale.ROOT, "%.2f", previewInput),
                String.format(Locale.ROOT, "%.0f", calibration.computeRateDegrees(selectedAxis, previewInput))
            ),
            chartX,
            chartY + CHART_HEIGHT + 8,
            FpvHudUi.TEXT_MUTED
        );

        renderRateChart(graphics, chartX, chartY);
    }

    private void renderRateChart(final GuiGraphics graphics, final int x, final int y) {
        graphics.fill(x, y, x + CHART_WIDTH, y + CHART_HEIGHT, 0xCC0E131A);
        graphics.fill(x, y + CHART_HEIGHT / 2, x + CHART_WIDTH, y + CHART_HEIGHT / 2 + 1, FpvHudUi.BORDER);
        graphics.fill(x, y, x + 1, y + CHART_HEIGHT, FpvHudUi.BORDER);
        graphics.fill(x + CHART_WIDTH - 1, y, x + CHART_WIDTH, y + CHART_HEIGHT, FpvHudUi.BORDER);
        graphics.fill(x, y, x + CHART_WIDTH, y + 1, FpvHudUi.BORDER);
        graphics.fill(x, y + CHART_HEIGHT - 1, x + CHART_WIDTH, y + CHART_HEIGHT, FpvHudUi.BORDER);

        final float maxDeg = Math.max(1.0F, calibration.computeRateDegrees(selectedAxis, 1.0F));
        int previousX = x;
        int previousY = mapRateToChartY(y, maxDeg, calibration.computeRateDegrees(selectedAxis, -1.0F));
        for (int i = 1; i <= CHART_WIDTH - 1; i++) {
            final float input = (i / (float) (CHART_WIDTH - 1)) * 2.0F - 1.0F;
            final float rate = calibration.computeRateDegrees(selectedAxis, input);
            final int currentX = x + i;
            final int currentY = mapRateToChartY(y, maxDeg, rate);
            drawLine(graphics, previousX, previousY, currentX, currentY, FpvHudUi.SLIDER_TRACK);
            previousX = currentX;
            previousY = currentY;
        }

        final int markerX = x + Math.round((Mth.clamp(previewInput, -1.0F, 1.0F) + 1.0F) * 0.5F * (CHART_WIDTH - 1));
        final int markerY = mapRateToChartY(y, maxDeg, calibration.computeRateDegrees(selectedAxis, previewInput));
        graphics.fill(markerX - 2, markerY - 2, markerX + 2, markerY + 2, FpvHudUi.TEXT_WARN);
    }

    private int mapRateToChartY(final int chartY, final float maxDeg, final float rate) {
        final float normalized = 1.0F - ((rate / maxDeg) + 1.0F) * 0.5F;
        return chartY + Math.round(Mth.clamp(normalized, 0.0F, 1.0F) * (CHART_HEIGHT - 1));
    }

    private void drawLine(
        final GuiGraphics graphics,
        final int x0,
        final int y0,
        final int x1,
        final int y1,
        final int color
    ) {
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
            case ControllerCalibration.AXIS_YAW -> "screen.fullfud.calibration.axis.yaw";
            case ControllerCalibration.AXIS_PITCH -> "screen.fullfud.calibration.axis.pitch";
            default -> "screen.fullfud.calibration.axis.roll";
        };
    }

    private void closeToParent() {
        parentScreen.persistWorkingCalibration();
        if (minecraft != null) {
            minecraft.setScreen(parentScreen);
        }
    }

    @Override
    public void onClose() {
        closeToParent();
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

    private final class RateSlider extends AbstractSliderButton {
        private final String labelKey;
        private final float minValue;
        private final float maxValue;
        private final AxisFloatGetter getter;
        private final AxisFloatSetter setter;

        private RateSlider(
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
            refreshFromSelectedAxis();
        }

        @Override
        public void renderWidget(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
            refreshFromSelectedAxis();
            FpvHudUi.renderSlider(graphics, getX(), getY(), width, height, isHoveredOrFocused(), active, value, getMessage());
        }

        @Override
        protected void updateMessage() {
            final float currentValue = currentValue();
            setMessage(Component.translatable(
                labelKey,
                String.format(Locale.ROOT, "%.2f", currentValue)
            ));
        }

        @Override
        protected void applyValue() {
            final float currentValue = currentValue();
            setter.set(selectedAxis, currentValue);
            parentScreen.persistWorkingCalibration();
        }

        private void refreshFromSelectedAxis() {
            final float current = getter.get(selectedAxis);
            value = (current - minValue) / (double) (maxValue - minValue);
            value = Mth.clamp(value, 0.0D, 1.0D);
            updateMessage();
        }

        private float currentValue() {
            return minValue + (float) value * (maxValue - minValue);
        }
    }
}
