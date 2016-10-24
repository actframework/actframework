package act.boot.app;

import act.conf.AppConfigKey;
import act.util.SysProps;
import org.osgl.$;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.E;
import org.osgl.util.FastStr;
import org.osgl.util.S;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * The entry to start an Act full stack app
 */
public class RunApp {

    private static final Logger logger = L.get(RunApp.class);

    public static void start() throws Exception {
        start(null, null, "");
    }

    public static void start(Class<?> anyController) throws Exception {
        start(null, null, anyController);
    }

    public static void start(String appName, String appVersion, Class<?> anyController) throws Exception {
        String pkg = anyController.getPackage().getName();
        start(appName, appVersion, pkg);
    }

    public static void start(String appName, String appVersion, String packageName) throws Exception {
        long ts = $.ms();
        String profile = SysProps.get(AppConfigKey.PROFILE.key());
        if (S.blank(profile)) {
            profile = "";
        } else {
            profile = "using profile[" + profile + "]";
        }
        logger.debug("run fullstack application with controller package[%s] %s", packageName, profile);
        System.setProperty(AppConfigKey.CONTROLLER_PACKAGE.key(), packageName);
        final String SCAN_KEY = AppConfigKey.SCAN_PACKAGE.key();
        if (!System.getProperties().containsKey(SCAN_KEY)) {
            String scan = packageName;
            if (FastStr.of(packageName).afterLast('.').startsWith("controller")) {
                scan = FastStr.of(packageName).beforeLast('.').toString();
            }
            System.setProperty(AppConfigKey.SCAN_PACKAGE.key(), scan);
        }
        FullStackAppBootstrapClassLoader classLoader = new FullStackAppBootstrapClassLoader(RunApp.class.getClassLoader());
        Thread.currentThread().setContextClassLoader(classLoader);
        Class<?> actClass = classLoader.loadClass("act.Act");
        Method m = actClass.getDeclaredMethod("startApp", String.class, String.class);
        try {
            m.invoke(null, appName, appVersion);
        } catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            if (t instanceof Exception) {
                throw (Exception) t;
            } else {
                throw E.unexpected(t, "Unknown error captured starting the application");
            }
        }
        logger.info("it takes %sms to start the app\n", $.ms() - ts);
    }

    public static void main(String[] args) throws Exception {
        start();
    }
}
