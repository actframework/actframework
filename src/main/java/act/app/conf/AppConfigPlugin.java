package act.app.conf;

import act.app.App;
import act.app.event.AppEventId;
import act.util.SubClassFinder;

/**
 * {@code AppConfigPlugin} scan source code or byte code to detect if there are
 * any user defined {@link AppConfigurator} implementation and use it to populate
 * {@link act.conf.AppConfig} default values
 */
public class AppConfigPlugin  {

    @SubClassFinder(callOn = AppEventId.CLASS_LOADED)
    public static void foundConfigurator(final Class<? extends AppConfigurator> configuratorClass) {
        final App app = App.instance();
        AppConfigurator configurator = app.getInstance(configuratorClass);
        configurator.app(app);
        configurator.configure();
        app.config()._merge(configurator);
    }

}