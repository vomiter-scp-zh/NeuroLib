package com.vomiter.neurolib.common.entity.hunt;

import com.vomiter.neurolib.util.TimeWindowHistory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractCappedHuntGoal<TMob extends Mob, TTarget extends LivingEntity>
        extends NearestAttackableTargetGoal<TTarget> {

    protected final TMob hunter;
    protected final TimeWindowHistory localHistory;

    protected AbstractCappedHuntGoal(
            TMob hunter,
            Class<TTarget> targetClass,
            int randomInterval,
            boolean mustSee,
            boolean mustReach
    ) {
        super(
                hunter,
                targetClass,
                randomInterval,
                mustSee,
                mustReach,
                (candidate, serverLevel) -> true
        );
        this.hunter = hunter;
        this.localHistory = new TimeWindowHistory(getLocalHistoryCapacity());
    }

    @Override
    public boolean canUse() {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        if (!isHuntEnabled()) {
            return false;
        }

        if (bypassAllCaps()) {
            return super.canUse();
        }

        if (!shouldSearchForTarget()) {
            return false;
        }

        long currentTime = mob.level().getGameTime();
        long lastLocalHunt = localHistory.getLatest();

        boolean cooldownEnded = lastLocalHunt < 0L
                || (currentTime - lastLocalHunt) > getHuntCooldownTicks();

        boolean localCapOk = localHistory.countRecent(currentTime, getHistoryWindowTicks())
                < getLocalHuntCap();

        boolean worldCapOk = KeyedHuntWorldData.get(serverLevel)
                .countRecent(getWorldHistoryKey(), getWorldHistoryCapacity(), currentTime, getHistoryWindowTicks())
                < getWorldHuntCap();

        return cooldownEnded && localCapOk && worldCapOk && super.canUse();
    }

    @Override
    protected void findTarget() {
        super.findTarget();

        if (this.target != null && !isValidHuntTarget((TTarget) this.target)) {
            this.target = null;
        }
    }

    @Override
    public void stop() {
        if (targetMob != null) {
            LivingEntity lastAttacker = targetMob.getLastAttacker();
        }
        super.stop();
    }

    public void recordSuccessfulHunt(long time) {
        localHistory.add(time);

        if (mob.level() instanceof ServerLevel serverLevel) {
            KeyedHuntWorldData.get(serverLevel)
                    .addTimestamp(getWorldHistoryKey(), getWorldHistoryCapacity(), time);
        }
    }

    @Override
    protected double getFollowDistance() {
        return super.getFollowDistance() * getFollowDistanceFactor();
    }

    protected boolean canAttack(LivingEntity target, @NotNull TargetingConditions conditions) {
        return super.canAttack(target, conditions) && isValidHuntTarget((TTarget) target);
    }

    protected int getLocalHistoryCapacity() {
        return 100;
    }

    protected int getWorldHistoryCapacity() {
        return 1000;
    }

    protected long getHistoryWindowTicks() {
        return 24000L;
    }

    protected double getFollowDistanceFactor() {
        return 1.0D;
    }

    protected boolean bypassAllCaps() {
        return false;
    }

    protected abstract boolean isHuntEnabled();

    protected abstract boolean shouldSearchForTarget();

    protected abstract boolean isValidHuntTarget(TTarget target);

    protected abstract String getWorldHistoryKey();

    protected abstract long getHuntCooldownTicks();

    protected abstract int getLocalHuntCap();

    protected abstract int getWorldHuntCap();
}