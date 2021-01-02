package act;

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

import static act.Destroyable.Util.tryDestroy;

import act.app.*;
import act.app.event.SysEventId;
import act.app.util.NamedPort;
import act.boot.BootstrapClassLoader;
import act.boot.PluginClassProvider;
import act.boot.app.FullStackAppBootstrapClassLoader;
import act.boot.app.RunApp;
import act.conf.*;
import act.controller.meta.*;
import act.crypto.AppCrypto;
import act.db.DbManager;
import act.event.*;
import act.exception.AppStartTerminateException;
import act.handler.RequestHandlerBase;
import act.handler.SimpleRequestHandler;
import act.handler.builtin.controller.*;
import act.handler.builtin.controller.impl.ReflectedHandlerInvoker;
import act.inject.DependencyInjector;
import act.internal.util.AppDescriptor;
import act.job.JobManager;
import act.metric.MetricPlugin;
import act.metric.SimpleMetricPlugin;
import act.plugin.*;
import act.route.RouteSource;
import act.sys.Env;
import act.util.*;
import act.view.ViewManager;
import act.xio.Network;
import act.xio.NetworkHandler;
import act.xio.undertow.UndertowNetwork;
import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.ISODateTimeFormat;
import org.osgl.$;
import org.osgl.cache.CacheService;
import org.osgl.exception.NotAppliedException;
import org.osgl.exception.UnexpectedException;
import org.osgl.http.H;
import org.osgl.inject.Genie;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.*;
import org.osgl.util.converter.TypeConverterRegistry;
import osgl.version.Version;
import osgl.version.Versioned;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.net.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * The Act runtime and facade
 */
@Versioned
public final class Act {

    public static final Version VERSION = Version.of(Act.class);
    public static final Logger LOGGER = LogManager.get(Act.class);

    /**
     * Used to set/get system property to communicate the app jar file if
     * app is loaded from jar
     */
    public static final String PROP_APP_JAR_FILE = "act_app_jar_file";

    /**
     * The manifest attributes property to fetch app jar file
     */
    private static final String ATTR_APP_JAR = "App-Jar";

    /**
     * The runtime mode
     */
    public enum Mode {

        /**
         * PROD mode should be used when app is running in product
         * deployment or SIT/UAT testing deployment etc
         */
        PROD,

        /**
         * DEV mode should be used by developer when developing the application.
         *
         * When Act is started in DEV mode the framework will monitor the
         * source/configuration file and do hot reload when source/configuration
         * file changes.
         */
        DEV() {
            @Override
            public AppScanner appScanner() {
                return AppScanner.SINGLE_APP_SCANNER;
            }

            @Override
            public AppClassLoader classLoader(App app) {
                return new DevModeClassLoader(app);
            }
        };

        private final String confPrefix = "%" + name().toLowerCase() + ".";

        /**
         * Returns if the current mode is {@link #DEV dev mode}
         *
         * @return `true` if the current mode is dev
         */
        public boolean isDev() {
            return DEV == this;
        }

        /**
         * Returns if the current mode is {@link #PROD prod mode}
         *
         * @return `true` if the current mode is product mode
         */
        public boolean isProd() {
            return PROD == this;
        }

        public String configKey(String key) {
            return confPrefix + key;
        }

        public AppScanner appScanner() {
            return AppScanner.DEF_SCANNER;
        }

        public AppClassLoader classLoader(App app) {
            return new AppClassLoader(app);
        }

        public ControllerAction createRequestHandler(ActionMethodMetaInfo action, App app) {
            return ReflectedHandlerInvoker.createControllerAction(action, app);
        }

        public BeforeInterceptor createBeforeInterceptor(InterceptorMethodMetaInfo before, App app) {
            return ReflectedHandlerInvoker.createBeforeInterceptor(before, app);
        }

        public AfterInterceptor createAfterInterceptor(InterceptorMethodMetaInfo after, App app) {
            return ReflectedHandlerInvoker.createAfterInterceptor(after, app);
        }

        public ExceptionInterceptor createExceptionInterceptor(CatchMethodMetaInfo ex, App app) {
            return ReflectedHandlerInvoker.createExceptionInterceptor(ex, app);
        }

        public FinallyInterceptor createFinallyInterceptor(InterceptorMethodMetaInfo fin, App app) {
            return ReflectedHandlerInvoker.createFinannyInterceptor(fin, app);
        }

        public static Mode valueOfIgnoreCase(String mode) {
            return valueOf(mode.trim().toUpperCase());
        }
    }

    private static Genie genie;
    private static ActConfig conf;
    private static Mode mode = Mode.PROD;
    private static String nodeGroup = "";
    private static AppManager appManager;
    private static ViewManager viewManager;
    private static Network network;
    private static MetricPlugin metricPlugin;
    private static BytecodeEnhancerManager enhancerManager;
    private static AppCodeScannerPluginManager scannerPluginManager;
    private static DbManager dbManager;
    private static GenericPluginManager pluginManager;
    private static AppServicePluginManager appPluginManager;
    private static Map<String, Plugin> genericPluginRegistry = new HashMap<>();
    private static Map<Class<? extends ActEvent>, List<ActEventListener>> listeners = new HashMap<>();

