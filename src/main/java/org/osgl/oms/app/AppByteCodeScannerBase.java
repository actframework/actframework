package org.osgl.oms.app;

import org.osgl.util.C;
import org.osgl.util.E;

import java.util.List;
import java.util.Set;

/**
 * Base class for all {@link AppByteCodeScanner} implementations
 */
public abstract class AppByteCodeScannerBase extends AppCodeScannerBase implements AppByteCodeScanner {

    private Set<String> dependencyClasses;

    protected final void reset0() {
        dependencyClasses = C.newSet();
    }

    protected final void addDependencyClass(String className) {
        dependencyClasses.add(className);
    }

    @Override
    public final Set<String> dependencyClasses() {
        return C.set(dependencyClasses);
    }

}
