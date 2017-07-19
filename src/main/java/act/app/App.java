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

import act.Act;
import act.Destroyable;
import act.app.data.BinderManager;
import act.app.data.StringValueResolverManager;
import act.app.event.AppEventId;
import act.app.util.AppCrypto;
import act.app.util.NamedPort;
import act.boot.BootstrapClassLoader;
import act.boot.app.BlockIssueSignal;
import act.cli.CliDispatcher;
import act.cli.bytecode.CommanderByteCodeScanner;
import act.conf.AppConfLoader;
import act.conf.AppConfig;
import act.conf.AppConfigKey;
import act.controller.bytecode.ControllerByteCodeScanner;
import act.data.DataPropertyRepository;
import act.data.JodaDateTimeCodec;
import act.data.util.ActPropertyHandlerFactory;
import act.event.AppEventListenerBase;
import act.event.EventBus;
import act.event.bytecode.SimpleEventListenerByteCodeScanner;
import act.handler.RequestHandler;
import act.handler.builtin.StaticResourceGetter;
import act.handler.builtin.controller.FastRequestHandler;
import act.inject.DependencyInjectionBinder;
import act.inject.DependencyInjector;
import act.inject.genie.GenieInjector;
import act.inject.genie.GenieModuleScanner;
import act.inject.param.JsonDTOClassManager;
import act.inject.param.ParamValueLoaderManager;
import act.job.AppJobManager;
import act.job.bytecode.JobByteCodeScanner;
import act.mail.MailerConfigManager;
import act.mail.bytecode.MailerByteCodeScanner;
import act.route.RouteSource;
import act.route.RouteTableRouterBuilder;
import act.route.Router;
import act.util.*;
import act.view.ActErrorResult;
import act.view.ImplicitVariableProvider;
import act.view.rythm.JodaDateTimeFormatter;
import act.view.rythm.JodaTransformers;
import act.view.rythm.RythmTransformerScanner;
import act.view.rythm.RythmView;
import act.ws.SecureTicketCodec;
import act.ws.SecureTicketHandler;
import act.ws.WebSocketConnectionManager;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.cache.CacheService;
import org.osgl.http.H;
import org.osgl.http.HttpConfig;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.mvc.MvcConfig;
import org.osgl.mvc.result.Result;
import org.osgl.storage.IStorageService;
import org.osgl.util.*;
import org.rythmengine.utils.I18N;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;

import static act.app.event.AppEventId.*;

/**
 * {@code App} represents an application that is deployed in a Act container
 */
public class App extends DestroyableBase {

    public interface HotReloadListener {
        void preHotReload();
    }

    @Deprecated
    public static final Logger logger = Act.LOGGER;

    public static final Logger LOGGER = Act.LOGGER;

    private static App INST;

    public enum F {
        ;
        public static $.Predicate<String> JAVA_SOURCE = S.F.endsWith(".java");
        public static $.Predicate<String> JAR_FILE = S.F.endsWith(".jar");
        public static $.Predicate<String> CONF_FILE = S.F.endsWith(".conf").or(S.F.endsWith(".properties").or(S.F.endsWith(".yaml").or(S.F.endsWith(".yml").or(S.F.endsWith(".xml")))));
        public static $.Predicate<String> ROUTES_FILE = $.F.eq(RouteTableRouterBuilder.ROUTES_FILE);
    }

    private volatile String profile;
    private String name;
    private String id;
    private File appBase;
    private File appHome;
    private Router router;
    private CliDispatcher cliDispatcher;
    private Map<NamedPort, Router> moreRouters;
    private AppConfig<?> config;
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
    private WebSocketConnectionManager webSocketConnectionManager;
    private AppCrypto crypto;
    private IdGenerator idGenerator;
    private CacheService cache;
    // used in dev mode only
    private CompilationException compilationException;
    private AppEventId currentState;
    private Set<AppEventId> eventEmitted;
    private Thread mainThread;
    private Set<String> scanList;
    private List<File> baseDirs;
    private volatile File tmpDir;
    private boolean restarting;
    private Result blockIssue;
    private Throwable blockIssueCause;
    private RequestHandler blockIssueHandler = new FastRequestHandler() {
        @Override
        public void handle(ActionContext context) {
            E.illegalArgumentIf(null == blockIssue);
            blockIssue.apply(context.req(), context.resp());
        }
    };
    private List<HotReloadListener> hotReloadListeners = new ArrayList<>();

