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

import act.app.App;
import act.inject.DefaultValue;
import act.inject.HeaderVariable;
import act.inject.SessionVariable;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.util.AnnotationUtil;
import org.osgl.mvc.annotation.Param;
import org.osgl.mvc.annotation.Resolve;
import org.osgl.util.StringValueResolver;

import java.lang.annotation.Annotation;

/**
 * Responsible for loading param value for {@link act.app.ActionContext}
 */
class ActionContextParamLoader extends ParamValueLoaderService {

    ActionContextParamLoader(App app) {
        super(app);
    }

    @Override
    protected ParamValueLoader findContextSpecificLoader(
            String bindName,
            BeanSpec spec
    ) {
        HeaderVariable headerVariable = spec.getAnnotation(HeaderVariable.class);
        if (null != headerVariable) {
            return new HeaderValueLoader(headerVariable.value(), spec);
        }

        SessionVariable sessionVariable = spec.getAnnotation(SessionVariable.class);
        if (null != sessionVariable) {
            return new SessionValueLoader(sessionVariable.value(), spec);
        }

        if (spec.hasAnnotation(PartialPath.class)) {
            return new PartialPathLoader(bindName);
        }

        DefaultValue def = spec.getAnnotation(DefaultValue.class);

        Class<?> rawType = spec.rawType();
        ParamValueLoader loader = binder(spec, bindName);
        if (null == loader) {
            Resolve resolve = spec.getAnnotation(Resolve.class);
            if (null != resolve) {
                Class<? extends StringValueResolver>[] resolvers = resolve.value();
                for (Class<? extends StringValueResolver> resolverClass : resolvers) {
                    StringValueResolver resolver = injector.get(resolverClass);
                    Class<?> targetType = resolver.targetType();
                    boolean matches = rawType.isAssignableFrom(targetType);
                    if (!matches) {
                        Class<?> rawType2 = $.wrapperClassOf(rawType);
                        if (rawType != rawType2) {
                            matches = rawType2.isAssignableFrom(targetType);
                        }
                    }
                    if (matches) {
                        loader = new StringValueResolverValueLoader(ParamKey.of(bindName), def, resolver, spec);
                    }
                }
            }
        }

        if (null == loader) {
            Annotation[] aa = spec.allAnnotations();
            for (Annotation a : aa) {
                Resolve resolve = AnnotationUtil.tagAnnotation(a, Resolve.class);
                if (null != resolve) {
                    Class<? extends StringValueResolver>[] resolvers = resolve.value();
                    for (Class<? extends StringValueResolver> resolverClass : resolvers) {
                        StringValueResolver resolver = injector.get(resolverClass);
                        resolver.attributes($.evaluate(a));
                        Class<?> targetType = resolver.targetType();
                        boolean matches = rawType.isAssignableFrom(targetType);
                        if (!matches) {
                            Class<?> rawType2 = $.wrapperClassOf(rawType);
                            if (rawType != rawType2) {
                                matches = rawType2.isAssignableFrom(targetType);
                            }
                        }
                        if (matches) {
                            loader = new StringValueResolverValueLoader(ParamKey.of(bindName), def, resolver, spec);
                            break;
                        }
                    }
                }
            }
        }

        if (null == loader) {
            StringValueResolver resolver = null;
            Param param = spec.getAnnotation(Param.class);
            if (null != param) {
                Class<? extends StringValueResolver> resolverClass = param.resolverClass();
                if (Param.DEFAULT_RESOLVER.class != resolverClass) {
                    resolver = injector.get(resolverClass);
                }
            }

            if (null == resolver) {
                resolver = resolverManager.resolver(rawType, spec);
            }

            loader = (null != resolver) ? new StringValueResolverValueLoader(ParamKey.of(bindName), def, spec) : buildLoader(ParamKey.of(bindName), spec);
        }

        return loader;
    }

    @Override
    protected boolean supportJsonDecorator() {
        return true;
    }
}
