package com.vomiter.neurolib.common.entity.gather;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

public final class DroppedItemTargetingHelper {

    private DroppedItemTargetingHelper() {
    }

    public static final class FailCache {
        private final Map<UUID, Long> failedUntil = new HashMap<>();
        private final int ttlTicks;

        public FailCache(int ttlTicks) {
            this.ttlTicks = Math.max(1, ttlTicks);
        }

        public boolean shouldSkip(ItemEntity item, long now) {
            Long until = failedUntil.get(item.getUUID());
            if (until == null) {
                return false;
            }
            if (until > now) {
                return true;
            }
            failedUntil.remove(item.getUUID());
            return false;
        }

        public void markFailed(ItemEntity item, long now) {
            failedUntil.put(item.getUUID(), now + ttlTicks);
        }

        public void clear(ItemEntity item) {
            failedUntil.remove(item.getUUID());
        }

        public void clearAll() {
            failedUntil.clear();
        }
    }

    public static ItemEntity findNearestItemEntity(List<ItemEntity> items, ToDoubleFunction<ItemEntity> distanceSqFn) {
        ItemEntity best = null;
        double bestDistanceSq = Double.MAX_VALUE;

        for (ItemEntity item : items) {
            double distanceSq = distanceSqFn.applyAsDouble(item);
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = item;
            }
        }

        return best;
    }

    public static List<ItemEntity> queryItems(Mob mob, double radius, Predicate<ItemEntity> predicate) {
        AABB box = mob.getBoundingBox().inflate(radius, 4.0D, radius);
        return mob.level().getEntitiesOfClass(ItemEntity.class, box, predicate);
    }

    public static final class ProgressTracker {
        private final int checkEveryTicks;
        private final int stuckMaxTicks;
        private final double minProgress;

        private long lastCheckTime;
        private double bestDistanceSqSinceReset;
        private int stuckTicks;

        public ProgressTracker(int checkEveryTicks, int stuckMaxTicks, double minProgress) {
            this.checkEveryTicks = Math.max(1, checkEveryTicks);
            this.stuckMaxTicks = Math.max(1, stuckMaxTicks);
            this.minProgress = Math.max(0.0D, minProgress);
            clear();
        }

        public void reset(long now, double distanceSq) {
            lastCheckTime = now;
            bestDistanceSqSinceReset = distanceSq;
            stuckTicks = 0;
        }

        /**
         * 回傳 true 表示目前可視為 stuck。
         */
        public boolean update(long now, double distanceSq) {
            long elapsed = now - lastCheckTime;
            if (elapsed < checkEveryTicks) {
                return false;
            }

            if (distanceSq < bestDistanceSqSinceReset - minProgress) {
                bestDistanceSqSinceReset = distanceSq;
                stuckTicks = 0;
            } else {
                stuckTicks += (int) elapsed;
            }

            lastCheckTime = now;
            return stuckTicks >= stuckMaxTicks;
        }

        public void clear() {
            lastCheckTime = 0L;
            bestDistanceSqSinceReset = Double.MAX_VALUE;
            stuckTicks = 0;
        }
    }
}