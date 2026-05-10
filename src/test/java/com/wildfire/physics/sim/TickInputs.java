package com.wildfire.physics.sim;

/** Test fixture builder: a baseline TickInput that can be tweaked per test. */
final class TickInputs {

    private TickInputs() {}

    static TickInput defaultInput() {
        return new TickInput(
                BodyState.AT_REST,
                PoseKind.STANDING,
                SwingState.IDLE,
                VehicleContribution.NONE,
                new AppearanceConfig(true, 1f, 1f, 0.5f, true),
                ArmorEffect.NONE,
                new TestRandom()
        );
    }

    static TickInput withBody(TickInput base, BodyState body) {
        return new TickInput(body, base.pose(), base.swing(), base.vehicle(), base.appearance(), base.armor(), base.random());
    }

    static TickInput withPose(TickInput base, PoseKind pose) {
        return new TickInput(base.body(), pose, base.swing(), base.vehicle(), base.appearance(), base.armor(), base.random());
    }

    static TickInput withSwing(TickInput base, SwingState swing) {
        return new TickInput(base.body(), base.pose(), swing, base.vehicle(), base.appearance(), base.armor(), base.random());
    }

    static TickInput withVehicle(TickInput base, VehicleContribution vehicle) {
        return new TickInput(base.body(), base.pose(), base.swing(), vehicle, base.appearance(), base.armor(), base.random());
    }

    static TickInput withAppearance(TickInput base, AppearanceConfig appearance) {
        return new TickInput(base.body(), base.pose(), base.swing(), base.vehicle(), appearance, base.armor(), base.random());
    }

    static TickInput withArmor(TickInput base, ArmorEffect armor) {
        return new TickInput(base.body(), base.pose(), base.swing(), base.vehicle(), base.appearance(), armor, base.random());
    }
}
