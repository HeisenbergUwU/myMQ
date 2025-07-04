package io.github.heisenberguwu.myrocketmq.common.stats;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.github.heisenberguwu.myrocketmq.common.UtilAll;
import io.github.heisenberguwu.myrocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;

public class StatsItemSet {
    private static final Logger COMMERCIAL_LOG = LoggerFactory.getLogger(LoggerName.COMMERCIAL_LOGGER_NAME);
    private final ConcurrentMap<String/* key */, StatsItem> statsItemTable =
        new ConcurrentHashMap<>(128);

    private final String statsName;
    private final ScheduledExecutorService scheduledExecutorService;

    private final Logger logger;

    public StatsItemSet(String statsName, ScheduledExecutorService scheduledExecutorService, Logger logger) {
        this.logger = logger;
        this.statsName = statsName;
        this.scheduledExecutorService = scheduledExecutorService;
        this.init();
    }

    public void init() {

        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    samplingInSeconds();
                } catch (Throwable ignored) {
                }
            }
        }, 0, 10, TimeUnit.SECONDS);

        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    samplingInMinutes();
                } catch (Throwable ignored) {
                }
            }
        }, 0, 10, TimeUnit.MINUTES);

        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    samplingInHour();
                } catch (Throwable ignored) {
                }
            }
        }, 0, 1, TimeUnit.HOURS);

        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    printAtMinutes();
                } catch (Throwable ignored) {
                }
            }
        }, Math.abs(UtilAll.computeNextMinutesTimeMillis() - System.currentTimeMillis()), 1000 * 60, TimeUnit.MILLISECONDS);

        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    printAtHour();
                } catch (Throwable ignored) {
                }
            }
        }, Math.abs(UtilAll.computeNextHourTimeMillis() - System.currentTimeMillis()), 1000 * 60 * 60, TimeUnit.MILLISECONDS);

        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    printAtDay();
                } catch (Throwable ignored) {
                }
            }
        }, Math.abs(UtilAll.computeNextMorningTimeMillis() - System.currentTimeMillis()), 1000 * 60 * 60 * 24, TimeUnit.MILLISECONDS);
    }

    private void samplingInSeconds() {
        Iterator<Entry<String, StatsItem>> it = this.statsItemTable.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, StatsItem> next = it.next();
            next.getValue().samplingInSeconds();
        }
    }

    private void samplingInMinutes() {
        Iterator<Entry<String, StatsItem>> it = this.statsItemTable.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, StatsItem> next = it.next();
            next.getValue().samplingInMinutes();
        }
    }

    private void samplingInHour() {
        Iterator<Entry<String, StatsItem>> it = this.statsItemTable.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, StatsItem> next = it.next();
            next.getValue().samplingInHour();
        }
    }

    private void printAtMinutes() {
        Iterator<Entry<String, StatsItem>> it = this.statsItemTable.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, StatsItem> next = it.next();
            next.getValue().printAtMinutes();
        }
    }

    private void printAtHour() {
        Iterator<Entry<String, StatsItem>> it = this.statsItemTable.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, StatsItem> next = it.next();
            next.getValue().printAtHour();
        }
    }

    private void printAtDay() {
        Iterator<Entry<String, StatsItem>> it = this.statsItemTable.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, StatsItem> next = it.next();
            next.getValue().printAtDay();
        }
    }

    public void addValue(final String statsKey, final int incValue, final int incTimes) {
        StatsItem statsItem = this.getAndCreateStatsItem(statsKey);
        statsItem.getValue().add(incValue);
        statsItem.getTimes().add(incTimes);
        statsItem.setLastUpdateTimestamp(System.currentTimeMillis());
    }

    public void addRTValue(final String statsKey, final int incValue, final int incTimes) {
        StatsItem statsItem = this.getAndCreateRTStatsItem(statsKey);
        statsItem.getValue().add(incValue);
        statsItem.getTimes().add(incTimes);
        statsItem.setLastUpdateTimestamp(System.currentTimeMillis());
    }

    public void delValue(final String statsKey) {
        StatsItem statsItem = this.statsItemTable.get(statsKey);
        if (null != statsItem) {
            this.statsItemTable.remove(statsKey);
        }
    }

    public void delValueByPrefixKey(final String statsKey, String separator) {
        Iterator<Entry<String, StatsItem>> it = this.statsItemTable.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, StatsItem> next = it.next();
            if (next.getKey().startsWith(statsKey + separator)) {
                it.remove();
            }
        }
    }

    public void delValueByInfixKey(final String statsKey, String separator) {
        Iterator<Entry<String, StatsItem>> it = this.statsItemTable.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, StatsItem> next = it.next();
            if (next.getKey().contains(separator + statsKey + separator)) {
                it.remove();
            }
        }
    }

    public void delValueBySuffixKey(final String statsKey, String separator) {
        Iterator<Entry<String, StatsItem>> it = this.statsItemTable.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, StatsItem> next = it.next();
            if (next.getKey().endsWith(separator + statsKey)) {
                it.remove();
            }
        }
    }

    public StatsItem getAndCreateStatsItem(final String statsKey) {
        return getAndCreateItem(statsKey, false);
    }

    public StatsItem getAndCreateRTStatsItem(final String statsKey) {
        return getAndCreateItem(statsKey, true);
    }

    public StatsItem getAndCreateItem(final String statsKey, boolean rtItem) {
        StatsItem statsItem = this.statsItemTable.get(statsKey);
        if (null == statsItem) {
            if (rtItem) {
                statsItem = new RTStatsItem(this.statsName, statsKey, this.scheduledExecutorService, logger);
            } else {
                statsItem = new StatsItem(this.statsName, statsKey, this.scheduledExecutorService, logger);
            }
            StatsItem prev = this.statsItemTable.putIfAbsent(statsKey, statsItem);

            if (null != prev) {
                statsItem = prev;
                // statsItem.init();
            }
        }

        return statsItem;
    }

    public StatsSnapshot getStatsDataInMinute(final String statsKey) {
        StatsItem statsItem = this.statsItemTable.get(statsKey);
        if (null != statsItem) {
            return statsItem.getStatsDataInMinute();
        }
        return new StatsSnapshot();
    }

    public StatsSnapshot getStatsDataInHour(final String statsKey) {
        StatsItem statsItem = this.statsItemTable.get(statsKey);
        if (null != statsItem) {
            return statsItem.getStatsDataInHour();
        }
        return new StatsSnapshot();
    }

    public StatsSnapshot getStatsDataInDay(final String statsKey) {
        StatsItem statsItem = this.statsItemTable.get(statsKey);
        if (null != statsItem) {
            return statsItem.getStatsDataInDay();
        }
        return new StatsSnapshot();
    }

    public StatsItem getStatsItem(final String statsKey) {
        return this.statsItemTable.get(statsKey);
    }


    public void cleanResource(int maxStatsIdleTimeInMinutes) {
        COMMERCIAL_LOG.info("CleanStatisticItemOld: kind:{}, size:{}", statsName, this.statsItemTable.size());
        Iterator<Entry<String, StatsItem>> it = this.statsItemTable.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, StatsItem> next = it.next();
            StatsItem statsItem = next.getValue();
            if (System.currentTimeMillis() - statsItem.getLastUpdateTimestamp() > maxStatsIdleTimeInMinutes * 60 * 1000L) {
                it.remove();
                COMMERCIAL_LOG.info("CleanStatisticItemOld: removeKind:{}, removeKey:{}", statsName, statsItem.getStatsKey());
            }
        }
    }
}
