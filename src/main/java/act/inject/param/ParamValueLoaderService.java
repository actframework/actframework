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
import act.app.data.BinderManager;
import act.app.data.StringValueResolverManager;
import act.controller.ActionMethodParamAnnotationHandler;
import act.inject.DefaultValue;
import act.inject.DependencyInjector;
import act.inject.SessionVariable;
import act.inject.genie.DependentScope;
import act.inject.genie.GenieInjector;
import act.inject.genie.RequestScope;
import act.inject.genie.SessionScope;
import act.util.ActContext;
import act.util.DestroyableBase;
import org.osgl.$;
import org.osgl.exception.UnexpectedException;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.InjectException;
import org.osgl.inject.util.AnnotationUtil;
import org.osgl.inject.util.ArrayLoader;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.mvc.annotation.Bind;
import org.osgl.mvc.annotation.Param;
import org.osgl.mvc.result.Result;
import org.osgl.mvc.util.Binder;
import org.osgl.util.*;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.New;
import javax.inject.Named;
import javax.inject.Provider;
import javax.validation.*;
import javax.validation.executable.ExecutableValidator;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manage {@link ParamValueLoader} grouped by Method
 */
public abstract class ParamValueLoaderService extends DestroyableBase {

    protected Logger logger = LogManager.get(getClass());

    private static final ParamValueLoader[] DUMB = new ParamValueLoader[0];
    private static final ThreadLocal<ParamTree> PARAM_TREE = new ThreadLocal<ParamTree>();
    private static final ParamValueLoader RESULT_LOADER = new ParamValueLoader() {
        @Override
        public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
            return context.attribute(ActionContext.ATTR_RESULT);
        }

