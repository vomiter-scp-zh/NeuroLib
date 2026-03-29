package com.vomiter.neurolib.common.entity.stalker;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 一個建立在 MeleeAttackGoal 之上的 stalk / investigate / search 版本：
 * CHASE:
 *   - 直接沿用 super.tick()
 *   - 有 LOS 時持續更新 lastSeenPos
 * INVESTIGATE:
 *   - 失去 LOS 後，前往最後一次看見 target 的位置
 * SEARCH:
 *   - 到達 last seen 後，原地左右張望一段時間
 * 結束條件：
 *   - SEARCH 超時仍未重新看到 target
 *   - 或 INVESTIGATE 超時
 * 注意：
 *   - super(...) 的第三個參數一定要用 false
 *   - 否則原版 MeleeAttackGoal 會在看不到 target 時仍持續追 target
 */
public class StalkerMeleeAttackGoal extends MeleeAttackGoal {

    protected enum Phase {
        CHASE,
        INVESTIGATE,
        SEARCH
    }

    protected final PathfinderMob stalker;
    protected final double stalkSpeedModifier;

    @Nullable
    protected Vec3 lastSeenPos = null;

    /**
     * 原版 target goal 可能會提早把 mob.setTarget(null)，
     * 所以這裡自己保留一份短期記憶。
     */
    @Nullable
    protected LivingEntity rememberedTarget = null;

    protected Phase phase = Phase.CHASE;

    /**
     * SEARCH 階段開始與截止 world tick
     */
    protected long searchStartGameTick = -1L;
    protected long searchUntilGameTick = -1L;

    /**
     * INVESTIGATE 階段截止 world tick
     */
    protected long investigateUntilGameTick = -1L;

    /**
     * INVESTIGATE 階段重新下發 moveTo 的冷卻
     */
    protected int ticksUntilNextInvestigateRepath = 0;

    /**
     * SEARCH 左右張望的基準 yaw
     */
    protected float searchBaseYaw = 0.0F;

    /**
     * 可調參數
     */
    protected final int searchDurationTicks;
    protected final int investigateTimeoutTicks;
    protected final double arrivedDistanceSqr;
    protected final int investigateRepathMin;
    protected final int investigateRepathMax;

    public StalkerMeleeAttackGoal(PathfinderMob mob, double speedModifier) {
        this(mob, speedModifier, 60, 60, 1.75D, 6, 10);
    }

    public StalkerMeleeAttackGoal(PathfinderMob mob,
                                  double speedModifier,
                                  int searchDurationTicks,
                                  int investigateTimeoutTicks,
                                  double arrivedDistance,
                                  int investigateRepathMin,
                                  int investigateRepathMax) {
        super(mob, speedModifier, false);
        this.stalker = mob;
        this.stalkSpeedModifier = speedModifier;
        this.searchDurationTicks = searchDurationTicks;
        this.investigateTimeoutTicks = investigateTimeoutTicks;
        this.arrivedDistanceSqr = arrivedDistance * arrivedDistance;
        this.investigateRepathMin = investigateRepathMin;
        this.investigateRepathMax = Math.max(investigateRepathMin, investigateRepathMax);
    }

    @Override
    public boolean canUse() {
        if (!super.canUse()) {
            return false;
        }

        LivingEntity target = stalker.getTarget();
        if (!isValidTarget(target)) {
            return false;
        }

        rememberedTarget = target;
        phase = Phase.CHASE;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity liveTarget = stalker.getTarget();

        if (isValidTarget(liveTarget)) {
            return true;
        }

        return phase == Phase.INVESTIGATE || phase == Phase.SEARCH;
    }

    @Override
    public void start() {
        super.start();

        LivingEntity target = stalker.getTarget();
        if(target == null) return;
        if (isValidTarget(target)) {
            rememberedTarget = target;
            lastSeenPos = target.position();
        } else {
            rememberedTarget = null;
            lastSeenPos = null;
        }

        phase = Phase.CHASE;
        searchStartGameTick = -1L;
        searchUntilGameTick = -1L;
        investigateUntilGameTick = -1L;
        ticksUntilNextInvestigateRepath = 0;
    }

    @Override
    public void stop() {
        super.stop();

        phase = Phase.CHASE;
        rememberedTarget = null;
        lastSeenPos = null;
        searchStartGameTick = -1L;
        searchUntilGameTick = -1L;
        investigateUntilGameTick = -1L;
        ticksUntilNextInvestigateRepath = 0;
    }

