package com.vomiter.neurolib.mixin;

import com.vomiter.neurolib.common.entity.IReasonTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Mixin(LivingEntity.class)
public abstract class LivingEntityReasonTrackerMixin extends Entity implements IReasonTracker {

    @Unique
    private final Map<String, Map<UUID, Long>> neuroLib$reasons = new HashMap<>();

    public LivingEntityReasonTrackerMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    @Unique
    public void neuroLib$refreshReason(String reason, UUID source, long expireTick) {
        neuroLib$reasons
                .computeIfAbsent(reason, k -> new HashMap<>())
                .put(source, expireTick);
    }

    @Override
    @Unique
    public void neuroLib$removeReason(String reason, UUID source) {
        Map<UUID, Long> sources = neuroLib$reasons.get(reason);
        if (sources == null) return;

        sources.remove(source);
        if (sources.isEmpty()) {
            neuroLib$reasons.remove(reason);
        }
    }

    @Override
    @Unique
    public boolean neuroLib$hasReason(String reason) {
        Map<UUID, Long> sources = neuroLib$reasons.get(reason);
        if (sources == null || sources.isEmpty()) return false;

        neuroLib$purgeExpired(sources);
        if (sources.isEmpty()) {
            neuroLib$reasons.remove(reason);
            return false;
        }
        return true;
    }

    @Override
    @Unique
    public int neuroLib$getReasonSourceCount(String reason) {
        Map<UUID, Long> sources = neuroLib$reasons.get(reason);
        if (sources == null || sources.isEmpty()) return 0;

        neuroLib$purgeExpired(sources);
        if (sources.isEmpty()) {
            neuroLib$reasons.remove(reason);
            return 0;
        }
        return sources.size();
    }

    @Unique
    private void neuroLib$purgeExpired(Map<UUID, Long> sources) {
        long now = level().getGameTime();
        Iterator<Map.Entry<UUID, Long>> it = sources.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            if (entry.getValue() < now) {
                it.remove();
            }
        }
    }
}