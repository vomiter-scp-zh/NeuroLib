package com.vomiter.neurolib.common.entity.generic;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public class EntityControlHelpers {
    public static void nudgeTowardTarget(Mob mob, LivingEntity target) {
        double dx = target.getX() - mob.getX();
        double dz = target.getZ() - mob.getZ();
        double distSqr = dx * dx + dz * dz;
        if (distSqr < 1.0E-6D) return;

        double dist = Math.sqrt(distSqr);
        double step = 0.05D; // 很小的位移
        double x = mob.getX() + dx / dist * step;
        double z = mob.getZ() + dz / dist * step;

        mob.getMoveControl().setWantedPosition(x, mob.getY(), z, 0.1D); // 低速
    }
}
