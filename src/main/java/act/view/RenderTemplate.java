package act.view;

import act.Act;
import act.app.ActionContext;
import act.exception.ActException;
import org.osgl.http.H;
import org.osgl.util.E;

import java.util.Map;

/**
 * Render a template with template path and arguments provided
 * by {@link ActionContext}
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

    public void apply(ActionContext context) {
        if (null != renderArgs && !renderArgs.isEmpty()) {
            for (String key : renderArgs.keySet()) {
                context.renderArg(key, renderArgs.get(key));
            }
        }
        ViewManager vm = Act.viewManager();
        Template t = vm.load(context);
        if (null == t) {
            throw new ActException("Render template[%s] not found", context.templatePath());
        }
        applyStatus(context.resp());
        H.Request req = context.req();
        H.Response resp = context.resp();
        resp.contentType(req.accept().contentType());
        applyBeforeCommitHandler(req, resp);
        t.merge(context);
        applyAfterCommitHandler(req, resp);
    }
}
