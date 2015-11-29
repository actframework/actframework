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
        long ts = $.ms();
        String pkg = anyController.getPackage().getName();
        logger.info("run fullstack application with controller package: %s", pkg);
        System.setProperty(AppConfigKey.CONTROLLER_PACKAGE.key(), pkg);
        String scan = pkg;
        if (FastStr.of(pkg).afterLast('.').startsWith("controller")) {
            scan = FastStr.of(pkg).beforeLast('.').toString();
        }
        System.setProperty(AppConfigKey.SCAN_PACKAGE.key(), scan);
        FullStackAppBootstrapClassLoader classLoader = new FullStackAppBootstrapClassLoader(RunApp.class.getClassLoader());
        Thread.currentThread().setContextClassLoader(classLoader);
        Class<?> actClass = classLoader.loadClass("act.Act");
        Method m = actClass.getDeclaredMethod("startApp");
        m.invoke(null);
        System.out.printf("it talks %sms to start the app\n", $.ms() - ts);
    }
}
