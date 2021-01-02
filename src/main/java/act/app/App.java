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

import static act.app.event.SysEventId.*;
import static act.util.TopLevelDomainList.CRON_TLD_RELOAD;
import static org.osgl.http.H.Method.GET;
import static org.osgl.util.S.F.contains;
import static org.osgl.util.S.F.endsWith;

import act.Act;
import act.Destroyable;
import act.apidoc.ApiManager;
import act.apidoc.SampleDataProviderManager;
import act.app.data.BinderManager;
import act.app.data.StringValueResolverManager;
import act.app.event.SysEventId;
import act.app.util.NamedPort;
import act.asm.AsmException;
import act.asm.ClassReader;
import act.asm.tree.ClassNode;
import act.asm.tree.*;
import act.asm.util.*;
import act.boot.BootstrapClassLoader;
import act.boot.app.BlockIssueSignal;
import act.cli.CliDispatcher;
import act.cli.bytecode.CommanderByteCodeScanner;
import act.conf.*;
import act.controller.bytecode.ControllerByteCodeScanner;
import act.controller.captcha.CaptchaManager;
import act.crypto.AppCrypto;
import act.data.DataPropertyRepository;
import act.data.JodaDateTimeCodec;
import act.data.util.ActPropertyHandlerFactory;
import act.db.meta.EntityInfoByteCodeScanner;
import act.db.meta.MasterEntityMetaInfoRepo;
import act.event.EventBus;
import act.event.SysEventListenerBase;
import act.event.bytecode.SimpleEventListenerByteCodeScanner;
import act.handler.RequestHandler;
import act.handler.builtin.ResourceGetter;
import act.handler.builtin.controller.FastRequestHandler;
import act.httpclient.HttpClientService;
import act.i18n.I18n;
import act.inject.*;
import act.inject.genie.*;
import act.inject.param.*;
import act.job.JobManager;
import act.job.bytecode.JobByteCodeScanner;
import act.mail.MailerConfigManager;
import act.mail.bytecode.MailerByteCodeScanner;
import act.meta.ClassMetaInfoBase;
import act.meta.ClassMetaInfoManager;
import act.metric.*;
import act.plugin.PrincipalProvider;
import act.route.*;
import act.session.CookieSessionMapper;
import act.session.SessionManager;
import act.sys.SystemAvailabilityMonitor;
import act.util.*;
import act.validation.Password;
import act.view.ActErrorResult;
import act.view.ImplicitVariableProvider;
import act.view.rythm.*;
import act.ws.*;
import org.osgl.$;
import org.osgl.Lang;
import org.osgl.cache.CacheService;
import org.osgl.cache.CacheServiceProvider;
import org.osgl.http.HttpConfig;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.mvc.MvcConfig;
import org.osgl.mvc.result.Result;
import org.osgl.util.*;
import org.rythmengine.utils.I18N;
import osgl.version.Version;

import java.io.*;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;

/**
 * {@code App} represents an application that is deployed in a Act container
 */
public class App extends LogSupportedDestroyableBase {

    public interface HotReloadListener {
        void preHotReload();
    }

    private static App INST;

    public enum F {
        ;
        public static $.Predicate<String> JAVA_SOURCE = endsWith(".java");
        public static $.Predicate<String> JAR_FILE = endsWith(".jar");
        public static $.Predicate<String> CONF_FILE = endsWith(".conf").or(endsWith(".properties").or(endsWith(".yaml").or(endsWith(".yml").or(endsWith(".xml"))))).and(contains("main/resources/test").negate());
        public static $.Predicate<String> ROUTES_FILE = $.F.eq(RouteTableRouterBuilder.ROUTES_FILE).or($.F.eq(RouteTableRouterBuilder.ROUTES_FILE2));
    }

