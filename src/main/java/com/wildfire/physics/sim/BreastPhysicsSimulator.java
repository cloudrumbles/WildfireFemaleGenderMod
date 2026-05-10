package com.wildfire.physics.sim;

/**
 * Pure-Java breast physics simulation. Holds mutable per-entity state and advances it
 * in fixed-step ticks driven by external input. No Minecraft or mod types reach this class.
 *
 * <p>Per tick, the simulator computes a target Y bounce and rotation from the body's motion,
 * pose, swing, and (already-resolved) vehicle nudge, then advances a damped spring toward
 * that target. The output snapshot exposes the current and previous values for renderer
 * interpolation.
 */
public final class BreastPhysicsSimulator {

    // ---- physical tunables (named constants for the magic numbers in the original) ----
    private static final float ARMOR_TIGHTNESS_BUST_SHRINK = 0.15f;
    private static final float BREAST_WEIGHT_PER_BUST     = 1.25f;
    private static final float BOUNCE_INTENSITY_FACTOR    = 9f;       // bust * mult * 9 (was 3*3 with rounding)
    private static final float UNIBOOB_JITTER_LO          = 0.5f;
    private static final float UNIBOOB_JITTER_HI          = 1.5f;
    private static final float WALK_ANIM_FREQ             = 0.6662f;
    private static final float TARGET_Y_MIN               = -1.5f;
    private static final float TARGET_Y_MAX               = 2.5f;
    private static final float ROTATION_TARGET_CLAMP      = 25f;
    private static final float POSITION_Y_MIN             = -0.5f;
    private static final float POSITION_Y_MAX             = 1.5f;
    private static final float BOUNCE_FLOOR               = -0.5f;
    private static final float BOUNCE_CEIL                = 2.5f;     // pushback engages above 2.5
    private static final float BOUNCE_CEIL_REF            = 2.65f;    // distanceFromMax is computed against 2.65
    private static final float Y_VELOCITY_GAIN            = 1.1625f;

    // X-axis spring state. The original code declared but never wrote a target for X, so this
    // axis is permanently zero; preserved in the snapshot for renderer ABI compatibility.
    private float positionX, prevPositionX;
    // Y-axis spring state.
    private float positionY, prevPositionY;
    private float bounceVelY;        // integrated bounce position before final clamp
    private float velocityY;         // spring velocity
    private float targetY;           // target bounce, refreshed each tick
    // Rotation spring state.
    private float bounceRotation, prevBounceRotation;
    private float bounceRotVel;
    private float rotVelocity;
    private float targetRot;
    // Smoothed breast size (eases toward the target derived from appearance + armor).
    private float breastSize, prevBreastSize;

    public void freezeStatic(AppearanceConfig appearance, ArmorEffect armor) {
        prevBreastSize = breastSize = effectiveBustSize(appearance, armor);
    }

    public void tick(TickInput input) {
        snapshotPrev();

        AppearanceConfig appearance = input.appearance();
        ArmorEffect armor = input.armor();
        BodyState body = input.body();

        float effectiveBust = effectiveBustSize(appearance, armor);
        breastSize = (breastSize + effectiveBust) * 0.5f;

        float breastWeight = appearance.bustSize() * BREAST_WEIGHT_PER_BUST;
        float bounceIntensity = computeBounceIntensity(effectiveBust, appearance, armor, input.random());

        targetY = body.motionDelta().y() * bounceIntensity + breastWeight;
        targetY += walkAnimContribution(body);

        targetRot = -yawDelta(body) * bounceIntensity;

        applyBoundaryPushback();
        clampTargets();
        integrateSpring(appearance.floppiness());
        clampOutputs();
    }

    public Snapshot snapshot() {
        return new Snapshot(
                prevBreastSize, breastSize,
                prevPositionY, positionY,
                prevPositionX, positionX,
                prevBounceRotation, bounceRotation
        );
    }

    // ---- per-tick steps ----

    private void snapshotPrev() {
        prevBreastSize = breastSize;
        prevPositionY = positionY;
        prevPositionX = positionX;
        prevBounceRotation = bounceRotation;
    }

    private static float computeBounceIntensity(float effectiveBust, AppearanceConfig appearance,
                                                ArmorEffect armor, RandomSource random) {
        float intensity = effectiveBust * appearance.bounceMultiplier() * BOUNCE_INTENSITY_FACTOR;
        if (!armor.overridePhysics()) {
            intensity *= 1f - clamp01(armor.physicsResistance());
        }
        if (!appearance.uniboob()) {
            intensity *= random.nextFloat(UNIBOOB_JITTER_LO, UNIBOOB_JITTER_HI);
        }
        return intensity;
    }

    private static float walkAnimContribution(BodyState body) {
        float f = body.velocity().lengthSquared() / 0.2f;
        f = Math.max(f * f * f, 1f);
        return (float) Math.cos(body.walkAnimPosition() * WALK_ANIM_FREQ + Math.PI)
                * 0.5f * body.walkAnimSpeed() * 0.5f / f;
    }

    private static float yawDelta(BodyState body) {
        return (body.effectiveBodyYaw() - body.prevEffectiveBodyYaw()) / 15f;
    }

    private void applyBoundaryPushback() {
        if (bounceVelY < BOUNCE_FLOOR) {
            targetY += Math.abs(bounceVelY - BOUNCE_FLOOR) * 0.5f;
        }
        if (bounceVelY > BOUNCE_CEIL) {
            targetY -= Math.abs(bounceVelY - BOUNCE_CEIL_REF) * 0.5f;
        }
    }

    private void clampTargets() {
        targetY = clamp(targetY, TARGET_Y_MIN, TARGET_Y_MAX);
        targetRot = clamp(targetRot, -ROTATION_TARGET_CLAMP, ROTATION_TARGET_CLAMP);
    }

    private void integrateSpring(float floppiness) {
        float bounceAmount = clamp(0.45f * (1f - floppiness) + 0.15f, 0.15f, 0.6f);
        float delta = 2.25f - bounceAmount;

        velocityY = lerp(bounceAmount, velocityY, (targetY - bounceVelY) * delta);
        bounceVelY += velocityY * floppiness * Y_VELOCITY_GAIN;

        rotVelocity = lerp(bounceAmount, rotVelocity, (targetRot - bounceRotVel) * delta);
        bounceRotVel += rotVelocity * floppiness;

        bounceRotation = bounceRotVel;
        positionY = bounceVelY;
    }

    private void clampOutputs() {
        if (positionY < POSITION_Y_MIN) {
            positionY = POSITION_Y_MIN;
        }
        if (positionY > POSITION_Y_MAX) {
            positionY = POSITION_Y_MAX;
            velocityY = 0f;
        }
    }

    // ---- helpers ----

    /** Bust size after applying armor tightness shrinkage and the gender gate. */
    private static float effectiveBustSize(AppearanceConfig appearance, ArmorEffect armor) {
        if (!appearance.canHaveBreasts()) {
            return 0f;
        }
        float size = appearance.bustSize();
        if (!armor.overridePhysics()) {
            size *= 1f - ARMOR_TIGHTNESS_BUST_SHRINK * clamp01(armor.tightness());
        }
        return size;
    }

    static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    static float lerp(float t, float a, float b) {
        return a + (b - a) * t;
    }
}
