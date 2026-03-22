package com.vomiter.neurolib.common.entity.movement;

import net.minecraft.world.entity.LivingEntity;

import javax.swing.text.html.parser.Entity;

public class MovementCacheHelper {
    public static double getDx(LivingEntity entity){
        if(entity instanceof IMovementCache cache) return cache.neuroLib$getLastSampleDx();
        return 0;
    }

    public static double getDz(LivingEntity entity){
        if(entity instanceof IMovementCache cache) return cache.neuroLib$getLastSampleDz();
        return 0;
    }

}
