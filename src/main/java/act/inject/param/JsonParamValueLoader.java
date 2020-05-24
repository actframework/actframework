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

import act.Act;
import act.app.ActionContext;
import act.app.App;
import act.app.data.StringValueResolverManager;
import act.inject.DependencyInjector;
import act.util.ActContext;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.util.S;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import javax.inject.Provider;

class JsonParamValueLoader implements ParamValueLoader {

    private static final Provider NULL_PROVIDER = new Provider() {
        @Override
        public Object get() {
            return null;
        }
    };

    private ParamValueLoader fallBack;
    private BeanSpec spec;
    private Provider defValProvider;
    private boolean notController;

    JsonParamValueLoader(ParamValueLoader fallBack, BeanSpec spec, DependencyInjector<?> injector) {
        this.fallBack = $.requireNotNull(fallBack);
        this.spec = $.requireNotNull(spec);
        this.defValProvider = findDefValProvider(spec, injector);
        this.notController = null == Act.app().classLoader().controllerClassMetaInfo(spec.rawType().getName());
    }

    private JsonParamValueLoader(JsonParamValueLoader parent, Class runtimeType) {
        this.fallBack = parent.fallBack.wrapWithRuntimeType(runtimeType);
        this.spec = parent.spec;
        this.defValProvider = parent.defValProvider;
    }

    @Override
    public String toString() {
        return S.concat("json param|", fallBack);
    }

    @Override
    public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
        if (context instanceof ActionContext && ((ActionContext)context).isPathVar(spec.name())) {
            return fallBack.load(bean, context, noDefaultValue);
        }
        JsonDto dto = notController ? (JsonDto) context.attribute(JsonDto.CTX_ATTR_KEY) : null;
        if (null == dto) {
            return this.fallBack.load(bean, context, noDefaultValue);
        } else {
            String key = spec.name();
            Object o = dto.get(key);
            if (null != o) {
                return o;
            }
            if (context instanceof ActionContext) {
                if (key.contains(".")) {
                    String body = ((ActionContext) context).patchedJsonBody();
                    JSONObject json = JSON.parseObject(body);
                    o = $.getProperty(json, key);
                }
            }
            return null != o ? o : defValProvider.get();
        }
    }

    @Override
    public String bindName() {
        return spec.name();
    }

    @Override
    public boolean supportScopeCaching() {
        return fallBack.supportScopeCaching();
    }

    @Override
    public boolean supportJsonDecorator() {
        return false;
    }

    private static Provider findDefValProvider(BeanSpec beanSpec, DependencyInjector<?> injector) {
        final Class c = beanSpec.rawType();
        final StringValueResolverManager resolver = App.instance().resolverManager();
        if (c.isPrimitive()) {
            return new Provider() {
                @Override
                public Object get() {
                    return resolver.resolve(null, c);
                }
            };
        } else if (Collection.class.isAssignableFrom(c) || Map.class.isAssignableFrom(c)) {
            return injector.getProvider(c);
        } else if (c.isArray()) {
            return new Provider() {
                @Override
                public Object get() {
                    return Array.newInstance(c.getComponentType(), 0);
                }
            };
        } else {
            return NULL_PROVIDER;
        }
    }

    @Override
    public boolean requireRuntimeTypeInfo() {
        return fallBack.requireRuntimeTypeInfo();
    }

    @Override
    public ParamValueLoader wrapWithRuntimeType(Class<?> type) {
        return new JsonParamValueLoader(this, type);
    }
}
