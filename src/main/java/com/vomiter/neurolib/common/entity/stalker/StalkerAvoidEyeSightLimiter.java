package com.vomiter.neurolib.common.entity.stalker;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class StalkerAvoidEyeSightLimiter {

    private static final Set<Entry> ACTIVE = new HashSet<>();

    private StalkerAvoidEyeSightLimiter() {}

    public static boolean canStart(Mob mob) {
        if (mob.level().isClientSide()) return true;

        cleanupDead(mob.level());

        int limit = getLimit(mob.level());
        return ACTIVE.size() < limit;
    }

    public static boolean isActive(Mob mob) {
        if (mob.level().isClientSide()) return true;
        return ACTIVE.contains(new Entry(mob.level(), mob.getUUID()));
    }

    public static boolean tryRegister(Mob mob) {
        if (mob.level().isClientSide()) return true;

        cleanupDead(mob.level());

        Entry self = new Entry(mob.level(), mob.getUUID());
        if (ACTIVE.contains(self)) {
            return true;
        }

        int limit = getLimit(mob.level());
        if (ACTIVE.size() >= limit) {
            return false;
        }

        ACTIVE.add(self);
        return true;
    }

    public static void unregister(Mob mob) {
        if (mob.level().isClientSide()) return;
        ACTIVE.remove(new Entry(mob.level(), mob.getUUID()));
    }

    public static int getActiveCount(Level level) {
        cleanupDead(level);
        return ACTIVE.size();
    }

    public static int getLimit(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            int playerCount = serverLevel.players().size();

            // 固定 10
            // return 10;

            // 10 + (playerCount - 1) * 5，但最低仍為 10
            return 10 + Math.max(0, playerCount - 1) * 5;
        }
        return 10;
    }

    private static void cleanupDead(Level level) {
        ACTIVE.removeIf(entry -> {
            if (entry.level() != level) {
                return false;
            }

            if (!(level instanceof ServerLevel serverLevel)) {
                return false;
            }

            var entity = serverLevel.getEntity(entry.mobUuid());
            return !(entity instanceof Mob mob) || !mob.isAlive();
        });
    }

    private record Entry(Level level, UUID mobUuid) {}
}