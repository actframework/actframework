package act.metric;

import java.util.List;

/**
 * {@code MetricStore} back all {@link Metric} instances generated
 * in ActFramework.
 */
public interface MetricStore {

    /**
     * Increment one on counter specified
     * @param name A string specify the counter
     */
    void countOnce(String name);

    void onTimerStart(String name);

    void onTimerStop(Timer timer);

    /**
     * Returns the counts of counter specified
     *
     * @param name A string specifies the counter
     * @return the counts or {@code null} if the counter cannot be found
     */
    Long count(String name);

    /**
     * Returns the aggregated time in nanoseconds of timer specified
     *
     * @param name A string specifies the timer
     * @return the aggregated time in nanoseconds or {@code null} if counter cannot be found
     */
    Long ns(String name);

    /**
     * Returns all counter names
     * @return counter names in a list
     */
    List<MetricInfo> counters();

    /**
     * Returns all timer names
     * @return timer names in a list
     */
    List<MetricInfo> timers();
}
