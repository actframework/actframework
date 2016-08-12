package act.inject.param;

import act.app.App;
import act.app.AppServiceBase;
import act.app.data.BinderManager;
import act.app.data.StringValueResolverManager;
import act.inject.ActProviders;
import act.inject.Context;
import act.inject.DependencyInjector;
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
import org.osgl.mvc.util.Binder;
import org.osgl.util.C;
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
public class ParamValueLoaderManager extends AppServiceBase<ParamValueLoaderManager> {

    private static final ParamValueLoader[] NULL = new ParamValueLoader[0];
    private static final ThreadLocal<ParamTree> PARAM_TREE = new ThreadLocal<>();
    private StringValueResolverManager resolverManager;
    private BinderManager binderManager;
    private ConcurrentMap<Method, ParamValueLoader[]> methodRegistry = new ConcurrentHashMap<>();
    private ConcurrentMap<Class, ParamValueLoader> classRegistry = new ConcurrentHashMap<>();
    private ConcurrentMap<$.T2<Type, Annotation[]>, ParamValueLoader> paramRegistry = new ConcurrentHashMap<>();

    public ParamValueLoaderManager(App app) {
        super(app);
        resolverManager = app.resolverManager();
        binderManager = app.binderManager();
    }

    @Override
    protected void releaseResources() {
        DestroyableBase.Util.tryDestroyAll(classRegistry.values(), ApplicationScoped.class);
        DestroyableBase.Util.tryDestroyAll(paramRegistry.values(), ApplicationScoped.class);
    }

    public Object loadHostBean(Class beanClass, ActContext ctx, DependencyInjector<?> injector) {
        ParamValueLoader loader = classRegistry.get(beanClass);
        if (null == loader) {
            loader = findBeanLoader(beanClass, injector);
            classRegistry.putIfAbsent(beanClass, loader);
        }
        return loader.load(null, ctx, false);
    }

