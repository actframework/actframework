package act.monitor;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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

import java.lang.management.*;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Singleton;

/**
 * A `Probe` is a worker that probe system and return data
 */
public interface Probe {
    /**
     * The identifier of this probe. This could be used
     * for consumer to subscribe the data collected by
     * the probe
     *
     * @return the probe id.
     */
    String id();
    /**
     * Do probe job once and return the data
     *
     * @return the data
     */
    Map<String, Object> doJob();

    @Singleton
    class SystemProbe implements Probe {

        private RuntimeMXBean runtimeMXBean;
        private ThreadMXBean threadMXBean;
        private MemoryMXBean memoryMXBean;

        private Map<String, Object> status = new HashMap<>();

        public SystemProbe() {
            runtimeMXBean = ManagementFactory.getRuntimeMXBean();
            threadMXBean = ManagementFactory.getThreadMXBean();
            memoryMXBean = ManagementFactory.getMemoryMXBean();
        }

        @Override
        public String id() {
            return "sys";
        }

        @Override
        public Map<String, Object> doJob() {
            status.put("memory", memoryMXBean.getHeapMemoryUsage());
            status.put("threads", threadMXBean.getThreadCount());
            status.put("start", runtimeMXBean.getStartTime());
            status.put("uptime", runtimeMXBean.getUptime());
            return status;
        }
    }
}
