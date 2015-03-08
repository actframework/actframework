package org.osgl.oms;

import org.osgl._;
import org.osgl.exception.NotAppliedException;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.oms.controller.meta.CatchMethodMetaInfo;
import org.osgl.oms.handler.builtin.controller.*;
import org.osgl.oms.app.*;
import org.osgl.oms.conf.OmsConfLoader;
import org.osgl.oms.conf.OmsConfig;
import org.osgl.oms.controller.meta.ActionMethodMetaInfo;
import org.osgl.oms.controller.meta.InterceptorMethodMetaInfo;
import org.osgl.oms.handler.builtin.controller.impl.ReflectedHandlerInvoker;
import org.osgl.oms.plugin.PluginScanner;
import org.osgl.oms.util.Banner;
import org.osgl.util.E;

/**
 * The OSGL MVC Server object
 */
public final class OMS {

    public static enum Mode {
        PROD, UAT, SIT, DEV () {
            @Override
            public AppScanner appScanner() {
                return AppScanner.DEV_MODE_SCANNER;
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

    private static OmsConfig conf;
    private static Logger logger = L.get(OMS.class);
    private static Mode mode = Mode.PROD;
    private static AppManager appManager;
    private static BytecodeEnhancerManager enhancerManager;


    public static BootstrapClassLoader classLoader() {
        return (BootstrapClassLoader)OMS.class.getClassLoader();
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
    public static BytecodeEnhancerManager enhancerManager() {
        return enhancerManager;
    }

    public static void start() {
        Banner.print("0.0.1-SNAPSHOT");
        loadConfig();
        initExecuteService();
        initEnhancerManager();
        loadPlugins();
        initNetworkLayer();
        initApplicationManager();
        startNetworkLayer();
    }

    public static RequestServerRestart requestRestart() {
        E.illegalStateIf(!isDev());
        throw new RequestServerRestart();
    }

    public static RequestRefreshClassLoader requestRefreshClassLoader() {
        E.illegalStateIf(!isDev());
        throw new RequestRefreshClassLoader();
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

    private static void initExecuteService() {
        E.tbd("init execute service");
    }

    static void initEnhancerManager() {
        enhancerManager = new BytecodeEnhancerManager();
    }

    private static void initNetworkLayer() {
        E.tbd("init network server");
    }

    private static void initApplicationManager() {
        appManager = AppManager.create();
    }

    private static void startNetworkLayer() {
    }

    public static enum F {
        ;
        public static final _.F0<Mode> MODE_ACCESSOR = new  _.F0<Mode>() {
            @Override
            public Mode apply() throws NotAppliedException, _.Break {
                return mode;
            }
        };
    }

}
