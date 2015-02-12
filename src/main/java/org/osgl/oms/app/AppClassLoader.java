package org.osgl.oms.app;

import org.osgl._;
import org.osgl.oms.OMS;
import org.osgl.oms.asm.ClassReader;
import org.osgl.oms.asm.ClassWriter;
import org.osgl.oms.util.Files;
import org.osgl.oms.util.Jars;
import org.osgl.oms.util.Names;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.osgl._.notNull;

/**
 * The top level class loader to load a specific application classes into JVM
 */
public class AppClassLoader extends ClassLoader {
    private App app;
    private Map<String, byte[]> libClsCache = C.newMap();

    public AppClassLoader(App app) {
        super(OMS.classLoader());
        this.app = notNull(app);
        preload();
    }

    public void detectChanges() {
        // don't do anything when running in none-dev mode
    }

    public boolean isAppClass(String className) {
        return false;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> c = findLoadedClass(name);
        if (c != null) {
            return c;
        }

        c = loadAppClass(name, resolve);

        if (null == c) {
            return super.loadClass(name, resolve);
        } else {
            return c;
        }
    }

    protected App app() {
        return app;
    }

    private void preload() {
        preloadLib();
        preloadClasses();
    }

    private void preloadLib() {
        libClsCache.putAll(Jars.buildClassNameIndex(RuntimeDirs.lib(app), _F.SYS_CLASS_NAME));
    }

    private void preloadClasses() {
        List<File> files = Files.filter(RuntimeDirs.classes(app), _F.SAFE_CLASS);
        for (File file: files) {
            preloadClassFile(file);
        }
    }

    protected void preloadClassFile(File file) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream((int)file.length());
        IO.copy(IO.is(file), baos);
        libClsCache.put(Names.fileToClass(file.getName()), baos.toByteArray());
    }

    private Class<?> loadAppClass(String name, boolean resolve) {
        byte[] bytecode = appBytecode(name);
        if (null == bytecode) return null;
        if (!app().config().needEnhancement(name)) {
            Class<?> c = super.defineClass(name, bytecode, 0, bytecode.length, DOMAIN);
            if (resolve) {
                super.resolveClass(c);
            }
            return c;
        }
        Class<?> c;
        ClassReader r;
        r = new ClassReader(bytecode);
        try {
            ClassWriter w = new ClassWriter(0);
            // TODO: inject class visitor here
            r.accept(w, 0);
            byte[] baNew = w.toByteArray();
            c = super.defineClass(name, baNew, 0, baNew.length, DOMAIN);
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Exception e) {
            throw E.unexpected("Error processing class " + name);
        }
        if (resolve) {
            super.resolveClass(c);
        }
        return c;
    }

    protected byte[] appBytecode(String name) {
        return libClsCache.get(name);
    }

    protected byte[] bytecode(String name) {
        byte[] bytes = appBytecode(name);
        if (null != bytes) {
            return  bytes;
        }
        name = name.replace('.', '/') + ".class";
        InputStream is = getParent().getResourceAsStream(name);
        if (null == is) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IO.copy(is, baos);
        return baos.toByteArray();
    }

    private static java.security.ProtectionDomain DOMAIN;

    static {
        DOMAIN = (java.security.ProtectionDomain)
                java.security.AccessController.doPrivileged(
                        new java.security.PrivilegedAction() {
                            public Object run() {
                                return AppClassLoader.class.getProtectionDomain();
                            }
                        });
    }

    private static enum _F {
        ;
        static _.Predicate<String> SYS_CLASS_NAME = new _.Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.startsWith("java") || s.startsWith("org.osgl.");
            }
        };
        static _.Predicate<String> SAFE_CLASS = S.F.endsWith(".class").and(SYS_CLASS_NAME.negate());
    }
}
