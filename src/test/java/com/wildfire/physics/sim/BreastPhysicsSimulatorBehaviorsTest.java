package com.wildfire.physics.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BreastPhysicsSimulatorBehaviorsTest {

    // ---- rotation ----

    @Test
    void positiveYawDeltaProducesNonZeroRotation() {
        BreastPhysicsSimulator sim = new BreastPhysicsSimulator();
        BodyState rotating = new BodyState(Vec3.ZERO, Vec3.ZERO, 0f, 0f, 0f, 30f, 0f);
        TickInput in = TickInputs.withBody(TickInputs.defaultInput(), rotating);

        for (int i = 0; i < 5; i++) sim.tick(in);

        assertNotEquals(0f, sim.snapshot().bounceRotation(), 1e-4f);
    }

    @Test
    void rotationTargetIsClamped() {
        BreastPhysicsSimulator sim = new BreastPhysicsSimulator();
        // huge yaw delta + huge intensity -> target would explode without clamp
        BodyState spinning = new BodyState(Vec3.ZERO, Vec3.ZERO, 0f, 0f, 0f, 10000f, 0f);
        TickInput in = TickInputs.withBody(TickInputs.defaultInput(), spinning);

        for (int i = 0; i < 50; i++) sim.tick(in);

        // rotation tracks bounceRotVel which converges toward target (clamped to ±25);
        // bounded by the spring math, but never blows up to thousands
        assertTrue(Math.abs(sim.snapshot().bounceRotation()) < 100f,
                "rotation should be bounded, got " + sim.snapshot().bounceRotation());
    }

    // ---- pose transition ----

    @Test
    void crouchTransitionAddsToTarget() {
        BreastPhysicsSimulator standing = new BreastPhysicsSimulator();
        BreastPhysicsSimulator crouching = new BreastPhysicsSimulator();
        TickInput stand = TickInputs.defaultInput();
        TickInput crouch = TickInputs.withPose(stand, PoseKind.CROUCHING);

        // settle both for a few ticks in standing
        for (int i = 0; i < 3; i++) {
            standing.tick(stand);
            crouching.tick(stand);
        }
        // now crouching transitions
        standing.tick(stand);
        crouching.tick(crouch);

        assertNotEquals(standing.snapshot().positionY(), crouching.snapshot().positionY(), 1e-4f);
    }

    @Test
    void sleepTransitionResetsTarget() {
        BreastPhysicsSimulator sim = new BreastPhysicsSimulator();
        TickInput stand = TickInputs.defaultInput();
        TickInput sleep = TickInputs.withPose(stand, PoseKind.SLEEPING);

        for (int i = 0; i < 5; i++) sim.tick(stand);
        float before = sim.snapshot().positionY();
        sim.tick(sleep);

        assertNotEquals(before, sim.snapshot().positionY(), 1e-4f);
    }

    // ---- vehicle ----

    @Test
    void vehicleReplaceModeOverridesTarget() {
        BreastPhysicsSimulator noVehicle = new BreastPhysicsSimulator();
        BreastPhysicsSimulator withVehicle = new BreastPhysicsSimulator();
        TickInput nv = TickInputs.defaultInput();
        TickInput wv = TickInputs.withVehicle(nv, VehicleContribution.replace(0.5f, 1f));

        for (int i = 0; i < 30; i++) {
            noVehicle.tick(nv);
            withVehicle.tick(wv);
        }

        assertNotEquals(noVehicle.snapshot().positionY(), withVehicle.snapshot().positionY(), 1e-3f);
    }

    @Test
    void vehicleNoneModeLeavesTargetUnchanged() {
        BreastPhysicsSimulator a = new BreastPhysicsSimulator();
        BreastPhysicsSimulator b = new BreastPhysicsSimulator();
        TickInput in = TickInputs.defaultInput();
        TickInput inExplicitNone = TickInputs.withVehicle(in, VehicleContribution.NONE);

        for (int i = 0; i < 50; i++) {
            a.tick(in);
            b.tick(inExplicitNone);
        }

        assertEquals(a.snapshot().positionY(), b.snapshot().positionY(), 1e-6f);
    }

    @Test
    void vehicleAddModeShiftsTarget() {
        BreastPhysicsSimulator noVehicle = new BreastPhysicsSimulator();
        BreastPhysicsSimulator withVehicle = new BreastPhysicsSimulator();
        TickInput nv = TickInputs.defaultInput();
        TickInput wv = TickInputs.withVehicle(nv, VehicleContribution.add(0.5f, 0f));

        for (int i = 0; i < 20; i++) {
            noVehicle.tick(nv);
            withVehicle.tick(wv);
        }

        assertNotEquals(noVehicle.snapshot().positionY(), withVehicle.snapshot().positionY(), 1e-3f);
    }

    // ---- swing ----

    @Test
    void swingingArmInfluencesRotation() {
        BreastPhysicsSimulator notSwinging = new BreastPhysicsSimulator();
        BreastPhysicsSimulator swinging = new BreastPhysicsSimulator();
        TickInput idle = TickInputs.defaultInput();
        SwingState swingingState = new SwingState(6, 3, true, Arm.RIGHT, Arm.RIGHT, 5);
        TickInput swing = TickInputs.withSwing(idle, swingingState);

        for (int i = 0; i < 10; i++) {
            notSwinging.tick(idle);
            swinging.tick(swing);
        }

        assertNotEquals(notSwinging.snapshot().bounceRotation(), swinging.snapshot().bounceRotation(), 1e-4f);
    }

    @Test
    void swingingDoesNotRotateWhileSleeping() {
        BreastPhysicsSimulator sleeping = new BreastPhysicsSimulator();
        TickInput sleep = TickInputs.withPose(TickInputs.defaultInput(), PoseKind.SLEEPING);
        SwingState swingingState = new SwingState(6, 3, true, Arm.RIGHT, Arm.RIGHT, 5);
        TickInput sleepSwing = TickInputs.withSwing(sleep, swingingState);

        for (int i = 0; i < 10; i++) sleeping.tick(sleepSwing);

        // Sleeping suppresses swing-driven rotation; rotation should stay near zero
        // (small from yaw delta which is 0 here -> exactly zero target -> 0 rotation)
        assertEquals(0f, sleeping.snapshot().bounceRotation(), 1e-3f);
    }

    // ---- snapshot lerp ----

    @Test
    void snapshotLerpsHalfwayBetweenPrevAndCurrent() {
        Snapshot snap = new Snapshot(0f, 1f, 0f, 0.4f, 0f, 0f, 0f, 10f);

        assertEquals(0.5f, snap.lerpedBreastSize(0.5f), 1e-6f);
        assertEquals(0.2f, snap.lerpedPositionY(0.5f), 1e-6f);
        assertEquals(5f, snap.lerpedBounceRotation(0.5f), 1e-6f);
    }
}
