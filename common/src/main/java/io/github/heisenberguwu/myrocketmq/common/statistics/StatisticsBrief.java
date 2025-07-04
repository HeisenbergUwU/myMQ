package io.github.heisenberguwu.myrocketmq.common.statistics;

import org.apache.commons.lang3.ArrayUtils;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 这个 StatisticsBrief 类是一个用于统计数据的类，它的功能主要包括对一系列数值进行统计并提供一些关键指标（如最大值、最小值、总数、平均值以及百分位数等）。它采用了线程安全的设计，适用于多线程环境下的统计数据收集。
 */
public class StatisticsBrief {
    public static final int META_RANGE_INDEX = 0;
    public static final int META_SLOT_NUM_INDEX = 1;

    // TopPercentile 百分数
    private long[][] topPercentileMeta;
    private AtomicInteger[] counts;
    private AtomicLong totalCount;

    // max min avg total
    private long max;
    private long min;
    private long total;

    public StatisticsBrief(long[][] topPercentileMeta) {
        if (!isLegalMeta(topPercentileMeta)) {
            throw new IllegalArgumentException("illegal topPercentileMeta");
        }

        this.topPercentileMeta = topPercentileMeta;
        this.counts = new AtomicInteger[slotNum(topPercentileMeta)];
        this.totalCount = new AtomicLong(0);
        reset();
    }

    public void reset() {
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] == null) {
                counts[i] = new AtomicInteger(0);
            } else {
                counts[i].set(0);
            }
        }
        totalCount.set(0);

        synchronized (this) {
            max = 0;
            min = Long.MAX_VALUE;
            total = 0;
        }
    }

    private static boolean isLegalMeta(long[][] meta) {
        if (ArrayUtils.isEmpty(meta)) {
            return false;
        }

        for (long[] line : meta) {
            if (ArrayUtils.isEmpty(line) || line.length != 2) {
                return false;
            }
        }

        return true;
    }

    private static int slotNum(long[][] meta) {
        int ret = 1;
        for (long[] line : meta) {
            ret += line[META_SLOT_NUM_INDEX];
        }
        return ret;
    }


    public void sample(long value) {
        int index = getSlotIndex(value);
        counts[index].incrementAndGet();
        totalCount.incrementAndGet();

        synchronized (this) {
            max = Math.max(max, value);
            min = Math.min(min, value);
            total += value;
        }
    }

    public long tp999() {
        return getTPValue(0.999f);
    }


    public long getTPValue(float ratio) {
        if (ratio <= 0 || ratio >= 1) {
            ratio = 0.99f;
        }
        long count = totalCount.get();
        long excludes = (long) (count - count * ratio);
        if (excludes == 0) {
            return getMax();
        }

        int tmp = 0;
        for (int i = counts.length - 1; i > 0; i--) {
            tmp += counts[i].get();
            if (tmp > excludes) {
                return Math.min(getSlotTPValue(i), getMax());
            }
        }
        return 0;
    }


    private long getSlotTPValue(int index) {
        int slotNumLeft = index;
        for (int i = 0; i < topPercentileMeta.length; i++) {
            int slotNum = (int) topPercentileMeta[i][META_SLOT_NUM_INDEX];
            if (slotNumLeft < slotNum) {
                long metaRangeMax = topPercentileMeta[i][META_RANGE_INDEX];
                long metaRangeMin = 0;
                if (i > 0) {
                    metaRangeMin = topPercentileMeta[i - 1][META_RANGE_INDEX];
                }

                return metaRangeMin + (metaRangeMax - metaRangeMin) / slotNum * (slotNumLeft + 1);
            } else {
                slotNumLeft -= slotNum;
            }
        }
        // MAX_VALUE: the last slot
        return Integer.MAX_VALUE;
    }

    private int getSlotIndex(long num) {
        int index = 0;
        for (int i = 0; i < topPercentileMeta.length; i++) {
            long rangeMax = topPercentileMeta[i][META_RANGE_INDEX];
            int slotNum = (int) topPercentileMeta[i][META_SLOT_NUM_INDEX];
            long rangeMin = (i > 0) ? topPercentileMeta[i - 1][META_RANGE_INDEX] : 0;
            if (rangeMin <= num && num < rangeMax) {
                index += (num - rangeMin) / ((rangeMax - rangeMin) / slotNum);
                break;
            }

            index += slotNum;
        }
        return index;
    }

    /**
     * Getters
     *
     * @return
     */
    public long getMax() {
        return max;
    }

    public long getMin() {
        return totalCount.get() > 0 ? min : 0;
    }

    public long getTotal() {
        return total;
    }

    public long getCnt() {
        return totalCount.get();
    }

    public double getAvg() {
        return totalCount.get() != 0 ? ((double) total) / totalCount.get() : 0;
    }
}