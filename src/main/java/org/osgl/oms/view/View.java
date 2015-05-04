package org.osgl.oms.view;

import org.osgl.oms.OMS;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.conf.AppConfig;
import org.osgl.oms.plugin.Plugin;
import org.osgl.util.S;

/**
 * The base class that different View solution should extends
 */
public abstract class View implements Plugin {

    /**
     * Returns the View solution's name. Recommended name should
     * be in lower case characters. E.g. freemarker, velocity,
     * rythm etc
     */
    public abstract String name();

    @Override
    public void register() {
        OMS.viewManager().register(this);
        init();
    }

    public Template load(AppContext context) {
        Template cached = context.cachedTemplate();
        if (null != cached) {
            return cached;
        }

        AppConfig config = context.config();

        TemplatePathResolver resolver = config.templatePathResolver();
        String path = resolver.resolve(context);

        String home = config.templateHome();
        if ("default".equals(home)) {
            home = name().toLowerCase();
        }

        StringBuilder sb = S.builder();
        if (!home.startsWith("/")) {
            sb.append("/");
        }
        sb.append(home);
        if (!home.endsWith("/")) {
            sb.append("/");
        }
        sb.append(path);

        return loadTemplate(sb.toString(), context);
    }

    /**
     * Sub class must implement this method to load the template
     *
     * @param resourcePath the path to the template
     * @param context      the application context
     * @return the template instance or {@code null} if template not found
     */
    protected abstract Template loadTemplate(String resourcePath, AppContext context);

    /**
     * Sub class could use this method initialize the implementation
     */
    protected void init() {
    }
}
