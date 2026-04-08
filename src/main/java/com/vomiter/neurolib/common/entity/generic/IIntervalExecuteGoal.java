package com.vomiter.neurolib.common.entity.generic;

import net.minecraft.world.entity.Mob;

import java.util.function.Consumer;

public interface IIntervalExecuteGoal {
    long getNextExecuteTick();
    void setNextExecuteTick(long l);
    long getExecuteIntervalTicks();
    Consumer<IIntervalExecuteGoal> getExecutable();

    default boolean isInExecuteInterval() {
        if (this instanceof Mob mob) {
            long currentTime = mob.level().getGameTime();
            return getNextExecuteTick() >= currentTime;
        }
        return false;
    }

    default long getExecuteIntervalOffset(Mob mob) {
        long interval = Math.max(1L, getExecuteIntervalTicks());
        return Math.floorMod(mob.getId(), interval);
    }

    default void startExecuteInterval() {
        if (this instanceof Mob mob) {
            long currentTime = mob.level().getGameTime();
            setNextExecuteTick(currentTime + getExecuteIntervalTicks() + getExecuteIntervalOffset(mob));
        }
    }

    default void execute() {
        if (isInExecuteInterval()) return;
        startExecuteInterval();
        getExecutable().accept(this);
    }
}