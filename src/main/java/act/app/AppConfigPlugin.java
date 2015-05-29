package act.app;

import act.util.SubTypeFinder;
import org.osgl._;
import org.osgl.exception.NotAppliedException;

/**
 * {@code AppConfigPlugin} scan source code or byte code to detect if there are
 * any user defined {@link AppConfigurator} implementation and use it to populate
 * {@link act.conf.AppConfig} default values
 */
public class AppConfigPlugin extends SubTypeFinder {
    public AppConfigPlugin() {
        super(AppConfigurator.class, new _.F2<App, String, Void>() {
            @Override
            public Void apply(App app, String className) throws NotAppliedException, _.Break {
                Class<? extends AppConfigurator> c = _.classForName(className, app.classLoader());
                AppConfigurator conf = _.newInstance(c);
                app.config()._merge(conf);
                return null;
            }
        });
    }

    @Override
    public boolean load() {
        return true;
    }
}