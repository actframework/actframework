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
import act.app.data.StringValueResolverManager;
import act.inject.DefaultValue;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.Injector;
import org.osgl.mvc.annotation.Param;
import org.osgl.util.E;
import org.osgl.util.StringValueResolver;

import java.util.HashMap;
import java.util.Map;

abstract class StringValueResolverValueLoaderBase extends ParamValueLoader.JsonBodySupported {

    protected final StringValueResolver<?> stringValueResolver;
    private final StringValueResolverManager resolverManager;
    private final boolean requireRuntimeType;
    private final Injector injector;
    protected final ParamKey paramKey;
    protected final Param param;
    protected final Object defVal;
    protected final DefaultValue defSpec;
    protected final BeanSpec paramSpec;
    protected final Map<Class, Object> defValMap = new HashMap<>();
    protected final Map<Class, StringValueResolver> resolverMap = new HashMap<>();


    public StringValueResolverValueLoaderBase(ParamKey key, DefaultValue def, BeanSpec spec, boolean simpleKeyOnly) {
        E.illegalArgumentIf(simpleKeyOnly && !key.isSimple());
        this.paramSpec = spec;
        this.param = spec.getAnnotation(Param.class);
        this.paramKey = key;
        this.defSpec = def;
        this.resolverManager = Act.app().resolverManager();
        this.injector = Act.app().injector();
        this.requireRuntimeType = !(spec.type() instanceof Class);
        this.stringValueResolver = this.requireRuntimeType ? null : lookupResolver(spec, spec.rawType());
        this.defVal = this.requireRuntimeType ? null : null != def ? this.stringValueResolver.resolve(def.value()) : defVal(param, spec.rawType());
    }

    protected StringValueResolverValueLoaderBase(ParamKey key, DefaultValue def, StringValueResolver resolver, BeanSpec paramSpec, boolean simpleKeyOnly) {
        E.illegalArgumentIf(simpleKeyOnly && !key.isSimple());
        this.paramSpec = paramSpec;
        this.param = paramSpec.getAnnotation(Param.class);
        this.paramKey = key;
        this.defSpec = def;
        this.resolverManager = Act.app().resolverManager();
        this.injector = Act.app().injector();
        this.requireRuntimeType = false;
        this.stringValueResolver = resolver;
        this.defVal = null != def ? this.stringValueResolver.resolve(def.value()) : defVal(param, paramSpec.rawType());
    }

    protected StringValueResolverValueLoaderBase(StringValueResolverValueLoaderBase parent, Class<?> runtimeType, StringValueResolver resolver, Object defVal) {
        this.injector = parent.injector;
        this.resolverManager = parent.resolverManager;
        this.requireRuntimeType = false;
        this.paramKey = parent.paramKey;
        this.param = parent.param;
        this.defVal = defVal;
        this.stringValueResolver = resolver;
        this.defSpec = parent.defSpec;
        this.paramSpec = BeanSpec.of(runtimeType, this.injector);
    }

    @Override
    public boolean requireRuntimeTypeInfo() {
        return this.requireRuntimeType;
    }

    @Override
    public String bindName() {
        return paramKey.toString();
    }

    protected StringValueResolver lookupResolver(BeanSpec spec, Class runtimeType) {
        StringValueResolver resolver = null;
        Param param = spec.getAnnotation(Param.class);
        if (null != param) {
            Class<? extends StringValueResolver> resolverClass = param.resolverClass();
            if (Param.DEFAULT_RESOLVER.class != resolverClass) {
                resolver = injector.get(resolverClass);
            }
        }

        if (null == resolver) {
            resolver = resolverManager.resolver(runtimeType, spec);
        }
        return resolver;
    }

    static Object defVal(Param param, Class<?> rawType) {
        if (boolean.class == rawType) {
            return null != param && param.defBooleanVal();
        } else if (int.class == rawType) {
            return null != param ? param.defIntVal() : 0;
        } else if (double.class == rawType) {
            return null != param ? param.defDoubleVal() : 0d;
        } else if (long.class == rawType) {
            return null != param ? param.defLongVal() : 0L;
        } else if (float.class == rawType) {
            return null != param ? param.defFloatVal() : 0f;
        } else if (char.class == rawType) {
            return null != param ? param.defCharVal() : '\0';
        } else if (byte.class == rawType) {
            return null != param ? param.defByteVal() : 0;
        } else if (short.class == rawType) {
            return null != param ? param.defShortVal() : 0;
        }
        return null;
    }
}