    /**
     * Returns the runtime mode of the process.
     *
     * @return Act process runtime mode
     * @see Mode
     */
    public static Mode mode() {
        return mode;
    }

    /**
     * Check if the Act process is running in {@link Mode#PROD} mode
     * @return `true` if Act process is running in prod mode
     */
    public static boolean isProd() {
        return mode.isProd();
    }

    /**
     * Check if the Act process is running in {@link Mode#DEV} mode
     * @return `true` if Act process is running in dev mode
     */
    public static boolean isDev() {
        return mode.isDev();
    }

    /**
     * Check if the Act process is running using `test` profile
     * @return `true` if Act process is running using `test` profile
     */
    public static boolean isTest() {
        return "test".equalsIgnoreCase(profile());
    }

    /**
     * Return the current profile name
     */
    public static String profile() {
        return ConfLoader.confSetName();
    }

    public static String nodeGroup() {
        return nodeGroup;
    }

    public static ActConfig conf() {
        return conf;
    }

    public static ClassInfoRepository classInfoRepository() {
        ClassLoader cl = Act.class.getClassLoader();
        if (cl instanceof BootstrapClassLoader) {
            return ((BootstrapClassLoader) cl).classInfoRepository();
        } else {
            LOGGER.warn("Class loader [%s] of Act is not a ActClassLoader", cl);
            return null;
        }
    }

    public static List<Class<?>> pluginClasses() {
        ClassLoader cl = Act.class.getClassLoader();
        if (cl instanceof PluginClassProvider) {
            return ((PluginClassProvider) cl).pluginClasses();
        } else {
            LOGGER.warn("Class loader [%s] of Act is not a PluginClassProvider", cl);
            return C.list();
        }
    }

    public static AppServicePluginManager appServicePluginManager() {
        return appPluginManager;
    }

    public static DbManager dbManager() {
        return dbManager;
    }

    public static BytecodeEnhancerManager enhancerManager() {
        return enhancerManager;
    }

    public static GenericPluginManager pluginManager() {
        return pluginManager;
    }

    public static MetricPlugin metricPlugin() {
        return metricPlugin;
    }

    public static AppCodeScannerPluginManager scannerPluginManager() {
        return scannerPluginManager;
    }

    public static AppManager applicationManager() {
        return appManager;
    }

    public static ViewManager viewManager() {
        return viewManager;
    }

    public static Network network() {
        return network;
    }

