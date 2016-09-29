package act.view;

import act.Act;
import act.app.App;
import act.conf.AppConfig;
import act.plugin.Plugin;
import act.util.ActContext;
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
        Act.viewManager().register(this);
        init();
    }

    public Template load(ActContext context) {
        Template cached = context.cachedTemplate();
        if (null != cached) {
            return cached;
        }

        AppConfig config = context.config();

        TemplatePathResolver resolver = config.templatePathResolver();
        String path = resolver.resolve(context);

        StringBuilder sb = S.builder();
        if (!path.startsWith("/")) {
            sb.append("/");
        }
        sb.append(path);

        Template template = loadTemplate(sb.toString(), context);
        if (null != template) {
            context.cacheTemplate(template);
        }
        return template;
    }

    /**
     * Sub class must implement this method to load the template
     *
     * @param resourcePath the path to the template
     * @param context      the view context
     * @return the template instance or {@code null} if template not found
     */
    protected abstract Template loadTemplate(String resourcePath, ActContext context);

    /**
     * Sub class could use this method initialize the implementation
     */
    protected void init() {
    }

    protected void reload(App app) {}
}
