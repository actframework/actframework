package act.app;

import act.Act;
import act.app.data.BinderManager;
import act.app.data.StringValueResolverManager;
import act.app.event.AppEventId;
import act.app.util.AppCrypto;
import act.app.util.NamedPort;
import act.conf.AppConfLoader;
import act.conf.AppConfig;
import act.conf.AppConfigKey;
import act.controller.bytecode.ControllerByteCodeScanner;
import act.di.DependencyInjector;
import act.di.DiBinder;
import act.event.AppEventListenerBase;
import act.event.EventBus;
import act.handler.builtin.StaticFileGetter;
import act.job.AppJobManager;
import act.job.bytecode.JobByteCodeScanner;
import act.mail.MailerConfigManager;
import act.mail.bytecode.MailerByteCodeScanner;
import act.route.RouteTableRouterBuilder;
import act.route.Router;
import act.util.ClassInfoByteCodeScanner;
import act.util.IdGenerator;
import act.util.SysProps;
import act.util.UploadFileStorageService;
import act.view.ActServerError;
import org.osgl._;
import org.osgl.cache.CacheService;
import org.osgl.cache.CacheServiceProvider;
import org.osgl.http.H;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.storage.IStorageService;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.File;
import java.util.EventObject;
import java.util.List;
import java.util.Map;

import static act.app.event.AppEventId.*;

/**
 * {@code App} represents an application that is deployed in a Act container
 */
public class App {

    public static Logger logger = L.get(App.class);

    private static App INST;

    public enum F {
        ;
        public static _.Predicate<String> JAVA_SOURCE = S.F.endsWith(".java");
        public static _.Predicate<String> JAR_FILE = S.F.endsWith(".jar");
        public static _.Predicate<String> CONF_FILE = S.F.endsWith(".conf").or(S.F.endsWith(".properties"));
        public static _.Predicate<String> ROUTES_FILE = _.F.eq(RouteTableRouterBuilder.ROUTES_FILE);
    }

    private volatile String profile;
    private File appBase;
    private File appHome;
    private Router router;
    private Map<NamedPort, Router> moreRouters;
    private AppConfig config;
    private AppClassLoader classLoader;
    private ProjectLayout layout;
    private AppBuilder builder;
    private EventBus eventBus;
    private AppCodeScannerManager scannerManager;
    private DbServiceManager dbServiceManager;
    private AppJobManager jobManager;
    private MailerConfigManager mailerConfigManager;
    private StringValueResolverManager resolverManager;
    private BinderManager binderManager;
    private AppInterceptorManager interceptorManager;
    private DependencyInjector<?> dependencyInjector;
    private IStorageService uploadFileStorageService;
    private AppServiceRegistry appServiceRegistry;
    private AppCrypto crypto;
    private IdGenerator idGenerator;
    private CacheService cache;
    // used in dev mode only
    private CompilationException compilationException;

    protected App() {
        INST = this;
    }

    protected App(File appBase, ProjectLayout layout) {
        this.appBase = appBase;
        this.layout = layout;
        this.appHome = RuntimeDirs.home(this);
        INST = this;
    }

    public static App instance() {
        return INST;
    }

    public String profile() {
        if (null == profile) {
            synchronized (this) {
                if (null == profile) {
                    String s = SysProps.get(AppConfigKey.PROFILE.key());
                    if (null == s) {
                        s = Act.mode().name().toLowerCase();
                    }
                    profile = s;
                }
            }
        }
        return profile;
    }

    public AppConfig config() {
        return config;
    }

    public Router router() {
        return router;
    }