    public static void registerPlugin(Plugin plugin) {
        genericPluginRegistry.put(plugin.getClass().getCanonicalName().intern(), plugin);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Plugin> T registeredPlugin(Class<T> type) {
        return (T) genericPluginRegistry.get(type.getCanonicalName().intern());
    }

    public static void startup(AppDescriptor descriptor) {
        long start = descriptor.getStart();
        processEnvironment(descriptor);
        registerMimeTypes();
        registerTypeConverters();
        loadConfig();
        Banner.print(descriptor);
        initMetricPlugin();
        initPluginManager();
        initAppServicePluginManager();
        initDbManager();
        initEnhancerManager();
        initViewManager();
        initAppCodeScannerPluginManager();
        loadPlugins();
        enhancerManager().registered(); // so it can order the enhancers
        NetworkBootupThread nbt = initNetworkLayer();
        initApplicationManager();
        LOGGER.info("loading application(s) ...");
        try {
            appManager.loadSingleApp(descriptor);
        } catch (AppStartTerminateException e) {
            System.exit(-1);
        }
        startNetworkLayer(nbt);
        Thread.currentThread().setContextClassLoader(Act.class.getClassLoader());
        App app = app();
        if (null == app) {
            shutdownNetworkLayer();
            throw new UnexpectedException("App not found. Please make sure your app start directory is correct");
        }
        int port = httpPort();
        String urlContext = appConfig().urlContext();
        CliServer cliServer = app.cliServer();
        if (null != cliServer) {
            cliServer.logStart();
        }
        if (null == urlContext) {
            LOGGER.info("app is ready in %sms at: http://%s:%s\n\n", $.ms() - start, getLocalIpAddr(), port);
        } else {
            LOGGER.info("app is ready in %sms at: http://%s:%s%s\n\n", $.ms() - start, getLocalIpAddr(), port, urlContext);
        }
        writePidFile();
        app.jobManager().post(SysEventId.POST_START, new Runnable() {
            @Override
            public void run() {
                emit(SysEventId.ACT_START);
            }
        }, true);
    }

    public static void shutdown() {
        shutdown(app(), 0);
    }

    public static void shutdownNow() {
        shutdown(app(), 0, false);
    }

    public static void shutdown(final App app) {
        shutdown(app, 0);
    }

    public static void shutdown(final App app, final int exitCode) {
        shutdown(app, exitCode, true);
    }

    private static void shutdown(final App app, final int exitCode, boolean async) {
        if (null == appManager) {
            return;
        }
        if (async) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        if (!appManager.unload(app)) {
                            app.destroy();
                        }
                    } finally {
                        shutdownAct(exitCode);
                    }
                }
            }.start();
        } else {
            try {
                if (!appManager.unload(app)) {
                    app.destroy();
                }
            } finally {
                shutdownAct(exitCode);
            }
        }
    }

    public static RequestServerRestart requestRestart() {
        E.illegalStateIf(!isDev());
        throw new RequestServerRestart();
    }

    public static RequestRefreshClassLoader requestRefreshClassLoader() {
        E.illegalStateIf(!isDev());
        throw RequestRefreshClassLoader.INSTANCE;
    }

    public static void hook(App app) {
        int port = app.config().httpPort();
        NetworkHandler networkHandler = new NetworkHandler(app);
        network.register(port, false, networkHandler);
        if (app.config().supportSsl()) {
            network.register(appConfig().httpsPort(), true, networkHandler);
        }
        Collection<NamedPort> portList = app.config().namedPorts();
        for (NamedPort np : portList) {
            network.register(np.port(), false, new NetworkHandler(app, np));
        }
    }

    public static synchronized void trigger(ActEvent<?> event) {
        List<ActEventListener> list = listeners.get(event.getClass());
        if (null != list) {
            for (ActEventListener l : list) {
                try {
                    l.on(event);
                } catch (Exception e) {
                    LOGGER.error(e, "error calling act event listener %s on event %s", l.id(), event);
                }
            }
        }
    }

    public static synchronized <T extends ActEvent> void registerEventListener(Class<T> eventClass, ActEventListener<T> listener) {
        List<ActEventListener> list = listeners.get(eventClass);
        if (null == list) {
            list = new ArrayList<>();
            listeners.put(eventClass, list);
        }
        if (!list.contains(listener)) {
            list.add(listener);
        }
    }

    /**
     * Generate custer unique ID via {@link App#cuid()}
     *
     * @return a cluster unique ID generated
     */
    public static String cuid() {
        return App.instance().cuid();
    }

    /**
     * Returns the current {@link App application's} crypto service
     *
     * @return an {@link AppCrypto} instance
     */
    public static AppCrypto crypto() {
        return app().crypto();
    }

    /**
     * Return the {@link App} instance
     *
     * @return the App instance
     */
    public static App app() {
        return App.instance();
    }

    /**
     * Returns the app version
     *
     * @return
     *      the app version
     */
    public static Version appVersion() {
        return app().version();
    }

    /**
     * Return the {@link App}'s config
     *
     * @return the app config
     */
    public static AppConfig<?> appConfig() {
        return App.instance().config();
    }

    /**
     * Utility method to retrieve singleton instance via {@link App#singleton(Class)} method.
     *
     * This method is deprecated. Please use {@link #getInstance(Class)} instead
     *
     * @param singletonClass
     * @param <T>
     * @return the singleton instance
     */
    @Deprecated
    public static <T> T singleton(Class<T> singletonClass) {
        return App.instance().singleton(singletonClass);
    }

    /**
     * Returns the application's {@link App#cache() cache service}
     *
     * @return the cache service
     */
    public static CacheService cache() {
        return App.instance().cache();
    }

    /**
     * Trigger an {@link SysEventId App event}
     *
     * @param sysEvent the app event
     */
    public static void emit(SysEventId sysEvent) {
        App.instance().emit(sysEvent);
    }

    /**
     * Alias of {@link #emit(SysEventId)}
     *
     * @param sysEventId the app event
     */
    public static void trigger(SysEventId sysEventId) {
        emit(sysEventId);
    }

    /**
     * Returns the {@link App app}'s {@link EventBus eventBus}
     *
     * @return the eventBus
     */
    public static EventBus eventBus() {
        return App.instance().eventBus();
    }

    /**
     * Returns the {@link App app}'s {@link JobManager}
     *
     * @return the app's jobManager
     */
    public static JobManager jobManager() {
        return App.instance().jobManager();
    }

    /**
     * Returns the {@link App app}'s {@link DependencyInjector}
     *
     * @param <DI> the generic type of injector
     * @return the app's injector
     */
    public static <DI extends DependencyInjector> DI injector() {
        return App.instance().injector();
    }

    /**
     * Call {@link App#classForName(String)} method on the current {@link #app() app instance}
     *
     * @param className the class name
     * @return the class corresponding to the name specified
     */
    public static <T> Class<T> appClassForName(String className) {
        return app().classForName(className);
    }

    public static <T> Class<T> classForName(String className) {
        App app = app();
        if (null != app) {
            return app.classForName(className);
        }
        return $.classForName(className, Act.class.getClassLoader());
    }

    /**
     * Return an instance with give class name
     *
     * @param className the class name
     * @param <T>       the generic type of the class
     * @return the instance of the class
     */
    public static <T> T getInstance(String className) {
        App app = app();
        if (null != app) {
            return app.getInstance(className);
        } else {
            return getInstance(Act.<T>classForName(className));
        }
    }

    /**
     * Return an instance with give class
     *
     * @param clz the class
     * @param <T> the generic type of the class
     * @return the instance of the class
     */
    public static <T> T getInstance(Class<? extends T> clz) {
        App app = app();
        return null == app ? genie().get(clz) : app.getInstance(clz);
    }

    /**
     * Find a resource by given name.
     *
     * This call delegate to {@link App#getResource(String)} on
     * {@link #app()}.
     *
     * @param name
     *      the resource name
     * @return the `URL` if found, or `null` if resource not found
     */
    public static URL getResource(String name) {
        App app = app();
        if (null == app) {
            if (name.startsWith("/")) {
                name = name.substring(1);
            }
            return Act.class.getClassLoader().getResource(name);
        }
        return app.getResource(name);
    }

    /**
     * Returns an input stream for reading the specified resource.
     *
     * This will call {@link App#getResourceAsStream(String)} on
     * {@link #app()}.
     *
     * @param name
     *      The resource name
     * @return the input stream to the resource or `null` if resource not found
     */
    public static InputStream getResourceAsStream(String name) {
        return app().getResourceAsStream(name);
    }

    // --- Spark style API for application to hook action handler to a certain http request endpoint

    public static void get(String url, SimpleRequestHandler handler) {
        get(url, RequestHandlerBase.wrap(handler));
    }

    public static void getNonblock(String url, SimpleRequestHandler handler) {
        get(url, RequestHandlerBase.wrap(handler).setExpress());
    }

    public static void post(String url, SimpleRequestHandler handler) {
        post(url, RequestHandlerBase.wrap(handler));
    }

    public static void put(String url, SimpleRequestHandler handler) {
        put(url, RequestHandlerBase.wrap(handler));
    }

    public static void delete(String url, SimpleRequestHandler handler) {
        delete(url, RequestHandlerBase.wrap(handler));
    }

    public static void get(String url, RequestHandlerBase handler) {
        app().router().addMapping(H.Method.GET, url, handler, RouteSource.APP_CONFIG);
    }

    public static void post(String url, RequestHandlerBase handler) {
        app().router().addMapping(H.Method.POST, url, handler, RouteSource.APP_CONFIG);
    }

    public static void put(String url, RequestHandlerBase handler) {
        app().router().addMapping(H.Method.PUT, url, handler, RouteSource.APP_CONFIG);
    }

    public static void delete(String url, RequestHandlerBase handler) {
        app().router().addMapping(H.Method.DELETE, url, handler, RouteSource.APP_CONFIG);
    }

    // --- ActFramework entry methods

    /**
     * Start Act application
     *
     * @throws Exception
     *      any exception raised during app start
     */
    public static void start() throws Exception {
        bootstrap(AppDescriptor.of(getCallerClass()));
    }

    /**
     * Start Act application with specified app name
     *
     * @param appName
     *      the app name, optional
     * @throws Exception
     *      any exception thrown out
     */
    public static void start(final String appName) throws Exception {
        bootstrap(AppDescriptor.of(appName, getCallerClass()));
    }

    /**
     * Start Act application with specified app name and scan package.
     *
     * If there are multiple packages, they should be joined in a single string
     * by comma `,`. And the first package name will be used to explore the
     * `.version` file in the class path
     *
     * @param appName
     *      the app name, optional
     * @param scanPackage
     *      the scan package
     * @throws Exception
     *      any exception raised during act start up
     */
    public static void start(String appName, String scanPackage) throws Exception {
        bootstrap(AppDescriptor.of(appName, scanPackage));
    }

    /**
     * Start Act application with specified name and scan package specified by a class
     *
     * @param appName
     *      the app name
     * @param anyAppClass
     *      specifies the scan package
     * @throws Exception
     *      any exception raised during act start up
     */
    public static void start(String appName, Class<?> anyAppClass) throws Exception {
        bootstrap(AppDescriptor.of(appName, anyAppClass));
    }

    /**
     * Start Act application with scan package specified by a class
     *
     * @param anyAppClass
     *      specifies the scan package
     * @throws Exception
     *      any exception raised during act start up
     */
    public static void start(Class<?> anyAppClass) throws Exception {
        bootstrap(AppDescriptor.of(anyAppClass));
    }

    /**
     * Start Act application with specified app name, app version and
     * scan page via an app class
     *
     * @param appName
     *      the app name
     * @param anyAppClass
     *      specifies the scan package
     * @param appVersion
     *      the app version tag
     * @throws Exception
     *      any exception raised during act start up
     */
    public static void start(String appName, Class<?> anyAppClass, Version appVersion) throws Exception {
        bootstrap(AppDescriptor.of(appName, anyAppClass, appVersion));
    }

    /**
     * Start Act application with specified app name, app version and scan package
     *
     * @param appName
     *      the app name
     * @param scanPackage
     *      the scan package, the package could be separated by {@link Constants#LIST_SEPARATOR}
     * @param appVersion
     *      the app version tag
     * @throws Exception any exception raised during act start up
     */
    public static void start(String appName, String scanPackage, Version appVersion) throws Exception {
        bootstrap(AppDescriptor.of(appName, scanPackage, appVersion));
    }

    public static int classCacheSize() {
        return ((FullStackAppBootstrapClassLoader) Act.class.getClassLoader()).libBCSize();
    }

    private static String appJar() {
        URL url = Act.class.getClassLoader().getResource("META-INF/MANIFEST.MF");
        if (null != url) {
            try {
                Manifest manifest = new Manifest(url.openStream());
                Attributes attributes = manifest.getMainAttributes();
                if (null != attributes) {
                    return attributes.getValue(ATTR_APP_JAR);
                }
            } catch (IOException e) {
                LOGGER.warn(e, "cannot open manifest resource: %s", url);
            }
        }
        return null;
    }

    private static void processEnvironment(AppDescriptor descriptor) {
        boolean traceEnabled = LOGGER.isTraceEnabled();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        List<String> cp = S.fastSplit(runtimeMXBean.getClassPath(), File.pathSeparator);
        boolean hasClassesDir = false;
        String artifactId = descriptor.getVersion().getArtifactId();
        String appJar = appJar();
        String appJarFile = null;
        for (String cpItem : cp) {
            if (!cpItem.endsWith(".jar")) {
                if (traceEnabled) {
                    LOGGER.trace("found class path that are not jar file: %s", cpItem);
                }
                hasClassesDir = true;
                break;
            } else if (null == appJarFile) {
                if (null != appJar && cpItem.contains(appJar)) {
                    appJarFile = cpItem;
                } else if (cpItem.contains(artifactId)) {
                    appJarFile = cpItem;
                }
            }
        }
        String s = System.getProperty("app.mode");
        if (!hasClassesDir) {
            mode = Mode.PROD;
            if (null != appJarFile) {
                System.setProperty(PROP_APP_JAR_FILE, appJarFile);
            }
        } else {
            if (null != s) {
                mode = Mode.valueOfIgnoreCase(s);
                if (traceEnabled) {
                    LOGGER.trace("set app mode to user specified: %s", s);
                }
            } else {
                String profile = SysProps.get(AppConfigKey.PROFILE.key());
                if (S.eq("prod", profile, S.IGNORECASE)) {
                    mode = Mode.PROD;
                    if (traceEnabled) {
                        LOGGER.trace("set app mode to prod based on profile setting");
                    }
                } else if (S.eq("dev", profile, S.IGNORECASE)) {
                    mode = Mode.DEV;
                    if (traceEnabled) {
                        LOGGER.trace("set app mode to dev based on profile setting");
                    }
                } else {
                    mode = hasClassesDir ? Mode.DEV : Mode.PROD;
                    if (traceEnabled) {
                        LOGGER.trace("set app mode to system determined: %s", mode);
                    }
                }
            }
        }
        if (mode.isDev()) {
            System.setProperty("osgl.version.suppress-var-found-warning", "true");
        } else {
            System.clearProperty("osgl.version.suppress-var-found-warning");
        }
        s = System.getProperty("app.nodeGroup");
        if (null != s) {
            nodeGroup = s;
            if (traceEnabled) {
                LOGGER.trace("set node group to %s", s);
            }
        }
    }

    public static void registerMimeTypes() {
        registerMimeType(MimeTypeExtensions.HTML_TABLE, "text/x-html-table");
        registerMimeType(MimeTypeExtensions.STRING_LIST, "text/x-string-list");
    }

    private static void registerMimeType(Keyword keyword, String contentType) {
        registerMimeType(keyword.javaVariable().toLowerCase(), contentType);
        registerMimeType(keyword.javaVariable(), contentType);
        registerMimeType(keyword.dashed(), contentType);
        registerMimeType(keyword.underscore(), contentType);
    }

    private static void registerMimeType(String name, String contentType) {
        MimeType.registerMimeType(name, contentType);
    }

    public static void registerTypeConverters() {
        TypeConverterRegistry.INSTANCE.register(new $.TypeConverter<ReadableInstant, Long>() {
            @Override
            public Long convert(ReadableInstant o) {
                return o.getMillis();
            }
        }).register(new $.TypeConverter<String, LocalTime>() {
            @Override
            public LocalTime convert(String s) {
                if (S.isIntOrLong(s)) {
                    Long l = Long.valueOf(s);
                    return $.convert(l).to(LocalTime.class);
                }
                AppConfig config = Act.appConfig();
                String pattern = config.localizedTimePattern(locale());
                return (DateTimeFormat.forPattern(pattern)).parseLocalTime(s);
            }
            @Override
            public LocalTime convert(String s, Object hint) {
                if (null == hint) {
                    return convert(s);
                }
                if (S.isIntOrLong(s)) {
                    Long l = Long.valueOf(s);
                    return $.convert(l).to(LocalTime.class);
                }
                String pattern = S.string(hint);
                if (pattern.toLowerCase().contains("iso")) {
                    return ISODateTimeFormat.dateTimeParser().parseLocalTime(s);
                }
                return (DateTimeFormat.forPattern(pattern)).parseLocalTime(s);
            }
        }).register(new $.TypeConverter<String, LocalDate>() {
            @Override
            public LocalDate convert(String s) {
                if (S.isIntOrLong(s)) {
                    Long l = Long.valueOf(s);
                    return $.convert(l).to(LocalDate.class);
                }
                AppConfig config = Act.appConfig();
                String pattern = config.localizedDatePattern(locale());
                return (DateTimeFormat.forPattern(pattern)).parseLocalDate(s);
            }
            @Override
            public LocalDate convert(String s, Object hint) {
                if (null == hint) {
                    return convert(s);
                }
                if (S.isIntOrLong(s)) {
                    Long l = Long.valueOf(s);
                    return $.convert(l).to(LocalDate.class);
                }
                String pattern = S.string(hint);
                if (pattern.toLowerCase().contains("iso")) {
                    return ISODateTimeFormat.dateTimeParser().parseLocalDate(s);
                }
                return (DateTimeFormat.forPattern(pattern)).parseLocalDate(s);
            }
        }).register(new $.TypeConverter<String, DateTime>() {
            @Override
            public DateTime convert(String s) {
                if (S.isIntOrLong(s)) {
                    Long l = Long.valueOf(s);
                    return $.convert(l).to(DateTime.class);
                }
                AppConfig config = Act.appConfig();
                String pattern = config.localizedDateTimePattern(locale());
                return (DateTimeFormat.forPattern(pattern)).parseDateTime(s);
            }
            @Override
            public DateTime convert(String s, Object hint) {
                if (null == hint) {
                    return convert(s);
                }
                if (S.isIntOrLong(s)) {
                    Long l = Long.valueOf(s);
                    return $.convert(l).to(DateTime.class);
                }
                String pattern = S.string(hint);
                if (pattern.toLowerCase().contains("iso")) {
                    return ISODateTimeFormat.dateTimeParser().parseDateTime(s);
                }
                return (DateTimeFormat.forPattern(pattern)).parseDateTime(s);
            }
        }).register(new $.TypeConverter<Long, DateTime>() {
            @Override
            public DateTime convert(Long o) {
                return new DateTime().withMillis(o);
            }
        }).register(new $.TypeConverter<DateTime, LocalDateTime>() {
            @Override
            public LocalDateTime convert(DateTime o) {
                return o.toLocalDateTime();
            }
        }).register(new $.TypeConverter<DateTime, LocalDate>() {
            @Override
            public LocalDate convert(DateTime o) {
                return o.toLocalDate();
            }
        }).register(new $.TypeConverter<DateTime, LocalTime>() {
            @Override
            public LocalTime convert(DateTime o) {
                return o.toLocalTime();
            }
        })
        ;
    }

    private static Locale locale() {
        ActContext ctx = ActContext.Base.currentContext();
        if (null != ctx) {
            return ctx.locale(true);
        } else {
            App app = app();
            return null == app ? Locale.getDefault() : app.config().locale();
        }
    }

    private static void loadConfig() {
        LOGGER.debug("loading configuration ...");

        String s = SysProps.get("act.mode");
        if (null != s) {
            mode = Mode.valueOfIgnoreCase(s);
        }
        LOGGER.debug("Act starts in %s mode", mode);

        conf = new ActConfLoader().load(null);
    }

    private static void unloadConfig() {
        conf = null;
    }

    private static void loadPlugins() {
        LOGGER.debug("scanning plugins ...");
        long ts = $.ms();
        int count = new PluginScanner().scan();
        LOGGER.debug("%s plugin scanning finished in %sms", count, $.ms() - ts);
    }

    private static void unloadPlugins() {
        new PluginScanner().unload();
    }

    private static void initViewManager() {
        LOGGER.debug("initializing view manager ...");
        viewManager = new ViewManager();
    }

    private static void destroyViewManager() {
        if (null != viewManager) {
            viewManager.destroy();
            viewManager = null;
        }
    }

    private static void initMetricPlugin() {
        LOGGER.debug("initializing metric plugin ...");
        metricPlugin = new SimpleMetricPlugin();
    }

    private static void destroyMetricPlugin() {
        if (null != metricPlugin) {
            tryDestroy(metricPlugin);
            metricPlugin = null;
        }
    }

    private static void initPluginManager() {
        LOGGER.debug("initializing generic plugin manager ...");
        pluginManager = new GenericPluginManager();
    }

    private static void destroyPluginManager() {
        if (null != pluginManager) {
            pluginManager.destroy();
            pluginManager = null;
        }
    }

    private static void initAppServicePluginManager() {
        LOGGER.debug("initializing app service plugin manager ...");
        appPluginManager = new AppServicePluginManager();
    }

    private static void destroyAppServicePluginManager() {
        if (null != appPluginManager) {
            appPluginManager.destroy();
            appPluginManager = null;
        }
    }

    private static void initDbManager() {
        LOGGER.debug("initializing db manager ...");
        dbManager = new DbManager();
    }

    private static void destroyDbManager() {
        if (null != dbManager) {
            dbManager.destroy();
            dbManager = null;
        }
    }

    private static void initAppCodeScannerPluginManager() {
        LOGGER.debug("initializing app code scanner plugin manager ...");
        scannerPluginManager = new AppCodeScannerPluginManager();
    }

    private static void destroyAppCodeScannerPluginManager() {
        if (null != scannerPluginManager) {
            scannerPluginManager.destroy();
            scannerPluginManager = null;
        }
    }

    static void initEnhancerManager() {
        LOGGER.debug("initializing byte code enhancer manager ...");
        enhancerManager = new BytecodeEnhancerManager();
    }

    private static void destroyEnhancerManager() {
        if (null != enhancerManager) {
            enhancerManager.destroy();
            enhancerManager = null;
        }
    }

    private static class NetworkBootupThread extends Thread {

        Network network;
        RuntimeException exception;

        public NetworkBootupThread(Network network) {
            this.network = network;
        }

        @Override
        public void run() {
            try {
                network.bootUp();
            } catch (RuntimeException e) {
                exception = e;
            }
        }
    }

    private static NetworkBootupThread initNetworkLayer() {
        LOGGER.debug("initializing network layer ...");
        network = new UndertowNetwork();
        NetworkBootupThread nbt = new NetworkBootupThread(network);
        nbt.start();
        return nbt;
    }

    private static void destroyNetworkLayer() {
        if (null != network) {
            network.destroy();
            network = null;
        }
    }

    private static void startNetworkLayer(NetworkBootupThread nbt) {
        if (network.isDestroyed()) {
            return;
        }
        LOGGER.debug("starting network layer ...");
        try {
            nbt.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UnexpectedException(e);
        }
        if (null != nbt.exception) {
            throw nbt.exception;
        }
        network.start();
    }

    private static void shutdownNetworkLayer() {
        LOGGER.debug("shutting down network layer ...");
        network.shutdown();
    }

    protected static void initApplicationManager() {
        LOGGER.debug("initializing application manager ...");
        appManager = AppManager.create();
    }

    private static Class<?> getCallerClass() {
        StackTraceElement[] sa = new RuntimeException().getStackTrace();
        E.unexpectedIf(sa.length < 3, "Whoops!");
        StackTraceElement ste = sa[2];
        String className = ste.getClassName();
        if ("act.Act$start".equals(className)) {
            // groovy launched app
            ste = sa[6];
            className = ste.getClassName();
        }
        E.unexpectedIf(!className.contains("."), "The main class must have package name to use Act");
        return $.classForName(className);
    }

    private static void bootstrap(AppDescriptor appDescriptor) throws Exception {
        String profile = SysProps.get(AppConfigKey.PROFILE.key());
        if (S.blank(profile)) {
            profile = "";
        } else {
            profile = "using profile[" + profile + "]";
        }
        String packageName = appDescriptor.getPackageName();
        LOGGER.debug("run fullstack application with package[%s] %s", packageName, profile);
        final String SCAN_PACKAGE = AppConfigKey.SCAN_PACKAGE_SYS.key();
        if (S.notBlank(packageName)) {
            System.setProperty(SCAN_PACKAGE, packageName);
        }
        FullStackAppBootstrapClassLoader classLoader = new FullStackAppBootstrapClassLoader(RunApp.class.getClassLoader());
        Thread.currentThread().setContextClassLoader(classLoader);
        Class<?> actClass = classLoader.loadClass("act.Act");
        Method m = actClass.getDeclaredMethod("startup", byte[].class);
        m.setAccessible(true);
        $.invokeStatic(m, appDescriptor.toByteArray());
        LOGGER.debug("bootstrap application takes: %sms", $.ms() - appDescriptor.getStart());
    }

    public static int httpPort() {
        return app().config().httpPort();
    }

    private static boolean shutdownStarted = false;
    private static void shutdownAct(int exitCode) {
        if (shutdownStarted) {
            return;
        }
        shutdownStarted = true;
        clearPidFile();
        shutdownNetworkLayer();
        destroyApplicationManager();
        unloadPlugins();
        destroyAppCodeScannerPluginManager();
        destroyViewManager();
        destroyEnhancerManager();
        destroyDbManager();
        destroyAppServicePluginManager();
        destroyPluginManager();
        destroyMetricPlugin();
        unloadConfig();
        destroyNetworkLayer();
        LOGGER.info("All components shutdown, bye!");
        if (0 != exitCode) {
            System.exit(exitCode);
        }
    }


    private static void destroyApplicationManager() {
        if (null != appManager) {
            appManager.destroy();
            appManager = null;
        }
    }

    private static void writePidFile() {
        String pidFile = pidFile();
        OS os = OS.get();
        String pid;
        if (os.isLinux()) {
            pid = Env.PID.get();
        } else {
            try {
                // see http://stackoverflow.com/questions/35842/how-can-a-java-program-get-its-own-process-id
                String name = ManagementFactory.getRuntimeMXBean().getName();
                int pos = name.indexOf('@');
                if (pos > 0) {
                    pid = name.substring(0, pos);
                } else {
                    LOGGER.warn("Write pid file not supported on non-linux system");
                    return;
                }
            } catch (Exception e) {
                LOGGER.warn("Write pid file not supported on non-linux system");
                return;
            }
        }
        try {
            IO.write(pid, new File(pidFile));
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    clearPidFile();
                    Act.shutdownNow();
                }
            });
        } catch (Exception e) {
            LOGGER.warn(e, "Error writing pid file: %s", e.getMessage());
        }
    }

    private static void clearPidFile() {
        String pidFile = pidFile();
        try {
            File file = new File(pidFile);
            if (null != file && file.canRead() && !file.delete()) {
                file.deleteOnExit();
            }
        } catch (Exception e) {
            LOGGER.warn(e, "Error delete pid file: %s", pidFile);
        }
    }

    private static String pidFile() {
        String pidFile = System.getProperty("pidfile");
        if (S.blank(pidFile)) {
            pidFile = "act.pid";
        }
        return pidFile;
    }

    // DONOT DELETE. this method is used by reflection
    @SuppressWarnings("unused")
    private static void startup(byte[] appDescriptor) {
        AppDescriptor descriptor = AppDescriptor.deserializeFrom(appDescriptor);
        startup(descriptor);
    }

    private static String getLocalIpAddr() {
        try {
            for (NetworkInterface ni : C.list(NetworkInterface.getNetworkInterfaces())) {
                if (ni.isLoopback() || !ni.isUp() || ni.isVirtual()) {
                    continue;
                }
                for (InetAddress addr : C.list(ni.getInetAddresses())) {
                    if (addr.isMulticastAddress() || addr.isLoopbackAddress() || addr.isLinkLocalAddress() || addr instanceof Inet6Address) {
                        continue;
                    }
                    return (addr.getHostAddress());
                }
            }
        } catch (Exception e) {
            LOGGER.warn(e, "cannot determine local ip address");
            return "localhost";
        }
        LOGGER.warn("cannot determine local ip address");
        return "localhost";
    }


    public enum F {
        ;
        public static final $.F0<Mode> MODE_ACCESSOR = new $.F0<Mode>() {
            @Override
            public Mode apply() throws NotAppliedException, $.Break {
                return mode;
            }
        };
    }

    private static Genie genie() {
        if (null == genie) {
            genie = Genie.create();
        }
        return genie;
    }

    // debug javadoc generating.
    // Note if you get `Illegal group reference` issue then make sure
    // you do not have `$` in the `{@link}` tag
