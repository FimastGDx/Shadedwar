package com.fullfud.fullfud.common.entity.drone;

/**
 * The four charges a drone can be loaded with, and the blast each one produces.
 *
 * <p>A drone with {@link #NONE} still hurts whatever is standing next to it — the mod's own blast and
 * shrapnel model runs regardless — but it leaves the terrain alone. Everything above {@code NONE}
 * switches vanilla block damage back on at the radius below.
 *
 * <p>{@code power} is the vanilla explosion radius parameter, not a TNT count. TNT is 4.0, and the
 * volume an explosion clears grows with the cube of that number, so "as much as N sticks of TNT" is
 * {@code 4 * cbrt(N)}: two sticks is 5.0, five is 6.8, ten is 8.6. Reading the tiers as TNT counts
 * directly would have made tier 4 a 40-block crater.
 */
public enum WarheadCharge {
    NONE(0, 0.0F, false),
    /** Roughly one stick of TNT. */
    TIER_1(1, 4.0F, false),
    /** Roughly two sticks. The heaviest an FPV airframe will take. */
    TIER_2(2, 5.0F, false),
    /** Roughly five sticks. */
    TIER_3(3, 6.8F, false),
    /** Roughly ten sticks, and it starts fires. */
    TIER_4(4, 8.6F, true);

    /** The heaviest charge an FPV drone will accept; a Shahed takes any of them. */
    public static final WarheadCharge FPV_MAX = TIER_2;

    /** The heaviest charge at all, i.e. what a Shahed or an FP-5 will accept. */
    public static final WarheadCharge SHAHED_MAX = TIER_4;

    private static final WarheadCharge[] BY_TIER = { NONE, TIER_1, TIER_2, TIER_3, TIER_4 };

    private final int tier;
    private final float power;
    private final boolean incendiary;

    WarheadCharge(final int tier, final float power, final boolean incendiary) {
        this.tier = tier;
        this.power = power;
        this.incendiary = incendiary;
    }

    public int tier() {
        return this.tier;
    }

    public float power() {
        return this.power;
    }

    public boolean incendiary() {
        return this.incendiary;
    }

    public boolean isPresent() {
        return this != NONE;
    }

    /**
     * Linear multiplier on the blast and shrapnel radii in {@code DroneExplosionEffects}, and on the
     * fragment count.
     *
     * <p>{@link #NONE} is exactly 1 on purpose: the existing profiles were tuned against an unloaded
     * airframe arriving at speed, which is what a charge-less impact still is, so loading a charge only
     * ever adds. Tier 1 matches it — a stick of TNT next to the wreck of a Shahed does not measurably
     * widen the lethal radius, it just digs a hole, which is what {@link #power} is for.
     *
     * <p>The numbers are the cube roots of the lethality steps 1 / 1 / 1.25 / 1.6 / 2, for the same
     * reason {@link #power} is a cube root: a radius scaled linearly scales the volume it covers, and
     * therefore the number of things inside it, by the cube.
     */
    public float blastScale() {
        return switch (this) {
            case NONE, TIER_1 -> 1.0F;
            case TIER_2 -> 1.08F;
            case TIER_3 -> 1.17F;
            case TIER_4 -> 1.26F;
        };
    }

    public static WarheadCharge byTier(final int tier) {
        return tier >= 0 && tier < BY_TIER.length ? BY_TIER[tier] : NONE;
    }
}
