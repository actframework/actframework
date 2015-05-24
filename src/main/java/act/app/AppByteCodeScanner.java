package act.app;

import act.util.ByteCodeVisitor;

import java.util.Set;

/**
 * A {@code AppByteCodeScanner} scans application bytecode
 */
public interface AppByteCodeScanner extends AppCodeScanner {
    /**
     * Returns the {@link ByteCodeVisitor}
     */
    ByteCodeVisitor byteCodeVisitor();

    /**
     * After visiting a class bytecode, this method will be called
     * to check if there are dependency class needs to be scanned
     * again
     */
    Set<String> dependencyClasses();

    /**
     * Called when scanning for one class finished
     */
    void scanFinished(String className);

    /**
     * Called when scanning for all classes finished
     */
    void allScanFinished();
}
