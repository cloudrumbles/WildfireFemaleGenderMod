package com.wildfire.physics.sim;

public enum Arm {
    LEFT, RIGHT;

    public Arm opposite() {
        return this == LEFT ? RIGHT : LEFT;
    }
}
