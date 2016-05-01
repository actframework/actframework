package act.metric;

import org.osgl.$;

/**
 * A simple implementation of {@link Timer}
 */
public class SimpleTimer implements Timer {

    private String name;
    private MetricStore metricStore;
    private long start;
    private long duration;

    public SimpleTimer(String name, MetricStore metricStore) {
        this.name = $.notNull(name);
        this.metricStore = $.notNull(metricStore);
        this.start = $.ns();
        metricStore.countOnce(name);
        metricStore.onTimerStart(name);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void stop() {
        duration = $.ns() - start;
        metricStore.onTimerStop(this);
    }

    @Override
    public long ns() {
        return duration;
    }

}
