package act.di.loader;

import act.conf.AppConfig;
import act.di.BeanLoader;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.util.C;

import java.util.Collection;
import java.util.List;

/**
 * Load beans from {@link act.conf.AppConfig application configuration}
 */
public class ConfigurationLoader<T> extends BeanLoaderBase<T> implements BeanLoader<T> {

    @Override
    public T load(Object hint, Object... options) {
        return (T) conf().get(hint.toString());
    }

    @Override
    public List<T> loadMultiple(Object hint, Object... options) {
        Object o = conf().get(hint.toString());
        if (o instanceof Collection) {
            return C.list((Collection<T>)o);
        }
        return C.list((T)o);
    }

    @Override
    public Osgl.Function<T, Boolean> filter(final Object hint, final Object... options) {
        return new $.Predicate<T>() {
            @Override
            public boolean test(T t) {
                List<T> list = loadMultiple(hint, options);
                return list.contains(t);
            }
        };
    }

    private AppConfig conf() {
        return app().config();
    }
}
