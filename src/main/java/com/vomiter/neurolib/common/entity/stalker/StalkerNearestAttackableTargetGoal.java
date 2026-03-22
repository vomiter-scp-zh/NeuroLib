package com.vomiter.neurolib.common.entity.stalker;

import com.vomiter.neurolib.mixin.TargetGoalAccessor;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

public class StalkerNearestAttackableTargetGoal extends NearestAttackableTargetGoal<Player> {
    public StalkerNearestAttackableTargetGoal(Mob p_26060_, boolean p_26062_) {
        super(p_26060_, Player.class, p_26062_);
    }

    public void suppressForgetting(int ticks){
        if(this instanceof TargetGoalAccessor accessor){
            accessor.setUnseenTicks(ticks * -1);
        }
    }

}
