package act.di;

import act.app.AppService;
import org.osgl.inject.Injector;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public interface DependencyInjector<DI extends DependencyInjector<DI>> extends AppService<DI>, Injector {

    /**
     * Register a {@link DependencyInjectionBinder} to the injector
     * @param binder the binder
     */
    void registerDiBinder(DependencyInjectionBinder binder);

    /**
     * Register a {@link DependencyInjectionListener} to the injector
     * @param listener the dependency injection event listener
     */
    void registerDiListener(DependencyInjectionListener listener);

    /**
     * Report if a given type is a provided type (e.g. ActContext, All application services etc, DAO)
     * @param type the type to be checked
     * @return `true` if the type is a provided type or `false` otherwise
     */
    boolean isProvided(Class<?> type);

    /**
     * Once an object has been created and ready for injection, this method will be
     * called to call back to the {@link DependencyInjectionListener listeners} that has been
     * {@link #registerDiListener(DependencyInjectionListener) registered}
     * @param injectee the object to be injected
     * @param typeParameters if the object is to be injected to a field, and the field's
     *                       {@link Field#getGenericType() generic type} is kind of
     *                       {@link java.lang.reflect.ParameterizedType} then the
     *                       {@link ParameterizedType#getActualTypeArguments() type parameters}
     *                       will be passed to the listener
     */
    void fireInjectedEvent(Object injectee, Type[] typeParameters);

    /**
     * Get a bean instance by class
     * @param clazz the class of the bean instance to be returned
     * @param <T> the generic type of the bean instance
     * @return the bean instance
     */
    <T> T get(Class<T> clazz);
}
