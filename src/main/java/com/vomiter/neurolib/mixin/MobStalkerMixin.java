package com.vomiter.neurolib.mixin;

import com.vomiter.neurolib.common.entity.stalker.IStalkerMob;
import com.vomiter.neurolib.common.entity.stalker.StalkerAvoidEyeSightGoal;
import com.vomiter.neurolib.common.entity.stalker.StalkerMeleeAttackGoal;
import com.vomiter.neurolib.common.entity.stalker.StalkerNearestAttackableTargetGoal;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Mob.class)
public class MobStalkerMixin implements IStalkerMob {
    @Unique
    StalkerNearestAttackableTargetGoal neuroLib$NATargetGoal;

    @Unique
    StalkerMeleeAttackGoal neuroLib$MAGoal;

    @Unique
    StalkerAvoidEyeSightGoal neuroLib$AESGoal;

    @Override
    public StalkerNearestAttackableTargetGoal neuroLib$getNearestGoal() {
        return neuroLib$NATargetGoal;
    }

    @Override
    public void neuroLib$setNearestGoal(StalkerNearestAttackableTargetGoal goal) {
        neuroLib$NATargetGoal = goal;
    }

    @Override
    public StalkerMeleeAttackGoal neuroLib$getMeleeGoal() {
        return neuroLib$MAGoal;
    }

    @Override
    public void neuroLib$setMeleeAttackGoal(StalkerMeleeAttackGoal goal) {
        neuroLib$MAGoal = goal;
    }

    @Override
    public StalkerAvoidEyeSightGoal neuroLib$getAvoidEyeSightGoal() {
        return neuroLib$AESGoal;
    }

    @Override
    public void neuroLib$setAvoidEyeSightGoal(StalkerAvoidEyeSightGoal goal) {
        neuroLib$AESGoal = goal;
    }
}
