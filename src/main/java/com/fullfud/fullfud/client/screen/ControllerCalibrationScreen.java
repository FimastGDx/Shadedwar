package com.fullfud.fullfud.client.screen;

import com.fullfud.fullfud.client.input.ControllerCalibration;
import com.fullfud.fullfud.client.input.ControllerCalibrationStore;
import com.fullfud.fullfud.client.input.FpvControllerInput;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ControllerCalibrationScreen extends Screen {
    private static final Component TITLE = Component.translatable("screen.fullfud.calibration.title");
    private static final int BAR_WIDTH = 120;
    private static final int BAR_HEIGHT = 8;
    private static final float ASSIGN_THRESHOLD = 0.3F;
    private static final int ROW_COUNT = 5;
    private static final int ARM_ROW = 4;

    private final ControllerCalibration targetCalibration;
    private final ControllerCalibration workingCalibration;

    private FpvHudButton controllerButton;
    private FpvHudButton stickWizardButton;
    private FpvHudButton armWizardButton;
    private FpvHudButton ratesButton;
    private FpvHudButton rangeButton;
    private FpvHudButton saveButton;
    private FpvHudButton cancelButton;
    private final FpvHudButton[] assignButtons = new FpvHudButton[ROW_COUNT];
    private final FpvHudButton[] invertButtons = new FpvHudButton[ROW_COUNT];

    private float[] baselineAxes = new float[0];
    private byte[] baselineButtons = new byte[0];
    private float[] liveAxes = new float[0];
    private byte[] liveButtons = new byte[0];
    private String liveControllerName = "";
    private int activeAssignRow = -1;

    public ControllerCalibrationScreen(final ControllerCalibration calibration) {
        super(TITLE);
        this.targetCalibration = calibration;
        this.workingCalibration = calibration.copy();
    }

    @Override
    protected void init() {
        final int left = width / 2 - 155;
        final int top = 46;

        controllerButton = addRenderableWidget(new FpvHudButton(left, top, 310, 20, Component.empty(), this::openControllerSelection));

        stickWizardButton = addRenderableWidget(new FpvHudButton(
            left, top + 24, 152, 20,
            Component.translatable("screen.fullfud.calibration.wizard.sticks"),
            this::openStickWizard
        ));
        armWizardButton = addRenderableWidget(new FpvHudButton(
            left + 158, top + 24, 152, 20,
            Component.translatable("screen.fullfud.calibration.wizard.arm"),
            this::openArmWizard
        ));
        ratesButton = addRenderableWidget(new FpvHudButton(
            left + 105, height - 54, 100, 20,
            Component.translatable("screen.fullfud.calibration.rates.button"),
            this::openRatesScreen
        ));

        for (int row = 0; row < ROW_COUNT; row++) {
            final int y = top + 68 + row * 26;
            final int currentRow = row;
            assignButtons[row] = addRenderableWidget(new FpvHudButton(left + 140, y, 90, 20, Component.empty(), () -> toggleAssignment(currentRow)));
            invertButtons[row] = addRenderableWidget(new FpvHudButton(left + 236, y, 74, 20, Component.empty(), () -> toggleInversion(currentRow)));
        }

        rangeButton = addRenderableWidget(new FpvHudButton(left, height - 54, 100, 20, Component.empty(), this::toggleRangeCalibration));
        saveButton = addRenderableWidget(new FpvHudButton(width / 2 - 75, height - 28, 150, 20, Component.translatable("screen.fullfud.calibration.save"), this::saveAndClose));
        cancelButton = addRenderableWidget(new FpvHudButton(left + 210, height - 54, 100, 20, Component.translatable("screen.fullfud.calibration.cancel"), this::onClose));

        refreshLiveState();
        refreshButtons();
    }

    @Override
    public void tick() {
        super.tick();
        refreshLiveState();

        if (workingCalibration.isSampling()) {
            workingCalibration.sampleAxes(liveAxes);
        } else if (activeAssignRow >= 0) {
            if (activeAssignRow == ARM_ROW) {
                detectArmAssignment();
            } else {
                detectAxisAssignment(activeAssignRow);
            }
        }

        refreshButtons();
    }

    public void handleControllerChosen(final FpvControllerInput.ConnectedController controller) {
        if (controller == null) {
            return;
        }
        workingCalibration.setControllerName(controller.name());
        beginListening();
        persistWorkingCalibration();
        if (minecraft != null) {
            minecraft.setScreen(new ControllerStickWizardScreen(this, workingCalibration));
        }
    }

    public String getSelectedControllerName() {
        return workingCalibration.getControllerName();
    }

    public void persistWorkingCalibration() {
        targetCalibration.copyFrom(workingCalibration);
        ControllerCalibrationStore.save(targetCalibration);
    }

    private void refreshLiveState() {
        final FpvControllerInput.RawJoystickState rawState = FpvControllerInput.snapshotRawState(workingCalibration);
        if (rawState == null) {
            liveAxes = new float[0];
            liveButtons = new byte[0];
            liveControllerName = "";
            return;
        }

        liveAxes = rawState.axes();
        liveButtons = rawState.buttons();
        liveControllerName = rawState.name();
        if (workingCalibration.getControllerName().isBlank()) {
            workingCalibration.setControllerName(liveControllerName);
        }
    }

    private void openControllerSelection() {
        if (minecraft == null) {
            return;
        }
        minecraft.setScreen(new ControllerSelectScreen(this));
    }

    public void openStickWizard() {
        if (minecraft == null || !hasControllerInput()) {
            return;
        }
        minecraft.setScreen(new ControllerStickWizardScreen(this, workingCalibration));
    }

    public void openArmWizard() {
        if (minecraft == null || !hasControllerInput()) {
            return;
        }
        minecraft.setScreen(new ControllerArmWizardScreen(this, workingCalibration));
    }

    public void openRatesScreen() {
        if (minecraft == null) {
            return;
        }
        minecraft.setScreen(new ControllerRatesScreen(this, workingCalibration));
    }

    private void toggleAssignment(final int row) {
        if (!hasControllerInput()) {
            return;
        }
        if (workingCalibration.isSampling()) {
            return;
        }
        if (activeAssignRow == row) {
            activeAssignRow = -1;
            return;
        }
        activeAssignRow = row;
        beginListening();
    }

    private void detectAxisAssignment(final int logicalAxis) {
        final int axisIndex = detectMovedAxis();
        if (axisIndex < 0) {
            return;
        }
        final float delta = deltaForAxis(axisIndex);
        workingCalibration.setAxisMapping(logicalAxis, axisIndex);
        workingCalibration.setAxisInverted(logicalAxis, delta < 0.0F);
        activeAssignRow = -1;
        persistWorkingCalibration();
    }

    private void detectArmAssignment() {
        final int buttonIndex = detectPressedButton();
        if (buttonIndex >= 0) {
            workingCalibration.setArmBinding(buttonIndex, false);
            activeAssignRow = -1;
            persistWorkingCalibration();
            return;
        }

        final int axisIndex = detectMovedAxis();
        if (axisIndex < 0) {
            return;
        }
        final float delta = deltaForAxis(axisIndex);
        workingCalibration.setArmBinding(-axisIndex - 1, delta < 0.0F);
        activeAssignRow = -1;
        persistWorkingCalibration();
    }

    private void toggleInversion(final int row) {
        if (row == ARM_ROW) {
            if (!workingCalibration.hasArmBinding()) {
                return;
            }
            workingCalibration.setArmBinding(workingCalibration.getArmBinding(), !workingCalibration.isArmInverted());
            persistWorkingCalibration();
            return;
        }

        if (workingCalibration.getAxisMapping(row) < 0) {
            return;
        }
        workingCalibration.setAxisInverted(row, !workingCalibration.isAxisInverted(row));
        persistWorkingCalibration();
    }

    private void toggleRangeCalibration() {
        if (!hasControllerInput() || !workingCalibration.hasCompleteAxisMapping()) {
            return;
        }

        if (workingCalibration.isSampling()) {
            workingCalibration.finishRangeSampling();
            persistWorkingCalibration();
            return;
        }

        workingCalibration.startRangeSampling(liveAxes.length, liveAxes);
        activeAssignRow = -1;
    }

    private void saveAndClose() {
        if (workingCalibration.isSampling()) {
            workingCalibration.finishRangeSampling();
        }
        persistWorkingCalibration();
        onClose();
    }

    private void beginListening() {
        final FpvControllerInput.RawJoystickState rawState = FpvControllerInput.snapshotRawState(workingCalibration);
        if (rawState == null) {
            baselineAxes = new float[0];
            baselineButtons = new byte[0];
            return;
        }
        baselineAxes = rawState.axes().clone();
        baselineButtons = rawState.buttons().clone();
    }

    private int detectMovedAxis() {
        int bestAxis = -1;
        float bestDelta = ASSIGN_THRESHOLD;
        final int count = Math.min(liveAxes.length, baselineAxes.length);
        for (int i = 0; i < count; i++) {
            final float delta = Math.abs(deltaForAxis(i));
            if (delta > bestDelta) {
                bestDelta = delta;
                bestAxis = i;
            }
        }
        return bestAxis;
    }

    private int detectPressedButton() {
        final int count = Math.min(liveButtons.length, baselineButtons.length);
        for (int i = 0; i < count; i++) {
            if (liveButtons[i] != baselineButtons[i] && liveButtons[i] != 0) {
                return i;
            }
        }
        return -1;
    }

    private float deltaForAxis(final int axisIndex) {
        final float current = axisIndex >= 0 && axisIndex < liveAxes.length && Float.isFinite(liveAxes[axisIndex]) ? liveAxes[axisIndex] : 0.0F;
        final float baseline = axisIndex >= 0 && axisIndex < baselineAxes.length && Float.isFinite(baselineAxes[axisIndex]) ? baselineAxes[axisIndex] : 0.0F;
        return current - baseline;
    }

    private boolean hasControllerInput() {
        return liveAxes.length > 0 && !workingCalibration.getControllerName().isBlank();
    }

    private void refreshButtons() {
        if (controllerButton == null) {
            return;
        }

        controllerButton.setMessage(Component.translatable(
            "screen.fullfud.calibration.controller.button",
            workingCalibration.getControllerName().isBlank() ? "-" : workingCalibration.getControllerName()
        ));
        stickWizardButton.active = hasControllerInput() && !workingCalibration.isSampling();
        armWizardButton.active = hasControllerInput() && !workingCalibration.isSampling();

        rangeButton.setMessage(Component.translatable(
            workingCalibration.isSampling()
                ? "screen.fullfud.calibration.range.finish"
                : "screen.fullfud.calibration.range.start"
        ));
        rangeButton.active = hasControllerInput() && workingCalibration.hasCompleteAxisMapping();
        ratesButton.active = !workingCalibration.isSampling();

        for (int row = 0; row < ROW_COUNT; row++) {
            final FpvHudButton assignButton = assignButtons[row];
            final FpvHudButton invertButton = invertButtons[row];
            assignButton.setMessage(bindingLabel(row));
            assignButton.active = hasControllerInput() && !workingCalibration.isSampling();
            assignButton.setSelected(activeAssignRow == row);

            invertButton.setMessage(invertLabel(row));
            invertButton.active = row == ARM_ROW
                ? workingCalibration.hasArmBinding() && !workingCalibration.isSampling()
                : workingCalibration.getAxisMapping(row) >= 0 && !workingCalibration.isSampling();
        }

        saveButton.active = workingCalibration.hasCompleteAxisMapping() && workingCalibration.hasArmBinding();
    }

    private Component bindingLabel(final int row) {
        final boolean active = activeAssignRow == row;
        final String key = active
            ? "screen.fullfud.calibration.assign.waiting"
            : "screen.fullfud.calibration.assign.button";
        return Component.translatable(key, describeBinding(row));
    }

    private Component invertLabel(final int row) {
        final boolean inverted = row == ARM_ROW ? workingCalibration.isArmInverted() : workingCalibration.isAxisInverted(row);
        return Component.translatable(
            "screen.fullfud.calibration.invert.button",
            shortRowName(row),
            Component.translatable(
                inverted
                    ? "screen.fullfud.calibration.invert.on.short"
                    : "screen.fullfud.calibration.invert.off.short"
            )
        );
    }

    private Component describeBinding(final int row) {
        if (row == ARM_ROW) {
            if (!workingCalibration.hasArmBinding()) {
                return Component.translatable("screen.fullfud.calibration.binding.none");
            }
            return Component.literal(FpvControllerInput.describeArmBinding(
                workingCalibration,
                FpvControllerInput.findJoystickId(workingCalibration)
            ));
        }

        final int mapping = workingCalibration.getAxisMapping(row);
        if (mapping < 0) {
            return Component.translatable("screen.fullfud.calibration.binding.none");
        }
        return Component.literal(FpvControllerInput.formatChannelLabel(mapping));
    }

    private Component currentInstruction() {
        if (workingCalibration.isSampling()) {
            return Component.translatable("screen.fullfud.calibration.range.instruction");
        }
        return switch (activeAssignRow) {
            case ControllerCalibration.AXIS_THROTTLE -> Component.translatable("screen.fullfud.calibration.wait_throttle");
            case ControllerCalibration.AXIS_YAW -> Component.translatable("screen.fullfud.calibration.wait_yaw");
            case ControllerCalibration.AXIS_PITCH -> Component.translatable("screen.fullfud.calibration.wait_pitch");
            case ControllerCalibration.AXIS_ROLL -> Component.translatable("screen.fullfud.calibration.wait_roll");
            case ARM_ROW -> Component.translatable("screen.fullfud.calibration.wait_arm");
            default -> Component.translatable("screen.fullfud.calibration.instruction_main");
        };
    }

    private static Component rowName(final int row) {
        return switch (row) {
            case ControllerCalibration.AXIS_ROLL -> Component.translatable("screen.fullfud.calibration.axis.roll");
            case ControllerCalibration.AXIS_PITCH -> Component.translatable("screen.fullfud.calibration.axis.pitch");
            case ControllerCalibration.AXIS_YAW -> Component.translatable("screen.fullfud.calibration.axis.yaw");
            case ControllerCalibration.AXIS_THROTTLE -> Component.translatable("screen.fullfud.calibration.axis.throttle");
            default -> Component.translatable("screen.fullfud.calibration.axis.arm");
        };
    }

    private static Component shortRowName(final int row) {
        return switch (row) {
            case ControllerCalibration.AXIS_ROLL -> Component.translatable("screen.fullfud.calibration.axis.roll.short");
            case ControllerCalibration.AXIS_PITCH -> Component.translatable("screen.fullfud.calibration.axis.pitch.short");
            case ControllerCalibration.AXIS_YAW -> Component.translatable("screen.fullfud.calibration.axis.yaw.short");
            case ControllerCalibration.AXIS_THROTTLE -> Component.translatable("screen.fullfud.calibration.axis.throttle.short");
            default -> Component.translatable("screen.fullfud.calibration.axis.arm.short");
        };
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        FpvHudUi.renderBackdrop(graphics, width, height);
        super.render(graphics, mouseX, mouseY, partialTick);

        final int cx = width / 2;
        final int left = width / 2 - 155;
        int y = 16;

        FpvHudUi.renderPanel(graphics, left - 12, 12, 334, 82);
        FpvHudUi.renderPanel(graphics, left - 12, 104, 334, 142);
        FpvHudUi.renderPanel(graphics, left - 12, height - 66, 334, 32);

        graphics.drawCenteredString(font, title, cx, y, FpvHudUi.TEXT);
        y += 14;
        FpvHudUi.drawMutedCentered(graphics, font, currentInstruction(), cx, y);
        y += 16;

        FpvHudUi.drawAccentCentered(
            graphics,
            font,
            Component.translatable(
                "screen.fullfud.calibration.debug.current_controller",
                liveControllerName.isBlank() ? "-" : liveControllerName
            ),
            cx,
            y
        );
        y += 10;
        FpvHudUi.drawMutedCentered(
            graphics,
            font,
            Component.translatable(
                "screen.fullfud.calibration.debug.saved_controller",
                workingCalibration.getControllerName().isBlank() ? "-" : workingCalibration.getControllerName()
            ),
            cx,
            y
        );
        y += 10;
        FpvHudUi.drawMutedCentered(
            graphics,
            font,
            Component.translatable(
                "screen.fullfud.calibration.debug.ready_state",
                Component.translatable(
                    workingCalibration.isReady()
                        ? "screen.fullfud.calibration.debug.yes"
                        : "screen.fullfud.calibration.debug.no"
                ),
                liveAxes.length,
                liveButtons.length
            ),
            cx,
            y
        );
        y += 10;
        FpvHudUi.drawMutedCentered(
            graphics,
            font,
            Component.translatable(
                "screen.fullfud.calibration.debug.range_state",
                workingCalibration.getRangeAxisCount()
            ),
            cx,
            y
        );

        int rowY = 116;
        for (int row = 0; row < ROW_COUNT; row++) {
            graphics.drawString(font, rowName(row), left, rowY + 6, FpvHudUi.TEXT);
            if (row != ARM_ROW) {
                renderAxisPreview(graphics, left + 72, rowY + 6, row);
            } else {
                graphics.drawString(font, armPressedLabel(), left + 72, rowY + 6, FpvHudUi.TEXT_WARN);
            }
            rowY += 26;
        }

        graphics.drawCenteredString(
            font,
            statusLine(),
            cx,
            height - 72,
            FpvHudUi.TEXT_MUTED
        );
    }

    private Component armPressedLabel() {
        final boolean pressed;
        if (!workingCalibration.hasArmBinding()) {
            pressed = false;
        } else if (workingCalibration.isArmBindingAxis()) {
            final int axisIndex = workingCalibration.getArmAxisIndex();
            float value = axisIndex >= 0 && axisIndex < liveAxes.length ? liveAxes[axisIndex] : 0.0F;
            if (workingCalibration.isArmInverted()) {
                value = -value;
            }
            pressed = value > 0.1F;
        } else {
            final int buttonIndex = workingCalibration.getArmButtonIndex();
            pressed = buttonIndex >= 0 && buttonIndex < liveButtons.length && liveButtons[buttonIndex] != 0;
        }
        return Component.translatable(
            pressed
                ? "screen.fullfud.calibration.debug.pressed"
                : "screen.fullfud.calibration.debug.not_pressed"
        );
    }

    private Component statusLine() {
        final MutableComponent status = Component.empty()
            .append(Component.translatable("screen.fullfud.calibration.status"))
            .append(": ")
            .append(Component.translatable(
                workingCalibration.isReady()
                    ? "screen.fullfud.calibration.status_ok"
                    : "screen.fullfud.calibration.status_none"
            ));
        if (workingCalibration.isSampling()) {
            status.append(" | ").append(Component.translatable("screen.fullfud.calibration.range.running"));
        }
        return status;
    }

    private void renderAxisPreview(final GuiGraphics graphics, final int x, final int y, final int logicalAxis) {
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xCC11161D);

        final int physicalAxis = workingCalibration.getAxisMapping(logicalAxis);
        if (physicalAxis < 0) {
            graphics.drawString(font, "-", x + BAR_WIDTH + 8, y - 1, FpvHudUi.TEXT_MUTED);
            return;
        }

        float value;
        if (workingCalibration.hasRangeForAxis(physicalAxis)) {
            value = workingCalibration.normalizeMappedAxis(liveAxes, logicalAxis);
        } else {
            value = workingCalibration.readMappedAxis(liveAxes, logicalAxis);
            value = Mth.clamp(value, -1.0F, 1.0F);
            if (workingCalibration.isAxisInverted(logicalAxis)) {
                value = -value;
            }
        }

        final int centerX = x + BAR_WIDTH / 2;
        graphics.fill(centerX, y, centerX + 1, y + BAR_HEIGHT, FpvHudUi.BORDER);

        final int indicatorX = x + (int) (((value + 1.0F) * 0.5F) * BAR_WIDTH);
        graphics.fill(indicatorX - 1, y, indicatorX + 1, y + BAR_HEIGHT, FpvHudUi.TEXT_OK);

        if (workingCalibration.isSampling()) {
            final float min = rangeToBar(workingCalibration.getSampleMin(physicalAxis));
            final float max = rangeToBar(workingCalibration.getSampleMax(physicalAxis));
            graphics.fill(x + (int) min, y, x + (int) min + 1, y + BAR_HEIGHT, 0xFFFF5555);
            graphics.fill(x + (int) max, y, x + (int) max + 1, y + BAR_HEIGHT, FpvHudUi.SLIDER_TRACK);
        }

        graphics.drawString(font, String.format("%.2f", value), x + BAR_WIDTH + 8, y - 1, FpvHudUi.TEXT_MUTED);
    }

    private float rangeToBar(final float raw) {
        return Mth.clamp((raw + 1.0F) * 0.5F, 0.0F, 1.0F) * BAR_WIDTH;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
