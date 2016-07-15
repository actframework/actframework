package act.di.air;

import act.app.event.AppEventId;
import act.job.OnAppEvent;
import act.util.SubClassFinder;
import org.osgl.$;
import org.osgl.util.C;

import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * The `Builder` is kind of {@link Provider} that when called to inject a dependent object
 * instance, will perform extra operation to initialize the object instance.
 *
 * Here is a typical usage of Builder:
 *
 * Suppose the application has defined a `Filter` instance. And there are multiple
 * `Filter` implementations has been created. Then there is a consumer class of `Filter` interface,
 * which needs to use all `Filter` implementations. So we should allow it inject a list or set
 * of `Filter`s:
 *
 * <pre><code>
 * public class FilterConsumer {
 *     private List&lt;Filter&gt; filters;
 *     &#64;Inject
 *     public class FilterConsumer(List&lt;Filter&gt; filters) {
 *         this.filters = filters;
 *     }
 * }
 * </code></pre>
 *
 * @see <a href="https://github.com/actframework/actframework/issues/61">GitHub #61</a>
 */
public abstract class Builder<T> implements Provider<T> {

    protected final Class<? extends T> targetClass;
    protected final Annotation[] annotations;
    protected final Type[] typeParameters;

    public Builder(Class<? extends T> targetClass, Annotation[] annotations, Type[] typeParameters) {
        this.targetClass = targetClass;
        this.annotations = annotations;
        this.typeParameters = typeParameters;
    }

    @Override
    public T get() {
        T t = createInstance();
        initializeInstance(t);
        return t;
    }

    /**
     * Create an empty instance
     * @return the new instance
     */
    protected abstract T createInstance();

    /**
     * Initialize the instance. This might be populate a collection etc.
     * @param instance the instance to be initialized
     */
    protected abstract void initializeInstance(T instance);

    public interface Factory<T> {
        /**
         * Create a builder
         * @param targetClass the class of the bean the builder will build
         * @param annotations the annotations data found on Field or method parameters
         * @param typeParams the type parameters found on Field or method parameters
         * @return a builder
         */
        Builder<T> createBuilder(Class<T> targetClass, Annotation[] annotations, Type[] typeParams);

        /**
         * Returns supported class the Builder created by this factory can build
         * @return
         */
        Class<T> targetClass();

        /**
         * Manage builder factories
         */
        class Manager {
            private static Map<Class, WeightedFactory> registry = C.newMap();

            @SubClassFinder(value = Factory.class, callOn = AppEventId.CLASS_LOADED)
            public static void found(Class<? extends Factory> factoryClass) {
                Factory factory = $.newInstance(factoryClass);
                register(factory);
            }

            @OnAppEvent(AppEventId.EVENT_BUS_INITIALIZED)
            public static void destroy() {
                registry.clear();
            }

            public static <T> Factory<T> get(Class<T> clazz) {
                return $.cast(registry.get(clazz));
            }

            /**
             * Multiple factory might be able to target to the same class. E.g.
             *
             * A factory target to `ArrayList` builder, should able to provide
             * injection for a general `List` and in turn the `AbstractList`,
             * `AbstractCollection` and `Collection`. Now suppose user provides
             * a factory target to `List` specifically, then that factory shall
             * take the precedence when `List` type is required than the
             * `ArrayList` factory.
             *
             * `AFFINITY` is designed measure how far a factory is to a specific
             * class to resolve the concern described above. The larger affinity
             * value is, to further the factory is away from the class
             */
            private static final ThreadLocal<Integer> AFFINITY = new ThreadLocal<>();

            private static synchronized void register(Factory<?> factory) {
                AFFINITY.set(0);
                Class<?> target = factory.targetClass();
                register(target, factory);
            }


            /**
             * Register for all super types and interfaces
             */
            private static void register(Class<?> target, Factory<?> factory) {
                addIntoRegistry(target, factory);
                AFFINITY.set(AFFINITY.get() + 1);
                Class dad = target.getSuperclass();
                if (null != dad && Object.class != dad) {
                    register(dad, factory);
                }
                Class[] roles = target.getInterfaces();
                if (null == roles) {
                    return;
                }
                for (Class role: roles) {
                    register(role, factory);
                }
            }

            private static void addIntoRegistry(Class<?> key, Factory<?> val) {
                WeightedFactory weighted = weighted(val);
                WeightedFactory old = registry.get(key);
                if (null == old || old.compareTo(weighted) > 0) {
                    registry.put(key, weighted);
                }
            }

            private static WeightedFactory weighted(Factory factory) {
                if (factory instanceof WeightedFactory) {
                    return (WeightedFactory) factory;
                }
                return new WeightedFactory(factory);
            }

            private static class WeightedFactory<T> implements Factory<T>, Comparable<WeightedFactory> {

                private Factory<T> realFactory;
                private int affility;

                public WeightedFactory(Factory<T> factory) {
                    realFactory = factory;
                    affility = AFFINITY.get();
                }

                @Override
                public Builder<T> createBuilder(Class<T> targetClass, Annotation[] annotations, Type[] typeParams) {
                    return realFactory.createBuilder(targetClass, annotations, typeParams);
                }

                @Override
                public Class<T> targetClass() {
                    return realFactory.targetClass();
                }

                @Override
                public int compareTo(WeightedFactory o) {
                    return affility - o.affility;
                }
            }
        }
    }
}