    protected App() {
        INST = this;
    }

    protected App(File appBase, ProjectLayout layout) {
        this("MyApp", appBase, layout);
    }

    protected App(String name, File appBase, ProjectLayout layout) {
        this.name = name;
        this.id = generateId(name);
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
        this.id = generateId(name);
        return this;
    }

    public String name() {
        return name;
    }

    /**
     * Returns short id which is derived from passed in app name.
     *
     * **Note** `App.id()` is by no means to create a unique identifier of application.
     *
     * @return the short name
     */
    public String id() {
        return id;
    }

    private static String generateId(String name) {
        String id;
        if (S.blank(name) || "MyApp".equals(name)) {
            return "act";
        }
        String[] sa = name.split("[\\s]+");
        int len = sa.length;
        switch (len) {
            case 1:
                String s = sa[0];
                id = s.length() > 2 ? s.substring(0, 3) : s;
                break;
            case 2:
                String s1 = sa[0], s2 = sa[1];
                s1 = s1.length() > 1 ? s1.substring(0, 2) : s1;
                s2 = s2.length() > 1 ? s2.substring(0, 2) : s2;
                id = S.concat(s1, "-", s2);
                break;
            default:
                id = S.concat(
                        sa[0].substring(0, 1),
                        sa[1].substring(0, 1),
                        sa[2].substring(0, 1),
                        "-"
                );
        }
        return id;
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

    public Act.Mode mode() {
        return Act.mode();
    }

    public boolean isDev() {
        return mode().isDev();
    }

    public boolean isProd() {
        return mode().isProd();
    }

    public AppConfig<?> config() {
        return config;
    }

    public List<File> baseDirs() {
        if (null == baseDirs) {
            baseDirs = C.newList();
            if (null != appBase && appBase.isDirectory()) {
                baseDirs.add(appBase);
            }
            for (File baseDir : config.moduleBases()) {
                if (null != baseDir && baseDir.isDirectory()) {
                    baseDirs.add(baseDir);
                }
            }
        }
        return baseDirs;
    }

    public List<File> sourceDirs() {
        return layoutDirs(ProjectLayout.F.SRC.curry(layout()));
    }

    public List<File> resourceDirs() {
        return layoutDirs(ProjectLayout.F.RSRC.curry(layout()));
    }

    public List<File> libDirs() {
        return layoutDirs(ProjectLayout.F.LIB.curry(layout()));
    }

    public List<File> testSourceDirs() {
        return layoutDirs(ProjectLayout.F.TST_SRC.curry(layout()));
    }

    public List<File> testResourceDirs() {
        return layoutDirs(ProjectLayout.F.TST_RSRC.curry(layout()));
    }

    public List<File> testLibDirs() {
        return layoutDirs(ProjectLayout.F.TST_LIB.curry(layout()));
    }

    public List<File> allSourceDirs(boolean requireTestProfile) {
        List<File> dirs = C.newList();
        dirs.addAll(sourceDirs());
        if (!requireTestProfile || "test".equals(Act.profile())) {
            dirs.addAll(testSourceDirs());
        }
        return dirs;
    }

    public List<File> allResourceDirs(boolean requireTestProfile) {
        List<File> dirs = C.newList();
        dirs.addAll(resourceDirs());
        if (!requireTestProfile || "test".equals(Act.profile())) {
            dirs.addAll(testResourceDirs());
        }
        return dirs;
    }

    public List<File> allLibDirs(boolean requireTestProfile) {
        List<File> dirs = C.newList();
        dirs.addAll(libDirs());
        if (!requireTestProfile || "test".equals(Act.profile())) {
            dirs.addAll(testLibDirs());
        }
        return dirs;
    }

    private List<File> layoutDirs($.Function<File, File> transformer) {
        return C.list(baseDirs()).map(transformer);
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

    public void registerHotReloadListener(HotReloadListener listener) {
        if (Act.isDev()) {
            hotReloadListeners.add(listener);
        }
    }

    public boolean checkUpdates(boolean async) {
        if (!Act.isDev()) {
            return false;
        }
        synchronized (this) {
            try {
                detectChanges();
                return false;
            } catch (RequestRefreshClassLoader refreshRequest) {
                refresh(async);
                return true;
            } catch (RequestServerRestart requestServerRestart) {
                refresh(async);
                return true;
            }
        }
    }

    public synchronized void detectChanges() {
        if (null == classLoader) {
            throw new RequestServerRestart();
        }
        classLoader.detectChanges();
        if (null != compilationException) {
            throw ActErrorResult.of(compilationException);
        }
    }

    public void restart() {
        build();
        refresh();
    }

    public synchronized void setBlockIssue(Throwable e) {
        LOGGER.fatal(e, "Block issue encountered");
        if (null != blockIssue || null != blockIssueCause) {
            // do not overwrite previous block issue
            return;
        }
        if (e instanceof ActErrorResult) {
            blockIssue = (ActErrorResult) e;
        } else {
            if (null != classLoader()) {
                blockIssue = ActErrorResult.of(e);
                blockIssueCause = null;
            } else {
                blockIssueCause = e;
            }
        }
        throw BlockIssueSignal.INSTANCE;
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

    public boolean isStarted() {
        return currentState == POST_START || currentState == ACT_START;
    }

    public boolean isMainThread() {
        return Thread.currentThread() == mainThread;
    }

    public void shutdown() {
        Act.shutdownApp(this);
    }

    @Override
    protected void releaseResources() {
        // shall not interrupt main thread
        // see https://stackoverflow.com/questions/44665552/undertow-xnio-i-o-thread-consistently-eat-cpu
        // mainThread.interrupt();
        if (null == daemonRegistry) {
            return;
        }
        LOGGER.info("App shutting down ....");
        for (HotReloadListener listener : hotReloadListeners) {
            listener.preHotReload();
        }
        if (null != classLoader && config().i18nEnabled()) {
            // clear resource bundle cache for Act I18n
            ResourceBundle.clearCache(classLoader);
            // clear resource bundle cache for Rythm I18n
            ResourceBundle.clearCache(I18N.class.getClassLoader());
        }

        for (Daemon d : daemonRegistry.values()) {
            stopDaemon(d);
        }
        shutdownCliServer();
        shutdownEventBus();
        shutdownJobManager();
        clearServiceResourceManager();
        classLoader = null;
    }

    public synchronized void refresh(boolean async) {
        if (async) {
            asyncRefresh();
        } else {
            refresh();
        }
    }

    public synchronized boolean isRestarting() {
        return restarting;
    }

    public RequestHandler blockIssueHandler() {
        if (null != blockIssue && Act.isDev()) {
            return blockIssueHandler;
        }
        return null;
    }

    public synchronized void refresh() {
        currentState = null;
        final long ms = $.ms();
        LOGGER.info("App starting ....");
        profile = null;
        blockIssue = null;
        blockIssueCause = null;

        Act.viewManager().clearAppDefinedVars();
        initScanlist();
        initServiceResourceManager();
        reload();
        mainThread = Thread.currentThread();
        restarting = mainThread.getName().contains("job");
        eventEmitted = C.newSet();

        initSingletonRegistry();
        initEventBus();
        emit(EVENT_BUS_INITIALIZED);

        try {

            loadConfig();
            emit(CONFIG_LOADED);

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

            initWebSocketConnectionManager();
            initDbServiceManager();

            Act.viewManager().reset();
            loadGlobalPlugin();
            emit(APP_ACT_PLUGIN_LOADED);
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
                throw ActErrorResult.of(e);
            }
        } catch (BlockIssueSignal e) {
            return;
        }

        try {
            //classLoader().loadClasses();
            emit(APP_CODE_SCANNED);
            emit(CLASS_LOADED);
            Act.viewManager().reload(this);
        } catch (BlockIssueSignal e) {
            // ignore and keep going with dependency injector initialization
        }

        try {
            loadDependencyInjector();
            emit(DEPENDENCY_INJECTOR_LOADED);
        } catch (BlockIssueSignal e) {
            return;
        }

        if (null == blockIssue && null == blockIssueCause) {
            try {
                initJsonDTOClassManager();
                initParamValueLoaderManager();
                initMailerConfigManager();

                // setting context class loader here might lead to memory leaks
                // and cause weird problems as class loader been set to thread
                // could be switched to handling other app in ACT or still hold
                // old app class loader instance after the app been refreshed
                // - Thread.currentThread().setContextClassLoader(classLoader());

                initHttpConfig();
                initViewManager();

                // let's any emit the dependency injector loaded event
                // in case some other service depend on this event.
                // If any DI plugin e.g. guice has emitted this event
                // already, it doesn't matter we emit the event again
                // because once app event is consumed the event listeners
                // are cleared
                emit(DEPENDENCY_INJECTOR_PROVISIONED);
                emit(SINGLETON_PROVISIONED);
                config().preloadConfigurations();
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (null != blockIssueCause) {
                            setBlockIssue(blockIssueCause);
                        }
                        emit(PRE_START);
                        emit(STATELESS_PROVISIONED);
                        emit(START);
                        daemonKeeper();
                        LOGGER.info("App[%s] loaded in %sms", name(), $.ms() - ms);
                        emit(POST_START);
                    }
                };
                if (!dbServiceManager().hasDbService() || eventEmitted(DB_SVC_LOADED)) {
                    runnable.run();
                } else {
                    jobManager().on(DB_SVC_LOADED, runnable, true);
                }
            } catch (BlockIssueSignal e) {
                // ignore
            }
        }

    }

