package act.db;

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

import act.util.LogSupportedDestroyableBase;
import org.osgl.$;

import java.util.HashMap;
import java.util.Map;

public class DbManager extends LogSupportedDestroyableBase {
    private Map<String, DbPlugin> plugins = new HashMap<>();
    private Map<Class, TimestampGenerator> timestampGeneratorMap = new HashMap<>();

    synchronized void register(DbPlugin plugin) {
        plugins.put(plugin.getClass().getCanonicalName(), plugin);
    }

    synchronized void register(TimestampGenerator timestampGenerator) {
        timestampGeneratorMap.put(timestampGenerator.timestampType(), timestampGenerator);
    }

    public synchronized DbPlugin plugin(String type) {
        return plugins.get(type);
    }

    public synchronized boolean hasPlugin() {
        return !plugins.isEmpty();
    }

    public synchronized <TIMESTAMP_TYPE> TimestampGenerator<TIMESTAMP_TYPE> timestampGenerator(Class<? extends TIMESTAMP_TYPE> c) {
        return $.cast(timestampGeneratorMap.get(c));
    }

    /**
     * Returns the plugin if there is only One plugin inside
     * the register, otherwise return {@code null}
     */
    public synchronized DbPlugin theSolePlugin() {
        if (plugins.size() == 1) {
            return plugins.values().iterator().next();
        } else {
            return null;
        }
    }
}
