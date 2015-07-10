package act.view;

import act.Act;
import act.app.AppContext;
import org.osgl.http.H;
import org.osgl.util.IO;

import java.util.Map;

/**
 * Base class for {@link Template} implementations
 */
public abstract class TemplateBase implements Template {

    @Override
    public void merge(AppContext context) {
        Map<String, Object> renderArgs = context.renderArgs();
        exposeImplicitVariables(renderArgs, context);
        beforeRender(context);
        merge(renderArgs, context.resp());
    }

    @Override
    public String render(AppContext context) {
        Map<String, Object> renderArgs = context.renderArgs();
        exposeImplicitVariables(renderArgs, context);
        beforeRender(context);
        return render(renderArgs);
    }

    /**
     * Sub class can implement this method to inject logic that needs to be done
     * before merge happening
     *
     * @param context
     */
    protected void beforeRender(AppContext context) {
    }

    protected void merge(Map<String, Object> renderArgs, H.Response response) {
        IO.writeContent(render(renderArgs), response.writer());
    }

    protected abstract String render(Map<String, Object> renderArgs);

    private void exposeImplicitVariables(Map<String, Object> renderArgs, AppContext context) {
        for (VarDef var : Act.viewManager().implicitVariables()) {
            renderArgs.put(var.name(), var.evaluate(context));
        }
    }
}
