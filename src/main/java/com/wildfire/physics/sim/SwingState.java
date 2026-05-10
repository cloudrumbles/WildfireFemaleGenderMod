package com.wildfire.physics.sim;

public record SwingState(
        int swingDuration,
        int swingTime,
        boolean swinging,
        Arm swingingArm,
        Arm mainArm,
        int tickCount
) {

    public static final SwingState IDLE = new SwingState(0, 0, false, Arm.RIGHT, Arm.RIGHT, 0);
}
