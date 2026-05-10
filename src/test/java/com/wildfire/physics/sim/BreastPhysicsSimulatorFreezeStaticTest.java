package com.wildfire.physics.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BreastPhysicsSimulatorFreezeStaticTest {

    @Test
    void freezeStaticUsesBustSizeWhenArmorOverridden() {
        BreastPhysicsSimulator sim = new BreastPhysicsSimulator();
        AppearanceConfig appearance = new AppearanceConfig(true, 0.8f, 1f, 0.5f, false);
        ArmorEffect armor = new ArmorEffect(true, 1f, 0.5f);

        sim.freezeStatic(appearance, armor);

        Snapshot snap = sim.snapshot();
        assertEquals(0.8f, snap.breastSize());
        assertEquals(0.8f, snap.prevBreastSize());
    }

    @Test
    void freezeStaticShrinksBustByArmorTightnessWhenNotOverridden() {
        BreastPhysicsSimulator sim = new BreastPhysicsSimulator();
        AppearanceConfig appearance = new AppearanceConfig(true, 1f, 1f, 0.5f, false);
        ArmorEffect armor = new ArmorEffect(false, 1f, 0f);

        sim.freezeStatic(appearance, armor);

        // tightness 1 shrinks by 0.15
        assertEquals(0.85f, sim.snapshot().breastSize(), 1e-6f);
    }

    @Test
    void freezeStaticReturnsZeroWhenCannotHaveBreasts() {
        BreastPhysicsSimulator sim = new BreastPhysicsSimulator();
        AppearanceConfig appearance = new AppearanceConfig(false, 1f, 1f, 0.5f, false);

        sim.freezeStatic(appearance, ArmorEffect.NONE);

        assertEquals(0f, sim.snapshot().breastSize());
    }

    @Test
    void freezeStaticClampsTightnessToZeroOne() {
        BreastPhysicsSimulator sim = new BreastPhysicsSimulator();
        AppearanceConfig appearance = new AppearanceConfig(true, 1f, 1f, 0.5f, false);
        ArmorEffect armor = new ArmorEffect(false, 5f, 0f);

        sim.freezeStatic(appearance, armor);

        // tightness clamped to 1: 1 - 0.15 * 1 = 0.85
        assertEquals(0.85f, sim.snapshot().breastSize(), 1e-6f);
    }
}
