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

import org.osgl.logging.Logger;

/**
 * Defines a Metric plugin
 */
public interface MetricPlugin {
    /**
     * Returns a {@link Metric} instance by name
     *
     * The plugin shall check if a {@link org.osgl.logging.Logger} get by name `metric.$name`
     * is {@link Logger#isTraceEnabled() trace enabled} to return the real Metric instance, otherwise
     * then it shall return the {@link Metric#NULL_METRIC the do-nothing metric instance}
     *
     * @param name the name (could be the name of the metric root hierarchy)
     * @return the metric instance corresponding to the name
     */
    Metric metric(String name);

    /**
     * Returns the default {@link Metric} instance
     * <p>
     *     Note the plugin shall always return the same instance with this method call
     * </p>
     * @return the metric instance
     */
    Metric metric();

    /**
     * Returns a {@link MetricStore} instance.
     *
     * Note the plugin shall always returns the same instance
     *
     * @return the metric store instance
     */
    MetricStore metricStore();

    /**
     * Enable/Disable metric data sync to persistent store
     * @param sync `true` if enable sync data, `false` otherwise
     */
    void enableDataSync(boolean sync);
}
