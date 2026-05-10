package com.wildfire.physics.sim;

/**
 * Per-tick bounce contribution from a vehicle whose visible motion isn't captured by the
 * rider's regular delta movement (boats, minecarts, pigs, horses, striders). The adapter
 * pre-computes the per-vehicle math each tick and expresses the result as factors of the
 * simulator's own {@code bounceIntensity} and {@code breastWeight}, then chooses how the
 * simulator should apply it.
 *
 * <ul>
 *   <li>{@link Mode#NONE} — no contribution this tick.</li>
 *   <li>{@link Mode#REPLACE} — overwrite the Y target with
 *       {@code intensityFactor * bounceIntensity + weightFactor * breastWeight}.</li>
 *   <li>{@link Mode#ADD} — add {@code intensityFactor * bounceIntensity + weightFactor * breastWeight}
 *       to the Y target.</li>
 * </ul>
 */
public record VehicleContribution(Mode mode, float intensityFactor, float weightFactor) {

    public enum Mode { NONE, REPLACE, ADD }

    public static final VehicleContribution NONE = new VehicleContribution(Mode.NONE, 0f, 0f);

    public static VehicleContribution replace(float intensityFactor, float weightFactor) {
        return new VehicleContribution(Mode.REPLACE, intensityFactor, weightFactor);
    }

    public static VehicleContribution add(float intensityFactor, float weightFactor) {
        return new VehicleContribution(Mode.ADD, intensityFactor, weightFactor);
    }
}
