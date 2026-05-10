package com.wildfire.physics.sim;

/** Deterministic RandomSource for tests. nextBoolean cycles, nextFloat returns the midpoint. */
final class TestRandom implements RandomSource {

    private boolean next = true;

    @Override
    public boolean nextBoolean() {
        boolean v = next;
        next = !next;
        return v;
    }

    @Override
    public float nextFloat(float lo, float hi) {
        return (lo + hi) * 0.5f;
    }
}
