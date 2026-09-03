package com.fullfud.fullfud.core;

import com.fullfud.fullfud.FullfudMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Item tags the mod owns. */
public final class FullfudItemTags {

    /**
     * Anvil repair material for {@link com.fullfud.fullfud.common.item.FpvGogglesItem}. Intentionally
     * ships empty — the Forge version returned {@code Ingredient.EMPTY}, so the goggles are not
     * repairable — but 1.21's {@code ArmorMaterial} takes a tag rather than an ingredient, and a data
     * pack can fill this one in.
     */
    public static final TagKey<Item> REPAIRS_FPV_GOGGLES = tag("repairs_fpv_goggles");

    private FullfudItemTags() {
    }

    private static TagKey<Item> tag(final String name) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, name));
    }
}