    private volatile String profile;
    private String name;
    private String shortId;
    private File appBase;
    private File appHome;
    private Router router;
    private CliDispatcher cliDispatcher;
    private CaptchaManager captchaManager;
    private Map<NamedPort, Router> moreRouters;
    private AppConfig<?> config;
    private AppClassLoader classLoader;
    private ProjectLayout layout;
    private AppBuilder builder;
    private EventBus eventBus;
    private AppCodeScannerManager scannerManager;
    private ClassMetaInfoRepo classMetaInfoRepo;
    private DbServiceManager dbServiceManager;
    private JobManager jobManager;
    private ApiManager apiManager;
    private ManagedCollectionService managedCollectionService;
    private CliServer cliServer;
    private MailerConfigManager mailerConfigManager;
    private StringValueResolverManager resolverManager;
    private SingletonRegistry singletonRegistry;
    private BinderManager binderManager;
    private AppInterceptorManager interceptorManager;
    private GenieInjector dependencyInjector;
    private HttpClientService httpClientService;
    private UploadFileStorageService uploadFileStorageService;
    private AppServiceRegistry appServiceRegistry;
    private MasterEntityMetaInfoRepo entityMetaInfoRepo;
    private Map<String, Daemon> daemonRegistry;
    private WebSocketConnectionManager webSocketConnectionManager;
    private MetricMetaInfoRepo metricMetaInfoRepo;
    private SampleDataProviderManager sampleDataProviderManager;
    private AppCrypto crypto;
    private IdGenerator idGenerator;
    private SessionManager sessionManager;
    private CacheService cache;
    // used in dev mode only
    private CompilationException compilationException;
    private AsmException asmException;
    private SysEventId currentState;
    private boolean hasStarted;
    private Set<SysEventId> eventEmitted;
    private Thread mainThread;
    private Set<String> jarFileBlackList;
    private Set<String> jarFileBlackList2;
    private Set<String> scanList;
    private Set<Pattern> scanPatterns;
    private Set<String> scanPrefixList;
    private Set<String> scanSuffixList;
    private List<File> baseDirs;
    private volatile File tmpDir;
    private Result blockIssue;
    private Throwable blockIssueCause;
    private RequestHandler blockIssueHandler = new FastRequestHandler() {
        @Override
        public void handle(ActionContext context) {
            E.illegalArgumentIf(null == blockIssue);
            blockIssue.apply(context.req(), context.prepareRespForResultEvaluation());
        }

        @Override
        public String toString() {
            return "fatal error: block issue found";
        }
    };
    private final Version version;
    private List<HotReloadListener> hotReloadListeners = new ArrayList<>();
    private PrincipalProvider principalProvider = PrincipalProvider.DefaultPrincipalProvider.INSTANCE;

    // lock app hot reload process
    private final AtomicBoolean loading = new AtomicBoolean();

    // for unit test purpose
    private App() {
        this.version = Version.get();
        this.appBase = new File(".");
        this.layout = ProjectLayout.PredefinedLayout.MAVEN;
        this.appHome = RuntimeDirs.home(this);
        this.config = new AppConfig<>().app(this);
        INST = this;
        Lang.setFieldValue("metricPlugin", Act.class, new SimpleMetricPlugin());
        this.eventEmitted = new HashSet<>();
        this.eventBus = new EventBus(this);
        this.jobManager = new JobManager(this);
        this.classLoader = new AppClassLoader(this);
        this.sessionManager = new SessionManager(this.config, config().cacheService("logout-session"));
        this.dependencyInjector = new GenieInjector(this);
        this.singletonRegistry = new SingletonRegistry(this);
        this.singletonRegistry.register(SingletonRegistry.class, this.singletonRegistry);
        this.singletonRegistry.register(SessionManager.class, this.sessionManager);
        this.singletonRegistry.register(CookieSessionMapper.class, new CookieSessionMapper(this.config));
        this.idGenerator = new IdGenerator();
    }

    protected App(File appBase, Version version, ProjectLayout layout) {
        this("MyApp", version, appBase, layout);
    }

    protected App(String name, Version version, File appBase, ProjectLayout layout) {
        this.name(name);
        this.version = version;
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
        this.shortId = generateId(name);
        return this;
    }

    /**
     * Returns the name of the app
     *
     * @return the app name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the app version
     *
     * @return the app version
     */
    public Version version() {
        return version;
    }

