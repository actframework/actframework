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

import act.app.ActionContext;
import act.inject.genie.RequestScope;
import act.inject.genie.SessionScope;
import act.util.ActContext;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.util.S;

class ScopedParamValueLoader implements ParamValueLoader {
    private ParamValueLoader realLoader;
    private String key;
    private ScopeCacheSupport scopeCache;
    private boolean supportCaching;

    ScopedParamValueLoader(ParamValueLoader loader, BeanSpec beanSpec, ScopeCacheSupport scopeCache) {
        this.realLoader = loader;
        if (loader.supportScopeCaching()) {
            this.scopeCache = scopeCache;
            this.supportCaching = true;
        }
        this.key = scopeCache.key(beanSpec);
    }

    private ScopedParamValueLoader(ScopedParamValueLoader parent, Class runtimeType) {
        this.realLoader = parent.realLoader.wrapWithRuntimeType(runtimeType);
        this.key = parent.key;
        this.scopeCache = parent.scopeCache;
        this.supportCaching = parent.supportCaching;
    }

    @Override
    public String toString() {
        return S.concat("scoped|", realLoader);
    }

    @Override
    public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
        if (context instanceof ActionContext) {
            ActionContext ac = $.cast(context);
            Object renderArg = ac.renderArg(realLoader.bindName());
            if (null != renderArg) {
                return renderArg;
            }
        }
        if (supportCaching) {
            Object cached = scopeCache.get(key);
            boolean isSession = SessionScope.INSTANCE == scopeCache;
            if (isSession && null == cached) {
                cached = RequestScope.INSTANCE.get(key);
            }
            cached = realLoader.load(cached, context, noDefaultValue);
            scopeCache.put(key, cached);
            if (isSession) {
                RequestScope.INSTANCE.put(key, cached);
            }
            return cached;
        } else {
            return realLoader.load(null, context, noDefaultValue);
        }
    }

    @Override
    public String bindName() {
        return realLoader.bindName();
    }

    @Override
    public boolean supportJsonDecorator() {
        return realLoader.supportJsonDecorator();
    }

    @Override
    public boolean supportScopeCaching() {
        return realLoader.supportScopeCaching();
    }

    @Override
    public boolean requireRuntimeTypeInfo() {
        return realLoader.requireRuntimeTypeInfo();
    }

    @Override
    public ParamValueLoader wrapWithRuntimeType(Class<?> type) {
        return new ScopedParamValueLoader(this, type);
    }
}
