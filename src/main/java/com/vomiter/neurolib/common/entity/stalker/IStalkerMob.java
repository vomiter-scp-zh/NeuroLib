package com.vomiter.neurolib.common.entity.stalker;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public interface IStalkerMob {
    @Nullable StalkerNearestAttackableTargetGoal neuroLib$getNearestGoal();
    void neuroLib$setNearestGoal(StalkerNearestAttackableTargetGoal goal);
    @Nullable StalkerMeleeAttackGoal neuroLib$getMeleeGoal();
    void neuroLib$setMeleeAttackGoal(StalkerMeleeAttackGoal goal);
    @Nullable StalkerAvoidEyeSightGoal neuroLib$getAvoidEyeSightGoal();
    void neuroLib$setAvoidEyeSightGoal(StalkerAvoidEyeSightGoal goal);

}
