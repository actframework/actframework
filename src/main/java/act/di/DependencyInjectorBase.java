package act.di;

import act.Destroyable;
import act.app.App;
import act.app.AppServiceBase;
import act.app.event.AppEventId;
import act.util.SubClassFinder;
import org.osgl.util.C;

import javax.enterprise.context.ApplicationScoped;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public abstract class DependencyInjectorBase<DI extends DependencyInjectorBase<DI>> extends AppServiceBase<DI> implements DependencyInjector<DI> {

    protected Map<Class, DependencyInjectionBinder> binders = C.newMap();
    protected Map<Class, List<DependencyInjectionListener>> listeners = C.newMap();

    public DependencyInjectorBase(App app) {
        this(app, false);
    }

    protected DependencyInjectorBase(App app, boolean noRegister) {
        super(app, true);
        if (!noRegister) {
            app.injector(this);
        }
    }

    @Override
    protected void releaseResources() {
        Destroyable.Util.tryDestroyAll(binders.values(), ApplicationScoped.class);
        binders.clear();
    }


    @Override
    public synchronized void registerDiBinder(DependencyInjectionBinder binder) {
        binders.put(binder.targetClass(), binder);
        ActProviders.addContextClass(binder.targetClass());
    }

    @Override
    public synchronized void registerDiListener(DependencyInjectionListener listener) {
        Class[] targets = listener.listenTo();
        for (Class c : targets) {
            List<DependencyInjectionListener> list = listeners.get(c);
            if (null == list) {
                list = C.newList();
                listeners.put(c, list);
            }
            list.add(listener);
        }
    }

    @Override
    public void fireInjectedEvent(Object injectee, Type[] typeParameters) {
        Class c = injectee.getClass();
        List<DependencyInjectionListener> list = listeners.get(c);
        if (null != list) {
            for (DependencyInjectionListener listener : list) {
                listener.onInjection(injectee, typeParameters);
            }
        }
    }


    @SubClassFinder(value = DependencyInjectionListener.class, callOn = AppEventId.DEPENDENCY_INJECTOR_LOADED)
    public static void discoverDiListener(final Class<? extends DependencyInjectionListener> target) {
        App app = App.instance();
        DependencyInjector di = app.injector();
        di.registerDiListener(app.getInstance(target));
    }

}
