package com.fullfud.fullfud.client.screen;

import com.fullfud.fullfud.client.input.FpvControllerInput;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ControllerSelectScreen extends Screen {
    private final ControllerCalibrationScreen parentScreen;
    private List<FpvControllerInput.ConnectedController> controllers = List.of();

    public ControllerSelectScreen(final ControllerCalibrationScreen parentScreen) {
        super(Component.translatable("screen.fullfud.calibration.controller.title"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        controllers = FpvControllerInput.listConnectedControllers();

        final int buttonWidth = 300;
        int y = 36;
        if (controllers.isEmpty()) {
            final FpvHudButton button = addRenderableWidget(new FpvHudButton(width / 2 - buttonWidth / 2, y, buttonWidth, 20, Component.translatable("screen.fullfud.calibration.controller.none"), () -> { }));
            button.active = false;
            y += 26;
        } else {
            final String selectedName = parentScreen.getSelectedControllerName();
            for (final FpvControllerInput.ConnectedController controller : controllers) {
                final Component label = Component.literal(
                    (controller.name().equalsIgnoreCase(selectedName) ? "> " : "")
                        + controller.name()
                        + " [" + controller.axisCount() + "/" + controller.buttonCount() + "]"
                );
                final FpvHudButton button = addRenderableWidget(new FpvHudButton(width / 2 - buttonWidth / 2, y, buttonWidth, 20, label, () -> choose(controller)));
                button.setSelected(controller.name().equalsIgnoreCase(selectedName));
                y += 24;
            }
        }

        addRenderableWidget(new FpvHudButton(width / 2 - 75, height - 28, 150, 20, Component.translatable("screen.fullfud.calibration.cancel"), this::onClose));
    }

    private void choose(final FpvControllerInput.ConnectedController controller) {
        parentScreen.handleControllerChosen(controller);
        if (minecraft != null && minecraft.screen == this) {
            minecraft.setScreen(parentScreen);
        }
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        FpvHudUi.renderBackdrop(graphics, width, height);
        super.render(graphics, mouseX, mouseY, partialTick);
        FpvHudUi.renderPanel(graphics, width / 2 - 162, 12, 324, Math.max(70, 52 + controllers.size() * 24));
        graphics.drawCenteredString(font, title, width / 2, 16, FpvHudUi.TEXT);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parentScreen);
        }
    }
}
