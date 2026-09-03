package com.fullfud.fullfud.common.item;

import com.fullfud.fullfud.core.FullfudDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

public class RebBatteryItem extends Item {
    /**
     * Sized from the cheap job: two in-game days of listening on one pack, one charge-tick per tick.
     * {@code RebEmitterEntity.JAM_DRAIN_MULTIPLIER} then divides this down to the ten minutes the
     * transmitter is allowed, so the two runtimes stay tied to this one number.
     */
    public static final int MAX_CHARGE_TICKS = 2 * 24000;

    public RebBatteryItem(final Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final List<Component> tooltip, final TooltipFlag flag) {
        final int ticks = getChargeTicks(stack);
        final float percent = (ticks / (float) MAX_CHARGE_TICKS) * 100.0F;
        tooltip.add(Component.translatable("item.fullfud.reb_battery.charge", String.format("%.0f%%", percent)).withStyle(ChatFormatting.GRAY));
    }

    /**
     * A full battery is the component default declared on the item's {@code Properties}, so unlike the
     * 1.20.1 version this no longer writes that default back into the stack on the first read.
     */
    public static int getChargeTicks(final ItemStack stack) {
        final Integer ticks = stack.get(FullfudDataComponents.REB_CHARGE_TICKS);
        return Math.min(ticks == null ? MAX_CHARGE_TICKS : ticks, MAX_CHARGE_TICKS);
    }

    public static void setChargeTicks(final ItemStack stack, final int value) {
        stack.set(FullfudDataComponents.REB_CHARGE_TICKS, Math.max(0, Math.min(MAX_CHARGE_TICKS, value)));
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        return InteractionResult.PASS;
    }
}
