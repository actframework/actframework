package act.metric;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.util.LogSupport;
import org.osgl.$;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.IO;
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
    private transient boolean dataSync = true;

    public SimpleMetricStore(SimpleMetricPlugin plugin) {
        this.plugin = $.requireNotNull(plugin);
        synchronizer = new FileSynchronizer();
        SimpleMetricStore persisted = synchronizer.read();
        if (null != persisted) {
            counters = persisted.counters;
            timers = persisted.timers;
        }
    }

    @Override
    public void countOnce(String name) {
        E.illegalArgumentIf(S.blank(name), "name expected");
        countOnce_(name);
    }

    private void countOnce_(String name) {
        AtomicLong al = counters.get(name);
        if (null == al) {
            AtomicLong newAl = new AtomicLong();
            al = counters.putIfAbsent(name, newAl);
            if (null == al) {
                al = newAl;
            }
        }
        al.incrementAndGet();
        name = getParent(name);
        if (S.notBlank(name)) {
            countOnce_(name);
        }
    }

    public void enableDataSync(boolean enabled) {
        dataSync = enabled;
    }

    @Override
    public void onTimerStart(String name) {
        E.illegalArgumentIf(S.blank(name), "name expected");
        countOnce_(name);
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
            AtomicLong newAl = new AtomicLong();
            al = timers.putIfAbsent(name, newAl);
            if (null == al) {
                al = newAl;
            }
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
        if (dataSync) {
            synchronizer.write(this);
        }
    }

    private Logger logger(String name) {
        Logger logger = plugin.logger(name);
        return null == logger ? defLogger : logger;
    }

    private String getParent(String name) {
        return S.beforeLast(name, ":");
    }

    private static class FileSynchronizer extends LogSupport {
        private static final String FILE_NAME = ".act.metric";
        private boolean ioError = false;

        void write(SimpleMetricStore store) {
            if (ioError) {
                return;
            }
            ObjectOutputStream oos = null;
            try {
                File file = new File(FILE_NAME);
                oos = new ObjectOutputStream(new FileOutputStream(file));
                oos.writeObject(store);
            } catch (IOException e) {
                ioError = true;
                throw E.ioException(e);
            } finally {
                IO.close(oos);
            }
        }

        SimpleMetricStore read() {
            File file = new File(FILE_NAME);
            if (file.exists() && file.canRead()) {
                ObjectInputStream ois = null;
                try {
                    ois = new ObjectInputStream(new FileInputStream(file));
                    SimpleMetricStore store = $.cast(ois.readObject());
                    return store;
                } catch (IOException e) {
                    ioError = true;
                    error(e, "Error reading simple metric store persisted file:%s. Will reset this file", file.getAbsolutePath());
                    if (!file.delete()) {
                        file.deleteOnExit();
                    }
                    return null;
                } catch (ClassNotFoundException e) {
                    throw E.unexpected(e);
                } finally {
                    IO.close(ois);
                }
            } else {
                return null;
            }
        }


    }

}
