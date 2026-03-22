package com.vomiter.neurolib.mixin;

import com.vomiter.neurolib.NeuroLib;
import com.vomiter.neurolib.common.entity.IReasonTracker;
import com.vomiter.neurolib.common.entity.NeuroLibReasons;
import com.vomiter.neurolib.common.entity.movement.IMovementCache;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMovementCacheMixin extends Entity implements IMovementCache {

    @Unique private static final int NeuroLib_SAMPLE_SIZE = 10;

    @Unique private double neuroLib$lastX;
    @Unique private double neuroLib$lastZ;
    @Unique private double neuroLib$lastSampleDx;
    @Unique private double neuroLib$lastSampleDz;

    @Unique private final double[] neuroLib$stepSqrSamples = new double[NeuroLib_SAMPLE_SIZE];
    @Unique private int neuroLib$sampleIndex = 0;
    @Unique private int neuroLib$sampleCount = 0;
    @Unique private int neuroLib$warmupSamples = 0;

    @Unique private boolean neuroLib$wasRecordingLastTick = false;

    public LivingEntityMovementCacheMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Unique
    private boolean neuroLib$shouldRecordMovementNow() {
        return (Object) this instanceof IReasonTracker tracker
                && tracker.neuroLib$hasReason(NeuroLibReasons.RECORD_MOVEMENT);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void neuroLib$capturePrevPos(CallbackInfo ci) {
        if (level().isClientSide()) return;

        boolean shouldRecord = neuroLib$shouldRecordMovementNow();
        if (!shouldRecord) {
            if (neuroLib$wasRecordingLastTick) {
                neuroLib$clearSamples();
                neuroLib$wasRecordingLastTick = false;
            }
            return;
        }

        neuroLib$wasRecordingLastTick = true;

        // 故意只在偶數 tick 記錄起點，避免每 tick 取樣全接近 0
        if (this.tickCount % 2 != 0) return;

        neuroLib$lastX = getX();
        neuroLib$lastZ = getZ();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void neuroLib$storeStepSqr(CallbackInfo ci) {
        if (level().isClientSide()) return;
        if (!neuroLib$shouldRecordMovementNow()) return;

        double dx = getX() - neuroLib$lastX;
        double dz = getZ() - neuroLib$lastZ;
        double stepSqr = dx * dx + dz * dz;
        neuroLib$lastSampleDx = dx;
        neuroLib$lastSampleDz = dz;

        if (neuroLib$warmupSamples >= 2) neuroLib$pushSample(stepSqr);
        else neuroLib$warmupSamples++;

        if ((Object) this instanceof Player && NeuroLib.DEBUG_MODE) {
            NeuroLib.LOGGER.debug("[NeuroLib] ENTITY MOVEMENT = {}", stepSqr);
        }
    }

    @Unique
    private void neuroLib$pushSample(double value) {
        neuroLib$stepSqrSamples[neuroLib$sampleIndex] = value;
        neuroLib$sampleIndex = (neuroLib$sampleIndex + 1) % NeuroLib_SAMPLE_SIZE;
        if (neuroLib$sampleCount < NeuroLib_SAMPLE_SIZE) {
            neuroLib$sampleCount++;
        }
    }

    @Override
    @Unique
    public double neuroLib$getStepSqr() {
        if (neuroLib$sampleCount == 0) return 0.0D;
        int latestIndex = (neuroLib$sampleIndex - 1 + NeuroLib_SAMPLE_SIZE) % NeuroLib_SAMPLE_SIZE;
        return neuroLib$stepSqrSamples[latestIndex];
    }

    @Override
    @Unique
    public boolean neuroLib$hasAnyStepSqrAbove(double threshold) {
        for (int i = 0; i < neuroLib$sampleCount; i++) {
            if (neuroLib$stepSqrSamples[i] > threshold) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private void neuroLib$clearSamples() {
        Arrays.fill(neuroLib$stepSqrSamples, 0.0D);
        neuroLib$sampleIndex = 0;
        neuroLib$sampleCount = 0;
        neuroLib$warmupSamples = 0;
        neuroLib$lastSampleDx = 0.0D;
        neuroLib$lastSampleDz = 0.0D;
    }

    @Override
    @Unique
    public double neuroLib$getLastSampleDx() {
        return neuroLib$lastSampleDx;
    }

    @Override
    @Unique
    public double neuroLib$getLastSampleDz() {
        return neuroLib$lastSampleDz;
    }
}