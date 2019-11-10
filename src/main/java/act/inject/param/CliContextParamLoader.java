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
import act.cli.CliContext;
import act.cli.Optional;
import act.cli.Required;
import act.cli.meta.CommandMethodMetaInfo;
import act.cli.util.CommandLineParser;
import act.inject.DefaultValue;
import act.util.ActContext;
import org.osgl.$;
import org.osgl.exception.UnexpectedException;
import org.osgl.http.H;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.util.AnnotationUtil;
import org.osgl.mvc.annotation.Resolve;
import org.osgl.util.S;
import org.osgl.util.StringValueResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
        if (!Modifier.isStatic(method.getModifiers())) {
            ParamValueLoader loader = findBeanLoader(commander);
            classRegistry.putIfAbsent(commander, loader);
        }
        $.Var<Boolean> boolBag = $.var();
        // create a pseudo ctx as we do not have one here
        // the ctx is just a way to pass the method info
        ActContext ctx = new ActContext.Base<ActContext.Base>(Act.app()) {
            @Override
            public Base accept(H.Format fmt) {
                return null;
            }

            @Override
            public H.Format accept() {
                return null;
            }

            @Override
            public String methodPath() {
                return null;
            }

            @Override
            public Set<String> paramKeys() {
                return null;
            }

            @Override
            public String paramVal(String key) {
                return null;
            }

            @Override
            public String[] paramVals(String key) {
                return new String[0];
            }
        };
        ctx.currentMethod(method);
        ParamValueLoader[] loaders = findMethodParamLoaders(method, commander, ctx, boolBag);
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
            BeanSpec spec
    ) {
        boolean isArray = spec.isArray();
        String defVal = null;
        DefaultValue defaultValue = spec.getAnnotation(DefaultValue.class);
        if (null != defaultValue) {
            defVal = defaultValue.value();
        }
        StringValueResolver resolver = findResolver(spec, isArray); //= isArray ? resolverManager.resolver(rawType.getComponentType(), spec) : resolverManager.resolver(rawType, spec);

        Required required = spec.getAnnotation(Required.class);
        Optional optional = null == required ? spec.getAnnotation(Optional.class) : null;
        if (null != required) {
            return new OptionLoader(bindName, required, resolver, spec);
        } else if (null != optional) {
            return new OptionLoader(bindName, optional, resolver, spec);
        }
        return isArray ? new CliVarArgumentLoader(spec.rawType().getComponentType(), resolver) : new CliArgumentLoader(resolver, defVal);
    }

    private StringValueResolver findResolver(BeanSpec spec, boolean isArray) {
        StringValueResolver resolver = findAnnotatedResolver(spec);
        return null == resolver ? findImplicitResolver(spec, isArray) : resolver;
    }

    private StringValueResolver findAnnotatedResolver(BeanSpec spec) {
        StringValueResolver resolver = findDirectAnnotatedResolver(spec);
        return null == resolver ? findIndirectAnnotatedResolver(spec) : resolver;
    }

    private StringValueResolver findDirectAnnotatedResolver(BeanSpec spec) {
        Resolve resolve = spec.getAnnotation(Resolve.class);
        Class<?> rawType = spec.rawType();
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
                    return resolver;
                }
            }
        }
        return null;
    }

    private StringValueResolver findIndirectAnnotatedResolver(BeanSpec spec) {
        Annotation[] aa = spec.allAnnotations();
        Class<?> rawType = spec.rawType();
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
                        return resolver;
                    }
                }
            }
        }
        return null;
    }

    private StringValueResolver findImplicitResolver(final BeanSpec spec, boolean isArray) {
        StringValueResolver resolver = resolverManager.resolver(spec.rawType(), spec);
        if (null != resolver) {
            return resolver;
        } else if (isArray) {
            final BeanSpec compSpec = spec.componentSpec();
            final StringValueResolver<ArrayList> colResolver = resolverManager.collectionResolver(ArrayList.class, compSpec.rawType(), S.COMMON_SEP);
            final boolean isPrimitive = $.isPrimitive(compSpec.rawType());
            return new StringValueResolver() {
                @Override
                public Object resolve(String s) {
                    List list = colResolver.resolve(s);
                    int size = list.size();
                    final Class<?> compType = compSpec.rawType();
                    Object array = Array.newInstance(compType, size);
                    for (int i = 0; i < size; ++i) {
                        Object item = list.get(i);
                        if (isPrimitive) {
                            if (boolean.class == compType) {
                                Array.setBoolean(array, i, ((Boolean)item).booleanValue());
                            } else if (byte.class == compType) {
                                Array.setByte(array, i, ((Byte) item).byteValue());
                            } else if (char.class == compType) {
                                Array.setChar(array, i, ((Character) item).charValue());
                            } else if (double.class == compType) {
                                Array.setDouble(array, i, ((Double) item).doubleValue());
                            } else if (float.class == compType) {
                                Array.setFloat(array, i, ((Float) item).floatValue());
                            } else if (int.class == compType) {
                                Array.setInt(array, i, ((Integer) item).intValue());
                            } else if (long.class == compType) {
                                Array.setLong(array, i, ((Long) item).longValue());
                            } else if (short.class == compType) {
                                Array.setShort(array, i, ((Short) item).shortValue());
                            } else {
                                throw new UnexpectedException("Unknown primitive type");
                            }
                        } else {
                            Array.set(array, i, item);
                        }
                    }
                    return array;
                }
            };
        } else {
            return null;
        }
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
            ParamValueLoader loader = provided ? ProvidedValueLoader.get(spec, injector) : findContextSpecificLoader(bindName, spec);
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
            ParamValueLoader loader = findContextSpecificLoader(bindName, spec);
            if (loader instanceof OptionLoader) {
                optionLoaders.add((OptionLoader) loader);
            } else if (!$.isSimpleType(spec.rawType())) {

            }
        }
    }
}
