package org.osgl.oms;

import io.undertow.UndertowLogger;
import io.undertow.UndertowMessages;
import org.osgl._;
import org.osgl.exception.NotAppliedException;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.oms.app.*;
import org.osgl.oms.boot.PluginClassProvider;
import org.osgl.oms.conf.OmsConfLoader;
import org.osgl.oms.conf.OmsConfig;
import org.osgl.oms.controller.meta.ActionMethodMetaInfo;
import org.osgl.oms.controller.meta.CatchMethodMetaInfo;
import org.osgl.oms.controller.meta.InterceptorMethodMetaInfo;
import org.osgl.oms.handler.builtin.controller.*;
import org.osgl.oms.handler.builtin.controller.impl.ReflectedHandlerInvoker;
import org.osgl.oms.plugin.PluginScanner;
import org.osgl.oms.util.AppCodeScannerPluginManager;
import org.osgl.oms.util.Banner;
import org.osgl.oms.util.SessionManager;
import org.osgl.oms.view.ViewManager;
import org.osgl.oms.xio.NetworkClient;
import org.osgl.oms.xio.NetworkService;
import org.osgl.oms.xio.undertow.UndertowService;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.List;

/**
 * The OSGL MVC Server
 */
public final class OMS {

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
         * DEV mode is special as OMS might load classes
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

    private static Logger logger = L.get(OMS.class);
    private static OmsConfig conf;
    private static Mode mode = Mode.PROD;
    private static boolean multiTenant = false;
    private static AppManager appManager;
    private static ViewManager viewManager;
    private static NetworkService network;
    private static BytecodeEnhancerManager enhancerManager;
    private static SessionManager sessionManager;
    private static AppCodeScannerPluginManager scannerPluginManager;

    public static List<Class<?>> pluginClasses() {
        ClassLoader cl = OMS.class.getClassLoader();
        if (cl instanceof PluginClassProvider) {
            return ((PluginClassProvider) cl).pluginClasses();
        } else {
            logger.warn("Class loader of OMS is not a PluginClassProvider");
            return C.list();
        }
    }

    public static Mode mode() {
        return mode;
    }

    public static boolean isDev() {
        return mode.isDev();
    }

    public static OmsConfig conf() {
        return conf;
    }

    public static boolean multiTenant() {
        return multiTenant;
    }

    public static BytecodeEnhancerManager enhancerManager() {
        return enhancerManager;
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
        long ts = _.ms();
        Banner.print("0.0.1-SNAPSHOT");
        loadConfig();
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

        Thread.currentThread().setContextClassLoader(OMS.class.getClassLoader());
        logger.info("It takes %sms to start OMS", _.ms() - ts);
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

        String s = System.getProperty("oms.mode");
        if (null != s) {
            mode = Mode.valueOfIgnoreCase(s);
        }
        logger.info("OMS start in %s mode", mode);

        conf = new OmsConfLoader().load(null);
    }

    private static void loadPlugins() {
        new PluginScanner().scan();
    }

    private static void initViewManager() {
        viewManager = new ViewManager();
    }

    private static void initSessionManager() {
        sessionManager = new SessionManager();
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
