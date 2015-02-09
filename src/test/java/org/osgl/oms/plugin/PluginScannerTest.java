package org.osgl.oms.plugin;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgl._;
import org.osgl.oms.BootstrapClassLoaderTestRunner;
import org.osgl.oms.OMS;
import org.osgl.oms.TestBase;
import org.osgl.oms.util.WatchServiceFsWatcher;

@RunWith(BootstrapClassLoaderTestRunner.class)
public class PluginScannerTest extends TestBase {

    protected PluginScanner pluginScanner;

    @Before
    public void prepare() throws Exception {
        pluginScanner = new PluginScanner();
    }

    @Test
    public void testScan() {
        pluginScanner.scan();
        _.newInstance(WatchServiceFsWatcher.class, TestBase.root()).run();
        same(4, OMS.mode().appScanner().test_probeCnt());
    }
}
