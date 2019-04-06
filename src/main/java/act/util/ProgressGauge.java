package act.util;

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

import java.util.Map;

/**
 * Class implement this interface can track progress
 */
public interface ProgressGauge {

    String PAYLOAD_MESSAGE = "message";

    /**
     * Set ID to the gauge
     *
     * @param id
     *      the gauge ID
     */
    void setId(String id);

    /**
     * Return ID of this gauge.
     *
     * **Note** the id of the gauge is the same
     * with the id of the Job this gauge monitor.
     *
     * @return the id of the gauge
     */
    String getId();

    /**
     * Update max hint. If the number is negative, then
     * it indicate the progress is indefinite
     *
     * @param maxHint the max steps hint
     */
    void updateMaxHint(int maxHint);

    /**
     * Increment max hint by 1.
     */
    void incrMaxHint();

    /**
     * Increment max hint by number specified
     * @param number the number to be add up to max hint
     */
    void incrMaxHintBy(int number);

    /**
     * Advances the progress by one step
     */
    void step();

    /**
     * Advances the progress by specified steps
     * @param steps the step size
     */
    void stepBy(int steps);

    /**
     * Log progress
     *
     * @param steps the new progress value
     */
    void stepTo(int steps);

    /**
     * Report the current progress steps
     *
     * @return the current progress
     */
    int currentSteps();

    /**
     * Returns the max hint
     *
     * @return the max hint setting
     */
    int maxHint();

    /**
     * Check if the progress has been finished
     */
    boolean isDone();

    /**
     * Mark the progress as done
     */
    void markAsDone();

    /**
     * Set payload to the gauge.
     *
     * This method could be used to pass additional message to
     * listener including websocket broadcast
     *
     * @param key the key
     * @param val the value
     */
    void setPayload(String key, Object val);

    /**
     * Returns the payload that has been set to this gauge.
     * @return the payload set to this gauge
     */
    Map<String, Object> getPayload();

    /**
     * Add an listener to this gauge that monitors the progress update
     * @param listener the listener
     */
    void addListener(Listener listener);

    interface Listener {
        void onUpdate(ProgressGauge progressGauge);
    }
}
