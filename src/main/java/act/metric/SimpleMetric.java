package act.metric;

import org.osgl.$;

/**
 * A simple implementation of {@link Metric}
 */
public class SimpleMetric implements Metric {
    private MetricStore metricStore;

    public SimpleMetric(MetricStore metricStore) {
        this.metricStore = $.notNull(metricStore);
    }

    @Override
    public Timer startTimer(String name) {
        return new SimpleTimer(name, metricStore);
    }

    @Override
    public void countOnce(String name) {
        metricStore.countOnce(name);
    }

}
