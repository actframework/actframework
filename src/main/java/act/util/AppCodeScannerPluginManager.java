package act.util;

import act.app.AppCodeScannerManager;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import act.app.App;
import act.app.AppByteCodeScanner;
import act.app.AppSourceCodeScanner;
import org.osgl.util.C;

import java.util.Map;

public class AppCodeScannerPluginManager {

    private static final Logger logger = L.get(AppCodeScannerPluginManager.class);

    private Map<Class<? extends AppCodeScannerPluginBase>, AppCodeScannerPluginBase> registry = C.newMap();

    public void register(AppCodeScannerPluginBase plugin) {
        Class<? extends AppCodeScannerPluginBase> clz = plugin.getClass();
        if (registry.containsKey(clz)) {
            logger.warn("%s has already been registered", clz);
            return;
        }
        registry.put(clz, plugin);
    }

    public void initApp(App app) {
        AppCodeScannerManager manager = app.scannerManager();
        for (AppCodeScannerPluginBase plugin : registry.values()) {
            AppSourceCodeScanner sourceCodeScanner = plugin.createAppSourceCodeScanner();
            if (null != sourceCodeScanner) {
                manager.register(sourceCodeScanner);
            }
            AppByteCodeScanner byteCodeScanner = plugin.createAppByteCodeScanner();
            if (null != byteCodeScanner) {
                manager.register(byteCodeScanner);
            }
        }
    }
}
