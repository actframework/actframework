package act.metric;

/**
 * {@code Metric} is used to measure the application and framework performance.
 * <p>
 *     Two kinds of metrics are been implemented in ActFramework:
 * </p>
 * <ul>
 *     <li>Counter - count number of time an event occurred</li>
 *     <li>Timer - measure the time spent on a specific event</li>
 * </ul>
 * <p>
 *     ActFramework supports hierarchical metric. For example, suppose you want to measure
 *     the time spent on direct web request handling and background jobs:
 * </p>
 * <p>measure direct request handling</p>
 * <pre><code>
 *     {@literal @}PostAction("/xyz")
 *     public void handleXyzRequest(String param) {
 *         Timer timer = metric.startTimer("direct_req_handling:xyz");
 *         try {
 *             // your logic to handle xyz request
 *         } finally {
 *             timer.stop();
 *         }
 *     }
 * </code></pre>
 * <p>measure a background job</p>
 * <pre><code>
 *     {@literal @}Every("1hr")
 *     public void scanDatabase() {
 *         Timer timer = metric.startTimer("background_job:scan_db");
 *         try {
 *             // your logic to scan database
 *         } finally {
 *             timer.stop();
 *         }
 *     }
 * </code></pre>
 * <p>
 *     In the sample code shown above, the name used to start timers are "{@code direct_req_handling:xyz}"
 *     and "{@code background_job:scan_db}" respectively. Both name contains a metric hierarchy, say
 *     "{@code direct_req_handling}" followed by "{@code xyz}" and "{@code background_job}" followed by
 *     "{@code scan_db}". The benefit of metric hierarchy is the value measured on child name will get
 *     aggregated automatically on parent name: Suppose you have two counters: "a:b": 100, and "a:c": 105,
 *     then your "a" counter will be 205.
 * </p>
 */
public interface Metric {

    /**
     * {@code PATH_SEPARATOR} is provided to support hierarchical metric
     */
    String PATH_SEPARATOR = ":";

    /**
     * The do-nothing metric instance
     */
    Metric NULL_METRIC = NullMetric.INSTANCE;

    /**
     * Call this method to increase one time for the counter specified
     * @param name A string specifies the counter
     */
    void countOnce(String name);

    /**
     * Call this method to start a {@link Timer} before starting a process.
     * <p>
     *     Note calling this method should automatically call {@link #countOnce(String)}
     *     method with the {@code name} specified
     * </p>
     * @param name A string specifies the timer
     * @return a Timer instance
     */
    Timer startTimer(String name);

}
