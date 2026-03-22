package com.vomiter.neurolib.common.entity.hunt;

import com.vomiter.neurolib.util.TimeWindowHistory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class KeyedHuntWorldData extends SavedData {
    private static final String NAME = "neurolib_keyed_hunt_data";
    private static final int DEFAULT_CAPACITY = 1000;

    private final Map<String, TimeWindowHistory> histories = new HashMap<>();

    public KeyedHuntWorldData() {
    }

    public static KeyedHuntWorldData load(CompoundTag tag) {
        KeyedHuntWorldData data = new KeyedHuntWorldData();

        CompoundTag historiesTag = tag.getCompound("histories");
        for (String key : historiesTag.getAllKeys()) {
            CompoundTag entry = historiesTag.getCompound(key);

            TimeWindowHistory history = new TimeWindowHistory(entry.getInt("capacity"));
            history.loadRaw(entry.getLongArray("timestamps"));
            history.setNextIndex(entry.getInt("nextIndex"));

            data.histories.put(key, history);
        }

        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        CompoundTag historiesTag = new CompoundTag();

        for (Map.Entry<String, TimeWindowHistory> entry : histories.entrySet()) {
            String key = entry.getKey();
            TimeWindowHistory history = entry.getValue();

            CompoundTag historyTag = new CompoundTag();
            historyTag.putInt("capacity", history.capacity());
            historyTag.putLongArray("timestamps", history.copyRaw());
            historyTag.putInt("nextIndex", history.getNextIndex());

            historiesTag.put(key, historyTag);
        }

        tag.put("histories", historiesTag);
        return tag;
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
        return level.getDataStorage().computeIfAbsent(
                KeyedHuntWorldData::load,
                KeyedHuntWorldData::new,
                NAME
        );
    }
}