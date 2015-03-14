package org.osgl.oms.app;

import org.osgl._;
import org.osgl.exception.NotAppliedException;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.oms.OMS;
import org.osgl.oms.asm.ClassReader;
import org.osgl.oms.asm.ClassWriter;
import org.osgl.oms.conf.AppConfig;
import org.osgl.oms.controller.bytecode.ControllerScanner;
import org.osgl.oms.controller.meta.ControllerClassMetaInfoHolder;
import org.osgl.oms.controller.meta.ControllerClassMetaInfo;
import org.osgl.oms.controller.meta.ControllerClassMetaInfoManager;
import org.osgl.oms.util.BytecodeVisitor;
import org.osgl.oms.util.Files;
import org.osgl.oms.util.Jars;
import org.osgl.oms.util.ClassNames;
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
public class AppClassLoader extends ClassLoader implements ControllerClassMetaInfoHolder {
    protected final static Logger logger = L.get(AppClassLoader.class);
    private App app;
    private Map<String, byte[]> libClsCache = C.newMap();
    private ControllerClassMetaInfoManager ctrlInfo =
            new ControllerClassMetaInfoManager(
                    new _.Factory<ControllerScanner>() {
                        @Override
                        public ControllerScanner create() {
                            return new ControllerScanner(app.config(), app.router(), bytecodeLookup);
                        }
                    }
            );

    public AppClassLoader(App app) {
        super(OMS.classLoader());
        this.app = notNull(app);
        preloadBytecode();
        scan();
    }

    protected App app() {
        return app;
    }

    public void detectChanges() {
        // don't do anything when running in none-dev mode
    }

    public boolean isSourceClass(String className) {
        return false;
    }

    public ControllerClassMetaInfo controllerClassMetaInfo(String controllerClassName) {
        return ctrlInfo.controllerMetaInfo(controllerClassName);
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

    protected void scan() {
        scanForActionMethods();
    }

    protected void scanForActionMethods() {
        AppConfig conf = app.config();
        for (String className : libClsCache.keySet()) {
            if (!conf.notControllerClass(className)) {
                ctrlInfo.scanForControllerMetaInfo(className);
            }
        }
        ctrlInfo.mergeActionMetaInfo();
    }

    protected void preloadBytecode() {
        preloadLib();
        preloadClasses();
    }

    private void preloadLib() {
        libClsCache.putAll(Jars.buildClassNameIndex(RuntimeDirs.lib(app), _F.SYS_CLASS_NAME));
    }

    private void preloadClasses() {
        File base = RuntimeDirs.classes(app);
        List<File> files = Files.filter(base, _F.SAFE_CLASS);
        for (File file : files) {
            preloadClassFile(base, file);
        }
    }

    protected void preloadClassFile(File base, File file) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream((int)file.length());
        IO.copy(IO.is(file), baos);
        byte[] bytes = baos.toByteArray();
        libClsCache.put(ClassNames.sourceFileNameToClassName(base, file.getAbsolutePath()), bytes);
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
        try {
            byte[] baNew = enhance(name, bytecode);
            Class<?> c = super.defineClass(name, baNew, 0, baNew.length, DOMAIN);
            if (resolve) {
                super.resolveClass(c);
            }
            return c;
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Exception e) {
            throw E.unexpected("Error processing class " + name);
        }
    }

    protected byte[] enhance(String className, byte[] bytecode) {
        return asmEnhance(className, bytecode);
    }

    private byte[] asmEnhance(String className, byte[] bytecode) {
        _.Var<ClassWriter> cw = _.var(null);
        BytecodeVisitor enhancer = OMS.enhancerManager().appEnhancer(app, className, cw);
        if (null == enhancer) {
            return bytecode;
        }
        cw.set(new ClassWriter(0));
        enhancer.commitDownstream();
        ClassReader r = new ClassReader(bytecode);
        r.accept(enhancer, 0);
        return cw.get().toByteArray();
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

    byte[] enhancedBytecode(String name) {
        byte[] bytecode = bytecode(name);
        return null == bytecode ? null : enhance(name, bytecode);
    }

    private _.F1<String, byte[]> bytecodeLookup = new _.F1<String, byte[]>() {
        @Override
        public byte[] apply(String s) throws NotAppliedException, _.Break {
            return libClsCache.get(s);
        }
    };

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
