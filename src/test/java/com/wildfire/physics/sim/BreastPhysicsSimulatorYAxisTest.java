package com.wildfire.physics.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BreastPhysicsSimulatorYAxisTest {

    @Test
    void zeroBustSettlesAtZeroY() {
        BreastPhysicsSimulator sim = new BreastPhysicsSimulator();
        TickInput in = TickInputs.withAppearance(
                TickInputs.defaultInput(),
                new AppearanceConfig(true, 0f, 1f, 0.5f, true)
        );

        for (int i = 0; i < 200; i++) sim.tick(in);

        assertEquals(0f, sim.snapshot().positionY(), 1e-3f);
    }

    @Test
    void positiveBustProducesPositiveYEquilibrium() {
        BreastPhysicsSimulator sim = new BreastPhysicsSimulator();
        TickInput in = TickInputs.defaultInput(); // bustSize 1

        for (int i = 0; i < 400; i++) sim.tick(in);

        assertTrue(sim.snapshot().positionY() > 0.5f,
                "expected positionY > 0.5 from bust gravity, got " + sim.snapshot().positionY());
    }

    @Test
    void positionYClampedAboveAtOnePointFive() {
        BreastPhysicsSimulator sim = new BreastPhysicsSimulator();
        // huge bust + huge bounce mult = wants to overshoot the upper clamp
        TickInput in = TickInputs.withAppearance(
                TickInputs.defaultInput(),
                new AppearanceConfig(true, 5f, 5f, 0.9f, true)
        );

        for (int i = 0; i < 1000; i++) sim.tick(in);

        assertTrue(sim.snapshot().positionY() <= 1.5f + 1e-4f,
                "positionY should be clamped to <= 1.5, got " + sim.snapshot().positionY());
    }

    @Test
    void positionYClampedBelowAtNegativeHalf() {
        BreastPhysicsSimulator sim = new BreastPhysicsSimulator();
        TickInput up = TickInputs.withBody(
                TickInputs.defaultInput(),
                new BodyState(new Vec3(0f, 5f, 0f), Vec3.ZERO, 0f, 0f, 0f, 0f, 0f)
        );

        for (int i = 0; i < 200; i++) sim.tick(up);

        assertTrue(sim.snapshot().positionY() >= -0.5f - 1e-4f,
                "positionY should be clamped to >= -0.5, got " + sim.snapshot().positionY());
    }

    @Test
    void downwardMotionShiftsEquilibriumLowerThanNoMotion() {
        BreastPhysicsSimulator restSim = new BreastPhysicsSimulator();
        BreastPhysicsSimulator fallingSim = new BreastPhysicsSimulator();
        TickInput rest = TickInputs.defaultInput();
        TickInput falling = TickInputs.withBody(
                rest,
                new BodyState(new Vec3(0f, -0.5f, 0f), new Vec3(0f, -0.5f, 0f), 0f, 0f, 0f, 0f, 0f)
        );

        for (int i = 0; i < 100; i++) {
            restSim.tick(rest);
            fallingSim.tick(falling);
        }

        assertNotEquals(restSim.snapshot().positionY(), fallingSim.snapshot().positionY(), 1e-3f);
    }
}
