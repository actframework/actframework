package act.db.util;

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

import act.app.DbServiceManager;
import act.conf.AppConfig;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Define the interface that can generate next or retrieve current sequence number
 */
public interface _SequenceNumberGenerator {
    /**
     * Generate next sequence number
     * @param name the name of the sequence
     * @return the next sequence number
     */
    long next(String name);

    /**
     * Returns the current sequence number
     * @param name the name of the sequence
     * @return the current sequence number
     */
    long get(String name);

    /**
     * Configure the sequence generator
     */
    void configure(AppConfig config, DbServiceManager dbManager);

    class InMemorySequenceNumberGenerator implements _SequenceNumberGenerator {

        private ConcurrentMap<String, AtomicLong> seqs = new ConcurrentHashMap<String, AtomicLong>();
        @Override
        public long next(String name) {
            return getSeq(name).getAndIncrement();
        }

        @Override
        public long get(String name) {
            return getSeq(name).get();
        }

        private AtomicLong getSeq(String name) {
            AtomicLong al = seqs.get(name);
            if (null == al) {
                AtomicLong newAl = new AtomicLong(0);
                al = seqs.putIfAbsent(name, newAl);
                if (null == al) {
                    al = newAl;
                }
            }
            return al;
        }

        @Override
        public void configure(AppConfig config, DbServiceManager dbManager) {
            // do nothing configuration
        }
    }

}
