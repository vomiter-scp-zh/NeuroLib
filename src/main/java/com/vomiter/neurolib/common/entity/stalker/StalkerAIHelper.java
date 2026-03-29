package com.vomiter.neurolib.common.entity.stalker;

import com.vomiter.neurolib.NeuroLib;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StalkerAIHelper {

    private static final boolean DEBUG = false;

    /**
     * 有實體不透明遮蔽時給的加分。
     * 可以先從 0.25 ~ 0.45 之間調。
     */
    private static final double COVER_BONUS = 0.35D;

    public boolean isInTargetView(LivingEntity stalker, LivingEntity target, double watchDotThreshold) {
        return computeDot(stalker, target) > watchDotThreshold && target.hasLineOfSight(stalker);
    }

    public double computeDot(LivingEntity stalker, LivingEntity target) {
        Vec3 view = horizontalForward(target);
        Vec3 toStalker = new Vec3(
                stalker.getX() - target.getX(),
                0.0D,
                stalker.getZ() - target.getZ()
        );

        if (toStalker.lengthSqr() < 1.0E-6D) {
            return 1.0D;
        }

        return view.dot(toStalker.normalize());
    }

    public double computeDotAt(LivingEntity target, double stalkerX, double stalkerZ) {
        Vec3 view = horizontalForward(target);
        Vec3 toPos = new Vec3(
                stalkerX - target.getX(),
                0.0D,
                stalkerZ - target.getZ()
        );

        if (toPos.lengthSqr() < 1.0E-6D) {
            return 1.0D;
        }

        return view.dot(toPos.normalize());
    }

    public boolean moveToLowerDotPosition(
            Mob stalker,
            LivingEntity target,
            double moveSpeed,
            double stepDistance,
            double idealRadius
    ) {
        PathCandidate best = findBestLowerDotPath(stalker, target, stepDistance, idealRadius);
        if (best == null) {
            debug("moveToLowerDotPosition result: no valid candidate");
            return false;
        }

        boolean started = stalker.getNavigation().moveTo(best.path, moveSpeed);
        debug("moveToLowerDotPosition choose best=%s score=%.3f moveStarted=%s pathNodes=%d",
                fmt(best.pos), best.score, started, best.path.getNodeCount());

        return started;
    }

    @Nullable
    public PathCandidate findBestLowerDotPath(
            Mob stalker,
            LivingEntity target,
            double stepDistance,
            double idealRadius
    ) {
        final double currentDot = computeDot(stalker, target);
        final double currentDist = horizontalDistance(stalker.getX(), stalker.getZ(), target.getX(), target.getZ());

        final Vec3 targetForward = horizontalForward(target);
        final Vec3 left = leftOf(targetForward);
        final Vec3 back = targetForward.scale(-1.0D);

        debug("findBestLowerDotPath start currentPos=%s targetPos=%s currentDot=%.3f currentDist=%.3f step=%.2f idealRadius=%.2f",
                fmt(stalker.position()), fmt(target.position()), currentDot, currentDist, stepDistance, idealRadius);

        Candidate[] candidates = new Candidate[] {
                new Candidate("left", left, 1.00D),
                new Candidate("right", left.scale(-1.0D), 1.00D),
                new Candidate("back_left", back.add(left).normalize(), 1.00D),
                new Candidate("back_right", back.add(left.scale(-1.0D)).normalize(), 1.00D),
                new Candidate("soft_back_left", back.scale(0.65D).add(left).normalize(), 1.00D),
                new Candidate("soft_back_right", back.scale(0.65D).add(left.scale(-1.0D)).normalize(), 1.00D),
                new Candidate("wide_left", rotateHorizontal(left, Math.toRadians(35.0D)), 1.00D),
                new Candidate("wide_right", rotateHorizontal(left.scale(-1.0D), Math.toRadians(-35.0D)), 1.00D)
        };

        // 先做便宜的幾何預評分
        List<PreScoredCandidate> preScored = new ArrayList<>(candidates.length);

        for (Candidate candidateDef : candidates) {
            Vec3 stepDir = candidateDef.direction.normalize();
            Vec3 candidatePos = stalker.position().add(stepDir.scale(stepDistance * candidateDef.stepScale));
            candidatePos = new Vec3(candidatePos.x, stalker.getY(), candidatePos.z);

            double candidateDot = computeDotAt(target, candidatePos.x, candidatePos.z);
            double dotReduction = currentDot - candidateDot;
            double candidateDist = horizontalDistance(candidatePos.x, candidatePos.z, target.getX(), target.getZ());

            double radiusPenalty = Math.abs(candidateDist - idealRadius) * 0.20D;
            double sideBias = sideBiasScore(target, stalker, candidatePos);
            double coverBonus = computeCoverBonus(stalker, target, candidatePos);

            double preScore = dotReduction + sideBias + coverBonus - radiusPenalty;

            String rejectReason = null;
            if (dotReduction <= 0.02D) {
                rejectReason = String.format(Locale.ROOT, "dot_not_improved_enough(%.3f)", dotReduction);
            } else if (preScore <= -0.15D) {
                rejectReason = String.format(Locale.ROOT, "prescore_too_low(%.3f)", preScore);
            }

            if (rejectReason != null) {
                debug("candidate %-16s pos=%s candDot=%.3f reduction=%.3f candDist=%.3f radiusPenalty=%.3f sideBias=%.3f coverBonus=%.3f preScore=%.3f REJECT=%s",
                        candidateDef.name, fmt(candidatePos), candidateDot, dotReduction, candidateDist, radiusPenalty, sideBias, coverBonus, preScore, rejectReason);
                continue;
            }

            debug("candidate %-16s pos=%s candDot=%.3f reduction=%.3f candDist=%.3f radiusPenalty=%.3f sideBias=%.3f coverBonus=%.3f preScore=%.3f PREPASS",
                    candidateDef.name, fmt(candidatePos), candidateDot, dotReduction, candidateDist, radiusPenalty, sideBias, coverBonus, preScore);

            preScored.add(new PreScoredCandidate(
                    candidateDef.name,
                    candidatePos,
                    candidateDot,
                    dotReduction,
                    candidateDist,
                    radiusPenalty,
                    sideBias,
                    coverBonus,
                    preScore
            ));
        }

        if (preScored.isEmpty()) {
            debug("findBestLowerDotPath result best=null bestScore=-INF reason=no_prepassed_candidate");
            return null;
        }

        // 只保留幾何上最有希望的前幾名
        preScored.sort((a, b) -> Double.compare(b.preScore, a.preScore));
        final int maxPathChecks = Math.min(3, preScored.size());

        PathCandidate best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < maxPathChecks; i++) {
            PreScoredCandidate c = preScored.get(i);

            Path path = stalker.getNavigation().createPath(BlockPos.containing(c.pos.x, c.pos.y, c.pos.z), 0);

            String rejectReason = null;
            if (path == null) {
                rejectReason = "path=null";
            } else if (!path.canReach()) {
                rejectReason = "path_unreachable";
            }

            if (rejectReason != null) {
                debug("candidate %-16s pos=%s candDot=%.3f reduction=%.3f candDist=%.3f radiusPenalty=%.3f sideBias=%.3f coverBonus=%.3f preScore=%.3f REJECT=%s",
                        c.name, fmt(c.pos), c.candidateDot, c.dotReduction, c.candidateDist, c.radiusPenalty, c.sideBias, c.coverBonus, c.preScore, rejectReason);
                continue;
            }

            // 對 path 本身加一點懲罰，避免繞太大圈
            double pathPenalty = path.getNodeCount() * 0.015D;
            double finalScore = c.preScore - pathPenalty;

            debug("candidate %-16s pos=%s candDot=%.3f reduction=%.3f candDist=%.3f radiusPenalty=%.3f sideBias=%.3f coverBonus=%.3f preScore=%.3f pathNodes=%d pathPenalty=%.3f finalScore=%.3f ACCEPT",
                    c.name, fmt(c.pos), c.candidateDot, c.dotReduction, c.candidateDist, c.radiusPenalty, c.sideBias, c.coverBonus, c.preScore, path.getNodeCount(), pathPenalty, finalScore);

            if (finalScore > bestScore) {
                bestScore = finalScore;
                best = new PathCandidate(c.pos, path, finalScore);
            }
        }

        debug("findBestLowerDotPath result best=%s bestScore=%.3f",
                best == null ? "null" : fmt(best.pos),
                best == null ? Double.NEGATIVE_INFINITY : best.score);

        return best;
    }

    public Vec3 horizontalForward(LivingEntity entity) {
        Vec3 look = entity.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);

        if (flat.lengthSqr() < 1.0E-6D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }

        return flat.normalize();
    }

    private Vec3 leftOf(Vec3 forward) {
        return new Vec3(-forward.z, 0.0D, forward.x).normalize();
    }

    private Vec3 rotateHorizontal(Vec3 vec, double angleRad) {
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        return new Vec3(
                vec.x * cos - vec.z * sin,
                0.0D,
                vec.x * sin + vec.z * cos
        ).normalize();
    }

    /**
     * 稍微偏好「維持目前在 target 左右哪一側」，避免每次重算都左右亂跳。
     */
    private double sideBiasScore(LivingEntity target, LivingEntity stalker, Vec3 candidate) {
        Vec3 forward = horizontalForward(target);
        Vec3 left = leftOf(forward);

        Vec3 currentRel = new Vec3(
                stalker.getX() - target.getX(),
                0.0D,
                stalker.getZ() - target.getZ()
        );

        Vec3 candidateRel = new Vec3(
                candidate.x - target.getX(),
                0.0D,
                candidate.z - target.getZ()
        );

        if (currentRel.lengthSqr() < 1.0E-6D || candidateRel.lengthSqr() < 1.0E-6D) {
            return 0.0D;
        }

        double currentSide = Math.signum(left.dot(currentRel.normalize()));
        double candidateSide = Math.signum(left.dot(candidateRel.normalize()));

        return currentSide != 0.0D && currentSide == candidateSide ? 0.08D : 0.0D;
    }

    /**
     * 如果 target 到 candidate 之間有可遮蔽視線的實體不透明方塊，給予加分。
     * 這裡用 eye-to-eye 的 clip，比單純看中心點更接近實際視線遮蔽。
     */
    public double computeCoverBonus(Mob stalker, LivingEntity target, Vec3 candidate) {
        Vec3 from = target.getEyePosition();
        Vec3 to = new Vec3(candidate.x, stalker.getY() + stalker.getEyeHeight(), candidate.z);

        BlockHitResult hit = stalker.level().clip(new ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                target
        ));

        if (hit.getType() != HitResult.Type.BLOCK) {
            return 0.0D;
        }

        BlockPos hitPos = hit.getBlockPos();
        BlockState state = stalker.level().getBlockState(hitPos);

        boolean occluding = state.canOcclude();
        boolean solidRender = state.isSolidRender();

        return (occluding && solidRender) ? COVER_BONUS : 0.0D;
    }

    private double horizontalDistance(double ax, double az, double bx, double bz) {
        double dx = ax - bx;
        double dz = az - bz;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private void debug(String msg, Object... args) {
        if (!DEBUG) return;
        NeuroLib.LOGGER.info("[StalkerAIHelper] {}", String.format(Locale.ROOT, msg, args));
    }

    private String fmt(Vec3 vec) {
        return String.format(Locale.ROOT, "(%.2f, %.2f, %.2f)", vec.x, vec.y, vec.z);
    }

    private record Candidate(String name, Vec3 direction, double stepScale) {
    }

    public record PathCandidate(Vec3 pos, Path path, double score) {
    }

    private record PreScoredCandidate(String name, Vec3 pos, double candidateDot, double dotReduction,
                                      double candidateDist, double radiusPenalty, double sideBias, double coverBonus,
                                      double preScore){
    }

}