    public Object[] loadMethodParams(Method method, ActContext ctx, DependencyInjector<?> injector) {
        try {
            ParamValueLoader[] loaders = methodRegistry.get(method);
            if (null == loaders) {
                loaders = findLoaders(method, injector);
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

    private <T> ParamValueLoader findBeanLoader(Class<T> beanClass, DependencyInjector<?> injector) {
        final Provider<T> provider = injector.getProvider(beanClass);
        final Map<Field, ParamValueLoader> loaders = new HashMap<>();
        for (Field field: $.fieldsOf(beanClass, true)) {
            Type type = field.getGenericType();
            Annotation[] annotations = field.getAnnotations();
            BeanSpec spec = BeanSpec.of(type, annotations, field.getName(), injector);
            ParamValueLoader loader = findLoader(spec, type, annotations, injector);
            loaders.put(field, loader);
        }
        ParamValueLoader loader = new ParamValueLoader() {
            @Override
            public Object load(Object bean, ActContext context, boolean noDefaultValue) {
                if (null == bean) {
                    bean = provider.get();
                }
                try {
                    for (Map.Entry<Field, ParamValueLoader> entry : loaders.entrySet()) {
                        entry.getKey().set(bean, entry.getValue().load(null, context, noDefaultValue));
                    }
                } catch (IllegalAccessException e) {
                    throw new InjectException(e);
                }
                return bean;
            }
        };
        return decorate(loader, BeanSpec.of(beanClass, injector), beanClass.getDeclaredAnnotations());
    }

    private ParamValueLoader[] findLoaders(Method method, DependencyInjector<?> injector) {
        Type[] types = method.getGenericParameterTypes();
        int sz = types.length;
        if (0 == sz) {
            return NULL;
        }
        ParamValueLoader[] loaders = new ParamValueLoader[sz];
        Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < sz; ++i) {
            BeanSpec spec = BeanSpec.of(types[i], annotations[i], injector);
            ParamValueLoader loader = paramRegistry.get(spec);
            if (null == loader) {
                loader = findLoader(spec, types[i], annotations[i], injector);
                // Cannot use spec as the key here because
                // spec does not compare Scoped annotation
                paramRegistry.putIfAbsent($.T2(types[i], annotations[i]), loader);
            }
            loaders[i] = loader;
        }
        return loaders;
    }

    private ParamValueLoader findLoader(
            BeanSpec spec,
            Type type,
            Annotation[] annotations,
            DependencyInjector<?> injector
    ) {
        Class rawType = BeanSpec.rawTypeOf(type);
        if (ActProviders.isProvided(rawType)
                || null != filter(annotations, Provided.class)
                || null != filter(annotations, Context.class)
                || null != filter(annotations, Singleton.class)
                || null != filter(annotations, ApplicationScoped.class)) {
            return ProvidedValueLoader.get(rawType, injector);
        }
        ParamValueLoader loader;
        Named named = filter(annotations, Named.class);
        String name = null != named ? named.value() : spec.name();
        Bind bind = filter(annotations, Bind.class);
        if (null != bind) {
            Binder binder = injector.get(bind.value());
            String model = bind.model();
            if (S.blank(model)) {
                model = name;
            }
            loader = new BoundedValueLoader(binder, model);
        } else {
            Binder binder = binderManager.binder(rawType);
            if (null != binder) {
                loader = new BoundedValueLoader(binder, name);
            } else {
                Param param = filter(annotations, Param.class);
                StringValueResolver resolver = null;
                if (null != param) {
                    String paramName = param.value();
                    if (S.notBlank(paramName)) {
                        name = paramName;
                    }
                    Class<? extends StringValueResolver> resolverClass = param.resolverClass();
                    if (Param.DEFAULT_RESOLVER.class != resolverClass) {
                        resolver = injector.get(resolverClass);
                    }
                }
                if (null == resolver) {
                    resolver = resolverManager.resolver(rawType);
                }
                loader = (null != resolver) ? new StringValueResolverValueLoader(ParamKey.of(name), resolver, param, rawType) : buildLoader(ParamKey.of(name), type, injector);
            }
        }
        return decorate(loader, spec, annotations);
    }

    ParamValueLoader buildLoader(final ParamKey key, final Type type, DependencyInjector<?> injector) {
        Class rawType = BeanSpec.rawTypeOf(type);
        if (rawType.isArray()) {
            return buildArrayLoader(key, rawType.getComponentType(), injector);
        }
        if (Collection.class.isAssignableFrom(rawType)) {
            Type elementType = ((ParameterizedType) type).getActualTypeArguments()[0];
            return buildCollectionLoader(key, rawType, elementType, injector);
        }
        if (Map.class.isAssignableFrom(rawType)) {
            Type[] typeParams = ((ParameterizedType) type).getActualTypeArguments();
            Type keyType = typeParams[0];
            Type valType = typeParams[1];
            return buildMapLoader(key, rawType, keyType, valType, injector);
        }
        return buildPojoLoader(key, rawType, injector);
    }

    private ParamValueLoader buildArrayLoader(
            final ParamKey key,
            final Type elementType,
            DependencyInjector<?> injector
    ) {
        final CollectionLoader collectionLoader = new CollectionLoader(key, ArrayList.class, elementType, injector, this);
        return new ParamValueLoader() {
            @Override
            public Object load(Object bean, ActContext context, boolean noDefaultValue) {
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
            DependencyInjector<?> injector
    ) {
        return new CollectionLoader(key, collectionClass, elementType, injector, this);
    }

    private ParamValueLoader buildMapLoader(
            final ParamKey key,
            final Class<? extends Map> mapClass,
            final Type keyType,
            final Type valType,
            DependencyInjector<?> injector
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

    private ParamValueLoader buildPojoLoader(final ParamKey key, final Class type, final DependencyInjector<?> injector) {
        final List<FieldLoader> fieldLoaders = fieldLoaders(key, type, injector);
        return new ParamValueLoader() {
            @Override
            public Object load(Object bean, ActContext context, boolean noDefaultValue) {
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

    private ParamValueLoader findLoader(ParamKey paramKey, Field field, DependencyInjector<?> injector) {
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

        return buildLoader(key, field.getGenericType(), injector);
    }

    private List<FieldLoader> fieldLoaders(ParamKey key, Class type, DependencyInjector<?> injector) {
        Class<?> current = type;
        List<FieldLoader> fieldLoaders = C.newList();
        while (null != current && !current.equals(Object.class)) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                fieldLoaders.add(fieldLoader(key, field, injector));
            }
            current = current.getSuperclass();
        }
        return fieldLoaders;
    }

    private FieldLoader fieldLoader(ParamKey key, Field field, DependencyInjector<?> injector) {
        return new FieldLoader(field, findLoader(key, field, injector));
    }

    private static <T extends Annotation> T filter(Annotation[] annotations, Class<T> annoType) {
        for (Annotation annotation : annotations) {
            if (annoType == annotation.annotationType()) {
                return (T) annotation;
            }
        }
        return null;
    }

    private static ParamValueLoader decorate(
            ParamValueLoader loader,
            BeanSpec paramSpec,
            Annotation[] annotations
    ) {
        return new ScopedParamValueLoader(loader, paramSpec, scopeCacheSupport(annotations));
    }

    private static ScopeCacheSupport scopeCacheSupport(Annotation[] annotations) {
        if (null != filter(annotations, RequestScoped.class) ||
                null != filter(annotations, org.osgl.inject.annotation.RequestScoped.class)) {
            return RequestScope.INSTANCE;
        } else if (null != filter(annotations, SessionScoped.class) ||
                null != filter(annotations, org.osgl.inject.annotation.SessionScoped.class)) {
            return SessionScope.INSTANCE;
        } else if (null != filter(annotations, Dependent.class) ||
                null != filter(annotations, New.class)) {
            return DependentScope.INSTANCE;
        }
        // Default to Request Scope
        return RequestScope.INSTANCE;
    }

}
