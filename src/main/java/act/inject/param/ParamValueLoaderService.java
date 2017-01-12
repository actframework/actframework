package act.inject.param;

import act.Act;
import act.app.ActionContext;
import act.app.App;
import act.app.data.BinderManager;
import act.app.data.StringValueResolverManager;
import act.controller.ActionMethodParamAnnotationHandler;
import act.inject.ActProviders;
import act.inject.Context;
import act.inject.DependencyInjector;
import act.inject.SessionVariable;
import act.inject.genie.DependentScope;
import act.inject.genie.GenieInjector;
import act.inject.genie.RequestScope;
import act.inject.genie.SessionScope;
import act.util.ActContext;
import act.util.DestroyableBase;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.InjectException;
import org.osgl.inject.annotation.Provided;
import org.osgl.inject.util.AnnotationUtil;
import org.osgl.inject.util.ArrayLoader;
import org.osgl.mvc.annotation.Bind;
import org.osgl.mvc.annotation.Param;
import org.osgl.mvc.result.Result;
import org.osgl.mvc.util.Binder;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;
import org.osgl.util.StringValueResolver;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.New;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manage {@link ParamValueLoader} grouped by Method
 */
public abstract class ParamValueLoaderService extends DestroyableBase {

    private static final ParamValueLoader[] DUMB = new ParamValueLoader[0];
    private static final ThreadLocal<ParamTree> PARAM_TREE = new ThreadLocal<ParamTree>();
    private static final ParamValueLoader RESULT_LOADER = new ParamValueLoader() {
        @Override
        public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
            return context.attribute(ActionContext.ATTR_RESULT);
        }
    };
    private static final ParamValueLoader EXCEPTION_LOADED = new ParamValueLoader() {
        @Override
        public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
            return context.attribute(ActionContext.ATTR_EXCEPTION);
        }
    };
    // contains field names that should be waived when looking for value loader
    private static final Set<String> fieldBlackList = new HashSet<>();

    StringValueResolverManager resolverManager;
    BinderManager binderManager;
    DependencyInjector<?> injector;
    ConcurrentMap<Method, ParamValueLoader[]> methodRegistry = new ConcurrentHashMap<Method, ParamValueLoader[]>();
    ConcurrentMap<Class, Map<Field, ParamValueLoader>> fieldRegistry = new ConcurrentHashMap<Class, Map<Field, ParamValueLoader>>();
    ConcurrentMap<Class, ParamValueLoader> classRegistry = new ConcurrentHashMap<Class, ParamValueLoader>();
    private ConcurrentMap<$.T2<Type, Annotation[]>, ParamValueLoader> paramRegistry = new ConcurrentHashMap<$.T2<Type, Annotation[]>, ParamValueLoader>();
    private ConcurrentMap<BeanSpec, Map<Class<? extends Annotation>, ActionMethodParamAnnotationHandler>> annoHandlers = new ConcurrentHashMap<BeanSpec, Map<Class<? extends Annotation>, ActionMethodParamAnnotationHandler>>();
    private Map<Class<? extends Annotation>, ActionMethodParamAnnotationHandler> allAnnotationHandlers;

    public ParamValueLoaderService(App app) {
        resolverManager = app.resolverManager();
        binderManager = app.binderManager();
        injector = app.injector();
        allAnnotationHandlers = new HashMap<Class<? extends Annotation>, ActionMethodParamAnnotationHandler>();
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
            loader = findBeanLoader(beanClass);
            classRegistry.putIfAbsent(beanClass, loader);
        }
        return loader.load(null, ctx, false);
    }

    public Object[] loadMethodParams(Method method, ActContext ctx) {
        try {
            ParamValueLoader[] loaders = methodRegistry.get(method);
            if (null == loaders) {
                loaders = findMethodParamLoaders(method);
                methodRegistry.putIfAbsent(method, loaders);
            }
            int sz = loaders.length;
            Object[] params = new Object[sz];
            for (int i = 0; i < sz; ++i) {
                params[i] = loaders[i].load(null, ctx, false);
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
                        // preset the render args for fields
                        if (null != fieldValue) {
                            context.renderArg(field.getName(), fieldValue);
                        }
                    }
                } catch (IllegalAccessException e) {
                    throw new InjectException(e);
                }
                return bean;
            }
        };
        return decorate(loader, BeanSpec.of(beanClass, injector), beanClass.getDeclaredAnnotations(), false);
    }

    private boolean shouldWaive(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isStatic(modifiers)
                || Modifier.isTransient(modifiers)
                || field.isAnnotationPresent(NoBind.class)
                || fieldBlackList.contains(field.getName())
                || Object.class.equals(field.getDeclaringClass());
    }

    private <T> Map<Field, ParamValueLoader> fieldLoaders(Class<T> beanClass) {
        Map<Field, ParamValueLoader> fieldLoaders = fieldRegistry.get(beanClass);
        if (null == fieldLoaders) {
            fieldLoaders = new HashMap<Field, ParamValueLoader>();
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
                    fieldLoaders.put(field, loader);
                }
            }
            fieldRegistry.putIfAbsent(beanClass, fieldLoaders);
        }
        return fieldLoaders;
    }

    protected ParamValueLoader[] findMethodParamLoaders(Method method) {
        Type[] types = method.getGenericParameterTypes();
        int sz = types.length;
        if (0 == sz) {
            return DUMB;
        }
        ParamValueLoader[] loaders = new ParamValueLoader[sz];
        Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < sz; ++i) {
            String name = paramName(i);
            BeanSpec spec = BeanSpec.of(types[i], annotations[i], name, injector);
            loaders[i] = paramValueLoaderOf(spec);
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
            loader = findLoader(spec, type, annotations, bindName);
            // Cannot use spec as the key here because
            // spec does not compare Scoped annotation
            paramRegistry.putIfAbsent(key, loader);
        }
        return loader;
    }

    protected abstract ParamValueLoader findContextSpecificLoader(
            String bindName,
            Class rawType,
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
        return decorate(loader, spec, annotations, supportJsonDecorator());
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
            return new StringValueResolverValueLoader(key, resolver, null, fieldType);
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
            boolean useJsonDecorator
    ) {
        final ParamValueLoader jsonDecorated = useJsonDecorator ? new JsonParamValueLoader(loader, spec, injector) : loader;
        final Map<Class<? extends Annotation>, ActionMethodParamAnnotationHandler> handlers = paramAnnoHandlers(spec);
        final ParamValueLoader annoHandlerDecorated = new ParamValueLoader() {
            @Override
            public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
                Object object = jsonDecorated.load(bean, context, noDefaultValue);
                if (!(context instanceof ActionContext) || null == handlers) {
                    return object;
                }
                for (Map.Entry<Class<? extends Annotation>, ActionMethodParamAnnotationHandler> entry : handlers.entrySet()) {
                    Annotation ann = filter(annotations, entry.getKey());
                    entry.getValue().handle(spec.name(), object, ann, (ActionContext) context);
                }
                return object;
            }
        };
        return new ScopedParamValueLoader(annoHandlerDecorated, spec, scopeCacheSupport(annotations));
    }

    private Map<Class<? extends Annotation>, ActionMethodParamAnnotationHandler> paramAnnoHandlers(BeanSpec spec) {
        Map<Class<? extends Annotation>, ActionMethodParamAnnotationHandler> handlers = annoHandlers.get(spec);
        if (null != handlers) {
            return handlers;
        }
        handlers = new HashMap<Class<? extends Annotation>, ActionMethodParamAnnotationHandler>();
        for (Annotation annotation : spec.allAnnotations()) {
            Class<? extends Annotation> c = annotation.annotationType();
            ActionMethodParamAnnotationHandler h = allAnnotationHandlers.get(c);
            if (null != h) {
                handlers.put(c, h);
            }
        }
        annoHandlers.putIfAbsent(spec, handlers);
        return handlers;
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

    public static String tryFindBindName(Annotation[] annotations, String defVal) {
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
        Class rawType = beanSpec.rawType();
        GenieInjector genieInjector = $.cast(injector);
        return (ActProviders.isProvided(rawType)
                || null != beanSpec.getAnnotation(Inject.class)
                || null != beanSpec.getAnnotation(Provided.class)
                || null != beanSpec.getAnnotation(Context.class)
                || null != beanSpec.getAnnotation(Singleton.class)
                || null != beanSpec.getAnnotation(ApplicationScoped.class)
                || genieInjector.subjectToInject(beanSpec)
        );
    }

    public static boolean noBindOrProvided(BeanSpec beanSpec, DependencyInjector<?> injector) {
        return null != beanSpec.getAnnotation(NoBind.class) || provided(beanSpec, injector);
    }

}
