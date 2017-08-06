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
import act.cli.CliContext;
import act.cli.Optional;
import act.cli.Required;
import act.cli.meta.CommandMethodMetaInfo;
import act.cli.util.CommandLineParser;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.util.AnnotationUtil;
import org.osgl.mvc.annotation.Resolve;
import org.osgl.util.S;
import org.osgl.util.StringValueResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Responsible for loading param value for {@link ActionContext}
 */
public class CliContextParamLoader extends ParamValueLoaderService {

    private final static transient ThreadLocal<CommandMethodMetaInfo> methodMetaInfoHolder = new ThreadLocal<CommandMethodMetaInfo>();

    private ConcurrentMap<Method, List<OptionLoader>> optionLoaderRegistry = new ConcurrentHashMap<Method, List<OptionLoader>>();

    CliContextParamLoader(App app) {
        super(app);
    }

    public CliContext.ParsingContext buildParsingContext(Class commander, Method method, CommandMethodMetaInfo methodMetaInfo) {
        CliContext.ParsingContextBuilder.start();
        ensureOptionLoaders(method, methodMetaInfo);
        methodMetaInfoHolder.set(methodMetaInfo);
        ParamValueLoader loader = findBeanLoader(commander);
        classRegistry.putIfAbsent(commander, loader);
        $.Var<Boolean> boolBag = $.var();
        ParamValueLoader[] loaders = findMethodParamLoaders(method, commander, boolBag);
        methodRegistry.putIfAbsent(method, loaders);
        methodValidationConstraintLookup.put(method, boolBag.get());
        return CliContext.ParsingContextBuilder.finish();
    }

    public void preParseOptions(Method method, CommandMethodMetaInfo methodMetaInfo, CliContext context) {
        List<OptionLoader> optionLoaders = ensureOptionLoaders(method, methodMetaInfo);
        CommandLineParser commandLineParser = context.commandLine();
        boolean argumentAsOption = false;
        if (1 == optionLoaders.size()) {
            OptionLoader loader = optionLoaders.get(0);
            if (loader.required) {
                String theOptionVal = commandLineParser.argumentAsOption();
                if (null != theOptionVal) {
                    argumentAsOption = true;
                    context.parsingContext().foundRequired(loader.requiredGroup);
                    context.param(loader.bindName, theOptionVal);
                }
            }
        }
        if (!argumentAsOption) {
            for (OptionLoader loader : optionLoaders) {
                String bindName = loader.bindName;
                String value = commandLineParser.getString(loader.lead1, loader.lead2);
                if (S.notBlank(value)) {
                    if (loader.required) {
                        context.parsingContext().foundRequired(loader.requiredGroup);
                    }
                    context.param(bindName, value);
                }
            }
        }
        context.parsingContext().raiseExceptionIfThereAreMissingOptions(context);
    }

    @Override
    protected ParamValueLoader findContextSpecificLoader(
            String bindName,
            Class<?> rawType,
            BeanSpec spec,
            Type type,
            Annotation[] annotations
    ) {
        boolean isArray = rawType.isArray();
        StringValueResolver resolver = findResolver(spec, rawType, isArray); //= isArray ? resolverManager.resolver(rawType.getComponentType(), spec) : resolverManager.resolver(rawType, spec);

        Required required = filter(annotations, Required.class);
        Optional optional = null == required ? filter(annotations, Optional.class) : null;
        if (null != required) {
            return new OptionLoader(bindName, required, resolver, spec);
        } else if (null != optional) {
            return new OptionLoader(bindName, optional, resolver, spec);
        }
        return isArray ? new CliVarArgumentLoader(rawType.getComponentType(), resolver) : new CliArgumentLoader(resolver);
    }

    private StringValueResolver findResolver(BeanSpec spec, Class rawType, boolean isArray) {
        StringValueResolver resolver = findAnnotatedResolver(spec, rawType);
        return null == resolver ? findImplictResolver(spec, rawType, isArray) : resolver;
    }

    private StringValueResolver findAnnotatedResolver(BeanSpec spec, Class rawType) {
        StringValueResolver resolver = findDirectAnnotatedResolver(spec, rawType);
        return null == resolver ? findIndirectAnnotatedResolver(spec, rawType) : resolver;
    }

