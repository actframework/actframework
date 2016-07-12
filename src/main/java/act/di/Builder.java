package act.di;

import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

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

    protected final Class<T> targetClass;
    protected final Annotation[] annotations;
    protected final Type[] typeParameters;

    public Builder(Class<T> targetClass, Annotation[] annotations, Type[] typeParameters) {
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
     * Returns the target classes supported by this builder
     * @return the supported classes
     */
    public abstract Set<Class<? extends T>> supportedClasses();

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

}