        @Override
        public String bindName() {
            return null;
        }
    };
    private static final ParamValueLoader EXCEPTION_LOADED = new ParamValueLoader() {
        @Override
        public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
            return context.attribute(ActionContext.ATTR_EXCEPTION);
        }

        @Override
        public String bindName() {
            return null;
        }
    };
    // contains field names that should be waived when looking for value loader
    private static final Set<String> fieldBlackList = new HashSet<>();

    protected StringValueResolverManager resolverManager;
    protected BinderManager binderManager;
    protected GenieInjector injector;
    ConcurrentMap<Method, ParamValueLoader[]> methodRegistry = new ConcurrentHashMap<>();
    Map<Method, Boolean> methodValidationConstraintLookup = new HashMap();
    ConcurrentMap<Class, Map<Field, ParamValueLoader>> fieldRegistry = new ConcurrentHashMap<>();
    ConcurrentMap<Class, ParamValueLoader> classRegistry = new ConcurrentHashMap<>();
    private ConcurrentMap<$.T2<Type, Annotation[]>, ParamValueLoader> paramRegistry = new ConcurrentHashMap<>();
    private ConcurrentMap<BeanSpec, Map<Class<? extends Annotation>, ActionMethodParamAnnotationHandler>> annoHandlers = new ConcurrentHashMap<>();
    private Map<Class<? extends Annotation>, ActionMethodParamAnnotationHandler> allAnnotationHandlers;
    private Validator validator;
    private volatile ExecutableValidator executableValidator;

    public ParamValueLoaderService(App app) {
        resolverManager = app.resolverManager();
        binderManager = app.binderManager();
        injector = app.injector();
        allAnnotationHandlers = new HashMap<>();
        List<ActionMethodParamAnnotationHandler> list = Act.pluginManager().pluginList(ActionMethodParamAnnotationHandler.class);
        for (ActionMethodParamAnnotationHandler h : list) {
            Set<Class<? extends Annotation>> set = h.listenTo();
            for (Class<? extends Annotation> c : set) {
                allAnnotationHandlers.put(c, h);
            }
        }
    }

    @Override
    protected void releaseResources() {
        DestroyableBase.Util.tryDestroyAll(classRegistry.values(), ApplicationScoped.class);
        DestroyableBase.Util.tryDestroyAll(paramRegistry.values(), ApplicationScoped.class);
    }

    public Object loadHostBean(Class beanClass, ActContext<?> ctx) {
        ParamValueLoader loader = classRegistry.get(beanClass);
        if (null == loader) {
            ParamValueLoader newLoader = findBeanLoader(beanClass);
            loader = classRegistry.putIfAbsent(beanClass, newLoader);
            if (null == loader) {
                loader = newLoader;
            }
        }
        return loader.load(null, ctx, false);
    }

    public ParamValueLoader[] methodParamLoaders(Object host, Method method) {
        ParamValueLoader[] loaders = methodRegistry.get(method);
        if (null == loaders) {
            $.Var<Boolean> boolBag = $.var(Boolean.FALSE);
            Class hostClass = null == host ? null : host.getClass();
            ParamValueLoader[] newLoaders = findMethodParamLoaders(method, hostClass, boolBag);
            loaders = methodRegistry.putIfAbsent(method, newLoaders);
            if (null == loaders) {
                loaders = newLoaders;
            }
            boolean hasValidationConstraint = boolBag.get();
            if (hasValidationConstraint && null == host) {
                logger.error("Cannot validate static method: %s", method);
                hasValidationConstraint = false;
            }
            methodValidationConstraintLookup.put(method, hasValidationConstraint);
        }
        return loaders;
    }

    public Object[] loadMethodParams(Object host, Method method, ActContext ctx) {
        try {
            ParamValueLoader[] loaders = methodParamLoaders(host, method);
            Boolean hasValidationConstraint = methodValidationConstraintLookup.get(method);
            int sz = loaders.length;
            Object[] params = new Object[sz];
            for (int i = 0; i < sz; ++i) {
                params[i] = loaders[i].load(null, ctx, false);
            }
            if (null != hasValidationConstraint && hasValidationConstraint) {
                Set<ConstraintViolation> violations = $.cast(executableValidator().validateParameters(host, method, params));
                if (!violations.isEmpty()) {
                    Map<String, ConstraintViolation> map = new HashMap<>();
                    for (ConstraintViolation v : violations) {
                        S.Buffer buf = ctx.strBuf();
                        for (Path.Node node : v.getPropertyPath()) {
                            if (node.getKind() == ElementKind.METHOD) {
                                continue;
                            } else if (node.getKind() == ElementKind.PARAMETER) {
                                Path.ParameterNode pnode = node.as(Path.ParameterNode.class);
                                int paramIdx = pnode.getParameterIndex();
                                ParamValueLoader ploader = loaders[paramIdx];
                                buf.append(ploader.bindName());
                            } else if (node.getKind() == ElementKind.PROPERTY) {
                                buf.append(".").append(node.toString());
                            }
                        }
                        map.put(buf.toString(), v);
                    }
                    ctx.addViolations(map);
                }
            }
            return params;
        } finally {
            PARAM_TREE.remove();
        }
    }

    protected <T> ParamValueLoader findBeanLoader(Class<T> beanClass) {
        final Provider<T> provider = injector.getProvider(beanClass);
        final Map<Field, ParamValueLoader> loaders = fieldLoaders(beanClass);
        final boolean hasField = !loaders.isEmpty();
        final $.Var<Boolean> hasValidateConstraint = $.var();
        ParamValueLoader loader = new ParamValueLoader() {
            @Override
            public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
                if (null == bean) {
                    bean = provider.get();
                }
                if (!hasField) {
                    return bean;
                }
                try {
                    for (Map.Entry<Field, ParamValueLoader> entry : loaders.entrySet()) {
                        Field field = entry.getKey();
                        ParamValueLoader loader = entry.getValue();
                        Object fieldValue = loader.load(null, context, noDefaultValue);
                        if (null != fieldValue) {
                            field.set(bean, fieldValue);
                        } else {
                            fieldValue = field.get(bean);
                        }
                        if (hasValidationConstraint(BeanSpec.of(field, injector))) {
                            hasValidateConstraint.set(true);
                        }
                    }
                } catch (IllegalAccessException e) {
                    throw new InjectException(e);
                }
                return bean;
            }

            @Override
            public String bindName() {
                return null;
            }
        };
        return decorate(loader, BeanSpec.of(beanClass, injector), beanClass.getDeclaredAnnotations(), false, true);
    }

    private boolean shouldWaive(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isStatic(modifiers)
                || Modifier.isTransient(modifiers)
                || noBind(field.getDeclaringClass())
                || field.isAnnotationPresent(NoBind.class)
                || fieldBlackList.contains(field.getName())
                || Object.class.equals(field.getDeclaringClass())
                || field.getDeclaringClass().isAnnotationPresent(NoBind.class);
    }

    private ConcurrentMap<Class, Boolean> noBindCache = new ConcurrentHashMap<>();

    private boolean noBind(Class c) {
        Boolean b = noBindCache.get(c);
        if (null != b) {
            return b;
        }
        Annotation[] aa = c.getDeclaredAnnotations();
        if (null != aa) {
            for (Annotation a: aa) {
                if (a.annotationType() == NoBind.class) {
                    noBindCache.putIfAbsent(c, true);
                    return true;
                }
            }
        }
        noBindCache.putIfAbsent(c, false);
        return false;
    }

    private <T> Map<Field, ParamValueLoader> fieldLoaders(Class<T> beanClass) {
        Map<Field, ParamValueLoader> fieldLoaders = fieldRegistry.get(beanClass);
        if (null == fieldLoaders) {
            Map<Field, ParamValueLoader> newFieldLoaders = new HashMap<>();
            for (Field field : $.fieldsOf(beanClass, true)) {
                if (shouldWaive(field)) {
                    continue;
                }
                Type type = field.getGenericType();
                Annotation[] annotations = field.getAnnotations();
                BeanSpec spec = BeanSpec.of(type, annotations, field.getName(), injector);
                ParamValueLoader loader = paramValueLoaderOf(spec);
                boolean provided = (loader instanceof ProvidedValueLoader);
                if (null != loader && !provided) {
                    newFieldLoaders.put(field, loader);
                }
            }
            fieldLoaders = fieldRegistry.putIfAbsent(beanClass, newFieldLoaders);
            if (null == fieldLoaders) {
                fieldLoaders = newFieldLoaders;
            }
        }
        return fieldLoaders;
    }

    protected ParamValueLoader[] findMethodParamLoaders(Method method, Class host, $.Var<Boolean> hasValidationConstraint) {
        Type[] types = method.getGenericParameterTypes();
        int sz = types.length;
        if (0 == sz) {
            return DUMB;
        }
        ParamValueLoader[] loaders = new ParamValueLoader[sz];
        Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < sz; ++i) {
            String name = paramName(i);
            Type type = types[i];
            if (type instanceof TypeVariable) {
                TypeVariable var = $.cast(type);
                if (null != host) {
                    type = Generics.buildTypeParamImplLookup(host).get(var.getName());
                }
                if (null == type) {
                    throw new UnexpectedException("Cannot infer param type: %s", var.getName());
                }
            }
            BeanSpec spec = BeanSpec.of(type, annotations[i], name, injector);
            if (hasValidationConstraint(spec)) {
                hasValidationConstraint.set(true);
            }
            ParamValueLoader loader = paramValueLoaderOf(spec);
            if (null == loader) {
                throw new UnexpectedException("Cannot find param value loader for param: " + spec);
            }
            loaders[i] = loader;
        }
        return loaders;
    }

    private ParamValueLoader paramValueLoaderOf(BeanSpec spec) {
        return paramValueLoaderOf(spec, null);
    }

    private ParamValueLoader paramValueLoaderOf(BeanSpec spec, String bindName) {
        Class<?> rawType = spec.rawType();
        if (Result.class.isAssignableFrom(rawType)) {
            return RESULT_LOADER;
        } else if (Exception.class.isAssignableFrom(rawType)) {
            return EXCEPTION_LOADED;
        }
        Type type = spec.type();
        Annotation[] annotations = spec.allAnnotations();
        $.T2<Type, Annotation[]> key = $.T2(type, annotations);
        ParamValueLoader loader = paramRegistry.get(key);
        if (null == loader) {
            ParamValueLoader newLoader = findLoader(spec, type, annotations, bindName);
            if (null != newLoader) {
                // Cannot use spec as the key here because
                // spec does not compare Scoped annotation
                loader = paramRegistry.putIfAbsent(key, newLoader);
                if (null == loader) {
                    loader = newLoader;
                }
            }
        }
        return loader;
    }

    protected abstract ParamValueLoader findContextSpecificLoader(
            String bindName,
            Class<?> rawType,
            BeanSpec spec,
            Type type,
            Annotation[] annotations
    );

    protected final ParamValueLoader binder(BeanSpec spec, String bindName) {
        Class rawType = spec.rawType();
        ParamValueLoader loader = null;
        {
            Bind bind = spec.getAnnotation(Bind.class);
            if (null != bind) {
                for (Class<? extends Binder> binderClass : bind.value()) {
                    Binder binder = injector.get(binderClass);
                    if (rawType.isAssignableFrom(binder.targetType())) {
                        loader = new BoundedValueLoader(binder, bindName);
                        break;
                    }
                }
            }
        }
        if (null == loader) {
            Annotation[] aa = spec.allAnnotations();
            for (Annotation a : aa) {
                Bind bind = AnnotationUtil.tagAnnotation(a, Bind.class);
                if (null != bind) {
                    for (Class<? extends Binder> binderClass : bind.value()) {
                        Binder binder = injector.get(binderClass);
                        binder.attributes($.evaluate(a));
                        if (rawType.isAssignableFrom(binder.targetType())) {
                            loader = new BoundedValueLoader(binder, bindName);
                            break;
                        }
                    }
                }
            }
        }
        if (null == loader) {
            Binder binder = binderManager.binder(rawType);
            if (null != binder) {
                loader = new BoundedValueLoader(binder, bindName);
            }
        }
        return loader;
    }

    protected String paramName(int i) {
        return null;
    }

    protected boolean supportJsonDecorator() {
        return false;
    }

    private ParamValueLoader findLoader(
            BeanSpec spec,
            Type type,
            Annotation[] annotations,
            String bindName
    ) {
        if (provided(spec, injector)) {
            return ProvidedValueLoader.get(spec, injector);
        }
        if (null != filter(annotations, NoBind.class)) {
            return null;
        }
        if (null == bindName) {
            bindName = bindName(annotations, spec.name());
        }
        Class rawType = spec.rawType();
        ParamValueLoader loader = findContextSpecificLoader(bindName, rawType, spec, type, annotations);
        if (null == loader) {
            return null;
        }
        return decorate(loader, spec, annotations, supportJsonDecorator(), false);
    }

    ParamValueLoader buildLoader(final ParamKey key, final Type type, BeanSpec targetSpec) {
        Class rawType = BeanSpec.rawTypeOf(type);
        if (rawType.isArray()) {
            return buildArrayLoader(key, rawType.getComponentType(), targetSpec);
        }
        if (Collection.class.isAssignableFrom(rawType)) {
            Type elementType = Object.class;
            if (type instanceof ParameterizedType) {
                elementType = ((ParameterizedType) type).getActualTypeArguments()[0];
            }
            return buildCollectionLoader(key, rawType, elementType, targetSpec);
        }
        if (Map.class.isAssignableFrom(rawType)) {
            Class<?> mapClass = rawType;
            Type mapType = type;
            boolean canProceed = false;
            Type[] typeParams = null;
            while (true) {
                if (mapType instanceof ParameterizedType) {
                    typeParams = ((ParameterizedType) mapType).getActualTypeArguments();
                    if (typeParams.length == 2) {
                        canProceed = true;
                        break;
                    }
                }
                boolean foundInInterfaces = false;
                Type[] ta = mapClass.getGenericInterfaces();
                if (ta.length > 0) {
                    mapType = null;
                    for (Type t : ta) {
                        if (t instanceof ParameterizedType) {
                            if (Map.class.isAssignableFrom((Class) ((ParameterizedType) t).getRawType())) {
                                mapType = t;
                                mapClass = mapClass.getSuperclass();
                                foundInInterfaces = true;
                                break;
                            }
                        }
                    }
                }
                if (!foundInInterfaces) {
                    mapType = mapClass.getGenericSuperclass();
                    mapClass = mapClass.getSuperclass();
                }
                if (mapClass == Object.class) {
                    break;
                }
            }
            E.unexpectedIf(!canProceed, "Cannot load Map type parameter loader: no generic type info available");

            Type keyType = typeParams[0];
            Type valType = typeParams[1];
            return buildMapLoader(key, rawType, keyType, valType, targetSpec);
        }
        return buildPojoLoader(key, rawType);
    }

    private ParamValueLoader buildArrayLoader(
            final ParamKey key,
            final Type elementType,
            final BeanSpec targetSpec
    ) {
        final CollectionLoader collectionLoader = new CollectionLoader(key, ArrayList.class, elementType, targetSpec, injector, this);
        return new ParamValueLoader() {
            @Override
            public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
                List list = new ArrayList();
                if (null != bean) {
                    int len = Array.getLength(bean);
                    for (int i = 0; i < len; ++i) {
                        list.add(Array.get(bean, i));
                    }
                }
                list = (List) collectionLoader.load(list, context, false);
                return null == list ? null : ArrayLoader.listToArray(list, BeanSpec.rawTypeOf(elementType));
            }

            @Override
            public String bindName() {
                return key.toString();
            }
        };
    }

    private ParamValueLoader buildCollectionLoader(
            final ParamKey key,
            final Class<? extends Collection> collectionClass,
            final Type elementType,
            BeanSpec targetSpec
    ) {
        return new CollectionLoader(key, collectionClass, elementType, targetSpec, injector, this);
    }

    private ParamValueLoader buildMapLoader(
            final ParamKey key,
            final Class<? extends Map> mapClass,
            final Type keyType,
            final Type valType,
            final BeanSpec targetSpec
    ) {
        return new MapLoader(key, mapClass, keyType, valType, targetSpec, injector, this);
    }

    static ParamTree paramTree() {
        return PARAM_TREE.get();
    }

    static ParamTree ensureParamTree(ActContext context) {
        ParamTree tree = PARAM_TREE.get();
        if (null == tree) {
            tree = new ParamTree();
            tree.build(context);
            PARAM_TREE.set(tree);
        }
        return tree;
    }

    private ParamValueLoader buildPojoLoader(final ParamKey key, final Class type) {
        final List<FieldLoader> fieldLoaders = fieldLoaders(key, type);
        return new ParamValueLoader() {
            @Override
            public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
                final $.Var<Object> beanBag = $.var(bean);
                $.Factory<Object> beanSource = new $.Factory<Object>() {
                    @Override
                    public Object create() {
                        Object bean = beanBag.get();
                        if (null == bean) {
                            try {
                                bean = injector.get(type);
                            } catch (RuntimeException e) {
                                throw e;
                            } catch (Exception e) {
                                throw new InjectException(e, "cannot instantiate %s", type);
                            }
                        }
                        beanBag.set(bean);
                        return bean;
                    }
                };
                for (FieldLoader fl : fieldLoaders) {
                    fl.applyTo(beanSource, context);
                }
                return beanBag.get();
            }

            @Override
            public String bindName() {
                return key.toString();
            }
        };
    }

    private ParamValueLoader findLoader(ParamKey paramKey, Field field) {
        BeanSpec spec = BeanSpec.of(field.getGenericType(), field.getDeclaredAnnotations(), injector);
        Annotation[] annotations = field.getDeclaredAnnotations();
        if (provided(spec, injector)) {
            return ProvidedValueLoader.get(spec, injector);
        }
        String name = null;
        Named named = filter(annotations, Named.class);
        if (null != named) {
            name = named.value();
        }
        if (S.blank(name)) {
            name = field.getName();
        }
        ParamKey key = paramKey.child(name);

        Class fieldType = field.getType();
        StringValueResolver resolver = resolverManager.resolver(fieldType, spec);
        if (null != resolver) {
            DefaultValue def = field.getAnnotation(DefaultValue.class);
            return new StringValueResolverValueLoader(key, resolver, null, def, fieldType);
        }

        return buildLoader(key, field.getGenericType(), spec);
    }

    private List<FieldLoader> fieldLoaders(ParamKey key, Class type) {
        Class<?> current = type;
        List<FieldLoader> fieldLoaders = C.newList();
        while (null != current && !current.equals(Object.class)) {
            for (Field field : current.getDeclaredFields()) {
                if (shouldWaive(field)) {
                    continue;
                }
                field.setAccessible(true);
                fieldLoaders.add(fieldLoader(key, field));
            }
            current = current.getSuperclass();
        }
        return fieldLoaders;
    }

    private FieldLoader fieldLoader(ParamKey key, Field field) {
        return new FieldLoader(field, findLoader(key, field));
    }

    static <T extends Annotation> T filter(Annotation[] annotations, Class<T> annoType) {
        for (Annotation annotation : annotations) {
            if (annoType == annotation.annotationType()) {
                return (T) annotation;
            }
        }
        return null;
    }

    private ParamValueLoader decorate(
            final ParamValueLoader loader,
            final BeanSpec spec,
            final Annotation[] annotations,
            boolean useJsonDecorator,
            boolean useValidationDecorator
    ) {
        final ParamValueLoader jsonDecorated = useJsonDecorator ? new JsonParamValueLoader(loader, spec, injector) : loader;
        ParamValueLoader validationDecorated = jsonDecorated;
        if (useValidationDecorator) {
            validationDecorated = new ParamValueLoader() {
                private Validator validator = Act.app().getInstance(Validator.class);

                @Override
                public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
                    Object object = jsonDecorated.load(bean, context, noDefaultValue);
                    Set<ConstraintViolation> violations = $.cast(validator.validate(object));
                    if (!violations.isEmpty()) {
                        Map<String, ConstraintViolation> map = new HashMap<>();
                        for (ConstraintViolation v : violations) {
                            map.put(v.getPropertyPath().toString(), v);
                        }
                        context.addViolations(map);
                    }
                    return object;
                }

                @Override
                public String bindName() {
                    return jsonDecorated.bindName();
                }
            };

        }

        return new ScopedParamValueLoader(validationDecorated, spec, scopeCacheSupport(annotations));
    }

    private boolean hasValidationConstraint(BeanSpec spec) {
        for (Annotation anno : spec.allAnnotations()) {
            Class<? extends Annotation> annoType = anno.annotationType();
            if (Valid.class == annoType || annoType.isAnnotationPresent(Constraint.class)) {
                return true;
            }
        }
        return false;
    }

    private Map<Class<? extends Annotation>, ActionMethodParamAnnotationHandler> paramAnnoHandlers(BeanSpec spec) {
        Map<Class<? extends Annotation>, ActionMethodParamAnnotationHandler> handlers = annoHandlers.get(spec);
        if (null != handlers) {
            return handlers;
        }
        Map<Class<? extends Annotation>, ActionMethodParamAnnotationHandler> newHandlers = new HashMap<>();
        for (Annotation annotation : spec.allAnnotations()) {
            Class<? extends Annotation> c = annotation.annotationType();
            ActionMethodParamAnnotationHandler h = allAnnotationHandlers.get(c);
            if (null != h) {
                newHandlers.put(c, h);
            }
        }
        handlers = annoHandlers.putIfAbsent(spec, newHandlers);
        if (null == handlers) {
            handlers = newHandlers;
        }
        return handlers;
    }

    private ExecutableValidator executableValidator() {
        if (null == executableValidator) {
            synchronized (this) {
                if (null == executableValidator) {
                    validator = Act.getInstance(Validator.class);
                    executableValidator = validator.forExecutables();
                }
            }
        }
        return executableValidator;
    }

    private static ScopeCacheSupport scopeCacheSupport(Annotation[] annotations) {
        if (null != filter(annotations, RequestScoped.class) ||
                null != filter(annotations, org.osgl.inject.annotation.RequestScoped.class)) {
            return RequestScope.INSTANCE;
        } else if (sessionScoped(annotations)) {
            return SessionScope.INSTANCE;
        } else if (null != filter(annotations, Dependent.class) ||
                null != filter(annotations, New.class)) {
            return DependentScope.INSTANCE;
        }
        // Default to Request Scope
        return RequestScope.INSTANCE;
    }

    static boolean sessionScoped(Annotation[] annotations) {
        return null != filter(annotations, SessionScoped.class)
                || null != filter(annotations, org.osgl.inject.annotation.SessionScoped.class)
                || null != filter(annotations, SessionVariable.class);
    }

    public static void waiveFields(String... fieldNames) {
        fieldBlackList.addAll(C.listOf(fieldNames));
    }

    public static String bindName(Annotation[] annotations, String defVal) {
        String name = tryFindBindName(annotations, defVal);
        E.illegalStateIf(null == name, "Cannot find bind name");
        return name;
    }

    static String tryFindBindName(Annotation[] annotations, String defVal) {
        Param param = filter(annotations, Param.class);
        if (null != param && S.notBlank(param.value())) {
            return param.value();
        }
        Bind bind = filter(annotations, Bind.class);
        if (null != bind && S.notBlank(bind.model())) {
            return bind.model();
        }
        Named named = filter(annotations, Named.class);
        if (null != named && S.notBlank(named.value())) {
            return named.value();
        }
        if (S.notBlank(defVal)) {
            return defVal;
        }
        return null;
    }

    public static String bindName(BeanSpec beanSpec) {
        return bindName(beanSpec.allAnnotations(), beanSpec.name());
    }

    public static boolean provided(BeanSpec beanSpec, DependencyInjector<?> injector) {
        GenieInjector genieInjector = $.cast(injector);
        return genieInjector.isProvided(beanSpec);
    }

    public static boolean noBindOrProvided(BeanSpec beanSpec, DependencyInjector<?> injector) {
        return null != beanSpec.getAnnotation(NoBind.class) || provided(beanSpec, injector) || beanSpec.isInstanceOf(Throwable.class);
    }

}
