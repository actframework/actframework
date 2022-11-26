package act.inject.genie;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.Act;
import act.app.App;
import act.app.conf.AppConfigurator;
import act.app.event.SysEventId;
import act.controller.ActionMethodParamAnnotationHandler;
import act.inject.*;
import act.sys.Env;
import act.util.*;
import org.osgl.$;
import org.osgl.exception.ConfigurationException;
import org.osgl.exception.NotAppliedException;
import org.osgl.inject.*;
import org.osgl.inject.annotation.*;
import org.osgl.mvc.annotation.Bind;
import org.osgl.mvc.annotation.Param;
import org.osgl.util.E;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import javax.inject.*;

public class GenieInjector extends DependencyInjectorBase<GenieInjector> {

    private static final org.osgl.inject.Module SCOPE_MODULE = new org.osgl.inject.Module() {
        @Override
        protected void configure() {
            bind(ScopeCache.SessionScope.class).to(new SessionScope());
            bind(ScopeCache.RequestScope.class).to(new RequestScope());
            bind(ScopeCache.SingletonScope.class).to(new SingletonScope());
        }
    };

    private volatile Genie genie;
    private Set<Object> modules;
    private Set<Class<? extends Annotation>> injectTags = new HashSet<Class<? extends Annotation>>();
    private boolean locked;

    public GenieInjector(App app) {
        super(app, true);
        modules = new LinkedHashSet<>();
        modules.add(SCOPE_MODULE);
        modules.addAll(factories());
        this.locked = true;
    }