    @Override
    public void tick() {
        LivingEntity liveTarget = stalker.getTarget();
        LivingEntity target = isValidTarget(liveTarget) ? liveTarget : rememberedTarget;

        boolean hasTarget = isValidTarget(target);
        boolean hasLOS = target != null && hasTarget && stalker.getSensing().hasLineOfSight(target);

        // 有視線時：更新 lastSeenPos，並保證狀態回 CHASE
        if (hasLOS) {
            rememberedTarget = target;
            lastSeenPos = target.position();

            if (phase != Phase.CHASE) {
                phase = Phase.CHASE;
                searchStartGameTick = -1L;
                searchUntilGameTick = -1L;
                investigateUntilGameTick = -1L;
                ticksUntilNextInvestigateRepath = 0;
            }
        }

        switch (phase) {
            case CHASE -> tickChasePhase(target, hasLOS);
            case INVESTIGATE -> tickInvestigatePhase(target, hasLOS);
            case SEARCH -> tickSearchPhase(target, hasLOS);
        }
    }

    protected void tickChasePhase(@Nullable LivingEntity target, boolean hasLOS) {
        super.tick();

        if (isValidTarget(target) && !hasLOS && lastSeenPos != null) {
            beginInvestigate();
        }
    }

    protected void beginInvestigate() {
        phase = Phase.INVESTIGATE;
        investigateUntilGameTick = stalker.level().getGameTime() + investigateTimeoutTicks;
        ticksUntilNextInvestigateRepath = 0;

        if (lastSeenPos != null) {
            stalker.getNavigation().moveTo(lastSeenPos.x, lastSeenPos.y, lastSeenPos.z, stalkSpeedModifier);
        }
    }

    protected void tickInvestigatePhase(@Nullable LivingEntity target, boolean hasLOS) {
        if (isValidTarget(target) && hasLOS) {
            phase = Phase.CHASE;
            investigateUntilGameTick = -1L;
            return;
        }

        if (lastSeenPos == null) {
            clearTargetAndStopSearching();
            return;
        }

        ticksUntilNextInvestigateRepath = Math.max(0, ticksUntilNextInvestigateRepath - 1);

        stalker.getLookControl().setLookAt(
                lastSeenPos.x,
                lastSeenPos.y + 1.0D,
                lastSeenPos.z,
                30.0F,
                30.0F
        );

        if (ticksUntilNextInvestigateRepath <= 0) {
            ticksUntilNextInvestigateRepath = randomInvestigateRepathDelay();
            stalker.getNavigation().moveTo(lastSeenPos.x, lastSeenPos.y, lastSeenPos.z, stalkSpeedModifier);
        }

        boolean reachedLastSeen = stalker.position().distanceToSqr(lastSeenPos) <= arrivedDistanceSqr;
        boolean navDone = stalker.getNavigation().isDone();

        if (reachedLastSeen || navDone) {
            beginSearch();
            return;
        }

        if (investigateUntilGameTick >= 0L && stalker.level().getGameTime() >= investigateUntilGameTick) {
            clearTargetAndStopSearching();
        }
    }

    protected void beginSearch() {
        phase = Phase.SEARCH;
        searchStartGameTick = stalker.level().getGameTime();
        searchUntilGameTick = searchStartGameTick + searchDurationTicks;
        searchBaseYaw = stalker.getYRot();
        stalker.getNavigation().stop();
    }

    protected void tickSearchPhase(@Nullable LivingEntity target, boolean hasLOS) {
        if (isValidTarget(target) && hasLOS) {
            phase = Phase.CHASE;
            searchStartGameTick = -1L;
            searchUntilGameTick = -1L;
            return;
        }

        doLookAround();

        if (searchUntilGameTick >= 0L && stalker.level().getGameTime() >= searchUntilGameTick) {
            clearTargetAndStopSearching();
        }
    }

    protected void clearTargetAndStopSearching() {
        phase = Phase.CHASE;
        lastSeenPos = null;
        searchStartGameTick = -1L;
        searchUntilGameTick = -1L;
        investigateUntilGameTick = -1L;
        ticksUntilNextInvestigateRepath = 0;

        stalker.getNavigation().stop();
        stalker.setTarget(null);
        rememberedTarget = null;
    }

    protected void doLookAround() {
        if (searchStartGameTick < 0L) {
            return;
        }

        long now = stalker.level().getGameTime();
        float progress = (float) (now - searchStartGameTick);

        float yawOffset = Mth.sin(progress * 0.35F) * 55.0F;
        float targetYaw = searchBaseYaw + yawOffset;

        stalker.setYRot(targetYaw);
        stalker.setYHeadRot(targetYaw);
        stalker.setYBodyRot(targetYaw);
    }

    protected int randomInvestigateRepathDelay() {
        int bound = investigateRepathMax - investigateRepathMin + 1;
        return adjustedTickDelay(investigateRepathMin + stalker.getRandom().nextInt(bound));
    }

    protected boolean isValidTarget(@Nullable LivingEntity target) {
        if (target == null) return false;
        if (!target.isAlive()) return false;
        if (!stalker.canAttack(target)) return false;
        //if (!stalker.isWithinRestriction(target.blockPosition())) return false;

        if (target instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
        }

        return true;
    }
}