package act.view;

import act.Act;
import act.app.ActionContext;
import act.exception.ActException;
import org.osgl.http.H;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.Map;

/**
 * Render a template with template path and arguments provided
 * by {@link ActionContext}
 */
public class RenderTemplate extends RenderAny {

    public static RenderTemplate INSTANCE = new RenderTemplate();

    static final ThreadLocal<Map<String, Object>> renderArgsBag = new ThreadLocal<>();

    private RenderTemplate() {
    }

    @Override
    public void apply(H.Request req, H.Response resp) {
        renderArgsBag.remove();
        throw E.unsupport("RenderTemplate does not support " +
                "apply to request and response. Please use apply(AppContext) instead");
    }

    public void apply(ActionContext context) {
        Map<String, Object> renderArgs = renderArgsBag.get();
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
        setContentType(req, resp);
        applyBeforeCommitHandler(req, resp);
        t.merge(context);
        applyAfterCommitHandler(req, resp);
    }

    protected void setContentType(H.Request req, H.Response resp) {
        String s = req.accept().contentType();
        String encoding = resp.characterEncoding();
        if(S.notBlank(encoding)) {
            s = S.builder(s).append("; charset=").append(encoding.toLowerCase()).toString();
        }

        resp.initContentType(s);
    }

    public static RenderTemplate get() {
        return INSTANCE;
    }

    public static RenderTemplate get(H.Status status) {
        payload.get().status(status);
        return INSTANCE;
    }

    public static RenderTemplate of(Map<String, Object> args) {
        renderArgsBag.set(args);
        return INSTANCE;
    }

    public static RenderTemplate of(H.Status status, Map<String, Object> args) {
        payload.get().status(status);
        renderArgsBag.set(args);
        return INSTANCE;
    }
}
