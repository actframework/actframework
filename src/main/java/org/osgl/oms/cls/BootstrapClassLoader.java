package org.osgl.oms.cls;

import org.osgl.oms.asm.ClassReader;
import org.osgl.oms.asm.ClassWriter;
import org.osgl.oms.util.Jars;
import org.osgl.util.C;
import org.osgl.util.E;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This class loader is responsible for loading OMS classes
 */
public class BootstrapClassLoader extends ClassLoader {

    public static final String OMS_HOME = "OMS_HOME";
    private static final String ASM_PKG = "org.osgl.oms.asm.";

    private File lib;
    private File plugin;

    private Map<String, byte[]> libBC = C.newMap();
    private Map<String, byte[]> pluginBC = C.newMap();

    public BootstrapClassLoader(ClassLoader parent) {
        super(parent);
        init();
    }

    public BootstrapClassLoader() {
        this(_getParent());
    }

    private static ClassLoader _getParent() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (null == cl) {
            cl = ClassLoader.getSystemClassLoader();
        }
        return cl;
    }

    private void init() {
        String omsHome = System.getProperty(OMS_HOME);
        if (null == omsHome) {
            omsHome = System.getenv(OMS_HOME);
        }
        if (null == omsHome) {
            omsHome = guessHome();
        }
        File home = new File(omsHome);
        lib = new File(home, "lib");
        plugin = new File(home, "plugin");
        verifyHome();
        buildIndex();
    }

    private String guessHome() {
        return new File(".").getAbsolutePath();
    }

    private void verifyHome() {
        if (!verifyDir(lib) || !verifyDir(plugin)) {
            throw E.unexpected("Cannot load OMS: can't find lib or plugin dir");
        }
    }

    private boolean verifyDir(File dir) {
        return dir.exists() && dir.canExecute() && dir.isDirectory();
    }

    private void buildIndex() {
        libBC.putAll(Jars.buildClassNameIndex(lib));
        pluginBC.putAll(Jars.buildClassNameIndex(plugin));
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> c = findLoadedClass(name);
        if (c != null) {
            return c;
        }

        if (!protectedClasses.contains(name)) {
            c = loadOmsClass(name, resolve);
        }

        if (null == c) {
            return super.loadClass(name, resolve);
        } else {
            return c;
        }
    }

    public Iterator<byte[]> pluginBytecodes() {
        return pluginBC.values().iterator();
    }

    private Class<?> loadOmsClass(String name, boolean resolve) throws ClassNotFoundException {
        byte[] ba = libBC.get(name);
        if (null == ba) {
            ba = pluginBC.get(name);
        }

        if (null == ba) {
            return null;
        }

        if (name.startsWith(ASM_PKG)) {
            // skip bytecode enhancement for asm classes
            return super.defineClass(name, ba, 0, ba.length, DOMAIN);
        }

        ClassReader r;
        r = new ClassReader(ba);
        try {
            ClassWriter w = new ClassWriter(0);
            // TODO: inject class visitor here
            r.accept(w, 0);
            byte[] baNew = w.toByteArray();
            return super.defineClass(name, baNew, 0, baNew.length, DOMAIN);
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Exception e) {
            throw E.unexpected("Error processing class " + name);
        }
    }

    public Class<?> createClass(String name, byte[] b) throws ClassFormatError {
        return super.defineClass(name, b, 0, b.length, DOMAIN);
    }

    private static java.security.ProtectionDomain DOMAIN;

    static {
        DOMAIN = (java.security.ProtectionDomain)
                java.security.AccessController.doPrivileged(
                        new java.security.PrivilegedAction() {
                            public Object run() {
                                return BootstrapClassLoader.class.getProtectionDomain();
                            }
                        });
    }

    private static final Set<String> protectedClasses = C.set(
            BootstrapClassLoader.class.getName()
            //Plugin.class.getName(),
            //ClassFilter.class.getName()
    );
}
