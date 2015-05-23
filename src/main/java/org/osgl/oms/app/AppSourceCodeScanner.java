package org.osgl.oms.app;

/**
 * A {@code SourceCodeScanner} scan source code.
 * This function is required only when application is running
 * at dev mode
 */
public interface AppSourceCodeScanner extends AppCodeScanner {

    /**
     * Visit the source code line. The implementation shall
     * set the internal state by inspecting the line
     * @param lineNumber
     * @param line
     * @param className
     */
    void visit(int lineNumber, String line, String className);

    /**
     * After scanning of a certain {@link Source} has been
     * finished, framework will call this method to check if
     * further byte code scanning is needed on the
     * class
     * @return {@code true} if it needs further bytecode
     * scanning on the class
     */
    boolean triggerBytecodeScanning();
}
