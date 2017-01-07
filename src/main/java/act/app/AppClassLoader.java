package act.app;

import act.Act;
import act.app.event.AppEventId;
import act.asm.ClassReader;
import act.asm.ClassWriter;
import act.boot.BootstrapClassLoader;
import act.boot.app.FullStackAppBootstrapClassLoader;
import act.cli.meta.CommanderClassMetaInfo;
import act.cli.meta.CommanderClassMetaInfoHolder;
import act.cli.meta.CommanderClassMetaInfoManager;
import act.conf.AppConfig;
import act.controller.meta.ControllerClassMetaInfo;
import act.controller.meta.ControllerClassMetaInfoHolder;
import act.controller.meta.ControllerClassMetaInfoManager;
import act.event.AppEventListenerBase;
import act.job.meta.JobClassMetaInfo;
import act.job.meta.JobClassMetaInfoManager;
import act.mail.meta.MailerClassMetaInfo;
import act.mail.meta.MailerClassMetaInfoHolder;
import act.mail.meta.MailerClassMetaInfoManager;
import act.metric.Metric;
import act.metric.MetricInfo;
import act.util.*;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.exception.UnexpectedException;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.S;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.security.ProtectionDomain;
import java.util.*;

import static act.util.ClassInfoRepository.canonicalName;
import static org.osgl.$.notNull;

/**
 * The top level class loader to load a specific application classes into JVM
 */
