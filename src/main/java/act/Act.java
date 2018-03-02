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

import act.app.*;
import act.app.event.SysEventId;
import act.app.util.NamedPort;
import act.boot.BootstrapClassLoader;
import act.boot.PluginClassProvider;
import act.boot.app.FullStackAppBootstrapClassLoader;
import act.boot.app.RunApp;
import act.conf.*;
import act.controller.meta.ActionMethodMetaInfo;
import act.controller.meta.CatchMethodMetaInfo;
import act.controller.meta.InterceptorMethodMetaInfo;
import act.crypto.AppCrypto;
import act.db.DbManager;
import act.event.ActEvent;
import act.event.ActEventListener;
import act.event.EventBus;
import act.handler.RequestHandlerBase;
import act.handler.SimpleRequestHandler;
import act.handler.builtin.controller.*;
import act.handler.builtin.controller.impl.ReflectedHandlerInvoker;
import act.inject.DependencyInjector;
import act.internal.util.AppDescriptor;
import act.job.JobManager;
import act.metric.MetricPlugin;
import act.metric.SimpleMetricPlugin;
import act.plugin.AppServicePluginManager;
import act.plugin.GenericPluginManager;
import act.plugin.Plugin;
import act.plugin.PluginScanner;
import act.route.RouteSource;
import act.sys.Env;
import act.util.AppCodeScannerPluginManager;
import act.util.Banner;
import act.util.ClassInfoRepository;
import act.util.SysProps;
import act.view.ViewManager;
import act.xio.Network;
import act.xio.NetworkHandler;
import act.xio.undertow.UndertowNetwork;
import org.osgl.$;
import org.osgl.cache.CacheService;
import org.osgl.exception.NotAppliedException;
import org.osgl.exception.UnexpectedException;
import org.osgl.http.H;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.*;
import osgl.version.Version;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static act.Destroyable.Util.tryDestroy;

/**
 * The Act runtime and facade
 */
public final class Act {

    /**
     * Used to set/get system property to communicate the app jar file if
     * app is loaded from jar
     */
    public static final String PROP_APP_JAR_FILE = "act_app_jar_file";

    /**
     * The manifest attributes property to fetch app jar file
     */
    private static final String ATTR_APP_JAR = "App-Jar";

    public enum Mode {
        PROD,
        /**
         * DEV mode is special as Act might load classes
         * directly from srccode code when running in this mode
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

    public static final osgl.version.Version VERSION = osgl.version.Version.of(Act.class);
    public static final Logger LOGGER = L.get(Act.class);
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

    public static Mode mode() {
        return mode;
    }

    public static boolean isProd() {
        return mode.isProd();
    }

    public static boolean isDev() {
        return mode.isDev();
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
        processEnvironment(descriptor);
        Banner.print(descriptor);
        loadConfig();
        initMetricPlugin();
        initPluginManager();
        initAppServicePluginManager();
        initDbManager();
        initEnhancerManager();
        initViewManager();
        initAppCodeScannerPluginManager();
        loadPlugins();
        enhancerManager().registered(); // so it can order the enhancers
        initNetworkLayer();
        initApplicationManager();
        LOGGER.info("loading application(s) ...");
        appManager.loadSingleApp(descriptor);
        startNetworkLayer();
        Thread.currentThread().setContextClassLoader(Act.class.getClassLoader());
        App app = app();
        if (null == app) {
            shutdownNetworkLayer();
            throw new UnexpectedException("App not found. Please make sure your app start directory is correct");
        }
        emit(SysEventId.ACT_START);
        writePidFile();
    }

    public static void shutdown(App app) {
        if (null == appManager) {
            return;
        }
        if (!appManager.unload(app)) {
            app.destroy();
        }
        shutdownAct();
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
        List<NamedPort> portList = app.config().namedPorts();
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
    public static AppConfig appConfig() {
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
    public static Class<?> appClassForName(String className) {
        return app().classForName(className);
    }

    /**
     * Return an instance with give class name
     *
     * @param className the class name
     * @param <T>       the generic type of the class
     * @return the instance of the class
     */
    public static <T> T getInstance(String className) {
        return app().getInstance(className);
    }

    /**
     * Return an instance with give class
     *
     * @param clz the class
     * @param <T> the generic type of the class
     * @return the instance of the class
     */
    public static <T> T getInstance(Class<? extends T> clz) {
        return app().getInstance(clz);
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

    static int classCacheSize() {
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
        s = System.getProperty("app.nodeGroup");
        if (null != s) {
            nodeGroup = s;
            if (traceEnabled) {
                LOGGER.trace("set node group to %s", s);
            }
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
        new PluginScanner().scan();
        LOGGER.debug("plugin scanning finished in %sms", $.ms() - ts);
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

    private static void initNetworkLayer() {
        LOGGER.debug("initializing network layer ...");
        network = new UndertowNetwork();
    }

    private static void destroyNetworkLayer() {
        if (null != network) {
            network.destroy();
            network = null;
        }
    }

    private static void startNetworkLayer() {
        if (network.isDestroyed()) {
            return;
        }
        LOGGER.debug("starting network layer ...");
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
        long ts = $.ms();
        String profile = SysProps.get(AppConfigKey.PROFILE.key());
        if (S.blank(profile)) {
            profile = "";
        } else {
            profile = "using profile[" + profile + "]";
        }
        String packageName = appDescriptor.getPackageName();
        LOGGER.debug("run fullstack application with package[%s] %s", packageName, profile);
        final String SCAN_PACKAGE = AppConfigKey.SCAN_PACKAGE.key();
        if (S.notBlank(packageName)) {
            System.setProperty(SCAN_PACKAGE, packageName);
        }
        FullStackAppBootstrapClassLoader classLoader = new FullStackAppBootstrapClassLoader(RunApp.class.getClassLoader());
        Thread.currentThread().setContextClassLoader(classLoader);
        Class<?> actClass = classLoader.loadClass("act.Act");
        Method m = actClass.getDeclaredMethod("startup", byte[].class);
        m.setAccessible(true);
        $.invokeStatic(m, appDescriptor.toByteArray());
        int port = $.invokeStatic(actClass, "httpPort");
        LOGGER.info("app is ready at: http://%s:%s", getLocalIpAddr(), port);
        LOGGER.info("it takes %sms to start the app\n", $.ms() - ts);
    }

    public static int httpPort() {
        return app().config().httpPort();
    }

    private static void shutdownAct() {
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
            IO.writeContent(pid, new File(pidFile));
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    clearPidFile();
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
            if (!file.delete()) {
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

}
