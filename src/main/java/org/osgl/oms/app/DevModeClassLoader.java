package org.osgl.oms.app;

import org.osgl._;
import org.osgl.oms.OMS;
import org.osgl.oms.util.FsChangeDetector;
import org.osgl.oms.util.FsEvent;
import org.osgl.oms.util.FsEventListener;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Dev mode application class loader, which is able to
 * load classes directly from app source folder
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
        setupFsChangeMonitors();
    }

    @Override
    public void detectChanges() {
        super.detectChanges();
    }

    @Override
    public boolean isAppClass(String className) {
        return sources.containsKey(className);
    }

    @Override
    protected byte[] appBytecode(String name) {
        byte[] bytecode = super.appBytecode(name);
        return null == bytecode ? bytecodeFromSource(name) : bytecode;
    }

    Source source(String name) {
        return sources.get(name);
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
            for (int i = 0; i < len; ++i) {
                FsEvent e = events[i];
                if (e.kind() == FsEvent.Kind.CREATE) {
                    for (String s : e.paths()) {
                        File file = new File(s);
                        preloadClassFile(file);
                    }
                } else {
                    OMS.requestRefreshClassLoader();
                }
            }
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

    private void setupFsChangeMonitors() {
        ProjectLayout layout = app().layout();
        File appBase = app().base();
        sourceChangeDetector = new FsChangeDetector(layout.source(appBase), JAVA_SOURCE, sourceChangeListener);
        libChangeDetector = new FsChangeDetector(layout.lib(appBase), JAR_FILE, libChangeListener);
        confChangeDetector = new FsChangeDetector(layout.conf(appBase), CONF_FILE, confChangeListener);
        resourceChangeDetector = new FsChangeDetector(layout.resource(appBase), null, resourceChangeListener);
    }
}
