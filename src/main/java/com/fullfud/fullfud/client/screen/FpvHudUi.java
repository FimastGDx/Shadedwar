package com.fullfud.fullfud.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class FpvHudUi {
    public static final int SCREEN_OVERLAY = 0xB40A0C10;
    public static final int PANEL_FILL = 0xCC0E131A;
    public static final int PANEL_FILL_LIGHT = 0xB8151B23;
    public static final int BORDER = 0x6647566A;
    public static final int BORDER_STRONG = 0xAA6D85A3;
    public static final int TEXT = 0xE7EDF6;
    public static final int TEXT_MUTED = 0x9AA8BA;
    public static final int TEXT_ACCENT = 0x9CC8FF;
    public static final int TEXT_OK = 0x8FE8B0;
    public static final int TEXT_WARN = 0xF0C07D;
    public static final int SLIDER_FILL = 0xAA1A2330;
    public static final int SLIDER_TRACK = 0xFF6DB2FF;
    public static final int SLIDER_KNOB = 0xFFE7EDF6;

    private FpvHudUi() {
    }

    public static void renderBackdrop(final GuiGraphics graphics, final int width, final int height) {
        graphics.fill(0, 0, width, height, SCREEN_OVERLAY);
    }

    public static void renderPanel(
        final GuiGraphics graphics,
        final int x,
        final int y,
        final int width,
        final int height
    ) {
        graphics.fill(x, y, x + width, y + height, PANEL_FILL);
        graphics.fill(x, y, x + width, y + 1, BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, BORDER);
        graphics.fill(x, y, x + 1, y + height, BORDER);
        graphics.fill(x + width - 1, y, x + width, y + height, BORDER);
    }

    public static void renderSectionHeader(
        final GuiGraphics graphics,
        final Font font,
        final Component text,
        final int x,
        final int y,
        final int width
    ) {
        graphics.drawString(font, text, x, y, TEXT);
        final int lineY = y + 9;
        graphics.fill(x, lineY, x + width, lineY + 1, BORDER);
    }

    public static void drawMutedCentered(
        final GuiGraphics graphics,
        final Font font,
        final Component text,
        final int centerX,
        final int y
    ) {
        graphics.drawCenteredString(font, text, centerX, y, TEXT_MUTED);
    }

    public static void drawAccentCentered(
        final GuiGraphics graphics,
        final Font font,
        final Component text,
        final int centerX,
        final int y
    ) {
        graphics.drawCenteredString(font, text, centerX, y, TEXT_ACCENT);
    }

    public static void renderSlider(
        final GuiGraphics graphics,
        final int x,
        final int y,
        final int width,
        final int height,
        final boolean hovered,
        final boolean active,
        final double value,
        final Component label
    ) {
        final int fill = active ? (hovered ? PANEL_FILL_LIGHT : SLIDER_FILL) : 0x8810151B;
        final int border = active ? (hovered ? BORDER_STRONG : BORDER) : 0x66506070;
        renderPanel(graphics, x, y, width, height);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);

        final int trackY = y + height - 4;
        final int trackLeft = x + 8;
        final int trackRight = x + width - 8;
        graphics.fill(trackLeft, trackY, trackRight, trackY + 1, 0x6647566A);

        final int knobX = trackLeft + (int) Math.round((trackRight - trackLeft) * Math.max(0.0D, Math.min(1.0D, value)));
        graphics.fill(trackLeft, trackY, knobX, trackY + 1, SLIDER_TRACK);
        graphics.fill(knobX - 1, y + 4, knobX + 1, y + height - 4, SLIDER_KNOB);

        final int textColor = active ? TEXT : TEXT_MUTED;
        graphics.drawString(Minecraft.getInstance().font, label, x + 8, y + 6, textColor);
    }
}
