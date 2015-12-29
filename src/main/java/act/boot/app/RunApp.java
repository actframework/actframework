package act.boot.app;

import act.conf.AppConfigKey;
import org.osgl.$;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.FastStr;

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
        logger.info("run fullstack application with controller package: %s", packageName);
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
        Method m = actClass.getDeclaredMethod("startApp", String.class);
        m.invoke(null, appName);
        System.out.printf("it talks %sms to start the app\n", $.ms() - ts);
    }
}
