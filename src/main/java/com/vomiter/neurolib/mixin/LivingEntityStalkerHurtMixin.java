package com.vomiter.neurolib.mixin;

import com.vomiter.neurolib.common.entity.stalker.IStalkerMob;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityStalkerHurtMixin {
    @Inject(method = "hurt", at = @At("RETURN"))
    private void stopAvoidEyesight(DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> cir){
        var self = (LivingEntity)(Object)this;
        if(self instanceof IStalkerMob stalkerMob){
            var goal = stalkerMob.neuroLib$getAvoidEyeSightGoal();
            if(goal != null) goal.reactToHurt(damageSource);
        }
    }

}
