package act.conf;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2020 ActFramework
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

import java.util.Map;

/**
 * Allow application to define customized application configuration loading logic
 */
public interface ExtendedAppConfLoader {
    /**
     * Implement class shall overwrite this method and return
     * application configurations.
     *
     * @return application configurations
     */
    Map<String, Object> loadConfigurations();

    static class DumbLoader implements ExtendedAppConfLoader {
        @Override
        public Map<String, Object> loadConfigurations() {
            return C.Map();
        }
    }
}
