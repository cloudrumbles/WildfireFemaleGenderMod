# Pure-Java Breast Physics Extraction — Design

## Goal

Extract the physics simulation in `src/main/java/com/wildfire/physics/BreastPhysics.java` into a pure-Java core with no Minecraft or mod dependencies, behind a small, clean public API. The existing class becomes a thin adapter that translates Minecraft entity state into pure-Java input records each tick and reads a snapshot back out.

## Non-Goals

- No new physics behavior. The simulation feel should be visually indistinguishable from the current implementation, with the small exceptions noted under "Allowed cleanups" below.
- No extension point for modded vehicles. The five vanilla vehicles with special handling stay as an internal switch in the adapter.
- No changes to renderers or callers beyond what is required to follow the renamed/relocated public methods.

## Module Layout

New package: `com.wildfire.physics.sim`. Every file in this package must compile against `java.base` only. No `net.minecraft.*` imports, no `com.wildfire.*` imports outside the `sim` package, no NeoForge imports. This is enforced by convention (and verified by a test that scans the package's imports).

The existing class `com.wildfire.physics.BreastPhysics` is rewritten as a thin adapter that:

1. Constructs a `BreastPhysicsSimulator` once.
2. Each tick, builds a `TickInput` record from the `LivingEntity`, `EntityConfig`, and `IGenderArmor`, calls `simulator.tick(input)`, and exposes the resulting snapshot through the same public getters the renderers already use (`getPositionY`, `getBounceRotation`, `getBreastSize`, etc.).
3. Special-cases armor stands by calling `simulator.freezeStatic(...)` instead of `tick(...)`.
4. Owns the small switch over `Boat`, `Minecart`, `Pig`, `AbstractHorse`, `Strider` that pre-computes a single float `bounceYNudge` and feeds it into the `TickInput` as a `VehicleContribution`.
5. Owns the resolution of "which yaw delta should we use" (entity, vehicle, or zero for chickens / unsaddled horses / sitting camels), producing a single `effectiveBodyYaw` and `prevEffectiveBodyYaw` for the simulator.

## Public API

```java
public final class BreastPhysicsSimulator {
    public BreastPhysicsSimulator();
    public void tick(TickInput input);
    public void freezeStatic(AppearanceConfig appearance, ArmorEffect armor);
    public Snapshot snapshot();
}
```

All input and output types are records under `com.wildfire.physics.sim`:

```java
public record TickInput(
    BodyState body,
    PoseKind pose,
    SwingState swing,
    VehicleContribution vehicle,
    AppearanceConfig appearance,
    ArmorEffect armor,
    RandomSource random
) {}

public record BodyState(
    Vec3 motionDelta,
    Vec3 velocity,
    float fallDistance,
    float walkAnimPosition,
    float walkAnimSpeed,
    float effectiveBodyYaw,
    float prevEffectiveBodyYaw
) {}

public enum PoseKind { STANDING, CROUCHING, SLEEPING, SWIMMING, OTHER }

public record SwingState(
    int swingDuration,
    int swingTime,
    boolean swinging,
    Arm swingingArm,
    Arm mainArm,
    int tickCount
) {}

public enum Arm { LEFT, RIGHT }

public record VehicleContribution(float bounceYNudge, boolean appliesThisTick) {
    public static final VehicleContribution NONE = new VehicleContribution(0f, false);
}

public record AppearanceConfig(
    boolean canHaveBreasts,
    float bustSize,
    float bounceMultiplier,
    float floppiness,
    boolean uniboob
) {}

public record ArmorEffect(boolean overridePhysics, float tightness, float physicsResistance) {
    public static final ArmorEffect NONE = new ArmorEffect(true, 0f, 0f);
}

public interface RandomSource {
    boolean nextBoolean();
    float nextFloat(float lo, float hi);
}

public record Vec3(float x, float y, float z) {
    public Vec3 subtract(Vec3 o);
    public float lengthSquared();
}

public record Snapshot(
    float prevBreastSize, float breastSize,
    float prevPositionY, float positionY,
    float prevPositionX, float positionX,
    float prevBounceRotation, float bounceRotation
) {
    public float lerpedBreastSize(float partialTicks);
    public float lerpedPositionY(float partialTicks);
    public float lerpedPositionX(float partialTicks);
    public float lerpedBounceRotation(float partialTicks);
}
```

## Internal Structure of the Simulator

The simulator owns mutable state (positions, velocities, last pose, swing tracking) and updates it through a sequence of pure-ish private steps. The current 200-line `update()` method is decomposed into:

- `computeBounceIntensity(appearance, armor, random)` — derives `bounceIntensity` and `breastWeight` from appearance and armor, including the uniboob jitter.
- `updateBreastSize(state, appearance, armor)` — lerps `breastSize` toward target accounting for armor tightness.
- `applyMotionTarget(state, body, intensity, weight)` — adds `motion.y * intensity + breastWeight` to `targetBounceY`, plus the walk-animation cosine term.
- `applyFallRotation(state, body, intensity, random)` — manages `randomB` flipping on fall edges, contributes to `targetRotVel`.
- `applyVehicleNudge(state, vehicle, intensity, weight)` — when `appliesThisTick`, sets `targetBounceY = bounceYNudge + breastWeight` (the adapter has already done the vehicle-specific math).
- `applyPoseTransition(state, pose, intensity)` — handles the crouch/sleep transition kicks.
- `applySwingForces(state, swing, pose, intensity, random)` — the swing-based bounce and rotation contributions.
- `applyBoundaryPushback(state)` — the `distanceFromMin` / `distanceFromMax` correction terms.
- `clampTargets(state)` — clamps `targetBounceY` and `targetRotVel`.
- `integrateSpring(state, floppiness)` — the lerp/velocity update producing the new `bounceVel`, `bounceVelX`, `bounceRotVel`.
- `clampOutputs(state)` — the final `positionY` clamp.

Each step is unit-testable by constructing the relevant subset of input records. Magic numbers live as named constants at the top of the simulator (`BOUNCE_MIN_Y`, `BOUNCE_MAX_Y`, `ROTATION_CLAMP`, `FLOPPINESS_BOUNCE_FLOOR`, `FLOPPINESS_BOUNCE_CEILING`, etc.).

## Allowed Cleanups (option 3 fidelity)

The following changes from the current implementation are intentional and may produce imperceptible feel differences:

- Drop `Math.round(bounceMultiplier * 3 * 100) / 100f` — replaced by plain `bounceMultiplier * 3`. The two-decimal rounding has no defensible reason to affect behavior.
- Replace `if (f < 1.0F) f = 1.0F` with `Math.max(f, 1.0F)`.
- Rename `wfg_bounceRotation` / `wfg_preBounceRotation` to `bounceRotation` / `prevBounceRotation`.
- Lift inline magic numbers to named constants.
- Remove dead commented-out code (the swimming-rotation block, the `WildfireGender.logger.debug` lines).

Math, tunable values, and clamp constants stay byte-identical to the current implementation. The randomness contract stays the same (per-tick `nextBoolean()` and `nextFloat(lo, hi)` calls in the same order).

## TDD Plan

1. **Test infrastructure.** Add a `src/test/java` source set to `build.gradle` with JUnit 5 (`org.junit.jupiter:junit-jupiter:5.10.2`). Confirm `./gradlew test` runs.
2. **Pure-Java core, red-green-refactor per step**, in dependency order:
   - `Vec3` operations.
   - `RandomSource` test double (deterministic seeded impl for tests).
   - `computeBounceIntensity` — tested across uniboob/non-uniboob, armor override on/off.
   - `updateBreastSize` — tested for grow toward target, shrink toward target, armor tightness shrinkage, gender-cannot-have-breasts → 0.
   - `applyMotionTarget` — tested that motion.y maps linearly into target.
   - `applyPoseTransition` — tested for STANDING→CROUCHING, CROUCHING→STANDING, ANY→SLEEPING.
   - `applySwingForces` — tested against the known swing-duration brackets.
   - `applyFallRotation` — tested that `randomB` flips on fall start and resets on landing.
   - `applyVehicleNudge` — tested that the nudge is applied only when `appliesThisTick`.
   - `applyBoundaryPushback`, `clampTargets`, `clampOutputs` — boundary tests.
   - `integrateSpring` — tested for convergence to a constant target after N ticks.
   - End-to-end simulator test: feed a sequence of synthetic inputs, assert deterministic snapshot output.
   - Purity guard test: read every `.java` source file under `src/main/java/com/wildfire/physics/sim/`, parse the import lines, and assert none start with `net.minecraft.`, `net.neoforged.`, or `com.wildfire.` (with the sole exception of `com.wildfire.physics.sim.` itself).
3. **Adapter rewrite.** Replace the body of `BreastPhysics` with a thin adapter that constructs `TickInput` from MC types. Public method signatures stay backward-compatible (`update`, `getPositionY`, `getBreastSize`, `getBounceRotation`, etc.), so renderers need no changes.
4. **In-game smoke test.** Launch the client, walk, run, jump, crouch, sleep, ride each of boat / minecart / pig / horse / strider, swing arms with and without haste / mining fatigue, confirm the feel matches.

## Risks

- **Behavior drift from cleanups.** The `randFloat` and rounding changes could in principle perturb the feel. Mitigation: in-game smoke test before merge, and the changes are individually small enough that any drift can be reverted.
- **Adapter mistranslation of yaw resolution.** The vehicle-yaw / entity-yaw / suppress-yaw logic is subtle (chickens, unsaddled horses, sitting camels). The adapter must preserve it exactly. Mitigation: keep the existing private helpers (`vehicleSuppressesRotation`, `shouldUseVehicleYaw`) verbatim in the adapter, just feeding into the new yaw-resolution method instead of inline math.
- **Renderer coupling.** The renderers read state through public getters on `BreastPhysics`. The adapter keeps these signatures, so renderers should be untouched. Confirmed by grep before merge.

## Files Affected

- `build.gradle` — add test source set + JUnit 5 dependency.
- `src/main/java/com/wildfire/physics/BreastPhysics.java` — rewritten as adapter.
- `src/main/java/com/wildfire/physics/sim/*.java` — new pure-Java core (simulator + records).
- `src/test/java/com/wildfire/physics/sim/*Test.java` — new tests.
- No changes to renderers, mixins, or other call sites.
