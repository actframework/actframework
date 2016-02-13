package act.view;

import act.cli.view.CliView;
import act.util.ActContext;
import act.util.PropertySpec;
import org.osgl.http.H;
import org.osgl.mvc.result.RenderContent;

/**
 * An enhanced version of {@link org.osgl.mvc.result.RenderXML} that
 * allows {@link PropertySpec} to be applied to control the
 * output fields
 */
public class FilteredRenderXML extends RenderContent {

    public FilteredRenderXML(Object v, PropertySpec.MetaInfo spec, ActContext context) {
        super(render(v, spec, context), H.Format.XML);
    }

    private static String render(Object v, PropertySpec.MetaInfo spec, ActContext context) {
        return CliView.XML.render(v, spec, context);
    }
}
