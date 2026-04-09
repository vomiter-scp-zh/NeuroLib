package com.vomiter.neurolib.mixin.compat.enhancedai;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.vomiter.neurolib.common.entity.hunt.AbstractCappedHuntGoal;
import insane96mcp.enhancedai.modules.mobs.targeting.Targeting;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Targeting.class)
public class TargetingMixin {
    @WrapOperation(method = "processTargetGoal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/goal/WrappedGoal;getGoal()Lnet/minecraft/world/entity/ai/goal/Goal;", ordinal = 0))
    private Goal neurolib$interceptGoal(WrappedGoal instance, Operation<Goal> original){
        var originalResult = original.call(instance);
        if(originalResult instanceof AbstractCappedHuntGoal<?,?>) return null;
        return originalResult;
    }
}
