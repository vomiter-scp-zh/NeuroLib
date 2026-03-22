package com.vomiter.neurolib.common.entity.generic;

import net.minecraft.world.entity.ai.goal.Goal;

/**
 * T = Wrapped, B = Basic
 */
public interface IMutatedGoal<B extends Goal> {
    B getMutatedGoal();
}