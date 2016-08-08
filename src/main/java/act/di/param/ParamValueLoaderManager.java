package act.di.param;

import act.app.App;
import act.app.AppServiceBase;
import act.app.data.StringValueResolverManager;
import act.di.ActProviders;
import act.di.DependencyInjector;
import act.util.ActContext;
import act.util.DestroyableBase;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.InjectException;
import org.osgl.inject.annotation.Provided;
import org.osgl.mvc.annotation.Bind;
import org.osgl.mvc.annotation.Param;
import org.osgl.mvc.util.Binder;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;
import org.osgl.util.StringValueResolver;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manage {@link ParamValueLoader} grouped by Method
 */
public class ParamValueLoaderManager extends AppServiceBase<ParamValueLoaderManager> {

    private static final ParamValueLoader[] NULL = new ParamValueLoader[0];
    private static final ThreadLocal<ParamTree> PARAM_TREE = new ThreadLocal<>();
    private StringValueResolverManager resolverManager;
    private ConcurrentMap<Method, ParamValueLoader[]> registry = new ConcurrentHashMap<>();

    public ParamValueLoaderManager(App app) {
        super(app);
        resolverManager = app.resolverManager();
    }

    @Override
    protected void releaseResources() {
        DestroyableBase.Util.tryDestroyAll(registry.values(), ApplicationScoped.class);
    }

    public Object[] load(Method method, ActContext ctx, DependencyInjector<?> injector) {
        ParamValueLoader[] loaders = registry.get(method);
        if (null == loaders) {
            loaders = findLoaders(method, injector);
            registry.putIfAbsent(method, loaders);
        }
        int sz = loaders.length;
        Object[] params = new Object[sz];
        for (int i = 0; i < sz; ++i) {
            params[i] = loaders[i].load(ctx);
        }
        return params;
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
            loaders[i] = findLoader(types[i], annotations[i], injector);
        }
        return loaders;
    }

    private ParamValueLoader findLoader(Type type, Annotation[] annotations, DependencyInjector<?> injector) {
        Class rawType = BeanSpec.rawTypeOf(type);
        if (ActProviders.isProvided(rawType) || null != filter(annotations, Provided.class)) {
            return ProvidedValueLoader.get(rawType, injector);
        }
        String name = filter(annotations, Named.class).value();
        Bind bind = filter(annotations, Bind.class);
        if (null != bind) {
            Binder binder = injector.get(bind.value());
            String model = bind.model();
            if (S.blank(model)) {
                model = name;
            }
            return new BoundedValueLoader(binder, model);
        }
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
        if (null != resolver) {
            return new StringValueResolverValueLoader(ParamKey.of(name), resolver, param, rawType);
        }
        return buildLoader(ParamKey.of(name), type, injector);
    }

    private ParamValueLoader buildLoader(final ParamKey key, final Type type, DependencyInjector<?> injector) {
        System.out.println("dd");
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
        List cache = new ArrayList();
        throw E.tbd();
    }

    private ParamValueLoader buildCollectionLoader(
            final ParamKey key,
            final Class<? extends Collection> collectionClass,
            final Type elementType,
            DependencyInjector<?> injector
    ) {
        return new CollectionLoader(key, injector.get(collectionClass), elementType, injector);
    }

    private ParamValueLoader buildMapLoader(
            final ParamKey key,
            final Class mapClass,
            final Type keyType,
            final Type valType,
            DependencyInjector<?> injector
    ) {
        throw E.tbd();
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

    private ParamValueLoader buildPojoLoader(final ParamKey key, final Class type, DependencyInjector<?> injector) {
        final List<FieldLoader> fieldLoaders = fieldLoaders(key, type, injector);
        try {
            final Constructor constructor = type.getDeclaredConstructor();
            if (null == constructor) {
                throw new InjectException("cannot instantiate %s: %s", type, "no default constructor found");
            }
            constructor.setAccessible(true);
            return new ParamValueLoader() {
                @Override
                public Object load(ActContext context) {
                    try {
                        Object bean = constructor.newInstance();
                        for (FieldLoader fl : fieldLoaders) {
                            fl.applyTo(bean, context);
                        }
                        return bean;
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new InjectException(e, "cannot instantiate %s", type);
                    }
                }
            };
        } catch (NoSuchMethodException e) {
            throw new InjectException("Cannot instantiate %s", type);
        }
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

    private <T extends Annotation> T filter(Annotation[] annotations, Class<T> annoType) {
        for (Annotation annotation : annotations) {
            if (annoType == annotation.annotationType()) {
                return (T) annotation;
            }
        }
        return null;
    }

}
