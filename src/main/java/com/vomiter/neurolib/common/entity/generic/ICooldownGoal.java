package com.vomiter.neurolib.common.entity.generic;

import net.minecraft.world.entity.Mob;

public interface ICooldownGoal {
    long getNextAllowedTick();
    void setNextAllowedTick(long l);
    long getCooldownTicks();

    default boolean isInCooldown(){
        if(this instanceof Mob mob){
            var currentTime = mob.level().getGameTime();
            return getNextAllowedTick() >= currentTime;
        }
        return false;
    }
    default void startCooldown(){
        if(this instanceof Mob mob){
            var currentTime = mob.level().getGameTime();
            setNextAllowedTick(currentTime + getCooldownTicks());
        }
    }
}
