package com.fullfud.fullfud.core.data;

import com.fullfud.fullfud.FullfudMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Replacement for Forge's {@code Entity#getPersistentData()}.
 *
 * <p>The mod uses that tag as a hand-rolled session store — "this player is piloting a drone", "this
 * explosion must not break blocks" — under {@code fullfud_}-prefixed keys. Fabric has no per-entity
 * NBT bag, so one persistent data attachment holds the same {@link CompoundTag} and every former call
 * site becomes {@code PersistentData.of(entity)}. The tag is returned by reference, so the existing
 * in-place {@code putBoolean}/{@code getBoolean} usage keeps working.
 *
 * <p>{@code copyOnDeath()} is deliberately not set: these are piloting-session tags, and dying should
 * end the session — {@code RemoteControlFailsafe} handles putting the player back together.
 */
public final class PersistentData {

    public static final AttachmentType<CompoundTag> ATTACHMENT = AttachmentRegistry.<CompoundTag>builder()
        .initializer(CompoundTag::new)
        .persistent(CompoundTag.CODEC)
        .buildAndRegister(ResourceLocation.fromNamespaceAndPath(FullfudMod.MOD_ID, "persistent_data"));

    private PersistentData() {
    }

    /**
     * Forces this class to load so {@link #ATTACHMENT} reaches the registry before any world does.
     * Called from {@code FullfudMod.onInitialize()}.
     */
    public static void init() {
    }

    /** The target's mutable tag, created empty on first access. */
    public static CompoundTag of(final AttachmentTarget target) {
        return target.getAttachedOrCreate(ATTACHMENT);
    }
}
