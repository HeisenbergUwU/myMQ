package io.github.heisenberguwu.myrocketmq.common.statistics;

import io.github.heisenberguwu.myrocketmq.common.utils.ThreadUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StatisticsManager {

    /**
     * Set of Statistics Kind Metadata
     */
    private Map<String, StatisticsKindMeta> kindMetaMap;

    /**
     * item names to calculate statistics brief
     */
    private Pair<String, long[][]>[] briefMetas;

    /**
     * Statistics
     */
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, StatisticsItem>> statsTable
            = new ConcurrentHashMap<>();

    private static final int MAX_IDLE_TIME = 10 * 60 * 1000;
    private final ScheduledExecutorService executor = ThreadUtils.newSingleThreadScheduledExecutor(
            "StatisticsManagerCleaner", true);

    private StatisticsItemStateGetter statisticsItemStateGetter;

    public StatisticsManager() {
        kindMetaMap = new HashMap<>();
        start();
    }

    public StatisticsManager(Map<String, StatisticsKindMeta> kindMeta) {
        this.kindMetaMap = kindMeta;
        start();
    }

    public void addStatisticsKindMeta(StatisticsKindMeta kindMeta) {
        kindMetaMap.put(kindMeta.getName(), kindMeta);
        statsTable.putIfAbsent(kindMeta.getName(), new ConcurrentHashMap<>(16));
    }

    public void setBriefMeta(Pair<String, long[][]>[] briefMetas) {
        this.briefMetas = briefMetas;
    }

    private void start() {
        int maxIdleTime = MAX_IDLE_TIME;
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Iterator<Map.Entry<String, ConcurrentHashMap<String, StatisticsItem>>> iter
                        = statsTable.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<String, ConcurrentHashMap<String, StatisticsItem>> entry = iter.next();
                    String kind = entry.getKey();
                    ConcurrentHashMap<String, StatisticsItem> itemMap = entry.getValue();

                    if (itemMap == null || itemMap.isEmpty()) {
                        continue;
                    }

                    HashMap<String, StatisticsItem> tmpItemMap = new HashMap<>(itemMap);
                    for (StatisticsItem item : tmpItemMap.values()) {
                        // remove when expired
                        if (System.currentTimeMillis() - item.getLastTimeStamp().get() > MAX_IDLE_TIME
                                && (statisticsItemStateGetter == null || !statisticsItemStateGetter.online(item))) {
                            remove(item);
                        }
                    }
                }
            }
        }, maxIdleTime, maxIdleTime / 3, TimeUnit.MILLISECONDS);
    }

    /**
     * Increment a StatisticsItem
     *
     * @param kind
     * @param key
     * @param itemAccumulates
     */
    public boolean inc(String kind, String key, long... itemAccumulates) {
        ConcurrentHashMap<String, StatisticsItem> itemMap = statsTable.get(kind);
        if (itemMap != null) {
            StatisticsItem item = itemMap.get(key);

            // if not exist, create and schedule
            if (item == null) {
                item = new StatisticsItem(kind, key, kindMetaMap.get(kind).getItemNames());
                item.setInterceptor(new StatisticsBriefInterceptor(item, this.briefMetas));
                StatisticsItem oldItem = itemMap.putIfAbsent(key, item);
                if (oldItem != null) {
                    item = oldItem;
                } else {
                    scheduleStatisticsItem(item);
                }
            }

            // do increment
            item.incItems(itemAccumulates);

            return true;
        }

        return false;
    }

    private void scheduleStatisticsItem(StatisticsItem item) {
        kindMetaMap.get(item.getStatKind()).getScheduledPrinter().schedule(item);
    }

    public void remove(StatisticsItem item) {
        ConcurrentHashMap<String, StatisticsItem> itemMap = statsTable.get(item.getStatKind());
        if (itemMap != null) {
            itemMap.remove(item.getStatObject(), item);
        }

        StatisticsKindMeta kindMeta = kindMetaMap.get(item.getStatKind());
        if (kindMeta != null && kindMeta.getScheduledPrinter() != null) {
            kindMeta.getScheduledPrinter().remove(item);
        }
    }

    public StatisticsItemStateGetter getStatisticsItemStateGetter() {
        return statisticsItemStateGetter;
    }

    public void setStatisticsItemStateGetter(StatisticsItemStateGetter statisticsItemStateGetter) {
        this.statisticsItemStateGetter = statisticsItemStateGetter;
    }
}
