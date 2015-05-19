package org.osgl.oms.view;

import org.osgl.http.H;
import org.osgl.mvc.result.Result;
import org.osgl.oms.OMS;
import org.osgl.oms.app.AppContext;
import org.osgl.util.E;

import java.util.Map;

/**
 * Render a template with template path and arguments provided
 * by {@link org.osgl.oms.app.AppContext}
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
        ViewManager vm = OMS.viewManager();
        Template t = vm.load(context);
        //context.dissolve();
        applyStatus(context.resp());
        t.merge(context);
    }
}
