package org.osgl.oms.cls;

/**
 * The {@code SourceCodeLoader} load byte code dynamically compiled
 * from source code into JVM
 */
public class SourceCodeLoader extends ClassLoader {
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return null;
    }
}
