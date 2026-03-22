package com.vomiter.neurolib.common.entity.stalker;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class StalkerAvoidEyeSightGoal extends Goal {

    private final Mob stalker;
    private final double moveSpeed;
    private final Config config;

    private final StalkerAIHelper helper = new StalkerAIHelper();

    private long freezeUntilTick;
    private long nextRepathTick;
    private long nextAllowTick;
    private double scoreUntilGiveUp;
    private long committedMoveUntilTick = 0L;

    public StalkerAvoidEyeSightGoal(Mob stalker, double moveSpeed) {
        this(stalker, moveSpeed, Config.DEFAULT);
    }

    public StalkerAvoidEyeSightGoal(Mob stalker, double moveSpeed, Config config) {
        this.stalker = stalker;
        this.moveSpeed = moveSpeed;
        this.config = config;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = stalker.getTarget();
        if (gameTime() < nextAllowTick) return false;
        if (!isValidTarget(target)) return false;
        if (stalker.distanceToSqr(target) > config.activationDistanceSqr()) return false;
        if (!StalkerAvoidEyeSightLimiter.canStart(stalker)) return false;

        return helper.computeDot(stalker, target) > config.watchDotThreshold()
                && target.hasLineOfSight(stalker);
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = stalker.getTarget();
        if (scoreUntilGiveUp <= 0.0D) return false;
        if (!isValidTarget(target)) return false;
        if (stalker.distanceToSqr(target) > config.activationDistanceSqr()) return false;
        if (!StalkerAvoidEyeSightLimiter.isActive(stalker)) return false;

        return helper.computeDot(stalker, target) > config.watchDotThreshold();
    }

    @Override
    public void start() {
        if (!StalkerAvoidEyeSightLimiter.tryRegister(stalker)) {
            this.scoreUntilGiveUp = -1.0D;
            return;
        }

        long now = gameTime();
        this.freezeUntilTick = now + config.freezeTicksWhenSpotted();
        this.nextRepathTick = this.freezeUntilTick;
        this.scoreUntilGiveUp = config.initialScoreUntilGiveUp();
        stopMovement();
    }

    @Override
    public void stop() {
        //NeuroLib.LOGGER.info("[NeuroLib] STOP STALKING");
        stopMovement();
        StalkerAvoidEyeSightLimiter.unregister(stalker);

        long cooldown = scoreUntilGiveUp <= 0.0D
                ? config.cooldownAfterFailTicks()
                : config.cooldownTicks();

        this.nextAllowTick = gameTime() + cooldown;
    }

    @Override
    public void tick() {

        LivingEntity target = stalker.getTarget();
        if (!isValidTarget(target)) return;

        long now = gameTime();
        if (now < committedMoveUntilTick) {
            return;
        }

        if (stalker instanceof IStalkerMob stalkerMob) {
            var nearestGoalTarget = stalkerMob.neuroLib$getNearestGoal();
            if (nearestGoalTarget != null) {
                nearestGoalTarget.suppressForgetting(config.suppressForgettingTicks());
            }
        }

        if (stalker.getNavigation().isDone()) {
            stalker.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        if (now < freezeUntilTick) {
            stopMovement();
            return;
        }

        if (!stalker.getNavigation().isDone()) {
            return;
        }

        if (now < nextRepathTick) {
            return;
        }

        helper.moveToLowerDotPosition(
                stalker,
                target,
                moveSpeed,
                config.stepDistance(),
                config.idealRadius()
        );
        committedMoveUntilTick = now + 8L;

        nextRepathTick = now + config.repathIntervalTicks();
        scoreUntilGiveUp -= (helper.computeDot(stalker, target) - helper.computeCoverBonus(stalker, target, stalker.getEyePosition())) * config.repathIntervalTicks();
    }

    public void giveUp() {
        this.scoreUntilGiveUp = -1.0D;
    }

    private boolean isValidTarget(LivingEntity target) {
        return target != null && target.isAlive();
    }

    private long gameTime() {
        return stalker.level().getGameTime();
    }

    private void stopMovement() {
        stalker.getNavigation().stop();
        stalker.setZza(0.0F);
        stalker.setXxa(0.0F);
    }

    public void reactToHurt(DamageSource source){
        if(source == null) return;
        if(source.getEntity() != null){
            if(config.shouldGiveUpWhenHurt()) giveUp();
        }
    }

    public record Config(
            double activationDistanceSqr,
            double watchDotThreshold,
            int freezeTicksWhenSpotted,
            int repathIntervalTicks,
            double initialScoreUntilGiveUp,
            int cooldownTicks,
            int cooldownAfterFailTicks,
            int suppressForgettingTicks,
            double stepDistance,
            double idealRadius,
            boolean shouldGiveUpWhenHurt
    ) {
        public static final Config DEFAULT = new Config(
                16.0D * 16.0D, // activationDistanceSqr
                0.1D,          // watchDotThreshold
                30,            // freezeTicksWhenSpotted
                5,             // repathIntervalTicks
                100.0D,        // initialScoreUntilGiveUp
                100,           // cooldownTicks
                600,           // cooldownAfterFailTicks
                600,           // suppressForgettingTicks
                1.75D,         // stepDistance
                5.0D,           // idealRadius
                true            // should give up avoidance after hurt by entity
        );
    }

    public boolean neuroLib$marker_can_use_0314() {
        return true;
    }
}