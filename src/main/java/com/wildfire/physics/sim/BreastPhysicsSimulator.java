package com.wildfire.physics.sim;

/**
 * Pure-Java breast physics simulation. Holds mutable per-entity state and advances it
 * in fixed-step ticks driven by external input. No Minecraft or mod types reach this class.
 */
public final class BreastPhysicsSimulator {

    private static final float ARMOR_TIGHTNESS_BUST_SHRINK = 0.15f;

    // X-axis spring state.
    private float positionX, prevPositionX;
    // Y-axis spring state.
    private float positionY, prevPositionY;
    // Rotation spring state.
    private float bounceRotation, prevBounceRotation;
    // Smoothed breast size (eases toward the target derived from appearance + armor).
    private float breastSize, prevBreastSize;

    public void freezeStatic(AppearanceConfig appearance, ArmorEffect armor) {
        prevBreastSize = breastSize = computeStaticBreastSize(appearance, armor);
    }

    public Snapshot snapshot() {
        return new Snapshot(
                prevBreastSize, breastSize,
                prevPositionY, positionY,
                prevPositionX, positionX,
                prevBounceRotation, bounceRotation
        );
    }

    private static float computeStaticBreastSize(AppearanceConfig appearance, ArmorEffect armor) {
        if (!appearance.canHaveBreasts()) {
            return 0f;
        }
        float size = appearance.bustSize();
        if (!armor.overridePhysics()) {
            float tightness = clamp01(armor.tightness());
            size *= 1f - ARMOR_TIGHTNESS_BUST_SHRINK * tightness;
        }
        return size;
    }

    static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
