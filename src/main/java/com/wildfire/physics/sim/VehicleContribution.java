package com.wildfire.physics.sim;

/**
 * Pre-computed bounce nudge supplied by the adapter for vehicles whose visible motion
 * isn't captured by the rider's regular delta movement (boats, minecarts, pigs, horses,
 * striders). When {@link #appliesThisTick} is false, the simulator ignores the nudge.
 */
public record VehicleContribution(float bounceYNudge, boolean appliesThisTick) {

    public static final VehicleContribution NONE = new VehicleContribution(0f, false);
}