    /**
     * Returns short id which is derived from passed in app name.
     *
     * **Note** `App.shortId()` is by no means to create a unique identifier of application.
     *
     * @return the short id of the app
     */
    public String shortId() {
        return shortId;
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
            baseDirs = new ArrayList<>();
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

    public List<File> allSourceDirs() {
        List<File> dirs = new ArrayList<>();
        dirs.addAll(sourceDirs());
        return dirs;
    }

    public List<File> allResourceDirs(boolean requireTestProfile) {
        List<File> dirs = new ArrayList<>();
        dirs.addAll(resourceDirs());
        if (!requireTestProfile || "test".equals(Act.profile())) {
            dirs.addAll(testResourceDirs());
        }
        return dirs;
    }

    public List<File> allLibDirs(boolean requireTestProfile) {
        List<File> dirs = new ArrayList<>();
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

    public CliServer cliServer() {
        return cliServer;
    }

    public CaptchaManager captchaManager() {
        return captchaManager;
    }

    public Router router() {
        return router;
    }

    public Router sysRouter() {
        return router(Router.PORT_SYS);
    }

    public Router cliOverHttpRouter() {
        return router(Router.PORT_CLI_OVER_HTTP);
    }

    public Router adminRouter() {
        return router(Router.PORT_ADMIN);
    }

    public Router router(String name) {
        if (S.blank(name) || "default".equalsIgnoreCase(name)) {
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

    private Iterable<Router> allRouters() {
        return new Iterable<Router>() {
            @Override
            public Iterator<Router> iterator() {
                return new Iterator<Router>() {

                    private Iterator<Router> moreRoutersIterator;

                    @Override
                    public boolean hasNext() {
                        return null == moreRoutersIterator || moreRoutersIterator.hasNext();
                    }

                    @Override
                    public Router next() {
                        if (null == moreRoutersIterator) {
                            moreRoutersIterator = moreRouters.values().iterator();
                            return router;
                        }
                        return moreRoutersIterator.next();
                    }

                    @Override
                    public void remove() {
                        E.unsupport();
                    }
                };
            }
        };
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

    /**
     * Check if app needs updates.
     *
     * @param async
     *         refresh app async or sync
     * @return `true` if there are updates and app require hot reload
     */
    public boolean checkUpdates(boolean async) {
        return checkUpdates(async, false);
    }

    public boolean checkUpdatesNonBlock(boolean async) {
        return checkUpdates(async, true);
    }

    private boolean checkUpdates(boolean async, boolean nonBlock) {
        if (!Act.isDev()) {
            return false;
        }
        if (loading.get()) {
            if (nonBlock) {
                return true;
            }
            synchronized (this) {
                while (loading.get()) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        throw E.unexpected(e);
                    }
                }
            }
            return false;
        }
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

    public synchronized void detectChanges() {
        if (null == classLoader) {
            throw new RequestServerRestart();
        }
        classLoader.detectChanges();
        if (null != compilationException) {
            throw ActErrorResult.of(compilationException);
        } else if (null != asmException) {
            throw ActErrorResult.of(asmException);
        }
    }

    public void restart() {
        build();
        refresh();
    }

    public synchronized void handleBlockIssue(Throwable e) {
        fatal(e, "Block issue encountered");
        if (Act.isDev()) {
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
        } else {
            Act.shutdown(App.instance());
        }
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
        if (null == currentState) {
            return false;
        }
        switch (currentState) {
            case POST_START:
            case POST_STARTED:
            case ACT_START:
                return true;
            default:
                return false;
        }
    }

    /**
     * Check if the app has been started before. This could be useful to
     * fix some state issue that caused by hotreload in dev mode
     */
    public boolean wasStarted() {
        return hasStarted;
    }

    public boolean isLoading() {
        return loading.get();
    }

    public boolean isMainThread() {
        return Thread.currentThread() == mainThread;
    }

    public void shutdown() {
        Act.shutdown(this);
    }

    public void shutdown(int exitCode) {
        Act.shutdown(this, exitCode);
    }


    @Override
    protected void releaseResources() {
        // shall not interrupt main thread
        // see https://stackoverflow.com/questions/44665552/undertow-xnio-i-o-thread-consistently-eat-cpu
        // mainThread.interrupt();
        if (null == daemonRegistry) {
            return;
        }
        info("Shutting down app [%s]....", name());
        if (Act.isDev()) {
            for (HotReloadListener listener : hotReloadListeners) {
                listener.preHotReload();
            }
            if (null != classLoader && config().i18nEnabled()) {
                debug("clearing resource bundle with classLoader: %s", classLoader);
                ResourceBundle.clearCache(App.class.getClassLoader());
                // clear resource bundle cache for Act I18n
                ResourceBundle.clearCache(classLoader);
                // clear resource bundle cache for Rythm I18n
                ResourceBundle.clearCache(I18N.class.getClassLoader());
                I18N.clearBundleCache();
            }
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

    public RequestHandler blockIssueHandler() {
        if (null != blockIssue && Act.isDev()) {
            return blockIssueHandler;
        }
        return null;
    }

    public synchronized void refresh() {
        startLoading();
        try {
            currentState = null;
            final long ms = $.ms();
            info("App starting ....");
            profile = null;
            blockIssue = null;
            blockIssueCause = null;

            Act.viewManager().clearAppDefinedVars();
            initScanList();
            initJarFileBlackList();
            initServiceResourceManager();
            reload();

            managedCollectionService = new ManagedCollectionService(this);
            I18n.classInit(this);
            ReflectedInvokerHelper.classInit(this);
            ParamValueLoaderService.classInit(this);
            JodaTransformers.classInit(this);
            FastJsonPropertyPreFilter.classInit(this);
            ActErrorResult.classInit(this);
            ActProviders.classInit(this);
            ProvidedValueLoader.classInit(this);
            GenieFactoryFinder.classInit(this);

            mainThread = Thread.currentThread();
            eventEmitted = C.newSet();

            initSingletonRegistry();
            initEventBus();
            emit(EVENT_BUS_INITIALIZED);

            loadConfig();
            profile(); // ensure profile get loaded
            emit(CONFIG_LOADED);

            initDataPropertyRepository();
            initCrypto();
            initIdGenerator();
            initJobManager();
            initDaemonRegistry();

            initInterceptorManager();
            initResolverManager();
            initBinderManager();
            initUploadFileStorageService();
            initClassLoader();
            emit(CLASS_LOADER_INITIALIZED);
            initRouters();
            emit(ROUTER_INITIALIZED);
            loadRoutes();
            emit(ROUTER_LOADED);
            initApiManager();
            initSampleDataProviderManager();
            initHttpClientService();
            initCaptchaPluginManager();
            initCliDispatcher();
            initCliServer();
            initMetricMetaInfoRepo();
            initEntityMetaInfoRepo();
            initClassMetaInfoRepo();

            initWebSocketConnectionManager();
            initDbServiceManager();

            Act.viewManager().reset();
            loadGlobalPlugin();
            emit(APP_ACT_PLUGIN_LOADED);
            initScannerManager();
            loadActScanners();
            loadBuiltInScanners();
            emit(PRE_LOAD_CLASSES);

            initCache();
            preloadClasses();
            try {
                scanAppCodes();
                compilationException = null;
                asmException = null;
            } catch (CompilationException e) {
                compilationException = e;
                throw ActErrorResult.of(e);
            } catch (AsmException e) {
                asmException = e;
                throw ActErrorResult.of(e);
            }

            emit(APP_CODE_SCANNED);
            emit(CLASS_LOADED);
            Act.viewManager().reload(this);

            loadDependencyInjector();
            emit(DEPENDENCY_INJECTOR_INITIALIZED);
            dependencyInjector.unlock();
            emit(DEPENDENCY_INJECTOR_LOADED);

            if (null == blockIssue && null == blockIssueCause) {
                final Runnable runnable1 = new Runnable() {
                    @Override
                    public void run() {
                        if ($.bool("false")) {
                            $.nil();
                        }
                        initJsonDtoClassManager();
                        initParamValueLoaderManager();
                        initMailerConfigManager();

                        // setting context class loader here might lead to memory leaks
                        // and cause weird problems as class loader been set to thread
                        // could be switched to handling other app in ACT or still hold
                        // old app class loader instance after the app been refreshed
                        // - Thread.currentThread().setContextClassLoader(classLoader());

                        initHttpConfig();
                        initViewManager();

                        registerMetricProvider();
                        config().preloadConfigurations();

                        // let's any emit the dependency injector loaded event
                        // in case some other service depend on this event.
                        // If any DI plugin e.g. guice has emitted this event
                        // already, it doesn't matter we emit the event again
                        // because once app event is consumed the event listeners
                        // are cleared
                        emit(DEPENDENCY_INJECTOR_PROVISIONED);
                        initSessionManager();
                        emit(SINGLETON_PROVISIONED);
                    }
                };
                if (!isDevColdStart()) {
                    runnable1.run();
                } else {
                    jobManager.now("post_di_load_init", runnable1);
                }
                Runnable runnable2 = new Runnable() {
                    @Override
                    public void run() {
                        if (null != blockIssueCause) {
                            handleBlockIssue(blockIssueCause);
                        }
                        emit(PRE_START);
                        emit(STATELESS_PROVISIONED);
                        $$.init();
                        emit(START);
                        if (isProd() || !wasStarted()) {
                            debug("App[%s] loaded in %sms", name(), $.ms() - ms);
                        } else {
                            info("App[%s] reloaded in %sms\n\n", name(), $.ms() - ms);
                        }
                        hasStarted = true;
                        daemonKeeper();
                        loadingDone();
                        emit(POST_START);
                        jobManager.post(SysEventId.DB_SVC_LOADED, new Runnable() {
                            @Override
                            public void run() {
                                emit(POST_STARTED);
                            }
                        }, true);
                    }
                };
                if (!dbServiceManager().hasDbService() || eventEmitted(DB_SVC_PROVISIONED)) {
                    if (Act.isDev()) {
                        jobManager.post(SINGLETON_PROVISIONED, "App:postDbSvcLogic", runnable2, true);
                    } else {
                        runnable2.run();
                    }
                } else {
                    jobManager().on(DB_SVC_PROVISIONED, "App:postDbSvcLogic", runnable2, true);
                }
            }
        } catch (BlockIssueSignal e) {
            loadingDone();
        } catch (RuntimeException e) {
            loadingDone();
            throw e;
        }
    }

    private boolean isDevColdStart() {
        return Act.isDev() && !wasStarted();
    }

    /**
     * Check if the app has block issue set
     *
     * @return `true` if the app has block issue encountered during start up
     */
    public boolean hasBlockIssue() {
        return null != blockIssue;
    }

    public AppBuilder builder() {
        return builder;
    }

    public <T extends ClassMetaInfoBase<T>> ClassMetaInfoManager<T> classMetaInfoManager(Class<T> metaInfoType) {
        return classMetaInfoRepo.manager(metaInfoType);
    }

    public void register(ClassMetaInfoManager<?> classMetaInfoManager) {
        classMetaInfoRepo.register(classMetaInfoManager);
    }

    /**
     * Report if a class is registered into singleton registry
     *
     * @param cls
     *         the class
     * @return `true` if the class is registered into singleton registry
     */
    public boolean isSingleton(Class<?> cls) {
        return null != singletonRegistry.get(cls) || hasSingletonAnnotation(cls);
    }

    public boolean isSingleton(Object o) {
        if (null == o) {
            return true;
        }
        Class type = o instanceof Class ? (Class) o : o.getClass();
        return isSingleton(type);
    }

    private void registerMetricProvider() {
        GenieInjector gi = this.injector();
        MetricProvider mp = new MetricProvider();
        gi.registerNamedProvider(Metric.class, mp);
        gi.registerProvider(Metric.class, mp);
    }

    private boolean hasSingletonAnnotation(Class<?> cls) {
        boolean found = false;
        GenieInjector injector = Act.app().injector();
        Annotation[] aa = cls.getAnnotations();
        for (Annotation a : aa) {
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

    public File testResource(String path) {
        return new File(this.layout.testResource(appBase), path);
    }

    public SampleDataProviderManager sampleDataProviderManager() {
        return sampleDataProviderManager;
    }

    public void registerDaemon(Daemon daemon) {
        daemonRegistry.put(daemon.id(), daemon);
    }

    public void unregisterDaemon(Daemon daemon) {
        daemonRegistry.remove(daemon.id());
    }

    public List<Daemon> registeredDaemons() {
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

    public SessionManager sessionManager() {
        return sessionManager;
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

    public HttpClientService httpClientService() {
        return httpClientService;
    }

    public MasterEntityMetaInfoRepo entityMetaInfoRepo() {
        return entityMetaInfoRepo;
    }

    public JobManager jobManager() {
        return jobManager;
    }

    public MetricMetaInfoRepo metricMetaInfoRepo() {
        return metricMetaInfoRepo;
    }

    @Deprecated
    public <DI extends DependencyInjector> App injector(DI dependencyInjector) {
        E.NPE(dependencyInjector);
        E.illegalStateIf(null != this.dependencyInjector, "Dependency injection factory already set");
        throw E.unsupport();
    }

    public <DI extends DependencyInjector> DI injector() {
        return (DI) dependencyInjector;
    }

    public SingletonRegistry singletonRegistry() {
        return singletonRegistry;
    }

    public UploadFileStorageService uploadFileStorageService() {
        return uploadFileStorageService;
    }

    public PrincipalProvider principalProvider() {
        return principalProvider;
    }

    public void registerPrincipalProvider(PrincipalProvider principalProvider) {
        this.principalProvider = $.requireNotNull(principalProvider);
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
     * @param className
     *         the className of the instance to be returned
     * @param <T>
     *         the generic type of the class
     * @return the instance of the class
     */
    public <T> T getInstance(String className) {
        return getInstance(this.<T>classForName(className));
    }

    /**
     * Get/Create new instance of a class
     *
     * This method will call the build in {@link DependencyInjector}
     * to load the instance. And this is dependency inject process,
     * not a simple constructor call
     *
     * @param clz
     *         the class
     * @param <T>
     *         the generic type of the class
     * @return the instance of the class
     */
    public <T> T getInstance(Class<T> clz) {
        if (null == dependencyInjector) {
            return $.newInstance(clz);
        }
        return dependencyInjector.get(clz);
    }

    /**
     * Finds the resource with the given name.
     *
     * This call delegate to {@link ClassLoader#getResource(String)}
     * on {@link #classLoader() App classloader}.
     *
     * @param name
     *         the resource name
     * @return `URL` to the resource if found or `null` if not found.
     */
    public URL getResource(String name) {
        return classLoader().getResource(name);
    }

    /**
     * Returns an input stream for reading the specified resource.
     *
     * This will call {@link ClassLoader#getResourceAsStream(String)}
     * on {@link #classLoader()}.
     *
     * @param name
     *         the resource name
     * @return the input stream to the resource or `null` if resource not found
     */
    public InputStream getResourceAsStream(String name) {
        return classLoader().getResourceAsStream(name);
    }

    public <K, V> Map<K, V> createMap() {
        return managedCollectionService.createMap();
    }

    public <K, V> ConcurrentMap<K, V> createConcurrentMap() {
        return managedCollectionService.createConcurrentMap();
    }

    public <E> Set<E> createSet() {
        return managedCollectionService.createSet();
    }

    /**
     * Load/get a class by name using the app's {@link #classLoader()}
     *
     * @param className
     *         the name of the class to be loaded
     * @return the class as described above
     */
    public <T> Class<T> classForName(String className) {
        if (className.contains("/")) {
            className = className.replace('/', '.');
        }
        try {
            return $.classForName(className, classLoader());
        } catch (VerifyError error) {
            if (Act.isDev()) {
                // try output the bad bytecode
                byte[] bytes = classLoader.cachedEnhancedBytecode(className);
                if (null != bytes) {
                    File outputDir = new File(tmpDir(), "bytes");
                    outputDir.mkdirs();
                    File output = new File(outputDir, className + ".java");
                    PrintWriter writer = new PrintWriter(IO.writer(output));
                    ClassReader cr = new ClassReader(bytes);
                    ClassNode cn = new ClassNode();
                    cr.accept(cn, 0);
                    final List<MethodNode> mns = cn.methods;
                    for (MethodNode mn : mns) {
                        InsnList inList = mn.instructions;
                        writer.println();
                        writer.println(mn.name + mn.desc);
                        Printer printer = new Textifier();
                        TraceMethodVisitor mp = new TraceMethodVisitor(printer);
                        for (int i = 0; i < inList.size(); i++) {
                            inList.get(i).accept(mp);
                        }
                        printer.print(writer);
                    }
                    IO.close(writer);
                    logger.error("Bad enhanced class encountered, asm code dumped to \n\t>>" + output.getAbsolutePath());
                } else {
                    logger.error("Bad enhanced class: " + className);
                }
                handleBlockIssue(error);
            } else {
                logger.fatal(error, "Bad enhanced class found: " + className);
                shutdown(-1);
            }
            return null;
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
            final Class serviceType = service.getClass();
            eventBus.bind(SysEventId.DEPENDENCY_INJECTOR_LOADED, new SysEventListenerBase() {
                @Override
                public void on(EventObject event) {
                    final App app = App.this;
                    eventBus.emit(new DependencyInjectionBinder(app, serviceType) {
                        @Override
                        public Object resolve(App app) {
                            return app.service(serviceType);
                        }
                    });
                }
            });
        }
        return this;
    }

    public void emit(SysEventId sysEvent) {
        if (isTraceEnabled()) {
            trace(S.concat("emitting event: ", sysEvent.name()));
        }
        currentState = sysEvent;
        eventEmitted().add(sysEvent);
        EventBus bus = eventBus();
        if (null != bus) {
            bus.emit(sysEvent);
        }
    }

    public Set<String> jarFileBlackList() {
        return jarFileBlackList;
    }

    public Set<String> jarFileBlackList2() {
        return jarFileBlackList2;
    }

    public Set<String> scanList() {
        return scanList;
    }

    public Set<String> scanPrefixList() {
        return scanPrefixList;
    }

    public Set<String> scanSuffixList() {
        return scanSuffixList;
    }

    public Set<Pattern> scanPattern() {
        return scanPatterns;
    }

    private Set<SysEventId> eventEmitted() {
        return eventEmitted;
    }

    public boolean eventEmitted(SysEventId sysEvent) {
        return eventEmitted().contains(sysEvent);
    }

    public SysEventId currentState() {
        return currentState;
    }

    public boolean isRoutedActionMethod(String className, String methodName) {
        if (router.isActionMethod(className, methodName)) {
            return true;
        }
        if (!moreRouters.isEmpty()) {
            for (Router routerX : moreRouters.values()) {
                if (routerX.isActionMethod(className, methodName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasMoreRouters() {
        return !moreRouters.isEmpty();
    }

    public Router getRouterFor(String className, String methodName) {
        if (!moreRouters.isEmpty()) {
            for (Router routerX : moreRouters.values()) {
                if (routerX.isActionMethod(className, methodName)) {
                    return routerX;
                }
            }
        }
        return router;
    }

    private synchronized void startLoading() {
        loading.set(true);
    }

    private synchronized void loadingDone() {
        if (config.selfHealing()) {
            SystemAvailabilityMonitor.start();
        }
        loading.set(false);
        this.notifyAll();
    }

    private void loadConfig() {
        JsonUtilConfig.configure(this);
        File resource = RuntimeDirs.resource(this);
        if (resource.exists()) {
            if (isDebugEnabled()) {
                debug("loading app configuration: %s ...", appBase.getAbsolutePath());
            }
            config = new AppConfLoader().load(resource);
        } else {
            String appJarFile = System.getProperty(Act.PROP_APP_JAR_FILE);
            if (null != appJarFile) {
                File jarFile = new File(appJarFile);
                if (isDebugEnabled()) {
                    debug("loading app configuration from jar file: %s", appJarFile);
                }
                config = new AppConfLoader().load(jarFile);
            } else {
                warn("Cannot determine where to load app configuration");
                config = new AppConfLoader().load();
            }
        }
        config.app(this);
        configureLoggingLevels(config);
        registerSingleton(AppConfig.class, config);
        registerValueObjectCodec();
        if (config.i18nEnabled()) {
            MvcConfig.enableLocalizedErrorMsg();
        }
        String tldReloadCron = (String) config.get(CRON_TLD_RELOAD);
        if (S.isBlank(tldReloadCron)) {
            config.setDefaultTldReloadCron();
        }
    }

    private void initHttpConfig() {
        HttpConfig.secure(config.httpSecure());
        HttpConfig.securePort(config.httpExternalSecurePort());
        HttpConfig.nonSecurePort(config.httpExternalPort());
        HttpConfig.defaultLocale(config.locale());
        HttpConfig.domain(config.host());
        HttpConfig.setXForwardedAllowed("all");
        HttpConfig.setCurrentStateStore(new HttpCurrentStateStore());
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

    private void initMetricMetaInfoRepo() {
        metricMetaInfoRepo = new MetricMetaInfoRepo(this);
    }

    private void initDaemonRegistry() {
        if (null != daemonRegistry) {
            Destroyable.Util.tryDestroyAll(daemonRegistry.values(), ApplicationScoped.class);
        }
        daemonRegistry = new HashMap<>();
        jobManager.on(SysEventId.START, "App:schedule-daemon-keeper", new Runnable() {
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
                    error(e, "Error starting daemon [%s]", daemon.id());
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

    private void initCaptchaPluginManager() {
        captchaManager = new CaptchaManager(this);
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
        UrlPath.classInit(this);
        router = new Router(this);
        moreRouters = new HashMap<>();
        Collection<NamedPort> ports = config().namedPorts();
        for (NamedPort port : ports) {
            moreRouters.put(port, new Router(this, port.name()));
        }
        if (config.cliOverHttp()) {
            NamedPort cliOverHttp = new NamedPort(AppConfig.PORT_CLI_OVER_HTTP, config.cliOverHttpPort());
            moreRouters.put(cliOverHttp, new Router(this, AppConfig.PORT_CLI_OVER_HTTP));
        }
    }

    private void initEventBus() {
        EventBus.classInit(this);
        eventBus = new EventBus(this);
    }

    private void initEntityMetaInfoRepo() {
        entityMetaInfoRepo = new MasterEntityMetaInfoRepo(this);
    }

    public void shutdownEventBus() {
        if (null != eventBus) {
            eventBus.destroy();
        }
    }

    private void initSessionManager() {
        sessionManager = getInstance(SessionManager.class);
        singletonRegistry.register(SessionManager.class, sessionManager);
    }

    private void initCache() {
        if (isDev()) {
            CacheServiceProvider.Impl.setClassLoader(this.classLoader);
            config().cacheServiceProvider().reset();
        }
        cache = cache(config().cacheName());
        CacheService sessionCache = cache(config().cacheNameSession());
        HttpConfig.setSessionCache(sessionCache);
    }

    private void initCrypto() {
        crypto = new AppCrypto(config());
        registerSingleton(AppCrypto.class, crypto);
    }

    private void initJobManager() {
        jobManager = new JobManager(this);
    }

    private void initClassMetaInfoRepo() {
        classMetaInfoRepo = new ClassMetaInfoRepo(this);
    }

    private void shutdownJobManager() {
        if (null != jobManager) {
            jobManager.destroy();
        }
    }

    private void initApiManager() {
        apiManager = new ApiManager(this);
    }

    private void initSampleDataProviderManager() {
        sampleDataProviderManager = new SampleDataProviderManager(this);
    }

    private void initScanList() {
        ClassLoader classLoader = getClass().getClassLoader();
        if (classLoader instanceof BootstrapClassLoader) {
            scanList = new HashSet<>();
            scanPatterns = new HashSet<>();
            scanPrefixList = new HashSet<>();
            scanSuffixList = new HashSet<>();
            for (String scanPackage : ((BootstrapClassLoader) classLoader).scanList()) {
                if (scanPackage.contains("\\.") || scanPackage.contains("*")) {
                    scanPatterns.add(Pattern.compile(scanPackage));
                    String prefix = S.cut(scanPackage).beforeFirst("\\");
                    scanPrefixList.add(prefix);
                    String suffix = S.cut(scanPackage).afterLast("*");
                    scanSuffixList.add(suffix);
                } else {
                    scanList.add(scanPackage);
                }
            }
        }
    }

    private void initJarFileBlackList() {
        ClassLoader classLoader = getClass().getClassLoader();
        if (classLoader instanceof BootstrapClassLoader) {
            jarFileBlackList = ((BootstrapClassLoader) classLoader).jarBlackList();
            jarFileBlackList2 = new HashSet<>();
            for (String s : jarFileBlackList) {
                if (s.contains("-")) {
                    jarFileBlackList2.add(s);
                }
            }
            jarFileBlackList.removeAll(jarFileBlackList2);
        }
    }

    private void initInterceptorManager() {
        interceptorManager = new AppInterceptorManager(this);
    }

    private void initHttpClientService() {
        httpClientService = new HttpClientService(this);
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
        scannerManager.register(new Password.ByteCodeScanner());
        scannerManager.register(new SimpleBean.ByteCodeScanner());
        scannerManager.register(new SimpleEventListenerByteCodeScanner());
        scannerManager.register(new CommanderByteCodeScanner());
        scannerManager.register(new EntityInfoByteCodeScanner());
        scannerManager.register(new RythmTransformerScanner());
        scannerManager.register(new MetricContextScanner());
        scannerManager.register(new ImplicitVariableProvider.TemplateVariableScanner(this));
        scannerManager.register(new ConfigurationByteCodeScanner());
    }

    private void loadDependencyInjector() {
        dependencyInjector = new GenieInjector(this);
    }

    private void loadRoutes() {
        loadBuiltInRoutes();
        debug("loading app routing table: %s ...", appBase.getPath());
        Map<String, List<File>> routes;
        if (Act.isProd()) {
            routes = RuntimeDirs.routes(this);
        } else {
            routes = layout().routeTables(base());
        }
        for (Map.Entry<String, List<File>> entry : routes.entrySet()) {
            String npName = entry.getKey();
            List<File> routesFileList = entry.getValue();
            Router router = S.eq(NamedPort.DEFAULT, npName) ? this.router : this.router(npName);
            for (File route : routesFileList) {
                if (route.exists() && route.canRead() && route.isFile()) {
                    List<String> lines = IO.readLines(route);
                    new RouteTableRouterBuilder(lines).build(router);
                }
            }
        }
    }

    private void loadBuiltInRoutes() {
        ResourceGetter actAsset = new ResourceGetter("asset/~act");
        ResourceGetter webjars = new ResourceGetter("META-INF/resources/webjars");
        ResourceGetter asset = new ResourceGetter("asset");
        SecureTicketCodec secureTicketCodec = config.secureTicketCodec();
        SecureTicketHandler secureTicketHandler = new SecureTicketHandler(secureTicketCodec);
        for (Router router : allRouters()) {
            router.addMapping(GET, "/asset/", asset, RouteSource.BUILD_IN);
            router.addMapping(GET, "/~/asset/", actAsset, RouteSource.BUILD_IN);
            router.addMapping(GET, "/webjars/", webjars, RouteSource.BUILD_IN);
            router.addContext("act.", "/~");
            router.addMapping(GET, "/~/ticket", secureTicketHandler, RouteSource.BUILD_IN);
        }
    }

    private void initClassLoader() {
        classLoader = Act.mode().classLoader(this);
    }

    private void initJsonDtoClassManager() {
        new JsonDtoClassManager(this);
    }

    private void preloadClasses() {
        classLoader.preload();
    }

    private void initResolverManager() {
        resolverManager = new StringValueResolverManager(this);
        $.propertyHandlerFactory = new ActPropertyHandlerFactory(this);
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

    private void configureLoggingLevels(AppConfig config) {
        Map map = config.subSet("log.level");
        map.putAll(config.subSet("act.log.level"));
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
        long ms = 0;
        if (isDebugEnabled()) {
            ms = $.ms();
            debug("scanning process starts ...");
        }
        classLoader().scan();
        if (isDebugEnabled()) {
            debug("Scanning process takes " + ($.ms() - ms) + "ms");
        }
    }

    static App create(File appBase, Version version, ProjectLayout layout) {
        return new App(appBase, version, layout);
    }


    // for unit test
    public static App testInstance() {
        return new App();
    }
}
