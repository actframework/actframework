package act.inject.genie;

import act.Act;
import act.app.App;
import act.app.event.AppEventId;
import act.controller.ActionMethodParamAnnotationHandler;
import act.inject.ActProviders;
import act.inject.DependencyInjectionBinder;
import act.inject.DependencyInjectorBase;
import act.inject.ModuleTag;
import act.sys.Env;
import act.util.AnnotatedClassFinder;
import act.util.SubClassFinder;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.exception.NotAppliedException;
import org.osgl.inject.*;
import org.osgl.inject.annotation.LoadValue;
import org.osgl.inject.annotation.Provided;
import org.osgl.mvc.annotation.Bind;
import org.osgl.mvc.annotation.Param;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import javax.inject.Inject;
import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GenieInjector extends DependencyInjectorBase<GenieInjector> {

    private static final Module SCOPE_MODULE = new Module() {
        @Override
        protected void configure() {
            bind(ScopeCache.SessionScope.class).to(new SessionScope());
            bind(ScopeCache.RequestScope.class).to(new RequestScope());
            bind(ScopeCache.SingletonScope.class).to(new SingletonScope());
        }
    };

    private volatile Genie genie;
    private List<Object> modules;
    private Set<Class<? extends Annotation>> injectTags = new HashSet<Class<? extends Annotation>>();

    public GenieInjector(App app) {
        super(app);
        modules = factories().append(SCOPE_MODULE);
    }

    @Override
    public <T> T get(Class<T> clazz) {
        return genie().get(clazz);
    }

    @Override
    public <T> Provider<T> getProvider(Class<T> aClass) {
        return genie().getProvider(aClass);
    }

    public <T> T get(BeanSpec spec) {
        return genie().get(spec);
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

    @Override
    public boolean isQualifier(Class<? extends Annotation> aClass) {
        return genie().isQualifier(aClass);
    }

    @Override
    public boolean isPostConstructProcessor(Class<? extends Annotation> aClass) {
        return genie().isPostConstructProcessor(aClass);
    }

    @Override
    public boolean isScope(Class<? extends Annotation> aClass) {
        return genie().isScope(aClass);
    }

    public void addModule(Object module) {
        E.illegalStateIf(null != genie);
        modules.add(module);
    }

    public boolean subjectToInject(BeanSpec spec) {
        return genie().subjectToInject(spec);
    }

    private C.List<Object> factories() {
        Set<String> factories = GenieFactoryFinder.factories();
        int len = factories.size();
        C.List<Object> list = C.newSizedList(factories.size());
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
                    InjectListener listener = new GenieListener(this);
                    genie = Genie.create(listener, modules.toArray(new Object[modules.size()]));
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
                    genie.registerQualifiers(Bind.class, Param.class);
                    List<ActionMethodParamAnnotationHandler> list = Act.pluginManager().pluginList(ActionMethodParamAnnotationHandler.class);
                    for (ActionMethodParamAnnotationHandler h : list) {
                        Set<Class<? extends Annotation>> set = h.listenTo();
                        for (Class<? extends Annotation> c: set) {
                            genie.registerQualifiers(c);
                        }
                    }

                    ActProviders.registerBuiltInProviders(ActProviders.class, register);
                    ActProviders.registerBuiltInProviders(GenieProviders.class, register);
                    for (Class<? extends Annotation> injectTag: injectTags) {
                        genie.registerInjectTag(injectTag);
                    }
                }
            }
        }
        return genie;
    }

    @SubClassFinder(callOn = AppEventId.DEPENDENCY_INJECTOR_LOADED)
    public static void foundModule(Class<? extends Module> moduleClass) {
        if (!isModuleAllowed(moduleClass)) {
            return;
        }
        App app = App.instance();
        GenieInjector genieInjector = app.injector();
        genieInjector.addModule($.newInstance(moduleClass));
    }

    @AnnotatedClassFinder(value = ModuleTag.class, callOn = AppEventId.DEPENDENCY_INJECTOR_LOADED, noAbstract = false)
    public static void foundTaggedModule(Class<?> taggedModuleClass) {
        if (!isModuleAllowed(taggedModuleClass)) {
            return;
        }
        App app = App.instance();
        GenieInjector genieInjector = app.injector();
        genieInjector.addModule(taggedModuleClass);
    }

    @AnnotatedClassFinder(value = LoadValue.class, noAbstract = false, callOn = AppEventId.DEPENDENCY_INJECTOR_LOADED)
    public static void foundValueLoader(Class<? extends Annotation> valueLoader) {
        App app = App.instance();
        GenieInjector genieInjector = app.injector();
        genieInjector.injectTags.add(valueLoader);
    }

    @SubClassFinder(callOn = AppEventId.DEPENDENCY_INJECTOR_PROVISIONED)
    public static void foundGenericTypedBeanLoader(Class<? extends GenericTypedBeanLoader> loaderClass) {
        App app = App.instance();
        GenieInjector genieInjector = app.injector();
        Type[] ta = loaderClass.getGenericInterfaces();
        for (Type t: ta) {
            if (t instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) t;
                if (GenericTypedBeanLoader.class == pt.getRawType()) {
                    Type compoentType = pt.getActualTypeArguments()[0];
                    genieInjector.genie().registerGenericTypedBeanLoader((Class)compoentType, app.getInstance(loaderClass));
                }
            }
        }
    }

    private static boolean isModuleAllowed(Class<?> moduleClass) {
        Env.Profile profile = moduleClass.getAnnotation(Env.Profile.class);
        if (null != profile) {
            return isProfileMatched(profile);
        }
        Env.Mode mode = moduleClass.getAnnotation(Env.Mode.class);
        if (null != mode) {
            return isModeMatched(mode);
        }
        return true;
    }

    private static boolean isProfileMatched(Env.Profile profile) {
        boolean unless = profile.unless();
        String required = profile.value();
        return unless ^ S.eq(required, Act.profile());
    }

    private static boolean isModeMatched(Env.Mode mode) {
        boolean unless = mode.unless();
        Act.Mode required = mode.value();
        return unless ^ (required == Act.mode());
    }

}
