package org.osgl.oms.view;

import org.osgl.http.H;
import org.osgl.oms.OMS;
import org.osgl.oms.app.AppContext;

import java.util.Map;

/**
 * Base class for {@link Template} implementations
 */
public abstract class TemplateBase implements Template {

    @Override
    public void merge(AppContext context) {
        Map<String, Object> renderArgs = context.renderArgs();
        exposeImplicitVariables(renderArgs, context);
        merge(renderArgs, context.resp());
    }

    /**
     * Sub class can implement this method to inject logic that needs to be done
     * before merge happening
     *
     * @param context
     */
    protected void prepareMerge(AppContext context) {
    }

    protected abstract void merge(Map<String, Object> renderArgs, H.Response response);

    private void exposeImplicitVariables(Map<String, Object> renderArgs, AppContext context) {
        for (VarDef var : OMS.viewManager().implicitVariables()) {
            renderArgs.put(var.name(), var.evaluate(context));
        }
    }
}
