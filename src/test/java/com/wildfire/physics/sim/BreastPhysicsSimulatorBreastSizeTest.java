package com.wildfire.physics.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BreastPhysicsSimulatorBreastSizeTest {

    @Test
    void breastSizeMovesHalfwayToTargetEachTick() {
        BreastPhysicsSimulator sim = new BreastPhysicsSimulator();
        TickInput in = TickInputs.defaultInput(); // bustSize = 1, breastSize starts at 0

        sim.tick(in);

        assertEquals(0.5f, sim.snapshot().breastSize(), 1e-6f);
    }

    @Test
    void breastSizeConvergesToTargetOverManyTicks() {
        BreastPhysicsSimulator sim = new BreastPhysicsSimulator();
        TickInput in = TickInputs.defaultInput();

        for (int i = 0; i < 30; i++) sim.tick(in);

        assertEquals(1f, sim.snapshot().breastSize(), 1e-3f);
    }

    @Test
    void breastSizeIsZeroWhenCannotHaveBreasts() {
        BreastPhysicsSimulator sim = new BreastPhysicsSimulator();
        TickInput base = TickInputs.defaultInput();
        TickInput in = TickInputs.withAppearance(base, new AppearanceConfig(false, 1f, 1f, 0.5f, true));

        for (int i = 0; i < 30; i++) sim.tick(in);

        assertEquals(0f, sim.snapshot().breastSize(), 1e-6f);
    }

    @Test
    void breastSizeShrinksByArmorTightnessWhenNotOverridden() {
        BreastPhysicsSimulator sim = new BreastPhysicsSimulator();
        TickInput base = TickInputs.defaultInput();
        TickInput in = TickInputs.withArmor(base, new ArmorEffect(false, 1f, 0f));

        for (int i = 0; i < 30; i++) sim.tick(in);

        // bust = 1, tightness = 1 -> target = 1 - 0.15 = 0.85
        assertEquals(0.85f, sim.snapshot().breastSize(), 1e-3f);
    }

    @Test
    void prevBreastSizeIsLastTicksValue() {
        BreastPhysicsSimulator sim = new BreastPhysicsSimulator();
        TickInput in = TickInputs.defaultInput();

        sim.tick(in); // breastSize -> 0.5
        sim.tick(in); // breastSize -> 0.75, prevBreastSize should be 0.5

        assertEquals(0.5f, sim.snapshot().prevBreastSize(), 1e-6f);
        assertEquals(0.75f, sim.snapshot().breastSize(), 1e-6f);
    }
}
