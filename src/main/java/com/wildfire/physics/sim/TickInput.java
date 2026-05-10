package com.wildfire.physics.sim;

public record TickInput(
        BodyState body,
        PoseKind pose,
        SwingState swing,
        VehicleContribution vehicle,
        AppearanceConfig appearance,
        ArmorEffect armor,
        RandomSource random
) {}
