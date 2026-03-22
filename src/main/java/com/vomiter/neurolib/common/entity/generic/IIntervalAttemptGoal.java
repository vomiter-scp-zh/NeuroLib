package com.vomiter.neurolib.common.entity.generic;

import net.minecraft.world.entity.Mob;

public interface IIntervalAttemptGoal {
    long getNextAttemptTick();
    void setNextAttemptTick(long l);
    long getAttemptIntervalTicks();

    default boolean isInAttemptInterval(){
        if(this instanceof Mob mob){
            var currentTime = mob.level().getGameTime();
            return getNextAttemptTick() >= currentTime;
        }
        return false;
    }
    default void startAttemptInterval(){
        if(this instanceof Mob mob){
            var currentTime = mob.level().getGameTime();
            setNextAttemptTick(currentTime + getAttemptIntervalTicks());
        }
    }

}