    public synchronized void unlock() {
        locked = false;
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
    public <T> void registerProvider(Class<? super T> type, Provider<? extends T> provider) {
        genie().registerProvider(type, provider);
    }

    @Override
    public <T> void registerNamedProvider(Class<? super T> type, NamedProvider<? extends T> provider) {
        genie().registerNamedProvider(type, provider);
    }

    @Override
    public boolean isProvided(Class<?> type) {
        return !$.isSimpleType(type) && ActProviders.isProvided(type)
                || type.isAnnotationPresent(Provided.class)
                || type.isAnnotationPresent(Inject.class)
                || type.isAnnotationPresent(Singleton.class)
                || SingletonBase.class.isAssignableFrom(type);
    }

    public boolean isProvided(BeanSpec beanSpec) {
        Class rawType = beanSpec.rawType();
        boolean provided = (ActProviders.isProvided(rawType)
                || null != beanSpec.getAnnotation(Inject.class)
                || null != beanSpec.getAnnotation(Provided.class)
                || null != beanSpec.getAnnotation(Context.class)
                || null != beanSpec.getAnnotation(Singleton.class)
                || beanSpec.isInstanceOf(SingletonBase.class)
                || subjectToInject(beanSpec)
        );
        return provided && (!($.isSimpleType(rawType) && !beanSpec.hasAnnotation()));
    }

    private boolean isPureSimpleType(BeanSpec spec, Class<?> rawType) {
        return $.isSimpleType(rawType) && spec.qualifiers().isEmpty();
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
    public boolean isScope(Class<? extends Annotation> annoClass) {
        return genie().isScope(annoClass);
    }

    @Override
    public boolean isInheritedScopeStopper(Class<? extends Annotation> annoClass) {
        return genie().isInheritedScopeStopper(annoClass);
    }

    @Override
    public Class<? extends Annotation> scopeByAlias(Class<? extends Annotation> aClass) {
        return genie().scopeByAlias(aClass);
    }

    public void addModule(Object module) {
        E.illegalStateIf(null != genie);
        modules.add(module);
    }

    public boolean subjectToInject(BeanSpec spec) {
        return app().isSingleton(spec.rawType()) || genie().subjectToInject(spec);
    }

    private Set<Object> factories() {
        Set<String> factories = GenieFactoryFinder.factories();
        if (null == factories) {
            // unit testing
            factories = new HashSet<>();
        }
        int len = factories.size();
        Set<Object> set = new HashSet<>();
        if (0 == len) {
            return set;
        }
        App app = Act.app();
        for (String className : factories) {
            set.add(app.classForName(className));
        }
        return set;
    }

    public Genie genie() {
        if (null == genie) {
            synchronized (this) {
                E.illegalStateIf(locked, "Injector locked");
                if (null == genie) {
                    InjectListener listener = new GenieListener(this);
                    genie = Genie.create(listener, modules.toArray(new Object[modules.size()]));
                    for (Map.Entry<Class, DependencyInjectionBinder> entry : binders.entrySet()) {
                        genie.registerProvider(entry.getKey(), entry.getValue());
                    }
                    $.F2<Class, NamedProvider, Void> namedProviderRegister = new $.F2<Class, NamedProvider, Void>()  {
                        @Override
                        public Void apply(Class aClass, NamedProvider namedProvider) throws NotAppliedException, $.Break {
                            genie.registerNamedProvider(aClass, namedProvider);
                            return null;
                        }
                    };
                    $.F2<Class, Provider, Void> register = new $.F2<Class, Provider, Void>() {
                        @Override
                        public Void apply(Class aClass, Provider provider) throws NotAppliedException, $.Break {
                            genie.registerProvider(aClass, provider);
                            return null;
                        }
                    };
                    genie.registerQualifiers(Bind.class, Param.class);
                    genie.registerScopeAlias(Singleton.class, Stateless.class);
                    genie.registerScopeAlias(Singleton.class, InheritedStateless.class);
                    genie.registerScopeAlias(StopInheritedScope.class, Stateful.class);
                    List<ActionMethodParamAnnotationHandler> list = Act.pluginManager().pluginList(ActionMethodParamAnnotationHandler.class);
                    for (ActionMethodParamAnnotationHandler h : list) {
                        Set<Class<? extends Annotation>> set = h.listenTo();
                        for (Class<? extends Annotation> c : set) {
                            genie.registerQualifiers(c);
                        }
                    }

                    ActProviders.registerBuiltInProviders(ActProviders.class, register);
                    ActProviders.registerBuiltInProviders(GenieProviders.class, register);
                    ActProviders.registerBuiltInNamedProviders(ActProviders.class, namedProviderRegister);
                    ActProviders.registerBuiltInNamedProviders(GenieProviders.class, namedProviderRegister);
                    for (Class<? extends Annotation> injectTag : injectTags) {
                        genie.registerInjectTag(injectTag);
                    }

                    genie.registerProvider(Genie.class, new Provider<Genie>() {
                        @Override
                        public Genie get() {
                            return genie;
                        }
                    });
                }
            }
        }
        return genie;
    }

    @SubClassFinder(callOn = SysEventId.DEPENDENCY_INJECTOR_INITIALIZED)
    public static void foundModule(Class<? extends org.osgl.inject.Module> moduleClass) {
        addModuleClass(moduleClass);
    }

    @SubClassFinder(callOn = SysEventId.DEPENDENCY_INJECTOR_INITIALIZED)
    public static void foundConfigurator(Class<? extends AppConfigurator> configurator) {
        addModuleClass(configurator);
    }

    private static boolean hasBinding(Class<?> clazz) {
        GenieInjector gi = Act.injector();
        Genie genie = gi.genie();
        return (genie.hasProvider(clazz));
    }

    @AnnotatedClassFinder(value = AutoBind.class, callOn = SysEventId.DEPENDENCY_INJECTOR_PROVISIONED, noAbstract = false)
    public static void foundAutoBinding(final Class<?> autoBinding) {
        // check if there are manual binding (via modules) for the class already
        if (hasBinding(autoBinding)) {
            return;
        }
        final App app = Act.app();
        ClassInfoRepository repo = app.classLoader().classInfoRepository();
        ClassNode root = repo.node(autoBinding.getName());
        E.invalidConfigurationIf(null == root, "Cannot find AutoBind root: %s", autoBinding.getName());
        final Set<Class<?>> candidates = new LinkedHashSet<>();
        root.visitPublicNotAbstractSubTreeNodes(new $.Visitor<ClassNode>() {
            @Override
            public void visit(ClassNode classNode) throws $.Break {
                try {
                    Class<?> clazz = app.classForName(classNode.name());
                    if (Env.matches(clazz)) {
                        candidates.add(clazz);
                    }
                } catch (ConfigurationException e) {
                    throw e;
                } catch (RuntimeException e) {
                    throw new ConfigurationException(e, "Unable to auto bind on %s", autoBinding.getName());
                }
            }
        });

        if (!candidates.isEmpty()) {
            GenieInjector injector = Act.app().injector();
            // key is: set of annotations plus name
            Map<$.T2<Set<Annotation>, String>, Class<?>> multiCandidatesMap = new HashMap<>();
            for (Class<?> c : candidates) {
                BeanSpec spec = BeanSpec.of(c, injector);
                Set<Annotation> qualifiers = spec.qualifiers();
                String name = spec.name();
                $.T2<Set<Annotation>, String> key = $.T2(qualifiers, name);
                if (multiCandidatesMap.containsKey(key)) {
                    throw new ConfigurationException("Unable to auto bind on %s: multiple same qualified candidates found", autoBinding);
                } else {
                    multiCandidatesMap.put(key, c);
                }
            }
            for (Map.Entry<$.T2<Set<Annotation>, String>, Class<?>> entry : multiCandidatesMap.entrySet()) {
                Genie.Binder binder = new Genie.Binder(autoBinding).to(entry.getValue());
                $.T2<Set<Annotation>, String> key = entry.getKey();
                Set<Annotation> qualifiers = key._1;
                String name = key._2;
                if (!qualifiers.isEmpty()) {
                    binder = binder.withAnnotation(qualifiers.toArray(new Annotation[qualifiers.size()]));
                }
                if (null != name) {
                    binder.named(name);
                }
                binder.register(injector.genie());
            }
        } else {
            Act.LOGGER.warn("Unable to auto bind on %s: implementation not found", autoBinding);
        }
    }

    @AnnotatedClassFinder(value = ModuleTag.class, callOn = SysEventId.DEPENDENCY_INJECTOR_INITIALIZED, noAbstract = false)
    public static void foundTaggedModule(Class<?> taggedModuleClass) {
        addModuleClass(taggedModuleClass);
    }

    public static void addModuleClass(Class<?> moduleClass) {
        if (!isModuleAllowed(moduleClass)) {
            return;
        }
        App app = App.instance();
        GenieInjector genieInjector = app.injector();
        genieInjector.addModule(moduleClass);
    }

    @AnnotatedClassFinder(value = LoadValue.class, noAbstract = false, callOn = SysEventId.DEPENDENCY_INJECTOR_LOADED)
    public static void foundValueLoader(Class<? extends Annotation> valueLoader) {
        App app = App.instance();
        GenieInjector genieInjector = app.injector();
        genieInjector.injectTags.add(valueLoader);
    }

    @SubClassFinder
    public static void foundGenericTypedBeanLoader(Class<? extends GenericTypedBeanLoader> loaderClass) {
        App app = App.instance();
        GenieInjector genieInjector = app.injector();
        Type[] ta = loaderClass.getGenericInterfaces();
        for (Type t : ta) {
            if (t instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) t;
                if (GenericTypedBeanLoader.class == pt.getRawType()) {
                    Type compoentType = pt.getActualTypeArguments()[0];
                    genieInjector.genie().registerGenericTypedBeanLoader((Class) compoentType, app.getInstance(loaderClass));
                }
            }
        }
    }

    @SubClassFinder
    public static void foundProviderBase(Class<? extends ActProvider> providerClass) {
        App app = App.instance();
        GenieInjector genieInjector = app.injector();
        ActProvider provider = app.getInstance(providerClass);
        genieInjector.genie().registerProvider(provider.targetType(), provider);
    }

    private static boolean isModuleAllowed(Class<?> moduleClass) {
        return Env.matches(moduleClass);
    }

}
