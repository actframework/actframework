package act.app;

import act.Act;
import act.ActComponent;
import act.controller.meta.ControllerClassMetaInfo;
import act.metric.Timer;
import act.util.Files;
import act.util.FsChangeDetector;
import act.util.FsEvent;
import act.util.FsEventListener;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.S;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dev mode application class loader, which is able to
 * load classes directly from app src folder
 */
@ActComponent
public class DevModeClassLoader extends AppClassLoader {
    private final static Logger logger = L.get(DevModeClassLoader.class);

    private Map<String, Source> sources = C.newMap();
    private final AppCompiler compiler;

    private FsChangeDetector confChangeDetector;
    private FsChangeDetector libChangeDetector;
    private FsChangeDetector resourceChangeDetector;
    private FsChangeDetector sourceChangeDetector;

    public DevModeClassLoader(App app) {
        super(app);
        compiler = new AppCompiler(this);
    }

    @Override
    protected void releaseResources() {
        sources.clear();
        compiler.destroy();
        super.releaseResources();
    }

    public boolean isSourceClass(String className) {
        return sources.containsKey(className);
    }

    public ControllerClassMetaInfo controllerClassMetaInfo(String controllerClassName) {
        return super.controllerClassMetaInfo(controllerClassName);
    }

    @Override
    protected void preload() {
        preloadSources();
        super.preload();
        setupFsChangeDetectors();
    }

    @Override
    protected void preloadClasses() {
        // do not preload classes in dev mode
    }

    @Override
    protected void scan() {
        super.scan();
        compileSources();
        scanSources();
    }

    @Override
    protected byte[] loadAppClassFromDisk(String name) {
        File srcRoot = app().layout().source(app().base());
        preloadSource(srcRoot, name);
        return bytecodeFromSource(name, true);
    }

    @Override
    protected byte[] appBytecode(String name, boolean compileSource) {
        byte[] bytecode = super.appBytecode(name, compileSource);
        return null == bytecode && compileSource ? bytecodeFromSource(name, compileSource) : bytecode;
    }

    public Source source(String className) {
        if (className.contains("$")) {
            String name0 = S.before(className, "$");
            return sources.get(name0);
        }
        return sources.get(className);
    }

    private void preloadSources() {
        final File sourceRoot = app().layout().source(app().base());
        Files.filter(sourceRoot, App.F.JAVA_SOURCE, new $.Visitor<File>() {
            @Override
            public void visit(File file) throws $.Break {
                Source source = Source.ofFile(sourceRoot, file);
                if (null != source) {
                    if (null == sources) {
                        sources = C.newMap();
                    }
                    sources.put(source.className(), source);
                }
            }
        });
        if ("test".equals(app().profile())) {
            final File testSourceRoot = app().layout().testSource(app().base());
            Files.filter(testSourceRoot, App.F.JAVA_SOURCE, new $.Visitor<File>() {
                @Override
                public void visit(File file) throws $.Break {
                    Source source = Source.ofFile(sourceRoot, file);
                    if (null != source) {
                        if (null == sources) {
                            sources = C.newMap();
                        }
                        sources.put(source.className(), source);
                    }
                }
            });
        }
    }

    private void preloadSource(File sourceRoot, String className) {
        if (null != sources) {
            Source source = sources.get(className);
            if (null != source) {
                return;
            }
        }
        Source source = Source.ofClass(sourceRoot, className);
        if (null != source) {
            if (null == sources) {
                sources = C.newMap();
            }
            sources.put(source.className(), source);
        }
    }

    private void compileSources() {
        logger.debug("start to compile sources ...");
        compiler.compile(sources.values());
    }

    private void scanSources() {
        Timer timer = metric.startTimer("act:classload:scan:scanSources");
        try {
            logger.debug("start to scan sources...");
            List<AppSourceCodeScanner> scanners = app().scannerManager().sourceCodeScanners();

            Set<String> classesNeedByteCodeScan = C.newSet();
            if (scanners.isEmpty()) {
                //logger.warn("No source code scanner found");
                for (String className : sources.keySet()) {
                    classesNeedByteCodeScan.add(className);
                }
            } else {
                for (String className : sources.keySet()) {
                    classesNeedByteCodeScan.add(className);
                    logger.debug("scanning %s ...", className);
                    List<AppSourceCodeScanner> l = C.newList();
                    for (AppSourceCodeScanner scanner : scanners) {
                        if (scanner.start(className)) {
                            //logger.trace("scanner %s added to the list", scanner.getClass().getName());
                            l.add(scanner);
                        }
                    }
                    Source source = source(className);
                    String[] lines = source.code().split("[\\n\\r]+");
                    for (int i = 0, j = lines.length; i < j; ++i) {
                        String line = lines[i];
                        for (AppSourceCodeScanner scanner : l) {
                            scanner.visit(i, line, className);
                        }
                    }
                }
            }

            if (classesNeedByteCodeScan.isEmpty()) {
                return;
            }

            final Set<String> embeddedClassNames = C.newSet();
            scanByteCode(classesNeedByteCodeScan, new $.F1<String, byte[]>() {
                @Override
                public byte[] apply(String s) throws NotAppliedException, $.Break {
                    return bytecodeFromSource(s, embeddedClassNames);
                }
            });

            while (!embeddedClassNames.isEmpty()) {
                Set<String> embeddedClassNameCopy = C.newSet(embeddedClassNames);
                scanByteCode(embeddedClassNameCopy, new $.F1<String, byte[]>() {
                    @Override
                    public byte[] apply(String s) throws NotAppliedException, $.Break {
                        return bytecodeFromSource(s, embeddedClassNames);
                    }
                });
                embeddedClassNames.removeAll(embeddedClassNameCopy);
            }
        } finally {
            timer.stop();
        }
    }

