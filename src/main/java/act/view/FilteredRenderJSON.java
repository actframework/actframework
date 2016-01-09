package act.view;

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
    public FilteredRenderJSON(Object v, PropertySpec.MetaInfo spec) {
        super(render(v, spec), H.Format.JSON);
    }

    private static String render(Object v, PropertySpec.MetaInfo spec) {
        return CliView.JSON.render(v, spec);
    }
}
