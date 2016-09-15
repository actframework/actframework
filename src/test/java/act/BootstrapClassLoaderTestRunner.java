package act;

import act.boot.server.ServerBootstrapClassLoader;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.File;

public class BootstrapClassLoaderTestRunner extends BlockJUnit4ClassRunner {

    public BootstrapClassLoaderTestRunner(Class<?> clazz) throws InitializationError {
        super(getFromTestClassloader(clazz));
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    private static Class<?> getFromTestClassloader(Class<?> clazz) throws InitializationError {
        try {
            Act.initEnhancerManager();
            return classLoader().loadClass(clazz.getName());
        } catch (ClassNotFoundException e) {
            throw new InitializationError(e);
        }
    }

    private static ServerBootstrapClassLoader classLoader() {
        if (null == classLoader) {
            initClassLoader();
        }
        return classLoader;
    }

    private static void initClassLoader() {
        prepareActHome();
        classLoader = new TestServerBootstrapClassLoader(TestBase.class.getClassLoader());
    }

    private static void prepareActHome() {
        File actHome = TestBase.root();
        File lib = ensureDir(actHome, "lib");
        File plugin = ensureDir(actHome, "plugin");
        packageActJarInto(lib);
        packageTestJarInto(lib);
        packagePluginJarInto(plugin);
        System.setProperty(Constants.ACT_HOME, actHome.getAbsolutePath());
    }

    private static File ensureDir(File root, String dir) {
        File file = new File(root, dir);
        E.unexpectedIf(!file.exists() && !file.mkdirs(), "Cannot create act dir: %s", file.getAbsolutePath());
        return file;
    }

    private static void packageActJarInto(File lib) {
        File classes = new File(TestBase.root(), "classes");
        packageJarInto(classes, new File(lib, "act.jar"), "*");
    }

    private static void packageTestJarInto(File lib) {
        File classes = new File(TestBase.root(), "test-classes");
        packageJarInto(classes, new File(lib, "act-test.jar"), "*");
    }

    private static void packagePluginJarInto(File lib) {
        File classes = new File(TestBase.root(), "test-classes");
        packageJarInto(classes, new File(lib, "playground.jar"), "playground");
    }

    private static void packageJarInto(File from, File to, String selector) {
        try {
            String cmd = S.fmt("jar cf %s %s", to.getAbsolutePath(), selector);
            Process p = Runtime.getRuntime().exec(cmd.split("[\\s]+"), null, from);
            p.waitFor();
            String out = IO.readContentAsString(p.getInputStream());
            String err = IO.readContentAsString(p.getErrorStream());
        } catch (Exception e) {
            throw E.unexpected(e);
        }
    }

    public static ServerBootstrapClassLoader classLoader;
}
