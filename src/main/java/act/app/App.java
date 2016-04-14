package act.app;

import act.Act;
import act.Destroyable;
import act.app.data.BinderManager;
import act.app.data.StringValueResolverManager;
import act.app.event.AppEventId;
import act.app.util.AppCrypto;
import act.app.util.NamedPort;
import act.cli.CliDispatcher;
import act.cli.bytecode.CommanderByteCodeScanner;
import act.conf.AppConfLoader;
import act.conf.AppConfig;
import act.conf.AppConfigKey;
import act.controller.bytecode.ControllerByteCodeScanner;
import act.data.DataPropertyRepository;
import act.data.JodaDateTimeCodec;
import act.data.util.ActPropertyHandlerFactory;
import act.di.DependencyInjectionBinder;
import act.di.DependencyInjector;
import act.event.AppEventListenerBase;
import act.event.EventBus;
import act.event.bytecode.SimpleEventListenerByteCodeScanner;
import act.handler.builtin.StaticFileGetter;
import act.job.AppJobManager;
import act.job.bytecode.JobByteCodeScanner;
import act.mail.MailerConfigManager;
import act.mail.MailerContext;
import act.mail.bytecode.MailerByteCodeScanner;
import act.route.RouteSource;
import act.route.RouteTableRouterBuilder;
import act.route.Router;
import act.util.*;
import act.view.ActServerError;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.cache.CacheService;
import org.osgl.cache.CacheServiceProvider;
import org.osgl.http.H;
import org.osgl.http.HttpConfig;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.storage.IStorageService;
import org.osgl.util.*;

import java.io.File;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static act.app.event.AppEventId.*;

/**
 * {@code App} represents an application that is deployed in a Act container
 */
public class App {

    public static Logger logger = L.get(App.class);

    private static App INST;

    public enum F {
        ;
        public static $.Predicate<String> JAVA_SOURCE = S.F.endsWith(".java");
        public static $.Predicate<String> JAR_FILE = S.F.endsWith(".jar");
        public static $.Predicate<String> CONF_FILE = S.F.endsWith(".conf").or(S.F.endsWith(".properties"));
        public static $.Predicate<String> ROUTES_FILE = $.F.eq(RouteTableRouterBuilder.ROUTES_FILE);
    }

    private volatile String profile;
    private String name;
    private File appBase;
    private File appHome;
    private Router router;
    private CliDispatcher cliDispatcher;
    private Map<NamedPort, Router> moreRouters;
    private AppConfig config;
    private AppClassLoader classLoader;
    private ProjectLayout layout;
    private AppBuilder builder;
    private EventBus eventBus;
    private AppCodeScannerManager scannerManager;
    private DbServiceManager dbServiceManager;
    private AppJobManager jobManager;
    private CliServer cliServer;
    private MailerConfigManager mailerConfigManager;
    private StringValueResolverManager resolverManager;
    private SingletonRegistry singletonRegistry;
    private BinderManager binderManager;
    private AppInterceptorManager interceptorManager;
    private DependencyInjector<?> dependencyInjector;
    private IStorageService uploadFileStorageService;
    private AppServiceRegistry appServiceRegistry;
    private Map<String, Daemon> daemonRegistry;
    private AppCrypto crypto;
    private IdGenerator idGenerator;
    private CacheService cache;
    // used in dev mode only
    private CompilationException compilationException;
    private AppEventId currentState;
    private Set<AppEventId> eventEmitted;

    protected App() {
        INST = this;
    }

    protected App(File appBase, ProjectLayout layout) {
        this("MyApp", appBase, layout);
    }

    protected App(String name, File appBase, ProjectLayout layout) {
        this.name = name;
        this.appBase = appBase;
        this.layout = layout;
        this.appHome = RuntimeDirs.home(this);
        INST = this;
    }

    public static App instance() {
        return INST;
    }

    App name(String name) {
        this.name = name;
        return this;
    }

