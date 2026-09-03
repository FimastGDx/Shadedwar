package com.fullfud.fullfud.common.item;

import com.fullfud.fullfud.client.render.FpvControllerRenderer;
import com.fullfud.fullfud.common.entity.FpvDroneEntity;
import com.fullfud.fullfud.core.FpvDroneRecovery;
import com.fullfud.fullfud.core.FullfudDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class FpvControllerItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public FpvControllerItem(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            clearLink(stack);
            player.displayClientMessage(Component.translatable("message.fullfud.fpv.link_cleared"), true);
            return InteractionResult.SUCCESS_SERVER;
        }
        final Optional<UUID> linked = getLinked(stack);
        if (linked.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.fullfud.fpv.no_link"), true);
            return InteractionResult.SUCCESS_SERVER;
        }
        final ServerLevel serverLevel = serverPlayer.serverLevel();
        final var entity = serverLevel.getEntity(linked.get());
        if (entity instanceof FpvDroneEntity drone && !drone.isRemoved()) {
            drone.beginControl(serverPlayer);
            return InteractionResult.SUCCESS_SERVER;
        }
        // Not in memory is not the same as gone: a drone parked in an unloaded chunk answers to nothing
        // until its chunk is pulled back in. Only when there is no record of it anywhere is the link a
        // dead end worth clearing.
        if (!FpvDroneRecovery.beginControlWhenLoaded(serverPlayer, linked.get())) {
            clearLink(stack);
            player.displayClientMessage(Component.translatable("message.fullfud.fpv.drone_missing"), true);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    public void link(final ItemStack stack, final FpvDroneEntity drone, final Player player) {
        setLinked(stack, drone.getUUID());
        if (player instanceof ServerPlayer serverPlayer) {
            drone.setOwner(serverPlayer);
        }
        player.displayClientMessage(Component.translatable("message.fullfud.fpv.linked"), true);
        linkGoggles(player, drone.getUUID());
    }

    public static Optional<UUID> getLinked(final ItemStack stack) {
        return Optional.ofNullable(stack.get(FullfudDataComponents.LINKED_FPV_DRONE_CONTROLLER));
    }

    public static void setLinked(final ItemStack stack, final UUID id) {
        stack.set(FullfudDataComponents.LINKED_FPV_DRONE_CONTROLLER, id);
    }

    public static void clearLink(final ItemStack stack) {
        stack.remove(FullfudDataComponents.LINKED_FPV_DRONE_CONTROLLER);
    }

    private static void linkGoggles(final Player player, final java.util.UUID id) {
        final ItemStack head = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        if (head.getItem() instanceof FpvGogglesItem) {
            FpvGogglesItem.setLinked(head, id);
            return;
        }
        for (final ItemStack stack : player.getHandSlots()) {
            if (stack.getItem() instanceof FpvGogglesItem) {
                FpvGogglesItem.setLinked(stack, id);
                return;
            }
        }
        if (player.getInventory() != null) {
            for (final ItemStack stack : player.getInventory().items) {
                if (stack.getItem() instanceof FpvGogglesItem) {
                    FpvGogglesItem.setLinked(stack, id);
                    return;
                }
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private FpvControllerRenderer renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (this.renderer == null)
                    this.renderer = new FpvControllerRenderer();
                return this.renderer;
            }
        });
    }
}