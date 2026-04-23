package com.fullfud.fullfud.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.Objects;

public class FpvHudButton extends AbstractWidget {
    private final Runnable onPress;
    private boolean selected;

    public FpvHudButton(
        final int x,
        final int y,
        final int width,
        final int height,
        final Component message,
        final Runnable onPress
    ) {
        super(x, y, width, height, message);
        this.onPress = Objects.requireNonNull(onPress, "onPress");
    }

    public void setSelected(final boolean selected) {
        this.selected = selected;
    }

    @Override
    public void onClick(final double mouseX, final double mouseY) {
        if (active) {
            onPress.run();
        }
    }

    @Override
    protected void renderWidget(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        final int x1 = getX();
        final int y1 = getY();
        final int x2 = x1 + width;
        final int y2 = y1 + height;
        int fill = FpvHudUi.PANEL_FILL;
        int border = FpvHudUi.BORDER;
        int text = FpvHudUi.TEXT;

        if (!active) {
            fill = 0x8810151B;
            border = 0x66506070;
            text = 0x6F7884;
        } else if (selected) {
            fill = 0xCC172230;
            border = FpvHudUi.BORDER_STRONG;
        } else if (isHoveredOrFocused()) {
            fill = FpvHudUi.PANEL_FILL_LIGHT;
            border = FpvHudUi.BORDER_STRONG;
        }

        graphics.fill(x1, y1, x2, y2, fill);
        graphics.fill(x1, y1, x2, y1 + 1, border);
        graphics.fill(x1, y2 - 1, x2, y2, border);
        graphics.fill(x1, y1, x1 + 1, y2, border);
        graphics.fill(x2 - 1, y1, x2, y2, border);
        graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), x1 + width / 2, y1 + (height - 8) / 2, text);
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if (!active || !visible) {
            return false;
        }
        if (keyCode == 257 || keyCode == 32) {
            onPress.run();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
