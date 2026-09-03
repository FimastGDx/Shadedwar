package com.fullfud.fullfud.common.item;

import com.fullfud.fullfud.FullfudMod;
import com.fullfud.fullfud.client.render.FpvGogglesRenderer;
import com.fullfud.fullfud.core.FullfudDataComponents;
import com.fullfud.fullfud.core.FullfudItemTags;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class FpvGogglesItem extends ArmorItem implements GeoItem {
    /** Client-side render asset id; the actual model comes from GeckoLib, this only satisfies {@code Equippable}. */
    public static final ResourceKey<EquipmentAsset> ASSET_ID = ResourceKey.create(
        EquipmentAssets.ROOT_ID, ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "fpv_goggles"));

    public static final ArmorMaterial MATERIAL = new ArmorMaterial(
        150,
        Map.of(ArmorType.HELMET, 1),
        8,
        BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.ARMOR_EQUIP_LEATHER.value()),
        0.0F,
        0.0F,
        FullfudItemTags.REPAIRS_FPV_GOGGLES,
        ASSET_ID
    );

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.fpv_goggles.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public FpvGogglesItem(final Properties properties) {
        super(MATERIAL, ArmorType.HELMET, properties);
    }

    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "fpv_goggles", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(final Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private FpvGogglesRenderer renderer;

            @Override
            public <E extends LivingEntity, S extends HumanoidRenderState> HumanoidModel<?> getGeoArmorRenderer(
                final E living, final ItemStack stack, final EquipmentSlot slot,
                final EquipmentClientInfo.LayerType type, final HumanoidModel<S> original) {
                if (renderer == null) {
                    renderer = new FpvGogglesRenderer();
                }
                return renderer;
            }
        });
    }

    public static void setLinked(final ItemStack stack, final UUID id) {
        stack.set(FullfudDataComponents.LINKED_FPV_DRONE_GOGGLES, id);
    }

    public static Optional<UUID> getLinked(final ItemStack stack) {
        return Optional.ofNullable(stack.get(FullfudDataComponents.LINKED_FPV_DRONE_GOGGLES));
    }

    public static void clearLink(final ItemStack stack) {
        stack.remove(FullfudDataComponents.LINKED_FPV_DRONE_GOGGLES);
    }
}
