package act.inject;

import org.osgl.inject.BeanSpec;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 * Listens for injections into instances of type {@link #listenTo()}. Useful for performing further
 * injections, post-injection initialization, and more.
 */
public interface DependencyInjectionListener {

    /**
     * Returns the classes this listener is interested in
     *
     * @return A list of classes
     */
    Class[] listenTo();

    /**
     * Invoked once an instance has been created.
     * <p>
     * If {@link DependencyInjector} inject a field and the {@link Field#getGenericType() generic type} of
     * the field is kind of {@link java.lang.reflect.ParameterizedType}, then the type parameters of that
     * generic type will be passed to the listener
     * </p>
     *
     * @param bean     instance to be returned by {@link DependencyInjector}
     * @param beanSpec the spec about the bean instance
     */
    void onInjection(Object bean, BeanSpec beanSpec);
}
