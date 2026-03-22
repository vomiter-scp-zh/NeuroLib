package com.vomiter.neurolib.common.entity.generic;

import com.vomiter.neurolib.mixin.MeleeAttackGoalAccessor;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class MutatedMeleeGoal extends MeleeAttackGoal implements IMutatedGoal<MeleeAttackGoal>, IConfigurableMutatedGoal<MutatedMeleeGoal> {

    private final MeleeAttackGoal basic;

    private Predicate<MutatedMeleeGoal> extraUseCheck;
    private Predicate<MutatedMeleeGoal> extraContinueCheck;
    private Predicate<MutatedMeleeGoal> forceUse;
    private Predicate<MutatedMeleeGoal> forceContinue;
    private Consumer<MutatedMeleeGoal> extraStop;
    private Consumer<MutatedMeleeGoal> extraStart;
    private Consumer<MutatedMeleeGoal> extraTick;

    public MutatedMeleeGoal(MeleeAttackGoal basicGoal) {
        super(
                ((MeleeAttackGoalAccessor) basicGoal).getMob(),
                ((MeleeAttackGoalAccessor) basicGoal).getSpeedModifier(),
                ((MeleeAttackGoalAccessor) basicGoal).getFollowOption()
        );
        this.basic = basicGoal;
        this.setFlags(basicGoal.getFlags());
    }

    @Override
    public MeleeAttackGoal getMutatedGoal() {
        return basic;
    }

    @Override
    public MutatedMeleeGoal setExtraUseCheck(Predicate<MutatedMeleeGoal> check) {
        this.extraUseCheck = check;
        return this;
    }

    @Override
    public MutatedMeleeGoal setExtraContinueCheck(Predicate<MutatedMeleeGoal> check) {
        this.extraContinueCheck = check;
        return this;
    }

    @Override
    public MutatedMeleeGoal setForceUse(Predicate<MutatedMeleeGoal> check) {
        this.forceUse = check;
        return this;
    }

    @Override
    public MutatedMeleeGoal setForceContinue(Predicate<MutatedMeleeGoal> check) {
        this.forceContinue = check;
        return this;
    }

    @Override
    public MutatedMeleeGoal setExtraStart(Consumer<MutatedMeleeGoal> action) {
        this.extraStart = action;
        return this;
    }

    @Override
    public MutatedMeleeGoal setExtraStop(Consumer<MutatedMeleeGoal> action) {
        this.extraStop = action;
        return this;
    }

    @Override
    public MutatedMeleeGoal setExtraTick(Consumer<MutatedMeleeGoal> action) {
        this.extraTick = action;
        return this;
    }

    @Override
    public boolean canUse() {
        if (forceUse != null && forceUse.test(this)) {
            return true;
        }
        if (extraUseCheck != null && !extraUseCheck.test(this)) {
            return false;
        }
        return basic.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (forceContinue != null && forceContinue.test(this)) {
            return true;
        }
        if (extraContinueCheck != null && !extraContinueCheck.test(this)) {
            return false;
        }
        return basic.canContinueToUse();
    }

    @Override
    public boolean isInterruptable() {
        return basic.isInterruptable();
    }

    @Override
    public void start() {
        if (extraStart != null) {
            extraStart.accept(this);
        }
        basic.start();
    }

    @Override
    public void stop() {
        if (extraStop != null) {
            extraStop.accept(this);
        }
        basic.stop();
    }

    @Override
    public void tick() {
        if (extraTick != null) {
            extraTick.accept(this);
        }
        basic.tick();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return basic.requiresUpdateEveryTick();
    }

    @Override
    public @NotNull String toString() {
        return "WrappedMeleeGoal[" + basic + "]";
    }
}