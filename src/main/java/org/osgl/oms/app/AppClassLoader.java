package org.osgl.oms.app;

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
import java.util.List;
import java.util.Map;

import static org.osgl._.notNull;

/**
 * The top level class loader to load a specific application classes into JVM
 */
public class AppClassLoader extends ClassLoader {
    private App app;

    private Map<String, byte[]> libClsCache = C.newMap();
    private Map<String, Source> srcIdx = C.newMap();

    public AppClassLoader(App app) {
        super(OMS.classLoader());
        this.app = notNull(app);
        preload();
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

    private void preload() {
        preloadLib();
        preloadClasses();
    }

    private void preloadLib() {
        libClsCache.putAll(Jars.buildClassNameIndex(RuntimeDirs.lib(app)));
    }

    private void preloadClasses() {
        if (OMS.isDev()) {
            loadSourceBytecodes();
        } else {
            loadCompiledBytecodes();
        }
    }

    private void loadSourceBytecodes() {
        E.tbd("load classes from source code");
    }

    private void loadCompiledBytecodes() {
        List<File> files = Files.filter(RuntimeDirs.classes(app), S.F.endsWith(".class"));
        for (File file: files) {
            preloadClassFile(file);
        }
    }

    private void preloadClassFile(File file) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream((int)file.length());
        IO.copy(IO.is(file), baos);
        libClsCache.put(Names.fileToClass(file.getName()), baos.toByteArray());
    }

    private Class<?> loadAppClass(String name, boolean resolve) {
        byte[] bytecode = libClsCache.get(name);
        if (null == bytecode) {
            // TODO: check source class cache
            return null;
        }

        Class<?> c = null;
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

}
