package com.vomiter.neurolib.common.entity;

import java.util.UUID;

public interface IReasonTracker {
    void neuroLib$refreshReason(String reason, UUID source, long expireTick);
    void neuroLib$removeReason(String reason, UUID source);
    boolean neuroLib$hasReason(String reason);
    int neuroLib$getReasonSourceCount(String reason);
}