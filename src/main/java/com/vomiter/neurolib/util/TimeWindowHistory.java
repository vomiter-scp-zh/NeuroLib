package com.vomiter.neurolib.util;

import java.util.Arrays;

public final class TimeWindowHistory {
    private final long[] timestamps;
    private int nextIndex = 0;

    public TimeWindowHistory(int capacity) {
        this.timestamps = new long[Math.max(1, capacity)];
        Arrays.fill(this.timestamps, -1L);
    }

    public void add(long time) {
        timestamps[nextIndex] = time;
        nextIndex++;
        if (nextIndex >= timestamps.length) {
            nextIndex = 0;
        }
    }

    public int countRecent(long currentTime, long windowTicks) {
        int count = 0;
        for (long ts : timestamps) {
            if (ts >= 0L && currentTime - ts <= windowTicks) {
                count++;
            }
        }
        return count;
    }

    public long getLatest() {
        long latest = -1L;
        for (long ts : timestamps) {
            if (ts > latest) {
                latest = ts;
            }
        }
        return latest;
    }

    public long[] copyRaw() {
        return Arrays.copyOf(timestamps, timestamps.length);
    }

    public void loadRaw(long[] loaded) {
        Arrays.fill(this.timestamps, -1L);
        System.arraycopy(
                loaded,
                0,
                this.timestamps,
                0,
                Math.min(loaded.length, this.timestamps.length)
        );
    }

    public int getNextIndex() {
        return nextIndex;
    }

    public void setNextIndex(int nextIndex) {
        if (nextIndex < 0 || nextIndex >= timestamps.length) {
            this.nextIndex = 0;
        } else {
            this.nextIndex = nextIndex;
        }
    }

    public int capacity() {
        return timestamps.length;
    }
}