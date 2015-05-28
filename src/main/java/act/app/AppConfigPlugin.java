package act.app;

import act.util.AppCodeScannerPluginBase;
import act.util.ByteCodeVisitor;
import org.osgl._;
import org.osgl.util.S;

/**
 * {@code AppConfigPlugin} scan source code or byte code to detect if there are
 * any user defined {@link AppConfigurator} implementation and use it to populate
 * {@link act.conf.AppConfig} default values
 */
public class AppConfigPlugin extends AppCodeScannerPluginBase {
    @Override
    public AppSourceCodeScanner createAppSourceCodeScanner(App app) {
        if (!shouldScan(app)) {
            return null;
        }
        return new AppConfigSourceCodeSensor();
    }

    @Override
    public AppByteCodeScanner createAppByteCodeScanner(App app) {
        if (!shouldScan(app)) {
            return null;
        }
        return new AppConfigByteCodeSensor();
    }

    @Override
    public boolean load() {
        return true;
    }

    private boolean shouldScan(App app) {
        AppConfigurator configurator = app.config().appConfigurator();
        if (null != configurator) {
            app.config()._merge(configurator);
            return false;
        }
        return true;
    }
}

class AppConfigSourceCodeSensor extends AppSourceCodeScannerBase {

    private static final String PKG = "act.app";
    private static final String CLS = "AppConfigurator";

    private boolean pkgFound = false;
    private boolean clsFound = false;

    @Override
    protected void _visit(int lineNumber, String line, String className) {
        if (!pkgFound) {
            if (line.contains(PKG)) {
                pkgFound = true;
            }
        }
        if (pkgFound) {
            clsFound = line.contains(CLS);
            if (clsFound) {
                logger.info("%s is a app configurator", className);
                markScanByteCode();
            }
        }
    }

    @Override
    protected boolean shouldScan(String className) {
        return true;
    }
}

class AppConfigByteCodeSensor extends AppByteCodeScannerBase {

    private boolean isAppConfigurator = false;

    @Override
    protected void reset(String className) {
        isAppConfigurator = false;
    }

    @Override
    protected boolean shouldScan(String className) {
        return true;
    }

    @Override
    public ByteCodeVisitor byteCodeVisitor() {
        return new _ByteCodeVisitor();
    }

    @Override
    public void scanFinished(String className) {
        if (isAppConfigurator) {
            Class<? extends AppConfigurator> c = _.classForName(className, app().classLoader());
            AppConfigurator conf = _.newInstance(c);
            app().config()._merge(conf);
            logger.info("User defined application configurator has been applied", className);
        }
    }

    @Override
    public void allScanFinished() {

    }

    private class _ByteCodeVisitor extends ByteCodeVisitor {
        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            if (S.eq("act/App/AppConfigurator", superName)) {
                isAppConfigurator = true;
                logger.info("user defined app configurator found: %s", name);
            }
            super.visit(version, access, name, signature, superName, interfaces);
        }
    }
}
