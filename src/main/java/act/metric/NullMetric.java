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
 * Implement a do-nothing {@link Metric}
 */
enum NullMetric implements Metric {
    INSTANCE
    ;

    private static final Timer NULL_TIMER = new Timer() {
        @Override
        public String name() {
            return null;
        }

        @Override
        public void stop() {
        }

        @Override
        public long ns() {
            return 0;
        }

        @Override
        public void close() {
            stop();
        }
    };

    @Override
    public void countOnce(String name) {
    }

    @Override
    public Timer startTimer(String name) {
        return NULL_TIMER;
    }

}
