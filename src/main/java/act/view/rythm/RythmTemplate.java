package act.view.rythm;

import act.app.App;
import act.app.AppContext;
import act.view.TemplateBase;
import org.osgl.http.H;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.rythmengine.RythmEngine;
import org.rythmengine.resource.ITemplateResource;
import org.rythmengine.template.ITemplate;

import java.lang.ref.SoftReference;
import java.util.Locale;
import java.util.Map;

public class RythmTemplate extends TemplateBase {

    private RythmEngine engine;
    private String path;
    private App app;

    public RythmTemplate(RythmEngine engine, String path, App app) {
        E.NPE(engine);
        this.engine = engine;
        this.path = path;
        this.app = app;
    }

    @Override
    protected void beforeRender(AppContext context) {
        Locale locale = context.locale();
        engine = engine.prepare(locale);
    }

    @Override
    protected String render(Map<String, Object> renderArgs) {
        // TODO handle render exception
        ITemplate t = template(renderArgs);
        return t.render();
    }

    private org.rythmengine.template.ITemplate template(Map<String, Object> renderArgs) {
        return engine.getTemplate(path, renderArgs);
    }

    public static RythmTemplate find(RythmEngine engine, String path, App app) {
        ITemplateResource resource = engine.resourceManager().getResource(path);
        if (!resource.isValid()) {
            return null;
        } else {
            return new RythmTemplate(engine, path, app);
        }
    }
}
