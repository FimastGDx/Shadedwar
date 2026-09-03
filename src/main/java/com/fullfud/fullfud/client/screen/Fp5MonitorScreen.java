package com.fullfud.fullfud.client.screen;

import com.fullfud.fullfud.common.menu.Fp5MonitorMenu;
import com.fullfud.fullfud.core.network.FullfudClientNetwork;
import com.fullfud.fullfud.core.network.packet.Fp5LaunchPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class Fp5MonitorScreen extends AbstractContainerScreen<Fp5MonitorMenu> {
    private static final int PANEL_WIDTH = 272;
    private static final int PANEL_HEIGHT = 176;
    private static final int PANEL_PADDING = 14;
    private static final int HEADER_HEIGHT = 34;
    private static final int FOOTER_HEIGHT = 28;
    private static final int FIELD_HEIGHT = 18;
    private static final int FIELD_GAP = 8;
    private static final int LABEL_WIDTH = 10;

    private static final int OVERLAY_FILL = 0x6C000000;
    private static final int PANEL_FILL = 0xB0101010;
    private static final int PANEL_BORDER = 0xA0383838;
    private static final int SECTION_FILL = 0x88131313;
    private static final int SECTION_BORDER = 0x903C3C3C;
    private static final int FIELD_FILL = 0xCC151515;
    private static final int FIELD_BORDER = 0xA0484848;
    private static final int BUTTON_FILL = 0xC01A1A1A;
    private static final int BUTTON_FILL_HOVER = 0xD0262626;
    private static final int BUTTON_FILL_DISABLED = 0x90111111;
    private static final int BUTTON_BORDER = 0xC05A5A5A;
    private static final int BUTTON_BORDER_HOVER = 0xD08A8A8A;
    private static final int BUTTON_BORDER_DISABLED = 0x80404040;
    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int TEXT_MUTED = 0xB8B8B8;
    private static final int TEXT_DISABLED = 0x808080;

    private EditBox xField;
    private EditBox yField;
    private EditBox zField;
    private SquareButton launchButton;

    public Fp5MonitorScreen(final Fp5MonitorMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = 0;
        this.imageHeight = 0;
        this.titleLabelX = 0;
        this.titleLabelY = 0;
        this.inventoryLabelX = 0;
        this.inventoryLabelY = 0;
    }

    @Override
    protected void init() {
        super.init();
        final int panelX = panelX();
        final int panelY = panelY();
        final int fieldWidth = PANEL_WIDTH - PANEL_PADDING * 2 - LABEL_WIDTH - 8;
        final int fieldX = panelX + PANEL_PADDING + LABEL_WIDTH + 8;
        final int firstFieldY = panelY + HEADER_HEIGHT + 28;

        xField = createField(fieldX, firstFieldY, fieldWidth, menu.getTargetPos().getX());
        yField = createField(fieldX, firstFieldY + FIELD_HEIGHT + FIELD_GAP, fieldWidth, menu.getTargetPos().getY());
        zField = createField(fieldX, firstFieldY + (FIELD_HEIGHT + FIELD_GAP) * 2, fieldWidth, menu.getTargetPos().getZ());

        addRenderableWidget(xField);
        addRenderableWidget(yField);
        addRenderableWidget(zField);

        launchButton = addRenderableWidget(new SquareButton(
            panelX + PANEL_PADDING,
            panelY + PANEL_HEIGHT - FOOTER_HEIGHT + 6,
            PANEL_WIDTH - PANEL_PADDING * 2,
            16,
            Component.translatable("screen.fullfud.fp5_monitor.launch"),
            this::launch
        ));
        setInitialFocus(xField);
        updateLaunchButton();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // 1.20.2 folded EditBox's cursor blink into the render pass, so the per-field tick() is gone.
        updateLaunchButton();
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        final int panelX = panelX();
        final int panelY = panelY();
        final int fieldWidth = PANEL_WIDTH - PANEL_PADDING * 2 - LABEL_WIDTH - 8;
        final int labelX = panelX + PANEL_PADDING;
        final int fieldX = panelX + PANEL_PADDING + LABEL_WIDTH + 8;
        final int firstFieldY = panelY + HEADER_HEIGHT + 28;
        final int headerY = panelY + 8;
        final int headerHeight = HEADER_HEIGHT;
        final int contentY = panelY + HEADER_HEIGHT + 16;
        final int contentHeight = PANEL_HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT - 24;
        final int footerY = panelY + PANEL_HEIGHT - FOOTER_HEIGHT;

        graphics.fill(0, 0, width, height, OVERLAY_FILL);
        fillOutlinedRect(graphics, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, PANEL_FILL, PANEL_BORDER);
        fillOutlinedRect(graphics, panelX + 8, headerY, PANEL_WIDTH - 16, headerHeight, SECTION_FILL, SECTION_BORDER);
        fillOutlinedRect(graphics, panelX + 8, contentY, PANEL_WIDTH - 16, contentHeight, SECTION_FILL, SECTION_BORDER);
        fillOutlinedRect(graphics, panelX + 8, footerY, PANEL_WIDTH - 16, FOOTER_HEIGHT - 8, SECTION_FILL, SECTION_BORDER);
        fillOutlinedRect(graphics, fieldX - 4, firstFieldY - 1, fieldWidth + 8, FIELD_HEIGHT, FIELD_FILL, FIELD_BORDER);
        fillOutlinedRect(graphics, fieldX - 4, firstFieldY + FIELD_HEIGHT + FIELD_GAP - 1, fieldWidth + 8, FIELD_HEIGHT, FIELD_FILL, FIELD_BORDER);
        fillOutlinedRect(graphics, fieldX - 4, firstFieldY + (FIELD_HEIGHT + FIELD_GAP) * 2 - 1, fieldWidth + 8, FIELD_HEIGHT, FIELD_FILL, FIELD_BORDER);

        drawShadowText(graphics, title, panelX + PANEL_PADDING, panelY + 18, TEXT_COLOR);
        drawShadowText(
            graphics,
            Component.translatable(menu.isLaunched()
                ? "screen.fullfud.fp5_monitor.state_in_flight"
                : "screen.fullfud.fp5_monitor.state_ready"),
            panelX + PANEL_PADDING,
            panelY + 32,
            TEXT_MUTED
        );
        drawShadowText(graphics, Component.literal("TARGET"), panelX + PANEL_PADDING, contentY + 10, TEXT_MUTED);

        drawShadowText(graphics, Component.translatable("screen.fullfud.fp5_monitor.coord_x"), labelX, firstFieldY + 4, TEXT_COLOR);
        drawShadowText(graphics, Component.translatable("screen.fullfud.fp5_monitor.coord_y"), labelX, firstFieldY + FIELD_HEIGHT + FIELD_GAP + 4, TEXT_COLOR);
        drawShadowText(graphics, Component.translatable("screen.fullfud.fp5_monitor.coord_z"), labelX, firstFieldY + (FIELD_HEIGHT + FIELD_GAP) * 2 + 4, TEXT_COLOR);

        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partialTick, final int mouseX, final int mouseY) {
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if (keyCode == 257 && launchButton.active) {
            launch();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private EditBox createField(final int x, final int y, final int width, final int value) {
        final EditBox field = new EditBox(font, x, y, width, FIELD_HEIGHT - 2, Component.empty());
        field.setValue(Integer.toString(value));
        field.setMaxLength(12);
        field.setBordered(false);
        field.setTextColor(TEXT_COLOR);
        field.setTextColorUneditable(TEXT_MUTED);
        field.setFilter(text -> text.matches("-?\\d*"));
        field.setResponder(ignored -> updateLaunchButton());
        return field;
    }

    private void updateLaunchButton() {
        launchButton.active = !menu.isLaunched() && parseField(xField) != null && parseField(yField) != null && parseField(zField) != null;
    }

    private void launch() {
        final Integer targetX = parseField(xField);
        final Integer targetY = parseField(yField);
        final Integer targetZ = parseField(zField);
        if (targetX == null || targetY == null || targetZ == null || menu.getFlamingoId() == null) {
            return;
        }
        FullfudClientNetwork.sendToServer(new Fp5LaunchPacket(menu.getFlamingoId(), targetX, targetY, targetZ));
        onClose();
    }

    private Integer parseField(final EditBox field) {
        if (field == null) {
            return null;
        }
        final String text = field.getValue();
        if (text == null || text.isBlank() || "-".equals(text)) {
            return null;
        }
        try {
            return Mth.clamp(Integer.parseInt(text), -30000000, 30000000);
        } catch (final NumberFormatException ignored) {
            return null;
        }
    }

    private void fillOutlinedRect(
        final GuiGraphics graphics,
        final int x,
        final int y,
        final int width,
        final int height,
        final int fill,
        final int border
    ) {
        graphics.fill(x, y, x + width, y + height, fill);
        graphics.fill(x, y, x + width, y + 1, border);
        graphics.fill(x, y + height - 1, x + width, y + height, border);
        graphics.fill(x, y, x + 1, y + height, border);
        graphics.fill(x + width - 1, y, x + width, y + height, border);
    }

    private void drawShadowText(final GuiGraphics graphics, final Component text, final int x, final int y, final int color) {
        graphics.drawString(font, text, x, y, color, true);
    }

    private int panelX() {
        return (width - PANEL_WIDTH) / 2;
    }

    private int panelY() {
        return (height - PANEL_HEIGHT) / 2;
    }

    private final class SquareButton extends AbstractWidget {
        private final Runnable onPress;

        private SquareButton(
            final int x,
            final int y,
            final int width,
            final int height,
            final Component message,
            final Runnable onPress
        ) {
            super(x, y, width, height, message);
            this.onPress = onPress;
        }

        @Override
        public void onClick(final double mouseX, final double mouseY) {
            if (active) {
                onPress.run();
            }
        }

        @Override
        protected void renderWidget(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
            final int fill;
            final int border;
            final int textColor;
            if (!active) {
                fill = BUTTON_FILL_DISABLED;
                border = BUTTON_BORDER_DISABLED;
                textColor = TEXT_DISABLED;
            } else if (isHoveredOrFocused()) {
                fill = BUTTON_FILL_HOVER;
                border = BUTTON_BORDER_HOVER;
                textColor = TEXT_COLOR;
            } else {
                fill = BUTTON_FILL;
                border = BUTTON_BORDER;
                textColor = TEXT_COLOR;
            }

            fillOutlinedRect(graphics, getX(), getY(), width, height, fill, border);
            final Font currentFont = Minecraft.getInstance().font;
            final int textX = getX() + (width - currentFont.width(getMessage())) / 2;
            final int textY = getY() + (height - 8) / 2;
            graphics.drawString(currentFont, getMessage(), textX, textY, textColor, true);
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
}