//    public static void main(String[] args) throws Exception {
//        args = new String[]{"@target/site/apidocs/options", "@target/site/apidocs/packages"};
//        // uncomment the following line and
//        // run `mvn javadoc:javadoc -Prelease -Ddebug=true` to generate options and packages file
//        com.sun.tools.javadoc.Main.execute(args);
//    }

    public static void main(String[] args) throws Exception {
        String text = "abc[[UC_FLD_APPLICATIONEFFECTIVEDATE]]xyz\t[[UC_FLD_NDV123985]]dsa[[UC_FLD_NDV123981]]adf";
        String text0 = text;
        int pos = text.indexOf("[[");
        int pos0 = 0;
        StringBuilder buf = new StringBuilder();
        buf.append(text.substring(pos0, pos));
        while (pos >= 0) {
            int bpos = text.indexOf("]]", pos);
            E.illegalStateIf(bpos < 0, "Invalid template content: " + text0);
            String varName = text.substring(pos + 2, bpos);
            String varVal = "<<" + S.cut(varName).after("UC_FLD_") + ">>";
            buf.append(varVal);
            pos0 = bpos + 2;
            pos = text.indexOf("[[", bpos);
            if (pos > 0) {
                buf.append(text.substring(pos0, pos));
            } else {
                buf.append(text.substring(pos0, text.length()));
            }
        }
        System.out.println(text);
        System.out.println(buf.toString());

    }
}
