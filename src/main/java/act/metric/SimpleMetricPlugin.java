package act.metric;

import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.C;

import java.util.Map;

/**
 * A simple implementation of {@link MetricPlugin}
 */
public class SimpleMetricPlugin implements MetricPlugin {

    private Map<String, Logger> enabledMap = C.newMap();
    private MetricStore defaultMetricStore = new SimpleMetricStore(this);
    private Metric defaultMetric = new SimpleMetric(defaultMetricStore);

    @Override
    public Metric metric(String name) {
        Logger logger = enabledMap.get(name);
        if (null == logger) {
            logger = LogManager.get("metric." + name);
            enabledMap.put(name, logger);
        }
        return logger.isTraceEnabled() ? metric() : Metric.NULL_METRIC;
    }

    @Override
    public Metric metric() {
        return defaultMetric;
    }

    @Override
    public MetricStore metricStore() {
        return defaultMetricStore;
    }

    Logger logger(String name) {
        return enabledMap.get(name);
    }
}
