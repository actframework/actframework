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

import org.osgl.$;

/**
 * A simple implementation of {@link Timer}
 */
public class SimpleTimer implements Timer {

    private String name;
    private MetricStore metricStore;
    private long start;
    private long duration;

    public SimpleTimer(String name, MetricStore metricStore) {
        this.name = $.requireNotNull(name);
        this.metricStore = $.requireNotNull(metricStore);
        this.start = $.ns();
        metricStore.onTimerStart(name);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void stop() {
        duration = $.ns() - start;
        metricStore.onTimerStop(this);
    }

    @Override
    public long ns() {
        return duration;
    }

    @Override
    public void close() {
        stop();
    }
}
