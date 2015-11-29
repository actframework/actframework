package act.app.conf;

import act.ActComponent;
import act.app.App;
import act.app.AppByteCodeScanner;
import act.app.AppClassLoader;
import act.controller.bytecode.ControllerByteCodeScanner;
import act.util.SubTypeFinder;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.util.C;

import java.util.Map;
import java.util.Set;

/**
 * {@code AppConfigPlugin} scan source code or byte code to detect if there are
 * any user defined {@link AppConfigurator} implementation and use it to populate
 * {@link act.conf.AppConfig} default values
 */
@ActComponent
public class AppConfigPlugin extends SubTypeFinder {
    public AppConfigPlugin() {
        super(true, false, AppConfigurator.class, new $.F2<App, String, Map<Class<? extends AppByteCodeScanner>, Set<String>>>() {
            @Override
            public Map<Class<? extends AppByteCodeScanner>, Set<String>> apply(App app, String className) throws NotAppliedException, $.Break {
                AppConfiguratorClassLoader cl = new AppConfiguratorClassLoader(app.classLoader());
                Class<? extends AppConfigurator> c = $.classForName(className, cl);
                AppConfigurator<?> conf = $.newInstance(c);
                conf.app(app);
                conf.configure();
                app.config()._merge(conf);
                Set<String> controllerClasses = conf.controllerClasses();
                conf.destroy();
                return C.newMap(ControllerByteCodeScanner.class, controllerClasses);
            }
        });
    }

    @Override
    public boolean load() {
        return true;
    }

    @ActComponent
    private static class AppConfiguratorClassLoader extends AppClassLoader {
        AppClassLoader p;
        protected AppConfiguratorClassLoader(AppClassLoader parent) {
            super(parent.app());
            p = parent;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            Class<?> c = p.loadedClass(name);
            if (null != c) return c;
            return loadClass(name, true);
        }

    }
}