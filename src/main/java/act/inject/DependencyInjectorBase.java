package act.inject;

import act.Destroyable;
import act.app.App;
import act.app.AppClassLoader;
import act.app.AppServiceBase;
import act.app.event.AppEventId;
import act.util.ClassNode;
import act.util.SubClassFinder;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.inject.BeanSpec;
import org.osgl.util.C;

import javax.enterprise.context.ApplicationScoped;
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
        ActProviders.addProvidedType(binder.targetClass());
    }

    @Override
    public synchronized void registerDiListener(DependencyInjectionListener listener) {
        Class[] targets = listener.listenTo();
        for (Class c : targets) {
            List<DependencyInjectionListener> list = listeners.get(c);
            if (null == list) {
                final List<DependencyInjectionListener> list0 = C.newList();
                list = list0;
                final AppClassLoader cl = app().classLoader();
                ClassNode node = cl.classInfoRepository().node(c.getName());
                node.visitPublicNotAbstractTreeNodes(new $.Visitor<ClassNode>() {
                    @Override
                    public void visit(ClassNode classNode) throws Osgl.Break {
                        listeners.put($.classForName(classNode.name(), cl), list0);
                    }
                });
                listeners.put(c, list);
            }
            list.add(listener);
        }
    }

    @Override
    public void fireInjectedEvent(Object bean, BeanSpec spec) {
        Class c = spec.rawType();
        List<DependencyInjectionListener> list = listeners.get(c);
        if (null != list) {
            for (DependencyInjectionListener listener : list) {
                listener.onInjection(bean, spec);
            }
        }
    }


    @SubClassFinder(value = DependencyInjectionListener.class, callOn = AppEventId.DEPENDENCY_INJECTOR_PROVISIONED)
    public static void discoverDiListener(final Class<? extends DependencyInjectionListener> target) {
        App app = App.instance();
        DependencyInjector di = app.injector();
        di.registerDiListener(app.getInstance(target));
    }

}
