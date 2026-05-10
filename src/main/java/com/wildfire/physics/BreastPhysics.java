/*
    Wildfire's Female Gender Mod is a female gender mod created for Minecraft.
    Copyright (C) 2023 WildfireRomeo

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 3 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package com.wildfire.physics;

import com.wildfire.api.IGenderArmor;
import com.wildfire.main.WildfireHelper;
import com.wildfire.main.entitydata.EntityConfig;
import com.wildfire.physics.sim.AppearanceConfig;
import com.wildfire.physics.sim.Arm;
import com.wildfire.physics.sim.ArmorEffect;
import com.wildfire.physics.sim.BodyState;
import com.wildfire.physics.sim.BreastPhysicsSimulator;
import com.wildfire.physics.sim.PoseKind;
import com.wildfire.physics.sim.RandomSource;
import com.wildfire.physics.sim.Snapshot;
import com.wildfire.physics.sim.SwingState;
import com.wildfire.physics.sim.TickInput;
import com.wildfire.physics.sim.Vec3;
import com.wildfire.physics.sim.VehicleContribution;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;

/**
 * Adapter between Minecraft entity state and the pure-Java {@link BreastPhysicsSimulator}.
 * Holds per-entity state that the simulator can't (last position for delta computation,
 * a wrapped random source) and translates Minecraft types into the simulator's input records
 * each tick. Renderer-facing public getters preserve the historical signatures.
 */
public class BreastPhysics {

    private final EntityConfig entityConfig;
    private final BreastPhysicsSimulator simulator = new BreastPhysicsSimulator();
    private Snapshot snapshot = new Snapshot(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f);
    private net.minecraft.world.phys.Vec3 prevPos;

    public BreastPhysics(EntityConfig entityConfig) {
        this.entityConfig = entityConfig;
    }

    /** @apiNote Only call on the client. */
    public void update(LivingEntity entity, IGenderArmor armor) {
        AppearanceConfig appearance = appearanceFrom(entityConfig);
        ArmorEffect armorEffect = armorEffectFrom(armor, entityConfig);

        if (entity instanceof ArmorStand) {
            simulator.freezeStatic(appearance, armorEffect);
            snapshot = simulator.snapshot();
            return;
        }

        net.minecraft.world.phys.Vec3 currentPos = entity.position();
        if (prevPos == null) {
            // First tick: record position so the next tick has a valid delta. Skip simulation.
            prevPos = currentPos;
            snapshot = simulator.snapshot();
            return;
        }
        net.minecraft.world.phys.Vec3 motion = currentPos.subtract(prevPos);
        prevPos = currentPos;

        BodyState body = bodyStateFrom(entity, motion);
        PoseKind poseKind = poseFrom(entity.getPose());
        SwingState swing = swingStateFrom(entity);
        VehicleContribution vehicle = vehicleContributionFrom(entity);
        RandomSource random = new MinecraftRandomSource(entity);

        TickInput input = new TickInput(body, poseKind, swing, vehicle, appearance, armorEffect, random);
        simulator.tick(input);
        snapshot = simulator.snapshot();
    }

    public float getBreastSize(float partialTicks) {
        return snapshot.lerpedBreastSize(partialTicks);
    }

    public float getPrePositionY()      { return snapshot.prevPositionY(); }
    public float getPositionY()         { return snapshot.positionY(); }
    public float getPrePositionX()      { return snapshot.prevPositionX(); }
    public float getPositionX()         { return snapshot.positionX(); }
    public float getBounceRotation()    { return snapshot.bounceRotation(); }
    public float getPreBounceRotation() { return snapshot.prevBounceRotation(); }

    // -----------------------------------------------------------------------
    // Translation helpers
    // -----------------------------------------------------------------------

    private static AppearanceConfig appearanceFrom(EntityConfig cfg) {
        return new AppearanceConfig(
                cfg.getGender().canHaveBreasts(),
                cfg.getBustSize(),
                cfg.getBounceMultiplier(),
                cfg.getFloppiness(),
                cfg.getBreasts().isUniboob()
        );
    }

