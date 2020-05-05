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
import act.app.App;
import act.inject.DefaultValue;
import act.util.ActContext;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.util.ArrayLoader;
import org.osgl.util.E;
import org.osgl.util.Keyword;
import org.osgl.util.S;
import org.osgl.util.StringValueResolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class HeaderValueLoader extends ParamValueLoader.NonCacheable {

    private final String key;
    private final boolean multiValues;
    private final boolean isArray;
    private final Class elementType;
    private final Class targetType;
    private final StringValueResolver stringValueResolver;
    private final Object defVal;

    public HeaderValueLoader(String name, BeanSpec beanSpec) {
        this.key = key(name, beanSpec);
        this.targetType = beanSpec.rawType();
        this.isArray = targetType.isArray();
        this.multiValues = (isArray || Collection.class.isAssignableFrom(targetType));
        if (this.isArray) {
            this.elementType = this.targetType.getComponentType();
        } else if (this.multiValues) {
            this.elementType = (Class)(beanSpec.typeParams().get(0));
        } else {
            this.elementType = null;
        }
        Class effectiveType = null != elementType ? elementType : targetType;
        this.stringValueResolver = App.instance().resolverManager().resolver(effectiveType, beanSpec);
        E.illegalArgumentIf(null == this.stringValueResolver, "Cannot find out StringValueResolver for %s", beanSpec);
        DefaultValue defValAnno = beanSpec.getAnnotation(DefaultValue.class);
        if (null != defValAnno) {
            this.defVal = stringValueResolver.resolve(defValAnno.value());
        } else {
            this.defVal = StringValueResolverValueLoaderBase.defVal(null, effectiveType);
        }
    }

    @Override
    public String toString() {
        return S.concat("header loader[", bindName(), "]");
    }

    @Override
    public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
        if (context instanceof ActionContext) {
            return load((ActionContext) context, noDefaultValue);
        }
        throw E.unsupport();
    }

    private Object load(ActionContext context, boolean noDefaultValue) {
        if (multiValues) {
            Iterable<String> iterable = context.req().headers(key);
            List list = new ArrayList();
            if (null != iterable) {
                for (String s : iterable) {
                    Object obj = stringValueResolver.resolve(s);
                    if (null != obj) {
                        list.add(obj);
                    }
                }
            }
            if (isArray) {
                return ArrayLoader.listToArray(list, elementType);
            } else {
                Collection c = (Collection) App.instance().injector().get(targetType);
                c.addAll(list);
                return c;
            }
        } else {
            String value = context.req().header(key);
            Object obj = (null == value) ? null : stringValueResolver.resolve(value);
            return (null == obj) && !noDefaultValue ? defVal : obj;
        }
    }

    @Override
    public String bindName() {
        return key;
    }

    private String key(String name, BeanSpec spec) {
        if (S.notBlank(name)) {
            return name;
        }
        name = spec.name();
        return Keyword.of(name).httpHeader();
    }
}
