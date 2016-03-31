package act.boot.app;

import act.Act;
import act.conf.AppConfigKey;
import act.metric.Timer;
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

    public static void start(Class<?> anyController) throws Exception {
        start(null, anyController);
    }

    public static void start(String appName, Class<?> anyController) throws Exception {
        String pkg = anyController.getPackage().getName();
        start(appName, pkg);
    }

    public static void start(String appName, String packageName) throws Exception {
        long ts = $.ms();
        String profile = SysProps.get(AppConfigKey.PROFILE.key());
        if (S.blank(profile)) {
            profile = "";
        } else {
            profile = "using profile[" + profile + "]";
        }
        logger.info("run fullstack application with controller package[%s] %s", packageName, profile);
        System.setProperty(AppConfigKey.CONTROLLER_PACKAGE.key(), packageName);
        final String SCAN_KEY = AppConfigKey.SCAN_PACKAGE.key();
        if (!System.getProperties().containsKey(SCAN_KEY)) {
            String scan = packageName;
            if (FastStr.of(packageName).afterLast('.').startsWith("controller")) {
                scan = FastStr.of(packageName).beforeLast('.').toString();
            }
            System.setProperty(AppConfigKey.SCAN_PACKAGE.key(), scan);
        }
        logger.debug("loading bootstrap classloader...");
        FullStackAppBootstrapClassLoader classLoader = new FullStackAppBootstrapClassLoader(RunApp.class.getClassLoader());
        Thread.currentThread().setContextClassLoader(classLoader);
        logger.debug("loading Act class...");
        Class<?> actClass = classLoader.loadClass("act.Act");
        Method m = actClass.getDeclaredMethod("startApp", String.class);
        try {
            m.invoke(null, appName);
        } catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            if (t instanceof Exception) {
                throw (Exception) t;
            } else {
                throw E.unexpected(t, "Unknown error captured starting the application");
            }
        }
        System.out.printf("it talks %sms to start the app\n", $.ms() - ts);
    }
}
