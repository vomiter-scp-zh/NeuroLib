package com.vomiter.neurolib.common.entity.loot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class LootJsonScanner {
    private LootJsonScanner() {}

    public static boolean lootTableContainsAnyTarget(
            ResourceManager resourceManager,
            Identifier lootTableId,
            Set<String> targetItemIds,
            String targetTagId
    ) {
        Map<Identifier, JsonObject> jsonCache = new HashMap<>();
        return dfs(resourceManager, lootTableId, targetItemIds, targetTagId, jsonCache, new HashSet<>());
    }

    public static boolean lootTableContainsAnyTarget(
            ResourceManager resourceManager,
            Identifier lootTableId,
            LootMatchSpec spec
    ) {
        return lootTableContainsAnyTarget(resourceManager, lootTableId, spec.itemIds(), spec.tagId());
    }

    private static boolean dfs(
            ResourceManager resourceManager,
            Identifier lootTableId,
            Set<String> targetItemIds,
            String targetTagId,
            Map<Identifier, JsonObject> jsonCache,
            Set<Identifier> visiting
    ) {
        if (!visiting.add(lootTableId)) {
            return false;
        }

        try {
            JsonObject json = jsonCache.computeIfAbsent(lootTableId, id -> readLootTableJson(resourceManager, id));
            if (json == null) {
                return false;
            }

            if (jsonContainsNameOrTag(json, targetItemIds, targetTagId)) {
                return true;
            }

            for (Identifier ref : findLootTableReferences(json)) {
                if (ref != null && dfs(resourceManager, ref, targetItemIds, targetTagId, jsonCache, visiting)) {
                    return true;
                }
            }

            return false;
        } finally {
            visiting.remove(lootTableId);
        }
    }

    private static JsonObject readLootTableJson(ResourceManager resourceManager, Identifier lootTableId) {
        Identifier fileId = Identifier.fromNamespaceAndPath(
                lootTableId.getNamespace(),
                "loot_table/" + lootTableId.getPath() + ".json"
        );

        try (var res = resourceManager.getResourceOrThrow(fileId).open();
             var reader = new InputStreamReader(res, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean jsonContainsNameOrTag(
            JsonElement element,
            Set<String> targetItemIds,
            String targetTagId
    ) {
        if (element == null) {
            return false;
        }

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();

            if (obj.has("name") && obj.get("name").isJsonPrimitive()) {
                String name = obj.get("name").getAsString();
                if (targetItemIds.contains(name)) {
                    return true;
                }
            }

            if (obj.has("tag") && obj.get("tag").isJsonPrimitive()) {
                String tag = obj.get("tag").getAsString();
                if (targetTagId.equals(tag)) {
                    return true;
                }
            }

            for (var entry : obj.entrySet()) {
                if (jsonContainsNameOrTag(entry.getValue(), targetItemIds, targetTagId)) {
                    return true;
                }
            }
        } else if (element.isJsonArray()) {
            for (var child : element.getAsJsonArray()) {
                if (jsonContainsNameOrTag(child, targetItemIds, targetTagId)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static List<Identifier> findLootTableReferences(JsonObject root) {
        ArrayList<Identifier> out = new ArrayList<>();
        collectRefs(root, out);
        return out;
    }

    private static void collectRefs(JsonElement element, List<Identifier> out) {
        if (element == null) {
            return;
        }

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();

            if (obj.has("type") && obj.has("name")
                    && obj.get("type").isJsonPrimitive()
                    && obj.get("name").isJsonPrimitive()) {
                String type = obj.get("type").getAsString();
                String name = obj.get("name").getAsString();
                if ("minecraft:loot_table".equals(type)) {
                    Identifier rl = Identifier.tryParse(name);
                    if (rl != null) {
                        out.add(rl);
                    }
                }
            }

            for (var entry : obj.entrySet()) {
                collectRefs(entry.getValue(), out);
            }
        } else if (element.isJsonArray()) {
            for (var child : element.getAsJsonArray()) {
                collectRefs(child, out);
            }
        }
    }
}