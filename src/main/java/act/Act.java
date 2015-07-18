package act;

import act.app.*;
import act.boot.BootstrapClassLoader;
import act.boot.PluginClassProvider;
import act.conf.ActConfLoader;
import act.conf.ActConfig;
import act.controller.meta.ActionMethodMetaInfo;
import act.controller.meta.CatchMethodMetaInfo;
import act.controller.meta.InterceptorMethodMetaInfo;
import act.db.DbManager;
import act.handler.builtin.controller.*;
import act.handler.builtin.controller.impl.ReflectedHandlerInvoker;
import act.plugin.AppServicePluginManager;
import act.plugin.GenericPluginManager;
import act.plugin.PluginScanner;
import act.util.AppCodeScannerPluginManager;
import act.util.Banner;
import act.util.ClassInfoRepository;
import act.util.SessionManager;
import act.view.ViewManager;
import act.xio.NetworkClient;
import act.xio.NetworkService;
import act.xio.undertow.UndertowService;
import org.osgl._;
import org.osgl.exception.NotAppliedException;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.List;

/**
 * The Act server
 */
public final class Act {

    public enum Mode {
        PROD, UAT, SIT, DEV() {
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

    private static Logger logger = L.get(Act.class);
    private static ActConfig conf;
    private static Mode mode = Mode.PROD;
    private static boolean multiTenant = false;
    private static AppManager appManager;
    private static ViewManager viewManager;
    private static NetworkService network;
    private static BytecodeEnhancerManager enhancerManager;
    private static SessionManager sessionManager;
    private static AppCodeScannerPluginManager scannerPluginManager;
    private static DbManager dbManager;
    private static GenericPluginManager pluginManager;
    private static AppServicePluginManager appPluginManager;

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
            return (ClassInfoRepository)((BootstrapClassLoader) cl).classInfoRepository();
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

    public static DbManager dbManager() {return dbManager;}

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

    public static void startServer() {
        start(false);
    }

    public static void startApp() {
        mode = Mode.DEV;
        start(true);
    }

    private static void start(boolean singleAppServer) {
        Banner.print("0.0.3-SNAPSHOT");
        loadConfig();
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
        if (singleAppServer) {
            appManager.loadSingleApp();
        } else {
            appManager.scan();
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
        throw new RequestRefreshClassLoader();
    }

    public static void hook(App app) {
        int port = app.config().port();
        network.register(port, new NetworkClient(app));
    }

    private static void loadConfig() {
        logger.debug("loading configuration ...");

        String s = System.getProperty("act.mode");
        if (null != s) {
            mode = Mode.valueOfIgnoreCase(s);
        }
        logger.info("Act starts in %s mode", mode);

        conf = new ActConfLoader().load(null);
    }

    private static void loadPlugins() {
        new PluginScanner().scan();
    }

    private static void initViewManager() {
        viewManager = new ViewManager();
    }

    private static void initPluginManager() {
        pluginManager = new GenericPluginManager();
    }

    private static void initAppServicePluginManager() {
        appPluginManager = new AppServicePluginManager();
    }

    private static void initSessionManager() {
        sessionManager = new SessionManager();
    }

    private static void initDbManager() {
        dbManager = new DbManager();
    }

    private static void initAppCodeScannerPluginManager() {
        scannerPluginManager = new AppCodeScannerPluginManager();
    }

    private static void initExecuteService() {
        E.tbd("init execute service");
    }

    static void initEnhancerManager() {
        enhancerManager = new BytecodeEnhancerManager();
    }

    private static void initNetworkLayer() {
        network = new UndertowService();
    }

    protected static void initApplicationManager() {
        appManager = AppManager.create();
    }

    private static void startNetworkLayer() {
        network.start();
    }

    public enum F {
        ;
        public static final _.F0<Mode> MODE_ACCESSOR = new _.F0<Mode>() {
            @Override
            public Mode apply() throws NotAppliedException, _.Break {
                return mode;
            }
        };
    }

}
