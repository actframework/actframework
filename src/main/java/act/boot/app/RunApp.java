package act.boot.app;

import act.Act;
import act.conf.AppConfigKey;
import act.util.SysProps;
import org.osgl.$;
import org.osgl.logging.Logger;
import org.osgl.util.E;
import org.osgl.util.S;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * The entry to start an Act full stack app
 */
public class RunApp {

    private static final Logger LOGGER = Act.LOGGER;

    /**
     * Start the application.
     *
     * ActFramework will scan the package defined by system property `act.scan_package`
     *
     * @throws Exception
     */
    public static void start() throws Exception {
        start(null, null, "");
    }

    public static void start(Class<?> anyController) throws Exception {
        start(null, null, anyController);
    }

    public static void start(String packageName) throws Exception {
        start(null, null, packageName);
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
        LOGGER.debug("run fullstack application with package[%s] %s", packageName, profile);
        //System.setProperty(AppConfigKey.CONTROLLER_PACKAGE.key(), packageName);
        final String SCAN_PACKAGE = AppConfigKey.SCAN_PACKAGE.key();
        if (S.notBlank(packageName)) {
            System.setProperty(SCAN_PACKAGE, packageName);
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
        LOGGER.info("it takes %sms to start the app\n", $.ms() - ts);
    }

    public static void main(String[] args) throws Exception {
        start();
    }
}
