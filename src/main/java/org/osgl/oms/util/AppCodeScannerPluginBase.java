package org.osgl.oms.util;

import org.osgl.oms.OMS;
import org.osgl.oms.app.AppByteCodeScanner;
import org.osgl.oms.app.AppSourceCodeScanner;
import org.osgl.oms.plugin.Plugin;

public abstract class AppCodeScannerPluginBase implements Plugin {

    @Override
    public void register() {
        OMS.scannerPluginManager().register(this);
    }

    public abstract AppSourceCodeScanner createAppSourceCodeScanner();

    public abstract AppByteCodeScanner createAppByteCodeScanner();
}
