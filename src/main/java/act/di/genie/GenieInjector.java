package act.di.genie;

import act.app.App;
import act.app.event.AppEventId;
import act.di.DependencyInjectionBinder;
import act.di.DependencyInjectorBase;
import act.di.Providers;
import act.util.SubClassFinder;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.exception.NotAppliedException;
import org.osgl.genie.Genie;
import org.osgl.genie.Module;
import org.osgl.util.C;
import org.osgl.util.E;

import javax.inject.Provider;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GenieInjector extends DependencyInjectorBase<GenieInjector> {

    private volatile Genie genie;
    private List<Object> modules;

    public GenieInjector(App app) {
        super(app);
        modules = factories();
    }

    @Override
    public <T> T get(Class<T> clazz) {
        Genie genie = genie();
        if (null == genie) {
            return $.newInstance(clazz);
        } else {
            return genie.get(clazz);
        }
    }

    @Override
    public synchronized void registerDiBinder(DependencyInjectionBinder binder) {
        if (null != genie) {
            genie.registerProvider(binder.targetClass(), binder);
        } else {
            super.registerDiBinder(binder);
        }
    }

    public void addModule(Object module) {
        E.illegalStateIf(null != genie);
        modules.add(module);
    }

    private List<Object> factories() {
        Set<String> factories = GenieFactoryFinder.factories();
        int len = factories.size();
        List<Object> list = C.newSizedList(factories.size());
        if (0 == len) {
            return list;
        }
        ClassLoader cl = App.instance().classLoader();
        for (String className : factories) {
            list.add($.classForName(className, cl));
        }
        return list;
    }

    private Genie genie() {
        if (null == genie) {
            synchronized (this) {
                if (null == genie) {
                    genie = Genie.create(modules.toArray(new Object[modules.size()]));
                    for (Map.Entry<Class, DependencyInjectionBinder> entry : binders.entrySet()) {
                        genie.registerProvider(entry.getKey(), entry.getValue());
                    }
                    $.F2<Class, Provider, Void> register = new $.F2<Class, Provider, Void>() {
                        @Override
                        public Void apply(Class aClass, Provider provider) throws NotAppliedException, Osgl.Break {
                            genie.registerProvider(aClass, provider);
                            return null;
                        }
                    };
                    Providers.registerBuiltInProviders(Providers.class, register);
                    Providers.registerBuiltInProviders(GenieProviders.class, register);
                }
            }
        }
        return genie;
    }

    @SubClassFinder(value = Module.class, callOn = AppEventId.APP_CODE_SCANNED)
    public static void foundModule(Class<? extends Module> moduleClass) {
        App app = App.instance();
        GenieInjector genieInjector = app.injector();
        genieInjector.addModule($.newInstance(moduleClass));
    }

}
