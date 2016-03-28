package act.metric;

import org.osgl.logging.Logger;

/**
 * Defines a Metric plugin
 */
public interface MetricPlugin {
    /**
     * Returns a {@link Metric} instance by name
     *
     * <p>
     *     The plugin shall check if a {@link org.osgl.logging.Logger} get by name "{@code metric.$name}"
     *     is {@link Logger#isTraceEnabled() trace enabled} to return the real Metric instance, otherwise
     *     then it shall return the {@link Metric#NULL_METRIC the do-nothing metric instance}
     * </p>
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
     * <p>
     *     Note the plugin shall always returns the same instance
     * </p>
     * @return the metric store instance
     */
    MetricStore metricStore();
}
