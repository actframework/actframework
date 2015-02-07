package org.osgl.oms.app;

import org.osgl.oms.OMS;
import org.osgl.util.E;
import org.osgl.util.IO;

import java.io.File;

/**
 * Build Application when OMS running in DEV mode.
 */
class AppBuilder {

    // When OMS running is other mode than DEV mode, there is no need to
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
        prepareTargetDir();
        copyFiles();
        E.tbd();
    }

    private void prepareTargetDir() {
        File target = layout.target(appBase);
        verifyDir(target, "target", true);
        verifyDir(new File(target, "asset"), "asset", true);
        File webInf = verifyDir(new File(target, "WEB-INF"), "WEB-INF", true);
        tgtClasses = verifyDir(new File(webInf, "classes"), "WEB-INF/classes", true);
        tgtLib = verifyDir(new File(webInf, "lib"), "WEB-INF/lib", true);
        tgtAsset = verifyDir(new File(webInf, "asset"), "WEB-INF/asset", true);
        tgtConf = verifyDir(new File(webInf, "conf"), "WEB-INF/conf", true);
    }

    private void copyFiles() {
        File lib = layout.lib(appBase);
        verifyDir(lib, "lib", false);
        IO.copyDirectory(lib, tgtLib);

        File asset = layout.asset(appBase);
        verifyDir(asset, "asset", false);
        IO.copyDirectory(asset, tgtAsset);

        File resource = layout.resource(appBase);
        verifyDir(resource, "resource", false);
        IO.copyDirectory(resource, tgtClasses);

        File conf = layout.conf(appBase);
        if (conf.isDirectory()) {
            IO.copyDirectory(conf, tgtConf);
        } else {
            IO.copyDirectory(conf, new File(tgtConf, conf.getName()));
        }
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
        if (OMS.mode() == OMS.Mode.DEV) {
            builder = new AppBuilder(app);
        }
        builder.build();
        return builder;
    }
}
