/*
 * Example implementation of improved physics model for BreastPhysics.java
 *
 * This file demonstrates how to implement a more physically accurate
 * spring-damper system for the breast physics simulation.
 *
 * Key improvements:
 * 1. Standard spring-damper equations
 * 2. Proper force accumulation
 * 3. Named constants instead of magic numbers
 * 4. Soft position clamping
 * 5. Velocity damping
 */

package com.wildfire.physics;

import net.minecraft.util.math.MathHelper;

public class ImprovedPhysicsExample {

    // ========== PHYSICS CONSTANTS ==========

    /**
     * Controls how quickly rotation responds to body yaw changes.
     * Lower values = less sensitive rotation.
     */
    private static final float ROTATION_SENSITIVITY = 1f / 15f;

    /**
     * Minimum spring responsiveness (when floppiness = 1, most floppy)
     */
    private static final float MIN_SPRING_RESPONSE = 0.15f;

    /**
     * Maximum spring responsiveness (when floppiness = 0, most firm)
     */
    private static final float MAX_SPRING_RESPONSE = 0.60f;

    /**
     * Spring constant range (controls oscillation frequency)
     * Higher = stiffer spring = faster oscillations
     */
    private static final float MIN_SPRING_CONSTANT = 0.5f;
    private static final float MAX_SPRING_CONSTANT = 3.0f;

    /**
     * Damping coefficient range (controls oscillation decay)
     * Higher = more damping = faster settling
     */
    private static final float MIN_DAMPING = 0.1f;
    private static final float MAX_DAMPING = 0.8f;

    /**
     * Simulated mass (affects how forces translate to acceleration)
     * Higher mass = slower response to forces
     */
    private static final float BREAST_MASS = 1.0f;

    /**
     * Air resistance applied to velocity each frame
     * Values < 1.0 slow down motion over time
     */
    private static final float AIR_RESISTANCE = 0.985f;

    /**
     * Multiplier for rotation damping relative to translation damping
     */
    private static final float ROTATION_DAMPING_MULT = 0.7f;

    /**
     * Multiplier for X-axis damping relative to Y-axis damping
     */
    private static final float X_AXIS_DAMPING_MULT = 0.85f;

    /**
     * Stiffness for soft position clamping
     * Higher values = harder clamp (closer to hard limit)
     */
    private static final float CLAMP_STIFFNESS = 3.0f;

    /**
     * Position limits for Y-axis
     */
    private static final float MIN_POSITION_Y = -0.5f;
    private static final float MAX_POSITION_Y = 1.5f;

    /**
     * Velocity damping when approaching limits
     */
    private static final float LIMIT_VELOCITY_DAMP = 0.5f;

    /**
     * Distance threshold from limit to start applying velocity damping
     */
    private static final float LIMIT_THRESHOLD = 0.1f;

    // ========== STATE VARIABLES ==========

    // Y-Axis (vertical)
    private float positionY = 0;
    private float velocityY = 0;
    private float prePositionY = 0;

    // X-Axis (horizontal)
    private float positionX = 0;
    private float velocityX = 0;
    private float prePositionX = 0;

    // Rotation
    private float rotation = 0;
    private float rotationVelocity = 0;
    private float preRotation = 0;

    // Rest positions (equilibrium points)
    private float restPositionY = 0;
    private float restPositionX = 0;
    private float restRotation = 0;

    // ========== HELPER METHODS ==========

    /**
     * Calculate spring constant from floppiness parameter
     * @param floppiness 0 (firm) to 1 (floppy)
     * @return Spring constant value
     */
    private float calculateSpringConstant(float floppiness) {
        // Inverse relationship: low floppiness = high spring constant
        return MathHelper.lerp(floppiness, MAX_SPRING_CONSTANT, MIN_SPRING_CONSTANT);
    }

    /**
     * Calculate damping coefficient from floppiness parameter
     * @param floppiness 0 (firm) to 1 (floppy)
     * @return Damping coefficient value
     */
    private float calculateDamping(float floppiness) {
        // Higher floppiness = lower damping = more oscillation
        return MathHelper.lerp(floppiness, MAX_DAMPING, MIN_DAMPING);
    }

    /**
     * Soft clamping function that uses exponential decay at limits
     * This prevents hard stops and maintains smooth motion
     */
    private float softClamp(float value, float min, float max, float stiffness) {
        if (value < min) {
            float overshoot = min - value;
            // Exponential decay: allows small overshoot that decays quickly
            return min - overshoot * (float) Math.exp(-stiffness * overshoot);
        } else if (value > max) {
            float overshoot = value - max;
            return max + overshoot * (float) Math.exp(-stiffness * overshoot);
        }
        return value;
    }

