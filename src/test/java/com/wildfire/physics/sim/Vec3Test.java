package com.wildfire.physics.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Vec3Test {

    @Test
    void subtractReturnsComponentWiseDifference() {
        Vec3 a = new Vec3(5f, 7f, 9f);
        Vec3 b = new Vec3(1f, 2f, 3f);

        Vec3 result = a.subtract(b);

        assertEquals(new Vec3(4f, 5f, 6f), result);
    }

    @Test
    void lengthSquaredReturnsSumOfSquares() {
        Vec3 v = new Vec3(3f, 4f, 0f);

        assertEquals(25f, v.lengthSquared());
    }
}
