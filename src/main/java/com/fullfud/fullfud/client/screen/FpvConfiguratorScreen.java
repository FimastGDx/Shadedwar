package com.fullfud.fullfud.client.screen;

import com.fullfud.fullfud.common.entity.drone.FpvDroneConfig;
import com.fullfud.fullfud.core.network.FullfudClientNetwork;
import com.fullfud.fullfud.core.network.packet.UpdateFpvDroneConfigPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Locale;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class FpvConfiguratorScreen extends Screen {
    private static final int MAX_CONTENT_WIDTH = 760;
    private static final int SCREEN_MARGIN = 24;
    private static final int PANEL_TOP = 44;
    private static final int PANEL_PADDING = 14;
    private static final int COLUMN_GAP = 16;
    private static final int TOP_PANEL_HEIGHT = 330;
    private static final int RIGHT_PANEL_HEIGHT = 268;
    private static final int PRESET_PANEL_HEIGHT = 54;
    private static final int CHART_HEIGHT = 128;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 8;

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
        final Layout layout = layout();
        final int left = layout.leftInnerX();
        final int right = layout.rightInnerX();
        final int innerWidth = layout.innerWidth();
        final int tabGap = 6;
        final int tabWidth = (innerWidth - tabGap * 2) / 3;
        final int tabY = layout.top() + 32;

        yawButton = addRenderableWidget(new FpvHudButton(left, tabY, tabWidth, ROW_HEIGHT, Component.translatable("screen.fullfud.fpv_configurator.rates.yaw"), () -> selectedAxis = FpvDroneConfig.CHANNEL_YAW));
        pitchButton = addRenderableWidget(new FpvHudButton(left + tabWidth + tabGap, tabY, tabWidth, ROW_HEIGHT, Component.translatable("screen.fullfud.fpv_configurator.rates.pitch"), () -> selectedAxis = FpvDroneConfig.CHANNEL_PITCH));
        rollButton = addRenderableWidget(new FpvHudButton(left + (tabWidth + tabGap) * 2, tabY, innerWidth - (tabWidth + tabGap) * 2, ROW_HEIGHT, Component.translatable("screen.fullfud.fpv_configurator.rates.roll"), () -> selectedAxis = FpvDroneConfig.CHANNEL_ROLL));

        addRenderableWidget(new AxisSlider(
            left, layout.top() + 66, innerWidth, ROW_HEIGHT,
            "screen.fullfud.fpv_configurator.rate",
            0.0F, 2.55F,
            workingConfig::getRcRate,
            workingConfig::setRcRate
        ));
        addRenderableWidget(new AxisSlider(
            left, layout.top() + 66 + ROW_HEIGHT + ROW_GAP, innerWidth, ROW_HEIGHT,
            "screen.fullfud.fpv_configurator.super",
            0.0F, 1.0F,
            workingConfig::getSuperRate,
            workingConfig::setSuperRate
        ));
        addRenderableWidget(new AxisSlider(
            left, layout.top() + 66 + (ROW_HEIGHT + ROW_GAP) * 2, innerWidth, ROW_HEIGHT,
            "screen.fullfud.fpv_configurator.expo",
            0.0F, 1.0F,
            workingConfig::getExpo,
            workingConfig::setExpo
        ));

        addRenderableWidget(new PhysicsSlider(
            right, layout.top() + 36, innerWidth, ROW_HEIGHT,
            "screen.fullfud.fpv_configurator.motor_kv",
            500.0F, 30000.0F,
            workingConfig::getMotorKv,
            workingConfig::setMotorKv
        ));
        addRenderableWidget(new PhysicsSlider(
            right, layout.top() + 36 + ROW_HEIGHT + ROW_GAP, innerWidth, ROW_HEIGHT,
            "screen.fullfud.fpv_configurator.prop_diameter",
            1.0F, 12.0F,
            workingConfig::getPropDiameterInch,
            workingConfig::setPropDiameterInch
        ));
        addRenderableWidget(new PhysicsSlider(
            right, layout.top() + 36 + (ROW_HEIGHT + ROW_GAP) * 2, innerWidth, ROW_HEIGHT,
            "screen.fullfud.fpv_configurator.prop_pitch",
            0.8F, 8.0F,
            workingConfig::getPropPitchInch,
            workingConfig::setPropPitchInch
        ));
        addRenderableWidget(new PhysicsSlider(
            right, layout.top() + 36 + (ROW_HEIGHT + ROW_GAP) * 3, innerWidth, ROW_HEIGHT,
            "screen.fullfud.fpv_configurator.drag",
            0.5F, 2.0F,
            workingConfig::getDragCoefficient,
            workingConfig::setDragCoefficient
        ));
        addRenderableWidget(new PhysicsSlider(
            right, layout.top() + 36 + (ROW_HEIGHT + ROW_GAP) * 4, innerWidth, ROW_HEIGHT,
            "screen.fullfud.fpv_configurator.thrust",
            0.5F, 2.0F,
            workingConfig::getThrustMultiplier,
            workingConfig::setThrustMultiplier
        ));

        mode3dButton = addRenderableWidget(new FpvHudButton(right, layout.top() + 180, innerWidth, ROW_HEIGHT, Component.empty(), () -> {
            workingConfig.setFlightMode3d(!workingConfig.isFlightMode3d());
            dirty = true;
            updateMode3dLabel();
        }));

        droneNameField = addRenderableWidget(new EditBox(font, right + 8, layout.nameFieldY() + 5, innerWidth - 16, 14, Component.translatable("screen.fullfud.fpv_configurator.drone_name")));
        droneNameField.setMaxLength(20);
        droneNameField.setValue(workingConfig.getDroneName());
        droneNameField.setBordered(false);
        droneNameField.setTextColor(FpvHudUi.TEXT);
        droneNameField.setTextColorUneditable(FpvHudUi.TEXT_MUTED);
        droneNameField.setResponder(value -> {
            workingConfig.setDroneName(value);
            dirty = true;
        });

        final int presetX = layout.presetInnerX();
        final int presetY = layout.presetButtonY();
        final int presetWidth = layout.presetButtonWidth();
        final int presetGap = layout.presetGap();
        addRenderableWidget(new FpvHudButton(presetX, presetY, presetWidth, ROW_HEIGHT, Component.translatable("screen.fullfud.fpv_configurator.turn_preset.slow"), () -> {
            workingConfig.applyTurnPresetSlow();
            dirty = true;
        }));
        addRenderableWidget(new FpvHudButton(presetX + presetWidth + presetGap, presetY, presetWidth, ROW_HEIGHT, Component.translatable("screen.fullfud.fpv_configurator.turn_preset.balanced"), () -> {
            workingConfig.applyTurnPresetBalanced();
            dirty = true;
        }));
        addRenderableWidget(new FpvHudButton(presetX + (presetWidth + presetGap) * 2, presetY, presetWidth, ROW_HEIGHT, Component.translatable("screen.fullfud.fpv_configurator.turn_preset.fast"), () -> {
            workingConfig.applyTurnPresetFast();
            dirty = true;
        }));
        addRenderableWidget(new FpvHudButton(presetX + (presetWidth + presetGap) * 3, presetY, layout.lastPresetButtonWidth(), ROW_HEIGHT, Component.translatable("screen.fullfud.fpv_configurator.turn_preset.extreme"), () -> {
            workingConfig.applyTurnPresetExtreme();
            dirty = true;
        }));
        addRenderableWidget(new FpvHudButton(width / 2 - 50, layout.doneY(), 100, ROW_HEIGHT, Component.translatable("gui.done"), this::saveAndClose));

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
        final Layout layout = layout();
        final int left = layout.leftInnerX();
        final int right = layout.rightInnerX();
        final int innerWidth = layout.innerWidth();

        FpvHudUi.renderBackdrop(graphics, width, height);
        FpvHudUi.renderPanel(graphics, layout.leftPanelX(), layout.top(), layout.columnWidth(), layout.leftPanelHeight());
        FpvHudUi.renderPanel(graphics, layout.rightPanelX(), layout.top(), layout.columnWidth(), RIGHT_PANEL_HEIGHT);
        FpvHudUi.renderPanel(graphics, layout.leftPanelX(), layout.presetY(), layout.contentWidth(), PRESET_PANEL_HEIGHT);
        FpvHudUi.renderPanel(graphics, right, layout.nameFieldY(), innerWidth, 22);

        graphics.drawCenteredString(font, title, width / 2, 16, FpvHudUi.TEXT);
        FpvHudUi.drawMutedCentered(graphics, font, Component.translatable("screen.fullfud.fpv_configurator.subtitle"), width / 2, 30);

        FpvHudUi.renderSectionHeader(graphics, font, Component.translatable("screen.fullfud.fpv_configurator.rates"), left, layout.top() + 12, innerWidth);
        FpvHudUi.renderSectionHeader(graphics, font, Component.translatable("screen.fullfud.fpv_configurator.physics"), right, layout.top() + 12, innerWidth);
        FpvHudUi.renderSectionHeader(graphics, font, Component.translatable("screen.fullfud.fpv_configurator.chart"), left, layout.chartHeaderY(), innerWidth);
        graphics.drawString(font, Component.translatable("screen.fullfud.fpv_configurator.drone_name"), right, layout.nameLabelY(), FpvHudUi.TEXT);
        graphics.drawString(font, Component.translatable("screen.fullfud.fpv_configurator.turn_presets"), layout.presetInnerX(), layout.presetY() + 9, FpvHudUi.TEXT_ACCENT);

        renderRateChart(graphics, layout.chartX(), layout.chartY(), layout.chartWidth(), layout.chartHeight());

        final Component liveInput = Component.translatable(
            "screen.fullfud.fpv_configurator.live_input",
            Component.translatable(axisTitleKey(selectedAxis)),
            String.format(Locale.ROOT, "%.0f", FpvDroneConfig.shapeRate(
                1.0F,
                workingConfig.getRcRate(selectedAxis),
                workingConfig.getSuperRate(selectedAxis),
                workingConfig.getExpo(selectedAxis)
            ))
        );
        graphics.drawString(font, FpvHudUi.fitText(font, liveInput, innerWidth), left, layout.chartY() + layout.chartHeight() + 8, FpvHudUi.TEXT_MUTED);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderRateChart(final GuiGraphics graphics, final int x, final int y, final int chartWidth, final int chartHeight) {
        graphics.fill(x, y, x + chartWidth, y + chartHeight, 0xCC0E131A);
        graphics.fill(x, y + chartHeight / 2, x + chartWidth, y + chartHeight / 2 + 1, FpvHudUi.BORDER);
        graphics.fill(x + chartWidth / 2, y, x + chartWidth / 2 + 1, y + chartHeight, 0x3347566A);
        graphics.fill(x, y, x + 1, y + chartHeight, FpvHudUi.BORDER);
        graphics.fill(x + chartWidth - 1, y, x + chartWidth, y + chartHeight, FpvHudUi.BORDER);
        graphics.fill(x, y, x + chartWidth, y + 1, FpvHudUi.BORDER);
        graphics.fill(x, y + chartHeight - 1, x + chartWidth, y + chartHeight, FpvHudUi.BORDER);

        final float maxDeg = Math.max(1.0F, FpvDroneConfig.shapeRate(
            1.0F,
            workingConfig.getRcRate(selectedAxis),
            workingConfig.getSuperRate(selectedAxis),
            workingConfig.getExpo(selectedAxis)
        ));
        int previousX = x;
        int previousY = mapRateToChartY(y, chartHeight, maxDeg, FpvDroneConfig.shapeRate(
            -1.0F,
            workingConfig.getRcRate(selectedAxis),
            workingConfig.getSuperRate(selectedAxis),
            workingConfig.getExpo(selectedAxis)
        ));
        for (int i = 1; i <= chartWidth - 1; i++) {
            final float input = (i / (float) (chartWidth - 1)) * 2.0F - 1.0F;
            final float rate = FpvDroneConfig.shapeRate(
                input,
                workingConfig.getRcRate(selectedAxis),
                workingConfig.getSuperRate(selectedAxis),
                workingConfig.getExpo(selectedAxis)
            );
            final int currentX = x + i;
            final int currentY = mapRateToChartY(y, chartHeight, maxDeg, rate);
            drawLine(graphics, previousX, previousY, currentX, currentY, FpvHudUi.SLIDER_TRACK);
            previousX = currentX;
            previousY = currentY;
        }
    }

    private int mapRateToChartY(final int chartY, final int chartHeight, final float maxDeg, final float rate) {
        final float normalized = 1.0F - ((rate / maxDeg) + 1.0F) * 0.5F;
        return chartY + Math.round(Mth.clamp(normalized, 0.0F, 1.0F) * (chartHeight - 1));
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
            FullfudClientNetwork.sendToServer(new UpdateFpvDroneConfigPacket(droneId, workingConfig.save()));
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

    private Layout layout() {
        final int contentWidth = Math.min(MAX_CONTENT_WIDTH, Math.max(360, width - SCREEN_MARGIN * 2));
        final int columnGap = Math.min(COLUMN_GAP, Math.max(8, contentWidth / 48));
        final int columnWidth = (contentWidth - columnGap) / 2;
        final int leftPanelX = (width - contentWidth) / 2;
        final int rightPanelX = leftPanelX + columnWidth + columnGap;
        final int leftPanelHeight = Math.min(TOP_PANEL_HEIGHT, Math.max(286, height - 140));
        final int chartHeight = Math.min(CHART_HEIGHT, Math.max(84, leftPanelHeight - 202));
        final int topPanelsBottom = PANEL_TOP + Math.max(leftPanelHeight, RIGHT_PANEL_HEIGHT);
        final int presetY = Math.max(topPanelsBottom + 12, height - PRESET_PANEL_HEIGHT - 34);
        final int doneY = Math.min(height - ROW_HEIGHT - 8, presetY + PRESET_PANEL_HEIGHT + 8);
        return new Layout(contentWidth, columnWidth, leftPanelX, rightPanelX, PANEL_TOP, leftPanelHeight, chartHeight, presetY, doneY);
    }

    private record Layout(
        int contentWidth,
        int columnWidth,
        int leftPanelX,
        int rightPanelX,
        int top,
        int leftPanelHeight,
        int chartHeight,
        int presetY,
        int doneY
    ) {
        private int innerWidth() {
            return columnWidth - PANEL_PADDING * 2;
        }

        private int leftInnerX() {
            return leftPanelX + PANEL_PADDING;
        }

        private int rightInnerX() {
            return rightPanelX + PANEL_PADDING;
        }

        private int chartHeaderY() {
            return top + 156;
        }

        private int chartX() {
            return leftInnerX();
        }

        private int chartY() {
            return top + 174;
        }

        private int chartWidth() {
            return innerWidth();
        }

        private int nameLabelY() {
            return top + 214;
        }

        private int nameFieldY() {
            return top + 230;
        }

        private int presetInnerX() {
            return leftPanelX + PANEL_PADDING;
        }

        private int presetInnerWidth() {
            return contentWidth - PANEL_PADDING * 2;
        }

        private int presetGap() {
            return 10;
        }

        private int presetButtonY() {
            return presetY + 25;
        }

        private int presetButtonWidth() {
            return (presetInnerWidth() - presetGap() * 3) / 4;
        }

        private int lastPresetButtonWidth() {
            return presetInnerWidth() - (presetButtonWidth() + presetGap()) * 3;
        }
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
