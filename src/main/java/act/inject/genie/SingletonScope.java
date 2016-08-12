package act.inject.genie;

import act.app.App;
import org.osgl.inject.ScopeCache;

public class SingletonScope implements ScopeCache.SingletonScope {

    public static final SingletonScope INSTANCE = new act.inject.genie.SingletonScope();

    private App app;

    public SingletonScope() {
        app = App.instance();
    }

    @Override
    public <T> T get(Class<T> aClass) {
        return app.singleton(aClass);
    }

    @Override
    public <T> void put(Class<T> aClass, T t) {
        app.registerSingleton(aClass, t);
    }

}
