package com.wildfire.physics.sim;

/**
 * Per-tick body kinematics consumed by the simulator. The adapter is responsible
 * for resolving {@code effectiveBodyYaw} (handling vehicle-overridden yaw and the
 * "suppress rotation" cases like chickens / unsaddled horses / sitting camels)
 * before constructing this record.
 */
public record BodyState(
        Vec3 motionDelta,
        Vec3 velocity,
        float fallDistance,
        float walkAnimPosition,
        float walkAnimSpeed,
        float effectiveBodyYaw,
        float prevEffectiveBodyYaw
) {

    public static final BodyState AT_REST = new BodyState(Vec3.ZERO, Vec3.ZERO, 0f, 0f, 0f, 0f, 0f);
}
