package act.internal.util;

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

import act.Act;
import act.conf.ActConfig;

// Calculate the default strBuf retention limit size
public class StrBufRetentionLimitCalculator {
    public int calculate() {
        long availableKilobytes = availableMemorySize() / 1024;
        long reservedForStrBuf = availableKilobytes / 128;
        long reservedForStrBufPerThread = reservedForStrBuf / threadsCount();
        return (int) reservedForStrBufPerThread;
    }

    private long availableMemorySize() {
        Runtime rt = Runtime.getRuntime();
        rt.gc();
        return rt.freeMemory();
    }

    private int threadsCount() {
        int workerThreads = Runtime.getRuntime().availableProcessors() * 2 * 8;
        ActConfig conf = Act.conf();
        int maxWorkerThreads = null == conf ? 0 : conf.xioMaxWorkerThreads();
        return maxWorkerThreads > 0 ? Math.min(maxWorkerThreads, workerThreads) : workerThreads;
    }

    public static void main(String[] args) {
        System.out.println(new StrBufRetentionLimitCalculator().calculate());
    }
}
