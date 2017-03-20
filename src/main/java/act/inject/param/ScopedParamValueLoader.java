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

import act.inject.genie.RequestScope;
import act.inject.genie.SessionScope;
import act.util.ActContext;
import org.osgl.inject.BeanSpec;

class ScopedParamValueLoader implements ParamValueLoader {
    private ParamValueLoader realLoader;
    private String key;
    private ScopeCacheSupport scopeCache;

    ScopedParamValueLoader(ParamValueLoader loader, BeanSpec beanSpec, ScopeCacheSupport scopeCache) {
        this.realLoader = loader;
        this.scopeCache = scopeCache;
        this.key = scopeCache.key(beanSpec);
    }

    @Override
    public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
        Object cached = scopeCache.get(key);
        boolean isSession = SessionScope.INSTANCE == scopeCache;
        if (isSession) {
            Object requestScoped = RequestScope.INSTANCE.get(key);
            if (null != requestScoped) {
                return requestScoped;
            }
        }
        cached = realLoader.load(cached, context, noDefaultValue);
        scopeCache.put(key, cached);
        if (isSession) {
            RequestScope.INSTANCE.put(key, cached);
        }
        return cached;
    }

    @Override
    public String bindName() {
        return realLoader.bindName();
    }
}
