package act.di.feather;

import act.app.App;
import act.di.DependencyInjector;
import org.osgl.util.C;
import org.osgl.util.E;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Qualifier;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Feather {
//
//    private App app;
//    private DependencyInjector di;
//    private final ConcurrentMap<Key, Key> keys = new ConcurrentHashMap<>();
//    private final Map<Key, Boolean> fieldInjectFlags = new ConcurrentHashMap<Key, Boolean>();
//    private final Map<Class, Object[][]> injectFields = new ConcurrentHashMap<Class, Object[][]>(0);
//
//    /**
//     * Constructs Feather with configuration modules
//     */
//    public static Feather with(Object... modules) {
//        return new Feather(C.list(modules));
//    }
//
//    /**
//     * Constructs Feather with configuration modules
//     */
//    public static Feather with(Iterable<?> modules) {
//        return new Feather(modules);
//    }
//
//    <T> void registerProvider(Class<T> c, Provider<T> p) {
//        Key key = Key.of(c, p);
//        E.illegalStateIf(keys.containsKey(key), "Provider already registered with key[%s]", c.getName());
//        keys.put(key, key);
//    }
//
//    private Feather(Iterable<?> modules) {
//        final Key<Feather> featherKey = Key.of(Feather.class, new Provider<Feather>() {
//            @Override
//            public Feather get() {
//                return Feather.this;
//            }
//        });
//        keys.put(featherKey, featherKey);
//        for (final Object module : modules) {
//            boolean moduleIsClass = module instanceof Class;
//            Class theClass = moduleIsClass ? (Class) module : module.getClass();
//            for (Method providerMethod : providers(theClass)) {
//                registerProviderMethod(module, providerMethod);
//            }
//        }
//    }
//
//    public Feather dependencyInjector(DependencyInjector<FeatherInjector> di) {
//        this.di = di;
//        this.app = di.app();
//        return this;
//    }
//
//    /**
//     * @return an instance of type
//     */
//    public <T> T instance(Class<T> type) {
//        return instance(Key.of(type), null);
//    }
//
//    /**
//     * @return instance specified by key (type and qualifiers)
//     */
//    public <T> T instance(Key<T> key) {
//        return instance(key, null);
//    }
//
//    private <T> T instance(Key<T> key, Type[] typeParameters) {
//        T t = provider(key, null).get();
//        if (null != di) di.fireInjectedEvent(t, typeParameters);
//        return t;
//    }
//
//    /**
//     * @return provider of type
//     */
//    public <T> Provider<T> provider(Class<T> type) {
//        return provider(Key.of(type), null);
//    }
//
//    /**
//     * @return provider of key (type, qualifiers)
//     */
//    public <T> Provider<T> provider(Key<T> key) {
//        return provider(key, null);
//    }
//
//    /**
//     * Injects fields to the target object
//     */
//    public void injectFields(Object target) {
//        if (!injectFields.containsKey(target.getClass())) {
//            injectFields.put(target.getClass(), injectFields(target.getClass()));
//        }
//        for (Object[] f : injectFields.get(target.getClass())) {
//            Field field = (Field) f[0];
//            Key key = (Key) f[2];
//            Type[] typeParameters = (Type[]) f[3];
//            try {
//                field.set(target, (Boolean) f[1] ? provider(key) : instance(key, typeParameters));
//            } catch (Exception e) {
//                throw new FeatherException(String.format("Can't inject field %s in %s", field.getName(), target.getClass().getName()));
//            }
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    private <T> Provider<T> provider(final Key<T> key, Set<Key> chain) {
//        if (!providers.containsKey(key)) {
//            if (fieldInjectionDetected(key)) {
//                fieldInjectFlags.put(key, true);
//            }
//            final Constructor constructor = constructor(key);
//            final Provider<?>[] paramProviders = paramProviders(key, constructor.getParameterTypes(), constructor.getGenericParameterTypes(), constructor.getParameterAnnotations(), chain);
//            providers.put(key, singletonProvider(key, key.type.getAnnotation(Singleton.class), new Provider() {
//                        @Override
//                        public Object get() {
//                            try {
//                                Object o = constructor.newInstance(params(paramProviders));
//                                if (fieldInjectFlags.containsKey(key)) {
//                                    injectFields(o);
//                                }
//                                return o;
//                            } catch (Exception e) {
//                                throw new FeatherException(String.format("Can't instantiate %s", key.toString()), e);
//                            }
//                        }
//                    })
//            );
//        }
//        return (Provider<T>) providers.get(key);
//    }
//
//    private void registerProviderMethod(final Object module, final Method m) {
//        final Key key = Key.of(m.getReturnType(), qualifiers(m.getAnnotations()), typeParameters(m.getGenericReturnType()));
//        if (keys.containsKey(key)) {
//            throw new FeatherException(String.format("%s has multiple providers, module %s", key.toString(), module.getClass()));
//        }
//        Singleton singleton = m.getAnnotation(Singleton.class) != null ? m.getAnnotation(Singleton.class) : m.getReturnType().getAnnotation(Singleton.class);
//        final Provider<?>[] paramProviders = paramProviders(
//                key,
//                m.getParameterTypes(),
//                m.getGenericParameterTypes(),
//                m.getParameterAnnotations(),
//                Collections.singleton(key)
//        );
//        providers.put(key, singletonProvider(key, singleton, new Provider() {
//                    @Override
//                    public Object get() {
//                        try {
//                            return m.invoke(module, params(paramProviders));
//                        } catch (Exception e) {
//                            throw new FeatherException(String.format("Can't instantiate %s with provider", key.toString()), e);
//                        }
//                    }
//                })
//        );
//    }
//
//    @SuppressWarnings("unchecked")
//    private <T> Provider<T> singletonProvider(final Key key, Singleton singleton, final Provider<T> provider) {
//        return singleton != null ? new Provider<T>() {
//            @Override
//            public T get() {
//                if (null != app) {
//                    T t = (T) app.singleton(key.type);
//                    if (null != t) {
//                        return t;
//                    }
//                }
//                if (!singletons.containsKey(key)) {
//                    synchronized (singletons) {
//                        if (!singletons.containsKey(key)) {
//                            singletons.put(key, provider.get());
//                        }
//                    }
//                }
//                return (T) singletons.get(key);
//            }
//        } : provider;
//    }
//
//    private List<Provider<?>> paramProviders2(final Key key, Class<?>[] paramClasses, Type[] paramTypes, Annotation[][] paramAnnotations, final Set<Key> chain) {
//        int len = paramClasses.length;
//        List<Provider<?>> providers = C.newSizedList(len);
//        for (int i = 0; i < len; ++i) {
//
//        }
//    }
//
//    private Provider<?>[] paramProviders(
//            final Key key,
//            Class<?>[] parameterClasses,
//            Type[] parameterTypes,
//            Annotation[][] annotations,
//            final Set<Key> chain
//    ) {
//        Provider<?>[] providers = new Provider<?>[parameterTypes.length];
//        for (int i = 0; i < parameterTypes.length; ++i) {
//            Class<?> parameterClass = parameterClasses[i];
//            Annotation qualifier = qualifiers(annotations[i]);
//            Class<?> providerType = Provider.class.equals(parameterClass) ?
//                    (Class<?>) ((ParameterizedType) parameterTypes[i]).getActualTypeArguments()[0] :
//                    null;
//            if (providerType == null) {
//                final Key newKey = Key.of(parameterClass, qualifier);
//                final Set<Key> newChain = append(chain, key);
//                if (newChain.contains(newKey)) {
//                    throw new FeatherException(String.format("Circular dependency: %s", chain(newChain, newKey)));
//                }
//                providers[i] = new Provider() {
//                    @Override
//                    public Object get() {
//                        return provider(newKey, newChain).get();
//                    }
//                };
//            } else {
//                final Key newKey = Key.of(providerType, qualifier);
//                providers[i] = new Provider() {
//                    @Override
//                    public Object get() {
//                        return provider(newKey, null);
//                    }
//                };
//            }
//        }
//        return providers;
//    }
//
//    private static Object[] params(Provider<?>[] paramProviders) {
//        Object[] params = new Object[paramProviders.length];
//        for (int i = 0; i < paramProviders.length; ++i) {
//            params[i] = paramProviders[i].get();
//        }
//        return params;
//    }
//
//    private static Set<Key> append(Set<Key> set, Key newKey) {
//        if (set != null && !set.isEmpty()) {
//            Set<Key> appended = new LinkedHashSet<Key>(set);
//            appended.add(newKey);
//            return appended;
//        } else {
//            return Collections.singleton(newKey);
//        }
//    }
//
//    private static Object[][] injectFields(Class<?> target) {
//        Set<Field> fields = fields(target);
//        Object[][] fs = new Object[fields.size()][];
//        int i = 0;
//        for (Field f : fields) {
//            Type type = f.getGenericType();
//            Type[] typeParameters = null;
//            if (type instanceof ParameterizedType) {
//                typeParameters = ((ParameterizedType) type).getActualTypeArguments();
//            }
//            Class<?> providerType = f.getType().equals(Provider.class) ?
//                    (Class<?>) typeParameters[0] :
//                    null;
//
//            fs[i++] = new Object[]{
//                    f,
//                    providerType != null,
//                    Key.of(providerType != null ? providerType : f.getType(), qualifiers(f.getAnnotations())),
//                    typeParameters
//            };
//        }
//        return fs;
//    }
//
//    private static Set<Field> fields(Class<?> type) {
//        Class<?> current = type;
//        Set<Field> fields = new HashSet<Field>();
//        while (!current.equals(Object.class)) {
//            for (Field field : current.getDeclaredFields()) {
//                if (field.isAnnotationPresent(Inject.class)) {
//                    field.setAccessible(true);
//                    fields.add(field);
//                }
//            }
//            current = current.getSuperclass();
//        }
//        return fields;
//    }
//
//    private static String chain(Set<Key> chain, Key lastKey) {
//        StringBuilder chainString = new StringBuilder();
//        for (Key key : chain) {
//            chainString.append(key.toString()).append(" -> ");
//        }
//        return chainString.append(lastKey.toString()).toString();
//    }
//
//    private static boolean fieldInjectionDetected(Key key) {
//        Class c = key.type;
//        while (c != Object.class && c != null) {
//            Field[] fa = c.getDeclaredFields();
//            for (Field f : fa) {
//                if (f.isAnnotationPresent(Inject.class)) {
//                    return true;
//                }
//            }
//            c = c.getSuperclass();
//        }
//        return false;
//    }
//
//    private static Constructor constructor(Key key) {
//        Constructor inject = null;
//        Constructor noarg = null;
//        for (Constructor c : key.type.getDeclaredConstructors()) {
//            if (c.isAnnotationPresent(Inject.class)) {
//                if (inject == null) {
//                    inject = c;
//                } else {
//                    throw new FeatherException(String.format("%s has multiple @Inject constructors", key.type));
//                }
//            } else if (c.getParameterTypes().length == 0) {
//                noarg = c;
//            }
//        }
//        Constructor constructor = inject != null ? inject : noarg;
//        if (constructor != null) {
//            constructor.setAccessible(true);
//            return constructor;
//        } else {
//            throw new FeatherException(String.format("%s doesn't have an @Inject or no-arg constructor, or a module provider", key.type.getName()));
//        }
//    }
//
//    private static Set<Method> providers(Class<?> type) {
//        Class<?> current = type;
//        Set<Method> providers = new HashSet<Method>();
//        while (!current.equals(Object.class)) {
//            for (Method method : current.getDeclaredMethods()) {
//                if (method.isAnnotationPresent(Provides.class) && (type.equals(current) || !providerInSubClass(method, providers))) {
//                    method.setAccessible(true);
//                    providers.add(method);
//                }
//            }
//            current = current.getSuperclass();
//        }
//        return providers;
//    }
//
//    private static List<Annotation> qualifiers(Annotation[] annotations) {
//        List<Annotation> list = C.newList();
//        for (Annotation annotation : annotations) {
//            if (annotation.annotationType().isAnnotationPresent(Qualifier.class)) {
//                list.add(annotation);
//            }
//        }
//        return list;
//    }
//
//    private static List<Class> typeParameters(Type genericType) {
//        if (genericType instanceof ParameterizedType) {
//            List<Class> l = C.newList();
//            ParameterizedType type = (ParameterizedType) genericType;
//            Type[] params = type.getActualTypeArguments();
//            for (Type t : params) {
//                if (t instanceof Class) {
//                    l.add((Class) t);
//                } else {
//                    // this must be type variable like "K" in `<K, V>` etc
//                    l.add(Object.class);
//                }
//            }
//            return l;
//        }
//        return C.list();
//    }
//
//    private static boolean providerInSubClass(Method method, Set<Method> discoveredMethods) {
//        for (Method discovered : discoveredMethods) {
//            if (discovered.getName().equals(method.getName()) && Arrays.equals(method.getParameterTypes(), discovered.getParameterTypes())) {
//                return true;
//            }
//        }
//        return false;
//    }
}
