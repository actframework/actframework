package act.inject.param;

import act.Act;
import act.app.ActionContext;
import act.app.App;
import act.app.data.BinderManager;
import act.app.data.StringValueResolverManager;
import act.controller.ActionMethodParamAnnotationHandler;
import act.inject.*;
import act.inject.genie.DependentScope;
import act.inject.genie.RequestScope;
import act.inject.genie.SessionScope;
import act.util.ActContext;
import act.util.DestroyableBase;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.InjectException;
import org.osgl.inject.annotation.Provided;
import org.osgl.inject.util.ArrayLoader;
import org.osgl.mvc.annotation.Bind;
import org.osgl.mvc.annotation.Param;
import org.osgl.mvc.result.Result;
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
    private static final ThreadLocal<ParamTree> PARAM_TREE = new ThreadLocal<>();
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

    StringValueResolverManager resolverManager;
    BinderManager binderManager;
    DependencyInjector<?> injector;
    ConcurrentMap<Method, ParamValueLoader[]> methodRegistry = new ConcurrentHashMap<>();
    ConcurrentMap<Class, ParamValueLoader> classRegistry = new ConcurrentHashMap<>();
    private ConcurrentMap<$.T2<Type, Annotation[]>, ParamValueLoader> paramRegistry = new ConcurrentHashMap<>();
    private ConcurrentMap<BeanSpec, Map<Class<? extends Annotation>, ActionMethodParamAnnotationHandler>> annoHandlers = new ConcurrentHashMap<>();
    private Map<Class<? extends Annotation>, ActionMethodParamAnnotationHandler> allAnnotationHandlers;

    public ParamValueLoaderService(App app) {
        resolverManager = app.resolverManager();
        binderManager = app.binderManager();
        injector = app.injector();
        allAnnotationHandlers = new HashMap<>();
        List<ActionMethodParamAnnotationHandler> list = Act.pluginManager().pluginList(ActionMethodParamAnnotationHandler.class);
        for (ActionMethodParamAnnotationHandler h : list) {
            Set<Class<? extends Annotation>> set = h.listenTo();
            for (Class<? extends Annotation> c: set) {
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
        final Map<Field, ParamValueLoader> loaders = new HashMap<>();
        for (Field field: $.fieldsOf(beanClass, true)) {
            Type type = field.getGenericType();
            Annotation[] annotations = field.getAnnotations();
            BeanSpec spec = BeanSpec.of(type, annotations, field.getName(), injector);
            ParamValueLoader loader = findLoader(spec, type, annotations);
            if (null != loader && !(loader instanceof ProvidedValueLoader)) {
                loaders.put(field, loader);
            }
        }
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
                        if (null == field.get(bean)) {
                            field.set(bean, loader.load(null, context, noDefaultValue));
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
            ParamValueLoader loader = paramRegistry.get($.T2(types[i], annotations[i]));
            if (null == loader) {
                if (Result.class.isAssignableFrom(spec.rawType())) {
                    loader = RESULT_LOADER;
                } else if (Exception.class.isAssignableFrom(spec.rawType())) {
                    loader = EXCEPTION_LOADED;
                } else {
                    loader = findLoader(spec, types[i], annotations[i]);
                }
                // Cannot use spec as the key here because
                // spec does not compare Scoped annotation
                paramRegistry.putIfAbsent($.T2(types[i], annotations[i]), loader);
            }
            loaders[i] = loader;
        }
        return loaders;
    }

    protected abstract ParamValueLoader findContextSpecificLoader(
            String bindName,
            Class rawType,
            BeanSpec spec,
            Type type,
            Annotation[] annotations
    );

    protected String paramName(int i) {
        return null;
    }

    protected boolean supportJsonDecorator() {
        return false;
    }

    private ParamValueLoader findLoader(
            BeanSpec spec,
            Type type,
            Annotation[] annotations
    ) {
        Class rawType = spec.rawType();
        if (provided(spec)) {
            return ProvidedValueLoader.get(rawType, injector);
        }
        if (null != filter(annotations, NoBind.class)) {
            return null;
        }
        String bindName = bindName(annotations, spec.name());
        ParamValueLoader loader = findContextSpecificLoader(bindName, rawType, spec, type, annotations);
        return decorate(loader, spec, annotations, supportJsonDecorator());
    }

    ParamValueLoader buildLoader(final ParamKey key, final Type type) {
        Class rawType = BeanSpec.rawTypeOf(type);
        if (rawType.isArray()) {
            return buildArrayLoader(key, rawType.getComponentType());
        }
        if (Collection.class.isAssignableFrom(rawType)) {
            Type elementType = Object.class;
            if (type instanceof ParameterizedType) {
                elementType = ((ParameterizedType) type).getActualTypeArguments()[0];
            }
            return buildCollectionLoader(key, rawType, elementType);
        }
        if (Map.class.isAssignableFrom(rawType)) {
            Type keyType = Object.class;
            Type valType = Object.class;
            if (type instanceof ParameterizedType) {
                Type[] typeParams = ((ParameterizedType) type).getActualTypeArguments();
                keyType = typeParams[0];
                valType = typeParams[1];
            }
            return buildMapLoader(key, rawType, keyType, valType);
        }
        return buildPojoLoader(key, rawType);
    }

    private ParamValueLoader buildArrayLoader(
            final ParamKey key,
            final Type elementType
    ) {
        final CollectionLoader collectionLoader = new CollectionLoader(key, ArrayList.class, elementType, injector, this);
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
            final Type elementType
    ) {
        return new CollectionLoader(key, collectionClass, elementType, injector, this);
    }

    private ParamValueLoader buildMapLoader(
            final ParamKey key,
            final Class<? extends Map> mapClass,
            final Type keyType,
            final Type valType
    ) {
        return new MapLoader(key, mapClass, keyType, valType, injector, this);
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
        Class fieldType = field.getType();
        Annotation[] annotations = field.getDeclaredAnnotations();
        if (ActProviders.isProvided(fieldType) || null != filter(annotations, Inject.class)) {
            return ProvidedValueLoader.get(fieldType, injector);
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

        StringValueResolver resolver = resolverManager.resolver(fieldType);
        if (null != resolver) {
            return new StringValueResolverValueLoader(key, resolver, null, fieldType);
        }

        return buildLoader(key, field.getGenericType());
    }

    private List<FieldLoader> fieldLoaders(ParamKey key, Class type) {
        Class<?> current = type;
        List<FieldLoader> fieldLoaders = C.newList();
        while (null != current && !current.equals(Object.class)) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
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
        handlers = new HashMap<>();
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

    public static String bindName(Annotation[] annotations, String defVal) {
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
        throw new IllegalStateException("Cannot find bind name");
    }

    public static String bindName(BeanSpec beanSpec) {
        return bindName(beanSpec.allAnnotations(), beanSpec.name());
    }

    public static boolean provided(BeanSpec beanSpec) {
        Class rawType = beanSpec.rawType();
        return (ActProviders.isProvided(rawType)
                || null != beanSpec.getAnnotation(Inject.class)
                || null != beanSpec.getAnnotation(Provided.class)
                || null != beanSpec.getAnnotation(Context.class)
                || null != beanSpec.getAnnotation(Singleton.class)
                || null != beanSpec.getAnnotation(ApplicationScoped.class));
    }
}
