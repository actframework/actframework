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
import act.db.AdaptiveRecord;
import act.db.DbBind;
import act.db.di.FindBy;
import act.inject.*;
import act.inject.genie.*;
import act.util.*;
import org.osgl.*;
import org.osgl.exception.UnexpectedException;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.InjectException;
import org.osgl.inject.util.AnnotationUtil;
import org.osgl.inject.util.ArrayLoader;
import org.osgl.mvc.annotation.Bind;
import org.osgl.mvc.annotation.Param;
import org.osgl.mvc.result.Result;
import org.osgl.mvc.util.Binder;
import org.osgl.util.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.enterprise.context.*;
import javax.enterprise.inject.New;
import javax.inject.*;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.validation.*;
import javax.validation.executable.ExecutableValidator;

/**
 * Manage {@link ParamValueLoader} grouped by Method
 */
public abstract class ParamValueLoaderService extends LogSupportedDestroyableBase {

    private static final ParamValueLoader[] DUMB = new ParamValueLoader[0];
    private static final ThreadLocal<ParamTree> PARAM_TREE = new ThreadLocal<ParamTree>();
    private static final ParamValueLoader RESULT_LOADER = new ParamValueLoader.NonCacheable() {
        @Override
        public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
            return ((ActionContext) context).result();
        }

