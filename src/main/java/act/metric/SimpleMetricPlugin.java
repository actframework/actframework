package act.metric;

import act.Act;
import act.app.App;
import act.plugin.AppServicePlugin;
import org.osgl.$;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.C;

import java.util.Map;

/**
 * A simple implementation of {@link MetricPlugin}
 */
public class SimpleMetricPlugin implements MetricPlugin {

    private Map<String, Logger> enabledMap = C.newMap();
    private SimpleMetricStore defaultMetricStore = new SimpleMetricStore(this);
    private Metric defaultMetric = new SimpleMetric(defaultMetricStore);

    public SimpleMetricPlugin() {
    }

    @Override
    public Metric metric(String name) {
        if (!Act.appConfig().metricEnabled()) {
            return Metric.NULL_METRIC;
        }
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

    @Override
    public void enableDataSync(boolean sync) {
        defaultMetricStore.enableDataSync(sync);
    }

    Logger logger(String name) {
        return enabledMap.get(name);
    }

    public static class SimpleMetricPersistService extends AppServicePlugin {
        @Override
        protected void applyTo(App app) {
            if (!app.config().metricEnabled()) {
                return;
            }
            MetricPlugin plugin = Act.metricPlugin();
            if (plugin instanceof SimpleMetricPlugin) {
                SimpleMetricPlugin smp = (SimpleMetricPlugin) plugin;
                final SimpleMetricStore store = $.cast(smp.defaultMetricStore);
                final Runnable takeSnapshot = new Runnable() {
                    @Override
                    public void run() {
                        store.takeSnapshot();
                    }
                };
                app.jobManager().every("metric:snapshot", takeSnapshot, "1mn");
            }
        }
    }
}