    private StringValueResolver findDirectAnnotatedResolver(BeanSpec spec, Class rawType) {
        Resolve resolve = spec.getAnnotation(Resolve.class);
        if (null != resolve) {
            Class<? extends StringValueResolver>[] resolvers = resolve.value();
            for (Class<? extends StringValueResolver> resolverClass : resolvers) {
                StringValueResolver resolver = injector.get(resolverClass);
                if (rawType.isAssignableFrom(resolver.targetType())) {
                    return resolver;
                }
            }
        }
        return null;
    }

    private StringValueResolver findIndirectAnnotatedResolver(BeanSpec spec, Class rawType) {
        Annotation[] aa = spec.allAnnotations();
        for (Annotation a : aa) {
            Resolve resolve = AnnotationUtil.tagAnnotation(a, Resolve.class);
            if (null != resolve) {
                Class<? extends StringValueResolver>[] resolvers = resolve.value();
                for (Class<? extends StringValueResolver> resolverClass : resolvers) {
                    StringValueResolver resolver = injector.get(resolverClass);
                    resolver.attributes($.evaluate(a));
                    if (rawType.isAssignableFrom(resolver.targetType())) {
                        return resolver;
                    }
                }
            }
        }
        return null;
    }

    private StringValueResolver findImplictResolver(BeanSpec spec, Class rawType, boolean isArray) {
        return isArray ? resolverManager.resolver(rawType.getComponentType(), spec) : resolverManager.resolver(rawType, spec);
    }

    @Override
    protected String paramName(int i) {
        return methodMetaInfoHolder.get().param(i).name();
    }

    private List<OptionLoader> ensureOptionLoaders(Method method, CommandMethodMetaInfo methodMetaInfo) {
        List<OptionLoader> optionLoaders = optionLoaderRegistry.get(method);
        if (null == optionLoaders) {
            optionLoaders = findOptionLoaders(method, methodMetaInfo);
            optionLoaderRegistry.put(method, optionLoaders);
        }
        return optionLoaders;
    }

    private List<OptionLoader> findOptionLoaders(Method method, CommandMethodMetaInfo methodMetaInfo) {
        List<OptionLoader> optionLoaders = new ArrayList<OptionLoader>();

        findParamOptionLoaders(method, methodMetaInfo, optionLoaders);
        findFieldOptionLoaders(method.getDeclaringClass(), optionLoaders);

        return optionLoaders;
    }

    private void findFieldOptionLoaders(Class c, List<OptionLoader> optionLoaders) {
        if (injector.isProvided(c)) {
            // No field injection for a provided host
            return;
        }
        for (Field field : $.fieldsOf(c, true)) {
            Type type = field.getGenericType();
            Annotation[] annotations = field.getAnnotations();
            String bindName = bindName(annotations, field.getName());
            BeanSpec spec = BeanSpec.of(type, annotations, bindName, injector);
            boolean provided = injector.isProvided(spec);
            ParamValueLoader loader = provided ? ProvidedValueLoader.get(spec, injector) : findContextSpecificLoader(bindName, field.getDeclaringClass(), spec, type, annotations);
            if (loader instanceof OptionLoader) {
                optionLoaders.add((OptionLoader) loader);
            }
        }
    }

    private void findParamOptionLoaders(Method m, CommandMethodMetaInfo methodMetaInfo, List<OptionLoader> optionLoaders) {
        Type[] types = m.getGenericParameterTypes();
        int len = types.length;
        if (len == 0) {
            return;
        }
        Annotation[][] allAnnotations = m.getParameterAnnotations();
        for (int i = len - 1; i >= 0; --i) {
            Type type = types[i];
            Annotation[] annotations = allAnnotations[i];
            BeanSpec spec = BeanSpec.of(type, annotations, null, injector);
            String bindName = tryFindBindName(annotations, spec.name());
            if (null == bindName) {
                bindName = methodMetaInfo.param(i).name();
            }
            ParamValueLoader loader = findContextSpecificLoader(bindName, spec.rawType(), spec, type, annotations);
            if (loader instanceof OptionLoader) {
                optionLoaders.add((OptionLoader) loader);
            } else if (!$.isSimpleType(spec.rawType())) {

            }
        }
    }
}
