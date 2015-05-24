package act.view;

import act.Act;
import org.osgl.http.H;
import act.app.AppContext;
import org.osgl.util.E;

import java.util.Map;

/**
 * Render a template with template path and arguments provided
 * by {@link AppContext}
 */
public class RenderTemplate extends RenderAny {

    private Map<String, Object> renderArgs;

    public RenderTemplate() {
    }

    public RenderTemplate(Map<String, Object> renderArgs) {
        this();
        this.renderArgs = renderArgs;
    }

    @Override
    public void apply(H.Request req, H.Response resp) {
        throw E.unsupport("RenderTemplate does not support " +
                "apply to request and response. Please use apply(AppContext) instead");
    }

    public void apply(AppContext context) {
        if (null != renderArgs && !renderArgs.isEmpty()) {
            for (String key : renderArgs.keySet()) {
                context.renderArg(key, renderArgs.get(key));
            }
        }
        ViewManager vm = Act.viewManager();
        Template t = vm.load(context);
        //context.dissolve();
        applyStatus(context.resp());
        t.merge(context);
    }
}
