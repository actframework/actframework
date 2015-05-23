package org.osgl.oms.di.guice;

import org.osgl.oms.app.AppByteCodeScanner;
import org.osgl.oms.app.AppSourceCodeScanner;
import org.osgl.oms.util.AppCodeScannerPluginBase;

public class GuicePlugin extends AppCodeScannerPluginBase {

    @Override
    public AppSourceCodeScanner createAppSourceCodeScanner() {
        return new GuiceSourceCodeSensor();
    }

    @Override
    public AppByteCodeScanner createAppByteCodeScanner() {
        return new GuiceByteCodeSensor();
    }
}
