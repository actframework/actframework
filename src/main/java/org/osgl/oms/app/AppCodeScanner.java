package org.osgl.oms.app;

/**
 * Capture commonality between {@link AppSourceCodeScanner} and {@link AppByteCodeScanner}
 */
public interface AppCodeScanner {

    void setApp(App app);

    /**
     * Reset the scanner internal state to start an new scanning session.
     * Returns {@code false} if the {@link org.osgl.oms.app.Source} is
     * not subject to scanning as per class name
     */
    boolean start(String className);
}