    private byte[] bytecodeFromSource(String name, boolean compile) {
        Source source = source(name);
        if (null == source) {
            return null;
        }
        byte[] bytes = source.bytes();
        if (null == bytes && compile) {
            compiler.compile(name);
            bytes = source.bytes();
        }
        if (name.contains("$")) {
            String innerClassName = S.afterFirst(name, "$");
            return source.bytes(innerClassName);
        }
        return bytes;
    }

    private byte[] bytecodeFromSource(String name, Set<String> embeddedClassNames) {
        Source source = source(name);
        if (null == source) {
            return null;
        }
        byte[] bytes = source.bytes();
        if (null == bytes) {
            compiler.compile(name);
            bytes = source.bytes();
        }
        if (!name.contains("$")) {
            embeddedClassNames.addAll(C.list(source.innerClassNames()).map(S.F.prepend(name + "$")));
        } else {
            String innerClassName = S.afterFirst(name, "$");
            return source.bytes(innerClassName);
        }
        return bytes;
    }

    @Override
    public void detectChanges() {
        detectChanges(confChangeDetector);
        detectChanges(libChangeDetector);
        detectChanges(resourceChangeDetector);
        detectChanges(sourceChangeDetector);
        super.detectChanges();
    }

    private void detectChanges(FsChangeDetector detector) {
        if (null != detector) {
            detector.detectChanges();
        }
    }

    private void setupFsChangeDetectors() {
        ProjectLayout layout = app().layout();
        File appBase = app().base();

        File src = layout.source(appBase);
        if (null != src) {
            sourceChangeDetector = new FsChangeDetector(src, App.F.JAVA_SOURCE, sourceChangeListener);
        }

        File lib = layout.lib(appBase);
        if (null != lib && lib.canRead()) {
            libChangeDetector = new FsChangeDetector(lib, App.F.JAR_FILE, libChangeListener);
        }

        File rsrc = layout.resource(appBase);
        if (null != rsrc && rsrc.canRead()) {
            confChangeDetector = new FsChangeDetector(rsrc, App.F.CONF_FILE.or(App.F.ROUTES_FILE), confChangeListener);
            resourceChangeDetector = new FsChangeDetector(rsrc, null, resourceChangeListener);
        }
    }

    private final FsEventListener sourceChangeListener = new FsEventListener() {
        @Override
        public void on(FsEvent... events) {
            throw Act.requestRefreshClassLoader();
        }
    };

    private final FsEventListener libChangeListener = new FsEventListener() {
        @Override
        public void on(FsEvent... events) {
            int len = events.length;
            if (len < 0) return;
            throw Act.requestRefreshClassLoader();
        }
    };

    private final FsEventListener confChangeListener = new FsEventListener() {
        @Override
        public void on(FsEvent... events) {
            throw Act.requestRestart();
        }
    };

    private final FsEventListener resourceChangeListener = new FsEventListener() {
        @Override
        public void on(FsEvent... events) {
            int len = events.length;
            for (int i = 0; i < len; ++i) {
                FsEvent e = events[i];
                List<String> paths = e.paths();
                File[] files = new File[paths.size()];
                int idx = 0;
                boolean routeChanged = false;
                for (String path : paths) {
                    if (path.endsWith("/routes") || path.endsWith("\\routes")) {
                        routeChanged = true;
                    }
                    files[idx++] = new File(path);
                }
                switch (e.kind()) {
                    case CREATE:
                    case MODIFY:
                        app().builder().copyResources(files);
                        if (routeChanged) {
                            throw Act.requestRestart();
                        }
                        break;
                    case DELETE:
                        app().builder().removeResources(files);
                        break;
                    default:
                        assert false;
                }
            }
        }
    };

}
