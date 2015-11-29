package act.plugin;

import act.Act;
import act.BootstrapClassLoaderTestRunner;
import act.TestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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
        same(4, Act.mode().appScanner().test_probeCnt());
    }
}
