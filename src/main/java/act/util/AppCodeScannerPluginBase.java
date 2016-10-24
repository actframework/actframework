package act.util;

import act.Act;
import act.ActComponent;
import act.app.App;
import act.app.AppByteCodeScanner;
import act.app.AppSourceCodeScanner;
import act.plugin.Plugin;
import org.osgl.logging.L;
import org.osgl.logging.Logger;

@ActComponent
public abstract class AppCodeScannerPluginBase extends DestroyableBase implements Plugin {

    @Override
    public void register() {
        if (!load()) {
            Act.logger.warn("Scanner plugin cannot be loaded: " + getClass().getName());
            return;
        }
        Act.scannerPluginManager().register(this);
        Act.logger.debug("Plugin registered: %s", getClass().getName());
    }

    public abstract AppSourceCodeScanner createAppSourceCodeScanner(App app);

    public abstract AppByteCodeScanner createAppByteCodeScanner(App app);

    public abstract boolean load();
}
