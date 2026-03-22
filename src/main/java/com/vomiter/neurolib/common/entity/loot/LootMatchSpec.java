package com.vomiter.neurolib.common.entity.loot;

import java.util.Set;

public record LootMatchSpec(Set<String> itemIds, String tagId) {
}