    /**
     * Check if the app has block issue set
     * @return `true` if the app has block issue encountered during start up
     */
    public boolean hasBlockIssue() {
        return null != blockIssue;
    }

    public AppBuilder builder() {
        return builder;
    }

    /**
     * Report if a class is registered into singleton registry
     * @param cls the class
     * @return `true` if the class is registered into singleton registry
     */
    public boolean isSingleton(Class<?> cls) {
        return null != singletonRegistry.get(cls) || hasSingletonAnnotation(cls);
    }

    private boolean hasSingletonAnnotation(Class<?> cls) {
        boolean found = false;
        GenieInjector injector = Act.app().injector();
        Annotation[] aa = cls.getAnnotations();
        for (Annotation a: aa) {
            Class<? extends Annotation> type = a.annotationType();
            if (injector.isInheritedScopeStopper(type)) {
                return false;
            }
            if (InheritedStateless.class == type || Stateless.class == type || Singleton.class == type) {
                found = true;
            }
        }
        return found;
    }

    void build() {
        builder = AppBuilder.build(this);
    }

    void hook() {
        Act.hook(this);
    }

    public File tmpDir() {
        if (null == tmpDir) {
            synchronized (this) {
                if (Act.isDev()) {
                    tmpDir = new File(this.layout().target(appBase), "tmp");
                } else {
                    try {
                        tmpDir = java.nio.file.Files.createTempDirectory(name()).toFile();
                    } catch (IOException e) {
                        throw E.ioException(e);
                    }
                }
            }
        }
        return tmpDir;
    }