    public Router router(String name) {
        if (S.blank(name)) {
            return router();
        }
        for (Map.Entry<NamedPort, Router> entry : moreRouters.entrySet()) {
            if (S.eq(entry.getKey().name(), name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public Router router(NamedPort port) {
        if (null == port) {
            return router();
        }
        return moreRouters.get(port);
    }

    public AppCrypto crypto() {
        return crypto;
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
        if (null != compilationException) {
            throw new ActServerError(compilationException, this);
        }
    }

    public void restart() {
        build();
        refresh();
    }

    public void refresh() {
        profile = null;
        Act.viewManager().reload(this);
        initServiceResourceManager();
        initEventBus();
        loadConfig();
        initCache();
        initCrypto();
        initJobManager();
        initInterceptorManager();
        initResolverManager();
        initBinderManager();
        initUploadFileStorageService();
        initRouters();
        initDbServiceManager();
        eventBus().emit(DB_SVC_LOADED);
        loadGlobalPlugin();
        initScannerManager();
        loadActScanners();
        loadBuiltInScanners();
        eventBus().emit(PRE_LOAD_CLASSES);
        initClassLoader();
        try {
            scanAppCodes();
            compilationException = null;
        } catch (CompilationException e) {
            compilationException = e;
            throw new ActServerError(e, this);
        }
        eventBus().emit(APP_CODE_SCANNED);
        initMailerConfigManager();
        loadRoutes();

        // setting context class loader here might lead to memory leaks
        // and cause weird problems as class loader been set to thread
        // could be switched to handling other app in ACT or still hold
        // old app class loader instance after the app been refreshed
        // - Thread.currentThread().setContextClassLoader(classLoader());

        // let's any emit the dependency injector loaded event
        // in case some other service depend on this event.
        // If any DI plugin e.g. guice has emitted this event
        // already, it doesn't matter we emit the event again
        // because once app event is consumed the event listeners
        // are cleared
        eventBus().emit(DEPENDENCY_INJECTOR_LOADED);
        eventBus().emit(PRE_START);
        eventBus().emit(START);
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

    public File tmpDir() {
        return new File(this.layout().target(appBase), "tmp");
    }

    public File file(String path) {
        return new File(home(), path);
    }

    public File resource(String path) {
        return new File(this.layout().resource(appBase), path);
    }

    public AppInterceptorManager interceptorManager() {
        return interceptorManager;
    }

    public AppCodeScannerManager scannerManager() {
        return scannerManager;
    }

    public DbServiceManager dbServiceManager() {
        return dbServiceManager;
    }

    public StringValueResolverManager resolverManager() {
        return resolverManager;
    }

    public BinderManager binderManager() {return binderManager;}

    public CacheService cache() {
        return cache;
    }

    public MailerConfigManager mailerConfigManager() {
        return mailerConfigManager;
    }

    public EventBus eventBus() {
        return eventBus;
    }

    public AppJobManager jobManager() {
        return jobManager;
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

    public IStorageService uploadFileStorageService() {
        return uploadFileStorageService;
    }

    public String sign(String message) {
        return crypto().sign(message);
    }

    public String encrypt(String message) {
        return crypto().encrypt(message);
    }

    public String decrypt(String message) {
        return crypto().decrypt(message);
    }

    public <T> T newInstance(Class<T> clz) {
        if (App.class == clz) return _.cast(this);
        if (AppConfig.class == clz) return _.cast(config());
        if (AppCrypto.class == clz) return _.cast(crypto());
        if (null != dependencyInjector) {
            return dependencyInjector.create(clz);
        } else {
            return _.newInstance(clz);
        }
    }

    <T> T newInstance(Class<T> clz, ActionContext context) {
        if (App.class == clz) return _.cast(this);
        if (AppConfig.class == clz) return _.cast(config());
        if (ActionContext.class == clz) return _.cast(context);
        if (AppCrypto.class == clz) return _.cast(crypto());
        if (null != dependencyInjector) {
            return dependencyInjector.createContextAwareInjector(context).create(clz);
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

    /**
     * Return an ID in string that is unique across the cluster
     * @return
     */
    public String cuid() {
        return idGenerator.genId();
    }

    public <T extends AppService<T>> T service(Class<T> serviceClass) {
        return appServiceRegistry.lookup(serviceClass);
    }

    App register(final AppService service) {
        return register(service, false);
    }

    App register(final AppService service, boolean noDiBinder) {
        appServiceRegistry.register(service);
        if (null != eventBus && !noDiBinder) {
            eventBus.bind(AppEventId.PRE_START, new AppEventListenerBase() {
                @Override
                public void on(EventObject event) throws Exception {
                    final App app = App.this;
                    eventBus.emit(new DiBinder(app, service.getClass()) {
                        @Override
                        public Object resolve(App app) {
                            return app.service(service.getClass());
                        }
                    });
                }
            });
        }
        return this;
    }

    private void loadConfig() {
        File conf = RuntimeDirs.conf(this);
        logger.debug("loading app configuration: %s ...", appBase.getPath());
        config = new AppConfLoader().load(conf);
        config.app(this);
    }

    private void initIdGenerator() {
        idGenerator = new IdGenerator(
                config().nodeIdProvider(),
                config().startIdProvider(),
                config().sequenceProvider()
        );
    }

    private void initServiceResourceManager() {
        if (null != appServiceRegistry) {
            eventBus().emit(STOP);
            appServiceRegistry.destroy();
            dependencyInjector = null;
            if (null != cache) {
                cache.shutdown();
            }
        }
        appServiceRegistry = new AppServiceRegistry();
    }

    private void initUploadFileStorageService() {
        uploadFileStorageService = UploadFileStorageService.create(this);
    }

    private void initRouters() {
        router = new Router(this);
        moreRouters = C.newMap();
        List<NamedPort> ports = config().namedPorts();
        for (NamedPort port: ports) {
            moreRouters.put(port, new Router(this, port.name()));
        }
    }

    private void initEventBus() {
        eventBus = new EventBus(this);
    }

    private void initCache() {
        cache = CacheServiceProvider.Impl.Simple.get("_act_app_");
        cache.startup();
    }

    private void initCrypto() {
        crypto = new AppCrypto(config());
    }

    private void initJobManager() {
        jobManager = new AppJobManager(this);
    }

    private void initInterceptorManager() {
        interceptorManager = new AppInterceptorManager(this);
    }

    private void initScannerManager() {
        scannerManager = new AppCodeScannerManager(this);
    }

    private void initDbServiceManager() {
        dbServiceManager = new DbServiceManager(this);
    }

    private void initMailerConfigManager() {
        mailerConfigManager = new MailerConfigManager(this);
    }

    private void loadGlobalPlugin() {
        Act.appServicePluginManager().applyTo(this);
    }

    private void loadActScanners() {
        Act.scannerPluginManager().initApp(this);
    }

    private void loadBuiltInScanners() {
        scannerManager.register(new ClassInfoByteCodeScanner());
        scannerManager.register(new ControllerByteCodeScanner());
        scannerManager.register(new MailerByteCodeScanner());
        scannerManager.register(new JobByteCodeScanner());
    }

    private void loadRoutes() {
        loadBuiltInRoutes();
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

    private void loadBuiltInRoutes() {
        router().addMapping(H.Method.GET, "/asset/", new StaticFileGetter(layout().asset(base())));
    }

    private void initClassLoader() {
        classLoader = Act.mode().classLoader(this);
        eventBus().emit(AppEventId.CLASS_LOADER_INITIALIZED);
        classLoader.preload();
        eventBus().emit(AppEventId.CLASS_LOADED);
    }

    private void initResolverManager() {
        resolverManager = new StringValueResolverManager(this);
    }
    private void initBinderManager() {
        binderManager = new BinderManager(this);
    }

    private void loadPlugins() {
        // TODO: load app level plugins
    }

    private void scanAppCodes() {
        classLoader().scan();
        //classLoader().scan();
    }

    static App create(File appBase, ProjectLayout layout) {
        return new App(appBase, layout);
    }


}
