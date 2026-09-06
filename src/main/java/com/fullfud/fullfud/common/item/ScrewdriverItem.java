package com.fullfud.fullfud.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The service tool. It carries no behaviour of its own — every use is a right-click on something else,
 * and that something else decides what to do: the drones open their service bay, the REB emitter
 * cycles its mode. This class exists so those checks can be an {@code instanceof} rather than a
 * comparison against a registry object.
 */
public class ScrewdriverItem extends Item {
    public ScrewdriverItem(final Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
        final ItemStack stack,
        final TooltipContext context,
        final List<Component> tooltip,
        final TooltipFlag flag
    ) {
        tooltip.add(Component.translatable("item.fullfud.screwdriver.hint").withStyle(ChatFormatting.GRAY));
    }
}