@ApplicationScoped
public class AppClassLoader
        extends ClassLoader
        implements
            ControllerClassMetaInfoHolder,
            CommanderClassMetaInfoHolder,
            MailerClassMetaInfoHolder,
            AppService<AppClassLoader>,
            ActClassLoader {

    private final static Logger logger = L.get(AppClassLoader.class);
    private App app;
    private Map<String, byte[]> libClsCache = C.newMap();
    private ClassInfoRepository classInfoRepository;
    private boolean destroyed;
    protected ControllerClassMetaInfoManager controllerInfo;
    protected MailerClassMetaInfoManager mailerInfo = new MailerClassMetaInfoManager();
    protected CommanderClassMetaInfoManager commanderInfo = new CommanderClassMetaInfoManager();
    protected JobClassMetaInfoManager jobInfo = new JobClassMetaInfoManager();
    protected SimpleBean.MetaInfoManager simpleBeanInfo;
    protected Metric metric = Act.metricPlugin().metric(MetricInfo.CLASS_LOADING);

    @Inject
    public AppClassLoader(final App app) {
        super(Act.class.getClassLoader());
        this.app = notNull(app);
        ClassInfoRepository actClassInfoRepository = Act.classInfoRepository();
        if (null != actClassInfoRepository) {
            this.classInfoRepository = new AppClassInfoRepository(app, actClassInfoRepository);
        }
        controllerInfo = new ControllerClassMetaInfoManager(app);
        if (null == app.eventBus()) {
            return; // for unit test only
        }
        simpleBeanInfo  = new SimpleBean.MetaInfoManager(this);
        app.eventBus().bind(AppEventId.APP_CODE_SCANNED, new AppEventListenerBase() {

            @Override
            public String id() {
                return "appClassLoader:controllerInfo:mergeActionMetaInfo";
            }

            @Override
            public void on(EventObject event) throws Exception {
                controllerInfo.mergeActionMetaInfo(app);
            }
        });
    }

    @Override
    public final boolean isDestroyed() {
        return destroyed;
    }

    @Override
    public Class<? extends Annotation> scope() {
        return ApplicationScoped.class;
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
        simpleBeanInfo.destroy();
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

    public CommanderClassMetaInfo commanderClassMetaInfo(String commanderClassName) {
        return commanderInfo.commanderMetaInfo(commanderClassName);
    }

    public CommanderClassMetaInfoManager commanderClassMetaInfoManager() {
        return commanderInfo;
    }

    public SimpleBean.MetaInfoManager simpleBeanInfoManager() {
        return simpleBeanInfo;
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

    public SimpleBean.MetaInfo simpleBeanMetaInfo(String className) {
        return simpleBeanInfo.get(className);
    }

    public boolean isSourceClass(String className) {
        return false;
    }

    public Class<?> loadedClass(String name) {
        Class<?> c = findLoadedClass(name);
        if (null == c) {
            ClassLoader p = getParent();
            if (null != p && (p instanceof ActClassLoader || p instanceof BootstrapClassLoader)) {
                return ((ActClassLoader) p).loadedClass(name);
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

        // ensure we can enhance the act classes specified below
        if (name.startsWith("act.") && name.endsWith("Admin")) {
            return super.loadClass(name, resolve);
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
     * <li>First loop: through all cached bytecode. Chain all scanner's bytecode visitor</li>
     * <li>Rest loops: through dependencies. Thus if some bytecode missed by a certain scanner
     * due to the context is not established can be captured eventually</li>
     * </ol>
     */
    protected void scanByteCode(Iterable<String> classes, $.Function<String, byte[]> bytecodeProvider) {
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
            libClsCache.put(className, ba);
            act.metric.Timer timer = metric.startTimer("act:classload:scan:bytecode:" + className);
            List<ByteCodeVisitor> visitors = C.newList();
            List<AppByteCodeScanner> scanners = C.newList();
            for (AppByteCodeScanner scanner : scannerManager.byteCodeScanners()) {
                if (scanner.start(className)) {
                    //logger.trace("scanner %s added to the list", scanner.getClass().getName());
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
                    //logger.trace("no dependencies found for %s by scanner %s", className, scanner);
                    continue;
                }
                for (Class<? extends AppByteCodeScanner> scannerClass : ss.keySet()) {
                    AppByteCodeScanner scannerA = scannerManager.byteCodeScannerByClass(scannerClass);
                    for (String dependencyClass : ss.get(scannerClass)) {
                        logger.trace("dependencies[%s] found for %s by scanner %s", dependencyClass, className, scannerA);
                        List<AppByteCodeScanner> l = dependencies.get(dependencyClass);
                        if (null == l) {
                            l = C.newList();
                            dependencies.put(dependencyClass, l);
                        }
                        if (!l.contains(scanner)) l.add(scannerA);
                    }
                }
            }
            timer.stop();
        }
        // loop through dependencies until it's all processed
        while (!dependencies.isEmpty()) {
            String className = dependencies.keySet().iterator().next();
            act.metric.Timer timer = metric.startTimer("act:classload:scan:bytecode:" + className);
            List<AppByteCodeScanner> scanners = dependencies.remove(className);
            List<ByteCodeVisitor> visitors = C.newList();
            for (AppByteCodeScanner scanner : scanners) {
                scanner.start(className);
                visitors.add(scanner.byteCodeVisitor());
            }
            ByteCodeVisitor theVisitor = ByteCodeVisitor.chain(visitors);
            byte[] bytes = bytecodeProvider.apply(className);
            libClsCache.put(className, bytes);
            ClassReader cr = new ClassReader(bytes);
            cr.accept(theVisitor, 0);
            for (AppByteCodeScanner scanner : scanners) {
                scanner.scanFinished(className);
                Map<Class<? extends AppByteCodeScanner>, Set<String>> ss = scanner.dependencyClasses();
                if (ss.isEmpty()) {
                    logger.trace("no dependencies found for %s by scanner %s", className, scanner);
                    continue;
                }
                for (Class<? extends AppByteCodeScanner> scannerClass : ss.keySet()) {
                    AppByteCodeScanner scannerA = scannerManager.byteCodeScannerByClass(scannerClass);
                    for (String dependencyClass : ss.get(scannerClass)) {
                        logger.trace("dependencies[%s] found for %s by scanner %s", dependencyClass, className, scannerA);
                        List<AppByteCodeScanner> l = dependencies.get(dependencyClass);
                        if (null == l) {
                            l = C.newList();
                            dependencies.put(dependencyClass, l);
                        }
                        if (!l.contains(scanner)) l.add(scannerA);
                    }
                }
            }
            timer.stop();
        }
    }

    protected void preload() {
        preloadLib();
        preloadClasses();
    }

    private void preloadLib() {
        final Map<String, byte[]> bytecodeIdx = C.newMap();
        final Map<String, Properties> jarConf = C.newMap();
        final $.Function<String, Boolean> ignoredClassNames = app().config().appClassTester().negate();
        Jars.F.JarEntryVisitor classNameIndexBuilder = Jars.F.classNameIndexBuilder(bytecodeIdx, ignoredClassNames);
        Jars.F.JarEntryVisitor confIndexBuilder = Jars.F.appConfigFileIndexBuilder(jarConf);
        List<File> jars = FullStackAppBootstrapClassLoader.jars(AppClassLoader.class.getClassLoader());
        for (File jar : jars) {
            Jars.scan(jar, classNameIndexBuilder, confIndexBuilder);
        }
        libClsCache.putAll(bytecodeIdx);
        AppConfig config = app().config();
        config.loadJarProperties(jarConf);
    }

    void loadClasses() {
        for (String key : libClsCache.keySet()) {
            try {
                Class<?> c = loadClass(key, true);
                cache(c);
            } catch (Exception e) {
                logger.warn(e, "error loading class");
            }
        }
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

    public Class<?> defineClass(String name, byte[] b, int off, int len, boolean resolve) {
        Class<?> c = super.defineClass(name, b, off, len, DOMAIN);
        if (resolve) {
            super.resolveClass(c);
        }
        return c;
    }

    private Class<?> loadAppClass(String name, boolean resolve) throws ClassNotFoundException {
        byte[] bytecode = appBytecode(name);
        if (null == bytecode) {
            bytecode = loadAppClassFromDisk(name);
            if (null == bytecode) return null;
        }
        if (!app().config().needEnhancement(name)) {
            Class<?> c;
            if (name.contains("$")) {
                return super.loadClass(name, resolve);
            } else {
                c = super.defineClass(name, bytecode, 0, bytecode.length, DOMAIN);
                if (resolve) {
                    super.resolveClass(c);
                }
                return c;
            }
        }
        try {
            byte[] baNew = enhance(name, bytecode);
            try {
                Class<?> c = super.defineClass(name, baNew, 0, baNew.length, DOMAIN);

                if (resolve) {
                    super.resolveClass(c);
                }
                return c;
            } catch (VerifyError e) {
                File f = File.createTempFile(name, ".class");
                IO.write(baNew, f);
                throw e;
            }
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
        $.Var<ClassWriter> cw = $.var(null);
        ByteCodeVisitor enhancer = Act.enhancerManager().appEnhancer(app, className, cw);
        if (null == enhancer) {
            return bytecode;
        }
        cw.set(new ClassWriter(ClassWriter.COMPUTE_FRAMES));
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

    private synchronized ClassNode cache(Class<?> c) {
        String cname = canonicalName(c);
        if (null == cname) {
            return null;
        }
        ClassInfoRepository repo = classInfoRepository();
        if (repo.has(cname)) {
            return repo.node(cname);
        }
        String name = c.getName();
        ClassNode node = repo.node(name, cname);
        node.modifiers(c.getModifiers());
        Class[] ca = c.getInterfaces();
        for (Class pc : ca) {
            if (pc == Object.class) continue;
            String pcname = canonicalName(pc);
            if (null != pcname) {
                cache(pc);
                node.addInterface(pcname);
            }
        }
        Class pc = c.getSuperclass();
        if (null != pc && Object.class != pc) {
            String pcname = canonicalName(pc);
            if (null != pcname) {
                cache(pc);
                node.parent(pcname);
            }
        }
        return node;
    }

    private $.F1<String, byte[]> bytecodeLookup = new $.F1<String, byte[]>() {
        @Override
        public byte[] apply(String s) throws NotAppliedException, $.Break {
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

    private enum _F {
        ;
        static $.Predicate<String> SYS_CLASS_NAME = new $.Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.startsWith("java") || s.startsWith("org.osgl.");
            }
        };
        static $.Predicate<String> SAFE_CLASS = S.F.endsWith(".class").and(SYS_CLASS_NAME.negate());
    }

    protected static boolean enhanceEligible(String name) {
        boolean sys = name.startsWith("java") || name.startsWith("com.google") || name.startsWith("org.apache") || name.startsWith("org.springframework");
        return !sys;
    }
}
