package com.wildfire.physics.sim;

public record ArmorEffect(boolean overridePhysics, float tightness, float physicsResistance) {

    public static final ArmorEffect NONE = new ArmorEffect(true, 0f, 0f);
}