    private static ArmorEffect armorEffectFrom(IGenderArmor armor, EntityConfig cfg) {
        return new ArmorEffect(
                cfg.getArmorPhysicsOverride(),
                armor.tightness(),
                armor.physicsResistance()
        );
    }

    private BodyState bodyStateFrom(LivingEntity entity, net.minecraft.world.phys.Vec3 motion) {
        net.minecraft.world.phys.Vec3 vel = entity.getDeltaMovement();
        float yaw = effectiveBodyYaw(entity, false);
        float prevYaw = effectiveBodyYaw(entity, true);
        return new BodyState(
                new Vec3((float) motion.x, (float) motion.y, (float) motion.z),
                new Vec3((float) vel.x, (float) vel.y, (float) vel.z),
                entity.fallDistance,
                entity.walkAnimation.position(),
                entity.walkAnimation.speed(),
                yaw,
                prevYaw
        );
    }

    private static PoseKind poseFrom(Pose pose) {
        return switch (pose) {
            case CROUCHING -> PoseKind.CROUCHING;
            case SLEEPING  -> PoseKind.SLEEPING;
            case SWIMMING  -> PoseKind.SWIMMING;
            case STANDING  -> PoseKind.STANDING;
            default        -> PoseKind.OTHER;
        };
    }

    private static Arm armFrom(HumanoidArm arm) {
        return arm == HumanoidArm.LEFT ? Arm.LEFT : Arm.RIGHT;
    }

    private static SwingState swingStateFrom(LivingEntity entity) {
        Arm swingingArm;
        if (entity.swingingArm == InteractionHand.MAIN_HAND) {
            swingingArm = armFrom(entity.getMainArm());
        } else {
            swingingArm = armFrom(entity.getMainArm().getOpposite());
        }
        return new SwingState(
                entity.getCurrentSwingDuration(),
                entity.swingTime,
                entity.swinging,
                swingingArm,
                armFrom(entity.getMainArm()),
                entity.tickCount
        );
    }

    /**
     * Resolves the body yaw the simulator should use, mirroring the original
     * {@code calcRotation} logic: zero for vehicles that suppress rotation
     * (chickens, unsaddled horses, sitting camels), the vehicle's own yaw when
     * the rider is yaw-controlled by the vehicle, otherwise the entity's own yaw.
     */
    private static float effectiveBodyYaw(LivingEntity entity, boolean previous) {
        Entity vehicle = entity.getVehicle();
        if (vehicle != null) {
            if (vehicleSuppressesRotation(vehicle)) {
                return 0f;
            } else if (shouldUseVehicleYaw(entity, vehicle)) {
                if (vehicle instanceof LivingEntity living) {
                    return previous ? living.yBodyRotO : living.yBodyRot;
                }
                return previous ? vehicle.yRotO : vehicle.getYRot();
            }
        }
        return previous ? entity.yBodyRotO : entity.yBodyRot;
    }

    private static boolean vehicleSuppressesRotation(Entity vehicle) {
        // Chickens force the rider's body yaw and break our physics; same with unsaddled
        // horses/llamas and sitting/standing camels.
        return vehicle instanceof Chicken
                || (vehicle instanceof AbstractHorse horseLike && !horseLike.isSaddled())
                || (vehicle instanceof Camel camel && camel.refuseToMove());
    }

    private static boolean shouldUseVehicleYaw(LivingEntity rider, Entity vehicle) {
        return vehicle.hasControllingPassenger()
                || vehicle instanceof Boat
                || vehicle.getVisualRotationYInDegrees() == rider.getVisualRotationYInDegrees();
    }

    // -----------------------------------------------------------------------
    // Vehicle contribution: per-vehicle nudges expressed as intensity/weight
    // factors so the simulator can scale them by its own bounceIntensity.
    // -----------------------------------------------------------------------

