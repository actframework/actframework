package act.view;

import act.cli.view.CliView;
import act.util.ActContext;
import act.util.PropertySpec;
import org.osgl.http.H;
import org.osgl.mvc.result.RenderContent;

/**
 * An enhanced version of {@link org.osgl.mvc.result.RenderJSON} that
 * allows {@link PropertySpec} to be applied to control the
 * output fields
 */
public class RenderCSV extends RenderContent {

    private ActContext context;

    public RenderCSV(Object v, PropertySpec.MetaInfo spec, ActContext context) {
        super(render(v, spec, context), H.Format.CSV);
        this.context = context;
    }

    private static String render(Object v, PropertySpec.MetaInfo spec, ActContext context) {
        return CliView.CSV.render(v, spec, context);
    }
}
