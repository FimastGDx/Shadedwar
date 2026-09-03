package com.fullfud.fullfud.core;

import com.fullfud.fullfud.FullfudMod;
import com.fullfud.fullfud.core.registry.DeferredRegister;
import com.fullfud.fullfud.core.registry.RegistryObject;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class FullfudCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, FullfudMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> FULLFUD_TAB = CREATIVE_MODE_TABS.register("fullfud_tab",
            () -> FabricItemGroup.builder()
                    .title(Component.literal("Shaded war"))
                    .icon(() -> new ItemStack(FullfudRegistries.SHAHED_ITEM.get()))
                    .displayItems((pParameters, pOutput) -> {
                        FullfudRegistries.ITEMS.getEntries().forEach(regObj -> {
                            pOutput.accept(regObj.get());
                        });
                    })
                    .build());

    public static void register() {
        CREATIVE_MODE_TABS.register();
    }
}
