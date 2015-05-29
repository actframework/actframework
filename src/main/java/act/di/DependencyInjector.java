package act.di;

import act.app.AppService;

public interface DependencyInjector<FT extends DependencyInjector> extends AppService<FT> {
    /**
     * Create an instance of type T using the class of type T
     * @param clazz
     * @param <T>
     * @return the instance
     */
    <T> T create(Class<T> clazz);
}
