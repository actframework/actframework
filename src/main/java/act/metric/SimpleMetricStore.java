package act.metric;

import org.osgl.$;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A simple implementation of {@link MetricStore}
 */
public class SimpleMetricStore implements MetricStore {

    private static final Logger defLogger = LogManager.get("metric.default");

    private ConcurrentMap<String, AtomicLong> counters = new ConcurrentHashMap<String, AtomicLong>();
    private ConcurrentMap<String, AtomicLong> timers = new ConcurrentHashMap<String, AtomicLong>();

    private SimpleMetricPlugin plugin;

    public SimpleMetricStore(SimpleMetricPlugin plugin) {
        this.plugin = $.notNull(plugin);
    }

    @Override
    public void countOnce(String name) {
        E.illegalArgumentIf(S.blank(name), "");
        countOnce_(name);
    }

    private void countOnce_(String name) {
        AtomicLong al = counters.get(name);
        if (null == al) {
            counters.putIfAbsent(name, new AtomicLong());
            al = counters.get(name);
        }
        al.incrementAndGet();
        name = getParent(name);
        if (S.notBlank(name)) {
            countOnce_(name);
        }
    }

    @Override
    public void onTimerStart(String name) {
        logger(name).trace("Timer started");
    }

    @Override
    public void onTimerStop(Timer timer) {
        String name = timer.name();
        long ns = timer.ns();
        logger(name).trace("Timer stopped. Time elapsed: %sns", ns);
        onTimerStop_(name, ns);
    }

    private void onTimerStop_(String name, long ns) {
        AtomicLong al = timers.get(name);
        if (null == al) {
            timers.putIfAbsent(name, new AtomicLong());
            al = timers.get(name);
        }
        al.addAndGet(ns);
        name = getParent(name);
        if (S.notBlank(name)) {
            onTimerStop_(name, ns);
        }
    }

    @Override
    public Long count(String name) {
        AtomicLong al = counters.get(name);
        return null == al ? null : al.get();
    }

    @Override
    public Long ns(String name) {
        AtomicLong al = counters.get(name);
        return null == al ? null : al.get();
    }

    @Override
    public List<MetricInfo> counters() {
        Set<MetricInfo> set = new TreeSet<MetricInfo>();
        for (Map.Entry<String, AtomicLong> entry : counters.entrySet()) {
            set.add(new MetricInfo(entry.getKey(), entry.getValue().get()));
        }
        return C.list(set);
    }

    @Override
    public List<MetricInfo> timers() {
        Set<MetricInfo> set = new TreeSet<MetricInfo>();
        for (Map.Entry<String, AtomicLong> entry : timers.entrySet()) {
            set.add(new MetricInfo(entry.getKey(), entry.getValue().get(), counters.get(entry.getKey()).get()));
        }
        return C.list(set);
    }

    private Logger logger(String name) {
        Logger logger = plugin.logger(name);
        return null == logger ? defLogger : logger;
    }

    private String getParent(String name) {
        return S.beforeLast(name, ":");
    }

}