    private static VehicleContribution vehicleContributionFrom(LivingEntity entity) {
        Entity vehicle = entity.getVehicle();
        if (vehicle == null) {
            return VehicleContribution.NONE;
        }
        if (vehicle instanceof Boat boat) {
            return boatContribution(entity, boat);
        }
        if (vehicle instanceof Minecart cart) {
            return minecartContribution(cart);
        }
        if (vehicle instanceof AbstractHorse horse) {
            return horseLikeContribution(horse, 0.05f);
        }
        if (vehicle instanceof Pig pig) {
            return pigContribution(pig);
        }
        if (vehicle instanceof Strider strider) {
            return striderContribution(strider);
        }
        return VehicleContribution.NONE;
    }

    private static VehicleContribution boatContribution(LivingEntity entity, Boat boat) {
        float walkAnimPos = entity.walkAnimation.position();
        int rowTime  = (int) boat.getRowingTime(0, walkAnimPos);
        int rowTime2 = (int) boat.getRowingTime(1, walkAnimPos);
        float rotationL = (float) Mth.clampedLerp(-(float) Math.PI / 3f, -0.2617994f,
                ((Mth.sin(-rowTime2) + 1.0f) / 2.0f));
        float rotationR = (float) Mth.clampedLerp(-(float) Math.PI / 4f, (float) Math.PI / 4f,
                ((Mth.sin(-rowTime + 1.0f) + 1.0f) / 2.0f));
        if (rotationL < -1f || rotationR < -0.6f) {
            return VehicleContribution.replace(1f / 3.25f, 0f);
        }
        return VehicleContribution.NONE;
    }

    private static VehicleContribution minecartContribution(Minecart cart) {
        float speed = (float) cart.getDeltaMovement().lengthSqr();
        if (Math.random() * speed < 0.5f && speed > 0.2f) {
            float intensityFactor = (Math.random() > 0.5 ? -1f : 1f) / 6f;
            return VehicleContribution.replace(intensityFactor, 1f);
        }
        return VehicleContribution.NONE;
    }

    private static VehicleContribution horseLikeContribution(AbstractHorse horse, float minMovement) {
        float movement = (float) horse.getDeltaMovement().length();
        if (horse.tickCount % clampMovement(movement) == 5 && movement > minMovement) {
            return VehicleContribution.replace(1f / 4f, 1f);
        }
        return VehicleContribution.NONE;
    }

    private static VehicleContribution pigContribution(Pig pig) {
        float movement = (float) pig.getDeltaMovement().length();
        if (pig.tickCount % clampMovement(movement) == 5 && movement > 0.002f) {
            float intensityFactor = Mth.clamp(movement * 75f, 0.1f, 1f) / 4f;
            return VehicleContribution.replace(intensityFactor, 1f);
        }
        return VehicleContribution.NONE;
    }

    private static VehicleContribution striderContribution(Strider strider) {
        double heightOffset = strider.getBbHeight() - 0.19
                + (0.12f * Mth.cos(strider.walkAnimation.position() * 1.5f)
                   * 2f * Math.min(0.25f, strider.walkAnimation.speed()));
        float intensityFactor = (float) (heightOffset * 3f) - 4.5f;
        return VehicleContribution.add(intensityFactor, 0f);
    }

    private static int clampMovement(float movement) {
        return Math.max((int) (10 - 2 * movement), 1);
    }

    // -----------------------------------------------------------------------
    // Random source backed by the entity's level random + WildfireHelper jitter.
    // -----------------------------------------------------------------------

    private static final class MinecraftRandomSource implements RandomSource {
        private final net.minecraft.util.RandomSource backing;

        MinecraftRandomSource(LivingEntity entity) {
            this.backing = entity.level().random;
        }

        @Override
        public boolean nextBoolean() {
            return backing.nextBoolean();
        }

        @Override
        public float nextFloat(float lo, float hi) {
            return WildfireHelper.randFloat(lo, hi);
        }
    }
}
