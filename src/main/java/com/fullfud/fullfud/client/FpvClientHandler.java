package com.fullfud.fullfud.client;

import com.fullfud.fullfud.client.render.FpvDroneRenderer;
import com.fullfud.fullfud.client.input.ControllerCalibration;
import com.fullfud.fullfud.client.input.ControllerCalibrationStore;
import com.fullfud.fullfud.client.input.FpvControllerInput;
import com.fullfud.fullfud.client.screen.ControllerCalibrationScreen;
import com.fullfud.fullfud.client.screen.FpvConfiguratorScreen;
import com.fullfud.fullfud.common.entity.ExplosionShrapnelEntity;
import com.fullfud.fullfud.common.entity.FpvDroneEntity;
import com.fullfud.fullfud.common.entity.drone.FpvDroneConfig;
import com.fullfud.fullfud.core.FullfudRegistries;
import com.fullfud.fullfud.core.config.FullfudClientConfig;
import com.fullfud.fullfud.core.network.FullfudClientNetwork;
import com.fullfud.fullfud.core.network.packet.FpvControlPacket;
import com.fullfud.fullfud.core.network.packet.FpvDetonatePacket;
import com.fullfud.fullfud.core.network.packet.FpvReleasePacket;
import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class FpvClientHandler {
    private static final Logger CAMERA_SMOOTHING_LOGGER = LogUtils.getLogger();
    private static final Logger DIAGNOSTIC_LOGGER = LogUtils.getLogger();
    private static final boolean CAMERA_SMOOTHING_DEBUG_ENABLED = false;
    // Arrow keys rather than Q/E: those two are vanilla drop-item and inventory, and KeyMapping keys its
    // lookup map on the key itself, so a mod mapping sharing a key silently takes the vanilla one out of
    // service. KeyBindMigration pushes existing profiles off the old defaults.
    private static final KeyMapping FPV_YAW_LEFT = new KeyMapping("key.fullfud.fpv_yaw_left", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT, "key.categories.fullfud");
    private static final KeyMapping FPV_YAW_RIGHT = new KeyMapping("key.fullfud.fpv_yaw_right", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT, "key.categories.fullfud");
    private static final KeyMapping FPV_ARM = new KeyMapping("key.fullfud.fpv_arm", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.fullfud");
    /** Fires the installed charge. Only does anything on a drone that has one. */
    private static final KeyMapping FPV_DETONATE = new KeyMapping("key.fullfud.fpv_detonate", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.fullfud");
    private static final KeyMapping FPV_CALIBRATE = new KeyMapping("key.fullfud.fpv_calibrate", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.fullfud");

    private static final ResourceLocation FONT_DIGITAL_LOC = ResourceLocation.fromNamespaceAndPath("fullfud", "digital");
    private static final Style DIGITAL_STYLE = Style.EMPTY.withFont(FONT_DIGITAL_LOC);

    private static final ResourceLocation TEX_PRICEL = ResourceLocation.fromNamespaceAndPath("fullfud", "textures/gui/hud/pricel.png");
    private static final ResourceLocation TEX_VERT = ResourceLocation.fromNamespaceAndPath("fullfud", "textures/gui/hud/vert.png");

    private static final ResourceLocation BATTERY_0 = ResourceLocation.fromNamespaceAndPath("fullfud", "textures/gui/hud/battery/a0.png");
    private static final ResourceLocation BATTERY_25 = ResourceLocation.fromNamespaceAndPath("fullfud", "textures/gui/hud/battery/a25.png");
    private static final ResourceLocation BATTERY_50 = ResourceLocation.fromNamespaceAndPath("fullfud", "textures/gui/hud/battery/a50.png");
    private static final ResourceLocation BATTERY_75 = ResourceLocation.fromNamespaceAndPath("fullfud", "textures/gui/hud/battery/a75.png");
    private static final ResourceLocation BATTERY_100 = ResourceLocation.fromNamespaceAndPath("fullfud", "textures/gui/hud/battery/a100.png");

    private static final ResourceLocation SIGNAL_0 = ResourceLocation.fromNamespaceAndPath("fullfud", "textures/gui/hud/signal/0.png");
    private static final ResourceLocation SIGNAL_25 = ResourceLocation.fromNamespaceAndPath("fullfud", "textures/gui/hud/signal/25.png");
    private static final ResourceLocation SIGNAL_50 = ResourceLocation.fromNamespaceAndPath("fullfud", "textures/gui/hud/signal/50.png");
    private static final ResourceLocation SIGNAL_75 = ResourceLocation.fromNamespaceAndPath("fullfud", "textures/gui/hud/signal/75.png");
    private static final ResourceLocation SIGNAL_100 = ResourceLocation.fromNamespaceAndPath("fullfud", "textures/gui/hud/signal/100.png");

    /**
     * 1.21.2 moved post chains behind {@link net.minecraft.client.renderer.ShaderManager}, which loads them
     * from {@code assets/<namespace>/post_effect/<name>.json} and keys them by a bare id — no directory and
     * no extension, unlike the file path the old {@code new PostChain(...)} constructor took.
     */
    private static final ResourceLocation SHADER_LOC = ResourceLocation.fromNamespaceAndPath("fullfud", "fpv_post");

    /**
     * The chain writes through an internal {@code swap} target and reads {@code minecraft:main}, which
     * {@link PostChain#process} imports from the render target handed to it.
     */
    private static final Set<ResourceLocation> SHADER_EXTERNAL_TARGETS = Set.of(PostChain.MAIN_TARGET_ID);
    private static final float KEYBOARD_THROTTLE_MAX = 0.65F;

    /** Which emulated stick {@link #shapeKeyboardAxis} is smoothing; each keeps its own position between ticks. */
    private static final int AXIS_PITCH = 0;
    private static final int AXIS_ROLL = 1;
    private static final int AXIS_YAW = 2;
    private static UUID activeDrone;
    private static FpvDroneEntity cachedResolvedDrone;
    private static long cachedResolvedDroneGameTime = Long.MIN_VALUE;
    private static UUID cachedResolvedPlayerId;
    private static UUID cachedResolvedPreferredDroneId;
    private static Object cachedResolvedLevel;
    private static boolean resolvedDroneCacheComputed;
    private static float throttleDemand;
    private static float throttleDisplayMax = 1.0F;
    /**
     * First throttle sample of the current controller session, or {@code null} while no device is present.
     * See {@link #isControllerInputActive}.
     */
    private static Float controllerThrottleBaseline;
    private static float keyPitchSmoothed;
    private static float keyYawSmoothed;
    private static float keyRollSmoothed;
    private static float verticalSpeedEstimate;
    private static double lastDroneY;
    private static UUID verticalSpeedDroneId;
    private static long lastDiagnosticLogMs;
    private static double speedMs;
    private static double groundSpeedKmh;
    private static boolean escRequested;
    private static boolean releaseSent;
    private static double distanceToPilot;

    private static double lastMouseX;
    private static double lastMouseY;
    private static boolean mouseInitialized = false;

    private static boolean inFpvMode = false;
    private static CameraType previousCameraType;
    private static boolean forcedFirstPerson;
    private static Integer previousFov;
    private static boolean forcedFov;
    /**
     * The {@code swap} target the chain bounces through is now an ephemeral frame-graph resource rather than
     * something the chain owns, so it has to be allocated from somewhere. Three frames of retention is what
     * vanilla's {@code GameRenderer} keeps for its own chains; without a pool the target would be created and
     * deleted every frame.
     */
    private static final CrossFrameResourcePool POST_CHAIN_RESOURCE_POOL = new CrossFrameResourcePool(3);
    private static Method cameraSetPositionMethodCache;
    private static float clientTime = 0.0F;
    private static final boolean OPTIFINE_PRESENT = isClassPresent("net.optifine.Config");
    private static float lastResolvedCameraYaw = 0.0F;
    private static final float CAMERA_ROTATION_SMOOTH_ALPHA = 0.42F;
    private static final double CAMERA_POSITION_SMOOTH_ALPHA = 0.48D;
    private static final float CAMERA_ROTATION_SNAP_DOT = 0.15F;
    private static final double CAMERA_POSITION_SNAP_DISTANCE_SQR = 4.0D;
    private static final float CAMERA_YAW_SINGULARITY_PITCH_DEG = 89.0F;
    private static final double CAMERA_YAW_SINGULARITY_HORIZ_EPS = 1.0E-3D;
    private static final Quaternionf smoothedCameraQuaternion = new Quaternionf();
    private static final Quaternionf targetCameraQuaternion = new Quaternionf();
    private static final Quaternionf cameraAnglesQuaternionScratch = new Quaternionf();
    private static final Quaternionf zeroRollQuaternionScratch = new Quaternionf();
    private static final Vector3f cameraForwardScratch = new Vector3f();
    private static final Vector3f cameraUpScratch = new Vector3f();
    private static final Vector3f zeroRollUpScratch = new Vector3f();
    private static final Vector3f rollCrossScratch = new Vector3f();
    private static UUID smoothedCameraDroneId;
    private static boolean smoothedCameraInitialized;
    private static double smoothedCameraX;
    private static double smoothedCameraY;
    private static double smoothedCameraZ;
    private static boolean localPlayerStateCaptured = false;

    private static final ControllerCalibration controllerCalibration = new ControllerCalibration();
    private static boolean lastControllerPresent = false;
    private static boolean localPlayerSilent;
    private static boolean localPlayerNoGravity;

    private FpvClientHandler() {
    }

    /**
     * Called from {@code FullfudClientMod}. Forge split this across three phases — two mod-bus listeners
     * for renderers and key mappings, then {@code FMLClientSetupEvent} for the eight Forge-bus listeners —
     * because those registries were only open during startup. Fabric's client initializer is that window,
     * so it all collapses into one method.
     *
     * <p>Only the two client-tick listeners are events here. The render-tick pair, the camera angles, the
     * HUD, the vanilla-HUD suppression, the hand and the entity-sound cancel have no Fabric API event and
     * are driven from {@code mixin.client}: {@code GameRendererMixin}, {@code GuiMixin},
     * {@code ItemInHandRendererMixin} and {@code LevelSoundMixin} call into this class directly.
     */
    public static void registerClientEvents() {
        EntityRendererRegistry.register(FullfudRegistries.FPV_DRONE_ENTITY.get(), FpvDroneRenderer::new);
        // 1.21.2 deleted Entity.noCulling; frustum culling is now the renderer's call, so the
        // never-cull the shrapnel used to ask for is an affectedByCulling override.
        EntityRendererRegistry.register(FullfudRegistries.EXPLOSION_SHRAPNEL_ENTITY.get(),
            context -> new ThrownItemRenderer<ExplosionShrapnelEntity>(context, 0.5F, false) {
                @Override
                protected boolean affectedByCulling(final ExplosionShrapnelEntity entity) {
                    return false;
                }
            });

        KeyBindingHelper.registerKeyBinding(FPV_YAW_LEFT);
        KeyBindingHelper.registerKeyBinding(FPV_YAW_RIGHT);
        KeyBindingHelper.registerKeyBinding(FPV_ARM);
        KeyBindingHelper.registerKeyBinding(FPV_DETONATE);
        KeyBindingHelper.registerKeyBinding(FPV_CALIBRATE);

        ControllerCalibrationStore.loadInto(controllerCalibration);

        ClientTickEvents.END_CLIENT_TICK.register(FpvClientHandler::onClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(FpvSoundHandler::onClientTick);
    }

    public static void openConfigurator(final UUID droneId, final FpvDroneConfig config) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        minecraft.setScreen(new FpvConfiguratorScreen(droneId, config));
    }

    /**
     * First half of the former render-tick listener, called from {@code GameRendererMixin} at the head of
     * the frame. Forge ran this under {@code TickEvent.Phase.START}.
     */
    public static void onRenderTickStart(final float partialTick) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        final FpvDroneEntity drone = resolveActiveControlledDrone(minecraft);
        if (isFpvActive(minecraft, drone)) {
            updateSmoothedCameraState(drone, partialTick);
        } else {
            invalidateSmoothedCameraState();
        }
    }

    /**
     * Second half of the former render-tick listener, called from {@code GameRendererMixin} at the tail of
     * the frame — where Forge ran the {@code END} phase, after the world is drawn and before the frame is
     * presented, so the post chain still processes a finished main render target.
     */
    public static void onRenderTickEnd(final float partialTick) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            if (inFpvMode) {
                inFpvMode = false;
                releaseFpvChain();
            }
            restoreFov();
            return;
        }

        final FpvDroneEntity drone = resolveActiveControlledDrone(minecraft);

        boolean shouldFpv = false;
        float signal = 1.0F;
        if (drone != null) {
            shouldFpv = true;
            signal = drone.getSignalQuality();
        }

        if (!shouldFpv) {
            if (inFpvMode) {
                inFpvMode = false;
                releaseFpvChain();
            }
            restoreFov();
            return;
        }

        inFpvMode = true;
        final float shaderTimeScale = (float) (double) FullfudClientConfig.CLIENT.fpvPostShaderTimeScale.get();
        clientTime += partialTick * shaderTimeScale;
        if (shouldUsePostShader()) {
            processFpvPostChain(minecraft, signal);
        } else {
            releaseFpvChain();
        }
    }

    /**
     * Runs the FPV post chain over the finished main render target. The shader manager owns the chain and
     * reloads it with the resource pack set, returning {@code null} when the definition is missing or failed
     * to compile — which is the same silent no-op the old code produced by leaving its field null.
     */
    private static void processFpvPostChain(final Minecraft mc, final float signalQuality) {
        final PostChain chain = mc.getShaderManager().getPostChain(SHADER_LOC, SHADER_EXTERNAL_TARGETS);
        if (chain == null) {
            return;
        }
        try {
            // setUniform replaces the reflective walk over PostChain's private List<PostPass>: it fans the
            // value out to every pass through safeGetUniform, so passes without the uniform just ignore it.
            chain.setUniform("SignalQuality", signalQuality);
            chain.setUniform("Time", clientTime);
            chain.process(mc.getMainRenderTarget(), POST_CHAIN_RESOURCE_POOL);
            POST_CHAIN_RESOURCE_POOL.endFrame();
        } catch (final Exception ignored) {
        }
    }

    /**
     * Replaces the old {@code destroyFpvChain}: there is no chain to close any more, only the pooled
     * {@code swap} target to hand back and the shader clock to rewind so the noise does not resume
     * mid-pattern on the next flight.
     */
    private static void releaseFpvChain() {
        POST_CHAIN_RESOURCE_POOL.clear();
        clientTime = 0.0F;
    }

    /**
     * Registered on {@code ClientTickEvents.END_CLIENT_TICK}, which is the end phase of Forge's
     * {@code TickEvent.ClientTickEvent} the listener used to filter for by hand.
     */
    public static void onClientTick(final Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null || minecraft.level == null) {
            resetState();
            return;
        }

        // Controller calibration: keybind and auto-show on first detect
        handleCalibration(minecraft);

        if (minecraft.getCameraEntity() instanceof FpvDroneEntity cameraDrone && (cameraDrone.isRemoved() || !cameraDrone.isAlive())) {
            resetState();
            return;
        }

        final FpvDroneEntity drone = resolveActiveControlledDrone(minecraft);
        if (drone == null) {
            resetState();
            return;
        }

        ensureDroneCamera(minecraft, drone);

        if (!hasUsableGoggles(minecraft, drone)) {
            if (!releaseSent) {
                FullfudClientNetwork.sendToServer(new FpvReleasePacket(drone.getUUID()));
                releaseSent = true;
            }
            return;
        } else {
            releaseSent = false;
        }

        ensureFirstPerson(minecraft);
        ensureFpvFov(minecraft);
        stabilizeLocalPlayer(minecraft.player, drone);
        if (FullfudClientConfig.CLIENT.fpvSuppressSpectatorHotbarKeys.get()) {
            suppressSpectatorHotbarKeys(minecraft);
        }

        if (activeDrone == null || !activeDrone.equals(drone.getUUID())) {
            throttleDemand = drone.getThrust();
            activeDrone = drone.getUUID();
        }

        final boolean canProcessInput = minecraft.screen == null;

        final FpvControllerInput.State controllerState = canProcessInput
            ? FpvControllerInput.poll(controllerCalibration)
            : new FpvControllerInput.State(false, 0, 0, 0, 0, false, false);
        final boolean controllerActive = isControllerInputActive(controllerState);

        double curX = minecraft.mouseHandler.xpos();
        double curY = minecraft.mouseHandler.ypos();

        if (!mouseInitialized) {
            lastMouseX = curX;
            lastMouseY = curY;
            mouseInitialized = true;
        }

        float mousePitchDelta = 0.0F;
        float mouseRollDelta = 0.0F;
        final boolean simpleFlight = FullfudClientConfig.CLIENT.fpvKeyboardSimpleFlight.get();
        // Arcade flight has its own switch and its own sensitivity: there the mouse steers, which is a different
        // job from leaning the airframe, and a setting tuned for one is wrong for the other.
        final boolean mouseEnabled = simpleFlight
            ? FullfudClientConfig.CLIENT.fpvKeyboardMouseSteer.get()
            : FullfudClientConfig.CLIENT.fpvCameraMouseLookEnabled.get();
        if (canProcessInput && mouseEnabled) {
            final double dx = curX - lastMouseX;
            final double dy = curY - lastMouseY;
            final float vanillaSensitivity = (float) (double) minecraft.options.sensitivity().get();
            final float multiplier = simpleFlight
                ? (float) (double) FullfudClientConfig.CLIENT.fpvKeyboardMouseSensitivity.get()
                : (float) (FullfudClientConfig.CLIENT.fpvCameraMouseSensitivity.get() / 0.015D);
            final float mouseScale = vanillaSensitivity * 0.007F * multiplier;
            mousePitchDelta = (float) dy * mouseScale;
            mouseRollDelta = (float) dx * mouseScale;
        }

        lastMouseX = curX;
        lastMouseY = curY;

        Vec3 velocity = drone.getDeltaMovement();
        speedMs = velocity.length() * 20.0D;
        Vec3 horiz = new Vec3(velocity.x, 0, velocity.z);
        groundSpeedKmh = horiz.length() * 20.0D * 3.6D;

        distanceToPilot = Math.sqrt(drone.distanceToSqr(minecraft.player));

        updateVerticalSpeedEstimate(drone);

        // A positive pitch input pitches the nose down, which is what tilts the rotor disc forward and makes
        // the drone accelerate the way it is looking, so W has to be the positive direction. It used to be S:
        // the mouse pitch axis was the only one with the right sense, so W flew backwards.
        //
        // invertPitch is not consulted in arcade flight. Flipping its default could not fix anyone who already
        // had a config file — ConfigSpec only writes keys that are new — so every install from before BETA.373
        // was still flying W backwards. In arcade flight the key is a direction, and W means forward, full stop.
        final boolean invertKeyPitch = !FullfudClientConfig.CLIENT.fpvKeyboardSimpleFlight.get()
            && FullfudClientConfig.CLIENT.fpvKeyboardInvertPitch.get();
        final float keyPitch = invertKeyPitch
            ? axis(minecraft.options.keyDown.isDown(), minecraft.options.keyUp.isDown())
            : axis(minecraft.options.keyUp.isDown(), minecraft.options.keyDown.isDown());

        // A and D as a turn means W and A is a circle, not a diagonal — the heading the forward key follows keeps
        // moving while the key is held. As a strafe the two axes are independent, so held together they add up to a
        // straight diagonal, and turning moves to the mouse and the dedicated yaw keys.
        final boolean strafeWithAd = simpleFlight && FullfudClientConfig.CLIENT.fpvKeyboardStrafeWithAd.get();
        final float keySideways = axis(minecraft.options.keyRight.isDown(), minecraft.options.keyLeft.isDown());
        final float keyYawFromMovement = strafeWithAd ? 0.0F : keySideways;

        // Positive yaw turns right (DronePhysics negates it before rotating about the up axis), so the
        // dedicated yaw keys read right-positive too; they used to be the other way round from A/D.
        final float keyYawRaw = Mth.clamp(
            keyYawFromMovement + axis(FPV_YAW_RIGHT.isDown(), FPV_YAW_LEFT.isDown()),
            -1.0F,
            1.0F
        );

        float pitchInput = shapeKeyboardAxis(keyPitch, AXIS_PITCH);
        float rollInput = strafeWithAd ? shapeKeyboardAxis(keySideways, AXIS_ROLL) : 0.0F;
        float yawInput = shapeKeyboardAxis(keyYawRaw, AXIS_YAW);
        final boolean jumpDown = minecraft.options.keyJump.isDown();
        final boolean sneakDown = minecraft.options.keyShift.isDown();

        if (controllerActive) {
            if (FullfudClientConfig.CLIENT.fpvCameraControllerPriority.get()) {
                pitchInput = controllerState.pitch();
                rollInput = controllerState.roll();
                yawInput = controllerState.yaw();
                mousePitchDelta = 0.0F;
                mouseRollDelta = 0.0F;
            } else {
                pitchInput = Mth.clamp(pitchInput + controllerState.pitch(), -1.0F, 1.0F);
                rollInput = Mth.clamp(rollInput + controllerState.roll(), -1.0F, 1.0F);
                yawInput = Mth.clamp(yawInput + controllerState.yaw(), -1.0F, 1.0F);
            }
        }

        if (controllerActive && controllerState.hasThrottle()) {
            final double slew = FullfudClientConfig.CLIENT.fpvControllerThrottleSlew.get();
            final float a = (float) Mth.clamp(1.0D - slew, 0.0D, 1.0D);
            throttleDemand = Mth.lerp(a, throttleDemand, Mth.clamp(controllerState.throttle(), 0.0F, 1.0F));
            throttleDisplayMax = 1.0F;
        } else if (FullfudClientConfig.CLIENT.fpvKeyboardThrottleHold.get()) {
            // A key cannot hold a position, so the key drives the rate of change and the value persists between
            // presses; otherwise the only two throttle settings reachable from a keyboard are "climb" and "motors off".
            final float rampPerTick = (float) (KEYBOARD_THROTTLE_MAX
                / Math.max(0.05D, FullfudClientConfig.CLIENT.fpvKeyboardThrottleRampSeconds.get()) / 20.0D);
            if (jumpDown && !sneakDown) {
                throttleDemand = Math.min(KEYBOARD_THROTTLE_MAX, throttleDemand + rampPerTick);
            } else if (sneakDown && !jumpDown) {
                throttleDemand = Math.max(0.0F, throttleDemand - rampPerTick);
            } else if (FullfudClientConfig.CLIENT.fpvKeyboardAltitudeHold.get()) {
                throttleDemand += altitudeHoldCorrection(rampPerTick);
            }
            throttleDemand = Mth.clamp(throttleDemand, 0.0F, KEYBOARD_THROTTLE_MAX);
            throttleDisplayMax = KEYBOARD_THROTTLE_MAX;
        } else if (jumpDown) {
            throttleDemand = KEYBOARD_THROTTLE_MAX;
            throttleDisplayMax = KEYBOARD_THROTTLE_MAX;
        } else {
            throttleDemand = 0.0F;
            throttleDisplayMax = KEYBOARD_THROTTLE_MAX;
        }

        byte armAction = 0;
        if (FPV_ARM.consumeClick()) {
            armAction = drone.isArmed() ? (byte) 2 : (byte) 1;
        }

        // Drained separately from the control packet: detonation is a one-shot command, not a control axis,
        // and the server refuses it when no charge is installed.
        boolean detonateRequested = false;
        while (FPV_DETONATE.consumeClick()) {
            detonateRequested = true;
        }
        if (detonateRequested) {
            FullfudClientNetwork.sendToServer(new FpvDetonatePacket(drone.getUUID()));
        }

        if (controllerState.present() && controllerState.armClicked()) {
            armAction = drone.isArmed() ? (byte) 2 : (byte) 1;
        }
        
        FullfudClientNetwork.sendToServer(new FpvControlPacket(
            drone.getUUID(),
            pitchInput,
            rollInput,
            yawInput,
            controllerCalibration.getRcRate(ControllerCalibration.AXIS_ROLL),
            controllerCalibration.getSuperRate(ControllerCalibration.AXIS_ROLL),
            controllerCalibration.getExpo(ControllerCalibration.AXIS_ROLL),
            controllerCalibration.getRcRate(ControllerCalibration.AXIS_PITCH),
            controllerCalibration.getSuperRate(ControllerCalibration.AXIS_PITCH),
            controllerCalibration.getExpo(ControllerCalibration.AXIS_PITCH),
            controllerCalibration.getRcRate(ControllerCalibration.AXIS_YAW),
            controllerCalibration.getSuperRate(ControllerCalibration.AXIS_YAW),
            controllerCalibration.getExpo(ControllerCalibration.AXIS_YAW),
            mousePitchDelta,
            mouseRollDelta,
            throttleDemand,
            armAction
        ));

        logInputDiagnostics(minecraft, drone, controllerState, controllerActive, jumpDown,
            pitchInput, rollInput, yawInput, mousePitchDelta, mouseRollDelta, armAction);

        if (FullfudClientConfig.CLIENT.fpvCameraReleaseOnPause.get() && minecraft.screen instanceof PauseScreen && !escRequested) {
            escRequested = true;
            FullfudClientNetwork.sendToServer(new FpvReleasePacket(drone.getUUID()));
        } else if (!(minecraft.screen instanceof PauseScreen)) {
            escRequested = false;
        }
    }

    /**
     * Tracks the drone's vertical speed in blocks per second from its position, not from
     * {@code getDeltaMovement()}: the client copy of a remotely simulated entity has its position interpolated
     * toward the server's, so the position delta is the signal that is actually maintained here. Smoothed
     * because that interpolation arrives in uneven steps.
     */
    private static void updateVerticalSpeedEstimate(final FpvDroneEntity drone) {
        if (!drone.getUUID().equals(verticalSpeedDroneId)) {
            verticalSpeedDroneId = drone.getUUID();
            lastDroneY = drone.getY();
            verticalSpeedEstimate = 0.0F;
            return;
        }
        final float raw = (float) ((drone.getY() - lastDroneY) * 20.0D);
        lastDroneY = drone.getY();
        verticalSpeedEstimate = Mth.lerp(0.3F, verticalSpeedEstimate, raw);
    }

    /**
     * Holds altitude when the pilot lets go of both throttle keys. A held throttle is what makes altitude
     * controllable at all from a keyboard, but on its own it means "release the key and keep climbing", because
     * whatever value got the drone off the ground is above what hovering needs. This trims the throttle toward
     * the value that nulls vertical speed — a P term on vertical speed, which integrates into the hover throttle
     * on its own and re-trims when the airframe tilts and the thrust vector stops pointing straight up.
     *
     * @param rampPerTick the pilot's own throttle rate, used as the correction limit so the assist never moves
     *                    the throttle faster than a key press would
     */
    private static float altitudeHoldCorrection(final float rampPerTick) {
        final float deadzone = (float) (double) FullfudClientConfig.CLIENT.fpvKeyboardAltitudeHoldDeadzone.get();
        if (Math.abs(verticalSpeedEstimate) <= deadzone) {
            return 0.0F;
        }
        final float gain = (float) (double) FullfudClientConfig.CLIENT.fpvKeyboardAltitudeHoldGain.get();
        final float correctionPerTick = -gain * verticalSpeedEstimate / 20.0F;
        return Mth.clamp(correctionPerTick, -rampPerTick, rampPerTick);
    }

    /**
     * Emulates a proportional stick from an on/off key. Without this a tap of W is a full-deflection command, and
     * the default rates turn full deflection into roughly 700 deg/s — the airframe is past vertical before the key
     * comes back up. The key now drives a stick that takes {@code axisRampSeconds} to reach {@code axisMax} and
     * springs back over {@code axisReturnSeconds}.
     */
    private static float shapeKeyboardAxis(final float direction, final int axis) {
        // Arcade flight turns the axis into a direction rather than a rotation rate, so the deflection cap that
        // keeps the Betaflight curve civilised only slows the drone down there.
        final float max = FullfudClientConfig.CLIENT.fpvKeyboardSimpleFlight.get()
            ? 1.0F
            : (float) (double) FullfudClientConfig.CLIENT.fpvKeyboardAxisMax.get();
        final float target = Mth.clamp(direction, -1.0F, 1.0F) * max;
        final float current = switch (axis) {
            case AXIS_PITCH -> keyPitchSmoothed;
            case AXIS_ROLL -> keyRollSmoothed;
            default -> keyYawSmoothed;
        };

        final double seconds = target == 0.0F
            ? FullfudClientConfig.CLIENT.fpvKeyboardAxisReturnSeconds.get()
            : FullfudClientConfig.CLIENT.fpvKeyboardAxisRampSeconds.get();

        float next;
        if (seconds <= 0.0D) {
            next = target;
        } else {
            final float step = (float) (max / (seconds * 20.0D));
            if (Math.abs(target - current) <= step) {
                next = target;
            } else {
                next = current + Math.copySign(step, target - current);
            }
        }
        next = Mth.clamp(next, -max, max);

        switch (axis) {
            case AXIS_PITCH -> keyPitchSmoothed = next;
            case AXIS_ROLL -> keyRollSmoothed = next;
            default -> keyYawSmoothed = next;
        }
        return next;
    }

    private static void logInputDiagnostics(
        final Minecraft minecraft,
        final FpvDroneEntity drone,
        final FpvControllerInput.State controllerState,
        final boolean controllerActive,
        final boolean jumpDown,
        final float pitchInput,
        final float rollInput,
        final float yawInput,
        final float mousePitchDelta,
        final float mouseRollDelta,
        final byte armAction
    ) {
        if (!FullfudClientConfig.CLIENT.fpvControllerDiagnosticLog.get()) {
            return;
        }
        final long now = System.currentTimeMillis();
        if (now - lastDiagnosticLogMs < 1000L) {
            return;
        }
        lastDiagnosticLogMs = now;

        final FpvControllerInput.DebugState debug = FpvControllerInput.getLastDebugState();
        DIAGNOSTIC_LOGGER.info(
            "[FPV-DIAG/client] screen={} keys[jump={} sneak={} up={} down={} left={} right={} yawL={} yawR={}] "
                + "ctrl[active={} present={} hasThrottle={} p={} r={} y={} t={} armClick={}] "
                + "device[enabled={} mode={} jid={} connected={} name={} calReady={} calMatches={} armBinding={}] "
                + "sent[p={} r={} y={} mp={} mr={} throttle={} arm={}] drone[armed={} battery={} thrust={} vy={} vsEst={}]",
            minecraft.screen == null ? "none" : minecraft.screen.getClass().getSimpleName(),
            jumpDown,
            minecraft.options.keyShift.isDown(),
            minecraft.options.keyUp.isDown(),
            minecraft.options.keyDown.isDown(),
            minecraft.options.keyLeft.isDown(),
            minecraft.options.keyRight.isDown(),
            FPV_YAW_LEFT.isDown(),
            FPV_YAW_RIGHT.isDown(),
            controllerActive,
            controllerState.present(),
            controllerState.hasThrottle(),
            fmt(controllerState.pitch()),
            fmt(controllerState.roll()),
            fmt(controllerState.yaw()),
            fmt(controllerState.throttle()),
            controllerState.armClicked(),
            debug == null ? "?" : String.valueOf(debug.inputEnabled()),
            debug == null ? "?" : debug.mode(),
            debug == null ? "?" : String.valueOf(debug.joystickId()),
            debug == null ? "?" : String.valueOf(debug.connectedControllers()),
            debug == null ? "?" : debug.joystickName(),
            debug == null ? "?" : String.valueOf(debug.calibrationReady()),
            debug == null ? "?" : String.valueOf(debug.calibrationMatches()),
            debug == null ? "?" : debug.armBinding(),
            fmt(pitchInput),
            fmt(rollInput),
            fmt(yawInput),
            fmt(mousePitchDelta),
            fmt(mouseRollDelta),
            fmt(throttleDemand),
            armAction,
            drone.isArmed(),
            drone.getBatteryTicks(),
            fmt(drone.getThrust()),
            fmt((float) drone.getDeltaMovement().y),
            fmt(verticalSpeedEstimate)
        );
    }

    private static String fmt(final float value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static void suppressSpectatorHotbarKeys(final Minecraft minecraft) {
        if (minecraft == null || minecraft.options == null) {
            return;
        }
        for (int i = 0; i < 9; i++) {
            final KeyMapping mapping = minecraft.options.keyHotbarSlots[i];
            if (mapping == null) {
                continue;
            }
            mapping.setDown(false);
            while (mapping.consumeClick()) {
                // drain queued presses
            }
        }
    }

    private static void ensureFirstPerson(final Minecraft minecraft) {
        if (minecraft == null || minecraft.options == null) {
            return;
        }
        if (forcedFirstPerson) {
            return;
        }
        previousCameraType = minecraft.options.getCameraType();
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        forcedFirstPerson = true;
    }

    private static void restoreCameraType() {
        if (!forcedFirstPerson) {
            return;
        }
        forcedFirstPerson = false;
        final CameraType restore = previousCameraType;
        previousCameraType = null;

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) {
            return;
        }
        if (restore != null) {
            minecraft.options.setCameraType(restore);
        }
    }

    private static void ensureFpvFov(final Minecraft minecraft) {
        if (minecraft == null || minecraft.options == null) {
            return;
        }
        if (!FullfudClientConfig.CLIENT.fpvCameraForceFov.get()) {
            restoreFov();
            return;
        }
        if (forcedFov) {
            return;
        }
        previousFov = minecraft.options.fov().get();
        minecraft.options.fov().set(FullfudClientConfig.CLIENT.fpvCameraFov.get());
        forcedFov = true;
    }

    private static void restoreFov() {
        if (!forcedFov) {
            return;
        }
        forcedFov = false;
        final Integer restore = previousFov;
        previousFov = null;

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) {
            return;
        }
        if (restore != null) {
            minecraft.options.fov().set(restore);
        }
    }

    private static void handleCalibration(final Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        final FpvControllerInput.RawJoystickState rawState = FpvControllerInput.snapshotRawState();
        final boolean controllerNow = rawState != null;

        if (controllerNow
                && !lastControllerPresent
                && rawState != null
                && !controllerCalibration.isReady()
                && !(minecraft.screen instanceof ControllerCalibrationScreen)) {
            openCalibrationScreen(minecraft);
        }
        lastControllerPresent = controllerNow;

        if (FPV_CALIBRATE.consumeClick() && !(minecraft.screen instanceof ControllerCalibrationScreen)) {
            openCalibrationScreen(minecraft);
        }
    }

    private static void openCalibrationScreen(final Minecraft minecraft) {
        if (minecraft == null) {
            return;
        }
        minecraft.setScreen(new ControllerCalibrationScreen(controllerCalibration));
    }

    public static ControllerCalibration getCalibration() {
        return controllerCalibration;
    }

    private static void resetState() {
        final Minecraft minecraft = Minecraft.getInstance();
        final boolean shouldRestoreCamera = minecraft != null
            && (minecraft.getCameraEntity() instanceof FpvDroneEntity || activeDrone != null);
        restoreCameraType();
        restoreFov();
        if (inFpvMode) {
            inFpvMode = false;
            releaseFpvChain();
        }
        stopActiveDroneAudio();
        FpvSoundHandler.clear();
        if (shouldRestoreCamera) {
            forceCameraToPlayer();
        }
        restoreLocalPlayerState();
        activeDrone = null;
        invalidateResolvedDroneCache();
        throttleDemand = 0.0F;
        throttleDisplayMax = 1.0F;
        controllerThrottleBaseline = null;
        keyPitchSmoothed = 0.0F;
        keyYawSmoothed = 0.0F;
        keyRollSmoothed = 0.0F;
        verticalSpeedEstimate = 0.0F;
        verticalSpeedDroneId = null;
        escRequested = false;
        releaseSent = false;
        distanceToPilot = 0;
        lastResolvedCameraYaw = 0.0F;
        invalidateSmoothedCameraState();
        mouseInitialized = false;
    }

    private static void stopActiveDroneAudio() {
        if (activeDrone == null) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null) {
            return;
        }
        for (final var entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof FpvDroneEntity drone)) {
                continue;
            }
            if (!activeDrone.equals(drone.getUUID())) {
                continue;
            }
            drone.stopClientSound();
            break;
        }
    }

    private static void forceCameraToPlayer() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        if (minecraft.getCameraEntity() != minecraft.player) {
            minecraft.setCameraEntity(minecraft.player);
        }
        tryUpdateSoundListener(minecraft);
    }

    private static void tryUpdateSoundListener(final Minecraft minecraft) {
        if (minecraft == null) {
            return;
        }
        try {
            minecraft.getSoundManager().updateSource(minecraft.gameRenderer.getMainCamera());
        } catch (Throwable ignored) {
        }
        try {
            minecraft.getSoundManager().resume();
        } catch (Throwable ignored) {
        }
    }

    private static void ensureDroneCamera(final Minecraft minecraft, final FpvDroneEntity drone) {
        if (minecraft == null || minecraft.player == null || drone == null) {
            return;
        }
        if (minecraft.getCameraEntity() != drone) {
            minecraft.setCameraEntity(drone);
            tryUpdateSoundListener(minecraft);
        }
    }

    private static boolean shouldUsePostShader() {
        return FullfudClientConfig.CLIENT.fpvPostShaderEnabled.get() && !OPTIFINE_PRESENT;
    }

    /**
     * True only once the controller has actually moved. The stick axes are self-centring, so a nonzero
     * value there is proof of input, but the throttle channel is not: a raw joystick resting at axis 0 reads
     * 0.5 after {@code axisToThrottle01}, and a transmitter resting at idle reads 0.0 once calibrated. The
     * old test compared it against a neutral derived from {@code gamepadThrottleMode} — which describes the
     * unused gamepad path, and with its {@code RIGHT_TRIGGER} default claimed a neutral of 0.0. A joystick
     * that merely existed therefore looked active at 0.5, and with {@code camera.controllerPriority} on that
     * pinned the throttle near half while replacing keyboard pitch/roll/yaw with the idle stick's zeros:
     * a drone that climbs and answers nothing. So the reference is now the first sample of the session, and
     * a device that never moves never becomes active.
     */
    private static boolean isControllerInputActive(final FpvControllerInput.State state) {
        if (state == null || !state.present()) {
            controllerThrottleBaseline = null;
            return false;
        }
        final float axisThreshold = 0.03F;
        if (Math.abs(state.pitch()) > axisThreshold || Math.abs(state.roll()) > axisThreshold || Math.abs(state.yaw()) > axisThreshold) {
            return true;
        }
        if (state.hasThrottle()) {
            final float throttle = Mth.clamp(state.throttle(), 0.0F, 1.0F);
            if (controllerThrottleBaseline == null) {
                controllerThrottleBaseline = throttle;
            } else if (Math.abs(throttle - controllerThrottleBaseline) > axisThreshold) {
                return true;
            }
        }
        if (state.armClicked()) {
            return true;
        }
        return false;
    }

    private static boolean isClassPresent(final String className) {
        try {
            Class.forName(className, false, FpvClientHandler.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static float axis(final boolean positive, final boolean negative) {
        final float pos = positive ? 1.0F : 0.0F;
        final float neg = negative ? 1.0F : 0.0F;
        return Mth.clamp(pos - neg, -1.0F, 1.0F);
    }

    private static void invalidateSmoothedCameraState() {
        smoothedCameraInitialized = false;
        smoothedCameraDroneId = null;
        smoothedCameraX = 0.0D;
        smoothedCameraY = 0.0D;
        smoothedCameraZ = 0.0D;
        smoothedCameraQuaternion.identity();
        targetCameraQuaternion.identity();
    }

    private static void updateSmoothedCameraState(final FpvDroneEntity drone, final float partialTick) {
        if (drone == null) {
            invalidateSmoothedCameraState();
            return;
        }

        final float clampedPartial = Mth.clamp(partialTick, 0.0F, 1.0F);
        targetCameraQuaternion.set(drone.getCameraQuaternion(clampedPartial));
        if (!Float.isFinite(targetCameraQuaternion.x)
            || !Float.isFinite(targetCameraQuaternion.y)
            || !Float.isFinite(targetCameraQuaternion.z)
            || !Float.isFinite(targetCameraQuaternion.w)) {
            return;
        }
        targetCameraQuaternion.normalize();

        final Vec3 targetPosition = drone.getEyePosition(clampedPartial);
        final UUID droneId = drone.getUUID();
        final Quaternionf previousSmoothQuaternion = CAMERA_SMOOTHING_DEBUG_ENABLED
            ? new Quaternionf(smoothedCameraQuaternion)
            : null;
        final double previousSmoothX = smoothedCameraX;
        final double previousSmoothY = smoothedCameraY;
        final double previousSmoothZ = smoothedCameraZ;
        if (!smoothedCameraInitialized || !Objects.equals(smoothedCameraDroneId, droneId)) {
            smoothedCameraQuaternion.set(targetCameraQuaternion);
            smoothedCameraDroneId = droneId;
            smoothedCameraInitialized = true;
            smoothedCameraX = targetPosition.x;
            smoothedCameraY = targetPosition.y;
            smoothedCameraZ = targetPosition.z;
            logCameraSmoothingDebug(
                drone,
                targetPosition,
                previousSmoothQuaternion,
                previousSmoothX,
                previousSmoothY,
                previousSmoothZ,
                1.0F,
                1.0F,
                "init",
                clampedPartial
            );
            return;
        }

        float dot = smoothedCameraQuaternion.x * targetCameraQuaternion.x
            + smoothedCameraQuaternion.y * targetCameraQuaternion.y
            + smoothedCameraQuaternion.z * targetCameraQuaternion.z
            + smoothedCameraQuaternion.w * targetCameraQuaternion.w;
        float alpha = 1.0F;
        String mode = "snap";
        if (dot < 0.0F) {
            targetCameraQuaternion.mul(-1.0F);
            dot = -dot;
        }

        if (!Float.isFinite(dot) || dot < CAMERA_ROTATION_SNAP_DOT) {
            smoothedCameraQuaternion.set(targetCameraQuaternion);
        } else {
            alpha = CAMERA_ROTATION_SMOOTH_ALPHA;
            mode = "slerp";
            smoothedCameraQuaternion.slerp(targetCameraQuaternion, alpha);
            smoothedCameraQuaternion.normalize();
        }

        final double dx = targetPosition.x - smoothedCameraX;
        final double dy = targetPosition.y - smoothedCameraY;
        final double dz = targetPosition.z - smoothedCameraZ;
        final double distSq = dx * dx + dy * dy + dz * dz;
        if (!Double.isFinite(distSq) || distSq > CAMERA_POSITION_SNAP_DISTANCE_SQR) {
            smoothedCameraX = targetPosition.x;
            smoothedCameraY = targetPosition.y;
            smoothedCameraZ = targetPosition.z;
        } else {
            smoothedCameraX = Mth.lerp(CAMERA_POSITION_SMOOTH_ALPHA, smoothedCameraX, targetPosition.x);
            smoothedCameraY = Mth.lerp(CAMERA_POSITION_SMOOTH_ALPHA, smoothedCameraY, targetPosition.y);
            smoothedCameraZ = Mth.lerp(CAMERA_POSITION_SMOOTH_ALPHA, smoothedCameraZ, targetPosition.z);
        }

        logCameraSmoothingDebug(
            drone,
            targetPosition,
            previousSmoothQuaternion,
            previousSmoothX,
            previousSmoothY,
            previousSmoothZ,
            dot,
            alpha,
            mode,
            clampedPartial
        );
    }

    private static CameraAngles resolveCameraAnglesFromQuaternion(final Quaternionf quaternion, final float fallbackYaw) {
        if (!hasFiniteQuaternion(quaternion)) {
            return new CameraAngles(Mth.wrapDegrees(fallbackYaw), 0.0F, 0.0F);
        }
        cameraForwardScratch.set(0.0F, 0.0F, 1.0F);
        cameraUpScratch.set(0.0F, 1.0F, 0.0F);
        final Quaternionf orientation = cameraAnglesQuaternionScratch.set(quaternion);
        orientation.transform(cameraForwardScratch);
        orientation.transform(cameraUpScratch);

        final double horiz = Math.sqrt(
            cameraForwardScratch.x * cameraForwardScratch.x
                + cameraForwardScratch.z * cameraForwardScratch.z
        );
        final float pitch = (float) Math.toDegrees(Math.atan2(-cameraForwardScratch.y, horiz));

        float yaw = fallbackYaw;
        if (horiz > CAMERA_YAW_SINGULARITY_HORIZ_EPS && Math.abs(pitch) < CAMERA_YAW_SINGULARITY_PITCH_DEG) {
            yaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-cameraForwardScratch.x, cameraForwardScratch.z)));
        }

        zeroRollQuaternionScratch.rotationYXZ(
            (float) Math.toRadians(-yaw),
            (float) Math.toRadians(pitch),
            0.0F
        );
        zeroRollUpScratch.set(0.0F, 1.0F, 0.0F);
        zeroRollQuaternionScratch.transform(zeroRollUpScratch);

        rollCrossScratch.set(zeroRollUpScratch).cross(cameraUpScratch);
        float roll = (float) Math.toDegrees(Math.atan2(
            rollCrossScratch.dot(cameraForwardScratch),
            zeroRollUpScratch.dot(cameraUpScratch)
        ));
        if (!Float.isFinite(roll)) {
            roll = 0.0F;
        }

        return new CameraAngles(
            Mth.wrapDegrees(yaw),
            Mth.wrapDegrees(pitch),
            Mth.wrapDegrees(roll)
        );
    }

    private static CameraAngles resolveSmoothedCameraOrientation(final float fallbackYaw) {
        if (!smoothedCameraInitialized) {
            return new CameraAngles(fallbackYaw, 0.0F, 0.0F);
        }
        return resolveCameraAnglesFromQuaternion(smoothedCameraQuaternion, fallbackYaw);
    }

    static Quaternionf resolveRenderCameraQuaternion(final FpvDroneEntity drone, final float partialTick) {
        if (drone == null) {
            return new Quaternionf();
        }
        if (smoothedCameraInitialized
            && Objects.equals(smoothedCameraDroneId, drone.getUUID())
            && hasFiniteQuaternion(smoothedCameraQuaternion)) {
            return new Quaternionf(smoothedCameraQuaternion);
        }
        return drone.getCameraQuaternion(Mth.clamp(partialTick, 0.0F, 1.0F));
    }

    static Vec3 resolveRenderCameraPosition(final FpvDroneEntity drone, final float partialTick) {
        if (drone == null) {
            return Vec3.ZERO;
        }
        if (smoothedCameraInitialized
            && Objects.equals(smoothedCameraDroneId, drone.getUUID())
            && Double.isFinite(smoothedCameraX)
            && Double.isFinite(smoothedCameraY)
            && Double.isFinite(smoothedCameraZ)) {
            return new Vec3(smoothedCameraX, smoothedCameraY, smoothedCameraZ);
        }
        return drone.getEyePosition(Mth.clamp(partialTick, 0.0F, 1.0F));
    }

    static CameraAngles resolveRenderCameraAngles(final FpvDroneEntity drone, final float partialTick) {
        if (drone == null) {
            return new CameraAngles(0.0F, 0.0F, 0.0F);
        }

        final float clampedPartial = Mth.clamp(partialTick, 0.0F, 1.0F);
        float fallbackYaw = drone.getVisualYaw(clampedPartial);
        if (!Float.isFinite(fallbackYaw)) {
            fallbackYaw = lastResolvedCameraYaw;
        }

        final CameraAngles smoothedOrientation = resolveSmoothedCameraOrientation(fallbackYaw);
        if (smoothedCameraInitialized
            && Objects.equals(smoothedCameraDroneId, drone.getUUID())
            && Float.isFinite(smoothedOrientation.yaw())
            && Float.isFinite(smoothedOrientation.pitch())
            && Float.isFinite(smoothedOrientation.roll())) {
            return new CameraAngles(
                Mth.wrapDegrees(smoothedOrientation.yaw()),
                Mth.wrapDegrees(smoothedOrientation.pitch()),
                Mth.wrapDegrees(smoothedOrientation.roll())
            );
        }

        float yaw = fallbackYaw;
        float pitch = drone.getVisualPitch(clampedPartial);
        float roll = drone.getVisualRoll(clampedPartial);
        if (!Float.isFinite(yaw)) {
            yaw = drone.getYRot();
        }
        if (!Float.isFinite(pitch)) {
            pitch = drone.getXRot();
        }
        if (!Float.isFinite(roll)) {
            roll = drone.getCameraRoll(clampedPartial);
        }
        return new CameraAngles(
            Mth.wrapDegrees(yaw),
            Mth.wrapDegrees(pitch),
            Mth.wrapDegrees(roll)
        );
    }

    private static boolean hasFiniteQuaternion(final Quaternionf quaternion) {
        return quaternion != null
            && Float.isFinite(quaternion.x)
            && Float.isFinite(quaternion.y)
            && Float.isFinite(quaternion.z)
            && Float.isFinite(quaternion.w);
    }

    private static void logCameraSmoothingDebug(
        final FpvDroneEntity drone,
        final Vec3 targetPosition,
        final Quaternionf previousSmoothQuaternion,
        final double previousSmoothX,
        final double previousSmoothY,
        final double previousSmoothZ,
        final float dot,
        final float alpha,
        final String mode,
        final float partialTick
    ) {
        if (!CAMERA_SMOOTHING_DEBUG_ENABLED || previousSmoothQuaternion == null) {
            return;
        }
        final CameraAngles targetAngles = resolveCameraAnglesFromQuaternion(targetCameraQuaternion, drone.getYRot());
        final CameraAngles previousAngles = resolveCameraAnglesFromQuaternion(previousSmoothQuaternion, targetAngles.yaw());
        final CameraAngles smoothAngles = resolveCameraAnglesFromQuaternion(smoothedCameraQuaternion, targetAngles.yaw());

        final float stepYaw = Mth.wrapDegrees(smoothAngles.yaw() - previousAngles.yaw());
        final float stepPitch = Mth.wrapDegrees(smoothAngles.pitch() - previousAngles.pitch());
        final float stepRoll = Mth.wrapDegrees(smoothAngles.roll() - previousAngles.roll());
        final float remainYaw = Mth.wrapDegrees(targetAngles.yaw() - smoothAngles.yaw());
        final float remainPitch = Mth.wrapDegrees(targetAngles.pitch() - smoothAngles.pitch());
        final float remainRoll = Mth.wrapDegrees(targetAngles.roll() - smoothAngles.roll());

        final double stepDx = smoothedCameraX - previousSmoothX;
        final double stepDy = smoothedCameraY - previousSmoothY;
        final double stepDz = smoothedCameraZ - previousSmoothZ;
        final double stepDist = Math.sqrt(stepDx * stepDx + stepDy * stepDy + stepDz * stepDz);
        final double remainDx = targetPosition.x - smoothedCameraX;
        final double remainDy = targetPosition.y - smoothedCameraY;
        final double remainDz = targetPosition.z - smoothedCameraZ;
        final double remainDist = Math.sqrt(remainDx * remainDx + remainDy * remainDy + remainDz * remainDz);

        CAMERA_SMOOTHING_LOGGER.info(
            "FPV_CAM dbg drone={} mode={} dot={} alpha={} pt={} target_ypr=[{}, {}, {}] smooth_ypr=[{}, {}, {}] step_ypr=[{}, {}, {}] left_ypr=[{}, {}, {}] step_pos=[{}, {}, {}, d={}] left_pos=[{}, {}, {}, d={}]",
            drone.getUUID(),
            mode,
            fmt(dot),
            fmt(alpha),
            fmt(partialTick),
            fmt(targetAngles.yaw()),
            fmt(targetAngles.pitch()),
            fmt(targetAngles.roll()),
            fmt(smoothAngles.yaw()),
            fmt(smoothAngles.pitch()),
            fmt(smoothAngles.roll()),
            fmt(stepYaw),
            fmt(stepPitch),
            fmt(stepRoll),
            fmt(remainYaw),
            fmt(remainPitch),
            fmt(remainRoll),
            fmt(stepDx),
            fmt(stepDy),
            fmt(stepDz),
            fmt(stepDist),
            fmt(remainDx),
            fmt(remainDy),
            fmt(remainDz),
            fmt(remainDist)
        );
    }

    private static String fmt(final double value) {
        if (!Double.isFinite(value)) {
            return "nan";
        }
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static void trySetCameraPosition(final net.minecraft.client.Camera camera, final Vec3 position) {
        if (camera == null || position == null) {
            return;
        }
        if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)) {
            return;
        }
        try {
            if (cameraSetPositionMethodCache == null) {
                cameraSetPositionMethodCache = net.minecraft.client.Camera.class.getDeclaredMethod(
                    "setPosition",
                    double.class,
                    double.class,
                    double.class
                );
                cameraSetPositionMethodCache.setAccessible(true);
            }
            cameraSetPositionMethodCache.invoke(camera, position.x, position.y, position.z);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Called from {@code GameRendererMixin} right after {@code Camera.setup}, where Forge fired
     * {@code ViewportEvent.ComputeCameraAngles}. The angles are read and written through
     * {@link ViewportAngles} instead of the event; the camera position is still set by reflection.
     */
    public static void onCameraAngles(final Camera camera, final float partialTick, final ViewportAngles angles) {
        final Minecraft minecraft = Minecraft.getInstance();
        final FpvDroneEntity drone = resolveActiveControlledDrone(minecraft);
        if (!isFpvActive(minecraft, drone)) {
            invalidateSmoothedCameraState();
            return;
        }

        final CameraAngles orientation = resolveRenderCameraAngles(drone, partialTick);
        float yaw = orientation.yaw();
        final float pitch = orientation.pitch();
        final float roll = orientation.roll();

        if (!Float.isFinite(yaw)) {
            yaw = lastResolvedCameraYaw;
        } else {
            lastResolvedCameraYaw = yaw;
        }

        trySetCameraPosition(camera, resolveRenderCameraPosition(drone, partialTick));

        final CameraType cameraType = minecraft != null && minecraft.options != null
            ? minecraft.options.getCameraType()
            : CameraType.FIRST_PERSON;

        if (cameraType == CameraType.THIRD_PERSON_FRONT) {
            angles.setYaw(Mth.wrapDegrees(yaw + 180.0F));
            angles.setPitch(-pitch);
            angles.setRoll(-roll);
            return;
        }

        angles.setYaw(yaw);
        angles.setPitch(pitch);
        angles.setRoll(roll);
    }

    static record CameraAngles(float yaw, float pitch, float roll) {
    }

    /** Called from {@code GuiMixin}, in place of the former {@code RenderGuiEvent.Post} listener. */
    public static void onRenderGui(final GuiGraphics graphics) {
        if (!FullfudClientConfig.CLIENT.fpvHudEnabled.get()) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) return;
        final FpvDroneEntity drone = resolveActiveControlledDrone(minecraft);
        if (drone == null) return;
        if (!isFpvActive(minecraft, drone)) {
            return;
        }

        FpvOsdHudRenderer.render(graphics, minecraft, drone, speedMs, groundSpeedKmh, distanceToPilot, throttleDisplayMax);
    }

    private static int displayedPowerPercent(final FpvDroneEntity drone) {
        if (drone == null) {
            return 0;
        }
        final float displayMax = Math.max(throttleDisplayMax, 0.01F);
        final float normalizedPower = Mth.clamp(drone.getThrust() / displayMax, 0.0F, 1.0F);
        return Mth.floor(normalizedPower * 100.0F);
    }

    private static void renderControllerDebug(final GuiGraphics graphics, final Font font, final int width, final int height) {
        final FpvControllerInput.DebugState debug = FpvControllerInput.getLastDebugState();
        if (debug == null) {
            return;
        }

        final String[] lines = {
            "CTRL dbg",
            "Ввод: " + (debug.inputEnabled() ? "вкл" : "выкл") + " | режим: " + debug.mode(),
            "Пульт: " + (debug.joystickName().isBlank() ? "-" : debug.joystickName()),
            "Калибровка: " + (debug.calibrationControllerName().isBlank() ? "-" : debug.calibrationControllerName()),
            "Совпадение: " + (debug.calibrationMatches() ? "да" : "нет") + " | готова: " + (debug.calibrationReady() ? "да" : "нет"),
            "Подключено: " + debug.connectedControllers() + " | jid: " + debug.joystickId(),
            "Взвод: " + debug.armBinding() + " | pressed=" + debug.armPressed() + " | click=" + debug.armClicked(),
            String.format("P %.2f | R %.2f | Y %.2f | T %.2f", debug.pitch(), debug.roll(), debug.yaw(), debug.throttle())
        };

        int maxWidth = 0;
        for (final String line : lines) {
            maxWidth = Math.max(maxWidth, font.width(line));
        }

        final int padding = 4;
        final int lineHeight = 10;
        final int boxWidth = maxWidth + padding * 2;
        final int boxHeight = lines.length * lineHeight + padding * 2;
        final int boxX = width - boxWidth - 10;
        final int boxY = height - boxHeight - 40;

        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0x88000000);
        for (int i = 0; i < lines.length; i++) {
            final int color = i == 0 ? 0xFF88FF88 : 0xFFFFFFFF;
            graphics.drawString(font, lines[i], boxX + padding, boxY + padding + i * lineHeight, color, false);
        }
    }

    /**
     * Asked by {@code GuiMixin} at the head of the vanilla HUD. Forge got the same answer by cancelling an
     * unfiltered {@code RenderGuiOverlayEvent.Pre}.
     */
    public static boolean shouldHideVanillaHud() {
        if (!FullfudClientConfig.CLIENT.fpvHideVanillaHud.get()) {
            return false;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) return false;
        final FpvDroneEntity drone = resolveActiveControlledDrone(minecraft);
        if (drone == null) return false;
        return isFpvActive(minecraft, drone);
    }

    /** Asked by {@code ItemInHandRendererMixin}, in place of cancelling {@code RenderHandEvent}. */
    public static boolean shouldHideHand() {
        if (!FullfudClientConfig.CLIENT.fpvHideHand.get()) {
            return false;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) return false;
        return isOwnerFpvSessionActive(minecraft);
    }

    /**
     * Asked by {@code LevelSoundMixin}, in place of cancelling {@code PlayLevelSoundEvent.AtEntity}.
     * Silences the pilot's own body while the camera is on a drone.
     */
    public static boolean shouldCancelEntitySound(final Entity entity, final SoundSource source) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null || entity == null) {
            return false;
        }
        if (!isLocalOwnerEntity(minecraft, entity)) {
            return false;
        }
        if (!isOwnerFpvSessionActive(minecraft)) {
            return false;
        }
        return source == SoundSource.PLAYERS;
    }

    private static ResourceLocation getBatteryTexture(int percent) {
        if (percent >= 75) return BATTERY_100;
        if (percent >= 50) return BATTERY_75;
        if (percent >= 25) return BATTERY_50;
        if (percent > 0) return BATTERY_25;
        return BATTERY_0;
    }

    private static ResourceLocation getSignalTexture(int percent) {
        if (percent >= 75) return SIGNAL_100;
        if (percent >= 50) return SIGNAL_75;
        if (percent >= 25) return SIGNAL_50;
        if (percent > 0) return SIGNAL_25;
        return SIGNAL_0;
    }

    private static boolean isFpvActive(final Minecraft minecraft, final FpvDroneEntity drone) {
        if (minecraft == null || minecraft.player == null || drone == null) {
            return false;
        }
        return isDroneControlledByLocalPlayer(minecraft, drone) && hasUsableGoggles(minecraft, drone);
    }

    private static boolean isOwnerFpvSessionActive(final Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null) {
            return false;
        }
        if (minecraft.getCameraEntity() instanceof FpvDroneEntity cameraDrone) {
            return !cameraDrone.isRemoved() && cameraDrone.isAlive() && isFpvActive(minecraft, cameraDrone);
        }
        final FpvDroneEntity resolved = resolveActiveControlledDrone(minecraft);
        if (resolved != null) {
            return true;
        }
        return activeDrone != null;
    }

    private static boolean isLocalOwnerEntity(final Minecraft minecraft, final Entity entity) {
        return minecraft != null
            && minecraft.player != null
            && entity != null
            && minecraft.player.getUUID().equals(entity.getUUID());
    }

    private static boolean hasUsableGoggles(final Minecraft minecraft, final FpvDroneEntity drone) {
        final var player = minecraft.player;
        if (player == null) {
            return false;
        }
        final ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(head.getItem() instanceof com.fullfud.fullfud.common.item.FpvGogglesItem)) {
            return false;
        }
        final var linked = com.fullfud.fullfud.common.item.FpvGogglesItem.getLinked(head);
        if (linked.isEmpty()) {
            return true;
        }
        return linked.get().equals(drone.getUUID()) || isDroneControlledByLocalPlayer(minecraft, drone);
    }

    private static boolean isDroneControlledByLocalPlayer(final Minecraft minecraft, final FpvDroneEntity drone) {
        if (minecraft == null || minecraft.player == null || drone == null) {
            return false;
        }
        final UUID controller = drone.getControllerId();
        if (controller == null || !controller.equals(minecraft.player.getUUID())) {
            return false;
        }
        return drone.isRemoteControlActiveSynced()
            || minecraft.getCameraEntity() == drone
            || (activeDrone != null && activeDrone.equals(drone.getUUID()));
    }

    private static void invalidateResolvedDroneCache() {
        cachedResolvedDrone = null;
        cachedResolvedDroneGameTime = Long.MIN_VALUE;
        cachedResolvedPlayerId = null;
        cachedResolvedPreferredDroneId = null;
        cachedResolvedLevel = null;
        resolvedDroneCacheComputed = false;
    }

    private static FpvDroneEntity cacheResolvedDrone(
        final Minecraft minecraft,
        final UUID preferredDroneId,
        final FpvDroneEntity drone
    ) {
        if (minecraft == null || minecraft.level == null || minecraft.player == null) {
            invalidateResolvedDroneCache();
            return drone;
        }
        cachedResolvedDrone = drone;
        cachedResolvedDroneGameTime = minecraft.level.getGameTime();
        cachedResolvedPlayerId = minecraft.player.getUUID();
        cachedResolvedPreferredDroneId = preferredDroneId;
        cachedResolvedLevel = minecraft.level;
        resolvedDroneCacheComputed = true;
        return drone;
    }

    private static boolean isResolvedDroneCacheHit(final Minecraft minecraft, final UUID preferredDroneId) {
        if (!resolvedDroneCacheComputed || minecraft == null || minecraft.level == null || minecraft.player == null) {
            return false;
        }
        if (cachedResolvedLevel != minecraft.level || cachedResolvedDroneGameTime != minecraft.level.getGameTime()) {
            return false;
        }
        if (!Objects.equals(cachedResolvedPlayerId, minecraft.player.getUUID())) {
            return false;
        }
        if (!Objects.equals(cachedResolvedPreferredDroneId, preferredDroneId)) {
            return false;
        }
        if (cachedResolvedDrone == null) {
            return true;
        }
        return !cachedResolvedDrone.isRemoved()
            && cachedResolvedDrone.isAlive()
            && isDroneControlledByLocalPlayer(minecraft, cachedResolvedDrone);
    }

    static FpvDroneEntity resolveActiveControlledDrone(final Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null || minecraft.level == null) {
            return null;
        }

        final ItemStack head = minecraft.player.getItemBySlot(EquipmentSlot.HEAD);
        UUID preferredDroneId = null;
        if (head.getItem() instanceof com.fullfud.fullfud.common.item.FpvGogglesItem) {
            preferredDroneId = com.fullfud.fullfud.common.item.FpvGogglesItem.getLinked(head).orElse(null);
        }
        if (preferredDroneId == null) {
            preferredDroneId = activeDrone;
        }

        if (minecraft.getCameraEntity() instanceof FpvDroneEntity cameraDrone
            && !cameraDrone.isRemoved()
            && cameraDrone.isAlive()
            && isDroneControlledByLocalPlayer(minecraft, cameraDrone)) {
            return cacheResolvedDrone(minecraft, preferredDroneId, cameraDrone);
        }

        if (isResolvedDroneCacheHit(minecraft, preferredDroneId)) {
            return cachedResolvedDrone;
        }

        FpvDroneEntity fallback = null;
        for (final var entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof FpvDroneEntity drone)) {
                continue;
            }
            if (drone.isRemoved() || !drone.isAlive()) {
                continue;
            }
            if (!isDroneControlledByLocalPlayer(minecraft, drone)) {
                continue;
            }
            if (preferredDroneId != null && preferredDroneId.equals(drone.getUUID())) {
                return cacheResolvedDrone(minecraft, preferredDroneId, drone);
            }
            if (fallback == null) {
                fallback = drone;
            }
        }
        return cacheResolvedDrone(minecraft, preferredDroneId, fallback);
    }

    private static void stabilizeLocalPlayer(final net.minecraft.client.player.LocalPlayer player, final FpvDroneEntity drone) {
        if (player == null || drone == null) {
            return;
        }
        if (!localPlayerStateCaptured) {
            localPlayerSilent = player.isSilent();
            localPlayerNoGravity = player.isNoGravity();
            localPlayerStateCaptured = true;
        }
        player.setSilent(true);
        // The chunk tracking view follows the drone, so once it is more than a view distance away the client
        // has no chunks left around its own body: every collision test there finds air and the body free-falls
        // into the void, which drags the pilot-to-drone distance (and with it the signal quality) with it.
        // The server parks the body too (RemoteControlFailsafe#holdPilotBody); this keeps the client from
        // spending the whole session being teleported back.
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
    }

    private static void restoreLocalPlayerState() {
        if (!localPlayerStateCaptured) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) {
            localPlayerStateCaptured = false;
            return;
        }
        minecraft.player.setSilent(localPlayerSilent);
        minecraft.player.setNoGravity(localPlayerNoGravity);
        localPlayerStateCaptured = false;
    }
}
