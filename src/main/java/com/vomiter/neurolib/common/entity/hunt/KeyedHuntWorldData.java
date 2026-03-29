package com.vomiter.neurolib.common.entity.hunt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vomiter.neurolib.util.TimeWindowHistory;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class KeyedHuntWorldData extends SavedData {
    private static final int DEFAULT_CAPACITY = 1000;

    private static final Codec<TimeWindowHistory> HISTORY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("capacity").forGetter(TimeWindowHistory::capacity),
            Codec.LONG.listOf().fieldOf("timestamps").forGetter(history -> {
                long[] raw = history.copyRaw();
                java.util.List<Long> list = new java.util.ArrayList<>(raw.length);
                for (long l : raw) list.add(l);
                return list;
            }),
            Codec.INT.fieldOf("nextIndex").forGetter(TimeWindowHistory::getNextIndex)
    ).apply(instance, (capacity, timestamps, nextIndex) -> {
        TimeWindowHistory history = new TimeWindowHistory(capacity);
        long[] raw = new long[timestamps.size()];
        for (int i = 0; i < timestamps.size(); i++) {
            raw[i] = timestamps.get(i);
        }
        history.loadRaw(raw);
        history.setNextIndex(nextIndex);
        return history;
    }));

    private static final Codec<KeyedHuntWorldData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, HISTORY_CODEC)
                    .optionalFieldOf("histories", Map.of())
                    .forGetter(data -> data.histories)
    ).apply(instance, KeyedHuntWorldData::new));

    public static final SavedDataType<@NotNull KeyedHuntWorldData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("neurolib", "neurolib_keyed_hunt_data"),
            KeyedHuntWorldData::new,
            CODEC
    );

    private final Map<String, TimeWindowHistory> histories;

    public KeyedHuntWorldData() {
        this(new HashMap<>());
    }

    private KeyedHuntWorldData(Map<String, TimeWindowHistory> histories) {
        this.histories = new HashMap<>(histories);
    }

    private TimeWindowHistory getOrCreate(String key, int capacity) {
        return histories.computeIfAbsent(key, k -> new TimeWindowHistory(Math.max(1, capacity)));
    }

    public void addTimestamp(String key, int capacity, long time) {
        getOrCreate(key, capacity).add(time);
        setDirty();
    }

    public int countRecent(String key, int capacity, long currentTime, long windowTicks) {
        return getOrCreate(key, capacity).countRecent(currentTime, windowTicks);
    }

    public long getLatest(String key, int capacity) {
        return getOrCreate(key, capacity).getLatest();
    }

    public static KeyedHuntWorldData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
}