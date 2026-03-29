package com.vomiter.neurolib.common.entity.gather.eat;

import com.vomiter.neurolib.common.entity.gather.MobMoveToDroppedItemGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public abstract class MobEatDroppedItemGoal<T extends PathfinderMob> extends MobMoveToDroppedItemGoal<T> {

    protected final MobEatingFx eatingFx;

    protected MobEatDroppedItemGoal(
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
            double interactVerticalRange,
            int eatingFxDurationTicks
    ) {
        super(
                mob,
                speed,
                scanIntervalTicks,
                searchRadius,
                failTtlTicks,
                stuckCheckIntervalTicks,
                stuckMaxTicks,
                stuckMinProgress,
                lossOfSightMaxTicks,
                repathIntervalTicks,
                interactHorizontalRange,
                interactVerticalRange
        );
        this.eatingFx = new MobEatingFx(eatingFxDurationTicks);
    }

    @Override
    public boolean canUse() {
        if (eatingFx.isEating()) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (mob.level().isClientSide()) {
            return false;
        }
        if (eatingFx.isEating()) {
            return true;
        }
        return super.canContinueToUse();
    }

    @Override
    public void tick() {
        if (eatingFx.isEating()) {
            eatingFx.tick(mob);
            return;
        }
        super.tick();
    }

    @Override
    protected final boolean isValidTarget(ItemStack stack, ItemEntity entity) {
        return isEdible(stack, entity);
    }

    @Override
    protected final void onReachedTarget(ItemEntity target) {
        ItemStack stack = target.getItem();
        if (stack.isEmpty()) {
            return;
        }

        ItemStack bite = stack.split(1);
        if (target.getItem().isEmpty()) {
            target.discard();
        }

        onAteFood(bite, target);
        eatingFx.start(bite);
    }

    protected void playDefaultEatSound() {
        MobEatingFx.playDefaultBiteSounds(mob);
    }

    protected abstract boolean isEdible(ItemStack stack, ItemEntity entity);

    protected abstract void onAteFood(ItemStack bite, ItemEntity source);
}