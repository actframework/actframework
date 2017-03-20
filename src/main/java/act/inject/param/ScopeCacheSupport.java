package act.inject.param;

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

import org.osgl.inject.BeanSpec;
import org.osgl.inject.ScopeCache;

public interface ScopeCacheSupport {

    <T> T get(String key);

    <T> void put(String key, T t);

    String key(BeanSpec spec);

    abstract class Base implements ScopeCacheSupport, ScopeCache {

        @Override
        public String key(BeanSpec spec) {
            return spec.toString();
        }
    }
}
