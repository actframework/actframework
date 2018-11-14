package act.app.conf;

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

import act.app.App;
import act.app.AppByteCodeScanner;
import act.app.data.StringValueResolverManager;
import act.app.event.SysEventId;
import act.conf.AppConfig;
import act.event.SysEventListenerBase;
import act.util.AnnotatedTypeFinder;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.Injector;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.Const;
import org.osgl.util.E;
import org.osgl.util.StringValueResolver;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.EventObject;
import java.util.Map;
import java.util.Set;

public class AutoConfigPlugin extends AnnotatedTypeFinder {

    private static final Logger logger = LogManager.get(AutoConfigPlugin.class);

    public AutoConfigPlugin() {
        super(true, false, AutoConfig.class, new $.F2<App, String, Map<Class<? extends AppByteCodeScanner>, Set<String>>>() {
            @Override
            public Map<Class<? extends AppByteCodeScanner>, Set<String>> apply(final App app, final String className) throws NotAppliedException, $.Break {
                app.eventBus().bind(SysEventId.PRE_START, new SysEventListenerBase() {
                    @Override
                    public void on(EventObject event) throws Exception {
                        Class<?> autoConfigClass = app.classForName(className);
                        new AutoConfigLoader(app, autoConfigClass).load();
                    }
                });
                return null;
            }
        });
    }

    /**
     * Used by plugin that to load their {@link AutoConfig auto configure class}
     *
     * @param autoConfigClass the class with {@link AutoConfig} annotation
     * @param app             the application instance
     */
    public static void loadPluginAutoConfig(Class<?> autoConfigClass, App app) {
        new AutoConfigLoader(app, autoConfigClass).load();
    }

    private static class AutoConfigLoader {
        //private App app;
        private AppConfig conf;
        private Class<?> autoConfigClass;
        private String ns;
        private StringValueResolverManager resolverManager;
        private Injector injector;

        AutoConfigLoader(App app, Class<?> autoConfigClass) {
            this.conf = app.config();
            this.autoConfigClass = autoConfigClass;
            this.ns = (autoConfigClass.getAnnotation(AutoConfig.class)).value();
            this.resolverManager = app.resolverManager();
            this.injector = app.injector();
        }

        void load() {
            loadClass(autoConfigClass, ns);
        }

        void loadClass(Class<?> c, String ns) {
            Class[] ca = c.getClasses();
            for (Class c0 : ca) {
                if (c0 == c) {
                    continue;
                }
                int mod = c0.getModifiers();
                if (Modifier.isStatic(mod)) {
                    loadClass(c0, ns + "." + c0.getSimpleName());
                }
            }
            Field[] fa = c.getDeclaredFields();
            for (Field f : fa) {
                if (Modifier.isStatic(f.getModifiers())) {
                    loadAutoConfig_(f, ns);
                }
            }
        }

        private void loadAutoConfig_(Field f, String ns) {
            String key = ns + "." + f.getName();
            Object val = conf.getIgnoreCase(key);
            if (null == val) {
                // try to change the "prefix.key_x_an_y" form to "prefix.key.x.an.y" form
                key = key.replace('_', '.');
                val = conf.getIgnoreCase(key);
                if (null == val) {
                    return;
                }
            }
            BeanSpec spec = BeanSpec.of(f, injector);
            try {
                setField(f, null, key, val, spec);
            } catch (Exception e) {
                throw E.invalidConfiguration(e, "Error get configuration " + key + ": " + e.getMessage());
            }
        }

        private void setField(Field f, Object host, String key, Object val, BeanSpec spec) throws Exception {
            f.setAccessible(true);
            if (spec.isInstanceOf($.Val.class)) {
                $.Val value = $.cast(f.get(host));
                Field fVal = $.Var.class.getDeclaredField("v");
                fVal.setAccessible(true);
                BeanSpec spec0 = BeanSpec.of(spec.typeParams().get(0), null, injector);
                setField(fVal, value, key, val, spec0);
                fVal.setAccessible(false);
            } else if (spec.isInstanceOf(Const.class)) {
                Const value = $.cast(f.get(host));
                Field fConst = Const.class.getDeclaredField("v");
                fConst.setAccessible(true);
                BeanSpec spec0 = BeanSpec.of(spec.typeParams().get(0), null, injector);
                setField(fConst, value, key, val, spec0);
                fConst.setAccessible(false);
            } else if (spec.isInstanceOf(Collection.class)) {
                BeanSpec spec0 = BeanSpec.of(spec.typeParams().get(0), null, injector);
                StringValueResolver resolver = resolverManager.resolver(spec0.rawType(), spec);
                if (null == resolver) {
                    logger.warn("Config[%s] field type[%s] not recognized", key, spec);
                } else {
                    Collection col = (Collection) injector.get(spec.rawType());
                    String[] sa = val.toString().split(",");
                    for (String s : sa) {
                        col.add(resolver.resolve(s));
                    }
                    f.set(host, col);
                }
            } else {
                StringValueResolver resolver = resolverManager.resolver(spec.rawType());
                if (null == resolver) {
                    logger.warn("Config[%s] field type[%s] not recognized", key, spec.rawType());
                } else {
                    f.set(host, resolver.resolve(val.toString()));
                }
            }
        }
    }

}