    public String name() {
        return name;
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

    public CliDispatcher cliDispatcher() {
        return cliDispatcher;
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

    public synchronized void detectChanges() {
        classLoader.detectChanges();
        if (null != compilationException) {
            throw ActServerError.of(compilationException);
        }
    }

    public void restart() {
        build();
        refresh();
    }

    /**
     * In dev mode it could request app to refresh. However if
     * the request is issued in a thread that will be interrupted
     * e.g. the cli thread, it should call refresh in an new thread
     */
    public void asyncRefresh() {
        new Thread() {
            @Override
            public void run() {
                refresh();
            }
        }.start();
    }

    public synchronized void refresh() {
        logger.info("App starting ....");
        profile = null;

        initServiceResourceManager();
        eventEmitted = C.newSet();

        initSingletonRegistry();
        initEventBus();
        emit(EVENT_BUS_INITIALIZED);
        loadConfig();
        emit(CONFIG_LOADED);

        Act.viewManager().reload(this);

        initCache();
        initDataPropertyRepository();
        initCrypto();
        initIdGenerator();
        initJobManager();
        initDaemonRegistry();

        initInterceptorManager();
        initResolverManager();
        initBinderManager();
        initUploadFileStorageService();
        initRouters();
        emit(ROUTER_INITIALIZED);
        loadRoutes();
        emit(ROUTER_LOADED);
        initCliDispatcher();
        initCliServer();

        initDbServiceManager();
        emit(DB_SVC_LOADED);

        loadGlobalPlugin();
        initScannerManager();
        loadActScanners();
        loadBuiltInScanners();
        emit(PRE_LOAD_CLASSES);

        initClassLoader();
        emit(AppEventId.CLASS_LOADER_INITIALIZED);
        preloadClasses();
        try {
            scanAppCodes();
            compilationException = null;
        } catch (CompilationException e) {
            compilationException = e;
            throw ActServerError.of(e);
        }
        //classLoader().loadClasses();
        emit(APP_CODE_SCANNED);
        emit(CLASS_LOADED);

        initMailerConfigManager();

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
        emit(DEPENDENCY_INJECTOR_LOADED);
        emit(DEPENDENCY_INJECTOR_PROVISIONED);
        emit(SINGLETON_PROVISIONED);
        emit(PRE_START);
        emit(START);
        logger.info("App[%s] started", name());
        emit(POST_START);
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

    public SingletonRegistry singletonRegistry() {
        return singletonRegistry;
    }

    public void registerDaemon(Daemon dameon) {
        daemonRegistry.put(dameon.id(), dameon);
    }

    public <T> void registerSingleton(Class<? extends T> cls, T instance) {
        singletonRegistry.register(cls, instance);
    }

    public void registerSingletonClass(Class<?> aClass) {
        singletonRegistry.register(aClass);
    }

    public void registerSingleton(Object instance) {
        singletonRegistry.register(instance.getClass(), instance);
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

    public <T> T singleton(Class<T> clz) {
        return singletonRegistry.get(clz);
    }

    public <T> T newInstance(Class<T> clz) {
        if (App.class == clz) return $.cast(this);
        if (AppConfig.class == clz) return $.cast(config());
        if (AppCrypto.class == clz) return $.cast(crypto());
        if (AppJobManager.class == clz) return $.cast(jobManager());
        if (EventBus.class == clz) return $.cast(eventBus());
        if (CacheService.class == clz) return $.cast(cache());
        T t = singleton(clz);
        if (null != t) {
            return t;
        }
        if (null != dependencyInjector) {
            return dependencyInjector.create(clz);
        } else {
            return $.newInstance(clz);
        }
    }

    <T> T newInstance(Class<T> clz, ActContext context) {
        if (App.class == clz) return $.cast(this);
        if (AppConfig.class == clz) return $.cast(config());
        if (ActionContext.class == clz) return $.cast(context);
        if (CliContext.class == clz) return $.cast(context);
        if (MailerContext.class == clz) return $.cast(context);
        if (AppCrypto.class == clz) return $.cast(crypto());
        T t = singleton(clz);
        if (null != t) {
            return t;
        }
        if (null != dependencyInjector) {
            return dependencyInjector.createContextAwareInjector(context).create(clz);
        } else {
            return $.newInstance(clz);
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
            return $.eq(that.appBase, appBase);
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
        if (null == appServiceRegistry) {
            return this; // for unit test only
        }
        appServiceRegistry.register(service);
        if (null != eventBus && !noDiBinder) {
            eventBus.bind(AppEventId.DEPENDENCY_INJECTOR_LOADED, new AppEventListenerBase() {
                @Override
                public void on(EventObject event) throws Exception {
                    final App app = App.this;
                    eventBus.emit(new DependencyInjectionBinder(app, service.getClass()) {
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

    public void emit(AppEventId appEvent) {
        currentState = appEvent;
        eventEmitted().add(appEvent);
        EventBus bus = eventBus();
        if (null != bus) {
            bus.emit(appEvent);
        }
    }

    private Set<AppEventId> eventEmitted() {
        return eventEmitted;
    }

    public boolean eventEmitted(AppEventId appEvent) {
        return eventEmitted().contains(appEvent);
    }

    public AppEventId currentState() {
        return currentState;
    }

    private void loadConfig() {
        JsonUtilConfig.configure(this);
        File conf = RuntimeDirs.conf(this);
        logger.debug("loading app configuration: %s ...", appBase.getPath());
        config = new AppConfLoader().load(conf);
        config.app(this);
        registerValueObjectCodec();
    }

    // TODO: move this to somewhere that is more appropriate
    private void registerValueObjectCodec() {
        ValueObject.register(new JodaDateTimeCodec(config));
    }

    private void initIdGenerator() {
        idGenerator = new IdGenerator(
                config().nodeIdProvider(),
                config().startIdProvider(),
                config().sequenceProvider()
        );
    }

    private void initDaemonRegistry() {
        if (null != daemonRegistry) {
            Destroyable.Util.tryDestroyAll(daemonRegistry.values());
        }
        daemonRegistry = C.newMap();
        jobManager.on(AppEventId.START, new Runnable() {
            @Override
            public void run() {
                jobManager.fixedDelay("daemon-keeper", new Runnable() {
                    @Override
                    public void run() {
                        daemonKeeper();
                    }
                }, "5mn");
            }
        });
    }

    private void daemonKeeper() {
        for (Daemon d : daemonRegistry.values()) {
            if (d.state() == Daemon.State.STOPPED) {
                try {
                    d.start();
                } catch (Exception e) {
                    logger.error(e, "Error starting daemon [%s]", d.id());
                }
            }
        }
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

    private void initCliDispatcher() {
        cliDispatcher = new CliDispatcher(this);
    }

    private void initCliServer() {
        try {
            cliServer = new CliServer(this);
        } catch (Exception e) {
            logger.error(e, "Error initializing CLI server");
        }
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
        HttpConfig.setCacheServiceProvider(new CacheServiceProvider() {
            @Override
            public CacheService get() {
                return cache;
            }

            @Override
            public CacheService get(String name) {
                return CacheServiceProvider.Impl.Simple.get(name);
            }
        });
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

    private void initDataPropertyRepository() {
        new DataPropertyRepository(this);
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
        scannerManager.register(new SimpleEventListenerByteCodeScanner());
        scannerManager.register(new CommanderByteCodeScanner());
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
        router().addMapping(H.Method.GET, "/asset/", new StaticFileGetter(layout().asset(base())), RouteSource.BUILD_IN);
        router().addMapping(H.Method.GET, "/~upload/{path}", new UploadFileStorageService.UploadFileGetter(), RouteSource.BUILD_IN);
    }

    private void initClassLoader() {
        classLoader = Act.mode().classLoader(this);
    }

    private void preloadClasses() {
        classLoader.preload();
    }

    private void initResolverManager() {
        resolverManager = new StringValueResolverManager(this);
        Osgl.propertyHandlerFactory = new ActPropertyHandlerFactory(this);
    }
    private void initBinderManager() {
        binderManager = new BinderManager(this);
    }

    private void initSingletonRegistry() {
        singletonRegistry = new SingletonRegistry(this);
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
