package com.vomiter.neurolib.mixin;

import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TargetGoal.class)
public interface TargetGoalAccessor {
    @Accessor("unseenTicks")
    int getUnseenTicks();

    @Accessor("unseenTicks")
    void setUnseenTicks(int i);

}
