package org.osgl.oms;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.File;

/**
 * Created by luog on 7/02/2015.
 */
public class BootstrapClassLoaderTestRunner extends BlockJUnit4ClassRunner {
    public BootstrapClassLoaderTestRunner(Class<?> clazz) throws InitializationError {
        super(getFromTestClassloader(clazz));
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    private static Class<?> getFromTestClassloader(Class<?> clazz) throws InitializationError {
        try {
            return classLoader().loadClass(clazz.getName());
        } catch (ClassNotFoundException e) {
            throw new InitializationError(e);
        }
    }

    private static BootstrapClassLoader classLoader() {
        if (null == classLoader) {
            initClassLoader();
        }
        return classLoader;
    }

    private static void initClassLoader() {
        prepareOmsHome();
        classLoader = new BootstrapClassLoader(TestBase.class.getClassLoader());
    }

    private static void prepareOmsHome() {
        File omsHome = TestBase.root();
        File lib = ensureDir(omsHome, "lib");
        File plugin = ensureDir(omsHome, "plugin");
        packageOmsJarInto(lib);
        packageTestJarInto(lib);
        packagePluginJarInto(plugin);
        System.setProperty(BootstrapClassLoader.OMS_HOME, omsHome.getAbsolutePath());
    }

    private static File ensureDir(File root, String dir) {
        File file = new File(root, dir);
        E.unexpectedIf(!file.exists() && !file.mkdirs(), "Cannot create oms dir: %s", file.getAbsolutePath());
        return file;
    }

    private static void packageOmsJarInto(File lib) {
        File classes = new File(TestBase.root(), "classes");
        packageJarInto(classes, new File(lib, "oms.jar"), "*");
    }

    private static void packageTestJarInto(File lib) {
        File classes = new File(TestBase.root(), "test-classes");
        packageJarInto(classes, new File(lib, "oms-test.jar"), "*");
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
            if (S.notEmpty(out)) {
                System.out.printf("%s\n", out);
            }
            if (S.notEmpty(err)) {
                System.err.println(err);
            }
        } catch (Exception e) {
            throw E.unexpected(e);
        }
    }

    public static BootstrapClassLoader classLoader;
}
