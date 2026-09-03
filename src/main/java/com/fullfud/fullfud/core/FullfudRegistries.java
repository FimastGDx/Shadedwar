package com.fullfud.fullfud.core;

import com.fullfud.fullfud.FullfudMod;
import com.fullfud.fullfud.common.entity.FpvDroneEntity;
import com.fullfud.fullfud.common.entity.ExplosionShrapnelEntity;
import com.fullfud.fullfud.common.entity.Fp5FlamingoEntity;
import com.fullfud.fullfud.common.entity.Fp5LauncherEntity;
import com.fullfud.fullfud.common.entity.RebEmitterEntity;
import com.fullfud.fullfud.common.entity.ShahedColor;
import com.fullfud.fullfud.common.entity.ShahedDroneEntity;
import com.fullfud.fullfud.common.entity.ShahedLauncherEntity;
import com.fullfud.fullfud.common.entity.drone.DronePreset;
import com.fullfud.fullfud.common.item.MonitorItem;
import com.fullfud.fullfud.common.item.FpvConfiguratorItem;
import com.fullfud.fullfud.common.item.Fp5FlamingoItem;
import com.fullfud.fullfud.common.item.Fp5LauncherItem;
import com.fullfud.fullfud.common.item.RebBatteryItem;
import com.fullfud.fullfud.common.item.RebEmitterItem;
import com.fullfud.fullfud.common.item.ScrewdriverItem;
import com.fullfud.fullfud.common.item.ShahedLauncherItem;
import com.fullfud.fullfud.common.item.ShahedDroneItem;
import com.fullfud.fullfud.common.item.FpvDroneItem;
import com.fullfud.fullfud.common.item.FpvControllerItem;
import com.fullfud.fullfud.common.item.FpvGogglesItem;
import com.fullfud.fullfud.common.item.WarheadItem;
import com.fullfud.fullfud.common.entity.drone.WarheadCharge;
import com.fullfud.fullfud.common.menu.DroneServiceMenu;
import com.fullfud.fullfud.common.menu.Fp5MonitorMenu;
import com.fullfud.fullfud.common.menu.ShahedMonitorMenu;
import com.fullfud.fullfud.core.registry.DeferredRegister;
import com.fullfud.fullfud.core.registry.RegistryObject;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Function;

