package act.app;

import act.Act;
import act.asm.ClassReader;
import act.asm.ClassWriter;
import act.boot.BootstrapClassLoader;
import act.controller.meta.ControllerClassMetaInfo;
import act.controller.meta.ControllerClassMetaInfoHolder;
import act.controller.meta.ControllerClassMetaInfoManager;
import act.job.meta.JobClassMetaInfo;
import act.job.meta.JobClassMetaInfoManager;
import act.mail.meta.MailerClassMetaInfo;
import act.mail.meta.MailerClassMetaInfoHolder;
import act.mail.meta.MailerClassMetaInfoManager;
import act.util.*;
import org.osgl._;
import org.osgl.exception.NotAppliedException;
import org.osgl.exception.UnexpectedException;
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
public class AppClassLoader
        extends ClassLoader
        implements ControllerClassMetaInfoHolder, MailerClassMetaInfoHolder, AppService<AppClassLoader>, ActClassLoader {
    protected final static Logger logger = L.get(AppClassLoader.class);
    private App app;
    private Map<String, byte[]> libClsCache = C.newMap();
    private ClassInfoRepository classInfoRepository;
    private boolean destroyed;
    protected ControllerClassMetaInfoManager controllerInfo = new ControllerClassMetaInfoManager();
    protected MailerClassMetaInfoManager mailerInfo = new MailerClassMetaInfoManager();
    protected JobClassMetaInfoManager jobInfo = new JobClassMetaInfoManager();

    public AppClassLoader(App app) {
        super(Act.class.getClassLoader());
        this.app = notNull(app);
        ClassInfoRepository actClassInfoRepository = Act.classInfoRepository();
        if (null != actClassInfoRepository) {
            this.classInfoRepository = new AppClassInfoRepository(app, actClassInfoRepository);
        }
    }

    @Override
    public final boolean isDestroyed() {
        return destroyed;
    }

    @Override
    public AppClassLoader app(App app) {
        throw E.unsupport();
    }


    public final App app() {
        return app;
    }

    @Override
    public final void destroy() {
        libClsCache.clear();
        controllerInfo.destroy();
        mailerInfo.destroy();
        jobInfo.destroy();
        releaseResources();
        destroyed = true;
    }

    @Override
    public ClassInfoRepository classInfoRepository() {
        return classInfoRepository;
    }

    protected void releaseResources() {
        classInfoRepository.destroy();
    }

    public void detectChanges() {
        // don't do anything when running in none-dev mode
    }

    public ControllerClassMetaInfo controllerClassMetaInfo(String controllerClassName) {
        return controllerInfo.controllerMetaInfo(controllerClassName);
    }

    public ControllerClassMetaInfoManager controllerClassMetaInfoManager() {
        return controllerInfo;
    }

    @Override
    public MailerClassMetaInfo mailerClassMetaInfo(String className) {
        return mailerInfo.mailerMetaInfo(className);
    }

    public MailerClassMetaInfoManager mailerClassMetaInfoManager() {
        return mailerInfo;
    }

    public JobClassMetaInfo jobClassMetaInfo(String jobClassName) {
        return jobInfo.jobMetaInfo(jobClassName);
    }

    public JobClassMetaInfoManager jobClassMetaInfoManager() {
        return jobInfo;
    }

    public boolean isSourceClass(String className) {
        return false;
    }

    public Class<?> loadedClass(String name) {
        Class<?> c = findLoadedClass(name);
        if (null == c) {
            ClassLoader p = getParent();
            if (null != p && (p instanceof ActClassLoader || p instanceof BootstrapClassLoader)) {
                return ((ActClassLoader)p).loadedClass(name);
            }
        }
        return c;
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
            if (null == ba) {
                throw new NullPointerException();
            }
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
            try {
                cr.accept(theVisitor, 0);
            } catch (UnexpectedException e) {
                Throwable t = e.getCause();
                if (t instanceof ClassNotFoundException) {
                    continue;
                } else {
                    throw e;
                }
            }
            for (AppByteCodeScanner scanner : scanners) {
                scanner.scanFinished(className);
                Map<Class<? extends AppByteCodeScanner>, Set<String>> ss = scanner.dependencyClasses();
                if (ss.isEmpty()) {
                    logger.debug("no dependencies found for %s by scanner %s", className, scanner);
                    continue;
                }
                for (Class<? extends AppByteCodeScanner> scannerClass : ss.keySet()) {
                    AppByteCodeScanner scannerA = scannerManager.byteCodeScannerByClass(scannerClass);
                    for (String dependencyClass : ss.get(scannerClass)) {
                        logger.debug("dependencies[%s] found for %s by scanner %s", dependencyClass, className, scannerA);
                        List<AppByteCodeScanner> l = dependencies.get(dependencyClass);
                        if (null == l) {
                            l = C.newList();
                            dependencies.put(dependencyClass, l);
                        }
                        if (!l.contains(scanner)) l.add(scannerA);
                    }
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
            ClassReader cr = new ClassReader(bytecodeProvider.apply(className));
            cr.accept(theVisitor, 0);
            for (AppByteCodeScanner scanner : scanners) {
                scanner.scanFinished(className);
                Map<Class<? extends AppByteCodeScanner>, Set<String>> ss = scanner.dependencyClasses();
                if (ss.isEmpty()) {
                    logger.debug("no dependencies found for %s by scanner %s", className, scanner);
                    continue;
                }
                for (Class<? extends AppByteCodeScanner> scannerClass : ss.keySet()) {
                    AppByteCodeScanner scannerA = scannerManager.byteCodeScannerByClass(scannerClass);
                    for (String dependencyClass : ss.get(scannerClass)) {
                        logger.debug("dependencies[%s] found for %s by scanner %s", dependencyClass, className, scannerA);
                        List<AppByteCodeScanner> l = dependencies.get(dependencyClass);
                        if (null == l) {
                            l = C.newList();
                            dependencies.put(dependencyClass, l);
                        }
                        if (!l.contains(scanner)) l.add(scannerA);
                    }
                }
            }
        }
    }

    protected void preload() {
        preloadLib();
        preloadClasses();
    }

    private void preloadLib() {
        libClsCache.putAll(Jars.buildClassNameIndex(RuntimeDirs.lib(app), app().config().appClassTester().negate()));
    }

    protected void preloadClasses() {
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

    protected byte[] loadAppClassFromDisk(String name) {
        File base = new File(app().layout().target(app().base()), "classes");
        if (base.canRead() && base.isDirectory()) {
            String path = ClassNames.classNameToClassFileName(name);
            File classFile = new File(base, path);
            if (classFile.canRead()) {
                return IO.readContent(classFile);
            }
        }
        return null;
    }

    private Class<?> loadAppClass(String name, boolean resolve) {
        byte[] bytecode = appBytecode(name);
        if (null == bytecode) {
            bytecode = loadAppClassFromDisk(name);
            if (null == bytecode) return null;
        }
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
        if (!enhanceEligible(className)) return bytecode;
        _.Var<ClassWriter> cw = _.var(null);
        ByteCodeVisitor enhancer = Act.enhancerManager().appEnhancer(app, className, cw);
        if (null == enhancer) {
            return bytecode;
        }
        cw.set(new ClassWriter(ClassWriter.COMPUTE_MAXS));
        enhancer.commitDownstream();
        ClassReader r = new ClassReader(bytecode);
        r.accept(enhancer, 0);
        return cw.get().toByteArray();
    }

    protected byte[] appBytecode(String name) {
        return appBytecode(name, true);
    }

    protected byte[] appBytecode(String name, boolean loadFromSource) {
        return libClsCache.get(name);
    }

    protected byte[] bytecode(String name) {
        return bytecode(name, true);
    }

    protected byte[] bytecode(String name, boolean compileSource) {
        byte[] bytes = appBytecode(name, compileSource);
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
        byte[] bytecode = bytecode(name, false);
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

    protected static boolean enhanceEligible(String name) {
        boolean sys = name.startsWith("java") || name.startsWith("com.google") || name.startsWith("org.apache") || name.startsWith("org.springframework");
        return !sys;
    }
}
