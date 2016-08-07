package act.di.genie;

import act.app.App;
import act.app.event.AppEventId;
import act.di.DependencyInjectionBinder;
import act.di.DependencyInjectorBase;
import act.di.ActProviders;
import act.util.SubClassFinder;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.exception.NotAppliedException;
import org.osgl.inject.Genie;
import org.osgl.inject.Module;
import org.osgl.inject.annotation.Provided;
import org.osgl.inject.annotation.Provides;
import org.osgl.mvc.annotation.Bind;
import org.osgl.mvc.annotation.Param;
import org.osgl.mvc.util.Binder;
import org.osgl.util.C;
import org.osgl.util.E;

import javax.inject.Inject;
import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
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
        return genie().get(clazz);
    }

    @Override
    public synchronized void registerDiBinder(DependencyInjectionBinder binder) {
        super.registerDiBinder(binder);
        if (null != genie) {
            genie.registerProvider(binder.targetClass(), binder);
        }
    }

    @Override
    public boolean isProvided(Class<?> type) {
        return ActProviders.isProvided(type)
                || type.isAnnotationPresent(Provided.class)
                || type.isAnnotationPresent(Inject.class);
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
                    genie.registerQualifiers(Param.class, Bind.class);
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
                    ActProviders.registerBuiltInProviders(ActProviders.class, register);
                    ActProviders.registerBuiltInProviders(GenieProviders.class, register);
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
