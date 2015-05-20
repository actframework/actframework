package org.osgl.oms.app;

import org.apache.commons.codec.Charsets;
import org.osgl._;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.oms.OMS;
import org.osgl.oms.conf.AppConfLoader;
import org.osgl.oms.conf.AppConfig;
import org.osgl.oms.route.RouteTableRouterBuilder;
import org.osgl.oms.route.Router;
import org.osgl.oms.util.Files;
import org.osgl.oms.util.FsChangeDetector;
import org.osgl.oms.util.FsEvent;
import org.osgl.oms.util.FsEventListener;
import org.osgl.util.C;
import org.osgl.util.Crypto;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * {@code App} represents an application that is deployed in a OMS container
 */
public class App {

    private static Logger logger = L.get(App.class);
    public static _.Predicate<String> JAVA_SOURCE = S.F.endsWith(".java");
    private static _.Predicate<String> JAR_FILE = S.F.endsWith(".jar");
    private static _.Predicate<String> CONF_FILE = S.F.endsWith(".conf").or(S.F.endsWith(".properties"));

    private File appBase;
    private File appHome;
    private Router router;
    private AppConfig config;
    private AppClassLoader classLoader;
    private ProjectLayout layout;
    private AppBuilder builder;
    private Map<String, Source> sources = C.newMap();
    private FsChangeDetector confChangeDetector;
    private FsChangeDetector libChangeDetector;
    private FsChangeDetector resourceChangeDetector;
    private FsChangeDetector sourceChangeDetector;

    protected App() {
    }

    protected App(File appBase, ProjectLayout layout) {
        this.appBase = appBase;
        this.layout = layout;
        this.appHome = RuntimeDirs.home(this);
    }

    public AppConfig config() {
        return config;
    }

    public Router router() {
        return router;
    }

    /**
     * The base dir where an application sit within
     */
    public File base() {
        return appBase;
    }

    /**
     * The home dir of an application, referenced only
     * at runtime.
     * <p><b>Note</b> when app is running in dev mode, {@code appHome}
     * shall be {@code appBase/target}, while app is deployed to
     * OMS at other mode, {@code appHome} shall be the same as
     * {@code appBase}</p>
     */
    public File home() {
        return appHome;
    }

    public AppClassLoader classLoader() {
        return classLoader;
    }

    public ProjectLayout layout() {
        return layout;
    }

    public void detectChanges() {
        if (!OMS.isDev()) return;
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

    public void refresh() {
        loadConfig();
        initRouter();
        loadClasses();
        loadRoutes();
        if (OMS.isDev()) {
            setupFsChangeDetectors();
        }
    }

    void build() {
        builder = AppBuilder.build(this);
    }

    void hook() {
        OMS.hook(this);
    }

    public AppBuilder builder() {
        return builder;
    }

    public String sign(String message) {
        return Crypto.sign(message, config().secret().getBytes(Charsets.UTF_8));
    }

    public String encrypt(String message) {
        return Crypto.encryptAES(message, config().secret());
    }

    public String decrypt(String message) {
        return Crypto.decryptAES(message, config().secret());
    }

    public boolean isSourceClass(String className) {
        return sources.containsKey(className);
    }

    public Source source(String className) {
        return sources.get(className);
    }

    public void preloadSources() {
        final File sourceRoot = layout().source(base());
        Files.filter(sourceRoot, JAVA_SOURCE, new _.Visitor<File>() {
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

    public void scanForActionMethods() {
        AppConfig conf = config();
        SourceCodeActionScanner scanner = new SourceCodeActionScanner();
        Router router = router();
        for (String className : sources.keySet()) {
            if (conf.possibleControllerClass(className)) {
                Source source = sources.get(className);
                boolean isController = scanner.scan(className, source.code(), router);
                if (isController) {
                    source.markAsController();
                    classLoader().scanForActionMethods(className);
                }
            }
        }
    }

    @Override
    public int hashCode() {
        return appBase.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof App) {
            App that = (App) obj;
            return _.eq(that.appBase, appBase);
        }
        return false;
    }

    @Override
    public String toString() {
        return appBase.getName();
    }

    private void loadConfig() {
        File conf = RuntimeDirs.conf(this);
        logger.debug("loading app configuration: %s ...", appBase.getPath());
        config = new AppConfLoader().load(conf);
    }

    private void initRouter() {
        router = new Router(this);
    }

    private void loadRoutes() {
        logger.debug("loading app routing table: %s ...", appBase.getPath());
        File routes = RuntimeDirs.routes(this);
        if (!(routes.isFile() && routes.canRead())) {
            logger.warn("Cannot find routeTable file: %s", appBase.getPath());
            // guess the app is purely using annotation based routes
            return;
        }
        List<String> lines = IO.readLines(routes);
        new RouteTableRouterBuilder(lines).build(router);
    }

    private void loadClasses() {
        classLoader = OMS.mode().classLoader(this);
        classLoader.init();
    }
    private void setupFsChangeDetectors() {
        ProjectLayout layout = layout();
        File appBase = base();

        File src = layout.source(appBase);
        if (null != src) {
            sourceChangeDetector = new FsChangeDetector(src, App.JAVA_SOURCE, sourceChangeListener);
        }

        File lib = layout.lib(appBase);
        if (null != lib && lib.canRead()) {
            libChangeDetector = new FsChangeDetector(lib, JAR_FILE, libChangeListener);
        }

        File conf = layout.conf(appBase);
        if (null != conf && conf.canRead()) {
            confChangeDetector = new FsChangeDetector(conf, CONF_FILE, confChangeListener);
        }

        File rsrc = layout.resource(appBase);
        if (null != rsrc && rsrc.canRead()) {
            resourceChangeDetector = new FsChangeDetector(rsrc, null, resourceChangeListener);
        }
    }

    static App create(File appBase, ProjectLayout layout) {
        return new App(appBase, layout);
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
                    builder().copyResources(files);
                } else {
                    OMS.requestRestart();
                }
            }
        }
    };

}
