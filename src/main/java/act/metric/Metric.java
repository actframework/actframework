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

/**
 * `Metric` is used to measure the application and framework performance.
 *
 * Two kinds of metrics are been implemented in ActFramework:
 *
 *
 * * Counter - count number of time an event occurred</li>
 * * Timer - measure the time spent on a specific event</li>
 *
 * ActFramework supports hierarchical metric. For example, suppose you want to measure
 * the time spent on direct web request handling and background jobs:
 *
 * measure direct request handling
 *
 * ```
 *     {@literal @}PostAction("/xyz")
 *     public void handleXyzRequest(String param) {
 *         Timer timer = metric.startTimer("direct_req_handling:xyz");
 *         try {
 *             // your logic to handle xyz request
 *         } finally {
 *             timer.stop();
 *         }
 *     }
 * ```
 *
 * measure a background job
 *
 * ```
 *     {@literal @}Every("1hr")
 *     public void scanDatabase() {
 *         Timer timer = metric.startTimer("background_job:scan_db");
 *         try {
 *             // your logic to scan database
 *         } finally {
 *             timer.stop();
 *         }
 *     }
 * ```
 *
 *  In the sample code shown above, the name used to start timers are `direct_req_handling:xyz`
 *  and `background_job:scan_db` respectively. Both name contains a metric hierarchy, say
 *  `direct_req_handling` followed by `xyz` and `background_job` followed by
 *  `scan_db`. The benefit of metric hierarchy is the value measured on child name will get
 *  aggregated automatically on parent name: Suppose you have two counters: "a:b": 100, and "a:c": 105,
 *  then your "a" counter will be 205.
 *
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
     *
     * Note calling this method should automatically call {@link #countOnce(String)}
     * method with the `name` specified
     *
     * @param name A string specifies the timer
     * @return a Timer instance
     */
    Timer startTimer(String name);

}
