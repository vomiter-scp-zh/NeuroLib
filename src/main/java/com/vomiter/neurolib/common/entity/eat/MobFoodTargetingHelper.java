package com.vomiter.neurolib.common.entity.eat;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public final class MobFoodTargetingHelper {

    private MobFoodTargetingHelper() {}

    public static final class FailCache {
        private final HashMap<UUID, Long> failedUntil = new HashMap<>();
        private final int ttlTicks;

        public FailCache(int ttlTicks) {
            this.ttlTicks = ttlTicks;
        }

        public boolean shouldSkip(ItemEntity it, long now) {
            Long until = failedUntil.get(it.getUUID());
            if (until == null) return false;
            if (until > now) return true;
            failedUntil.remove(it.getUUID());
            return false;
        }

        public void markFailed(ItemEntity it, long now) {
            failedUntil.put(it.getUUID(), now + ttlTicks);
        }
    }

    public static ItemEntity findNearestItemEntity(
            List<ItemEntity> items,
            java.util.function.ToDoubleFunction<ItemEntity> dist2Fn
    ) {
        ItemEntity best = null;
        double bestDist2 = Double.MAX_VALUE;
        for (ItemEntity it : items) {
            double d2 = dist2Fn.applyAsDouble(it);
            if (d2 < bestDist2) {
                bestDist2 = d2;
                best = it;
            }
        }
        return best;
    }

    public static List<ItemEntity> queryItems(net.minecraft.world.entity.Mob mob, double radius, Predicate<ItemEntity> pred) {
        AABB box = mob.getBoundingBox().inflate(radius, 4.0, radius);
        return mob.level().getEntitiesOfClass(ItemEntity.class, box, pred);
    }

    public static final class ProgressTracker {
        private final int checkEveryTicks;
        private final int stuckMaxTicks;
        private final double epsilon;

        private long lastCheckTime;
        private double lastDist2;
        private int stuckTicks;

        public ProgressTracker(int checkEveryTicks, int stuckMaxTicks, double epsilon) {
            this.checkEveryTicks = Math.max(1, checkEveryTicks);
            this.stuckMaxTicks = Math.max(1, stuckMaxTicks);
            this.epsilon = epsilon;
        }

        public void reset(long now, double dist2) {
            lastCheckTime = now;
            lastDist2 = dist2;
            stuckTicks = 0;
        }

        // 回傳：是否 stuck
        public boolean update(long now, double dist2) {
            if (now - lastCheckTime < checkEveryTicks) return false;

            if (dist2 >= lastDist2 - epsilon) {
                stuckTicks += (int) (now - lastCheckTime);
            } else {
                stuckTicks = 0;
                lastDist2 = dist2;
            }
            lastCheckTime = now;
            return stuckTicks >= stuckMaxTicks;
        }

        public void clear() {
            lastCheckTime = 0;
            lastDist2 = Double.MAX_VALUE;
            stuckTicks = 0;
        }
    }
}