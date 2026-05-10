package com.wildfire.physics.sim;

public record Vec3(float x, float y, float z) {

    public static final Vec3 ZERO = new Vec3(0f, 0f, 0f);

    public Vec3 subtract(Vec3 other) {
        return new Vec3(x - other.x, y - other.y, z - other.z);
    }

    public float lengthSquared() {
        return x * x + y * y + z * z;
    }
}
