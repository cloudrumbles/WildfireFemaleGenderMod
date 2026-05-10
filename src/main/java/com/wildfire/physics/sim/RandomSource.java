package com.wildfire.physics.sim;

/**
 * Source of randomness used by the simulator. Kept as an interface so tests can supply
 * deterministic implementations and the production adapter can wrap the host RNG.
 */
public interface RandomSource {

    boolean nextBoolean();

    /** @return uniform float in {@code [lo, hi)}. */
    float nextFloat(float lo, float hi);
}
