package com.vomiter.neurolib.common.entity.generic;

import net.minecraft.world.entity.ai.goal.Goal;

import java.util.function.Consumer;
import java.util.function.Predicate;

public interface IConfigurableMutatedGoal<SELF extends Goal> {
    SELF setExtraUseCheck(Predicate<SELF> check);
    SELF setExtraContinueCheck(Predicate<SELF> check);
    SELF setForceUse(Predicate<SELF> check);
    SELF setForceContinue(Predicate<SELF> check);
    SELF setExtraStart(Consumer<SELF> action);
    SELF setExtraStop(Consumer<SELF> action);
    SELF setExtraTick(Consumer<SELF> action);
}
