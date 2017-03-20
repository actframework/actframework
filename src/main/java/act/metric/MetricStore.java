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

    /**
     * Clear metric data
     */
    void clear();
}