    public File file(String path) {
        return new File(base(), path);
    }

    public File resource(String path) {
        return new File(this.layout().resource(appBase), path);
    }

    public void registerDaemon(Daemon daemon) {
        daemonRegistry.put(daemon.id(), daemon);
    }

    public void unregisterDaemon(Daemon daemon) {
        daemonRegistry.remove(daemon.id());
    }

    List<Daemon> registeredDaemons() {
        return C.list(daemonRegistry.values());
    }

    Daemon registeredDaemon(String id) {
        return daemonRegistry.get(id);
    }

    public <T> void registerSingleton(Class<? extends T> cls, T instance) {
        if (null != singletonRegistry) {
            singletonRegistry.register(cls, instance);
        }
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

    public BinderManager binderManager() {
        return binderManager;
    }

    public CacheService cache() {
        return cache;
    }

    public CacheService cache(String name) {
        return config().cacheService(name);
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

    public <DI extends DependencyInjector> App injector(DI dependencyInjector) {
        E.NPE(dependencyInjector);
        E.illegalStateIf(null != this.dependencyInjector, "Dependency injection factory already set");
        this.dependencyInjector = dependencyInjector;
        return this;
    }

    public <DI extends DependencyInjector> DI injector() {
        return (DI) dependencyInjector;
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

    /**
     * Get/Create new instance of a class specified by the className
     *
     * This method will call the build in {@link DependencyInjector}
     * to load the instance. And this is dependency inject process,
     * not a simple constructor call
     *
     * **Note** the class will be loaded by the app's {@link #classLoader()}
     *
     * @param className the className of the instance to be returned
     * @param <T>       the generic type of the class
     * @return the instance of the class
     */
    public <T> T getInstance(String className) {
        Class<T> c = $.classForName(className, classLoader());
        return getInstance(c);
    }

    /**
     * Get/Create new instance of a class
     *
     * This method will call the build in {@link DependencyInjector}
     * to load the instance. And this is dependency inject process,
     * not a simple constructor call
     *
     * @param clz the class
     * @param <T> the generic type of the class
     * @return the instance of the class
     */
    public <T> T getInstance(Class<T> clz) {
        if (null == dependencyInjector) {
            return $.newInstance(clz);
        }
        return dependencyInjector.get(clz);
    }

    /**
     * Load/get a class by name using the app's {@link #classLoader()}
     *
     * @param className the name of the class to be loaded
     * @return the class as described above
     */
    public Class<?> classForName(String className) {
        return $.classForName(className, classLoader());
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
        String path = appBase.getPath();
        return S.concat("app@[", path, "]");
    }

    /**
     * Return an ID in string that is unique across the cluster
     *
     * @return a cluster unique id
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
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(S.concat("emitting event: ", appEvent.name()));
        }
        currentState = appEvent;
        eventEmitted().add(appEvent);
        EventBus bus = eventBus();
        if (null != bus) {
            bus.emit(appEvent);
        }
    }

    public Set<String> scanList() {
        return new HashSet<String>(scanList);
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
        File resource = RuntimeDirs.resource(this);
        LOGGER.debug("loading app configuration: %s ...", appBase.getAbsolutePath());
        config = new AppConfLoader().load(resource);
        config.app(this);
        configureLoggingLevels();
        registerSingleton(AppConfig.class, config);
        registerValueObjectCodec();
        if (config.i18nEnabled()) {
            MvcConfig.enableLocalizedErrorMsg();
        }
    }

    private void initHttpConfig() {
        HttpConfig.secure(config.httpSecure());
        HttpConfig.securePort(config.httpExternalSecurePort());
        HttpConfig.nonSecurePort(config.httpExternalPort());
        HttpConfig.defaultLocale(config.locale());
        HttpConfig.domain(config.host());
    }

    // TODO: move this to somewhere that is more appropriate
    private void registerValueObjectCodec() {
        ValueObject.register(new JodaDateTimeCodec(config));
    }

    private void initIdGenerator() {
        idGenerator = new IdGenerator(
                config().nodeIdProvider(),
                config().startIdProvider(),
                config().sequenceProvider(),
                config().longEncoder()
        );
    }

    private void initDaemonRegistry() {
        if (null != daemonRegistry) {
            Destroyable.Util.tryDestroyAll(daemonRegistry.values(), ApplicationScoped.class);
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
                }, "1mn");
            }
        });
    }

    private void daemonKeeper() {
        final String KEY_COUNTER = "c";
        final String KEY_SEQ_NO = "sn";
        final String KEY_LAST_SEQ_NO = "lsn";
        for (final Daemon d : daemonRegistry.values()) {
            Daemon.State state = d.state();
            if (state == Daemon.State.STARTED) {
                // daemon running successfully, clear the error recovery state
                if (d.getAttribute(KEY_COUNTER) != null) {
                    d.removeAttribute(KEY_COUNTER);
                    d.removeAttribute(KEY_SEQ_NO);
                    d.removeAttribute(KEY_LAST_SEQ_NO);
                }
            } else if (state == Daemon.State.STOPPED) {
                startDaemon(d);
            } else if (state == Daemon.State.ERROR) {
                // try to recover daemon, the recovery duration shall
                // follow a fibonacci sequence
                Integer counter = d.getAttribute(KEY_COUNTER);
                Integer seqNo, lastSeqNo;
                if (null == counter) {
                    counter = 1;
                    seqNo = 1;
                    lastSeqNo = 0;
                } else {
                    seqNo = d.getAttribute(KEY_SEQ_NO);
                    lastSeqNo = d.getAttribute(KEY_LAST_SEQ_NO);
                }
                if (--counter == 0) {
                    startDaemon(d);
                    int nextSeqNo = seqNo + lastSeqNo;
                    lastSeqNo = seqNo;
                    seqNo = nextSeqNo;
                    counter = seqNo;
                    d.setAttribute(KEY_SEQ_NO, seqNo);
                    d.setAttribute(KEY_LAST_SEQ_NO, lastSeqNo);
                }
                d.setAttribute(KEY_COUNTER, counter);
            }
        }
    }

    private void startDaemon(final Daemon daemon) {
        jobManager().now(new Runnable() {
            @Override
            public void run() {
                try {
                    daemon.start();
                } catch (Exception e) {
                    LOGGER.error(e, "Error starting daemon [%s]", daemon.id());
                }
            }
        });
    }

    private void stopDaemon(final Daemon daemon) {
        daemon.stop();
    }

    private void initServiceResourceManager() {
        clearServiceResourceManager();
        appServiceRegistry = new AppServiceRegistry(this);
    }

    private void clearServiceResourceManager() {
        if (null != appServiceRegistry) {
            emit(STOP);
            appServiceRegistry.destroy();
            dependencyInjector = null;
            if (null != cache) {
                cache.shutdown();
            }
        }
    }

    private void initUploadFileStorageService() {
        uploadFileStorageService = UploadFileStorageService.create(this);
    }

    private void initCliDispatcher() {
        if (config().cliEnabled()) {
            cliDispatcher = new CliDispatcher(this);
        }
    }

    private void initCliServer() {
        if (config().cliEnabled()) {
            cliServer = new CliServer(this);
        }
    }

    private void shutdownCliServer() {
        if (null != cliServer) {
            cliServer.destroy();
        }
    }

    private void initRouters() {
        router = new Router(this);
        moreRouters = C.newMap();
        List<NamedPort> ports = config().namedPorts();
        for (NamedPort port : ports) {
            moreRouters.put(port, new Router(this, port.name()));
        }
        if (config.cliOverHttp()) {
            NamedPort cliOverHttp = new NamedPort(AppConfig.PORT_CLI_OVER_HTTP, config.cliOverHttpPort());
            moreRouters.put(cliOverHttp, new Router(this, AppConfig.PORT_CLI_OVER_HTTP));
        }
    }

    private void initEventBus() {
        eventBus = new EventBus(this);
    }

    public void shutdownEventBus() {
        if (null != eventBus) {
            eventBus.destroy();
        }
    }

    private void initCache() {
        cache = cache(config().cacheName());
        cache.startup();
        CacheService sessionCache = cache(config().cacheNameSession());
        if (cache != sessionCache) {
            sessionCache.startup();
        }
        HttpConfig.setSessionCache(sessionCache);
    }

    private void initCrypto() {
        crypto = new AppCrypto(config());
        registerSingleton(AppCrypto.class, crypto);
    }

    private void initJobManager() {
        jobManager = new AppJobManager(this);
    }

    private void shutdownJobManager() {
        if (null != jobManager) {
            jobManager.destroy();
        }
    }

    private void initScanlist() {
        ClassLoader classLoader = getClass().getClassLoader();
        if (classLoader instanceof BootstrapClassLoader) {
            scanList = ((BootstrapClassLoader) classLoader).scanList();
        }
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
        scannerManager.register(new GenieModuleScanner());
        scannerManager.register(new ClassInfoByteCodeScanner());
        scannerManager.register(new ClassFinderByteCodeScanner());
        scannerManager.register(new ControllerByteCodeScanner());
        scannerManager.register(new MailerByteCodeScanner());
        scannerManager.register(new JobByteCodeScanner());
        scannerManager.register(new SimpleBean.ByteCodeScanner());
        scannerManager.register(new SimpleEventListenerByteCodeScanner());
        scannerManager.register(new CommanderByteCodeScanner());
        scannerManager.register(new RythmTransformerScanner());
        scannerManager.register(new ImplicitVariableProvider.TemplateVariableScanner(this));
    }

    private void loadDependencyInjector() {
        DependencyInjector di = injector();
        if (null == di) {
            new GenieInjector(this);
        } else {
            LOGGER.warn("Third party injector[%s] loaded. Please consider using Act air injection instead", di.getClass());
        }
    }

    private void loadRoutes() {
        loadBuiltInRoutes();
        LOGGER.debug("loading app routing table: %s ...", appBase.getPath());
        List<File> routes;
        if (Act.isProd()) {
            routes = RuntimeDirs.routes(this);
        } else {
            routes = layout().routeTables(base());
        }
        for (File route : routes) {
            if (route.exists() && route.canRead() && route.isFile()) {
                List<String> lines = IO.readLines(route);
                new RouteTableRouterBuilder(lines).build(router);
            }
        }
    }

    private void loadBuiltInRoutes() {
        router().addMapping(H.Method.GET, "/asset/", new StaticResourceGetter("asset"), RouteSource.BUILD_IN);
        router().addMapping(H.Method.GET, "/asset/act/", new StaticResourceGetter("asset/act"), RouteSource.BUILD_IN);
        if (config().allowDownloadUploadFile()) {
            router().addMapping(H.Method.GET, "/~upload/{path}", new UploadFileStorageService.UploadFileGetter(), RouteSource.BUILD_IN);
        }
        router().addContext("act.", "/~");
        if (config.cliOverHttp()) {
            Router router = router(AppConfig.PORT_CLI_OVER_HTTP);
            router.addMapping(H.Method.GET, "/asset/", new StaticResourceGetter("asset"), RouteSource.BUILD_IN);
        }
        SecureTicketCodec secureTicketCodec = config.secureTicketCodec();
        SecureTicketHandler secureTicketHandler = new SecureTicketHandler(secureTicketCodec);
        router().addMapping(H.Method.GET, "/~/ticket", secureTicketHandler);
    }

    private void initClassLoader() {
        classLoader = Act.mode().classLoader(this);
    }

    private void initJsonDTOClassManager() {
        new JsonDTOClassManager(this);
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

    private void initWebSocketConnectionManager() {
        webSocketConnectionManager = new WebSocketConnectionManager(this);
    }

    private void initParamValueLoaderManager() {
        new ParamValueLoaderManager(this);
    }

    private void initSingletonRegistry() {
        singletonRegistry = new SingletonRegistry(this);
        singletonRegistry.register(App.class, this);
        singletonRegistry.register(SingletonRegistry.class, singletonRegistry);
        appServiceRegistry.bulkRegisterSingleton();
    }

    private void loadPlugins() {
        // TODO: load app level plugins
    }

    private void initViewManager() {
        Act.viewManager().onAppStart();
        registerBuiltInRythmTransformers();
    }

    private void registerBuiltInRythmTransformers() {
        RythmView rythmView = (RythmView) Act.viewManager().view(RythmView.ID);
        rythmView.registerBuiltInTransformer(this, JodaTransformers.class);
        rythmView.registerFormatter(this, new JodaDateTimeFormatter());
    }

    private void configureLoggingLevels() {
        Map map = config().subSet("log.level");
        map.putAll(config().subSet("act.log.level"));
        for (Object o : map.entrySet()) {
            Map.Entry<String, String> entry = $.cast(o);
            String key = entry.getKey();
            if (key.startsWith("log.level")) {
                key = key.substring("log.level.".length());
            } else {
                key = key.substring("act.log.level.".length());
            }
            Logger.Level level = loggerLevelOf(entry.getValue());
            E.invalidConfigurationIf(null == level, "Unknown log level: %s", entry.getValue());
            Logger logger = LogManager.get(key);
            logger.setLevel(level);
        }
    }

    private Logger.Level loggerLevelOf(String s) {
        Map<String, Logger.Level> map = new HashMap<>();
        for (Logger.Level level : Logger.Level.values()) {
            map.put(level.name().toUpperCase(), level);
        }
        map.put("WARNING", Logger.Level.WARN);
        return map.get(s.toUpperCase());
    }

    private void scanAppCodes() {
        classLoader().scan();
    }

    static App create(File appBase, ProjectLayout layout) {
        return new App(appBase, layout);
    }


}
