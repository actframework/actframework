package act.di.loader;

import act.app.App;
import act.di.BeanLoader;

/**
 * Base class for {@link act.di.BeanLoader} implementations
 */
public abstract class BeanLoaderBase<T> implements BeanLoader<T> {

    protected App app() {
        return App.instance();
    }

}
