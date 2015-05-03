package org.osgl.oms.app;

import org.osgl._;
import org.osgl.oms.OMS;
import org.osgl.oms.conf.AppConfig;
import org.osgl.oms.route.Router;
import org.osgl.oms.util.*;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Dev mode application class loader, which is able to
 * load classes directly from app srccode folder
 */
public class DevModeClassLoader extends AppClassLoader {

    private static _.Predicate<String> JAVA_SOURCE = S.F.endsWith(".java");
    private static _.Predicate<String> JAR_FILE = S.F.endsWith(".jar");
    private static _.Predicate<String> CONF_FILE = S.F.endsWith(".conf").or(S.F.endsWith(".properties"));

    private final AppCompiler compiler;
    private final Map<String, Source> sources = C.newMap();
    private FsChangeDetector confChangeDetector;
    private FsChangeDetector libChangeDetector;
    private FsChangeDetector resourceChangeDetector;
    private FsChangeDetector sourceChangeDetector;

    public DevModeClassLoader(App app) {
        super(app);
        compiler = new AppCompiler(this);
        setupFsChangeDetectors();
    }

    @Override
    public void detectChanges() {
        detectChanges(confChangeDetector);
        detectChanges(libChangeDetector);
        detectChanges(resourceChangeDetector);
        detectChanges(sourceChangeDetector);
    }

    private void detectChanges(FsChangeDetector detector) {
        if (null != detector) {
            detector.detectChanges();
        }
    }

    @Override
    public boolean isSourceClass(String className) {
        return sources.containsKey(className);
    }

    @Override
    protected void preloadBytecode() {
        super.preloadBytecode();
        preloadSources();
    }

    @Override
    protected void scanForActionMethods() {
        AppConfig conf = app().config();
        SourceCodeActionScanner scanner = new SourceCodeActionScanner();
        Router router = app().router();
        for (String className : sources.keySet()) {
            if (conf.notControllerClass(className)) {
                scanner.scan(className, sources.get(className).code(), router);
            }
        }
        super.scan();
    }

    @Override
    protected byte[] appBytecode(String name) {
        byte[] bytecode = super.appBytecode(name);
        return null == bytecode ? bytecodeFromSource(name) : bytecode;
    }

    Source source(String name) {
        return sources.get(name);
    }

    private void preloadSources() {
        final File sourceRoot = app().layout().source(app().base());
        Files.filter(sourceRoot, JAVA_SOURCE, new _.Visitor<File>(){
            @Override
            public void visit(File file) throws _.Break {
                Source source = Source.ofFile(sourceRoot, file);
                if (null != source) {
                    sources.put(source.className(), source);
                }
            }
        });
    }


    private byte[] bytecodeFromSource(String name) {
        Source source = sources.get(name);
        if (null == source) {
            return null;
        }
        byte[] bytes = source.bytes();
        if (null == bytes) {
            compiler.compile(name);
            bytes = source.bytes();
        }
        return bytes;
    }

    private final FsEventListener sourceChangeListener = new FsEventListener() {
        @Override
        public void on(FsEvent... events) {
            throw E.tbd("handle source code changes");
        }
    };

    private final FsEventListener libChangeListener = new FsEventListener() {
        @Override
        public void on(FsEvent... events) {
            int len = events.length;
            if (len < 0) return;
            OMS.requestRefreshClassLoader();
        }
    };

    private final FsEventListener confChangeListener = new FsEventListener() {
        @Override
        public void on(FsEvent... events) {
            OMS.requestRestart();
        }
    };

    private final FsEventListener resourceChangeListener = new FsEventListener() {
        @Override
        public void on(FsEvent... events) {
            int len = events.length;
            for (int i = 0; i < len; ++i) {
                FsEvent e = events[i];
                if (e.kind() == FsEvent.Kind.CREATE) {
                    List<String> paths = e.paths();
                    File[] files = new File[paths.size()];
                    int idx = 0;
                    for (String path : paths) {
                        files[idx++] = new File(path);
                    }
                    app().builder().copyResources(files);
                } else {
                    OMS.requestRestart();
                }
            }
        }
    };

    private void setupFsChangeDetectors() {
        ProjectLayout layout = app().layout();
        File appBase = app().base();

        File src = layout.source(appBase);
        if (null != src) {
            sourceChangeDetector = new FsChangeDetector(src, JAVA_SOURCE, sourceChangeListener);
        }

        File lib = layout.lib(appBase);
        if (null != lib) {
            libChangeDetector = new FsChangeDetector(lib, JAR_FILE, libChangeListener);
        }

        File conf = layout.conf(appBase);
        if (null != conf) {
            confChangeDetector = new FsChangeDetector(conf, CONF_FILE, confChangeListener);
        }

        File rsrc = layout.resource(appBase);
        if (null != rsrc) {
            resourceChangeDetector = new FsChangeDetector(rsrc, null, resourceChangeListener);
        }
    }
}
