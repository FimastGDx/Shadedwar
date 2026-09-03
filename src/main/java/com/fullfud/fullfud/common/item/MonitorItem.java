package com.fullfud.fullfud.common.item;

import com.fullfud.fullfud.client.render.MonitorRenderer;
import com.fullfud.fullfud.common.entity.Fp5FlamingoEntity;
import com.fullfud.fullfud.common.entity.ShahedDroneEntity;
import com.fullfud.fullfud.common.menu.Fp5MonitorMenu;
import com.fullfud.fullfud.common.menu.ShahedMonitorMenu;
import com.fullfud.fullfud.core.FullfudDataComponents;
import com.fullfud.fullfud.core.data.ShahedLinkData;
import com.fullfud.fullfud.core.network.FullfudNetwork;
import com.fullfud.fullfud.core.network.packet.ShahedLinkPacket;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
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

public class MonitorItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MonitorItem(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        final Optional<UUID> linkedDrone = getLinkedDrone(stack);
        if (linkedDrone.isPresent()) {
            openLinkedShahedMonitor(serverPlayer, stack, linkedDrone.get());
            return InteractionResult.SUCCESS_SERVER;
        }

        final Optional<UUID> linkedFp5 = getLinkedFp5(stack);
        if (linkedFp5.isPresent()) {
            openLinkedFp5Monitor(serverPlayer, stack, linkedFp5.get());
            return InteractionResult.SUCCESS_SERVER;
        }

        player.displayClientMessage(Component.translatable("message.fullfud.monitor.no_link"), true);

        return InteractionResult.SUCCESS_SERVER;
    }

    public static Optional<UUID> getLinkedDrone(final ItemStack stack) {
        return Optional.ofNullable(stack.get(FullfudDataComponents.LINKED_SHAHED));
    }

    public static void setLinkedDrone(final ItemStack stack, final UUID droneId) {
        clearLinkedFp5(stack);
        stack.set(FullfudDataComponents.LINKED_SHAHED, droneId);
    }

    public static void clearLinkedDrone(final ItemStack stack) {
        stack.remove(FullfudDataComponents.LINKED_SHAHED);
    }

    public static Optional<UUID> getLinkedFp5(final ItemStack stack) {
        return Optional.ofNullable(stack.get(FullfudDataComponents.LINKED_FP5));
    }

    public static void setLinkedFp5(final ItemStack stack, final UUID flamingoId) {
        clearLinkedDrone(stack);
        stack.set(FullfudDataComponents.LINKED_FP5, flamingoId);
    }

    public static void clearLinkedFp5(final ItemStack stack) {
        stack.remove(FullfudDataComponents.LINKED_FP5);
    }

    public static void linkAndOpenFp5Monitor(final ServerPlayer player, final ItemStack stack, final Fp5FlamingoEntity flamingo) {
        if (player == null || stack == null || flamingo == null || !flamingo.isAlive()) {
            return;
        }
        setLinkedFp5(stack, flamingo.getUUID());
        if (openFp5Monitor(player, flamingo)) {
            player.displayClientMessage(Component.translatable("message.fullfud.monitor.fp5_linked"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.fullfud.monitor.open_failed"), true);
        }
    }

    public static boolean openFp5Monitor(final ServerPlayer player, final Fp5FlamingoEntity flamingo) {
        if (player == null || flamingo == null || !flamingo.isAlive()) {
            return false;
        }
        try {
            player.openMenu(new ExtendedScreenHandlerFactory<Fp5MonitorMenu.Data>() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("menu.fullfud.fp5_monitor");
                }

                @Override
                public AbstractContainerMenu createMenu(final int containerId, final Inventory inv, final Player ply) {
                    return new Fp5MonitorMenu(
                        containerId,
                        inv,
                        flamingo.getUUID(),
                        flamingo.getId(),
                        flamingo.getMonitorTarget(),
                        flamingo.isLaunched()
                    );
                }

                @Override
                public Fp5MonitorMenu.Data getScreenOpeningData(final ServerPlayer serverPlayer) {
                    return new Fp5MonitorMenu.Data(
                        flamingo.getUUID(),
                        flamingo.getId(),
                        flamingo.getMonitorTarget(),
                        flamingo.isLaunched()
                    );
                }
            });
            return true;
        } catch (final Throwable ignored) {
            return false;
        }
    }

    private static Optional<ShahedDroneEntity> findLinkedDrone(final ServerPlayer player, final UUID droneId) {
        final ServerLevel currentLevel = player.serverLevel();
        final Optional<ShahedDroneEntity> local = ShahedDroneEntity.find(currentLevel, droneId);
        if (local.isPresent()) {
            return local;
        }
        if (player.getServer() == null) {
            return Optional.empty();
        }
        for (final ServerLevel level : player.getServer().getAllLevels()) {
            if (level == currentLevel) {
                continue;
            }
            final Optional<ShahedDroneEntity> found = ShahedDroneEntity.find(level, droneId);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private static Optional<Fp5FlamingoEntity> findLinkedFp5(final ServerPlayer player, final UUID flamingoId) {
        final ServerLevel currentLevel = player.serverLevel();
        final Optional<Fp5FlamingoEntity> local = Fp5FlamingoEntity.find(currentLevel, flamingoId);
        if (local.isPresent()) {
            return local;
        }
        if (player.getServer() == null) {
            return Optional.empty();
        }
        for (final ServerLevel level : player.getServer().getAllLevels()) {
            if (level == currentLevel) {
                continue;
            }
            final Optional<Fp5FlamingoEntity> found = Fp5FlamingoEntity.find(level, flamingoId);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private static void unlinkAcrossLevels(final ServerPlayer player, final UUID droneId) {
        if (player.getServer() == null) {
            ShahedLinkData.get(player.serverLevel()).unlink(droneId);
            return;
        }
        for (final ServerLevel level : player.getServer().getAllLevels()) {
            ShahedLinkData.get(level).unlink(droneId);
        }
    }

    private static void openLinkedShahedMonitor(final ServerPlayer serverPlayer, final ItemStack stack, final UUID droneId) {
        findLinkedDrone(serverPlayer, droneId).ifPresentOrElse(drone -> {
            if (!drone.assignOwner(serverPlayer)) {
                serverPlayer.displayClientMessage(Component.translatable("message.fullfud.monitor.in_use"), true);
                return;
            }
            if (!drone.beginRemoteControl(serverPlayer)) {
                serverPlayer.displayClientMessage(Component.translatable("message.fullfud.monitor.in_use"), true);
                return;
            }
            drone.addViewer(serverPlayer);
            try {
                serverPlayer.openMenu(new ExtendedScreenHandlerFactory<ShahedMonitorMenu.Data>() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable("menu.fullfud.shahed_monitor");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(final int containerId, final Inventory inv, final Player ply) {
                        return new ShahedMonitorMenu(containerId, inv, droneId, drone.getId());
                    }

                    @Override
                    public ShahedMonitorMenu.Data getScreenOpeningData(final ServerPlayer viewer) {
                        return new ShahedMonitorMenu.Data(droneId, drone.getId());
                    }
                });
            } catch (final Throwable t) {
                drone.removeViewer(serverPlayer);
                drone.endRemoteControl(serverPlayer);
                serverPlayer.displayClientMessage(Component.translatable("message.fullfud.monitor.open_failed"), true);
            }
        }, () -> {
            unlinkAcrossLevels(serverPlayer, droneId);
            clearLinkedDrone(stack);
            FullfudNetwork.sendToPlayer(serverPlayer, new ShahedLinkPacket(droneId, false));
            serverPlayer.displayClientMessage(Component.translatable("message.fullfud.monitor.drone_missing"), true);
        });
    }

    private static void openLinkedFp5Monitor(final ServerPlayer serverPlayer, final ItemStack stack, final UUID flamingoId) {
        findLinkedFp5(serverPlayer, flamingoId).ifPresentOrElse(flamingo -> {
            if (!openFp5Monitor(serverPlayer, flamingo)) {
                serverPlayer.displayClientMessage(Component.translatable("message.fullfud.monitor.open_failed"), true);
            }
        }, () -> {
            clearLinkedFp5(stack);
            serverPlayer.displayClientMessage(Component.translatable("message.fullfud.monitor.fp5_missing"), true);
        });
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
            private MonitorRenderer renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (this.renderer == null)
                    this.renderer = new MonitorRenderer();
                return this.renderer;
            }
        });
    }
}
