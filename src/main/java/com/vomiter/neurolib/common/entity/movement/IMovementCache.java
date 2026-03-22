package com.vomiter.neurolib.common.entity.movement;

public interface IMovementCache {
    double neuroLib$getStepSqr();
    boolean neuroLib$hasAnyStepSqrAbove(double threshold);
    double neuroLib$getLastSampleDx();
    double neuroLib$getLastSampleDz();
}