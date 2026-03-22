package com.vomiter.neurolib.common.entity.generic;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;

public class GoalReplacer {
    public static boolean replaceFirst(GoalSelector goalSelector, Class<? extends Goal> classToReplace, Goal goal){
         var toReplace = goalSelector.getAvailableGoals().stream().filter(g -> g.getGoal().getClass() == classToReplace).findFirst();
         if(toReplace.isEmpty()) return false;
         int priority = toReplace.get().getPriority();
         goalSelector.removeGoal(toReplace.get().getGoal());
         goalSelector.addGoal(priority, goal);
         return true;
    }
}
