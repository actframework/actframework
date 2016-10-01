package act.view;

import act.Act;
import act.app.App;
import act.app.event.AppEventId;
import act.conf.AppConfig;
import act.plugin.AppServicePlugin;
import act.util.ActContext;
import org.osgl.util.S;

import java.io.File;

/**
 * The base class that different View solution should extends
 */
public abstract class View extends AppServicePlugin {

    /**
     * Returns the View solution's name. Recommended name should
     * be in lower case characters. E.g. freemarker, velocity,
     * rythm etc
     */
    public abstract String name();

    @Override
    protected void applyTo(final App app) {
        Act.viewManager().register(this);
        app.jobManager().on(AppEventId.CLASS_LOADER_INITIALIZED, new Runnable() {
            @Override
            public void run() {
                init(app);
            }
        });
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
    protected void init(App app) {
    }

    protected void reload(App app) {
        init(app);
    }

    protected final String templateHome() {
        String templateHome = Act.appConfig().templateHome();
        if (S.blank(templateHome) || "default".equals(templateHome)) {
            templateHome = "/" + name();
        }
        return templateHome;
    }

    protected final File templateRootDir() {
        App app = Act.app();
        return new File(app.layout().resource(app.base()), templateHome());
    }
}