public final class FullfudRegistries {
    private FullfudRegistries() {
    }

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, FullfudMod.MOD_ID);
    /**
     * The mod's only blocks: lithium ore and the steel storage block. Everything else the mod places in
     * the world is an entity, which is why this register did not exist before survival crafting did.
     * It is flushed <em>before</em> {@link #ITEMS} in {@link #register()}, because the two
     * {@code BlockItem}s resolve their block while the item suppliers run.
     */
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, FullfudMod.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, FullfudMod.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(BuiltInRegistries.MENU, FullfudMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, FullfudMod.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, FullfudMod.MOD_ID);

    public static final RegistryObject<Block> LITHIUM_ORE_BLOCK = block("lithium_ore", p ->
        new Block(p.mapColor(MapColor.STONE).strength(3.0F, 3.0F).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> STEEL_BLOCK = block("steel_block", p ->
        new Block(p.mapColor(MapColor.METAL).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL))
    );

    public static final RegistryObject<Item> LITHIUM_ORE_ITEM = blockItem("lithium_ore", LITHIUM_ORE_BLOCK);
    public static final RegistryObject<Item> STEEL_BLOCK_ITEM = blockItem("steel_block", STEEL_BLOCK);

    public static final RegistryObject<Item> LITHIUM_ITEM = item("lithium", Item::new);
    public static final RegistryObject<Item> STEEL_INGOT_ITEM = item("ingot_steel", Item::new);
    public static final RegistryObject<Item> STEEL_PLATE_ITEM = item("insert_steel", Item::new);
    public static final RegistryObject<Item> CONTROLLER_ITEM = item("controller", Item::new);
    public static final RegistryObject<Item> COPPER_WIRE_ITEM = item("wire_copper_new", Item::new);
    public static final RegistryObject<Item> SYNTH_POWDER_ITEM = item("synth_powder", Item::new);

    /** One canister is a third of a Shahed's tank. Consumed out of the service bay into the tank. */
    public static final RegistryObject<Item> SHAHED_FUEL_ITEM = item("shahed_fuel", Item::new);

    public static final RegistryObject<Item> SCREWDRIVER_ITEM = item("screwdriver", p ->
        new ScrewdriverItem(p.stacksTo(1))
    );

    public static final RegistryObject<Item> WARHEAD_TIER_1_ITEM = item("ball_1", p ->
        new WarheadItem(p, WarheadCharge.TIER_1)
    );
    public static final RegistryObject<Item> WARHEAD_TIER_2_ITEM = item("ball_2", p ->
        new WarheadItem(p, WarheadCharge.TIER_2)
    );
    public static final RegistryObject<Item> WARHEAD_TIER_3_ITEM = item("ball_3", p ->
        new WarheadItem(p, WarheadCharge.TIER_3)
    );
    public static final RegistryObject<Item> WARHEAD_TIER_4_ITEM = item("ball_4", p ->
        new WarheadItem(p, WarheadCharge.TIER_4)
    );

    public static final RegistryObject<Item> MONITOR_ITEM = item("monitor_control", p ->
        new MonitorItem(p.stacksTo(1))
    );

    public static final RegistryObject<Item> FPV_CONFIGURATOR_ITEM = item("fpv_configurator", p ->
        new FpvConfiguratorItem(p.stacksTo(1))
    );

    public static final RegistryObject<Item> SHAHED_ITEM = item("shahed_136", p ->
        new ShahedDroneItem(p.stacksTo(1), ShahedColor.WHITE)
    );

    public static final RegistryObject<Item> SHAHED_BLACK_ITEM = item("shahed_136_black", p ->
        new ShahedDroneItem(p.stacksTo(1), ShahedColor.BLACK)
    );

    public static final RegistryObject<Item> SHAHED_ITEM_SLOW = item("shahed_136_slow", p ->
        new ShahedDroneItem(p.stacksTo(1), ShahedColor.WHITE, 0.5D)
    );

    public static final RegistryObject<Item> SHAHED_BLACK_ITEM_SLOW = item("shahed_136_black_slow", p ->
        new ShahedDroneItem(p.stacksTo(1), ShahedColor.BLACK, 0.5D)
    );

    public static final RegistryObject<Item> FPV_DRONE_ITEM = item("fpv_drone", p ->
        new FpvDroneItem(p.stacksTo(1), DronePreset.STANDARD_STRIKE, 1.0D, 1.0D)
    );

    public static final RegistryObject<Item> FPV_DRONE_ITEM_X2 = item("fpv_drone_x2", p ->
        new FpvDroneItem(p.stacksTo(1), DronePreset.STANDARD_STRIKE, 2.0D, 2.0D)
    );

    public static final RegistryObject<Item> FPV_DRONE_ITEM_X4 = item("fpv_drone_x4", p ->
        new FpvDroneItem(p.stacksTo(1), DronePreset.STANDARD_STRIKE, 4.0D, 4.0D)
    );

    public static final RegistryObject<Item> FPV_DRONE_WHOOP_ITEM = item("fpv_drone_whoop", p ->
        new FpvDroneItem(p.stacksTo(1), DronePreset.TINY_WHOOP, 1.0D, 1.0D)
    );

    public static final RegistryObject<Item> FPV_DRONE_WHOOP_ITEM_X2 = item("fpv_drone_whoop_x2", p ->
        new FpvDroneItem(p.stacksTo(1), DronePreset.TINY_WHOOP, 2.0D, 2.0D)
    );

    public static final RegistryObject<Item> FPV_DRONE_WHOOP_ITEM_X4 = item("fpv_drone_whoop_x4", p ->
        new FpvDroneItem(p.stacksTo(1), DronePreset.TINY_WHOOP, 4.0D, 4.0D)
    );

    public static final RegistryObject<Item> FPV_DRONE_STRIKE_ITEM = item("fpv_drone_strike", p ->
        new FpvDroneItem(p.stacksTo(1), DronePreset.STRIKE_7INCH, 1.0D, 1.0D)
    );

    public static final RegistryObject<Item> FPV_DRONE_STRIKE_ITEM_X2 = item("fpv_drone_strike_x2", p ->
        new FpvDroneItem(p.stacksTo(1), DronePreset.STRIKE_7INCH, 2.0D, 2.0D)
    );

    public static final RegistryObject<Item> FPV_DRONE_STRIKE_ITEM_X4 = item("fpv_drone_strike_x4", p ->
        new FpvDroneItem(p.stacksTo(1), DronePreset.STRIKE_7INCH, 4.0D, 4.0D)
    );

    /**
     * Cargo airframes. Cargo is a property of the item and the entity rather than of the
     * {@link DronePreset}, because the preset is the physics tuning surface and a hauler flies like the
     * standard airframe it is built from — the difference is a chest bolted under it, i.e. slots.
     */
    public static final RegistryObject<Item> FPV_DRONE_CARGO_ITEM = item("fpv_drone_cargo", p ->
        new FpvDroneItem(p.stacksTo(1), DronePreset.STANDARD_STRIKE, 1.0D, 1.0D, true)
    );

    public static final RegistryObject<Item> FPV_DRONE_CARGO_ITEM_X2 = item("fpv_drone_cargo_x2", p ->
        new FpvDroneItem(p.stacksTo(1), DronePreset.STANDARD_STRIKE, 2.0D, 2.0D, true)
    );

    public static final RegistryObject<Item> FPV_DRONE_CARGO_ITEM_X4 = item("fpv_drone_cargo_x4", p ->
        new FpvDroneItem(p.stacksTo(1), DronePreset.STANDARD_STRIKE, 4.0D, 4.0D, true)
    );

    public static final RegistryObject<Item> FP5_FLAMINGO_ITEM = item("fp5flamingo", p ->
        new Fp5FlamingoItem(p.stacksTo(1))
    );

    public static final RegistryObject<Item> FPV_CONTROLLER_ITEM = item("fpv_controller", p ->
        new FpvControllerItem(p.stacksTo(1))
    );

    public static final RegistryObject<Item> FPV_GOGGLES_ITEM = item("fpv_goggles", p ->
        new FpvGogglesItem(p.stacksTo(1))
    );

    public static final RegistryObject<Item> SHAHED_LAUNCHER_ITEM = item("shahed_launcher", p ->
        new ShahedLauncherItem(p.stacksTo(1))
    );

    public static final RegistryObject<Item> FP5_LAUNCHER_ITEM = item("launcherfp5", p ->
        new Fp5LauncherItem(p.stacksTo(1))
    );

    public static final RegistryObject<Item> REB_EMITTER_ITEM = item("reb_emitter", p ->
        new RebEmitterItem(p.stacksTo(1))
    );

    public static final RegistryObject<Item> REB_BATTERY_ITEM = item("reb_battery", p ->
        new RebBatteryItem(p
            .stacksTo(1)
            .component(FullfudDataComponents.REB_CHARGE_TICKS, RebBatteryItem.MAX_CHARGE_TICKS))
    );

    public static final RegistryObject<EntityType<ShahedDroneEntity>> SHAHED_ENTITY = ENTITY_TYPES.register("shahed_136", () ->
        EntityType.Builder.<ShahedDroneEntity>of(ShahedDroneEntity::new, MobCategory.MISC)
            .sized(3.0F, 1.0F)
            .clientTrackingRange(2000)
            .updateInterval(1)
            .build(entityKey("shahed_136"))
    );

    public static final RegistryObject<EntityType<FpvDroneEntity>> FPV_DRONE_ENTITY = ENTITY_TYPES.register("fpv_drone", () ->
        EntityType.Builder.<FpvDroneEntity>of(FpvDroneEntity::new, MobCategory.MISC)
            .sized(0.9F, 0.35F)
            .clientTrackingRange(4096)
            .updateInterval(1)
            .build(entityKey("fpv_drone"))
    );

    public static final RegistryObject<EntityType<ExplosionShrapnelEntity>> EXPLOSION_SHRAPNEL_ENTITY = ENTITY_TYPES.register("explosion_shrapnel", () ->
        EntityType.Builder.<ExplosionShrapnelEntity>of(ExplosionShrapnelEntity::new, MobCategory.MISC)
            .sized(0.1F, 0.1F)
            .clientTrackingRange(160)
            .updateInterval(1)
            .build(entityKey("explosion_shrapnel"))
    );

    public static final RegistryObject<EntityType<ShahedLauncherEntity>> SHAHED_LAUNCHER_ENTITY = ENTITY_TYPES.register("shahed_launcher", () ->
        EntityType.Builder.<ShahedLauncherEntity>of(ShahedLauncherEntity::new, MobCategory.MISC)
            .sized(1.0F, 1.0F)
            .clientTrackingRange(64)
            .updateInterval(10)
            .build(entityKey("shahed_launcher"))
    );

    public static final RegistryObject<EntityType<Fp5LauncherEntity>> FP5_LAUNCHER_ENTITY = ENTITY_TYPES.register("launcherfp5", () ->
        EntityType.Builder.<Fp5LauncherEntity>of(Fp5LauncherEntity::new, MobCategory.MISC)
            .sized(1.125F * Fp5LauncherEntity.SCALE, 2.75F * Fp5LauncherEntity.SCALE)
            .clientTrackingRange(64)
            .updateInterval(10)
            .build(entityKey("launcherfp5"))
    );

    public static final RegistryObject<EntityType<Fp5FlamingoEntity>> FP5_FLAMINGO_ENTITY = ENTITY_TYPES.register("fp5flamingo", () ->
        EntityType.Builder.<Fp5FlamingoEntity>of(Fp5FlamingoEntity::new, MobCategory.MISC)
            .sized(1.9375F * Fp5FlamingoEntity.SCALE, 1.2F * Fp5FlamingoEntity.SCALE)
            .clientTrackingRange(2048)
            .updateInterval(1)
            .build(entityKey("fp5flamingo"))
    );

    public static final RegistryObject<EntityType<RebEmitterEntity>> REB_EMITTER_ENTITY = ENTITY_TYPES.register("reb_emitter", () ->
        EntityType.Builder.<RebEmitterEntity>of(RebEmitterEntity::new, MobCategory.MISC)
            .sized(1.0F, 1.0F)
            .clientTrackingRange(64)
            .updateInterval(10)
            .build(entityKey("reb_emitter"))
    );

    public static final RegistryObject<MenuType<ShahedMonitorMenu>> SHAHED_MONITOR_MENU = MENU_TYPES.register("shahed_monitor", () ->
        new ExtendedScreenHandlerType<>(ShahedMonitorMenu::new, ShahedMonitorMenu.Data.STREAM_CODEC)
    );

    public static final RegistryObject<MenuType<Fp5MonitorMenu>> FP5_MONITOR_MENU = MENU_TYPES.register("fp5_monitor", () ->
        new ExtendedScreenHandlerType<>(Fp5MonitorMenu::new, Fp5MonitorMenu.Data.STREAM_CODEC)
    );

    /** The screwdriver-opened loading bay. Unlike the two monitor menus, this one has real slots. */
    public static final RegistryObject<MenuType<DroneServiceMenu>> DRONE_SERVICE_MENU = MENU_TYPES.register("drone_service", () ->
        new ExtendedScreenHandlerType<>(DroneServiceMenu::new, DroneServiceMenu.Data.STREAM_CODEC)
    );

    public static final RegistryObject<SoundEvent> SHAHED_ENGINE_START = SOUND_EVENTS.register("shahed.engine_start",
        () -> SoundEvent.createVariableRangeEvent(resource("shahed.engine_start"))
    );
    public static final RegistryObject<SoundEvent> SHAHED_ENGINE_LOOP = SOUND_EVENTS.register("shahed.engine_loop",
        () -> SoundEvent.createVariableRangeEvent(resource("shahed.engine_loop"))
    );
    public static final RegistryObject<SoundEvent> SHAHED_ENGINE_DIVE = SOUND_EVENTS.register("shahed.engine_dive",
        () -> SoundEvent.createVariableRangeEvent(resource("shahed.engine_dive"))
    );
    public static final RegistryObject<SoundEvent> SHAHED_ENGINE_END = SOUND_EVENTS.register("shahed.engine_end",
        () -> SoundEvent.createVariableRangeEvent(resource("shahed.engine_end"))
    );
    public static final RegistryObject<SoundEvent> SHAHED_FLYBY = SOUND_EVENTS.register("shahed.flyby",
        () -> SoundEvent.createVariableRangeEvent(resource("shahed.flyby"))
    );

    public static final RegistryObject<SoundEvent> FPV_ENGINE_START = SOUND_EVENTS.register("fpv.engine_start",
        () -> SoundEvent.createVariableRangeEvent(resource("fpv.engine_start"))
    );
    public static final RegistryObject<SoundEvent> FPV_ENGINE_LOOP = SOUND_EVENTS.register("fpv.engine_loop",
        () -> SoundEvent.createVariableRangeEvent(resource("fpv.engine_loop"))
    );
    public static final RegistryObject<SoundEvent> FPV_ENGINE_INTERIOR = SOUND_EVENTS.register("fpv.engine_interior",
        () -> SoundEvent.createFixedRangeEvent(resource("fpv.engine_interior"), 8.0F)
    );
    public static final RegistryObject<SoundEvent> FPV_ENGINE_STOP = SOUND_EVENTS.register("fpv.engine_stop",
        () -> SoundEvent.createVariableRangeEvent(resource("fpv.engine_stop"))
    );

    public static final RegistryObject<SoundEvent> EXPLOSION_CLOSE = SOUND_EVENTS.register("explosion_close",
        () -> SoundEvent.createVariableRangeEvent(resource("explosion_close"))
    );
    public static final RegistryObject<SoundEvent> EXPLOSION_MEDIUM = SOUND_EVENTS.register("explosion_medium",
        () -> SoundEvent.createVariableRangeEvent(resource("explosion_medium"))
    );
    public static final RegistryObject<SoundEvent> EXPLOSION_FAR = SOUND_EVENTS.register("explosion_far",
        () -> SoundEvent.createVariableRangeEvent(resource("explosion_far"))
    );
    public static final RegistryObject<SoundEvent> EXPLOSION_VERYFAR = SOUND_EVENTS.register("explosion_veryfar",
        () -> SoundEvent.createVariableRangeEvent(resource("explosion_veryfar"))
    );
    public static final RegistryObject<SoundEvent> SHRAPNEL_HIT = SOUND_EVENTS.register("shrapnel_hit",
        () -> SoundEvent.createVariableRangeEvent(resource("shrapnel_hit"))
    );

    /** Alert tones for the drone screens. Played through the UI channel, so the range never matters. */
    public static final RegistryObject<SoundEvent> ALERT_WARN = SOUND_EVENTS.register("alert.warn",
        () -> SoundEvent.createVariableRangeEvent(resource("alert.warn"))
    );
    public static final RegistryObject<SoundEvent> ALERT_CAUTION = SOUND_EVENTS.register("alert.caution",
        () -> SoundEvent.createVariableRangeEvent(resource("alert.caution"))
    );

    public static void register() {
        BLOCKS.register();
        ITEMS.register();
        ENTITY_TYPES.register();
        MENU_TYPES.register();
        BLOCK_ENTITY_TYPES.register();
        SOUND_EVENTS.register();
    }

    private static ResourceLocation resource(final String name) {
        return ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, name);
    }

    /**
     * 1.21 requires every {@link Item} to know its own registry key before construction
     * ({@code Item.Properties.setId}), so items are registered through a factory that receives
     * pre-stamped properties instead of building their own.
     */
    private static RegistryObject<Item> item(final String name, final Function<Item.Properties, ? extends Item> factory) {
        return ITEMS.register(name, () -> factory.apply(
            new Item.Properties().setId(ResourceKey.create(Registries.ITEM, resource(name)))));
    }

    /** Blocks need the same pre-stamped-id treatment as items in 1.21. */
    private static RegistryObject<Block> block(
        final String name,
        final Function<BlockBehaviour.Properties, ? extends Block> factory
    ) {
        return BLOCKS.register(name, () -> factory.apply(
            BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, resource(name)))));
    }

    /**
     * The item form of a block. Shares the block's registry name, and takes
     * {@code useBlockDescriptionPrefix} so the lang key is {@code block.fullfud.<name>} rather than
     * {@code item.fullfud.<name>}.
     */
    private static RegistryObject<Item> blockItem(final String name, final RegistryObject<Block> block) {
        return item(name, p -> new BlockItem(block.get(), p.useBlockDescriptionPrefix()));
    }

    private static ResourceKey<EntityType<?>> entityKey(final String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, resource(name));
    }
}
