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

import java.io.Closeable;

/**
 * A Timer that could be used to measure execution duration of a certain process
 */
public interface Timer extends Closeable {

    /**
     * Returns the name of the timer. The string returned should be
     * identical to the string passed in {@link Metric#startTimer(String)}
     * method that returns this timer
     *
     * @return the timer's name
     */
    String name();

    /**
     * Stop the timer
     */
    void stop();

    /**
     * Returns nanoseconds the time has elapsed since time being created till
     * {@link #stop()} method get called
     *
     * @return the duration time in nanoseconds
     */
    long ns();

    /**
     * Overwrite {@link Closeable#close()} without throwing out
     * the {@link java.io.IOException}.
     *
     * Calling `Timer.close()` shall trigger {@link #stop()}.
     */
    @Override
    void close();
}
