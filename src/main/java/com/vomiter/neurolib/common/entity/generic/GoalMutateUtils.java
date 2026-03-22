package com.vomiter.neurolib.common.entity.generic;

import com.vomiter.neurolib.mixin.WrappedGoalAccessor;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.function.Function;

public final class GoalMutateUtils {

    private GoalMutateUtils() {
    }

    public static Goal unMutate(Goal goal) {
        Goal current = goal;
        while (current instanceof IMutatedGoal<?> mutatedGoal) {
            current = mutatedGoal.getMutatedGoal();
        }
        return current;
    }

    @Nullable
    public static <T extends Goal> T unMutateAs(Goal goal, Class<T> clazz) {
        Goal unmutated = unMutate(goal);
        return clazz.isInstance(unmutated) ? clazz.cast(unmutated) : null;
    }

    public static boolean hasInnerMutatedGoal(Goal goal, Class<? extends Goal> clazz) {
        Goal current = goal;
        while (current instanceof IMutatedGoal<?> mutatedGoal) {
            Goal inner = mutatedGoal.getMutatedGoal();
            if (clazz.isInstance(inner)) {
                return true;
            }
            current = inner;
        }
        return false;
    }

    public static boolean isMutated(Goal goal) {
        return goal instanceof IMutatedGoal<?>;
    }

    public static MutatedMeleeGoal mutateMelee(MeleeAttackGoal goal) {
        if (goal instanceof MutatedMeleeGoal mutated) {
            return mutated;
        }
        return new MutatedMeleeGoal(goal);
    }

    @Nullable
    public static MutatedMeleeGoal mutateIfMelee(Goal goal) {
        if (goal instanceof MutatedMeleeGoal mutated) {
            return mutated;
        }
        if (goal instanceof MeleeAttackGoal meleeGoal) {
            return new MutatedMeleeGoal(meleeGoal);
        }
        return null;
    }

    public static boolean replaceFirstMeleeWithMutated(GoalSelector selector) {
        for (WrappedGoal wrappedGoal : new ArrayList<>(selector.getAvailableGoals())) {
            Goal innerGoal = ((WrappedGoalAccessor) wrappedGoal).getGoal();

            if (innerGoal instanceof MutatedMeleeGoal) {
                continue;
            }

            if (innerGoal instanceof MeleeAttackGoal meleeGoal) {
                int priority = ((WrappedGoalAccessor) wrappedGoal).getPriority();
                selector.removeGoal(innerGoal);
                selector.addGoal(priority, mutateMelee(meleeGoal));
                return true;
            }
        }
        return false;
    }

    /**
    * returns the first priority of replaced
     */
    public static int replaceAllMeleeWithMutated(GoalSelector selector, @Nullable Function<MeleeAttackGoal, MutatedMeleeGoal> function, ArrayList<Replacement> replacements) {
        int firstPriority = 10000;
        for (WrappedGoal wrappedGoal : new ArrayList<>(selector.getAvailableGoals())) {
            Goal innerGoal = ((WrappedGoalAccessor) wrappedGoal).getGoal();

            if (innerGoal instanceof MutatedMeleeGoal) {
                continue;
            }

            if (innerGoal instanceof MeleeAttackGoal meleeGoal) {
                int priority = ((WrappedGoalAccessor) wrappedGoal).getPriority();
                firstPriority = Math.min(firstPriority, priority);
                replacements.add(
                        new Replacement(
                                innerGoal,
                                function == null? mutateMelee(meleeGoal): function.apply(meleeGoal),
                                priority)
                );
            }
        }

        for (Replacement replacement : replacements) {
            selector.removeGoal(replacement.oldGoal);
        }

        for (Replacement replacement : replacements) {
            selector.addGoal(replacement.priority, replacement.newGoal);
        }

        return firstPriority;
    }

    public record Replacement(Goal oldGoal, Goal newGoal, int priority) {}
}