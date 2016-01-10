package act.view;

import act.app.ActionContext;
import act.cli.view.CliView;
import act.util.PropertySpec;
import org.osgl.http.H;
import org.osgl.mvc.result.RenderContent;

/**
 * An enhanced version of {@link org.osgl.mvc.result.RenderJSON} that
 * allows {@link act.util.PropertySpec} to be applied to control the
 * output fields
 */
public class FilteredRenderJSON extends RenderContent {

    private ActionContext context;

    public FilteredRenderJSON(Object v, PropertySpec.MetaInfo spec, ActionContext context) {
        super(render(v, spec, context), H.Format.JSON);
        this.context = context;
    }

    private static String render(Object v, PropertySpec.MetaInfo spec, ActionContext context) {
        return CliView.JSON.render(v, spec, context);
    }
}
