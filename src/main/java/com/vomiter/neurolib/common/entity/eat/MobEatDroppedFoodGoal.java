package com.vomiter.neurolib.common.entity.eat;

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

@Deprecated
public abstract class MobEatDroppedFoodGoal<T extends PathfinderMob> extends Goal implements ICooldownGoal, IIntervalAttemptGoal, IIntervalExecuteGoal {

    protected final T mob;
    protected final double speed;
    protected final int scanIntervalTicks;
    protected final double searchRadius;

    protected ItemEntity targetFood;
    protected long nextScanTick;
    private long nextAllowedToEatTick;
    protected long nextRepathTick = 0;

    protected final int lossOfSightMaxTicks;
    protected int lossOfSightTicks = 0;

    protected final int repathIntervalTicks;
    protected final double eatHorizontalRange;
    protected final double eatVerticalRange;

    protected final MobFoodTargetingHelper.FailCache failCache;
    protected final MobFoodTargetingHelper.ProgressTracker progress;
    protected final MobEatingFx eatingFx;

    protected MobEatDroppedFoodGoal(
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
            double eatHorizontalRange,
            double eatVerticalRange,
            int eatingFxDurationTicks
    ) {
        this.mob = mob;
        this.speed = speed;
        this.scanIntervalTicks = Math.max(1, scanIntervalTicks);
        this.searchRadius = Math.max(1.0, searchRadius);
        this.lossOfSightMaxTicks = Math.max(1, lossOfSightMaxTicks);
        this.repathIntervalTicks = Math.max(1, repathIntervalTicks);
        this.eatHorizontalRange = Math.max(0.1D, eatHorizontalRange);
        this.eatVerticalRange = Math.max(0.1D, eatVerticalRange);

        this.failCache = new MobFoodTargetingHelper.FailCache(failTtlTicks);
        this.progress = new MobFoodTargetingHelper.ProgressTracker(
                stuckCheckIntervalTicks,
                stuckMaxTicks,
                stuckMinProgress
        );
        this.eatingFx = new MobEatingFx(eatingFxDurationTicks);

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

    public long getNextAllowedTick(){
        return nextAllowedToEatTick;
    }

    public void setNextAllowedTick(long l){
        nextAllowedToEatTick = l;
    }

    public long getCooldownTicks(){
        return getEatCooldownTicks();
    }

    public long getNextExecuteTick(){
        return nextRepathTick;
    }

    public void setNextExecuteTick(long l){
        nextRepathTick = l;
    }

    public long getExecuteIntervalTicks(){
        return repathIntervalTicks;
    }

    public Consumer<IIntervalExecuteGoal> getExecutable(){
        return (goal) -> {
            moveToFood();
        };
    }



    @Override
    public boolean canUse() {
        if (mob.level().isClientSide()) return false;
        if (!isGoalEnabled()) return false;
        if (!canStartEating()) return false;
        if (eatingFx.isEating()) return false;
        if (isInCooldown()) return false;
        if (isInAttemptInterval()) return false;
        startAttemptInterval();
        targetFood = findNearestFoodItem();
        return targetFood != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (mob.level().isClientSide()) return false;
        if (eatingFx.isEating()) return true;
        if (!isGoalEnabled()) return false;
        if (!canContinueEating()) return false;
        if (targetFood == null || !targetFood.isAlive()) return false;
        if (targetFood.getItem().isEmpty()) return false;

        if (lossOfSightTicks >= lossOfSightMaxTicks) {
            failCache.markFailed(targetFood, mob.level().getGameTime());
            lossOfSightTicks = 0;
            return false;
        }

        return mob.distanceToSqr(targetFood) <= (searchRadius * searchRadius);
    }

    @Override
    public void start() {
        onStart();
        lossOfSightTicks = 0;

        if (targetFood != null) {
            execute();
            long now = mob.level().getGameTime();
            progress.reset(now, horizontalDistanceToSqr(targetFood));
        } else {
            progress.clear();
        }
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        targetFood = null;
        lossOfSightTicks = 0;
        setNextExecuteTick(mob.level().getGameTime());
        progress.clear();
        onStop();
        startCooldown();
    }

    @Override
    public void tick() {
        if (eatingFx.isEating()) {
            eatingFx.tick(mob);
            return;
        }

        if (targetFood == null || !targetFood.isAlive()) {
            return;
        }

        if (!mob.hasLineOfSight(targetFood)) {
            lossOfSightTicks++;
        } else {
            lossOfSightTicks = 0;
        }

        long now = mob.level().getGameTime();
        double dist2 = horizontalDistanceToSqr(targetFood);

        if (progress.update(now, dist2)) {
            failCache.markFailed(targetFood, now);
            mob.getNavigation().stop();
            targetFood = null;
            return;
        }

        mob.getLookControl().setLookAt(targetFood, 30.0F, 30.0F);

        if (!isCloseEnoughToEat(targetFood) && mob.getNavigation().isDone()) {
            nudgeTowardFood();
        }

        if (targetFood.onGround()
                && mob.hasLineOfSight(targetFood)
                && isCloseEnoughToEat(targetFood)) {
            mob.getNavigation().stop();
            consumeFoodEntity(targetFood);
            targetFood = null;
            return;
        }
        execute();
    }

    protected ItemEntity findNearestFoodItem() {
        long now = mob.level().getGameTime();

        List<ItemEntity> items = MobFoodTargetingHelper.queryItems(mob, searchRadius, it -> {
            if (!it.isAlive()) return false;
            if (failCache.shouldSkip(it, now)) return false;

            ItemStack stack = it.getItem();
            if (stack.isEmpty()) return false;

            return isEdible(stack, it);
        });

        if (items.isEmpty()) return null;
        return MobFoodTargetingHelper.findNearestItemEntity(items, this::distanceToCandidate);
    }

    protected double distanceToCandidate(ItemEntity item) {
        return mob.distanceToSqr(item);
    }

    protected void consumeFoodEntity(ItemEntity it) {
        ItemStack stack = it.getItem();
        if (stack.isEmpty()) return;

        ItemStack bite = stack.split(1);
        if (it.getItem().isEmpty()) {
            it.discard();
        }

        onAteFood(bite, it);
        eatingFx.start(bite);
    }

    protected void moveToFood() {
        if (targetFood == null || !targetFood.isAlive()) return;

        double tx = targetFood.getX();
        double ty = targetFood.getY();
        double tz = targetFood.getZ();

        mob.getNavigation().moveTo(tx, ty, tz, speed);
        mob.getLookControl().setLookAt(targetFood, 30.0F, 30.0F);
    }

    protected double horizontalDistanceToSqr(ItemEntity item) {
        double dx = mob.getX() - item.getX();
        double dz = mob.getZ() - item.getZ();
        return dx * dx + dz * dz;
    }

    protected void nudgeTowardFood() {
        if (targetFood == null) return;

        double dx = targetFood.getX() - mob.getX();
        double dz = targetFood.getZ() - mob.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0e-4) return;

        double push = getNudgeStrength();
        mob.setDeltaMovement(
                mob.getDeltaMovement().x + dx / len * push,
                mob.getDeltaMovement().y,
                mob.getDeltaMovement().z + dz / len * push
        );
    }

    protected boolean isCloseEnoughToEat(ItemEntity item) {
        double dx = mob.getX() - item.getX();
        double dz = mob.getZ() - item.getZ();
        double dy = Math.abs(mob.getY() - item.getY());

        return dx * dx + dz * dz <= eatHorizontalRange * eatHorizontalRange
                && dy <= eatVerticalRange;
    }

    protected double getNudgeStrength() {
        return 0.08D;
    }

    protected void playDefaultEatSound() {
        MobEatingFx.playDefaultBiteSounds(mob);
    }

    protected abstract boolean isGoalEnabled();

    protected abstract boolean canStartEating();

    protected abstract boolean canContinueEating();

    protected abstract boolean isEdible(ItemStack stack, ItemEntity entity);

    protected abstract void onAteFood(ItemStack bite, ItemEntity source);

    protected abstract int getEatCooldownTicks();

    protected void onStart() {
    }

    protected void onStop() {
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}