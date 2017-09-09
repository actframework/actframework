package act;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
        classLoader = new TestServerBootstrapClassLoader(ActTestBase.class.getClassLoader());
    }

    private static void prepareActHome() {
        File actHome = ActTestBase.root();
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
        File classes = new File(ActTestBase.root(), "classes");
        packageJarInto(classes, new File(lib, "act.jar"), "*");
    }

    private static void packageTestJarInto(File lib) {
        File classes = new File(ActTestBase.root(), "test-classes");
        packageJarInto(classes, new File(lib, "act-test.jar"), "*");
    }

    private static void packagePluginJarInto(File lib) {
        File classes = new File(ActTestBase.root(), "test-classes");
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