    /**
     * Apply velocity damping when near position limits
     */
    private float applyLimitDamping(float position, float velocity, float min, float max) {
        // Approaching upper limit with positive velocity
        if (position > max - LIMIT_THRESHOLD && velocity > 0) {
            float proximityFactor = (position - (max - LIMIT_THRESHOLD)) / LIMIT_THRESHOLD;
            proximityFactor = MathHelper.clamp(proximityFactor, 0, 1);
            velocity *= MathHelper.lerp(proximityFactor, 1.0f, LIMIT_VELOCITY_DAMP);
        }
        // Approaching lower limit with negative velocity
        else if (position < min + LIMIT_THRESHOLD && velocity < 0) {
            float proximityFactor = ((min + LIMIT_THRESHOLD) - position) / LIMIT_THRESHOLD;
            proximityFactor = MathHelper.clamp(proximityFactor, 0, 1);
            velocity *= MathHelper.lerp(proximityFactor, 1.0f, LIMIT_VELOCITY_DAMP);
        }
        return velocity;
    }

    // ========== MAIN PHYSICS UPDATE ==========

    /**
     * Improved physics tick using proper spring-damper model
     *
     * @param floppiness User-configured floppiness (0 = firm, 1 = floppy)
     * @param forceY Accumulated forces on Y-axis this frame
     * @param forceX Accumulated forces on X-axis this frame
     * @param torque Accumulated rotational forces this frame
     */
    public void updatePhysics(float floppiness, float forceY, float forceX, float torque) {
        // Save previous positions for interpolation
        this.prePositionY = this.positionY;
        this.prePositionX = this.positionX;
        this.preRotation = this.rotation;

        // Calculate physics parameters from floppiness
        float springConstant = calculateSpringConstant(floppiness);
        float dampingCoeff = calculateDamping(floppiness);

        // ========== Y-AXIS PHYSICS ==========

        // Spring-damper equation: F = -k(x - x0) - c*v
        // where k = spring constant, c = damping coefficient, x0 = rest position
        float springForceY = -springConstant * (positionY - restPositionY);
        float dampingForceY = -dampingCoeff * velocityY;
        float totalForceY = springForceY + dampingForceY + forceY;

        // F = ma, so a = F/m
        float accelerationY = totalForceY / BREAST_MASS;

        // Semi-implicit Euler integration
        // Update velocity first, then use new velocity to update position
        // This is more stable than explicit Euler
        velocityY += accelerationY;
        positionY += velocityY;

        // Apply air resistance (global damping)
        velocityY *= AIR_RESISTANCE;

        // Apply velocity damping near limits
        velocityY = applyLimitDamping(positionY, velocityY, MIN_POSITION_Y, MAX_POSITION_Y);

        // Soft clamp position
        positionY = softClamp(positionY, MIN_POSITION_Y, MAX_POSITION_Y, CLAMP_STIFFNESS);

        // ========== X-AXIS PHYSICS ==========

        float xDamping = dampingCoeff * X_AXIS_DAMPING_MULT;

        float springForceX = -springConstant * (positionX - restPositionX);
        float dampingForceX = -xDamping * velocityX;
        float totalForceX = springForceX + dampingForceX + forceX;

        float accelerationX = totalForceX / BREAST_MASS;

        velocityX += accelerationX;
        positionX += velocityX;

        velocityX *= AIR_RESISTANCE;

        // ========== ROTATION PHYSICS ==========

        float rotDamping = dampingCoeff * ROTATION_DAMPING_MULT;

        float springTorque = -springConstant * (rotation - restRotation);
        float dampingTorque = -rotDamping * rotationVelocity;
        float totalTorque = springTorque + dampingTorque + torque;

        // For rotation, using same mass (could use moment of inertia instead)
        float angularAcceleration = totalTorque / BREAST_MASS;

        rotationVelocity += angularAcceleration;
        rotation += rotationVelocity;

        rotationVelocity *= AIR_RESISTANCE;

        // Clamp rotation to reasonable range
        rotation = MathHelper.clamp(rotation, -25f, 25f);
    }

    // ========== ALTERNATIVE: USING EXISTING FLOPPINESS PARAMETER ==========

