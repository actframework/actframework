package act.di.guice;

import act.app.App;
import act.app.AppByteCodeScanner;
import act.app.AppSourceCodeScanner;
import act.di.DependencyInjector;
import act.util.AppCodeScannerPluginBase;

public class GuicePlugin extends AppCodeScannerPluginBase {

    @Override
    public AppSourceCodeScanner createAppSourceCodeScanner(App app) {
        DependencyInjector injector = app.injector();
        if (null != injector && !(injector instanceof GuiceDependencyInjector)) {
            return null;
        }
        return new GuiceSourceCodeSensor();
    }

    @Override
    public AppByteCodeScanner createAppByteCodeScanner(App app) {
        DependencyInjector injector = app.injector();
        if (null != injector && !(injector instanceof GuiceDependencyInjector)) {
            return null;
        }
        return new GuiceByteCodeSensor();
    }

    @Override
    public boolean load() {
        try {
            new GuiceByteCodeSensor();
            return true;
        } catch (Throwable e) {
            return false;
        }
    }
}
