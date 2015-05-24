package act.app;

import act.Act;
import act.asm.ClassReader;
import act.asm.ClassWriter;
import act.conf.AppConfig;
import act.controller.bytecode.ControllerScanner;
import act.controller.meta.ControllerClassMetaInfo;
import act.controller.meta.ControllerClassMetaInfoHolder;
import act.controller.meta.ControllerClassMetaInfoManager;
import act.controller.meta.ControllerClassMetaInfoManager2;
import act.util.ByteCodeVisitor;
import act.util.ClassNames;
import act.util.Files;
import act.util.Jars;
import org.osgl._;
import org.osgl.exception.NotAppliedException;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.osgl._.notNull;

/**
 * The top level class loader to load a specific application classes into JVM
 */
public class AppClassLoader extends ClassLoader implements ControllerClassMetaInfoHolder {
    protected final static Logger logger = L.get(AppClassLoader.class);
    private App app;
    private Map<String, byte[]> libClsCache = C.newMap();
    protected ControllerClassMetaInfoManager controllerInfo =
            new ControllerClassMetaInfoManager(
                    new _.Factory<ControllerScanner>() {
                        @Override
                        public ControllerScanner create() {
                            return new ControllerScanner(app.config(), app.router(), bytecodeLookup);
                        }
                    }
            );
    protected ControllerClassMetaInfoManager2 controllerInfo2 = new ControllerClassMetaInfoManager2();

    public AppClassLoader(App app) {
        super(Act.class.getClassLoader());
        this.app = notNull(app);
    }

//    protected void init() {
//        preload();
//        //scanByteCode();
//        scan2();
//    }

    protected App app() {
        return app;
    }

    public void detectChanges() {
        // don't do anything when running in none-dev mode
    }

    public ControllerClassMetaInfo controllerClassMetaInfo(String controllerClassName) {
        return controllerInfo2.controllerMetaInfo(controllerClassName);
    }

    public ControllerClassMetaInfoManager controllerClassMetaInfoManager() {
        return controllerInfo;
    }

    public ControllerClassMetaInfoManager2 controllerClassMetaInfoManager2() {
        return controllerInfo2;
    }

    public boolean isSourceClass(String className) {
        return false;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> c = findLoadedClass(name);
        if (c != null) {
            return c;
        }
        c = getParent().loadClass(name);
        if (null != null) {
            return c;
        }

        c = loadAppClass(name, resolve);

        if (null == c) {
            return super.loadClass(name, resolve);
        } else {
            return c;
        }
    }

    @Deprecated
    protected void scan() {
        scanForActionMethods();
    }

    protected void scan2() {
        scanByteCode(libClsCache.keySet(), bytecodeLookup);
    }

    /**
     * This method implement a event listener based scan process:
     * <ol>
     *     <li>First loop: through all cached bytecode. Chain all scanner's bytecode visitor</li>
     *     <li>Rest loops: through dependencies. Thus if some bytecode missed by a certain scanner
     *     due to the context is not established can be captured eventually</li>
     * </ol>
     */
    protected void scanByteCode(Iterable<String> classes, _.Function<String, byte[]> bytecodeProvider) {
        logger.debug("start to scan bytecode ...");
        final AppCodeScannerManager scannerManager = app().scannerManager();
        Map<String, List<AppByteCodeScanner>> dependencies = C.newMap();
        for (String className : classes) {
            logger.debug("scanning %s ...", className);
            dependencies.remove(className);
            byte[] ba = bytecodeProvider.apply(className);
            List<ByteCodeVisitor> visitors = C.newList();
            List<AppByteCodeScanner> scanners = C.newList();
            for (AppByteCodeScanner scanner : scannerManager.byteCodeScanners()) {
                if (scanner.start(className)) {
                    logger.debug("scanner %s added to the list", scanner);
                    visitors.add(scanner.byteCodeVisitor());
                    scanners.add(scanner);
                }
            }
            if (visitors.isEmpty()) {
                continue;
            }
            ByteCodeVisitor theVisitor = ByteCodeVisitor.chain(visitors);
            ClassReader cr = new ClassReader(ba);
            cr.accept(theVisitor, 0);
            for (AppByteCodeScanner scanner : scanners) {
                scanner.scanFinished(className);
                Set<String> ss = scanner.dependencyClasses();
                if (ss.isEmpty()) {
                    logger.debug("no dependencies found for %s by scanner %s", className, scanner);
                    continue;
                }
                for (String dependencyClass : ss) {
                    logger.debug("dependencies[%s] found for %s by scanner %s", dependencyClass, className, scanner);
                    List<AppByteCodeScanner> l = dependencies.get(dependencyClass);
                    if (null == l) {
                        l = C.newList();
                        dependencies.put(dependencyClass, l);
                    }
                    if (!l.contains(scanner)) l.add(scanner);
                }
            }
        }
        // loop through dependencies until it's all processed
        while (!dependencies.isEmpty()) {
            String className = dependencies.keySet().iterator().next();
            List<AppByteCodeScanner> scanners = dependencies.remove(className);
            List<ByteCodeVisitor> visitors = C.newList();
            for (AppByteCodeScanner scanner: scanners) {
                scanner.start(className);
                visitors.add(scanner.byteCodeVisitor());
            }
            ByteCodeVisitor theVisitor = ByteCodeVisitor.chain(visitors);
            ClassReader cr = new ClassReader(libClsCache.get(className));
            cr.accept(theVisitor, 0);
            for (AppByteCodeScanner scanner : scanners) {
                scanner.scanFinished(className);
                Set<String> ss = scanner.dependencyClasses();
                if (ss.isEmpty()) {
                    continue;
                }
                for (String dependencyClass : ss) {
                    List<AppByteCodeScanner> l = dependencies.get(dependencyClass);
                    if (null == l) {
                        l = C.newList();
                        dependencies.put(dependencyClass, l);
                    }
                    if (!l.contains(scanner)) l.add(scanner);
                }
            }
        }
    }

    @Deprecated
    protected void scanForActionMethods() {
        AppConfig conf = app.config();
        for (String className : libClsCache.keySet()) {
            if (conf.possibleControllerClass(className)) {
                controllerInfo.scanForControllerMetaInfo(className);
            }
        }
        controllerInfo.mergeActionMetaInfo();
    }

    protected void scanForActionMethods(String className) {
        controllerInfo.scanForControllerMetaInfo(className);
        controllerInfo.mergeActionMetaInfo();
    }

    protected void preload() {
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
        ByteArrayOutputStream baos = new ByteArrayOutputStream((int) file.length());
        IO.copy(IO.is(file), baos);
        byte[] bytes = baos.toByteArray();
        libClsCache.put(ClassNames.sourceFileNameToClassName(base, file.getAbsolutePath().replace(".class", ".java")), bytes);
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
        ByteCodeVisitor enhancer = Act.enhancerManager().appEnhancer(app, className, cw);
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
            return bytes;
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
            return appBytecode(s);
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
