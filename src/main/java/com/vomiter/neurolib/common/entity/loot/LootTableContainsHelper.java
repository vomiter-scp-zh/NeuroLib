package com.vomiter.neurolib.common.entity.loot;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class LootTableContainsHelper {
    private LootTableContainsHelper() {
    }

    private static final ConcurrentHashMap<CacheKey, Boolean> CACHE = new ConcurrentHashMap<>();

    public static boolean entityDefaultLootTableContains(
            MinecraftServer server,
            EntityType<?> entityType,
            Supplier<LootMatchSpec> specSupplier
    ) {
        if (server == null || entityType == null) return false;

        ResourceLocation lootTableId = entityType.getDefaultLootTable().location();
        return lootTableContains(server, lootTableId, specSupplier);
    }

    public static boolean lootTableContains(
            MinecraftServer server,
            ResourceLocation lootTableId,
            Supplier<LootMatchSpec> specSupplier
    ) {
        if (server == null || lootTableId == null || specSupplier == null) return false;
        if (BuiltInLootTables.EMPTY.equals(lootTableId)) return false;

        LootMatchSpec spec = specSupplier.get();
        if (spec == null) return false;

        CacheKey key = new CacheKey(
                lootTableId,
                spec.tagId(),
                spec.itemIds().hashCode()
        );

        return CACHE.computeIfAbsent(key, __ -> compute(server.getResourceManager(), lootTableId, spec));
    }

    private static boolean compute(
            ResourceManager resourceManager,
            ResourceLocation lootTableId,
            LootMatchSpec spec
    ) {
        return LootJsonScanner.lootTableContainsAnyTarget(
                resourceManager,
                lootTableId,
                spec.itemIds(),
                spec.tagId()
        );
    }

    public static void clear() {
        CACHE.clear();
    }

    private record CacheKey(
            ResourceLocation lootTableId,
            String tagId,
            int itemIdsHash
    ) {
        private CacheKey {
            Objects.requireNonNull(lootTableId);
            Objects.requireNonNull(tagId);
        }
    }
}