package act.app;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import static act.boot.app.FullStackAppBootstrapClassLoader.KEY_CLASSPATH;
import static act.util.ClassInfoRepository.canonicalName;
import static org.osgl.Lang.requireNotNull;

import act.Act;
import act.app.event.SysEventId;
import act.app.util.EnvMatcher;
import act.asm.*;
import act.boot.BootstrapClassLoader;
import act.boot.app.FullStackAppBootstrapClassLoader;
import act.cli.meta.*;
import act.conf.AppConfig;
import act.controller.meta.*;
import act.event.SysEventListenerBase;
import act.exception.EnvNotMatchException;
import act.job.meta.JobClassMetaInfo;
import act.job.meta.JobClassMetaInfoManager;
import act.mail.meta.*;
import act.metric.Metric;
import act.metric.MetricInfo;
import act.util.*;
import act.view.ActErrorResult;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.*;

import java.io.*;
import java.lang.annotation.Annotation;
import java.net.*;
import java.util.*;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
    private Map<String, byte[]> libClsCache = new HashMap<>();
    private Map<String, byte[]> enhancedResourceCache = new HashMap<>();
    private ClassInfoRepository classInfoRepository;
    private boolean destroyed;
    private boolean fullClassGraphBuilt;
    protected ControllerClassMetaInfoManager controllerInfo;
    protected MailerClassMetaInfoManager mailerInfo = new MailerClassMetaInfoManager();
    protected CommanderClassMetaInfoManager commanderInfo = new CommanderClassMetaInfoManager();
    protected JobClassMetaInfoManager jobInfo = new JobClassMetaInfoManager();
    protected SimpleBean.MetaInfoManager simpleBeanInfo;
    protected Metric metric = Act.metricPlugin().metric(MetricInfo.CLASS_LOADING);

    @Inject
    public AppClassLoader(final App app) {
        super(Act.class.getClassLoader());
        this.app = requireNotNull(app);
        ClassInfoRepository actClassInfoRepository = Act.classInfoRepository();
        if (null != actClassInfoRepository) {
            this.classInfoRepository = new AppClassInfoRepository(app, actClassInfoRepository);
        }
        controllerInfo = new ControllerClassMetaInfoManager(app);
        if (null == app.eventBus()) {
            return; // for unit test only
        }
        simpleBeanInfo  = new SimpleBean.MetaInfoManager(this);
        app.eventBus().bind(SysEventId.APP_CODE_SCANNED, new SysEventListenerBase() {

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

    public synchronized boolean isFullClassGraphBuilt() {
        return fullClassGraphBuilt;
    }

    public synchronized void buildFullClassGraph() {
        if (fullClassGraphBuilt) {
            return;
        }
        E.illegalStateIf(null == classInfoRepository);
        C.List<String> path = C.listOf(System.getProperty(KEY_CLASSPATH).split(File.pathSeparator));
        path = path.filter(S.F.endsWith(".jar"));
        List<File> jars = path.map(new $.Transformer<String, File>() {
            @Override
            public File transform(String s) {
                return new File(s);
            }
        }).sorted();
        Map<String, byte[]> index = Jars.buildClassNameIndex(jars);
        ClassInfoByteCodeScanner scanner = new ClassInfoByteCodeScanner(classInfoRepository());
        ByteCodeVisitor bv = scanner.byteCodeVisitor();
        for (Map.Entry<String, byte[]> entry : index.entrySet()) {
            byte[] ba = entry.getValue();
            ClassReader cr = new ClassReader(ba);
            cr.accept(bv, 0);
        }
        fullClassGraphBuilt = true;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> c = findLoadedClass(name);
        if (c != null) {
            return c;
        }

//        // ensure we can enhance the act classes specified below
//        if (name.startsWith("act.") && name.endsWith("Admin")) {
//            return super.loadClass(name, resolve);
//        }

        c = loadAppClass(name, resolve);

        if (null == c) {
            return super.loadClass(name, resolve);
        } else {
            return c;
        }
    }

    protected Set<String> libClasses() {
        return libClsCache.keySet();
    }

    protected void scan() {
        scanByteCode(libClasses(), bytecodeLookup);
    }

    protected void scan(Set<String> libClasses) {
        scanByteCode(libClasses, bytecodeLookup);
    }

    /**
     * This method implement a event listener based scan process:
     * <ol>
     * <li>First loop: through all cached bytecode. Chain all scanner's bytecode visitor</li>
     * <li>Rest loops: through dependencies. Thus if some bytecode missed by a certain scanner
     * due to the context is not established can be captured eventually</li>
     * </ol>
     */
    protected void scanByteCode(Collection<String> classes, $.Function<String, byte[]> bytecodeProvider) {
        long ms = 0;
        if (logger.isDebugEnabled()) {
            ms = $.ms();
            logger.debug("Bytecode scanning starts on %s classes ...", classes.size());
        }
        final AppCodeScannerManager scannerManager = app().scannerManager();
        Map<String, List<AppByteCodeScanner>> dependencies = new HashMap<>();
        for (String className : classes) {
            logger.debug("scanning %s ...", className);
            dependencies.remove(className);
            byte[] ba = bytecodeProvider.apply(className);
            if (null == ba) {
                logger.warn("Cannot find any bytecode for class: %s. You might have an empty Java source file for that.", className);
                continue;
            }
            libClsCache.put(className, ba);
            act.metric.Timer timer = metric.startTimer("act:classload:scan:bytecode:" + className);
            List<ByteCodeVisitor> visitors = new ArrayList<>();
            List<AppByteCodeScanner> scanners = new ArrayList<>();
            for (AppByteCodeScanner scanner : scannerManager.byteCodeScanners()) {
                if (scanner.start(className)) {
                    //LOGGER.trace("scanner %s added to the list", scanner.getClass().getName());
                    visitors.add(scanner.byteCodeVisitor());
                    scanners.add(scanner);
                }
            }
            if (visitors.isEmpty()) {
                continue;
            }
            ByteCodeVisitor theVisitor = ByteCodeVisitor.chain(visitors);
            EnvMatcher matcher = new EnvMatcher();
            matcher.setDownstream(theVisitor);
            ClassReader cr = new ClassReader(ba);
            try {
                cr.accept(matcher, 0);
            } catch (EnvNotMatchException e) {
                continue;
            } catch (AsmException e) {
                Throwable t = e.getCause();
                if (t instanceof ClassNotFoundException) {
                    continue;
                } else {
                    logger.error(e, "Error scanning bytecode at %s", e.context());
                    ActErrorResult error = ActErrorResult.scanningError(e);
                    if (Act.isDev()) {
                        app.handleBlockIssue(error);
                    } else {
                        throw error;
                    }
                }
            }
            for (AppByteCodeScanner scanner : scanners) {
                scanner.scanFinished(className);
                Map<Class<? extends AppByteCodeScanner>, Set<String>> ss = scanner.dependencyClasses();
                if (ss.isEmpty()) {
                    //LOGGER.trace("no dependencies found for %s by scanner %s", className, scanner);
                    continue;
                }
                for (Class<? extends AppByteCodeScanner> scannerClass : ss.keySet()) {
                    AppByteCodeScanner scannerA = scannerManager.byteCodeScannerByClass(scannerClass);
                    for (String dependencyClass : ss.get(scannerClass)) {
                        logger.trace("dependencies[%s] found for %s by scanner %s", dependencyClass, className, scannerA);
                        List<AppByteCodeScanner> l = dependencies.get(dependencyClass);
                        if (null == l) {
                            l = new ArrayList<>();
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
            List<ByteCodeVisitor> visitors = new ArrayList<>();
            for (AppByteCodeScanner scanner : scanners) {
                scanner.start(className);
                visitors.add(scanner.byteCodeVisitor());
            }
            ByteCodeVisitor theVisitor = ByteCodeVisitor.chain(visitors);
            byte[] bytes = bytecodeProvider.apply(className);
            libClsCache.put(className, bytes);
            ClassReader cr = new ClassReader(bytes);
            try {
                cr.accept(theVisitor, 0);
            } catch (AsmException e) {
                throw ActErrorResult.of(e);
            }
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
                            l = new ArrayList<>();
                            dependencies.put(dependencyClass, l);
                        }
                        if (!l.contains(scanner)) l.add(scannerA);
                    }
                }
            }
            timer.stop();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Bytecode scanning takes: " + ($.ms() - ms) + "ms");
        }
    }

    protected void preload() {
        preloadLib();
        preloadClasses();
    }

    private void preloadLib() {
        final Map<String, byte[]> bytecodeIdx = new HashMap<>();
        final Map<String, Properties> jarConf = new HashMap<>();
        final $.Function<String, Boolean> ignoredClassNames = app().config().appClassTester().negate();
        Jars.F.JarEntryVisitor classNameIndexBuilder = Jars.F.classNameIndexBuilder(bytecodeIdx, ignoredClassNames);
        Jars.F.JarEntryVisitor confIndexBuilder = Jars.F.appConfigFileIndexBuilder(jarConf);
        ClassLoader parent = getParent();
        List<File> jars = C.list();
        if (parent instanceof FullStackAppBootstrapClassLoader) {
            jars = ((FullStackAppBootstrapClassLoader) parent).jars(AppClassLoader.class.getClassLoader());
        }
        Set<String> blackList = app().jarFileBlackList();
        Set<String> blackList2 = app().jarFileBlackList2();
        for (File jar : jars) {
            String filename = jar.getName();
            String name = S.cut(filename).beforeFirst("-");
            if ("".equals(name)) {
                name = S.cut(filename).beforeLast(".");
            }
            if (blackList.contains(name)) {
                continue;
            }
            boolean shouldScan = true;
            for (String prefix : blackList2) {
                if (filename.startsWith(prefix)) {
                    shouldScan = false;
                    break;
                }
            }
            if (shouldScan) {
                Jars.scan(jar, classNameIndexBuilder, confIndexBuilder);
            }
        }
        libClsCache.putAll(bytecodeIdx);
        AppConfig config = app().config();
        config.loadJarProperties(jarConf);
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
        IO.copy(IO.inputStream(file), baos);
        byte[] bytes = baos.toByteArray();
        libClsCache.put(ClassNames.sourceFileNameToClassName(base, file.getAbsolutePath().replace(".class", ".java")), bytes);
    }

    protected byte[] loadAppClassFromDisk(String name) {
        File base = RuntimeDirs.classes(app);
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
        // we need to skip getting byte code
        if (null == bytecode) {
            if (!(name.contains("$") && name.endsWith("MethodAccess") && !name.endsWith("$MethodAccess"))) {
                // We need to skip getting byte code for reflectasm generated class for inner class method access
                bytecode = loadAppClassFromDisk(name);
                if (null == bytecode) return null;
            } else {
                return null;
            }
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
                if (baNew.length != bytecode.length) {
                    enhancedResourceCache.put(name.replace('.', '/') + ".class", baNew);
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

    /**
     * This returns the normal {@link ClassLoader#getResourceAsStream(String)}
     * result.
     *
     * @param name the resource name
     * @return the input stream pointing to the resource
     * @see #getResourceAsStream(String)
     */
    public InputStream getOriginalResourceAsStream(String name) {
        return super.getResourceAsStream(name);
    }

    /**
     * The implementation check if the name is an enhanced class and
     * returns an input stream pointing to the enhanced bytecode.
     *
     * Otherwise it delegate the call to normal
     * {@link ClassLoader#getResourceAsStream(String)} call.
     *
     * To make sure it returns real inputstream from the class file,
     * use {@link #getOriginalResourceAsStream(String)} method instead.
     *
     * @param name the resource name
     * @return a URL pointing to the resource.
     */
    @Override
    public InputStream getResourceAsStream(String name) {
        byte[] ba = enhancedResourceCache.get(name);
        if (null != ba) {
            return new ByteArrayInputStream(ba);
        }
        return super.getResourceAsStream(name);
    }

    /**
     * This returns the normal {@link ClassLoader#getResource(String)} result
     *
     * @param name the resource name
     * @return the URL pointing to the resource
     * @see #getResource(String)
     */
    public URL getOriginalResource(String name) {
        return super.getResource(name);
    }

    /**
     * The implementation check if the name is an enhanced class and
     * returns a URL pointing to the enhanced bytecode.
     *
     * In which case the URL returned is using a special protocol `act-class`
     * instead of `jar` or `file`, the only meaningful operation on the
     * returned URL is {@link URL#openConnection()} which has special
     * implementation of {@link URLConnection#getInputStream()} to return
     * a {@link ByteArrayInputStream} pointing to the enhanced bytecode
     * of the class.
     *
     * To make sure it returns real URL to the class file,
     * use {@link #getOriginalResource(String)} method instead.
     *
     * @param name the resource name
     * @return a URL pointing to the resource.
     */
    @Override
    public URL getResource(String name) {
        final byte[] ba = enhancedResourceCache.get(name);
        if (null != ba) {
            try {
                return new URL("act-class", "", -1, name, new URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(URL u) {
                        return new URLConnection(null) {
                            @Override
                            public void connect() {
                            }

                            @Override
                            public InputStream getInputStream() {
                                return new ByteArrayInputStream(ba);
                            }

                        };
                    }
                });
            } catch (MalformedURLException e) {
                throw E.unexpected(e);
            }
        }
        return super.getResource(name);
    }

    protected byte[] enhance(String className, byte[] bytecode) {
        return asmEnhance(className, bytecode);
    }

    private byte[] asmEnhance(String className, byte[] bytecode) {
        //if (isSystemClass(className)) return bytecode;
        if (!app().config().needEnhancement(className)) return bytecode;
        $.Var<ClassWriter> cw = $.var(null);
        ByteCodeVisitor enhancer = Act.enhancerManager().appEnhancer(app, className, cw);
        if (null == enhancer) {
            return bytecode;
        }
        EnvMatcher matcher = new EnvMatcher();
        matcher.setDownstream(enhancer);
        cw.set(new ClassWriter(ClassWriter.COMPUTE_FRAMES));
        enhancer.commitDownstream();
        ClassReader r = new ClassReader(bytecode);
        try {
            r.accept(matcher, ClassReader.EXPAND_FRAMES);
        } catch (EnvNotMatchException e) {
            return bytecode;
        } catch (AsmException e) {
            logger.error(e, "error enhancing bytecode at %s", e.context());
            throw ActErrorResult.enhancingError(e);
        }
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

    byte[] cachedEnhancedBytecode(String className) {
        String key = className.replace('.', '/') + ".class";
        return enhancedResourceCache.get(key);
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
                return isSystemClass(s);
            }
        };
        static $.Predicate<String> SAFE_CLASS = S.F.endsWith(".class").and(SYS_CLASS_NAME.negate());
    }

    public static boolean isSystemClass(String name) {
        boolean sys = name.startsWith("java.")
                || name.startsWith("javax.")
                || name.startsWith("com.google.")
                || name.startsWith("org.apache.")
                || name.startsWith("org.springframework.")
                || name.startsWith("sun.")
                || name.startsWith("com.sun.")
                || name.startsWith("org.osgl.")
                || name.startsWith("osgl.");
        return sys;
    }
}
