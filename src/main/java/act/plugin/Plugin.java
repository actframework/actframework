package act.plugin;

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

import org.osgl.util.C;

import java.util.List;
import java.util.Set;

/**
 * Tag a class that could be plug into Act stack
 */
public interface Plugin {
    void register();

    class InfoRepo {

        private static Set<String> plugins = C.newSet();

        public static synchronized void register(Plugin plugin) {
            boolean added = plugins.add(plugin.getClass().getName());
            if (added) {
                plugin.register();
            }
        }

        public static void clear() {
            plugins.clear();
        }

        public static List<String> plugins() {
            return C.list(plugins);
        }
    }
}
