package act.di.guice;

import act.app.AppByteCodeScanner;
import act.app.AppSourceCodeScanner;
import act.util.AppCodeScannerPluginBase;

public class GuicePlugin extends AppCodeScannerPluginBase {

    @Override
    public AppSourceCodeScanner createAppSourceCodeScanner() {
        return new GuiceSourceCodeSensor();
    }

    @Override
    public AppByteCodeScanner createAppByteCodeScanner() {
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
