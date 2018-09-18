package act.app;

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

import static act.app.RuntimeDirs.*;

import act.Act;
import org.osgl.util.E;
import org.osgl.util.IO;

import java.io.File;

/**
 * Build Application when Act running in DEV mode.
 * TODO: we probably don't need to copy resources in DEV mode
 */
class AppBuilder {

    // When Act running is other mode than DEV mode, there is no need to
    // build an app as the app has already been built and deployed
    private static final AppBuilder PLACE_HOLDER = new AppBuilder() {
        @Override
        public void build() {
            // just placeholder
        }
    };

    private final App app;
    private final ProjectLayout layout;
    private final File appBase;
    private File tgtLib;
    private File tgtAsset;
    private File tgtClasses;
    private File tgtConf;

    private AppBuilder() {
        app = null;
        layout = null;
        appBase = null;
    }

    private AppBuilder(App app) {
        this.app = app;
        this.layout = app.layout();
        this.appBase = app.base();
    }

    public void build() {
        //prepareTargetDir();
        //copyFiles();
    }

    private void prepareTargetDir() {
        File target = layout.target(appBase);
        verifyDir(target, "target", true);
        tgtClasses = verifyDir(new File(target, CLASSES), CLASSES, true);
        tgtLib = verifyDir(new File(target, LIB), LIB, true);
        tgtAsset = verifyDir(new File(target, ASSET), ASSET, true);
        tgtConf = verifyDir(new File(tgtClasses, CONF), CONF, true);
    }

    private void copyFiles() {
        copyLibs();
        // copyResources();
        // copyConf();
        // copyRoutes();
    }

    void copyResources() {
        File resource = layout.resource(appBase);
        if (null == resource || !resource.canRead()) {
            return;
        }
        verifyDir(resource, "resource", false);
        IO.copyDirectory(resource, tgtClasses);
    }

    void copyResources(File... files) {
        File base = layout.resource(appBase);
        int baseLen = base.getAbsolutePath().length();
        for (File file : files) {
            String path = file.getAbsolutePath().substring(baseLen);
            File target = new File(tgtClasses, path);
            IO.copyDirectory(file, target);
        }
    }

    void removeResources(File ... files) {
        File base = layout.resource(appBase);
        int baseLen = base.getAbsolutePath().length();
        for (File file : files) {
            String path = file.getAbsolutePath().substring(baseLen);
            File target = new File(tgtClasses, path);
            target.delete();
        }
    }

    void copyRoutes() {
        //TODO fix me
//        List<File> routes = layout.routeTables(appBase);
//        for (File file : routes) {
//            if (!file.canRead()) {
//                continue;
//            }
//            //IO.copyDirectory(file, RuntimeDirs.routes(app));
//            E.tbd();
//        }
    }

    void copyConf() {
        File conf = layout.conf(appBase);
        if (null == conf || !conf.canRead()) return;
        if (conf.isDirectory()) {
            IO.copyDirectory(conf, tgtConf);
        } else {
            IO.copyDirectory(conf, new File(tgtConf, conf.getName()));
        }
    }

    void copyLibs() {
        File lib = layout.lib(appBase);
        if (null == lib || !lib.exists()) {
            return;
        }
        verifyDir(lib, "lib", false);
        IO.copyDirectory(lib, tgtLib);
    }

    private File verifyDir(File dir, String label, boolean create) {
        if (create && !dir.exists()) {
            E.unexpectedIf(!dir.mkdirs(), "Cannot create %s dir %s for %s", dir, label, app);
            return dir;
        }
        E.unexpectedIf(!dir.isDirectory(), "%s %s is not a directory", label, dir);
        E.unexpectedIf(!dir.canRead(), "Cannot read %s dir %s", label, dir);
        E.unexpectedIf(!dir.canWrite(), "Cannot write %s dir %s", label, dir);
        return dir;
    }

    public static AppBuilder build(App app) {
        AppBuilder builder = PLACE_HOLDER;
        if (Act.mode() == Act.Mode.DEV) {
            builder = new AppBuilder(app);
        }
        builder.build();
        return builder;
    }
}
