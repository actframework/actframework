package act.inject.param;

import act.Destroyable;
import act.app.AppServiceBase;
import act.inject.DependencyInjector;
import act.inject.genie.GenieInjector;
import act.util.ActContext;
import act.util.DestroyableBase;
import act.util.SingletonBase;
import org.osgl.inject.BeanSpec;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class ProvidedValueLoader extends DestroyableBase implements ParamValueLoader {
    private DependencyInjector<?> injector;
    private BeanSpec beanSpec;
    private Object singleton;
    private ProvidedValueLoader(BeanSpec beanSpec, DependencyInjector<?> injector) {
        Class type = beanSpec.rawType();
        if (AppServiceBase.class.isAssignableFrom(type)
                || SingletonBase.class.isAssignableFrom(type)
                || type.isAnnotationPresent(Singleton.class)
                || type.isAnnotationPresent(ApplicationScoped.class)) {
            singleton = injector.get(type);
        } else {
            this.beanSpec = beanSpec;
        }
        this.injector = injector;
    }

    @Override
    public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
        if (context.getClass().equals(beanSpec.rawType())) {
            return context;
        } else if (null != singleton) {
            return singleton;
        } else {
            GenieInjector genieInjector = (GenieInjector) injector;
            return genieInjector.get(beanSpec);
        }
    }

    @Override
    protected void releaseResources() {
        Destroyable.Util.tryDestroy(singleton);
        singleton = null;
        injector = null;
        lookup.clear();
    }

    private static ConcurrentMap<BeanSpec, ProvidedValueLoader> lookup = new ConcurrentHashMap<>();

    static ProvidedValueLoader get(BeanSpec beanSpec, DependencyInjector<?> injector) {
        ProvidedValueLoader loader = lookup.get(beanSpec);
        if (null == loader) {
            loader = new ProvidedValueLoader(beanSpec, injector);
            lookup.putIfAbsent(beanSpec, loader);
        }
        return loader;
    }
}