        @Override
        public String toString() {
            return S.concat("result loader[", bindName(), "]");
        }
    };

    private static class ThrowableLoader extends ParamValueLoader.NonCacheable {
        private Class<? extends Throwable> throwableType;

        public ThrowableLoader(Class<? extends Throwable> throwableType) {
            this.throwableType = throwableType;
        }

        @Override
        public String toString() {
            return S.concat("throwable loader[", bindName(), "|", throwableType.getSimpleName(), "]");
        }

        @Override
        public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
            Object o = context.attribute(ActionContext.ATTR_EXCEPTION);
            return throwableType.isInstance(o) ? o : null;
        }

    }

    // contains field names that should be waived when looking for value loader
    private static final Set<String> fieldBlackList = new HashSet<>();

    protected StringValueResolverManager resolverManager;
    protected BinderManager binderManager;
    protected GenieInjector injector;
    ConcurrentHashMap<Class, Map<String, Class>> typeLookupLookup = new ConcurrentHashMap<>();
    ConcurrentHashMap<$.Pair<ParamValueLoader, Class>, ParamValueLoader> runtimeLoaders = new ConcurrentHashMap<>();
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
        noBindCache.clear();
    }

    public ParamValueLoader hostBeanLoader(Class beanClass) {
        ParamValueLoader loader = classRegistry.get(beanClass);
        if (null == loader) {
            ParamValueLoader newLoader = findBeanLoader(beanClass);
            loader = classRegistry.putIfAbsent(beanClass, newLoader);
            if (null == loader) {
                loader = newLoader;
            }
        }
        return loader;
    }

    public Object loadHostBean(Class beanClass, ActContext<?> ctx) {
        ParamValueLoader loader = hostBeanLoader(beanClass);
        return loader.load(null, ctx, false);
    }

    public ParamValueLoader[] methodParamLoaders(Object host, Method method, ActContext ctx) {
        ParamValueLoader[] loaders = methodRegistry.get(method);
        if (null == loaders) {
            $.Var<Boolean> boolBag = $.var(Boolean.FALSE);
            Class hostClass = null == host ? null : host.getClass();
            ParamValueLoader[] newLoaders = findMethodParamLoaders(method, hostClass, ctx, boolBag);
            loaders = methodRegistry.putIfAbsent(method, newLoaders);
            if (null == loaders) {
                loaders = newLoaders;
            }
            boolean hasValidationConstraint = boolBag.get();
            if (hasValidationConstraint && null == host) {
                error("Cannot validate static method: %s", method);
                hasValidationConstraint = false;
            }
            methodValidationConstraintLookup.put(method, hasValidationConstraint);
        }
        return loaders;
    }

    private Map<String, Class> getTypeLookup(Class host) {
        Map<String, Class> lookup = typeLookupLookup.get(host);
        if (null == lookup) {
            lookup = Generics.buildTypeParamImplLookup(host);
            typeLookupLookup.put(host, lookup);
        }
        return lookup;
    }

    public Object[] loadMethodParams(Object host, Method method, ActContext ctx) {
        ParamValueLoader[] loaders = methodParamLoaders(host, method, ctx);
        Boolean hasValidationConstraint = methodValidationConstraintLookup.get(method);
        int sz = loaders.length;
        Object[] params = new Object[sz];
        for (int i = 0; i < sz; ++i) {
            ParamValueLoader loader = loaders[i];
            if (loader.requireRuntimeTypeInfo()) {
                Class hostType = host.getClass();
                $.Pair<ParamValueLoader, Class> key = $.Pair(loader, hostType);
                ParamValueLoader runtimeLoader = runtimeLoaders.get(key);
                if (null == runtimeLoader) {
                    Map<String, Class> typeLookups = getTypeLookup(hostType);
                    Type[] paramTypes = method.getGenericParameterTypes();
                    Type paramType = paramTypes[i];
                    Class runtimeType = typeLookups.get(((TypeVariable) paramType).getName());
                    runtimeLoader = loader.wrapWithRuntimeType(runtimeType);
                    runtimeLoaders.put(key, runtimeLoader);
                }
                loader = runtimeLoader;
            }
            params[i] = loader.load(null, ctx, false);
        }
        if (null != hasValidationConstraint && hasValidationConstraint) {
            Set<ConstraintViolation> violations = $.cast(executableValidator().validateParameters(host, method, params));
            if (!violations.isEmpty()) {
                Map<String, ConstraintViolation> map = new HashMap<>();
                for (ConstraintViolation v : violations) {
                    S.Buffer buf = S.buffer();
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
    }

    protected <T> ParamValueLoader findBeanLoader(final Class<T> beanClass) {
        final Provider<T> provider = injector.getProvider(beanClass);
        final Map<Field, ParamValueLoader> loaders = fieldLoaders(beanClass);
        final boolean hasField = !loaders.isEmpty();
        final $.Var<Boolean> hasValidateConstraint = $.var();
        ParamValueLoader loader = new ParamValueLoader.NonCacheable() {
            @Override
            public String toString() {
                return S.concat("findBeanLoader[", bindName(), "]");
            }

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
                        if (loader.requireRuntimeTypeInfo()) {
                            $.Pair<ParamValueLoader, Class> key = $.Pair(loader, (Class)beanClass);
                            ParamValueLoader runtimeLoader = runtimeLoaders.get(key);
                            if (null == runtimeLoader) {
                                Map<String, Class> typeLookups = getTypeLookup(beanClass);
                                Type paramType = field.getGenericType();
                                Class runtimeType = typeLookups.get(((TypeVariable) paramType).getName());
                                runtimeLoader = loader.wrapWithRuntimeType(runtimeType);
                                runtimeLoaders.put(key, runtimeLoader);
                            }
                            loader = runtimeLoader;
                        }
                        Object fieldValue = loader.load(null, context, noDefaultValue);
                        if (null != fieldValue) {
                            field.set(bean, fieldValue);
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
        };
        return decorate(loader, BeanSpec.of(beanClass, injector), false, true);
    }

    public static boolean shouldWaive(Field field) {
        int modifiers = field.getModifiers();
        if (Modifier.isTransient(modifiers) || Modifier.isStatic(modifiers)) {
            return true;
        }
        String fieldName = field.getName();
        Class<?> entityType = field.getDeclaringClass();
        return noBind(entityType)
                || field.isAnnotationPresent(NoBind.class)
                || field.isAnnotationPresent(Stateless.class)
                || field.isAnnotationPresent(Global.class)
                || fieldBlackList.contains(fieldName)
                || Object.class.equals(entityType)
                || Class.class.equals(entityType)
                || OsglConfig.globalMappingFilter_shouldIgnore(fieldName);
    }

    public static boolean isInBlackList(String fieldName) {
        return fieldBlackList.contains(fieldName);
    }

    private static ConcurrentMap<Class, Boolean> noBindCache;

    public static void classInit(App app) {
        noBindCache = app.createConcurrentMap();
    }

    public static boolean noBind(Class c) {
        Boolean b = noBindCache.get(c);
        if (null != b) {
            return b;
        }
        if (SingletonBase.class.isAssignableFrom(c)
                || c.isAnnotationPresent(NoBind.class)
                || c.isAnnotationPresent(Stateless.class)
                || c.isAnnotationPresent(Singleton.class)) {
            noBindCache.putIfAbsent(c, true);
            return true;
        }
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
                BeanSpec spec = BeanSpec.of(field, injector);
                ParamValueLoader loader = paramValueLoaderOf(spec, null);
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

    protected ParamValueLoader[] findMethodParamLoaders(
            Method method, Class host,
            ActContext ctx, $.Var<Boolean> hasValidationConstraint) {
        Type[] types = method.getGenericParameterTypes();
        int sz = types.length;
        if (0 == sz) {
            return DUMB;
        }
        ParamValueLoader[] loaders = new ParamValueLoader[sz];
        Annotation[][] annotations = ReflectedInvokerHelper.requestHandlerMethodParamAnnotations(method);
        for (int i = 0; i < sz; ++i) {
            String name = paramName(i);
            Type type = types[i];
            Map<String, Class> typeLookups = null;
            if (type instanceof TypeVariable || type instanceof ParameterizedType) {
                typeLookups = Generics.buildTypeParamImplLookup(host);
            }
            BeanSpec spec = BeanSpec.of(type, annotations[i], name, injector, typeLookups);
            if (hasValidationConstraint(spec)) {
                hasValidationConstraint.set(true);
            }
            ParamValueLoader loader = paramValueLoaderOf(spec, ctx);
            if (null == loader) {
                throw new UnexpectedException("Cannot find param value loader for param: " + spec);
            }
            loaders[i] = loader;
        }
        return loaders;
    }

    private ParamValueLoader paramValueLoaderOf(BeanSpec spec, ActContext ctx) {
        return paramValueLoaderOf(spec, null, ctx);
    }

    private ParamValueLoader paramValueLoaderOf(BeanSpec spec, String bindName, ActContext ctx) {
        Class<?> rawType = spec.rawType();
        if (Result.class.isAssignableFrom(rawType)) {
            return RESULT_LOADER;
        } else if (Throwable.class.isAssignableFrom(rawType)) {
            return new ThrowableLoader((Class<? extends Throwable>) rawType);
        } else if (Annotation.class.isAssignableFrom(rawType)) {
            return findHandlerMethodAnnotation((Class<? extends Annotation>) rawType, ctx);
        }
        Type type = spec.type();
        Annotation[] annotations = spec.allAnnotations();
        $.T2<Type, Annotation[]> key = $.T2(type, annotations);
        ParamValueLoader loader = paramRegistry.get(key);
        if (null == loader) {
            ParamValueLoader newLoader = findLoader(bindName, spec);
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

    /**
     * Returns a `ParamValueLoader` that load annotation from:
     * * the handler method
     * * the current method (might be a intercepter method)
     *
     * @param annoType
     *         the annotation type
     * @param ctx
     *         the current {@link ActContext}
     * @return a `ParamValueLoader` instance
     */
    private ParamValueLoader findHandlerMethodAnnotation(final Class<? extends Annotation> annoType, ActContext<?> ctx) {
        if (null == ctx) {
            return ParamValueLoader.NIL;
        }
        return new ParamValueLoader.NonCacheable() {
            @Override
            public Object load(Object bean, ActContext<?> ctx, boolean noDefaultValue) {
                Method handlerMethod = ctx.handlerMethod();
                Method curMethod = ctx.currentMethod();
                boolean methodIsCurrent = handlerMethod == curMethod || null == curMethod;

                Annotation anno = handlerMethod.getAnnotation(annoType);
                if (null == anno && !methodIsCurrent) {
                    anno = curMethod.getAnnotation(annoType);
                }
                return anno;
            }

            @Override
            public String toString() {
                return S.concat("findHandlerMethodAnnotation[", bindName(), "]");
            }
        };
    }


    protected abstract ParamValueLoader findContextSpecificLoader(
            String bindName,
            BeanSpec spec
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
                        loader = new BoundValueLoader(binder, bindName);
                        break;
                    }
                }
            }
        }
        if (null == loader) {
            Annotation[] aa = spec.taggedAnnotations(Bind.class);
            if (aa.length > 0) {
                for (Annotation a : aa) {
                    Bind bind = AnnotationUtil.tagAnnotation(a, Bind.class);
                    for (Class<? extends Binder> binderClass : bind.value()) {
                        Binder binder = injector.get(binderClass);
                        binder.attributes($.evaluate(a));
                        if (rawType.isAssignableFrom(binder.targetType())) {
                            loader = new BoundValueLoader(binder, bindName);
                            break;
                        }
                    }
                }
            }
        }
        if (null == loader) {
            Binder binder = binderManager.binder(rawType);
            if (null != binder) {
                loader = new BoundValueLoader(binder, bindName);
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
            String bindName,
            BeanSpec spec
    ) {
        if (provided(spec, injector)) {
            return ProvidedValueLoader.get(spec, injector);
        }
        if (spec.hasAnnotation(NoBind.class)) {
            return null;
        }
        if (null == bindName) {
            bindName = bindName(spec.allAnnotations(), spec.name());
        }
        ParamValueLoader loader = findContextSpecificLoader(bindName, spec);
        if (null == loader) {
            return null;
        }
        return decorate(loader, spec, supportJsonDecorator(), false);
    }

    ParamValueLoader buildLoader(final ParamKey key, BeanSpec spec) {
        Type type = spec.type();
        Class rawType = spec.rawType();
        if (rawType.isArray()) {
            return buildArrayLoader(key, rawType.getComponentType(), spec);
        }
        if (Collection.class.isAssignableFrom(rawType)) {
            List<Type> typeParams = spec.typeParams();
            Type elementType = Object.class;
            if (type instanceof ParameterizedType) {
                elementType = typeParams.get(0);
            }
            return buildCollectionLoader(key, rawType, elementType, spec);
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
            return buildMapLoader(key, rawType, keyType, valType, spec);
        }
        if (AdaptiveRecord.class.isAssignableFrom(rawType)) {
            return buildAdaptiveRecordLoader(key, spec);
        }
        return buildPojoLoader(key, spec);
    }

    private ParamValueLoader buildArrayLoader(
            final ParamKey key,
            final Type elementType,
            final BeanSpec targetSpec
    ) {
        final CollectionLoader collectionLoader = new CollectionLoader(key, ArrayList.class, elementType, targetSpec, injector, this);
        return new ParamValueLoader.JsonBodySupported() {
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

    private ParamValueLoader buildAdaptiveRecordLoader(
            final ParamKey key, final BeanSpec spec) {
        return new AdaptiveRecordLoader(key, spec, this);
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

    public static void clearParamTree() {
        PARAM_TREE.remove();
    }

    private ParamValueLoader buildPojoLoader(final ParamKey key, final BeanSpec spec) {
        return new PojoLoader(key, spec, this);
    }

    private ParamValueLoader findLoader(ParamKey paramKey, BeanSpec spec) {
        if (provided(spec, injector)) {
            return ProvidedValueLoader.get(spec, injector);
        }
        String name = spec.name();
        ParamKey key = paramKey.child(name);

        ParamValueLoader loader = binder(spec, key.toString());
        if (null != loader) {
            return loader;
        }

        Class fieldType = spec.rawType();
        StringValueResolver resolver = resolverManager.resolver(fieldType, spec);
        if (null != resolver) {
            DefaultValue def = spec.getAnnotation(DefaultValue.class);
            return new StringValueResolverValueLoader(key, def, spec);
        }

        return buildLoader(key, spec);
    }

    FieldLoader fieldLoader(ParamKey key, Field field, BeanSpec fieldSpec) {
        if (field.isAnnotationPresent(ManyToOne.class) || field.isAnnotationPresent(OneToOne.class)) {
            final FindBy findBy = new FindBy();
            Map map = new HashMap<>();
            map.put("byId", true);
            findBy.init(map, BeanSpec.of(field, Act.injector()));
            Lang.TypeConverter<Object, Object> converter = new Lang.TypeConverter<Object, Object>() {
                @Override
                public Object convert(Object s) {
                    findBy.setOnetimeValue(S.string(s));
                    return findBy.get();
                }
            };
            return new FieldLoader(field, findLoader(key, fieldSpec), findLoader(key, BeanSpec.of(String.class, new Annotation[]{}, fieldSpec.name(), fieldSpec.injector())), converter);
        }
        return new FieldLoader(field, findLoader(key, fieldSpec));
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
            boolean useJsonDecorator,
            boolean useValidationDecorator
    ) {
        final ParamValueLoader jsonDecorated = useJsonDecorator && loader.supportJsonDecorator() ? new JsonParamValueLoader(loader, spec, injector) : loader;
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

                @Override
                public boolean supportJsonDecorator() {
                    return false;
                }

                @Override
                public boolean supportScopeCaching() {
                    return jsonDecorated.supportScopeCaching();
                }

                @Override
                public boolean requireRuntimeTypeInfo() {
                    return false;
                }

                @Override
                public ParamValueLoader wrapWithRuntimeType(Class<?> type) {
                    throw E.unsupport();
                }
            };

        }

        return new ScopedParamValueLoader(validationDecorated, spec, scopeCacheSupport(spec));
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

    private static ScopeCacheSupport scopeCacheSupport(BeanSpec spec) {
        if (spec.hasAnnotation(RequestScoped.class) || spec.hasAnnotation(org.osgl.inject.annotation.RequestScoped.class)) {
            return RequestScope.INSTANCE;
        } else if (sessionScoped(spec)) {
            return SessionScope.INSTANCE;
        } else if (spec.hasAnnotation(Dependent.class) || spec.hasAnnotation(New.class)) {
            return DependentScope.INSTANCE;
        }
        // Default to Request Scope
        return RequestScope.INSTANCE;
    }

    static boolean sessionScoped(BeanSpec spec) {
        return spec.hasAnnotation(SessionScoped.class)
                || spec.hasAnnotation(org.osgl.inject.annotation.SessionScoped.class);
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

    public static boolean isThrowable(BeanSpec beanSpec) {
        return beanSpec.isInstanceOf(Throwable.class);
    }

    public static boolean provided(BeanSpec beanSpec, DependencyInjector<?> injector) {
        if (isThrowable(beanSpec)) {
            return true;
        }
        GenieInjector genieInjector = $.cast(injector);
        return genieInjector.isProvided(beanSpec);
    }

    public static boolean providedButNotDbBind(BeanSpec beanSpec, DependencyInjector<?> injector) {
        if (!provided(beanSpec, injector)) {
            return false;
        }
        Annotation[] aa = beanSpec.allAnnotations();
        return !hasDbBind(aa);
    }

    private static final String DB_BIND_CNAME = DbBind.class.getName();

    // DbBind is special: it's class loader is AppClassLoader
    public static boolean hasDbBind(Annotation[] annotations) {
        final String name = DB_BIND_CNAME;
        for (Annotation a : annotations) {
            if (a.annotationType().getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

}
