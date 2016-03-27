package act.di;

import act.app.AppService;
import act.util.ActContext;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public interface DependencyInjector<DI extends DependencyInjector> extends AppService<DI> {
    /**
     * Create an instance of type T using the class of type T
     * @param clazz
     * @param <T>
     * @return the injector instance
     */
    <T> T create(Class<T> clazz);

    /**
     * Create an injector that is able to inject {@link ActContext} instance if the type
     * is required
     * @param context
     * @return a injector instance support injecting app context
     */
    DependencyInjector<DI> createContextAwareInjector(ActContext context);

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
}
