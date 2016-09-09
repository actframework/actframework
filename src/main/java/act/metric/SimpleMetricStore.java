package act.metric;

import act.app.App;
import org.osgl.$;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.io.*;
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
public class SimpleMetricStore implements MetricStore, Serializable {


    private transient static final Logger defLogger = LogManager.get("metric.default");

    private static final long serialVersionUID = 7357409264403928225L;

    private ConcurrentMap<String, AtomicLong> counters = new ConcurrentHashMap<String, AtomicLong>();
    private ConcurrentMap<String, AtomicLong> timers = new ConcurrentHashMap<String, AtomicLong>();

    private transient SimpleMetricPlugin plugin;

    private transient FileSynchronizer synchronizer;

    public SimpleMetricStore(SimpleMetricPlugin plugin) {
        this.plugin = $.notNull(plugin);
        synchronizer = new FileSynchronizer();
        SimpleMetricStore persisted = synchronizer.read();
        if (null != persisted) {
            counters = persisted.counters;
            timers = persisted.timers;
        }
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
        logger(name).trace("Timer[%s] started", name);
    }

    @Override
    public void onTimerStop(Timer timer) {
        String name = timer.name();
        long ns = timer.ns();
        logger(name).trace("Timer[%s] stopped. Time elapsed: %sns", name, ns);
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
        Set<MetricInfo> set = C.newSet();
        for (Map.Entry<String, AtomicLong> entry : timers.entrySet()) {
            set.add(new MetricInfo(entry.getKey(), entry.getValue().get(), counters.get(entry.getKey()).get()));
        }
        return C.list(set);
    }

    @Override
    public void clear() {
        timers.clear();
        counters.clear();
    }

    public void takeSnapshot() {
        synchronizer.write(this);
    }

    private Logger logger(String name) {
        Logger logger = plugin.logger(name);
        return null == logger ? defLogger : logger;
    }

    private String getParent(String name) {
        return S.beforeLast(name, ":");
    }

    private static class FileSynchronizer {
        private static final String FILE_NAME = ".act.metric";
        private boolean ioError = false;

        void write(SimpleMetricStore store) {
            if (ioError) {
                return;
            }
            try {
                File file = new File(FILE_NAME);
                ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
                oos.writeObject(store);
            } catch (IOException e) {
                ioError = true;
                throw E.ioException(e);
            }
        }

        SimpleMetricStore read() {
            File file = new File(FILE_NAME);
            if (file.exists() && file.canRead()) {
                try {
                    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
                    SimpleMetricStore store = $.cast(ois.readObject());
                    return store;
                } catch (IOException e) {
                    ioError = true;
                    App.logger.error(e, "Error reading simple metric store persisted file:%s. Will reset this file", file.getAbsolutePath());
                    if (!file.delete()) {
                        file.deleteOnExit();
                    }
                    return null;
                } catch (ClassNotFoundException e) {
                    throw E.unexpected(e);
                }
            } else {
                return null;
            }
        }


    }

}
