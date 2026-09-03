package com.fullfud.fullfud.mixin.client;

import com.fullfud.fullfud.client.FpvClientHandler;
import com.fullfud.fullfud.client.ShahedClientHandler;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stands in for the pair of Forge HUD events the mod used: {@code RenderGuiEvent.Post} to draw the FPV
 * OSD and the REB status readout, and an unfiltered {@code RenderGuiOverlayEvent.Pre} cancel to hide the
 * vanilla HUD while flying.
 *
 * <p>Fabric API's {@code HudRenderCallback} is injected at the tail of this method, so cancelling the
 * vanilla HUD would take the mod's own overlays down with it. Both draws therefore go through one entry
 * point here: when the vanilla HUD is hidden the overlays are drawn at the head and the method is
 * cancelled, otherwise they are drawn on the way out. Exactly one of the two injectors ever draws in a
 * given frame.
 */
@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 1)
    private void fullfud$replaceVanillaHud(final GuiGraphics graphics, final DeltaTracker deltaTracker, final CallbackInfo callback) {
        if (FpvClientHandler.shouldHideVanillaHud()) {
            fullfud$renderOverlays(graphics);
            callback.cancel();
        }
    }

    @Inject(method = "render", at = @At("TAIL"), require = 1)
    private void fullfud$renderOverVanillaHud(final GuiGraphics graphics, final DeltaTracker deltaTracker, final CallbackInfo callback) {
        fullfud$renderOverlays(graphics);
    }

    private static void fullfud$renderOverlays(final GuiGraphics graphics) {
        FpvClientHandler.onRenderGui(graphics);
        ShahedClientHandler.onRenderGui(graphics);
    }
}
