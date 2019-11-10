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

import act.Act;
import act.app.App;
import act.plugin.AppServicePlugin;
import org.osgl.$;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.S;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple implementation of {@link MetricPlugin}
 */
public class SimpleMetricPlugin implements MetricPlugin {

    private Map<String, Logger> enabledMap = new HashMap<>();
    private SimpleMetricStore defaultMetricStore = new SimpleMetricStore(this);
    private Metric defaultMetric = new SimpleMetric("act", defaultMetricStore);

    public SimpleMetricPlugin() {
    }

    @Override
    public Metric metric(String name) {
        if (!Act.appConfig().metricEnabled()) {
            return Metric.NULL_METRIC;
        }
        Logger logger = enabledMap.get(name);
        if (null == logger) {
            logger = LogManager.get("act.metric." + name);
            enabledMap.put(name, logger);
        }
        return logger.isTraceEnabled() ? S.blank(name) ? defaultMetric : new SimpleMetric(name, defaultMetricStore) : Metric.NULL_METRIC;
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
