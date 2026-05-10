package com.wildfire.physics.sim;

public record Snapshot(
        float prevBreastSize, float breastSize,
        float prevPositionY, float positionY,
        float prevPositionX, float positionX,
        float prevBounceRotation, float bounceRotation
) {

    public float lerpedBreastSize(float partialTicks) {
        return lerp(partialTicks, prevBreastSize, breastSize);
    }

    public float lerpedPositionY(float partialTicks) {
        return lerp(partialTicks, prevPositionY, positionY);
    }

    public float lerpedPositionX(float partialTicks) {
        return lerp(partialTicks, prevPositionX, positionX);
    }

    public float lerpedBounceRotation(float partialTicks) {
        return lerp(partialTicks, prevBounceRotation, bounceRotation);
    }

    private static float lerp(float t, float a, float b) {
        return a + (b - a) * t;
    }
}
