package com.vomiter.neurolib.common.entity.gather;

import com.vomiter.neurolib.common.entity.generic.ICooldownGoal;
import com.vomiter.neurolib.common.entity.generic.IIntervalAttemptGoal;
import com.vomiter.neurolib.common.entity.generic.IIntervalExecuteGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

public abstract class MobMoveToDroppedItemGoal<T extends PathfinderMob> extends Goal implements ICooldownGoal, IIntervalAttemptGoal, IIntervalExecuteGoal {

    protected final T mob;
    protected final double speed;
    protected final int scanIntervalTicks;
    protected final double searchRadius;

    protected ItemEntity targetItem;
    protected long nextScanTick;
    protected long nextAllowedActionTick;
    protected long nextRepathTick = 0L;

    protected final int lossOfSightMaxTicks;
    protected int lossOfSightTicks = 0;

    protected final int repathIntervalTicks;
    protected final double interactHorizontalRange;
    protected final double interactVerticalRange;

    protected final DroppedItemTargetingHelper.FailCache failCache;
    protected final DroppedItemTargetingHelper.ProgressTracker progress;

    protected boolean reachedTargetThisRun = false;

    protected MobMoveToDroppedItemGoal(
            T mob,
            double speed,
            int scanIntervalTicks,
            double searchRadius,
            int failTtlTicks,
            int stuckCheckIntervalTicks,
            int stuckMaxTicks,
            double stuckMinProgress,
            int lossOfSightMaxTicks,
            int repathIntervalTicks,
            double interactHorizontalRange,
            double interactVerticalRange
    ) {
        this.mob = mob;
        this.speed = speed;
        this.scanIntervalTicks = Math.max(1, scanIntervalTicks);
        this.searchRadius = Math.max(1.0D, searchRadius);
        this.lossOfSightMaxTicks = Math.max(1, lossOfSightMaxTicks);
        this.repathIntervalTicks = Math.max(1, repathIntervalTicks);
        this.interactHorizontalRange = Math.max(0.1D, interactHorizontalRange);
        this.interactVerticalRange = Math.max(0.1D, interactVerticalRange);

        this.failCache = new DroppedItemTargetingHelper.FailCache(failTtlTicks);
        this.progress = new DroppedItemTargetingHelper.ProgressTracker(
                stuckCheckIntervalTicks,
                stuckMaxTicks,
                stuckMinProgress
        );

        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public long getNextAttemptTick() {
        return nextScanTick;
    }

    @Override
    public void setNextAttemptTick(long l) {
        nextScanTick = l;
    }

    @Override
    public long getAttemptIntervalTicks() {
        return scanIntervalTicks;
    }

    public long getNextAllowedTick() {
        return nextAllowedActionTick;
    }

    public void setNextAllowedTick(long l) {
        nextAllowedActionTick = l;
    }

    public long getCooldownTicks() {
        return getActionCooldownTicks();
    }

    @Override
    public long getNextExecuteTick() {
        return nextRepathTick;
    }

    @Override
    public void setNextExecuteTick(long l) {
        nextRepathTick = l;
    }

    @Override
    public long getExecuteIntervalTicks() {
        return repathIntervalTicks;
    }

    @Override
    public Consumer<IIntervalExecuteGoal> getExecutable() {
        return goal -> moveToTarget();
    }

    @Override
    public boolean canUse() {
        if (mob.level().isClientSide()) return false;
        if (!isGoalEnabled()) return false;
        if (!canStartAction()) return false;
        if (isInCooldown()) return false;
        if (isInAttemptInterval()) return false;

        startAttemptInterval();
        targetItem = findNearestItem();
        return targetItem != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (mob.level().isClientSide()) return false;
        if (!isGoalEnabled()) return false;
        if (!canContinueAction()) return false;
        if (targetItem == null || !targetItem.isAlive()) return false;
        if (targetItem.getItem().isEmpty()) return false;

        if (lossOfSightTicks >= lossOfSightMaxTicks) {
            failCache.markFailed(targetItem, mob.level().getGameTime());
            lossOfSightTicks = 0;
            return false;
        }

        return mob.distanceToSqr(targetItem) <= (searchRadius * searchRadius);
    }

    @Override
    public void start() {
        onStart();
        reachedTargetThisRun = false;
        lossOfSightTicks = 0;

        if (targetItem != null) {
            execute();
            long now = mob.level().getGameTime();
            progress.reset(now, horizontalDistanceToSqr(targetItem));
        } else {
            progress.clear();
        }
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        targetItem = null;
        lossOfSightTicks = 0;
        setNextExecuteTick(mob.level().getGameTime());
        progress.clear();

        if (reachedTargetThisRun) {
            startCooldown();
        }

        reachedTargetThisRun = false;
        onStop();
    }

    @Override
    public void tick() {
        if (targetItem == null || !targetItem.isAlive()) {
            return;
        }

        if (!mob.hasLineOfSight(targetItem)) {
            lossOfSightTicks++;
        } else {
            lossOfSightTicks = 0;
        }

        long now = mob.level().getGameTime();
        double dist2 = horizontalDistanceToSqr(targetItem);

        if (progress.update(now, dist2)) {
            failCache.markFailed(targetItem, now);
            mob.getNavigation().stop();
            targetItem = null;
            return;
        }

        mob.getLookControl().setLookAt(targetItem, 30.0F, 30.0F);

        if (!isCloseEnoughToInteract(targetItem) && mob.getNavigation().isDone()) {
            nudgeTowardTarget();
        }

        if (canInteractNow(targetItem)) {
            mob.getNavigation().stop();
            reachedTargetThisRun = true;
            onReachedTarget(targetItem);
            targetItem = null;
            return;
        }

        // execute() 內部自帶 interval 檢查
        execute();
    }

    protected ItemEntity findNearestItem() {
        long now = mob.level().getGameTime();

        List<ItemEntity> items = DroppedItemTargetingHelper.queryItems(mob, searchRadius, it -> {
            if (!it.isAlive()) return false;
            if (failCache.shouldSkip(it, now)) return false;

            ItemStack stack = it.getItem();
            if (stack.isEmpty()) return false;

            return isValidTarget(stack, it);
        });

        if (items.isEmpty()) return null;
        return DroppedItemTargetingHelper.findNearestItemEntity(items, this::distanceToCandidate);
    }

    protected double distanceToCandidate(ItemEntity item) {
        return mob.distanceToSqr(item);
    }

    protected void moveToTarget() {
        if (targetItem == null || !targetItem.isAlive()) return;

        mob.getNavigation().moveTo(
                targetItem.getX(),
                targetItem.getY(),
                targetItem.getZ(),
                speed
        );
        mob.getLookControl().setLookAt(targetItem, 30.0F, 30.0F);
    }

    protected boolean canInteractNow(ItemEntity item) {
        return item.onGround()
                && mob.hasLineOfSight(item)
                && isCloseEnoughToInteract(item);
    }

    protected double horizontalDistanceToSqr(ItemEntity item) {
        double dx = mob.getX() - item.getX();
        double dz = mob.getZ() - item.getZ();
        return dx * dx + dz * dz;
    }

    protected void nudgeTowardTarget() {
        if (targetItem == null) return;

        double dx = targetItem.getX() - mob.getX();
        double dz = targetItem.getZ() - mob.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0e-4D) return;

        double push = getNudgeStrength();
        mob.setDeltaMovement(
                mob.getDeltaMovement().x + dx / len * push,
                mob.getDeltaMovement().y,
                mob.getDeltaMovement().z + dz / len * push
        );
    }

    protected boolean isCloseEnoughToInteract(ItemEntity item) {
        double dx = mob.getX() - item.getX();
        double dz = mob.getZ() - item.getZ();
        double dy = Math.abs(mob.getY() - item.getY());

        return dx * dx + dz * dz <= interactHorizontalRange * interactHorizontalRange
                && dy <= interactVerticalRange;
    }

    protected double getNudgeStrength() {
        return 0.08D;
    }

    protected void markCurrentTargetFailed() {
        if (targetItem != null) {
            failCache.markFailed(targetItem, mob.level().getGameTime());
        }
    }

    protected abstract boolean isGoalEnabled();

    protected abstract boolean canStartAction();

    protected abstract boolean canContinueAction();

    protected abstract boolean isValidTarget(ItemStack stack, ItemEntity entity);

    protected abstract void onReachedTarget(ItemEntity target);

    protected abstract int getActionCooldownTicks();

    protected void onStart() {
    }

    protected void onStop() {
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}