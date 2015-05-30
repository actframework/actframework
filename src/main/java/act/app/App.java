package act.app;

import act.Act;
import act.conf.AppConfLoader;
import act.conf.AppConfig;
import act.controller.ControllerSourceCodeScanner;
import act.controller.bytecode.ControllerByteCodeScanner;
import act.di.DependencyInjector;
import act.route.RouteTableRouterBuilder;
import act.route.Router;
import act.util.FsChangeDetector;
import act.util.FsEvent;
import act.util.FsEventListener;
import org.apache.commons.codec.Charsets;
import org.osgl._;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.*;

import java.io.File;
import java.util.List;

/**
 * {@code App} represents an application that is deployed in a Act container
 */
public class App {

    private static Logger logger = L.get(App.class);

    public enum F {
        ;
        public static _.Predicate<String> JAVA_SOURCE = S.F.endsWith(".java");
        public static _.Predicate<String> JAR_FILE = S.F.endsWith(".jar");
        public static _.Predicate<String> CONF_FILE = S.F.endsWith(".conf").or(S.F.endsWith(".properties"));
        public static _.Predicate<String> ROUTES_FILE = _.F.eq(RouteTableRouterBuilder.ROUTES_FILE);
    }

    private File appBase;
    private File appHome;
    private Router router;
    private AppConfig config;
    private AppClassLoader classLoader;
    private ProjectLayout layout;
    private AppBuilder builder;
    private AppCodeScannerManager scannerManager;
    private AppInterceptorManager interceptorManager;


    private DependencyInjector<?> dependencyInjector;
    private ServiceResourceManager serviceResourceManager;

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
     * Act at other mode, {@code appHome} shall be the same as
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
        classLoader.detectChanges();
    }

    public void restart() {
        build();
        refresh();
    }

    public void refresh() {
        initServiceResourceManager();
        initInterceptorManager();
        loadConfig();
        initRouter();
        initScannerManager();
        loadActScanners();
        loadBuiltInScanners();
        initClassLoader();
        scanAppCodes();
        loadRoutes();
    }

    public AppBuilder builder() {
        return builder;
    }

    void build() {
        builder = AppBuilder.build(this);
    }

    void hook() {
        Act.hook(this);
    }

    public AppInterceptorManager interceptorManager() {
        return interceptorManager;
    }

    public AppCodeScannerManager scannerManager() {
        return scannerManager;
    }

    public App injector(DependencyInjector<?> dependencyInjector) {
        E.NPE(dependencyInjector);
        E.illegalStateIf(null != this.dependencyInjector, "Dependency injection factory already set");
        this.dependencyInjector = dependencyInjector;
        return this;
    }

    public <T extends DependencyInjector<T>> T injector() {
        return (T) dependencyInjector;
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

    public <T> T newInstance(Class<T> clz) {
        if (null != dependencyInjector) {
            return dependencyInjector.create(clz);
        } else {
            return _.newInstance(clz);
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
        return S.builder("app@[").append(appBase).append("]").toString();
    }

    App register(AppService service) {
        serviceResourceManager.register(service);
        return this;
    }

    private void loadConfig() {
        File conf = RuntimeDirs.conf(this);
        logger.debug("loading app configuration: %s ...", appBase.getPath());
        config = new AppConfLoader().load(conf);
        config.app(this);
    }

    private void initServiceResourceManager() {
        if (null != serviceResourceManager) {
            serviceResourceManager.destroy();
            dependencyInjector = null;
        }
        serviceResourceManager = new ServiceResourceManager();
    }

    private void initRouter() {
        router = new Router(this);
    }

    private void initInterceptorManager() {
        interceptorManager = new AppInterceptorManager(this);
    }

    private void initScannerManager() {
        scannerManager = new AppCodeScannerManager(this);
    }

    private void loadActScanners() {
        Act.scannerPluginManager().initApp(this);
    }

    private void loadBuiltInScanners() {
        scannerManager.register(new ControllerSourceCodeScanner());
        scannerManager.register(new ControllerByteCodeScanner());
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

    private void initClassLoader() {
        classLoader = Act.mode().classLoader(this);
        classLoader.preload();
    }

    private void loadPlugins() {
        // TODO: load app level plugins
    }

    private void scanAppCodes() {
        classLoader().scan2();
        //classLoader().scan();
    }

    static App create(File appBase, ProjectLayout layout) {
        return new App(appBase, layout);
    }


}
