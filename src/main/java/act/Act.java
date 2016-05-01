package act;

import act.app.*;
import act.app.util.NamedPort;
import act.boot.BootstrapClassLoader;
import act.boot.PluginClassProvider;
import act.conf.ActConfLoader;
import act.conf.ActConfig;
import act.controller.meta.ActionMethodMetaInfo;
import act.controller.meta.CatchMethodMetaInfo;
import act.controller.meta.InterceptorMethodMetaInfo;
import act.db.DbManager;
import act.event.ActEvent;
import act.event.ActEventListener;
import act.handler.builtin.controller.*;
import act.handler.builtin.controller.impl.ReflectedHandlerInvoker;
import act.metric.MetricPlugin;
import act.metric.SimpleMetricPlugin;
import act.plugin.AppServicePluginManager;
import act.plugin.GenericPluginManager;
import act.plugin.Plugin;
import act.plugin.PluginScanner;
import act.util.*;
import act.view.ViewManager;
import act.xio.NetworkClient;
import act.xio.NetworkService;
import act.xio.undertow.UndertowService;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.List;
import java.util.Map;

/**
 * The Act server
 */
public final class Act {

    public enum Mode {
        PROD, DEV() {
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
         * DEV mode is special as Act might load classes
         * directly from srccode code when running in this mode
         */
        public boolean isDev() {
            return DEV == this;
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

    public static final String VERSION = Version.fullVersion();
    public static String APP_NAME;
    public static String APP_VERSION;
    private static Logger logger = L.get(Act.class);
    private static ActConfig conf;
    private static Mode mode = Mode.PROD;
    private static boolean multiTenant = false;
    private static AppManager appManager;
    private static ViewManager viewManager;
    private static NetworkService network;
    private static MetricPlugin metricPlugin;
    private static BytecodeEnhancerManager enhancerManager;
    private static SessionManager sessionManager;
    private static AppCodeScannerPluginManager scannerPluginManager;
    private static DbManager dbManager;
    private static GenericPluginManager pluginManager;
    private static AppServicePluginManager appPluginManager;
    private static IdGenerator idGenerator = new IdGenerator();
    private static Map<String, Plugin> genericPluginRegistry = C.newMap();
    private static Map<Class<? extends ActEvent>, List<ActEventListener>> listeners = C.newMap();

    public static List<Class<?>> pluginClasses() {
        ClassLoader cl = Act.class.getClassLoader();
        if (cl instanceof PluginClassProvider) {
            return ((PluginClassProvider) cl).pluginClasses();
        } else {
            logger.warn("Class loader [%s] of Act is not a PluginClassProvider", cl);
            return C.list();
        }
    }

    public static ClassInfoRepository classInfoRepository() {
        ClassLoader cl = Act.class.getClassLoader();
        if (cl instanceof BootstrapClassLoader) {
            return ((BootstrapClassLoader) cl).classInfoRepository();
        } else {
            logger.warn("Class loader [%s] of Act is not a ActClassLoader", cl);
            return null;
        }
    }

    public static Mode mode() {
        return mode;
    }

    public static boolean isDev() {
        return mode.isDev();
    }

    public static ActConfig conf() {
        return conf;
    }

    public static boolean multiTenant() {
        return multiTenant;
    }

    public static BytecodeEnhancerManager enhancerManager() {
        return enhancerManager;
    }

    public static GenericPluginManager pluginManager() {
        return pluginManager;
    }

    public static DbManager dbManager() {
        return dbManager;
    }

    public static ViewManager viewManager() {
        return viewManager;
    }

    public static SessionManager sessionManager() {
        return sessionManager;
    }

    public static AppCodeScannerPluginManager scannerPluginManager() {
        return scannerPluginManager;
    }

    public static AppServicePluginManager appServicePluginManager() {
        return appPluginManager;
    }

    public static AppManager applicationManager() {
        return appManager;
    }

    public static MetricPlugin metricPlugin() {
        return metricPlugin;
    }

    public static void registerPlugin(Plugin plugin) {
        genericPluginRegistry.put(plugin.getClass().getCanonicalName().intern(), plugin);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Plugin> T registeredPlugin(Class<T> type) {
        return (T)genericPluginRegistry.get(type.getCanonicalName().intern());
    }

    public static void startServer() {
        start(false, null, null);
    }

    public static void startApp(String appName, String appVersion) {
        String s = System.getProperty("app.mode");
        if (null != s) {
            mode = Mode.valueOfIgnoreCase(s);
        } else {
            mode = Mode.DEV;
        }
        start(true, appName, appVersion);
    }

    private static void start(boolean singleAppServer, String appName, String appVersion) {
        APP_NAME = appName;
        APP_VERSION = appVersion;
        Banner.print(appName, appVersion);
        loadConfig();
        initMetricPlugin();
        initPluginManager();
        initAppServicePluginManager();
        initDbManager();
        //initExecuteService();
        initEnhancerManager();
        initViewManager();
        initSessionManager();
        initAppCodeScannerPluginManager();
        loadPlugins();
        initNetworkLayer();
        initApplicationManager();
        try {
            logger.info("loading application(s) ...");
            if (singleAppServer) {
                appManager.loadSingleApp(appName);
            } else {
                appManager.scan();
            }
        } catch (ActAppException e) {
            logger.fatal(e, "Error starting ACT");
            return;
        }
        startNetworkLayer();

        Thread.currentThread().setContextClassLoader(Act.class.getClassLoader());
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
        network.register(port, new NetworkClient(app));
        List<NamedPort> portList = app.config().namedPorts();
        for (NamedPort np : portList) {
            network.register(np.port(), new NetworkClient(app, np));
        }
    }

    public static synchronized void trigger(ActEvent<?> event) {
        List<ActEventListener> list = listeners.get(event.getClass());
        if (null != list) {
            for (ActEventListener l : list) {
                try {
                    l.on(event);
                } catch (Exception e) {
                    logger.error(e, "error calling act event listener %s on event %s", l.id(), event);
                }
            }
        }
    }

    public static synchronized  <T extends ActEvent> void registerEventListener(Class<T> eventClass, ActEventListener<T> listener) {
        List<ActEventListener> list = listeners.get(eventClass);
        if (null == list) {
            list = C.newList();
            listeners.put(eventClass, list);
        }
        if (!list.contains(listener)) {
            list.add(listener);
        }
    }

    /**
     * Generate custer unique ID
     *
     * @return a cluster unique ID generated
     */
    public static String cuid() {
        return idGenerator.genId();
    }

    /**
     * Utility method to retrieve singleton instance via {@link App#singleton(Class)} method
     * @param singletonClass
     * @param <T>
     * @return the singleton instance
     */
    public static <T> T singleton(Class<T> singletonClass) {
        return App.instance().singleton(singletonClass);
    }

    private static void loadConfig() {
        logger.info("loading configuration ...");

        String s = SysProps.get("act.mode");
        if (null != s) {
            mode = Mode.valueOfIgnoreCase(s);
        }
        logger.info("Act starts in %s mode", mode);

        conf = new ActConfLoader().load(null);
    }

    private static void loadPlugins() {
        logger.info("scanning plugins ...");
        new PluginScanner().scan();
    }

    private static void initViewManager() {
        logger.info("initializing view manager ...");
        viewManager = new ViewManager();
    }

    private static void initMetricPlugin() {
        logger.info("initializing metric plugin ...");
        metricPlugin = new SimpleMetricPlugin();
    }

    private static void initPluginManager() {
        logger.info("initializing generic plugin manager ...");
        pluginManager = new GenericPluginManager();
    }

    private static void initAppServicePluginManager() {
        logger.info("initializing app service plugin manager ...");
        appPluginManager = new AppServicePluginManager();
    }

    private static void initSessionManager() {
        logger.info("initializing session manager ...");
        sessionManager = new SessionManager();
    }

    private static void initDbManager() {
        logger.info("initializing db manager ...");
        dbManager = new DbManager();
    }

    private static void initAppCodeScannerPluginManager() {
        logger.info("initializing app code scanner plugin manager ...");
        scannerPluginManager = new AppCodeScannerPluginManager();
    }

    static void initEnhancerManager() {
        logger.info("initializing byte code enhancer manager ...");
        enhancerManager = new BytecodeEnhancerManager();
    }

    private static void initNetworkLayer() {
        logger.info("initializing network layer ...");
        network = new UndertowService();
    }

    protected static void initApplicationManager() {
        logger.info("initializing application manager ...");
        appManager = AppManager.create();
    }

    private static void startNetworkLayer() {
        logger.info("starting network layer ...");
        network.start();
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
