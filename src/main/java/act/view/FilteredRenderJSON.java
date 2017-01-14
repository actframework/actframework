package act.view;

import act.app.ActionContext;
import act.cli.view.CliView;
import act.util.ActContext;
import act.util.PropertySpec;
import org.osgl.http.H;
import org.osgl.mvc.result.RenderContent;

/**
 * An enhanced version of {@link org.osgl.mvc.result.RenderJSON} that
 * allows {@link act.util.PropertySpec} to be applied to control the
 * output fields
 */
public class FilteredRenderJSON extends RenderContent {

    public static final FilteredRenderJSON _INSTANCE = new FilteredRenderJSON() {
        @Override
        public String content() {
            return payload().message;
        }
    };

    private FilteredRenderJSON() {
        super(H.Format.JSON);
    }

    public FilteredRenderJSON(Object v, PropertySpec.MetaInfo spec, ActContext context) {
        super(render(v, spec, context), H.Format.JSON);
    }

    private static String render(Object v, PropertySpec.MetaInfo spec, ActContext context) {
        return CliView.JSON.render(v, spec, context);
    }

    public static FilteredRenderJSON get(Object v, PropertySpec.MetaInfo spec, ActionContext context) {
        payload.get().message(render(v, spec, context));
        return _INSTANCE;
    }
}