    /**
     * Alternative implementation that better matches the existing parameter ranges
     * This version maps the existing "floppiness" to physically meaningful values
     */
    public void updatePhysicsCompatible(float floppiness, float forceY, float forceX, float torque) {
        this.prePositionY = this.positionY;
        this.prePositionX = this.positionX;
        this.preRotation = this.rotation;

        // Map floppiness (0.25-1.0 range) to damping ratio
        // Damping ratio: 0 = undamped, 1 = critically damped, >1 = overdamped
        // We want range from slightly underdamped to slightly overdamped
        float dampingRatio = MathHelper.lerp(floppiness, 1.2f, 0.3f);

        // Fixed natural frequency (affects oscillation speed)
        // Higher = faster oscillations
        float naturalFrequency = 2.5f;

        // Calculate spring constant and damping from damping ratio
        // These formulas ensure proper spring-damper behavior
        float k = BREAST_MASS * naturalFrequency * naturalFrequency;
        float c = 2.0f * dampingRatio * BREAST_MASS * naturalFrequency;

        // Y-Axis
        float springForceY = -k * (positionY - restPositionY);
        float dampingForceY = -c * velocityY;
        float totalForceY = springForceY + dampingForceY + forceY;

        velocityY += totalForceY / BREAST_MASS;
        positionY += velocityY;
        velocityY *= AIR_RESISTANCE;

        velocityY = applyLimitDamping(positionY, velocityY, MIN_POSITION_Y, MAX_POSITION_Y);
        positionY = softClamp(positionY, MIN_POSITION_Y, MAX_POSITION_Y, CLAMP_STIFFNESS);

        // X-Axis (same as Y-axis but with different damping)
        float cX = c * X_AXIS_DAMPING_MULT;
        float springForceX = -k * (positionX - restPositionX);
        float dampingForceX = -cX * velocityX;
        float totalForceX = springForceX + dampingForceX + forceX;

        velocityX += totalForceX / BREAST_MASS;
        positionX += velocityX;
        velocityX *= AIR_RESISTANCE;

        // Rotation (different natural frequency for rotation)
        float rotNaturalFreq = naturalFrequency * 1.2f; // Slightly faster settling
        float kRot = BREAST_MASS * rotNaturalFreq * rotNaturalFreq;
        float cRot = 2.0f * dampingRatio * BREAST_MASS * rotNaturalFreq * ROTATION_DAMPING_MULT;

        float springTorque = -kRot * (rotation - restRotation);
        float dampingTorque = -cRot * rotationVelocity;
        float totalTorque = springTorque + dampingTorque + torque;

        rotationVelocity += totalTorque / BREAST_MASS;
        rotation += rotationVelocity;
        rotationVelocity *= AIR_RESISTANCE;

        rotation = MathHelper.clamp(rotation, -25f, 25f);
    }

    // ========== GETTERS ==========

    public float getPositionY() { return positionY; }
    public float getPositionX() { return positionX; }
    public float getRotation() { return rotation; }

    public float getPrePositionY() { return prePositionY; }
    public float getPrePositionX() { return prePositionX; }
    public float getPreRotation() { return preRotation; }

    // ========== EXAMPLE: INTEGRATING WITH EXISTING CODE ==========

    /**
     * Example showing how to integrate this into the existing BreastPhysics class
     *
     * Replace the finishTick() method with this approach:
     */
    public void exampleIntegration(float entityConfigFloppiness) {
        // At the start of update(), initialize force accumulators
        float forceY = 0;
        float forceX = 0;
        float torque = 0;

        // In tickMovement(), tickPose(), etc., accumulate forces:
        // forceY += (float) motion.y * bounceIntensity;
        // forceY += breastWeight;
        // torque += calcRotation(entity, bounceIntensity);

        // At the end of update(), call improved physics:
        updatePhysicsCompatible(entityConfigFloppiness, forceY, forceX, torque);
    }

    // ========== DEBUGGING HELPERS ==========

    /**
     * Calculate current kinetic energy (useful for debugging)
     * Should decrease over time due to damping
     */
    public float getKineticEnergy() {
        // KE = 0.5 * m * v^2
        float keY = 0.5f * BREAST_MASS * velocityY * velocityY;
        float keX = 0.5f * BREAST_MASS * velocityX * velocityX;
        float keRot = 0.5f * BREAST_MASS * rotationVelocity * rotationVelocity;
        return keY + keX + keRot;
    }

    /**
     * Calculate potential energy (useful for debugging)
     * Should oscillate with kinetic energy in underdamped systems
     */
    public float getPotentialEnergy(float springConstant) {
        // PE = 0.5 * k * x^2
        float peY = 0.5f * springConstant * (positionY - restPositionY) * (positionY - restPositionY);
        float peX = 0.5f * springConstant * (positionX - restPositionX) * (positionX - restPositionX);
        float peRot = 0.5f * springConstant * (rotation - restRotation) * (rotation - restRotation);
        return peY + peX + peRot;
    }
}
