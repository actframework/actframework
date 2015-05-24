package act.di.guice;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.osgl._;
import act.di.DependencyInjector;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.List;

/**
 * Implement {@link DependencyInjector}
 */
public class GuiceDependencyInjector implements DependencyInjector<GuiceDependencyInjector> {

    volatile Injector injector;
    List<Module> modules = C.newList();

    public GuiceDependencyInjector() {
    }

    public void addModule(Module module) {
        E.NPE(module);
        modules.add(module);
    }

    @Override
    public <T> T create(Class<T> clazz) {
        Injector injector = injector();
        if (null == injector) {
            return _.newInstance(clazz);
        } else {
            return injector.getInstance(clazz);
        }
    }

    private Injector injector() {
        if (modules.isEmpty()) {
            return null;
        }
        if (null == injector) {
            synchronized (this) {
                if (null == injector) {
                    injector = Guice.createInjector(modules);
                }
            }
        }
        return injector;
    }
}
