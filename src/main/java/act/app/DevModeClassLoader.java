package act.app;

import act.Act;
import act.util.FsChangeDetector;
import act.util.FsEvent;
import act.util.FsEventListener;
import org.osgl._;
import org.osgl.exception.NotAppliedException;
import act.conf.AppConfig;
import act.controller.meta.ControllerClassMetaInfo;
import act.route.Router;
import act.util.Files;
import org.osgl.util.C;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dev mode application class loader, which is able to
 * load classes directly from app src folder
 */
public class DevModeClassLoader extends AppClassLoader {


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
    protected void scan2() {
        super.scan2();
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
        return sources.get(className);
    }

    private void preloadSources() {
        final File sourceRoot = app().layout().source(app().base());
        Files.filter(sourceRoot, App.F.JAVA_SOURCE, new _.Visitor<File>() {
            @Override
            public void visit(File file) throws _.Break {
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

    private void scanSources() {
        logger.debug("start to scan sources...");
        List<AppSourceCodeScanner> scanners = app().scannerManager().sourceCodeScanners();
        if (scanners.isEmpty()) {
            logger.warn("No source code scanner found");
            return;
        }

        Set<String> classesNeedByteCodeScan = C.newSet();
        for (String className : sources.keySet()) {
            logger.debug("scanning %s ...", className);
            List<AppSourceCodeScanner> l = C.newList();
            for (AppSourceCodeScanner scanner : scanners) {
                if (scanner.start(className)) {
                    logger.debug("scanner %s added to the list", scanner);
                    l.add(scanner);
                }
            }
            if (l.isEmpty()) {
                continue;
            }
            Source source = source(className);
            String[] lines = source.code().split("[\\n\\r]+");
            for (int i = 0, j = lines.length; i < j; ++i) {
                String line = lines[i];
                for (AppSourceCodeScanner scanner : l) {
                    scanner.visit(i, line, className);
                }
            }
            for (AppSourceCodeScanner scanner: l) {
                if (scanner.triggerBytecodeScanning()) {
                    logger.debug("bytecode scanning triggered on %s", className);
                    classesNeedByteCodeScan.add(className);
                    break;
                }
            }
        }

        if (classesNeedByteCodeScan.isEmpty()) {
            return;
        }

        scanByteCode(classesNeedByteCodeScan, new _.F1<String, byte[]>() {
            @Override
            public byte[] apply(String s) throws NotAppliedException, _.Break {
                return bytecodeFromSource(s, true);
            }
        });
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

        File conf = layout.conf(appBase);
        if (null != conf && conf.canRead()) {
            confChangeDetector = new FsChangeDetector(conf, App.F.CONF_FILE.or(App.F.ROUTES_FILE), confChangeListener);
        }

        File rsrc = layout.resource(appBase);
        if (null != rsrc && rsrc.canRead()) {
            resourceChangeDetector = new FsChangeDetector(rsrc, null, resourceChangeListener);
        }
    }

    private final FsEventListener sourceChangeListener = new FsEventListener() {
        @Override
        public void on(FsEvent... events) {
            throw RequestRefreshClassLoader.INSTANCE;
        }
    };

    private final FsEventListener libChangeListener = new FsEventListener() {
        @Override
        public void on(FsEvent... events) {
            int len = events.length;
            if (len < 0) return;
            Act.requestRefreshClassLoader();
        }
    };

    private final FsEventListener confChangeListener = new FsEventListener() {
        @Override
        public void on(FsEvent... events) {
            Act.requestRestart();
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
                for (String path : paths) {
                    files[idx++] = new File(path);
                }
                switch (e.kind()) {
                    case CREATE:
                    case MODIFY:
                        app().builder().copyResources(files);
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
