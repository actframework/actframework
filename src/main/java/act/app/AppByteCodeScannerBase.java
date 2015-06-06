package act.app;

import org.osgl.util.C;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Base class for all {@link AppByteCodeScanner} implementations
 */
public abstract class AppByteCodeScannerBase extends AppCodeScannerBase implements AppByteCodeScanner {
    
    private Map<Class<? extends AppByteCodeScanner>, Set<String>> dependencyClasses;

    protected final void reset() {
        dependencyClasses = C.newMap();
    }

    protected final void addDependencyClass(String className) {
        Set<String> set = dependencyClasses.get(getClass());
        if (null == set) {
            set = C.newSet();
            dependencyClasses.put(getClass(), set);
        }
        set.add(className);
    }

    protected final void addDependencyClassToScanner(Class<? extends AppByteCodeScanner> scannerClass, String className) {
        Set<String> set = dependencyClasses.get(scannerClass);
        if (null == set) {
            set = C.newSet();
            dependencyClasses.put(scannerClass, set);
        }
        set.add(className);
    }

    protected final void addDependencyClassToScanner(Class<? extends AppByteCodeScanner> scannerClass, Collection<String> classNames) {
        Set<String> set = dependencyClasses.get(scannerClass);
        if (null == set) {
            set = C.newSet();
            dependencyClasses.put(scannerClass, set);
        }
        set.addAll(classNames);
    }

    @Override
    public final Map<Class<? extends AppByteCodeScanner>, Set<String>> dependencyClasses() {
        return C.map(dependencyClasses);
    }

}
