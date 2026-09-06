package com.fullfud.fullfud.common.item;

import com.fullfud.fullfud.common.entity.drone.WarheadCharge;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A charge that can be bolted into a drone's service bay. The tier is a property of the item, not of
 * the stack, so the four registry entries are the four tiers.
 */
public class WarheadItem extends Item {
    private final WarheadCharge charge;

    public WarheadItem(final Properties properties, final WarheadCharge charge) {
        super(properties);
        this.charge = charge;
    }

    public WarheadCharge getCharge() {
        return this.charge;
    }

    /** {@link WarheadCharge#NONE} for anything that is not a charge, so call sites need no instanceof. */
    public static WarheadCharge chargeOf(final ItemStack stack) {
        return stack.getItem() instanceof WarheadItem warhead ? warhead.charge : WarheadCharge.NONE;
    }

    @Override
    public void appendHoverText(
        final ItemStack stack,
        final TooltipContext context,
        final List<Component> tooltip,
        final TooltipFlag flag
    ) {
        tooltip.add(Component.translatable("item.fullfud.warhead.tier", this.charge.tier())
            .withStyle(ChatFormatting.GRAY));
        if (this.charge.incendiary()) {
            tooltip.add(Component.translatable("item.fullfud.warhead.incendiary")
                .withStyle(ChatFormatting.GOLD));
        }
        if (this.charge.tier() > WarheadCharge.FPV_MAX.tier()) {
            tooltip.add(Component.translatable("item.fullfud.warhead.shahed_only")
                .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
